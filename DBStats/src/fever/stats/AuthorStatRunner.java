package fever.stats;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.utils.DBConnection;

public class AuthorStatRunner
{
	public static final String rel = "4.3";

	public static String o_collab  = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/feature_collab_"+rel+".csv";
	public static String o_expertise = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/feature_expertise_"+rel+".csv";
	public static String o_spaces = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/edited_space_"+rel+".csv";
	public static String o_author_details = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/author_details_"+rel+".csv";
	public static String o_feature_details = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/feature_details_"+rel+".csv";
	public static String o_features_per_commit = "/Users/Dante/Documents/workspace/tools/neo4j-2.2.5/extracts/feature_per_commit_"+rel+".csv";
	
	public static void main(String[] args) throws Exception
	{
		GraphDatabaseService s = DBConnection.getService();
		getAuthorStats(s);
		FeatureTimeLineBasedStats.dumpFeatureLvlInfo();
		
//		
//		
//		System.out.println("rel: "+rel);
//		getAuthorStats(s);
//
//		Long nb_timeline = (long) 0.0; 
//		try(Transaction tx = s.beginTx())
//		{
//			String q = "match (t:TimeLine) return count (distinct t);";
//			Result r = s.execute(q);
//			while(r.hasNext())
//			{
//				Map<String,Object> row = r.next();
//				nb_timeline = ((Long)row.get("count (distinct t)"));
//			}
//		}
//		catch(Exception e)
//		{
//			System.out.println(e.getMessage());
//		}
//		System.out.println("nb timelines : " + nb_timeline);


//		BufferedWriter bw_expertise = new BufferedWriter(new FileWriter (o_expertise));
//		BufferedWriter bw_author_details = new BufferedWriter(new FileWriter(o_author_details));
//		
//		try(Transaction tx = s.beginTx())
//		{
//			String q = "match (t:TimeLine)-->()<--(c:Commit) return c.author, count (distinct t.name); ";
//			//this one covers everything : core and influence update.
//			// author <--> feature : 
//			//		the author must know about this feature
//			
//			Result r = s.execute(q);
//			while(r.hasNext())
//			{
//				Map<String,Object> row = r.next();
//				nb_timeline = (Long)(row.get("count (distinct t.name)"));
//				bw_expertise.append(Long.valueOf(nb_timeline).toString()+"\n");
//				bw_expertise.flush();
//				String who = (String)(row.get("c.author"));
//				bw_author_details.append(who +"," + nb_timeline+"\n");
//			}
//		}
//		catch(Exception e)
//		{
//			System.out.println(e.getMessage());
//		}
//		
//		bw_expertise.close();
//		bw_author_details.flush();
//		bw_author_details.close();
//		
//		
//		BufferedWriter bw_collab = new BufferedWriter(new FileWriter (o_collab));
		
//		
//		try(Transaction tx = s.beginTx())
//		{
//			String q = "match (t:TimeLine)-->()<--(c:Commit) return t.name, count (distinct c.author);";
//			Result r = s.execute(q);
//			while(r.hasNext())
//			{
//				Map<String,Object> row = r.next();
//				nb_timeline = ((Long)row.get("count (distinct c.author)"));
//				bw_collab.append(Long.valueOf(nb_timeline).toString() +"\n");
//				bw_collab.flush();
//			}
//		}
//		catch(Exception e)
//		{
//			
//		}
//		bw_collab.flush();
//		bw_collab.close();

		
		
		
		//feature evolution in spaces. Which features evolves only through the VM/Build/Code and so forth. 
		// the above code gives us this info per authors - not per feature.

//		BufferedWriter bw_feature_details = new BufferedWriter(new FileWriter(o_feature_details));
//		bw_feature_details.flush();
//		bw_feature_details.close();
	



		//expertise detail - split influence update and core update. 
		// looking for : core updates, to start with
		//	influence update overlapping with core update
		// 	if core update on feature x, for influence of y, and y in core update 
		//	give y a higher expertise score ?
//		BufferedWriter bw_fpc = new BufferedWriter(new FileWriter(o_features_per_commit));
//
//		try(Transaction tx = s.beginTx())
//		{
//			String query = "match (c:Commit)-->()<--(t:TimeLine) return c, count (distinct t)";
//			Result r = s.execute(query);
//			while(r.hasNext())
//			{
//				Map<String,Object> row = r.next();
//				nb_timeline = ((Long)row.get("count (distinct t)"));
//				bw_fpc.append(Long.valueOf(nb_timeline).toString() +"\n");
//				bw_fpc.flush();
//			}
//			tx.success();
//			tx.close();
//		}
//		catch(Exception e)
//		{
//		}
//		bw_fpc.close();
	}
	
