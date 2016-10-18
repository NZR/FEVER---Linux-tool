package fever.parsers.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import models.BuildModel;
import models.CompilationTarget;
import models.CompilationTargetType;
import models.MakeSymbol;
import models.MappedFeature;
import models.ModelsFactory;
import fever.GitRepoFactory;
import fever.PropReader;
import fever.utils.FeatureUtils;
import fever.utils.ParsingUtils;



public class BuildScriptBuilder {
	
	public Map<String,List<String>> raw_mapping = new HashMap<String,List<String>>();

	List<CompilationTarget> _known_targets = new ArrayList<CompilationTarget>();
	
	public String _path= "";
	public String _folder = "";
	public String _guard_suffix = ""; 
	
	public BuildScriptBuilder() 
	{
		try{
			PropReader p = new PropReader(); 
			_guard_suffix  = p.getProperty("guard_suffix");
		}
		catch(Exception e)
		{
			
		}
	}
	
	/**
	 * Creates a BuildModel from the content of a Makefile
	 * 
	 * WARNING: in many cases, f will not be at "makefile_location". 
	 * We restore the makefile in a temp folder (f), and "makefile_location"
	 * is the location of the original makefile in the repository! 
	 * 
	 * @param f the makefile to parse
	 * @param location of the file in the file tree.
	 * @return a well formed buildmodel entity
	 * @throws Exception
	 */
	public BuildModel buildModelFromFile(File f, String makefile_location) throws Exception 
	{
		setPathsForExtraction(f,makefile_location);
		try
		{
			fillRawMapping(f);
			resolveAliases();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage()  + " " + _folder );
			System.err.println("extract mapping from Makefile, the build model will be partial" );
		}

