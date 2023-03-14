package org.glygen.gws2xlsx.rectify;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.MolecularFramework.sugar.BaseType;
import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.Modification;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.NonMonosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Substituent;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitAlternative;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitCyclic;
import org.eurocarbdb.MolecularFramework.sugar.SugarUnitRepeat;
import org.eurocarbdb.MolecularFramework.sugar.UnvalidatedGlycoNode;
import org.eurocarbdb.MolecularFramework.util.traverser.GlycoTraverser;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitor;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;

public class GlycoVisitorRectifyFuc1d implements GlycoVisitor
{
	@Override
	public void clear() 
	{
		// nothing to do
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
		try
		{
			ArrayList<GlycoNode> t_rootNodes = a_sugar.getRootNodes();
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

	@Override
	public void visit(Monosaccharide a_ms) throws GlycoVisitorException 
	{
		try
		{
			boolean t_fixDGal = false;
			// check if its a Fuc
			List<BaseType> t_baseTypes = a_ms.getBaseType();
			if (t_baseTypes.size() != 1 )
			{
				return;
			}
			for (BaseType t_baseType : t_baseTypes) 
			{
				if ( t_baseType.equals(BaseType.DGAL)) 
				{
					List<Modification> t_modifications = a_ms.getModification();
					Modification t_modification1D = null;
					for (Modification t_modification : t_modifications) 
					{
						if ( this.is1d(t_modification) )
						{
							t_modification1D = t_modification;
						}
					}
					if ( t_modification1D != null )
					{
						// remove 1d
						t_modifications.remove(t_modification1D);
						// add 6d
						Modification t_modification6d = new Modification(ModificationType.DEOXY, 6);
						t_modifications.add(t_modification6d);
						t_fixDGal = true;
					}
				}
			}
			if ( t_fixDGal )
			{
				t_baseTypes.clear();
				t_baseTypes.add(BaseType.LGAL);
			}
		}
		catch (Exception e) 
		{
			throw new GlycoVisitorException("Error processing monosaccharides: " + e.getMessage(),e);
		}
	}

	private boolean is1d(Modification a_modification) 
	{
		if ( a_modification.getModificationType().equals(ModificationType.DEOXY) )
		{
			if ( a_modification.getPositionOne() == 1)
			{
				if ( a_modification.getPositionTwo() == null )
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void visit(NonMonosaccharide arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyFuc1d does not support NonMonosaccharide residues"); 
	}

	@Override
	public void visit(SugarUnitRepeat arg0) throws GlycoVisitorException 
	{
		// nothing to do
	}

	@Override
	public void visit(Substituent arg0) throws GlycoVisitorException 
	{
		// nothing to do
	}

	@Override
	public void visit(SugarUnitCyclic arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyFuc1d does not support SugarUnitCyclic residues");
	}

	@Override
	public void visit(SugarUnitAlternative arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyFuc1d does not support SugarUnitAlternative residues");
	}

	@Override
	public void visit(UnvalidatedGlycoNode arg0) throws GlycoVisitorException 
	{
		throw new GlycoVisitorException("GlycoVisitorRectifyFuc1d does not support UnvalidatedGlycoNode residues");
	}

	@Override
	public void visit(GlycoEdge arg0) throws GlycoVisitorException 
	{
		// nothing to do
	}
}
