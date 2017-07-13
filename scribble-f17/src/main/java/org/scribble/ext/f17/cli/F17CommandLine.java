package org.scribble.ext.f17.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.cli.CLArgFlag;
import org.scribble.cli.CommandLine;
import org.scribble.cli.CommandLineException;
import org.scribble.del.ModuleDel;
import org.scribble.ext.annot.visit.context.AnnotSetter;
import org.scribble.ext.f17.ast.global.F17GProtocolDeclTranslator;
import org.scribble.ext.f17.ast.global.F17GType;
import org.scribble.ext.f17.ast.local.F17LType;
import org.scribble.ext.f17.ast.local.F17Projector;
import org.scribble.ext.f17.main.F17Exception;
import org.scribble.ext.f17.main.F17Job;
import org.scribble.ext.f17.main.F17MainContext;
import org.scribble.ext.f17.model.endpoint.F17EGraphBuilder;
import org.scribble.ext.f17.model.global.F17ProgressErrors;
import org.scribble.ext.f17.model.global.F17SModel;
import org.scribble.ext.f17.model.global.F17SModelBuilder;
import org.scribble.ext.f17.model.global.F17SState;
import org.scribble.ext.f17.model.global.F17SafetyErrors;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.main.resource.DirectoryResourceLocator;
import org.scribble.main.resource.ResourceLocator;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.endpoint.EState;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.Role;
import org.scribble.util.ScribParserException;
import org.scribble.visit.context.RecRemover;

public class F17CommandLine extends CommandLine
{
	private final Map<F17CLArgFlag, String[]> f17Args;  // Maps each flag to list of associated argument values
	
	public F17CommandLine(String... args) throws CommandLineException
	{
		this(new F17CLArgParser(args));
	}

	public F17CommandLine(F17CLArgParser p) throws CommandLineException
	{
		super(p);  // calls p.parse()
		if (this.args.containsKey(CLArgFlag.INLINE_MAIN_MOD))
		{
			// FIXME: should be fine
			throw new RuntimeException("[f17] Inline modules not supported:\n" + this.args.get(CLArgFlag.INLINE_MAIN_MOD));
		}
		// FIXME? Duplicated from core
		if (!this.args.containsKey(CLArgFlag.MAIN_MOD))
		{
			throw new CommandLineException("No main module has been specified\r\n");
		}

		this.f17Args = p.getF17Args();
	}

	public static void main(String[] args) throws CommandLineException, ScribbleException
	{
		new F17CommandLine(args).run();
	}

	@Override
	protected F17MainContext newMainContext() throws ScribParserException, ScribbleException
	{
		//boolean jUnit = this.args.containsKey(ArgFlag.JUNIT);
		boolean debug = this.args.containsKey(CLArgFlag.VERBOSE);
		boolean useOldWF = this.args.containsKey(CLArgFlag.OLD_WF);
		boolean noLiveness = this.args.containsKey(CLArgFlag.NO_LIVENESS);
		boolean minEfsm = this.args.containsKey(CLArgFlag.LTSCONVERT_MIN);
		boolean fair = this.args.containsKey(CLArgFlag.FAIR);
		boolean noLocalChoiceSubjectCheck = this.args.containsKey(CLArgFlag.NO_LOCAL_CHOICE_SUBJECT_CHECK);
		boolean noAcceptCorrelationCheck = this.args.containsKey(CLArgFlag.NO_ACCEPT_CORRELATION_CHECK);
		boolean noValidation = this.args.containsKey(CLArgFlag.NO_VALIDATION);

		List<Path> impaths = this.args.containsKey(CLArgFlag.IMPORT_PATH)
				? F17CommandLine.parseImportPaths(this.args.get(CLArgFlag.IMPORT_PATH)[0])
				: Collections.emptyList();
		ResourceLocator locator = new DirectoryResourceLocator(impaths);
		if (this.args.containsKey(CLArgFlag.INLINE_MAIN_MOD))
		{
			return new F17MainContext(debug, locator, this.args.get(CLArgFlag.INLINE_MAIN_MOD)[0], useOldWF, noLiveness, minEfsm, fair,
					noLocalChoiceSubjectCheck, noAcceptCorrelationCheck, noValidation);
		}
		else
		{
			Path mainpath = F17CommandLine.parseMainPath(this.args.get(CLArgFlag.MAIN_MOD)[0]);
			//return new MainContext(jUnit, debug, locator, mainpath, useOldWF, noLiveness);
			return new F17MainContext(debug, locator, mainpath, useOldWF, noLiveness, minEfsm, fair,
					noLocalChoiceSubjectCheck, noAcceptCorrelationCheck, noValidation);
		}
	}

	@Override
	protected void doValidationTasks(Job job) throws ScribbleException, ScribParserException
	{
		if (this.f17Args.containsKey(F17CLArgFlag.F17))
		{
			if (this.args.containsKey(CLArgFlag.PROJECT))  // HACK
				// modules/f17/src/test/scrib/demo/fase17/AppD.scr in [default] mode bug --- projection/EFSM not properly formed if this if is commented ????
			{

			}

			GProtocolName simpname = new GProtocolName(this.f17Args.get(F17CLArgFlag.F17)[0]);
			if (simpname.toString().equals("[F17AllTest]"))  // HACK: F17AllTest
			{
				parseAndCheckWF(job);  // Includes base passes
			}
			else
			{
				parseAndCheckWF(job, simpname);  // Includes base passes
			}
			
			// FIXME? f17 FSM building only used for f17 validation -- output tasks, e.g., -api, will still use default Scribble FSMs
		}
		else
		{
			super.doValidationTasks(job);
		}
	}

	
	// Refactor into F17Job?

