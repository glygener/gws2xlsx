package org.glygen.gws2xlsx.rectify;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;

public class RectifyGlycoCTApp
{
	static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());

	public static void main(String[] args) throws SugarImporterException, GlycoVisitorException 
	{
		String t_gws = "redEnd--??1L-Fuc,p(--??1D-GlcA,p)--??1D-GlcNAc,p$MONO,perMe,Na,0,redEnd";
//		t_gws = "redEnd--??1L-Rha,p(--??1D-GlcNAc,p)--??1D-GlcA,p$MONO,perMe,Na,0,redEnd";
		// UND issues
		t_gws = "freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p(--3a1D-Man,p(--??1D-GlcNAc,p--??1D-Gal,p)--??1D-GlcNAc,p--??1D-Gal,p)--6a1D-Man,p(--??1D-GlcNAc,p--??1D-Gal,p)--??1D-GlcNAc,p--??1D-Gal,p}(--??2D-NeuAc,p)--??1L-Fuc,p$MONO,perMe,Na,0,freeEnd";
		t_gws = "redEnd--?a1D-GalNAc,p--3b1D-Gal,p}--??2D-NeuAc,p$MONO,perMe,Na,0,redEnd";
//		t_gws = "freeEnd--??1D-GlcNAc,p--?[--??1D-GlcNAc,p--?]--??1D-GlcNAc,p}--??2D-NeuAc,p$MONO,perMe,Na,0,freeEnd";
		Glycan t_glycan = Glycan.fromString(t_gws);
		System.out.println(t_glycan.toString());
		String t_glycoCT = t_glycan.toGlycoCTCondensed();
		System.out.println(t_glycoCT);

		RectifyGlycoCT t_rectifier = new RectifyGlycoCT();
		String t_glycoCTNew = t_rectifier.rectify(t_glycoCT);

		System.out.println("---------------");
		System.out.println(t_glycoCTNew);
	}
}
