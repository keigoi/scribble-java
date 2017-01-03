package org.scribble.ext.f17.ast;

import org.antlr.runtime.tree.CommonTree;
import org.scribble.ast.AstFactoryImpl;
import org.scribble.ast.name.NameNode;
import org.scribble.ast.name.PayloadElemNameNode;
import org.scribble.ast.name.simple.OpNode;
import org.scribble.ast.name.simple.RecVarNode;
import org.scribble.ast.name.simple.RoleNode;
import org.scribble.del.name.RecVarNodeDel;
import org.scribble.del.name.RoleNodeDel;
import org.scribble.ext.f17.ast.name.simple.PayloadVarNode;
import org.scribble.ext.f17.del.AnnotUnaryPayloadElemDel;
import org.scribble.sesstype.kind.DataTypeKind;
import org.scribble.sesstype.kind.Kind;
import org.scribble.sesstype.kind.OpKind;
import org.scribble.sesstype.kind.PayloadTypeKind;
import org.scribble.sesstype.kind.RecVarKind;
import org.scribble.sesstype.kind.RoleKind;


public class AnnotAstFactoryImpl extends AstFactoryImpl implements AnnotAstFactory
{
	//public static final AnnotAstFactory FACTORY = new AnnotAstFactoryImpl();
	
	// Duplicated from AstFactoryImpl
	@Override
	public <K extends Kind> NameNode<K> SimpleNameNode(CommonTree source, K kind, String identifier)
	{
		NameNode<? extends Kind> snn = null;
		
		// Custom dels
		if (kind.equals(RecVarKind.KIND))
		{
			snn = new RecVarNode(source, identifier);
			snn = del(snn, new RecVarNodeDel());
		}
		else if (kind.equals(RoleKind.KIND))
		{
			snn = new RoleNode(source, identifier);
			snn = del(snn, new RoleNodeDel());
		}
		if (snn != null)
		{
			return castNameNode(kind, snn);
		}

		// Default del
		if (kind.equals(OpKind.KIND))
		{
			snn = new OpNode(source, identifier);
		}
		//else if (kind.equals(PayloadVarKind.KIND))
		else if (kind.equals(DataTypeKind.KIND))  // No conflict with regular simple data type name nodes?
		{
			snn = new PayloadVarNode(source, identifier);
		}
		else
		{
			throw new RuntimeException("Shouldn't get in here: " + kind);
		}
		return castNameNode(kind, del(snn, createDefaultDelegate()));
	}

	@Override
	public <K extends PayloadTypeKind> AnnotUnaryPayloadElem<K> UnaryPayloadElem(CommonTree source, PayloadElemNameNode<K> name)
	{
		AnnotUnaryPayloadElem<K> pe = new AnnotUnaryPayloadElem<>(source, name);
		pe = del(pe, createDefaultDelegate());
		return pe;
	}

	@Override
	public <K extends PayloadTypeKind> AnnotUnaryPayloadElem<K> AnnotUnaryPayloadElem(CommonTree source, PayloadVarNode payvar, PayloadElemNameNode<K> name)
	{
		AnnotUnaryPayloadElem<K> pe = new AnnotUnaryPayloadElem<>(source, payvar, name);
		//pe = del(pe, createDefaultDelegate());
		pe = del(pe, new AnnotUnaryPayloadElemDel());
		return pe;
	}

	/*@Override
	public GMessageTransfer GMessageTransfer(CommonTree source, RoleNode src, MessageNode msg, List<RoleNode> dests)
	{
		GMessageTransfer gmt = new GMessageTransfer(source, src, msg, dests);
		gmt = del(gmt, new GMessageTransferDel());
		return gmt;
	}

	@Override
	public GConnect GConnect(CommonTree source, RoleNode src, MessageNode msg, RoleNode dest)
	//public GConnect GConnect(RoleNode src, RoleNode dest)
	{
		GConnect gc = new GConnect(source, src, msg, dest);
		//GConnect gc = new GConnect(src, dest);
		gc = del(gc, new GConnectDel());
		return gc;
	}

	@Override
	public GDisconnect GDisconnect(CommonTree source, RoleNode src, RoleNode dest)
	{
		GDisconnect gc = new GDisconnect(source, src, dest);
		gc = del(gc, new GDisconnectDel());
		return gc;
	}
	@Override
	public LSend LSend(CommonTree source, RoleNode src, MessageNode msg, List<RoleNode> dests)
	{
		LSend ls = new LSend(source, src, msg, dests);
		ls = del(ls, new LSendDel());
		return ls;
	}

	@Override
	public LReceive LReceive(CommonTree source, RoleNode src, MessageNode msg, List<RoleNode> dests)
	{
		LReceive ls = new LReceive(source, src, msg, dests);
		ls = del(ls, new LReceiveDel());
		return ls;
	}
	
	@Override
	public LConnect LConnect(CommonTree source, RoleNode src, MessageNode msg, RoleNode dest)
	//public LConnect LConnect(RoleNode src, RoleNode dest)
	{
		LConnect lc = new LConnect(source, src, msg, dest);
		//LConnect lc = new LConnect(src, dest);
		lc = del(lc, new LConnectDel());
		return lc;
	}

	@Override
	public LAccept LAccept(CommonTree source, RoleNode src, MessageNode msg, RoleNode dest)
	//public LAccept LAccept(RoleNode src, RoleNode dest)
	{
		LAccept la = new LAccept(source, src, msg, dest);
		//LAccept la = new LAccept(src, dest);
		la = del(la, new LAcceptDel());
		return la;
	}

	@Override
	public LDisconnect LDisconnect(CommonTree source, RoleNode self, RoleNode peer)
	{
		LDisconnect lc = new LDisconnect(source, self, peer);
		lc = del(lc, new LDisconnectDel());
		return lc;
	}*/
}