package org.glygen.gws2xlsx;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
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
    
    private static final Double IMAGE_CELL_WIDTH_FACTOR = 36.55D;
    private static final Double IMAGE_CELL_HEIGHT_FACTOR = 0.76D;

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
                    if (cartoonGeneration) {
                        try {    
                            glycan.setCartoon(SequenceUtil.getCartoon(glycan.getGwsSequence()));
                        } catch (Exception e) {
                            System.out.println ("Cannot generate image for " + glycan.getRowNumber());
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
     // headline font
        Font fontHeadline = workbook.createFont();
        fontHeadline.setBold(true);
        fontHeadline.setFontHeightInPoints((short) 12);
        // headline style
        CellStyle cellStyleHeadline = workbook.createCellStyle();
        cellStyleHeadline.setFillBackgroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        cellStyleHeadline.setFillPattern(FillPatternType.FINE_DOTS);
        cellStyleHeadline.setFont(fontHeadline);
        // text horizontal and veritcal middle aligned
        CellStyle cellStyleTextMiddle = workbook.createCellStyle();
        cellStyleTextMiddle.setAlignment(HorizontalAlignment.CENTER);
        cellStyleTextMiddle.setVerticalAlignment(VerticalAlignment.CENTER);
        Sheet sheet = workbook.createSheet("Glycans");
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        Row header = sheet.createRow(0);
        Cell cell1 = header.createCell(0, CellType.STRING);
        cell1.setCellValue("File name");
        cell1.setCellStyle(cellStyleHeadline);
        Cell cell2 = header.createCell(1, CellType.NUMERIC);
        cell2.setCellValue("Row number in file");
        cell2.setCellStyle(cellStyleHeadline);
        Cell cell3 = header.createCell(2, CellType.STRING);
        cell3.setCellValue("Glytoucan ID");
        cell3.setCellStyle(cellStyleHeadline);
        Cell cell4 = header.createCell(3, CellType.BLANK);
        cell4.setCellValue("Cartoon");
        cell4.setCellStyle(cellStyleHeadline);
        Cell cell5 = header.createCell(4, CellType.STRING);
        cell5.setCellValue("Status");
        cell5.setCellStyle(cellStyleHeadline);
        Cell cell6 = header.createCell(5, CellType.STRING);
        cell6.setCellValue("Error");
        cell6.setCellStyle(cellStyleHeadline);
        
        int maxImageWidth = determineMaxColumnWidth (processed);
        sheet.setColumnWidth(3, (int) (maxImageWidth * IMAGE_CELL_WIDTH_FACTOR));
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
                cell.setCellStyle(cellStyleTextMiddle);
                cell = r.createCell(3, CellType.BLANK);
                if (glycan.getCartoon() != null) {
                    // convert byte[] back to a BufferedImage
                    InputStream is = new ByteArrayInputStream(glycan.getCartoon());
                    BufferedImage newBi = ImageIO.read(is);
                    r.setHeightInPoints((int) (newBi.getHeight() * IMAGE_CELL_HEIGHT_FACTOR) + 1);
                    addCartoon (drawing, workbook, cell, glycan.getCartoon());
                }
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
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        
        String outputFile = outputFolder + File.separator + "Glycans.xlsx";
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        workbook.close();
    }

    private int determineMaxColumnWidth(JobObject processed) throws IOException {
        Integer maxImageWidth = 100;
        for (InputFile file: processed.getFiles()) {
            for (GlycanObject glycan: file.getGlycans()) {
             // convert byte[] back to a BufferedImage
                InputStream is = new ByteArrayInputStream(glycan.getCartoon());
                BufferedImage newBi = ImageIO.read(is);
                if (newBi.getWidth() > maxImageWidth) {
                    maxImageWidth = newBi.getWidth();
                }
            }
        }
        return maxImageWidth;
    }

    private void addCartoon(Drawing drawing, Workbook workbook, Cell cell, byte[] cartoon) {
        CreationHelper m_creationHelper = workbook.getCreationHelper();
        int t_pictureIndex = workbook.addPicture(cartoon,
                Workbook.PICTURE_TYPE_PNG);
        // add a picture shape
        ClientAnchor t_anchor = m_creationHelper.createClientAnchor();
        // set top-left corner of the picture,
        // subsequent call of Picture#resize() will operate relative to it
        t_anchor.setCol1(cell.getColumnIndex());
        t_anchor.setRow1(cell.getRowIndex());
        Picture t_picture = drawing.createPicture(t_anchor, t_pictureIndex);
        // auto-size picture relative to its top-left corner
        t_picture.resize();   
    }

    public void saveJob(JobObject job, String jobFile) throws JsonProcessingException, FileNotFoundException {
        ObjectMapper mapper = new ObjectMapper();         
        String json = mapper.writeValueAsString(job);
        PrintWriter out = new PrintWriter(new File (jobFile));
        out.append(json);
        out.close();
    }
}
