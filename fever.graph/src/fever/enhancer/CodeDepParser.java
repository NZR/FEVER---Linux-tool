package fever.enhancer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Node;

public class CodeDepParser
{

	
	//private String[] include_blacklist = {"init.h","module.h","kernel.h" };
	private String[] include_blacklist = {};
	private String txt = "";
	
	List<String> addedLines = new ArrayList<String>(); 
	List<String> removedLines = new ArrayList<String>();
	

	List<String> addedIncludes = new ArrayList<String>(); 
	List<String> removedIncludes = new ArrayList<String>();
	
	Pattern added_includes = Pattern.compile("(\\+\\s?(#include\\s+?(<|\")(\\S*)(\"|>)))");
	Pattern removed_includes = Pattern.compile("(\\-\\s?(#include\\s+?(<|\")(\\S*)(\"|>)))");
	
	public CodeDepParser(Node codeEditNode) throws Exception
	{
		try{
			txt = (String) codeEditNode.getProperty("Content");
		}
		catch(Exception e)
		{
			
			try{
				txt = (String) codeEditNode.getProperty("Diff");
			}
			catch(Exception ex)
			{
				throw new Exception("Failed to identify content of diff in the database for node : " + codeEditNode.getId() + "\n Make sure this node is a code Edit node. The process will stop here.");
			}
		}
		
		if(txt.length()!= 0) //no point in extracting stuff if there is no content.
			extractLineChanges();
	}
	
	public CodeDepParser(String text)
	{
		txt = text;
		extractLineChanges();
	}
	
	private void extractLineChanges()
	{

		txt = txt.replaceFirst("@@.*@@", "");

		addedIncludes.addAll(extractIncs(added_includes,txt));
		removedIncludes.addAll(extractIncs(removed_includes,txt));
		
	}
	
	private List<String> extractIncs(Pattern p,String txt)
	{
		List<String> targets = new ArrayList<String>();
		Matcher m = p.matcher(txt);
		while (m.find())
		{
			boolean sys = false;
			
			String g = m.group();
			if(g.contains("<") && g.contains(">"))
				sys = true;
			
			g = g.replace("+", "");
			g = g.replace("#include", " ");
			g = g.replace("- ", "");
			g = g.replace("\"", "");
			g = g.replace("<", "");
			g = g.replace(">", "");
			
			boolean dont = false;
			
			for(String s : include_blacklist )
			if(g.contains(s))
			{
				dont = true;
			}
			
			if(!dont) targets.add((sys?"sys-":"local-" )+ g.trim());
		}
		return targets;
	}
	
	public List<String> getAddedIncludes()
	{
		return addedIncludes;
	}
	
	public List<String> getRemovedIncludes()
	{
		return removedIncludes;
	}
	
	
	
	
	
}
