package fever.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import fever.PropReader;
import fever.enhancer.Enhancer;
import fever.graph.model.TimeLine.TimeLineNodeGenerator;

@SuppressWarnings("unused")
public class Main 
{
	//soletta : introduction of Kconfig system (for future refs: 78569ee01c81d1292bcacc3372de6792a5c6e617 and to setup the mapping/feature extraction)

	public static String start_commit = "6a13feb9c82803e2b815eca72fa7a9f5561d7861"; 
	public static String end_commit = "afd2ff9b7e1b367172f18ba7f693dfb62bdcb2dc";
	//

	public static String prop_file = "resources/settings.properties";
	
	public static List<String> Makefile_GuardedVars = new ArrayList<String>(); 
	
	static public void main(String[] args) throws Exception
	{
		File file = new File("extraction_errors.txt");
		FileOutputStream fos = new FileOutputStream(file);
		PrintStream ps = new PrintStream(fos);
		System.setErr(ps);

		PropReader.setPropReaderFile(prop_file);

		Makefile_GuardedVars.add("obj-y");
		Makefile_GuardedVars.add("lib-y");
		Makefile_GuardedVars.add("ccflags-y");
		Makefile_GuardedVars.add("asflags-y");
		Makefile_GuardedVars.add("ldflags-y");
		
		
		extractCommitRange(start_commit, end_commit);
		
		TimeLineNodeGenerator.main(null);
		
		Enhancer.update_db = true;
		Enhancer.dump_info = false;
		
		Enhancer.main(null);
		
		
		System.out.println("Done");
	}
	
	/**
	 * Main  method used to import dataset from CVS files: 
	 * 	the file must contain: 
	 * 	feature name;commitId;<at least one another value>
	 * 
	 * The program then takes each commit and extract the feature oriented changes
	 * from it and stores everything into the Neo4J database.
	 * 
	 * @param file_path path to the CSV file
	 * @throws Exception if extraction/import goes wrong.
	 */
	static public void extractFromCSVRefFile(String file_path) throws Exception
	{
		DataExtractor de = new DataExtractor();
		
		File commitFile = new File(file_path);
		BufferedReader reader = new BufferedReader(new FileReader(commitFile));
		
		String line = "";
		List<String> commit_ids = new ArrayList<String>();

		while(line != null)
		{
			line = reader.readLine();
			if(line!= null && !line.isEmpty() && line.contains(";"))
			{
				String id  = line.split(";")[1];
				commit_ids.add(id);
			}
		}
		reader.close();
		de.extractCommitInfo(commit_ids);		
	}
	
	
	/**
	 * Extract feature oriented changes from all commits contained within 
	 * the commit range defined by the Start and End commit id (included).
	 * Each commit is analyzed and all the commits are then related to one another based on the commit sequence.
	 * 
	 * Note: the set of analyzed commits is created based on all the commits that can be reached
	 * starting from "Start" and ``walking'' until the "End" commit - this depends on the topological 
	 * organization of the commits.
	 *  
	 * @param start commit from which to start
	 * @param end the commit where we'll stop
	 * @throws Exception if the extraction goes wrong.
	 */
	static public void extractCommitRange(String start, String end) throws Exception
	{
		DataExtractor de = new DataExtractor();
		de.extractCommitInfo(start, end);
	}
	
	
	/**
	 * Extract information from a single commit 
	 * 
	 * @param commitId the id of the commit to analyze
	 * @throws Exception if the extraction goes wrong.
	 */
	static public void extractSingleCommit(String commitId) throws Exception
	{
		DataExtractor de = new DataExtractor();
		List<String> commit_ids = new ArrayList<String>();
		commit_ids.add(commitId);
		de.extractCommitInfo(commit_ids);
	}
}
