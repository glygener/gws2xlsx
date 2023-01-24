package org.glygen.gws2xlsx.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.glycoinfo.GlycanFormatconverter.Glycan.GlyContainer;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.GlyContainerToSugar;
import org.glycoinfo.GlycanFormatconverter.util.exchange.WURCSGraphToGlyContainer.WURCSGraphToGlyContainer;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GlytoucanUtil {
	
	String apiKey;
	String userId;
	
	static String glycanURL = "https://sparqlist.glycosmos.org/sparqlist/api/gtc_wurcs_by_accession?accNum=";
	static String retrieveURL ="https://sparqlist.glyconavi.org/api/WURCS2GlyTouCan?WURCS=";
	static String registerURL = "https://api.glytoucan.org/glycan/register";
	static String validateURL = "wurcsframework/wurcsvalidator/1.0.1/";
	static String apiURL = "https://api.glycosmos.org/";
	
	private static RestTemplate restTemplate = new RestTemplate();
	
	Logger logger = LoggerFactory.getLogger(GlytoucanUtil.class);
	
	// needs to be done to initialize static variables to parse glycan sequence
   /* static {
        BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
        glycanWorkspace.initData();
    }*/
	
	static GlytoucanUtil instance;
	
	private GlytoucanUtil() {
	}
	
	public static GlytoucanUtil getInstance () {
		if (instance == null)
			instance = new GlytoucanUtil();
		return instance;
	}
	
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String registerGlycan (String wurcsSequence) {
	    
	    Sequence input = new Sequence();
	    input.setSequence(wurcsSequence);
	    
	    HttpEntity<Sequence> requestEntity = new HttpEntity<Sequence>(input, createHeaders(userId, apiKey));
		
		try {
			ResponseEntity<Response> response = restTemplate.exchange(registerURL, HttpMethod.POST, requestEntity, Response.class);
			return response.getBody().getMessage();
		} catch (HttpClientErrorException e) {
			logger.error("Client Error: Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			logger.info("Sequence: " + wurcsSequence);
		} catch (HttpServerErrorException e) {
			logger.error("Server Error: Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
			logger.info("Sequence: " + wurcsSequence);
		} catch (Exception e) {
		    logger.error("General Error: Exception adding glycan " + e.getMessage());
            logger.info("Sequence: " + wurcsSequence);
		}
		
		return null;
	}
	
	public String getAccessionNumber (String wurcsSequence) {
		String accessionNumber = null;
		
		String url;
		//try {
			url = retrieveURL + wurcsSequence;
			
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
			try {
				ResponseEntity<GlytoucanResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, GlytoucanResponse[].class);
				if (response.getBody().length == 0) {
				    logger.info ("No accession number is found! " + wurcsSequence);
				    return null;
				}
				if (response.getBody()[0].message  != null) {
				    logger.info("Error retrieving glycan " + response.getBody()[0].message);
				}
				return response.getBody()[0].id;
			} catch (HttpClientErrorException e) {
				logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			} catch (HttpServerErrorException e) {
				logger.info("Exception retrieving glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
			}
		//} catch (UnsupportedEncodingException e1) {
		//	logger.error("Could not encode wurcs sequence", e1);
		//}
		
		
		return accessionNumber;
	}
	
	/**
	 * use Glycosmos validation API to validate a glycan
	 * 
	 * @param wurcsSequence the glycan sequence in WURCS format
	 * @return error string if there is a validation error or an error during validation, null if there are no errors
	 */
	public String validateGlycan (String wurcsSequence) {
	    try {
            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("wurcs", wurcsSequence);
            
            UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(apiURL)
                    .path(validateURL)
                    .pathSegment("{wurcs}")
                    .buildAndExpand(uriVariables)
                    .encode();
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
            ResponseEntity<ValidationResponse> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, requestEntity, ValidationResponse.class);
            if (response.getBody().getM_mapTypeToReports().getErrors() != null && !response.getBody().getM_mapTypeToReports().getErrors().isEmpty()) {
                return response.getBody().getM_mapTypeToReports().getErrors().get(0).toString();
            }
            return null;
        } catch (HttpClientErrorException e) {
            logger.info("Exception validating glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
            return "Exception validating glycan " + ((HttpClientErrorException) e).getResponseBodyAsString();
        } catch (HttpServerErrorException e) {
            logger.info("Exception validating glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
            return "Exception validating glycan " + ((HttpServerErrorException) e).getResponseBodyAsString();
        } 
	}
	
	/**
	 * calls Glytoucan API to retrieve the glycan with the given accession number
	 * 
	 * @param accessionNumber the glytoucan id to search
	 * @return WURCS sequence if the glycan is found, null otherwise
	 */
	public String retrieveGlycan (String accessionNumber) {
		String sequence = null;
		
		String url = glycanURL + accessionNumber;
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
		try {
			ResponseEntity<RetrieveResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RetrieveResponse[].class);
			RetrieveResponse[] arr = response.getBody();
			if (arr.length > 0)
			    return response.getBody()[0].getWurcsLabel();
			else 
			    return null;
		} catch (HttpClientErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.info("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		
		return sequence;
	}
	
	static HttpHeaders createHeaders(String username, String password){
	   return new HttpHeaders() {{
	         String auth = username + ":" + password;
	         byte[] encodedAuth = Base64.encodeBase64( 
	            auth.getBytes(Charset.forName("US-ASCII")) );
	         String authHeader = "Basic " + new String( encodedAuth );
	         set( "Authorization", authHeader );
	         setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	      }};
	}
	
	public static Sugar getSugarFromWURCS (String wurcsSequence) throws IOException {
	    try {
            WURCSFactory wf = new WURCSFactory(wurcsSequence);
            WURCSGraph graph = wf.getGraph();

            // Exchange WURCSGraph to GlyContainer
            WURCSGraphToGlyContainer wg2gc = new WURCSGraphToGlyContainer();
            wg2gc.start(graph);
            GlyContainer t_gc = wg2gc.getGlycan();

            // Exchange GlyConatainer to Sugar
            GlyContainerToSugar t_export = new GlyContainerToSugar();
            t_export.start(t_gc);
            Sugar t_sugar = t_export.getConvertedSugar();
            return t_sugar;
	    } catch (Exception e) {
	        throw new IOException ("Cannot be converted to Sugar object. Reason: " + e.getMessage());
	    }
	}
}

class Sequence {
	String sequence;
	
	public void setSequence (String s) {
		this.sequence = s;
	}
	
	public String getSequence() {
		return sequence;
	}
}

class Response {
	String timestamp;
	String status;
	String error;
	String message;
	String path;
	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
}

class RetrieveResponse {
	String accessionNumber;
	String hashKey;
	String wurcsLabel;
	
	/**
	 * @return the accessionNumber
	 */
	@JsonProperty("AccessionNumber")
	public String getAccessionNumber() {
		return accessionNumber;
	}
	/**
	 * @param accessionNumber the accessionNumber to set
	 */
	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}
	/**
	 * @return the hashKey
	 */
	@JsonProperty("HashKey")
	public String getHashKey() {
		return hashKey;
	}
	/**
	 * @param hashKey the hashKey to set
	 */
	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}
	/**
	 * @return the wurcsLabel
	 */
	@JsonProperty("NormalizedWurcs")
	public String getWurcsLabel() {
		return wurcsLabel;
	}
	/**
	 * @param wurcsLabel the wurcsLabel to set
	 */
	public void setWurcsLabel(String wurcsLabel) {
		this.wurcsLabel = wurcsLabel;
	}	  
}

class GlytoucanResponse {
	String id;
	String wurcs;
	String message; // in case of error
	
	@JsonProperty("GlyTouCan")
	public String getId() {
		return id;
	}
	
	@JsonProperty("WURCS")
	public String getWurcs() {
		return wurcs;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setWurcs(String wurcs) {
		this.wurcs = wurcs;
	}
	
	public String getMessage() {
        return message;
    }
	
	public void setMessage(String message) {
        this.message = message;
    }
}

class ValidationResponse {
    String m_sInputString;
    ValidationResult m_mapTypeToReports;
    String m_sStandardString;
    /**
     * @return the m_sInputString
     */
    public String getM_sInputString() {
        return m_sInputString;
    }
    /**
     * @param m_sInputString the m_sInputString to set
     */
    public void setM_sInputString(String m_sInputString) {
        this.m_sInputString = m_sInputString;
    }
    /**
     * @return the m_mapTypeToReports
     */
    public ValidationResult getM_mapTypeToReports() {
        return m_mapTypeToReports;
    }
    /**
     * @param m_mapTypeToReports the m_mapTypeToReports to set
     */
    public void setM_mapTypeToReports(ValidationResult m_mapTypeToReports) {
        this.m_mapTypeToReports = m_mapTypeToReports;
    }
    /**
     * @return the m_sStandardString
     */
    public String getM_sStandardString() {
        return m_sStandardString;
    }
    /**
     * @param m_sStandardString the m_sStandardString to set
     */
    public void setM_sStandardString(String m_sStandardString) {
        this.m_sStandardString = m_sStandardString;
    }
    
}

class ValidationResult {
    
    @JsonProperty ("ERROR")
    List<ValidationError> errors;

    /**
     * @return the errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }
}

class ValidationError {
    String strMessage;
    ExceptionMessage exception;

    /**
     * @return the strMessage
     */
    public String getStrMessage() {
        return strMessage;
    }

    /**
     * @param strMessage the strMessage to set
     */
    public void setStrMessage(String strMessage) {
        this.strMessage = strMessage;
    }
    
    @Override
    public String toString() {
        return strMessage + " Exception: " + (exception == null ? "" :exception.toString()); 
    }

    /**
     * @return the exception
     */
    public ExceptionMessage getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(ExceptionMessage exception) {
        this.exception = exception;
    }
}

class ExceptionMessage {
    String m_strInput;
    String m_strMessage;
    
    @Override
    public String toString() {
        return m_strInput + " : " + m_strMessage;
    }

    /**
     * @return the m_strInput
     */
    public String getM_strInput() {
        return m_strInput;
    }

    /**
     * @param m_strInput the m_strInput to set
     */
    public void setM_strInput(String m_strInput) {
        this.m_strInput = m_strInput;
    }

    /**
     * @return the m_strMessage
     */
    public String getM_strMessage() {
        return m_strMessage;
    }

    /**
     * @param m_strMessage the m_strMessage to set
     */
    public void setM_strMessage(String m_strMessage) {
        this.m_strMessage = m_strMessage;
    }
}
