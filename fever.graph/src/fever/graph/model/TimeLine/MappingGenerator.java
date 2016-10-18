package fever.graph.model.TimeLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fever.graph.Main;
import fever.PropReader;
import fever.parsers.CommitInfoExtractor;
import fever.parsers.build.BuildScriptBuilder;
import fever.utils.FeatureUtils;
import fever.utils.ParsingUtils;
import models.Feature;
import models.VariabilityModel;
import models.VariabilityModelEntity;

public class MappingGenerator
{
	static List<String> file_names = new ArrayList<String>();
	
	static List<String> makefiles_to_parse = new ArrayList<String>();
	static List<String> virtual_features = new ArrayList<String>();
	
	static List<String> configfiles_to_parse = new ArrayList<String>(); 
	static List<String> feature_names = new ArrayList<String>();
	
	static Map<String, List<String>> feature_file_mapping = new HashMap<String, List<String>>();
	
	static String repo_p	 = ""; // read from property files.
	
	public static final String feats_out = 		"initial_features_4.0.txt";
	public static final String mapping_out = 	"intial_mapping_4.0.txt";
	
	
	public static void  main (String[] args) throws Exception
	{
		launch(args);
	}
	
	
	public static void launch(String[] args) throws Exception
	{
		PropReader.setPropReaderFile(Main.prop_file);
		PropReader read = new PropReader();
		String path = read.getProperty("repo.path");
		repo_p = path;
		Path p = Paths.get(path);
		
		
		FileVisitor<Path> fv = new SimpleFileVisitor<Path>()
		{
			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				file_names.add(file.toString().replace(repo_p, ""));
				if (ParsingUtils.isBuildFile(file.getFileName().toString()))
				{
					makefiles_to_parse.add(file.toString());
				}
				if (ParsingUtils.isVariabilityFile(file.getFileName().toString()))
				{
					configfiles_to_parse.add(file.toString());
				}
				//System.out.println(file.toString().replace(repo_p, ""));
				return FileVisitResult.CONTINUE;
			}
		};
		try
		{
			Files.walkFileTree(p, fv);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new Exception("couldn't find all find the files. Aborting.");
		}
		
		System.out.println("Got the files. starting extraction.");
		
		extractFeatureNames();
		dumpFeatureNames();
		
		
		extractBuildMapping();
		resolveVirtualMappings();
		dumpMapping();
	}

	private static void dumpFeatureNames() throws Exception
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(feats_out));
		for(String f : feature_names)
		{
			bw.append(f+"\n");
		}
		bw.flush();
		bw.close();
	}

	private static void extractFeatureNames() throws Exception
	{
		for(String file_name : configfiles_to_parse)
		{

			File f = new File(file_name);			
			CommitInfoExtractor cie = new CommitInfoExtractor();
			VariabilityModel vm = cie.buildFMFromKconfigFile(0, f);
			
			List<VariabilityModelEntity> feats = vm.getFeatures();
			for(VariabilityModelEntity vme : feats)
			{
				if(vme instanceof Feature)
				{
					Feature feature = (Feature) vme;
					String n = feature.getName();
					if(!feature_names.contains(n))
						feature_names.add(n);
				}
				else
				{
					continue;
				}
			}
		}
	}

	private static void dumpMapping() throws IOException
	{
		File f = new File(mapping_out);
		f.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (String f_name : feature_file_mapping.keySet())
		{
			bw.append(f_name + " :: ");
			for (String s : feature_file_mapping.get(f_name))
			{
				bw.append(s + "=");
			}
			bw.append("\n");
			bw.flush();
		}
		bw.flush();
		bw.close();
	}
	
	private static void resolveVirtualMappings()
	{
		for (String vf : virtual_features)
		{
			String folder_name = vf.replace("VIRTUAL_FEATURE_", "");
			folder_name += "/";
			for (String feature : feature_file_mapping.keySet())
			{
				if (feature.startsWith("VIRTUAL"))
					continue;
				List<String> targets = feature_file_mapping.get(feature);
				boolean got_it = false;
				for (String t : targets)
				{
					if (t.equals(folder_name))
					{
						got_it = true;
					}
				}
				if (got_it)
				{
					feature_file_mapping.get(feature).addAll(feature_file_mapping.get(vf));
					break; // only one guard feature per folder. Hopefully.
				}
			}
		}
		for (String vf : virtual_features)
		{
			feature_file_mapping.remove(vf);
		}
	}
	
	private static void extractBuildMapping() throws Exception
	{
		for (String make_file_path : makefiles_to_parse)
		{
			File file = new File(make_file_path);
			String make_file_folder = file.getAbsolutePath().replace(repo_p, "");
			if (make_file_folder.contains("/"))
				make_file_folder = make_file_folder.substring(0, make_file_folder.lastIndexOf("/"));
			
			BuildScriptBuilder m = new BuildScriptBuilder();
			m.buildModelFromFile(new File(make_file_path),make_file_path);
			debug(m);
			for (String symbol : m.raw_mapping.keySet())
			{
				List<String> feature_names = FeatureUtils.getFeatureNames(symbol);
				if (!feature_names.isEmpty())
				{
					extractFeatureMapping(make_file_folder, m, symbol, feature_names);
				}
				else if (Main.Makefile_GuardedVars.contains(symbol)) //(symbol.equals("obj-y") || symbol.equals("lib-y"))
				{
					String virtual_symbol = "VIRTUAL_FEATURE_" + make_file_folder;
					if (!virtual_features.contains(virtual_symbol))
						virtual_features.add(virtual_symbol);
					List<String> virtual_features = new ArrayList<String>();
					virtual_features.add(virtual_symbol);
					extractFeatureMapping(make_file_folder, m, symbol, virtual_features);
				}
			}
		}
	}
	
	private static void debug(BuildScriptBuilder m)
	{
		System.out.println("==TEMP DEBUG OUTPUT");
		for (String s : m.raw_mapping.keySet())
		{
			System.out.print(s + "=> ");
			for (String s2 : m.raw_mapping.get(s))
				System.out.print(s2 + " ");
			System.out.println();
		}
	}
	
	private static void extractFeatureMapping(String make_file_folder, BuildScriptBuilder m, String symbol, List<String> feature_names)
	{
		List<String> targets = m.raw_mapping.get(symbol);
		List<String> targeted_files = new ArrayList<String>();
		for (String s : targets)
		{
			if (ParsingUtils.hasSourceFileExtension(s)) // obj file, compilation unit, I should find a file associated with it.
			{
				String potential_file = s.substring(0, s.lastIndexOf("."));
				potential_file = make_file_folder + "/" + potential_file;
				for (String f_name : file_names)
				{
					if (!f_name.contains("."))
						continue;
					if (f_name.substring(0, f_name.lastIndexOf(".")).equals(potential_file))
					{
						targeted_files.add(f_name);
					}
				}
			}
			else if (s.endsWith("/"))
			{
				targeted_files.add(make_file_folder + "/" + s);
			}
		}
		for (String feature : feature_names)
		{
			if (feature_file_mapping.containsKey(feature))
			{
				feature_file_mapping.get(feature).addAll(targeted_files);
			}
			else
			{
				feature_file_mapping.put(feature, targeted_files);
			}
		}
	}
}
