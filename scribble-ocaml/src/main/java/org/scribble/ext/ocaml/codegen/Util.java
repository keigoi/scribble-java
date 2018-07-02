package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.ScribbleException;
import org.scribble.model.MAction;
import org.scribble.model.MState;
import org.scribble.type.kind.ProtocolKind;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.LProtocolName;
import org.scribble.type.name.MessageId;
import org.scribble.type.name.Role;

public class Util {

	public static String uncapitalise(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
	public static String label(MessageId<?> msg) {
		return label(msg.toString());
	}

	public static String label(String msg) {
		if(msg.matches("^[0-9].*")) {
			return "_" + msg;
		} else if("".equals(msg)) {
			return "msg";
		} else {
			return msg;
		}
	}
	
	public static 
		<T,
		L,
		A extends MAction<K>,
		S extends MState<L, A, S, K>,
		K extends ProtocolKind> T traverse(S state, T init, BiFunction<T, S, T> fun) {
		return traverse(state, init, fun, new ArrayList<>());
	}
	
	private static <T,
	L,
	A extends MAction<K>,
	S extends MState<L, A, S, K>,
	K extends ProtocolKind> T traverse(S state, T init, BiFunction<T, S, T> fun, List<S> visited) {
		if(visited.contains(state)) {
			return init;
		}
		
		visited.add(state);
		
		T accum = init;
		for(S succ : state.getSuccessors()) {
			accum = fun.apply(accum, succ);
		}
		return accum;
	}
	
	public static class GProtocolNameRole {
		public final GProtocolName name;
		public final Role role;
		public GProtocolNameRole(GProtocolName name, Role role) {
			this.name = name;
			this.role = role;
		}
	}
	
	// brain-dead approach to recover global name from a local name
	public static GProtocolName searchGlobalProtocolName(Module module, LProtocolName local) throws ScribbleException {
		String localname = local.getSimpleName().toString();
		for (GProtocolDecl global : module.getGlobalProtocolDecls()) {
			GProtocolName name = global.getHeader().getDeclName();
			String globalname = name.getSimpleName().toString();
			for (Role role : global.header.roledecls.getRoles()) {
				if (localname.equals(globalname + "_" + role.toString())) {
					return new GProtocolNameRole(new GProtocolName(module.getFullModuleName(), name), role).name;
				}
			}
		}
		throw new RuntimeException("[OCaml] cannot find GProtocolName from LProtocolName " + local);
	}

	public static String getRoleConnTypeParams(List<Role> roles, Role myrole) {
		StringBuffer buf = new StringBuffer();
		boolean multiple = false;
		for(Role role : roles) {
			if(role.equals(myrole)) continue;
			if( multiple ) {
				buf.append(", ");
			} else {
				multiple = true;				
			}
			buf.append("'c_" + role);
		}
		return multiple ? "(" + buf.toString() + ")" : buf.toString();
	}

	protected static String generatePolyvarBranch(List<LabelAndPayload> branch, boolean abstract_tyvar) {
		int count = 0;
		StringBuffer buf = new StringBuffer();
		for(LabelAndPayload label : branch) {
			buf.append("`" + Util.label(label.label) + " of " + label.getPayloadTypeRepr() + " * " + (abstract_tyvar ? "" : "'") + "p" + count);
			if (count<branch.size()-1) {
				buf.append("|");
			}
			count++;
		}
		return "[" + buf.toString() + "]";
	}

}
