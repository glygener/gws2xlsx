package org.glygen.gws2xlsx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glygen.gws2xlsx.model.GlycanObject;
import org.glygen.gws2xlsx.model.InputFile;
import org.glygen.gws2xlsx.model.JobObject;
import org.glygen.gws2xlsx.model.RegistrationStatus;
import org.glygen.gws2xlsx.util.GlytoucanUtil;
import org.glygen.gws2xlsx.util.SequenceUtil;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlytoucanRegistryApp {
    
    public InputFile processSingleFile (String gwsFile, Boolean glytoucanGeneration, Boolean cartoonGeneration) throws Exception {
       InputFile processed = new InputFile();
       processed.setFilename(gwsFile);
       List<GlycanObject> glycans = new ArrayList<>();
       
       Scanner input = new Scanner(new File(gwsFile));
       String line = input.nextLine ();
       String[] structures = line.split(";");
       int count = 0;
       for (String sequence: structures) {
           GlycanObject glycan = new GlycanObject();
           glycan.setGwsSequence(sequence);
           glycan.setRowNumber(count+1);
           try {
               if (glytoucanGeneration) {
                   String wurcs = SequenceUtil.parseGWSIntoWURCS(sequence);
                   glycan.setWurcs(wurcs);
                   if (wurcs != null) {
                       try {
                           // validate first to get errors if any
                           String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
                           if (errors != null) {
                               glycan.setError("Validation errors: " + errors);
                               glycan.setStatus(RegistrationStatus.ERROR);
                               glycans.add(glycan);
                               count++;
                               continue;
                           }
                           String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
                           if (glytoucanId == null) {
                               glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                               glytoucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
                               if (glytoucanId == null || glytoucanId.length() > 10) {
                                   glycan.setStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION);
                                   glycan.setGlytoucanHash(glytoucanId);
                               } else {
                                   glycan.setGlytoucanID(glytoucanId);
                               }
                           } else {
                               glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
                               glycan.setGlytoucanID(glytoucanId);
                           }
                           
                       } catch (Exception e) {
                           glycan.setError("Error getting accession number: " + e.getMessage());
                           glycan.setStatus(RegistrationStatus.ERROR);
                       }
                   } else {
                       glycan.setError("Could not convert to WURCS");
                       glycan.setStatus(RegistrationStatus.ERROR);
                   }
               }
               if (cartoonGeneration) {
                   try {    
                       glycan.setCartoon(SequenceUtil.getCartoon(sequence));
                   } catch (Exception e) {
                       System.out.println ("Cannot generate image for " + (count+1));
                   } 
               }
           } catch (Exception e) {
               glycan.setError(e.getMessage());
               e.printStackTrace();
           }
           count++;
           glycans.add(glycan);
       }
       input.close();
       
       processed.setGlycans(glycans);
       return processed;
    }
    
    public JobObject processInputFolder (String inputFolder, Boolean glytoucanGeneration, Boolean cartoonGeneration) {
        JobObject job = new JobObject ();
        
        File folder = new File (inputFolder);
        if (folder.exists() && folder.isDirectory()) {
            for (File f: folder.listFiles()) {
                try {
                    InputFile processed = processSingleFile(f.getAbsolutePath(), glytoucanGeneration, cartoonGeneration);
                    if (processed != null) {
                        job.addFile(processed);
                    }
                } catch (Exception e) {
                    System.err.println ("Error processing " + f.getName() + " Exception: " + e.getMessage());
                }
            }
        }
        return job;
    }
    
    public JobObject processJobFile (String jobFile, Boolean glytoucanGeneration, Boolean cartoonGeneration) throws FileNotFoundException, JsonMappingException, JsonProcessingException {
        // read json object from file
        Scanner scanner = new Scanner (new File(jobFile));
        String json = scanner.nextLine();
        JSONObject jo = new JSONObject(json);
        ObjectMapper objectMapper = new ObjectMapper();
        JobObject job = objectMapper.readValue(jo.toString(), JobObject.class);
        scanner.close();
        
        for (InputFile file: job.getFiles()) {
            for (GlycanObject glycan: file.getGlycans()) {
                try {
                    if (glytoucanGeneration) {
                        if (glycan.getGlytoucanID() == null || glycan.getStatus() == RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION) {
                            if (glycan.getWurcs() != null) {
                                String errors = GlytoucanUtil.getInstance().validateGlycan(glycan.getWurcs());
                                if (errors != null) {
                                    glycan.setError("Validation errors: " + errors);
                                    glycan.setStatus(RegistrationStatus.ERROR);
                                    continue;
                                }
                                String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getWurcs());
                                if (glytoucanId != null && glytoucanId.length() <= 10) {
                                    glycan.setGlytoucanID(glytoucanId);
                                    glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    glycan.setStatus(RegistrationStatus.ERROR);
                    glycan.setError("Could not get glytoucanId (previously registered) : " + e.getMessage());
                }
            }
        }
        
        return job;
    }
    
    public void writeIntoExcel (JobObject processed, String outputFolder) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Glycans");
        Row header = sheet.createRow(0);
        Cell cell1 = header.createCell(0, CellType.STRING);
        cell1.setCellValue("File name");
        Cell cell2 = header.createCell(1, CellType.NUMERIC);
        cell2.setCellValue("Row number in file");
        Cell cell3 = header.createCell(2, CellType.STRING);
        cell3.setCellValue("Glytoucan ID");
        Cell cell4 = header.createCell(3, CellType.BLANK);
        cell4.setCellValue("Cartoon");
        Cell cell5 = header.createCell(4, CellType.STRING);
        cell5.setCellValue("Status");
        Cell cell6 = header.createCell(5, CellType.STRING);
        cell6.setCellValue("Error");
        
        int row = 1;
        Cell cell = null;
        for (InputFile file: processed.getFiles()) {
            for (GlycanObject glycan: file.getGlycans()) {
                Row r = sheet.createRow(row++);
                cell = r.createCell(0, CellType.STRING);
                cell.setCellValue(file.getFilename());
                cell = r.createCell(1, CellType.NUMERIC);
                cell.setCellValue(glycan.getRowNumber());
                cell = r.createCell(2, CellType.STRING);
                cell.setCellValue(glycan.getGlytoucanID());
                cell = r.createCell(3, CellType.BLANK);
                addCartoon (cell, glycan.getCartoon());
                cell = r.createCell(4, CellType.STRING);
                if (glycan.getStatus() == RegistrationStatus.NONE) {
                    cell.setCellValue("");
                } else {
                    cell.setCellValue(glycan.getStatus().name());
                }
                cell = r.createCell(5, CellType.STRING);
                cell.setCellValue(glycan.getError());
            }
        }
        
        String outputFile = outputFolder + File.separator + "Glycans.xlsx";
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        workbook.close();
    }

    private void addCartoon(Cell cell, byte[] cartoon) {
        // TODO Auto-generated method stub
        
    }

    public void saveJob(JobObject job, String jobFile) throws JsonProcessingException, FileNotFoundException {
        ObjectMapper mapper = new ObjectMapper();         
        String json = mapper.writeValueAsString(job);
        PrintWriter out = new PrintWriter(new File (jobFile));
        out.append(json);
        out.close();
    }
}
