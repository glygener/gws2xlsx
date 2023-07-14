package org.glygen.gws2xlsx.rectify;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;

public class RectifyGlycoCT 
{
	public String rectify(String a_glycoCT) throws SugarImporterException, GlycoVisitorException
	{
		// parse GlycoCT to Sugar
		SugarImporterGlycoCTCondensed t_importer = new SugarImporterGlycoCTCondensed();
		Sugar t_sugar = t_importer.parse(a_glycoCT);
		
		// fix the issues
		GlycoVisitorRectifyFuc1d t_visitorFuc = new GlycoVisitorRectifyFuc1d();
		t_visitorFuc.start(t_sugar);
		
		GlycoVisitorRectifyUNDparents t_visitorUND = new GlycoVisitorRectifyUNDparents();
		t_visitorUND.start(t_sugar);
		
		// export back to GlycoCT
		SugarExporterGlycoCTCondensed t_exporter = new SugarExporterGlycoCTCondensed();
		t_exporter.start(t_sugar);
		String t_glycoCTNew = t_exporter.getHashCode();
		
		return t_glycoCTNew;
	}
}
