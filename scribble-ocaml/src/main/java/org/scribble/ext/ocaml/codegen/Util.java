package org.scribble.ext.ocaml.codegen;

import org.scribble.sesstype.name.MessageId;

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

}
