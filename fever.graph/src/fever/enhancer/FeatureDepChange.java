package fever.enhancer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a change to feature dependency, with its associated code level include change.
 * 
 * This record the change for a single feature, and for one "change" operation: added or removed.
 * If a feature has a new dep, and one removed in a single commit, this will result in the creation of 2 of such FeatureDepChange.
 * 
 * 
 * @author Dante
 *
 */
public class FeatureDepChange
{
	
	String feature = ""; 
	String subsystem = ""; 
	String commit_id = "";
	
	List<String> added_targets = new ArrayList<String>();
	List<String> removed_targets = new ArrayList<String>();
	
	
	public void print()
	{
		System.out.println("feature name : " + feature);
		System.out.println("added dependencies : ");
		for(String s : added_targets)
		{
			System.out.println("\t" +s );
		}
		System.out.println("removed dependencies : ");
		for(String s : removed_targets)
		{
			System.out.println("\t" +s );
		}
		
	}
}
