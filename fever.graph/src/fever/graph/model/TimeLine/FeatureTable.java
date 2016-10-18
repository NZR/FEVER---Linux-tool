package fever.graph.model.TimeLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class FeatureTable
{
	static List<String> feature_names = new ArrayList<String>(); 
	public FeatureTable()
	{
		
	}

	public void loadFeatures(String file) throws Exception
	{
		BufferedReader r = new BufferedReader (new FileReader(file));
		String l = null; 
		while( null!= (l = r.readLine()))
		{
			 l = l.trim();
			 if(!feature_names.contains(l))
				 feature_names.add(l);
		}
		r.close();
	}

	public boolean hasFeature(String f_name)
	{
		return feature_names.contains(f_name);
	}

	public void addFeature(String f_name)
	{
		if(f_name!=null && f_name.length() > 0 && !feature_names.contains(f_name))
		{
			feature_names.add(f_name);
		}
		
	}
	
	
}
