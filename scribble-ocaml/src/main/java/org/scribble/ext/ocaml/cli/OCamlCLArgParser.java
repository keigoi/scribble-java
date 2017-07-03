/**
 * Copyright 2008 The Scribble Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.scribble.ext.ocaml.cli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.scribble.cli.CLArgParser;
import org.scribble.cli.CommandLineException;

public class OCamlCLArgParser extends CLArgParser
{
	// Non-unique flags
	public static final String OCAML_API_GEN_FLAG = "-ocamlapi";

	private static final Map<String, OCamlCLArgFlag> OCAML_NON_UNIQUE_FLAGS = new HashMap<>();
	{
		OCamlCLArgParser.OCAML_NON_UNIQUE_FLAGS.put(OCamlCLArgParser.OCAML_API_GEN_FLAG, OCamlCLArgFlag.OCAML_API_GEN);
	}

	private static final Map<String, OCamlCLArgFlag> OCAML_FLAGS = new HashMap<>();
	{
		OCamlCLArgParser.OCAML_FLAGS.putAll(OCamlCLArgParser.OCAML_NON_UNIQUE_FLAGS);
	}

	private final Map<OCamlCLArgFlag, String[]> ocamlParsed = new HashMap<>();
	
	public OCamlCLArgParser(String[] args) throws CommandLineException
	{
		super(args);  // Assigns this.args and calls parseArgs
	}		
	
	public Map<OCamlCLArgFlag, String[]> getGoArgs()
	{
		return this.ocamlParsed;
	}
	
	@Override
	protected boolean isFlag(String arg)
	{
		return OCamlCLArgParser.OCAML_FLAGS.containsKey(arg) || super.isFlag(arg);
	}
	
	// Pre: i is the index of the current flag to parse
	// Post: i is the index of the last argument parsed -- parseArgs does the index increment to the next current flag
	@Override
	protected int parseFlag(int i) throws CommandLineException
	{
		String flag = this.args[i];
		switch (flag)
		{
			case OCamlCLArgParser.OCAML_API_GEN_FLAG:
			{
				return ocamlParseProtoAndRoleArgs(flag, i);
			}
			default:
			{
				return super.parseFlag(i);
			}
		}
	}

	// FIXME: factor out with core arg parser -- issue is GoCLArgFlag is currently an unlreated type to CLArgFlag
	private int ocamlParseProtoAndRoleArgs(String f, int i) throws CommandLineException
	{
		OCamlCLArgFlag flag = OCamlCLArgParser.OCAML_NON_UNIQUE_FLAGS.get(f);
		if ((i + 2) >= this.args.length)
		{
			throw new CommandLineException("Missing protocol/role arguments");
		}
		String proto = this.args[++i];
		String role = this.args[++i];
		ocamlConcatArgs(flag, proto, role);
		return i;
	}
	
	// FIXME: factor out with core arg parser -- issue is GoCLArgFlag is currently an unlreated type to CLArgFlag
	private void ocamlConcatArgs(OCamlCLArgFlag flag, String... toAdd)
	{
		String[] args = this.ocamlParsed.get(flag);
		if (args == null)
		{
			args = Arrays.copyOf(toAdd, toAdd.length);
		}
		else
		{
			String[] tmp = new String[args.length + toAdd.length];
			System.arraycopy(args, 0, tmp, 0, args.length);
			System.arraycopy(toAdd, 0, tmp, args.length, toAdd.length);
			args = tmp;
		}
		this.ocamlParsed.put(flag, args);
	}
}
