package fever.enhancer;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;

import fever.graph.model.FeatureEditNode;

public class FeatureDescParser
{
	
	List<String> features = new ArrayList<String>();
	
	
	public FeatureDescParser(String pc, String select, String depends, String default_value)
	{
		extractFeatures(pc);
		extractFeatures(select);
		extractFeatures(depends);
		extractFeatures(default_value);		
	}
	
	
	
	public FeatureDescParser(Node n)
	{
		
		String select = (String)n.getProperty("selects");
		String depends = (String)n.getProperty("depends on");
		String default_value  = (String)n.getProperty("default_values");
		String visibility_condition = (String)n.getProperty(FeatureEditNode._visibility_condition);
		
		extractFeatures(select);
		extractFeatures(depends);
		extractFeatures(default_value);
		extractFeatures(visibility_condition);
	}
	
	
	private void extractFeatures(String attr)
	{
		if (attr == null || attr.length() == 0)
			return;
		
		attr = removeSpecialChars(attr);
		
		String[] attrs = attr.split(" ");
		
		for(int i = 0; i < attrs.length ; i++)
		{
			String var = attrs[i].trim();
			if(var == null || var.length() == 0 )
				continue;
			
//			if(!var.toUpperCase().equals(var))
//				continue; //assumption: feature names will be all upper case. so "toUpper" shouldn't change the string.
			if(var.contains(".") || var.contains("0x"))
				continue;
//			
			if(isNumeric(var))
				continue;
			
			if(!features.contains(var) && !var.equals("__UNRESOLVED_REFERENCE__") && var.length() > 1)
					features.add(var);
		}
	}
	
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	private String removeSpecialChars(String attr)
	{
		attr = attr.replace("(", " ");
		attr = attr.replace(")", " ");
		attr = attr.replace("|", " ");
		attr = attr.replace("&", " ");
		attr = attr.replace("\'", "");
		attr = attr.replace("=", "");
		attr = attr.replace("!", "");
		attr = attr.replace("[", " ");
		attr = attr.replace("]", " ");
		
		attr = attr.replace("if", "");
		attr = attr.replace("y", "");
		attr = attr.replace("n", "");
		attr = attr.replace("m", "");
		attr = attr.replace(" Y ", "");
		attr = attr.replace(" N ", "");
		
		return attr;
	}



	public List<String> getFeatures()
	{
		return features;
	}
	
	
	public List<String> getRemovedFeatures(FeatureDescParser reference)
	{
		List<String> removed_features = new ArrayList<String>();
		
		List<String> ref_features = reference.getFeatures();
		
		for(String s : ref_features)
			if(!features.contains(s))
				removed_features.add(s);
		
		return removed_features;
	}
	
	public List<String> getAddedFeatures(FeatureDescParser reference)
	{
		List<String> added_features = new ArrayList<String>();
		
		List<String> ref_features = reference.getFeatures();
		
		for(String s : features)
			if(!ref_features.contains(s))
				added_features.add(s);
		
		return added_features;
	}
}
