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

import java.util.HashMap;
import java.util.Map;

import org.scribble.cli.CLArgFlag;
import org.scribble.cli.CommandLine;
import org.scribble.cli.CommandLineException;
import org.scribble.ext.ocaml.codegen.OCamlAPIBuilder;
import org.scribble.ext.ocaml.codegen.Util;
import org.scribble.main.AntlrSourceException;
import org.scribble.main.Job;
import org.scribble.main.JobContext;
import org.scribble.main.ScribbleException;
import org.scribble.type.name.GProtocolName;

public class OCamlCommandLine extends CommandLine
{
	protected final Map<OCamlCLArgFlag, String[]> ocamlArgs;  // Maps each flag to list of associated argument values
	
	public OCamlCommandLine(OCamlCLArgParser p) throws CommandLineException
	{
		super(p);
		this.ocamlArgs = p.getOCamlArgs();
		// Duplicated from super
		if (!this.args.containsKey(CLArgFlag.MAIN_MOD) && !this.args.containsKey(CLArgFlag.INLINE_MAIN_MOD))
		{
			throw new CommandLineException("No main module has been specified\r\n");
		}
	}

	@Override
	protected void doNonAttemptableOutputTasks(Job job) throws ScribbleException, CommandLineException
	{		
		if (this.ocamlArgs.containsKey(OCamlCLArgFlag.OCAML_API_GEN))
		{
			JobContext jcontext = job.getContext();
			String[] args = this.ocamlArgs.get(OCamlCLArgFlag.OCAML_API_GEN);
			for (int i = 0; i < args.length; i += 2)
			{
				GProtocolName fullname = checkGlobalProtocolArg(jcontext, args[i]);
				String program = new OCamlAPIBuilder(job, fullname).generateAPI();
				
				Map<String, String> map = new HashMap<>(); 				
				map.put(Util.uncapitalise(fullname.getSimpleName().toString()) + ".ml", program);
				outputClasses(map);
			}
		}
		else
		{
			super.doNonAttemptableOutputTasks(job);
		}
	}

	public static void main(String[] args) throws CommandLineException, ScribbleException, AntlrSourceException
	{
		new OCamlCommandLine(new OCamlCLArgParser(args)).run();
	}
}
