package org.scribble.ext.ocaml.codegen;

import java.util.List;
import java.util.function.Consumer;

public class Indent {
	
	private int indent_level;
	
	public void incr() {
		this.indent_level++;
	}
	
	public void decr() {
		this.indent_level--;
	}
	
	public void reset() {
		this.indent_level = 0;
	}
	
	public int curr() {
		return this.indent_level;
	}
	
	// indent == -1 is the rightmost 
	public void indent(StringBuffer buf) {
		for (int i = 0; i < (indent_level + 1) * 2; i++) {
			buf.append(' ');
		}
	}
	
	public <T> void iterate(StringBuffer buf, List<T> list, Consumer<T> f) {
		if (list.size() > 1) {
			this.indent_level++;
			for(T v : list) {
				buf.append("\n");
				for (int i = 0; i < (indent_level + 1) * 2; i++) {
					buf.append(' ');
				}
				f.accept(v);
			}
			this.indent_level--;
		} else {
			buf.append(" ");
			f.accept(list.get(0));
		}
	}

}
