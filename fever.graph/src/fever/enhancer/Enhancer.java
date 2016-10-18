package fever.enhancer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.model.TimeLine.FeatureTimeLineNode;
import fever.graph.utils.DBConnection;

/**
 * This small program is meant to reformat part of the FEVER graph schema to facilate certain queries - 
 * namely on feature relationships.
 *  
 *  The various program create new nodes, and create new relationships to feature TimeLine objects
 *  It attempts to identify changes in code dependencies, and provide data dump capabilities to facilitate statistical analysis of the data.
 *  
 * @author Dante
 *
 */
public class Enhancer
{
	public static boolean update_db = false; 
	public static boolean dump_info = false;
	
	public static final String OUTPUT_FILE = "complete/changes_315.csv";
	public static final String ITEMS_FILE = "items315.list";
	public static final String OUTPUT_REL_FILE = "rel_change_dump_315.csv";
	
	
	public static void main(String[] args) throws Exception
	{
		//perCommitExtraction();
		perTimeLineExtraction();
		return;
	}


	private static void perTimeLineExtraction() throws Exception
	{
		List<Node> timeLines = getTimeLines();		
		GraphDatabaseService s = DBConnection.getService();
		List<Change> changes = new ArrayList<Change>() ;
		String name = "";

		int nb_iteration = 0;
		for(Node n : timeLines)
		{
			nb_iteration ++;
			
			try(Transaction t = s.beginTx())
			{
				Long id = n.getId();
				Change c = new Change(); 
				name = (String) n.getProperty("name");
				c.feature = name;
				System.out.println("starting iteration : " + nb_iteration + " out of " + timeLines.size());
				System.out.println("starting feature changed : " + name);
				
				getFeatureRelChanges(s, id, c);
				getFeatureAttrs(s,id,c);
//				getDirectCodeChanges(s, id, c);
//				getIndirectCodeChanges(s, changes, id, c);
				
				t.success();
			}
			catch(Exception e )
			{
				throw new Exception("failed to extract info from time line "+name+": " + e.getMessage() );
			}
		}
		
		if(dump_info)
			transactionDump(changes);
	

	
	}


	private static void getFeatureAttrs(GraphDatabaseService s, Long id, Change c) throws Exception
	{
		String query = "match (t:TimeLine)-->(m:MappingEdit) where ID(t) = "+id+" return distinct m";
		
		Result r = s.execute(query);
		while (r.hasNext())
		{
			Map<String,Object> row = r.next();
			Node edit = (Node)row.get("m");
			String type  = DBConnection.getPropValue(edit, "target_type");
			if(type.equals("COMPILATION_UNIT"))
				c.mapped_to_file = true;
			else if (type.equals("FOLDER"))
				c.mapped_to_folder = true;
			else if (type.equals("COMPILATION_FLAG"))
				c.mapped_to_cf = true;
		}

	}


	private static void getFeatureRelChanges(GraphDatabaseService s, Long id, Change c) throws Exception
	{
		String query = "match (t:TimeLine)-->(f:FeatureEdit) where ID(t) = "+ id +" return distinct f" ;
		Result r = s.execute(query);
		while (r.hasNext())
		{
			Map<String,Object> row = r.next();
			Node edit = (Node)row.get("f");
			FeatureUpdate fu = new FeatureUpdate(edit);
			fu.extractInfo();
			
			for(String f_name : fu.added_features)
			{
				if(!c.added_features.contains(f_name))
					c.added_features.add(f_name);
			}

			for(String f_name : fu.removed_features)
			{
				if(!c.removed_features.contains(f_name))
					c.removed_features.add(f_name);
			}
			
			c.Bool = fu.Bool;
			c.Tristate = fu.Tristate;
			c.String_var = fu.String_var;
			c.Visible = fu.Visible;
			
			if(update_db)
				UpdateDB(edit, fu);

		}
		
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("rel_changes", true));
		
		
		
		int added = 0; 
		int removed = 0; 
		int edited = 0; 
		int unchanged = 0;
		
		List<String> added_removed_targets = new ArrayList<String>();
		