		BuildModel m = instanciateModel();
		return m;
	}

	/**
	 * sets the _path and _folder parameter for this extraction.
	 * 
	 * @param f
	 * @throws Exception
	 */
	private void setPathsForExtraction(File f,String makefile_location) throws Exception
	{
		_path = f.getAbsolutePath();
		
		_folder = "/"+makefile_location;
		_folder = _folder.replace("Makefile","").trim();
	}

	
	private BuildModel instanciateModel() 
	{
		BuildModel build_model = ModelsFactory.eINSTANCE.createBuildModel();
		
		for(String k : raw_mapping.keySet())
		{
			List<String> feats = FeatureUtils.getFeatureNames(k);
			if(feats.size() == 0)
			{
				extractMappingModelForSymbol(build_model, k);
			}
			else
			{
				for(String f_name : feats)
				extractMappingModelForFeature(build_model, k, f_name);
			}
		}
		return build_model;
	}

	private void extractMappingModelForFeature(BuildModel build_model, String k, String f_name)
	{	
		//search for the feature in the already extracted ones  (just in case).
		MappedFeature existing_mf = getExistingFeat(build_model,f_name);
		MappedFeature mf = null; 
		if(existing_mf != null)
		{
			mf = existing_mf;
		}
		else 
		{
			mf = ModelsFactory.eINSTANCE.createMappedFeature(); 
		}

		mf.setFeatureName(f_name);
		mf.setId(f_name);
		
		List<CompilationTarget> found_targets = new ArrayList<CompilationTarget>();
		extractCompilationTargets(k, f_name, found_targets);
		addTargetsToFeature(mf,found_targets);
		
		if(mf.getTargets().size() != 0)
		{
			build_model.getFeatures().add(mf);
		}
	}

	private void extractMappingModelForSymbol(BuildModel build_model, String k)
	{
		String s_name = "make_symbol_"+k.trim();
		
		MakeSymbol existing_symbol = getExistingSymbol(build_model,s_name);
		MakeSymbol symbol = null; 
		if(existing_symbol != null)
		{
			symbol = existing_symbol;
		}
		else 
		{
			symbol = ModelsFactory.eINSTANCE.createMakeSymbol();
		}
		
		symbol.setName(s_name);

		List<CompilationTarget> found_targets = new ArrayList<CompilationTarget>();
		extractCompilationTargets(k, s_name, found_targets);
		symbol.getTargets().addAll(found_targets);

		if(symbol.getTargets().size() != 0)
			build_model.getSymbols().add(symbol);
	}

	
	private void addTargetsToFeature(MappedFeature mf, List<CompilationTarget> targetsToMerge)
	{
		for(CompilationTarget t: targetsToMerge)
		{
			boolean found = false;
			for(CompilationTarget existing_t : mf.getTargets())
			{
				if(t.getTargetName().equals(existing_t.getTargetName()) && (t.getMappedToSymbol().equals(existing_t.getMappedToSymbol())))
				{
					found = true;
				}
			}
			if (! found)
			{
				mf.getTargets().add(t);
			}
		}
	}
	
	
	private void extractCompilationTargets(String k, String f_name,List<CompilationTarget> found_targets) 
	{
		
		for(String t : raw_mapping.get(k))
		{
			 t = t.trim();
			 CompilationTarget target = ModelsFactory.eINSTANCE.createCompilationTarget();
			 
			 target.setMappedToSymbol(f_name);

			 CompilationTargetType type = getMappingTargetType(t);
			 
			 if(type == CompilationTargetType.FOLDER)
			 {
				 if(_guard_suffix.length() > 1)
					 t = t.replace(_guard_suffix, "");	 
				 t = _folder+t;
			 }
			 
			 target.setTargetName(t);
			 
			 if(type != null)
			 {
				 target.setTargetType(type);
				 target.setId(t);
				 found_targets.add(target);
				 _known_targets.add(target);
			 }
		}
	}

	private CompilationTargetType getMappingTargetType(String t)
	{
		CompilationTargetType type;
		if(ParsingUtils.hasBinaryExtension(t))
		 {
			 type = CompilationTargetType.BINARY;
		 }
		 else if (ParsingUtils.hasDataExtension(t))
		 {
			 type = CompilationTargetType.DATA;
		 }
		 else if(ParsingUtils.hasSourceFileExtension(t))
		 {
			 type = CompilationTargetType.COMPILATION_UNIT;
		 }
		 else if (ParsingUtils.isFolderName(t) ) //call parsing utils instead !
		 {
			 type = CompilationTargetType.FOLDER;
		 }
		 else if (ParsingUtils.isCompilationFlag(t))
		 {
			 type = CompilationTargetType.CC_FLAG;
		 }
		 else
		 {
			 type = CompilationTargetType.CC_VAR;
		 }
		return type;
	}

	private MappedFeature getExistingFeat(BuildModel m, String f_name) 
	{	
		for(MappedFeature f : m.getFeatures())
		{
			if(f.getFeatureName().equals(f_name))
				return f;
		}
		return null;
	}
	
	private MakeSymbol getExistingSymbol(BuildModel m, String s_name) 
	{
		for(MakeSymbol s : m.getSymbols())
		{
			if(s.getName().equals(s_name))
				return s;
		}
		return null;
	}


	
	private void resolveAliases() 
	{
		
		Map<String,List<String>> alias = new HashMap<String,List<String>>();
		
		for(String k : raw_mapping.keySet())
		{
			List<String> targets = raw_mapping.get(k);
			for(String t : targets)
			{
				if((ParsingUtils.hasSourceFileExtension(t)  &&  t.indexOf("$") != -1) || t.indexOf("/") != -1 || t.startsWith("-"))
				{ //not a compilation unit name, nor a variable, let's carry on.
					continue;
				}
				String srch_str  = "";
				boolean isVar = false;
				if(ParsingUtils.hasSourceFileExtension(t))
				{
					srch_str = t.substring(0, t.length() -2);
				}
				else if (t.indexOf("$") != -1)
				{
					isVar = true;
					t = t.replace("(","");t = t.replace(")","");t = t.replace("$","");
					srch_str = t;
				}
				
				for(String candidate : raw_mapping.keySet())
				{
					if(candidate.equals(k))
						continue;
					if(candidate.indexOf("CONFIG_") != -1)
						continue;
					
					String s = candidate;
					if(candidate.indexOf("-") != -1 && !isVar)
						s = candidate.substring(0, candidate.lastIndexOf("-"));
					
					if(srch_str.equals(s))
					{
						alias.put(candidate, raw_mapping.get(candidate));
					}
				}
			}
		}
		
	
		for(String s : alias.keySet())
		{
			String alias_key ; 
			if( s.indexOf("-") != -1 )
				alias_key = s.substring(0, s.lastIndexOf("-"));
			else 
				alias_key = s;
			
			for(String key : raw_mapping.keySet())
			{
				if(key.equals(s))
					continue;
				List<String> vals = raw_mapping.get(key);
				List<Integer> idx = new ArrayList<Integer>();
				for(String val : vals)
				{
					String checkVal = val;
					if(val.contains("."))
						checkVal = val.substring(0, val.indexOf("."));

					if(alias_key.equals(checkVal))
					{
						Integer a = vals.indexOf(val);
						idx.add(a);
					}
				}
				
				if(!idx.isEmpty())
				{
					for(Integer i : idx)
					{
						vals.remove(i.intValue());
					}
					vals.addAll(alias.get(s));
				}
			}
		}
		
		for(String s : alias.keySet())
		{
			raw_mapping.remove(s);
		}
	}

	private void fillRawMapping(File f) throws Exception 
	{
		BufferedReader r = new BufferedReader(new FileReader(f)); //makefile reader
		String l;												  //line from Makefile
		Stack<String> ifeq = new Stack<String>(); 				  //stack of if (statement) affecting feature->file mapping
		
		while(null != ( l= r.readLine()))
		{
			l = resolve_line_continuation(r, l.trim()).trim();
			
			if(l.trim().length() == 0 || l.startsWith("#")) //skip commented or empty lines - *must* be done after line continuation resolution. 
				continue;

			if(l.indexOf("#") != -1) //clean possible comments at the end of the lines
				l = l.substring(0, l.indexOf("#"));
			
			updateContextStack(l, ifeq);
			
			String equality_op = getAssignmentOperator(l);
			if(equality_op!=null)
			{
				String[]elems = l.split("\\"+equality_op); 
				if(elems.length != 2)
					continue;	//skipping malformed line.

				String left = elems[0].trim();
				String right = elems[1].trim(); 
				extractMappingFromStatement(ifeq, left, right);
			}
		}
		r.close();
	}

	private void updateContextStack(String l, Stack<String> ifeq)
	{
		if(l.startsWith("ifeq") || l.startsWith("ifdef") || l.startsWith("ifndef")) //axtls fix
		{
			ifeq.push(l);
		}
		
		if(l.startsWith("else ifdef"))
		{
			String tmp = l.replace("else ", "");
			tmp = tmp + " && "+("!"+ifeq.pop());
			ifeq.push(tmp);
		}
		else  if(l.startsWith("else"))
		{
			ifeq.push(("!("+ifeq.pop()+")"));
		}
		
		if(l.startsWith("endif") && !ifeq.isEmpty())
		{
			ifeq.pop();
		}
	}

	/**
	 * Extract mapping for a assignment statement.
	 * ifeq stack must be passed so the targets can include their context
	 * 
	 * @param ifeq existing ifeq stacks
	 * @param left left-hand side of the assignment op.
	 * @param right right-hand side of the assignment op.
	 * @return nothing, the result is stored in the "raw_mapping" hashmap.
	 * 
	 */
	private void extractMappingFromStatement(Stack<String> ifeq, String left, String right) throws Exception
	{
		List<String> target_list = extractTargetsFromRHS(right.trim());
		
		createMappingForSymbolPair(left, target_list);
		
		if(!ifeq.isEmpty())
		{	//deal if existing stack 
			for(String s : ifeq)
			{
				List<String> alt = new ArrayList<String>();
				alt.addAll(target_list);
				createMappingForSymbolPair(s, alt);
			}
		}
		return;
	}

	public List<String> extractTargetsFromRHS(String rhs) throws Exception
	{
		List<String> target_list = new ArrayList<String>();

		//@Linux specific calls. 
		PropReader r = new PropReader();
		boolean isLinux = r.getProperty("repo.path").contains("linux");
		
		if(isLinux)
		{
			rhs = linuxSpecificTargetCleanUp(rhs);
		}
		
		String[] targets = rhs.split("\\s+"); //split along white spaces.
		for(int i = 0 ; i < targets.length; i++)
		{
			String target = targets[i].trim();
			
			target = removeVariablePrefix(target);//Remove weird variable depend construct around compilation units as this is confuse further attempt to map them to proper files.

			if(target.startsWith("$(")) //avoiding remaining noise.
				continue;
			
			target_list.add(target);
		}
		return target_list;
	}
	
	private String linuxSpecificTargetCleanUp(String s)
	{

		
		if(!s.contains("ifeq"))
		{
			//specific function calls in Makefiles 
			if(s.contains("$(call cc-option"))
			{
				s = s.replace("$(call cc-option", "");
				s = s.replace(")", "");
			}
			if(s.startsWith("$(obj)"))
			{
				s = s.replace("$(obj)", "");
			}
			s = s.replace(","," ");
		}

		return s.trim();
	}
	
	private String removeVariablePrefix(String target)
	{
		if(target.endsWith(".o") && target.startsWith("$(") )
			target = target.substring(target.indexOf(")")+1,target.length());
		return target;
	}

	/**
	 * Returns the assignment operator used in this build script line.
	 * Returns null if none are found.
	 * 
	 * @param l the line to scan
	 * @return ":=", or equivalent, null if no operator is found.
	 */
	private String getAssignmentOperator(String l)
	{
		String equality;
		if(l.indexOf(":=") != -1)
		{
			equality = ":=";
		}
		else if (l.indexOf("+=") != -1)
		{
			equality = "+="; 
		}
		else if (l.indexOf("-=") != -1)
		{
			equality = "-="; 
		}
		else if (l.indexOf("=") != -1)
		{
			equality = "="; 
		}
		else
		{
			equality = null;
		}
		return equality;
	}

	
	private List<String> createMappingForSymbolPair(String left, List<String> target_list) 
	{
		
		List<String> existing_mapping = raw_mapping.get(left);
		if(existing_mapping != null)
		{
			raw_mapping.get(left).addAll(target_list);
		}
		else
		{
			raw_mapping.put(left, target_list);
		}
		return existing_mapping;
	}
	
	/**
	 * Completes the current line obtained from the reader "r", if necessary.
	 * 
	 * Checks if the current line finishes with a line continuation symbol, and if so,
	 * appends the following line (as often as necessary) by reading it from "r". 
	 * 
	 * @param r the reader from which lines are read (must be initialized and running)
	 * @param l the current line
	 * @return the complete line, with continuation if need be, and updated reader accordingly.
	 * 
	 * @throws Exception
	 */
	private String resolve_line_continuation(BufferedReader r, String l) throws Exception
	{
		while(l.endsWith("\\"))
		{			//line continuation first.
			try{
				l = l.substring(0, l.lastIndexOf("\\"));
				l+= " ";
				String tmp = r.readLine();
				if(tmp != null)
				{
					l = l+tmp.trim();	
				}
			}
			catch(Exception e)
			{
				throw new Exception("error on line : "+  l, e);
			}
		}
		return l;
	}

}
