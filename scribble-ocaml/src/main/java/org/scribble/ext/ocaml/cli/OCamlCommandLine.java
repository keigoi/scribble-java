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
import org.scribble.ext.ocaml.codegen.OCamlTypeBuilder;
import org.scribble.main.Job;
import org.scribble.main.JobContext;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.Role;

public class OCamlCommandLine extends CommandLine
{
	protected final Map<OCamlCLArgFlag, String[]> goArgs;  // Maps each flag to list of associated argument values
	
	public OCamlCommandLine(String... args) throws CommandLineException
	{
		/*super(args);  // No: go-args will make core arg parser throw exception -- refactor?
		this.goArgs = new GoCLArgParser(args).getGoArgs();*/

		// FIXME: refactor
		OCamlCLArgParser p = new OCamlCLArgParser(args);
		this.args = p.getArgs();
		this.goArgs = p.getGoArgs();
		// Duplicated from super
		if (!this.args.containsKey(CLArgFlag.MAIN_MOD) && !this.args.containsKey(CLArgFlag.INLINE_MAIN_MOD))
		{
			throw new CommandLineException("No main module has been specified\r\n");
		}
	}

	@Override
	protected void doNonAttemptableOutputTasks(Job job) throws ScribbleException, CommandLineException
	{		
		if (this.goArgs.containsKey(OCamlCLArgFlag.OCAML_API_GEN))
		{
			JobContext jcontext = job.getContext();
			String[] args = this.goArgs.get(OCamlCLArgFlag.OCAML_API_GEN);
			for (int i = 0; i < args.length; i += 2)
			{
				GProtocolName fullname = checkGlobalProtocolArg(jcontext, args[i]);
				Role role = checkRoleArg(jcontext, fullname, args[i+1]);
				OCamlTypeBuilder apigen = new OCamlTypeBuilder(job, fullname, role, job.getContext().getEGraph(fullname, role));
				Map<String, String> map = new HashMap<>(); 
				map.put("type.ml", apigen.build());
				outputClasses(map);
			}
		}
		else
		{
			super.doNonAttemptableOutputTasks(job);
		}
	}

	public static void main(String[] args) throws CommandLineException, ScribbleException
	{
		new OCamlCommandLine(args).run();
	}
}
