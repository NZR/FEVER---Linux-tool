package fever.enhancer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.utils.DBConnection;

public class RelTester
{
	public static void main(String[] args) throws Exception
	{
		//
		
		String path = "/Users/Dante/Documents/workspace/eclipse_workspaces/fever/RuleCleaner/313_rel_changes.list"; 
		
		BufferedReader r = new BufferedReader(new FileReader(path));
		List<String> only_in_rel_changes = new ArrayList<String>(); 
		List<String> in_rels_and_updated = new ArrayList<String>(); 
		
		String line = ""; 
		while(null != (line = r.readLine()))
		{
			String f_name = line.trim();
			if(f_name.contains("."))
				continue;
			
			GraphDatabaseService s = DBConnection.getService();
			try(Transaction tx = s.beginTx())
			{
				String query = "match (t:TimeLine) where t.name = \""+f_name+"\" return t.name;";
				
				Result results = s.execute(query);
				int found = 0;
				while(results.hasNext())
				{
					results.next();
					found++;
				}
				
				if(found == 0)
				{
					if(!only_in_rel_changes.contains(f_name))
						only_in_rel_changes.add(f_name);
				}
				else
				{
					if(!in_rels_and_updated.contains(f_name))
						in_rels_and_updated.add(f_name);
				}
				tx.success();
			}
			catch(Exception e)
			{
				System.err.println("oops");
			}
		}
		r.close();
		
		System.out.println("Feature only in rels - stable and connected : ");
		for(String s : only_in_rel_changes)
		{
			System.out.println("\t"+s);
		}
		
		System.out.println("Feature with changed references and updates - connected and unstable : ");
		for(String s: in_rels_and_updated)
		{
			System.out.println("\t" + s);
		}
		
	}
}
