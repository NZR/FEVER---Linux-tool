package fever.graph.model.TimeLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingTable
{
	static Map<String,List<String>> feature_file_mapping = new HashMap<String,List<String>>();
	static Map<String,List<String>> file_feature_mapping = new HashMap<String,List<String>>();
	
	public MappingTable()
	{
		
	}
	
	public void loadMappingFromFile(String file_path) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(file_path));
		String line = null;
		while(( line = br.readLine() ) != null)
		{
			String[] args = line.split(" :: ");
			String feature_name= args[0];
			if(args.length != 2)
				continue;
			
			String[] targets = args[1].split("=");
			List<String> files = new ArrayList<String>();
			for(int i = 0; i < targets.length ; i ++)
			{
				if(targets[i] != null && targets[i].length() != 0 && !files.contains(targets[i]))
					files.add(targets[i]);
			}
			
			for(String t : files)
				addEntry(feature_name,t);
		}
		br.close();
	}
	
	public List<String> getFeaturesForFile(String file)
	{
		List<String> mapped_feature_names = file_feature_mapping.get(file);
		if(mapped_feature_names == null || mapped_feature_names.isEmpty())
			mapped_feature_names =  file_feature_mapping.get(file+".o");
		if(mapped_feature_names == null || mapped_feature_names.isEmpty())
			mapped_feature_names = file_feature_mapping.get("/"+file);
		
		
		if(mapped_feature_names == null || mapped_feature_names.isEmpty())
		{
			String guard = findGuardFeatureForFile(file);
			if(guard != null && guard.length() != 0 )
			{
				mapped_feature_names = new ArrayList<String>();
				mapped_feature_names.add(guard);
			}
		}
		return mapped_feature_names;
	}
	
	public void addEntry(String f_name, String file_name)
	{
		if(file_name.contains("."))
			file_name= file_name.substring(0,file_name.lastIndexOf("."));
		
		List<String> known_features = file_feature_mapping.get(file_name);
		
		if(known_features == null)
		{
			known_features = new ArrayList<String>();
			known_features.add(f_name);
			file_feature_mapping.put(file_name, known_features);
		}
		else if(!known_features.contains(f_name))
		{
			known_features.add(f_name);
		}
		
		List<String> mapped_files = feature_file_mapping.get(f_name);
		if(mapped_files==null)
		{
			mapped_files = new ArrayList<String>();
			mapped_files.add(file_name);
			feature_file_mapping.put(f_name, mapped_files);
		}
		else
		{
			if(!mapped_files.contains(file_name))
				mapped_files.add(file_name);
		}
	}
	
	public void removeEntry(String f_name, String file_name)
	{
		if(file_name.contains("."))
			file_name= file_name.substring(0,file_name.lastIndexOf("."));
		
		List<String> known_features = file_feature_mapping.get(file_name);
		
		if(known_features != null && known_features.contains(f_name))
		{
			known_features.remove(f_name);
		}
		
		List<String> mapped_files = feature_file_mapping.get(f_name);
		if(mapped_files!=null && mapped_files.contains(file_name))
		{
			mapped_files.remove(file_name);
		}
	}
	
	
	public String findGuardFeatureForFile(String p)
	{
		String path = p.replace("_guarded_","");
		if(!path.startsWith("/"))
			path = "/"+path;
		
		List<String> guard_name = file_feature_mapping.get(path);
		if((guard_name == null) && path.endsWith("/"))
			path = path.substring(0,path.length() -1);
		guard_name = file_feature_mapping.get(path);
		
		if(guard_name == null)
		{
			path = path.substring(0, path.lastIndexOf("/")+1);
			guard_name = file_feature_mapping.get(path);
			if((guard_name == null) && path.endsWith("/"))
				path = path.substring(0,path.length() -1);
			guard_name = file_feature_mapping.get(path);
		}
		
		//LINUX SPECIFIC HACK : we know ./arch/xxx folders are guarded by xxx. 
		if(p.startsWith("arch/") && guard_name == null)
		{
			String hack = p.replace("arch/", "");
			if(hack.indexOf("/")!= -1) //otherwise, we are in ./arch/. directly, and there won't be any features for that.
			{
				hack = hack.substring(0,hack.indexOf("/"));
				hack = hack.toUpperCase();
				guard_name = new ArrayList<String>(); 
				guard_name.add(hack);
			}
		}

		if(guard_name == null)
			return null;
		else
			return guard_name.get(0);
	}

	
}
