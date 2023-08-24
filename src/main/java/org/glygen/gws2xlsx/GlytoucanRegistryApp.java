package org.glygen.gws2xlsx;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.apache.poi.util.Units;
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

    public InputFile processSingleFile (String gwsFile, Boolean glytoucanGeneration, Boolean cartoonGeneration, Boolean debug) throws Exception {
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
                   SequenceUtil.parseGWSIntoWURCS(glycan);
                   if (glycan.getWurcs() != null) {
                       try {
                           // validate first to get errors if any
                           String errors = GlytoucanUtil.getInstance().validateGlycan(glycan.getWurcs());
                           if (errors != null) {
                               glycan.setError("Validation errors: " + errors);
                               if (!debug) glycan.setStatus(RegistrationStatus.ERROR);
                               glycans.add(glycan);
                               count++;
                               continue;
                           }
                           if (!debug) {
                               String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getWurcs());
                               if (glytoucanId == null) {
                                   glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                                   glytoucanId = GlytoucanUtil.getInstance().registerGlycan(glycan.getWurcs());
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
                           } 
                       } catch (Exception e) {
                           glycan.setError("Error getting accession number: " + e.getMessage());
                           if (!debug) glycan.setStatus(RegistrationStatus.ERROR);
                       }
                   } else {
                       glycan.setError("Could not convert to WURCS");
                       if (!debug) glycan.setStatus(RegistrationStatus.ERROR);
                   }
               }
               if (cartoonGeneration) {
                   try {    
                       glycan.setCartoon(SequenceUtil.getCartoon(sequence));
                       if (glycan.getGlytoucanID() != null)
                           glycan.setGlytoucanImage(GlytoucanUtil.getInstance().getGlytoucanImage(glycan.getGlytoucanID()));
                   } catch (Exception e) {
                       System.out.println ("Cannot generate image for " + (count+1));
                   } 
               }
           } catch (Exception e) {
               glycan.setError(e.getMessage());
               if (!debug) glycan.setStatus(RegistrationStatus.ERROR);
               e.printStackTrace();
           }
           count++;
           glycans.add(glycan);
       }
       input.close();
       
       processed.setGlycans(glycans);
       return processed;
    }
    
    public JobObject processInputFolder (String inputFolder, Boolean glytoucanGeneration, Boolean cartoonGeneration, Boolean debug) {
        JobObject job = new JobObject ();
        
        File folder = new File (inputFolder);
        if (folder.exists() && folder.isDirectory()) {
            for (File f: folder.listFiles()) {
                try {
                    if (!f.getName().endsWith("gws")) {
                        System.out.println ("skipping file " + f.getName());
                        continue;
                    }
                    InputFile processed = processSingleFile(f.getAbsolutePath(), glytoucanGeneration, cartoonGeneration, debug);
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
    
    public JobObject processJobFile (String jobFile, Boolean glytoucanGeneration, Boolean cartoonGeneration, Boolean rerun) throws FileNotFoundException, JsonMappingException, JsonProcessingException {
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
                        if (rerun) {
                            System.out.println("Rerun");
                            String existingWurcs = glycan.getWurcs();
                            SequenceUtil.parseGWSIntoWURCS(glycan);
                            String wurcs = glycan.getWurcs();
                            if (existingWurcs != null && !existingWurcs.equals(wurcs)) {
                                System.out.println("WURCS has changed for row " + glycan.getRowNumber());
                                // even if there is a glytoucan id, retrieve it again
                                String errors = GlytoucanUtil.getInstance().validateGlycan(glycan.getWurcs());
                                if (errors != null) {
                                    glycan.setError("Validation errors: " + errors);
                                    glycan.setStatus(RegistrationStatus.ERROR);
                                    continue;
                                }
                                String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getWurcs());
                                if (glytoucanId == null) {
                                    glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                                    glytoucanId = GlytoucanUtil.getInstance().registerGlycan(glycan.getWurcs());
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
                            } 
                        }
                        if (glycan.getGlytoucanID() == null || glycan.getStatus() == RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION) {
                            if (glycan.getWurcs() == null) {
                                SequenceUtil.parseGWSIntoWURCS(glycan);
                            }
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
                                } else {
                                    if (glycan.getStatus() != RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION) {
                                        glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                                        glytoucanId = GlytoucanUtil.getInstance().registerGlycan(glycan.getWurcs());
                                        if (glytoucanId == null || glytoucanId.length() > 10) {
                                            glycan.setStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION);
                                            glycan.setGlytoucanHash(glytoucanId);
                                        } else {
                                            glycan.setGlytoucanID(glytoucanId);
                                        }
                                        //reset any errors
                                        glycan.setError(null);
                                    }
                                }
                            } 
                        }
                    }
                    if (cartoonGeneration) {
                        try {    
                            glycan.setCartoon(SequenceUtil.getCartoon(glycan.getGwsSequence()));
                            if (glycan.getGlytoucanID() != null)
                                glycan.setGlytoucanImage(GlytoucanUtil.getInstance().getGlytoucanImage(glycan.getGlytoucanID()));
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
    
    public void writeIntoExcel (JobObject processed, String outputFolder, Boolean debug) throws IOException {
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
        int columnCount = 7;
        Row header = sheet.createRow(0);
        Cell cell = header.createCell(0, CellType.STRING);
        cell.setCellValue("File name");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(1, CellType.NUMERIC);
        cell.setCellValue("Row number in file");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(2, CellType.STRING);
        cell.setCellValue("Glytoucan ID");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(3, CellType.BLANK);
        cell.setCellValue("Cartoon");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(4, CellType.BLANK);
        cell.setCellValue("Glytoucan Image");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(5, CellType.STRING);
        cell.setCellValue("Status");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(6, CellType.STRING);
        cell.setCellValue("Error");
        cell.setCellStyle(cellStyleHeadline);
        if (debug) {
            cell = header.createCell(7, CellType.STRING);
            cell.setCellValue("GlycoCT");
            cell.setCellStyle(cellStyleHeadline);
            cell = header.createCell(8, CellType.STRING);
            cell.setCellValue("WURCS");
            cell.setCellStyle(cellStyleHeadline);
            columnCount = 9;
        } 
        
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Evidence");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Species");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Strain");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Tissue");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Cell line ID");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Disease");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Glycan dictionary term ID");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("has_abundance");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("has_expression");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Functional annotation/Keyword");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Experimental technique");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Variant (Fly, yeast, mouse)");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Organismal/cellular Phenotype");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Molecular Phenotype");
        cell.setCellStyle(cellStyleHeadline);
        cell = header.createCell(columnCount++, CellType.STRING);
        cell.setCellValue("Contributor");
        cell.setCellStyle(cellStyleHeadline);
        
        
        int maxImageWidth = determineMaxColumnWidth (processed, false);
        int maxImageWidth2 = determineMaxColumnWidth (processed, true);
        sheet.setColumnWidth(3, (int) (maxImageWidth * IMAGE_CELL_WIDTH_FACTOR));
        sheet.setColumnWidth(4, (int) (maxImageWidth2 * IMAGE_CELL_WIDTH_FACTOR));
        int row = 1;
        for (InputFile file: processed.getFiles()) {
            String filename = file.getFilename().substring(file.getFilename().lastIndexOf(File.separator)+1);
            for (GlycanObject glycan: file.getGlycans()) {
                columnCount = 7;
                Row r = sheet.createRow(row++);
                cell = r.createCell(0, CellType.STRING);
                cell.setCellValue(filename);
                cell = r.createCell(1, CellType.NUMERIC);
                cell.setCellValue(glycan.getRowNumber());
                cell = r.createCell(2, CellType.STRING);
                cell.setCellValue(glycan.getGlytoucanID());
                cell.setCellStyle(cellStyleTextMiddle);
                cell = r.createCell(3, CellType.BLANK);
                Float rowHeightinPixels = null;
                if (glycan.getCartoon() != null) {
                    // convert byte[] back to a BufferedImage
                    InputStream is = new ByteArrayInputStream(glycan.getCartoon());
                    BufferedImage newBi = ImageIO.read(is);
                    r.setHeightInPoints((int) (newBi.getHeight() * IMAGE_CELL_HEIGHT_FACTOR));
                    rowHeightinPixels = r.getHeightInPoints() * Units.PIXEL_DPI / Units.POINT_DPI;
                    addCartoon (drawing, workbook, cell, glycan.getCartoon(), sheet.getColumnWidthInPixels(3), rowHeightinPixels);
                }
                cell = r.createCell(4, CellType.BLANK);
                if (glycan.getGlytoucanImage() != null) {
                    InputStream is = new ByteArrayInputStream(glycan.getGlytoucanImage());
                    BufferedImage newBi = ImageIO.read(is);
                    r.setHeightInPoints((int) (newBi.getHeight() * IMAGE_CELL_HEIGHT_FACTOR));
                    float rowHeight = rowHeightinPixels != null ? rowHeightinPixels : r.getHeightInPoints() * Units.PIXEL_DPI / Units.POINT_DPI;
                    addCartoon (drawing, workbook, cell, glycan.getGlytoucanImage(), sheet.getColumnWidthInPixels(3), rowHeight);
                }
                cell = r.createCell(5, CellType.STRING);
                if (glycan.getStatus() == RegistrationStatus.NONE) {
                    cell.setCellValue("");
                } else {
                    cell.setCellValue(glycan.getStatus().name());
                }
                cell = r.createCell(6, CellType.STRING);
                cell.setCellValue(glycan.getError());
                if (debug) {
                    cell = r.createCell(7, CellType.STRING);
                    cell.setCellValue(glycan.getGlycoCT());
                    cell = r.createCell(8, CellType.STRING);
                    cell.setCellValue(glycan.getWurcs());
                    columnCount = 9;
                }
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell.setCellValue("no");
                cell = r.createCell(columnCount++, CellType.STRING);
                cell.setCellValue("no");
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell = r.createCell(columnCount++, CellType.STRING);
                cell.setCellValue("createdBy:Mindy Porterfield(mindyp@ccrc.uga.edu, CCRC)|createdBy:Anh Nguyen(apnguyen@gwu.edu,The George Washington University)");
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        
        String outputFile = outputFolder + File.separator + "Glycans-" + new SimpleDateFormat("yyyyMMdd'T'hh_mm").format(new Date()) + ".xlsx";
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        workbook.close();
    }

    private int determineMaxColumnWidth(JobObject processed, boolean glytoucanImage) throws IOException {
        Integer maxImageWidth = 0;
        for (InputFile file: processed.getFiles()) {
            for (GlycanObject glycan: file.getGlycans()) {
                if (glytoucanImage && glycan.getGlytoucanImage() != null) {
                    InputStream is = new ByteArrayInputStream(glycan.getGlytoucanImage());
                    BufferedImage newBi = ImageIO.read(is);
                    if (newBi.getWidth() > maxImageWidth) {
                        maxImageWidth = newBi.getWidth();
                    }
                }
             // convert byte[] back to a BufferedImage
                else if (glycan.getCartoon() != null) {
                    InputStream is = new ByteArrayInputStream(glycan.getCartoon());
                    BufferedImage newBi = ImageIO.read(is);
                    if (newBi.getWidth() > maxImageWidth) {
                        maxImageWidth = newBi.getWidth();
                    }
                }
            }
        }
        return maxImageWidth;
    }

    private void addCartoon(Drawing<?> drawing, Workbook workbook, Cell cell, byte[] cartoon, float columnWidthInPixel, float rowHeightinPixels) {
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
        
        int pictWidthPx = t_picture.getImageDimension().width;
        //get the picture height in px
        int pictHeightPx = t_picture.getImageDimension().height;
        
        
        //calculate scale
        float scale = 1;
        if (pictHeightPx > rowHeightinPixels) {
            float tmpscale = rowHeightinPixels / (float)pictHeightPx;
            if (tmpscale < scale) scale = tmpscale;
        }
        if (pictWidthPx > columnWidthInPixel) {
            float tmpscale = columnWidthInPixel / (float)pictWidthPx;
            if (tmpscale < scale) scale = tmpscale;
        }
        
        if (scale < 1) {
            t_picture.resize(scale);
        }
    }

    public void saveJob(JobObject job, String jobFile) throws JsonProcessingException, FileNotFoundException {
        ObjectMapper mapper = new ObjectMapper();         
        String json = mapper.writeValueAsString(job);
        PrintWriter out = new PrintWriter(new File (jobFile));
        out.append(json);
        out.close();
    }
}
