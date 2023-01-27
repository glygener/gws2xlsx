package org.glygen.gws2xlsx.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;

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
    
    public static String parseGWSIntoWURCS (String sequence) throws Exception  {
        String wurcsSequence = null;
        FixGlycoCtUtil fixGlycoCT = new FixGlycoCtUtil();
        try {
            Glycan glycanObject = Glycan.fromString(sequence.trim());
            String glycoCT = glycanObject.toGlycoCTCondensed();
            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
            try {
                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                exporter.start(glycoCT);
                wurcsSequence = exporter.getWURCS();
            } catch (Exception e) {
                throw new WURCSException("Error converting " + glycoCT + " to WURCS. Reason: " + e.getMessage());
            }
        } catch (GlycoVisitorException e) {
            throw e;
        } 
            
        return wurcsSequence;
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
}