		List<String> feature_list = c.added_features;
		for(String f :feature_list)
		{
			String check1 = "match (fe:FeatureEdit) where fe.name=\""+f+"\" return fe.change";
			Result check_results = s.execute(check1);
			boolean hit = false;
			
			while(check_results.hasNext())
			{
				Map<String,Object> row = check_results.next();
				String change = (String)row.get("fe.change");
				hit = true;
				
				if(change.equals("Add"))
				{
					added++;
				}
				else if (change.equals("Remove"))
				{
					removed++;
					added_removed_targets.add(f);
				}
				else if (change.equals("Modify"))
				{
					edited++;
				}
			}
			
			if(!hit)
			{
				unchanged ++;
			}
		}
		if ( c.added_features.size() >0)
		{
			bw.append("added rels,"+c.feature+","+added+","+removed+","+edited+","+unchanged+",");
			
			if(added_removed_targets.size()!=0)
				bw.append("[");
			
			for(String weird_target : added_removed_targets)
			{
				bw.append(weird_target+"-");
			}
			
			if(added_removed_targets.size()!=0)
				bw.append("]\n");
			else
				bw.append("\n");
			
			bw.flush();
		}
		
		
		added = 0; 
		removed = 0; 
		edited = 0; 
		unchanged = 0;
		feature_list = c.removed_features;
		
		for(String f :feature_list)
		{
			String check1 = "match (fe:FeatureEdit) where fe.name=\""+f+"\" return fe.change";
			Result check_results = s.execute(check1);
			boolean hit = false;
			
			while(check_results.hasNext())
			{
				Map<String,Object> row = check_results.next();
				String change = (String)row.get("fe.change");
				hit = true;
				
				if(change.equals("ADDED"))
				{
					added++;
				}
				else if (change.equals("REMOVED"))
				{
					removed++;
				}
				else if (change.equals("MODIFIED"))
				{
					edited++;
				}
			}
			
			if(!hit)
			{
				unchanged ++;
			}
		}
		
		if ( c.removed_features.size() >0)
		{
			bw.append("removed rels,"+c.feature+","+added+","+removed+","+edited+","+unchanged+"\n");
			bw.flush();
		}
		bw.close();
		
	}


	private static void UpdateDB(Node edit, FeatureUpdate fu) throws Exception
	{
		
		//System.out.println("now creating relationships between edits and " + (fu.added_features.size() + fu.removed_features.size()) + " time lines");
		for(String target : fu.added_features)
		{
			Node n = getOrCreateTimeLineForFeature(target);
			
			boolean exists = false;
			Iterable<Relationship> rels = n.getRelationships();
			for(Relationship rel : rels)
			{
				if ( rel.getEndNode().equals(fu.feature_edit))
				{
					exists = true;
					break;
				}
			}
			
			if(!exists)
			{
				Relationship rel = n.createRelationshipTo(edit, FeatureTimeLineNode.Relationships.FEATURE_INFLUENCE_UPDATE);
				rel.setProperty("change", "ADDED");
			}
		}
		
		for(String target : fu.removed_features)
		{
			Node n = getOrCreateTimeLineForFeature(target);
			boolean exists = false;
			
			Iterable<Relationship> rels = n.getRelationships();
			for(Relationship rel : rels)
			{
				if ( rel.getEndNode().equals(fu.feature_edit))
				{
					exists = true;
					break;
				}
			}
			
			if(!exists)
			{
				Relationship rel = n.createRelationshipTo(edit, FeatureTimeLineNode.Relationships.FEATURE_INFLUENCE_UPDATE);
				rel.setProperty("change", "REMOVED");
			}
		}
	}


	private static Node getOrCreateTimeLineForFeature(String target) throws Exception
	{
		
		GraphDatabaseService s = DBConnection.getService();
		Node n = null;
		try(Transaction tx = s.beginTx())
		{
			
			String query = "match (t:TimeLine) where t.name = \""+target+"\" return t;";
			
			Result r = s.execute(query);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				n = (Node)row.get("t");
			}
			
			if(n == null)
			{
				n = s.createNode(DynamicLabel.label("TimeLine"));
				n.setProperty("name", target);
			}
			tx.success();
		}
		catch(Exception e)
		{
			System.err.println("failed to get or create time line node for feature " + target +" with error : " + e.getMessage());
			System.err.println("aborting ... ");
			throw e;
		}
		
		return n;
	}


	private static void getDirectCodeChanges(GraphDatabaseService s, Long id, Change c) throws Exception
	{
		Result r;
		String query2 = "match (t:TimeLine)-[:FEATURE_CORE_UPDATE]->(a:ArtefactEdit)-[:DIFF]->(c:LineEdit) where ID(t) = "+id+" return c";
		r = s.execute(query2);
		while (r.hasNext())
		{
			Map<String,Object> row = r.next();
			Node edit = (Node)row.get("c");
			CodeDepParser fu = new CodeDepParser(edit);
			
			for(String inc : fu.getAddedIncludes())
			{
				if(!c.added_includes.contains(inc))
					c.added_includes.add(inc);
			}
			
			for(String inc : fu.getRemovedIncludes())
			{
				if(!c.removed_includes.contains(inc))
					c.removed_includes.add(inc);
			}
		}
	}


	private static void getIndirectCodeChanges(GraphDatabaseService s, List<Change> changes, Long id, Change c) throws Exception
	{
		Result r;
		String query3 = "match (t:TimeLine)-[:FEATURE_CORE_UPDATE]->(a:ArtefactEdit)<-[:IN]-(ce:SourceEdit)-[:EDITED_BY]->(c:LineEdit) "
				+ "where ID(t) = "+id+" return c";

		r = s.execute(query3);
		while (r.hasNext())
		{
			Map<String,Object> row = r.next();
			Node edit = (Node)row.get("c");
			CodeDepParser fu = new CodeDepParser(edit);
			
			for(String inc : fu.getAddedIncludes())
			{
				if(!c.added_includes.contains(inc))
					c.added_includes.add(inc);
			}
			
			for(String inc : fu.getRemovedIncludes())
			{
				if(!c.removed_includes.contains(inc))
					c.removed_includes.add(inc);
			}
		}
		changes.add(c);
	}


