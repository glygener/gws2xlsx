package org.glygen.gws2xlsx.rectify;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.NonMonosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Substituent;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitAlternative;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitCyclic;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitRepeat;
import org.eurocarbdb.MolecularFramework.sugar.UnderdeterminedSubTree;
import org.eurocarbdb.MolecularFramework.sugar.UnvalidatedGlycoNode;
import org.eurocarbdb.MolecularFramework.util.traverser.GlycoTraverser;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitor;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;

public class GlycoVisitorRectifyUNDparents implements GlycoVisitor
{
	private List<GlycoNode> m_allParents = new ArrayList<>();
	
	@Override
	public void clear() 
	{
		this.m_allParents.clear();
	}

	@Override
	public GlycoTraverser getTraverser(GlycoVisitor arg0) throws GlycoVisitorException 
	{
		// nothing to do
		return null;
	}

	@Override
	public void start(Sugar a_sugar) throws GlycoVisitorException 
	{
		List<UnderdeterminedSubTree> t_und = a_sugar.getUndeterminedSubTrees();
		if ( t_und.size() > 0 )
		{
			try
			{
				ArrayList<GlycoNode> t_rootNodes = a_sugar.getNodes();
				for (GlycoNode t_glycoNode : t_rootNodes) 
				{
					t_glycoNode.accept(this);
				}
			}
			catch (Exception e) 
			{
				throw new GlycoVisitorException("Error in sugar object : " + e.getMessage(),e);
			}
		}
		for (UnderdeterminedSubTree t_underdeterminedSubTree : t_und) 
		{
			List<GlycoNode> t_parents = t_underdeterminedSubTree.getParents();
			t_parents.clear();
			t_parents.addAll(this.m_allParents);
		}
	}

	@Override
	public void visit(Monosaccharide a_ms) throws GlycoVisitorException 
	{
		this.m_allParents.add(a_ms);
	}

	@Override
	public void visit(NonMonosaccharide arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyUNDparents does not support NonMonosaccharide residues"); 
	}

	@Override
	public void visit(SugarUnitRepeat arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyUNDparents does not support SugarUnitRepeat residues");
	}

	@Override
	public void visit(Substituent arg0) throws GlycoVisitorException 
	{
		// nothing to do
	}

	@Override
	public void visit(SugarUnitCyclic arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyUNDparents does not support SugarUnitCyclic residues");
	}

	@Override
	public void visit(SugarUnitAlternative arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyUNDparents does not support SugarUnitAlternative residues");
	}

	@Override
	public void visit(UnvalidatedGlycoNode arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyUNDparents does not support UnvalidatedGlycoNode residues");
	}

	@Override
	public void visit(GlycoEdge arg0) throws GlycoVisitorException 
	{
		// nothing to do
	}
}
