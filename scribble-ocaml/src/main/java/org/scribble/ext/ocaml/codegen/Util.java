package org.scribble.ext.ocaml.codegen;

public class Util {

	public static String uncapitalise(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

}