	private static void appendAuthorsToFile(List<String> authors, String file) throws Exception
	{
		BufferedWriter bw = new BufferedWriter( new FileWriter(file,true));		
		for(String s: authors)
		{
			bw.append(s+"\n");
		}
		bw.flush();
		bw.close();
	}

	private static void getAuthorStats(GraphDatabaseService s) throws Exception
	{		
		//let's first fill those up, and then we split them based 
		//on the list in which they appear
		List<String> source_authors = new ArrayList<String>();
		List<String> build_authors = new ArrayList<String>();
		List<String> vm_authors = new ArrayList<String>();
		
		List<String> authors = new ArrayList<String>();
		
		//for a release		
		findAuthors(s,authors);
			appendAuthorsToFile(authors , FilePath.o_authors);
		
		findVMAuthors(s, vm_authors);
			appendAuthorsToFile(vm_authors ,FilePath.o_authors_in_vm);
		//dump vm authors? 
			
		findBuildAuthors(s, build_authors);
			appendAuthorsToFile(build_authors , FilePath.o_authors_in_build);
		//dump buid authors?
			
		findSourceAuthors(s, source_authors);
			appendAuthorsToFile(source_authors , FilePath.o_authors_in_source);
		//dump authors authors?

		
//		List<String> source_only_authors = new ArrayList<String>();
//		List<String> vm_only_authors = new ArrayList<String>();
//		List<String> build_only_authors = new ArrayList<String>();
//		List<String> vm_and_build_authors = new ArrayList<String>();
//		List<String> vm_and_source_authors = new ArrayList<String>();
//		List<String> source_and_build_authors = new ArrayList<String>();
//		List<String> all_spaces = new ArrayList<String>();
//		
//		
//		for(String a : authors)
//		{
//			if(source_authors.contains(a) && build_authors.contains(a) && vm_authors.contains(a))
//			{
//				all_spaces.add(a);
//			}
//			else if ( source_authors.contains(a) && build_authors.contains(a) )
//			{
//				source_and_build_authors.add(a);
//			}
//			else if (build_authors.contains(a) && vm_authors.contains(a))
//			{
//				vm_and_build_authors.add(a);
//			}
//			else if (source_authors.contains(a) && vm_authors.contains(a))
//			{
//				vm_and_source_authors.add(a);
//			}
//			else if (source_authors.contains(a))
//			{
//				source_only_authors.add(a);
//			}
//			else if ( vm_authors.contains(a))
//			{
//				vm_only_authors.add(a);
//			}
//			else if (build_authors.contains(a))
//			{
//				build_only_authors.add(a);
//			}
//		}
//		
//		BufferedWriter bw_edits = new BufferedWriter(new FileWriter (o_spaces));
//		
//		bw_edits.append("label:value\n");
//		bw_edits.append("all spaces:" +   all_spaces.size()+"\n");
//		bw_edits.append("vm+build spaces:" +   vm_and_build_authors.size() + "\n");
//		bw_edits.append("vm+source spaces:" +   vm_and_source_authors.size() + "\n");
//		bw_edits.append("build+source spaces:" +   source_and_build_authors.size() + "\n");
//		bw_edits.append("only the vm space:" +   vm_only_authors.size() + "\n");
//		bw_edits.append("only the build space:" +   build_only_authors.size() + "\n");
//		bw_edits.append("only the source space:" +   source_only_authors.size() + "\n");
//		bw_edits.flush();
//		bw_edits.close();
		
	}

	
	private static void findAuthors(GraphDatabaseService s, List<String> authors) throws Exception
	{
		try(Transaction tx = s.beginTx())
		{
			Result r = s.execute("match (c:Commit) return distinct c.author;");
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String author = (String)row.get("c.author");
				authors.add(author);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("Query failed",e);
		}
	}
	
	private static void findBuildAuthors(GraphDatabaseService s, List<String> build_authors) throws Exception
	{
		try(Transaction tx = s.beginTx())
		{
			Result r = s.execute("match (c:Commit)-->(a:ArtefactEdit {type:\"build\"}) return distinct c.author;");
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String author = (String)row.get("c.author");
				build_authors.add(author);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("Query failed",e);
		}
	}

	private static void findVMAuthors(GraphDatabaseService s, List<String> source_authors) throws Exception
	{
		try(Transaction tx = s.beginTx())
		{
			Result r = s.execute("match (c:Commit)-->(a:ArtefactEdit {type:\"vm\"}) return distinct c.author;");
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String author = (String)row.get("c.author");
				source_authors.add(author);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("Query failed",e);
		}
	}
	
	private static void findSourceAuthors(GraphDatabaseService s, List<String> source_authors) throws Exception
	{
		try(Transaction tx = s.beginTx())
		{
			Result r = s.execute("match (c:Commit)-->(a:ArtefactEdit {type:\"source\"}) return distinct c.author;");
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String author = (String)row.get("c.author");
				source_authors.add(author);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("Query failed",e);
		}
	}
}