//	private static void dumpIncludes(List<String> added_includes, List<String> removed_includes)
//	{
//		System.out.println("includes:\n");
//		for(String b : added_includes)
//		{
//			System.out.print("\""+b.replace("/",".")+"=\",");
//		}
//		for(String b : removed_includes)
//		{
//			System.out.print("\""+b.replace("/",".")+"=\",");
//		}
//	}


	private static void transactionDump(List<Change> changes) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE));
		List<String> added_features = new ArrayList<String>();
		List<String> removed_features = new ArrayList<String>(); 
		
		for(Change c : changes)
		{
			//FILTERING OCCURS HERE 
			if(c.added_features.isEmpty() && c.removed_features.isEmpty() )
				continue; 
			
			if(c.Bool)
				bw.append("boolean,");
			else if (c.Tristate)
				bw.append("tristate,");
			else if (c.String_var)
				bw.append("string,");
			else
				bw.append("type,");
			
			if (c.mapped_to_folder)
				bw.append("folder,");
			if(c.mapped_to_file)
				bw.append("file,");
			else if (c.mapped_to_cf)
				bw.append("c_flag,");
			else
				bw.append("unmapped,");
			
			for(String f : c.added_features)
			{
				bw.append("added_"+f + ",");
				if(!added_features.contains("added_"+f))
					added_features.add("added_"+f);
			}
			
			for(String f : c.removed_features)
			{
				bw.append("removed_"+f + ",");
				if(!removed_features.contains("removed_" + f))
					removed_features.add("removed_" + f);
			}
			for(String f : c.added_includes)
				bw.append("added_"+f + ",");
			for(String f : c.removed_includes)
				bw.append("removed_"+f + ",");
			bw.append("\n");
			bw.flush();
		}
		
		bw.close();
		
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(ITEMS_FILE));
		
		for(String f : added_features)
		{
			bw2.append("\""+f+"\",");
		}
		bw2.flush();
		for(String f : removed_features)
		{
			bw2.append("\""+f+"\",");
		}
		
		bw2.flush();bw2.close();
		
		
		bw = new BufferedWriter(new FileWriter(OUTPUT_REL_FILE));
		for(Change c : changes)
		{
			bw.append(c.feature + ",added,");
			for( String d : c.added_features)
			{
				bw.append(d+",");
			}
			bw.append("\n");
			
			bw.append(c.feature + ",removed,");
			for( String d : c.removed_features)
			{
				bw.append(d+",");
			}
			bw.append("\n");
			bw.flush();
		}
		bw.close();
	}


	private static List<Node> getTimeLines() throws Exception
	{
		List<Node> timeLines = new ArrayList<Node>();
		
		GraphDatabaseService s = DBConnection.getService();
		String query = "match (t:TimeLine) return distinct t"; 
		try(Transaction tx= s.beginTx())
		{
			Result r = s.execute(query);
			while (r.hasNext())
			{
				Map<String,Object> row = r.next();
				Node id = (Node)row.get("t");
				timeLines.add(id);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("failed to retrieve time lines for further extraction. Better stop now.");
		}
		return timeLines;
	}
	
	public static void extractInfoPerCommit(String commit) throws Exception
	{
		GraphDatabaseService s = DBConnection.getService();

		List<CodeDepChange> code_changes= new ArrayList<CodeDepChange>();
		List<FeatureDepChange> feature_changes = new ArrayList<FeatureDepChange>();

		getInfoFromCommit(commit, s, code_changes, feature_changes);
		
		System.out.println("feature name; new dependencies; removed dependencies; new includes; removed includes");
		
		for(FeatureDepChange fdc: feature_changes)
		{
			String target_feat = fdc.feature; 
			System.out.print(target_feat + ";");
			
			for(String a : fdc.added_targets)
				System.out.print(a+" ");
			System.out.print(";");

			for(String a : fdc.removed_targets)
				System.out.print(a+" ");
			System.out.print(";");
	
			System.out.print("\n");
			
		}
		
	}


	private static void getInfoFromCommit(String commit, GraphDatabaseService s, List<CodeDepChange> code_changes, List<FeatureDepChange> feature_changes)
	{
		try(Transaction t = s.beginTx())
		{
			String q2 = "match (c:commit)-->(a:ArtefactEdit)<--(t:TimeLine), (a)-->(l:LineEdit) where c.hash=\""+commit+"\" return c.hash,t.name,a.name,l;";
			Result rs = s.execute(q2);
			while ( rs.hasNext() )
		    {
		        getCodeChanges(code_changes, rs);
		    }
			
			String q1 = "match (c:commit)-->(f:FeatureEdit) where c.hash=\""+commit+"\" return c.hash,f";
			rs = s.execute(q1);
			while ( rs.hasNext() )
		    {
		        getFeatureChanges(feature_changes, rs);
		    }
			t.success();
		}
		catch(Exception e)
		{
			System.err.println("transaction failed : " + e.getMessage());
		}
	}


	private static void getFeatureChanges(List<FeatureDepChange> feature_changes, Result rs) throws Exception
	{
		Map<String,Object> row = rs.next();
		
		Node f = (Node) row.get("f");
		FeatureUpdate f_update = new FeatureUpdate(f);
		f_update.extractInfo();
		
		
		FeatureDepChange fdc = new FeatureDepChange();
		fdc.feature = (String)f.getProperty("name");
		fdc.commit_id = (String) row.get("c.hash");
		
		fdc.added_targets.addAll(f_update.added_features);
		fdc.removed_targets.addAll(f_update.removed_features);
		
		
		feature_changes.add(fdc);
	}

	
	
	private static void getCodeChanges(List<CodeDepChange> code_changes, Result rs) throws Exception
	{
		Map<String,Object> row = rs.next();
		
		CodeDepChange c = new CodeDepChange(); 
		
		
		c.artefact_name  = (String) row.get("a.name");
		c.feature_name  = (String) row.get("t.name");
		c.commit_id = (String) row.get("c.hash");
		
		
		Node lineedit = (Node) row.get("l");
		CodeDepParser p = new CodeDepParser(lineedit);
		
		c.added_includes.addAll(p.getAddedIncludes());
		c.removed_includes.addAll(p.getRemovedIncludes());
		
		code_changes.add(c);
	}
	
	
}
