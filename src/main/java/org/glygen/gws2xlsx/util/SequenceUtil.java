package org.glygen.gws2xlsx.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.validation.GlycoVisitorValidation;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.WURCSFramework.io.GlycoCT.GlycoVisitorValidationForWURCS;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glygen.gws2xlsx.model.GlycanObject;
import org.glygen.gws2xlsx.rectify.RectifyGlycoCT;

public class SequenceUtil {
    
    static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
    static {       
            glycanWorkspace.initData();
            // Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
            glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
            // Set flag to show information such as linkage positions and anomers
            glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
            // Set flag to show mass
            glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
            // Set flag to show reducing end
            glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

            glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
            glycanWorkspace.setNotation(GraphicOptions.NOTATION_SNFG);
    }
    
    public static void parseGWSIntoWURCS (GlycanObject glycan) throws Exception  {
        String wurcsSequence = null;
        FixGlycoCtUtil fixGlycoCT = new FixGlycoCtUtil();
        try {
            Glycan glycanObject = Glycan.fromString(glycan.getGwsSequence().trim());
            String glycoCT = glycanObject.toGlycoCTCondensed();
            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
            RectifyGlycoCT t_rectifier = new RectifyGlycoCT();
            glycoCT = t_rectifier.rectify(glycoCT);
            glycan.setGlycoCT(glycoCT);
            try {
                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                exporter.start(glycoCT);
                wurcsSequence = exporter.getWURCS();
                glycan.setWurcs(wurcsSequence);
            } catch (Exception e) {
                // run the validator to get the detailed error messages
                String errorMessage = validGlycoCT(glycoCT);
                if (!errorMessage.isEmpty()) {
                    throw new WURCSException("Error converting " + glycoCT + " to WURCS, validation errors: " + errorMessage);
                }
            }
        } catch (GlycoVisitorException e) {
            throw e;
        } 
    }
    
    public static byte[] getCartoon (String gwsSequence) throws IOException {
        BufferedImage t_image = null;
        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = Glycan.fromString(gwsSequence.trim());
        if (glycanObject != null) {
            t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
        }
        if (t_image != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(t_image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        }
        return null;
    }
    
    private static String validGlycoCT(String glycoCT)
            throws SugarImporterException, GlycoVisitorException
    {
        StringBuffer errorMessage = new StringBuffer();
        // parse the GlycoCT and create sugar object
        SugarImporterGlycoCTCondensed t_importerGlycoCT = new SugarImporterGlycoCTCondensed();
        Sugar t_sugar = t_importerGlycoCT.parse(glycoCT);

        // Validate sugar with GlycoCT validator
        GlycoVisitorValidation t_validation = new GlycoVisitorValidation();
        t_validation.start(t_sugar);
        
        // Remove error "Sugar has more than one root residue."
        for (String t_string : t_validation.getErrors()) {
            if (!t_string.equals("Sugar has more than one root residue.")) {
                errorMessage.append(t_string + "\n");
            }
        }
        
        // Validate for WURCS
        GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
        t_validationWURCS.start(t_sugar);
        for (String error: t_validationWURCS.getErrors()) {
            errorMessage.append(error + "\n");
           
        }
        return errorMessage.toString();
    }
}
