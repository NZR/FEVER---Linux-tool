package fever.stats;

import java.util.Map;
import java.io.BufferedWriter;
import java.io.FileWriter;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.utils.DBConnection;

public class FileBasedAuthorshipStats
{
	
	
	 
	
	public static void main(String[] args) throws Exception
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter("file_authorship_3.12.csv"));
		GraphDatabaseService s = DBConnection.getService();

		String query = "match (c:Commit)-->(a:ArtefactEdit) return c.author, count(distinct a.name) ";
		
		try(Transaction tx = s.beginTx())
		{
			Result r =s.execute(query);
			
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String author = (String) row.get("c.author");
				if(author.contains(","))
					author = author.replace(",", " ");
				
				Long nb_edited_files = (Long)row.get("count(distinct a.name)");
				bw.append(author + "," + nb_edited_files+"\n");
				bw.flush();
			}
			tx.success();
		}
		catch(Exception e)
		{
			bw.append("crashed : " + e.getMessage());
		}
		bw.close();
		
		
	}
}
