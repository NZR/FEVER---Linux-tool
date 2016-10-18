package fever.enhancer;

import java.util.ArrayList;
import java.util.List;

public class CodeDepChange
{
	
	
	public String artefact_name = ""; 
	public String feature_name = ""; 
	public String subsystem = ""; 
	public List<String> added_includes = new ArrayList<String>();
	public List<String> removed_includes = new ArrayList<String>();
	public String commit_id = "";
	
	public void print()
	{
		System.out.println("feature : " + feature_name);
		
		System.out.println("new includes  : ");
		for(String s : added_includes)
			System.out.println("\t"+s);
		
		System.out.println("removed includes  : ");
		for(String s : removed_includes)
			System.out.println("\t"+s);
		
	}
	
}