	private static void parseAndCheckWF(Job job, GProtocolName simpname) throws ScribbleException, ScribParserException
	{
		f17PreContextBuilding(job);
		
		Module main = job.getContext().getMainModule();
		
		/*if (simpname.toString().equals("[F17AllTest]")) // HACK: F17AllTest
		{
			simpname = main.getGlobalProtocolDecls().iterator().next().getHeader().getNameNode().toName();
		}*/

		if (!main.hasProtocolDecl(simpname))
		{
			throw new ScribbleException("Global protocol not found: " + simpname);
		}
		GProtocolDecl gpd = (GProtocolDecl) main.getProtocolDecl(simpname);
		
		parseAndCheckWF(job, main, gpd);
	}
	
	private static void parseAndCheckWF(Job job) throws ScribbleException, ScribParserException
	{
		f17PreContextBuilding(job);

		Module main = job.getContext().getMainModule();
		for (GProtocolDecl gpd : main.getGlobalProtocolDecls())
		{
			parseAndCheckWF(job, main, gpd);
		}
	}
	
	private static void f17PreContextBuilding(Job job) throws ScribbleException

	{
		job.runContextBuildingPasses();
		job.runVisitorPassOnParsedModules(RecRemover.class);  // FIXME: Integrate into main passes?  Do before unfolding?
		job.runVisitorPassOnParsedModules(AnnotSetter.class);  // Hacky -- run after inlining, because original dels discarded
	}

	// Pre: f17PreContextBuilding
	private static void parseAndCheckWF(Job job, Module main, GProtocolDecl gpd) throws ScribbleException, ScribParserException
	{
		F17GType gt = new F17GProtocolDeclTranslator().translate(job, ((ModuleDel) main.del()).getModuleContext(), gpd);
		
		gt.checkRoleEnabling(new HashSet<>(gpd.header.roledecls.getRoles()));

		job.debugPrintln
		//System.out.println
			("[f17] Translated:\n  " + gt);

		Map<Role, F17LType> P0 = new HashMap<>();
		F17Projector p = new F17Projector();
		for (Role r : gpd.header.roledecls.getRoles())
		{
			F17LType lt = p.project(gt, r, Collections.emptySet());
			P0.put(r, lt);

			job.debugPrintln
			//System.out.println
				("[f17] Projected onto " + r + ":\n  " + lt);
		}

		F17EGraphBuilder builder = new F17EGraphBuilder(job.ef);
		Map<Role, EState> E0 = new HashMap<>();
		for (Role r : P0.keySet())
		{
			EGraph g = builder.build(P0.get(r));
			E0.put(r, g.init);

			job.debugPrintln
			//System.out.println
					("[f17] Built endpoint graph for " + r + ":\n" + g.toDot());
		}
		
		validate(job, gpd.isExplicitModifier(), E0);

		if (!job.fair)
		{
			Map<Role, EState> U0 = new HashMap<>();
			for (Role r : E0.keySet())
			{
				EState u = E0.get(r).unfairTransform();
				U0.put(r, u);

				job.debugPrintln
				//System.out.println
						("[f17] Unfair transform for " + r + ":\n" + u.toDot());
			}
			
			validate(job, gpd.isExplicitModifier(), U0, true);
		}
		
		/*// Needed for API gen (base projections and EFSMs) -- no: JobContext getters built on demand -- no: projection not done
		job.runUnfoldingPass();
		job.runWellFormednessPasses();*/
		((F17Job) job).runF17ProjectionPasses();  // projections not built on demand; cf. models

		//return gt;
	}

	private static void validate(Job job, boolean isExplicit, Map<Role, EState> E0, boolean... unfair) throws F17Exception
	{
		F17SModel m = new F17SModelBuilder(job.sf).build(E0, isExplicit);

		job.debugPrintln
		//System.out.println
				("[f17] Built model:\n" + m.toDot());
		
		if (unfair.length == 0)
		{
			F17SafetyErrors serrs = m.getSafetyErrors();
			if (serrs.isSafe())
			{
				job.debugPrintln
				//System.out.println
						("[f17] Protocol safe.");
			}
			else
			{
				throw new F17Exception("[f17] Protocol not safe.\n" + serrs);
			}
		}
		
		F17ProgressErrors perrs = m.getProgressErrors();
		if (perrs.satisfiesProgress())
		{
			job.debugPrintln
			//System.out.println
					("[f17] " + ((unfair.length == 0) ? "Fair protocol" : "Protocol") + " satisfies progress.");
		}
		else
		{

			// FIXME: refactor eventual reception as 1-bounded stable check
			Set<F17SState> staberrs = m.getStableErrors();
			if (perrs.eventualReception.isEmpty())
			{
				if (!staberrs.isEmpty())
				{
					throw new RuntimeException("[f17] 1-stable check failure: " + staberrs);
				}
			}
			else
			{
				if (staberrs.isEmpty())
				{
					throw new RuntimeException("[f17] 1-stable check failure: " + perrs);
				}
			}
			
			throw new F17Exception("[f17] " + ((unfair.length == 0) ? "Fair protocol" : "Protocol") + " violates progress.\n" + perrs);
		}
	}
}
