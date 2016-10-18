package fever.stats;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.utils.DBConnection;

public class FeatureTimeLineBasedStats
{
	//in which spaces are feature touched ? (kinky)
	public static void main(String[] args) throws Exception
	{
		dumpFeatureLvlInfo();
	}
	
	
	public static void dumpFeatureLvlInfo() throws Exception
	{
		String all_timelines = "match (t:TimeLine) return distinct (t.name)";		
		String q_vm_only = "match (t:TimeLine)-->(f:FeatureEdit) where not (t)-->(:MappingEdit) and not (t)-->(:ArtefactEdit) and not (t)-->(:SourceEdit) return distinct (t.name)"; 
		String q_build_only = "match (t:TimeLine)-->(f:MappingEdit) where not (t)-->(:FeatureEdit) and not (t)-->(:SourceEdit) and not (t)-->(:ArtefactEdit) return distinct (t.name)";
		String q_source_only_1 = "match (t:TimeLine) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:FeatureEdit) and not (t)-->(:MappingEdit) return distinct (t.name)";
		String q_vm_build = "match (t:TimeLine)-->(f:FeatureEdit), (t)-->(m:MappingEdit) where not (t)-->(:ArtefactEdit) and not (t)-->(:SourceEdit) return distinct (t.name)";
		String q_source_build_1 = "match (t:TimeLine)-->(m:MappingEdit) where ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:FeatureEdit) return distinct (t.name)";
		String q_vm_source_1 = "match (t:TimeLine)-->(m:FeatureEdit) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:MappingEdit) return distinct (t.name)";
		String q_all_spaces = "match (t:TimeLine)-->(a:FeatureEdit), (t)-->(m:MappingEdit) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) return distinct (t.name)";
		
		WriteFeatureToFile(all_timelines, FilePath.o_features);
		WriteFeatureToFile(q_vm_only, FilePath.o_feature_in_vm);
		WriteFeatureToFile(q_build_only, FilePath.o_feature_in_build);
		WriteFeatureToFile(q_source_only_1, FilePath.o_feature_in_source);
		WriteFeatureToFile(q_vm_build, FilePath.o_feature_in_vm_build);
		WriteFeatureToFile(q_vm_source_1,FilePath.o_feature_in_vm_source );
		WriteFeatureToFile(q_source_build_1,FilePath.o_feature_in_build_source );
		WriteFeatureToFile(q_all_spaces,FilePath.o_feature_in_all);
		
	}


	private static void WriteFeatureToFile(String query, String file) throws Exception, IOException
	{
		GraphDatabaseService s = DBConnection.getService();
		String vals = "(t.name)";
		BufferedWriter bw = new BufferedWriter(new FileWriter(file,true)); //append mode.
		try(Transaction tx = s.beginTx())
		{
			Result r= s.execute(query);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				String val = (String) row.get(vals);		
				bw.append(val+"\n");
			}
		}
		catch(Exception e)
		{
			System.err.println("oops: "+e.getMessage());
		}
		bw.flush();
		bw.close();
	}
	
	

	public static void dumpFeatureLvlStat(BufferedWriter output) throws Exception
	{
		if(output == null)
			return;
		
		String all_timelines = "match (t:TimeLine) return count(distinct t)";		
		String q_vm_only = "match (t:TimeLine)-->(f:FeatureEdit) where not (t)-->(:MappingEdit) and not (t)-->(:ArtefactEdit) and not (t)-->(:SourceEdit) return count (distinct t)"; 
		String q_build_only = "match (t:TimeLine)-->(f:MappingEdit) where not (t)-->(:FeatureEdit) and not (t)-->(:SourceEdit) and not (t)-->(:ArtefactEdit) return count (distinct t)";
		String q_source_only_1 = "match (t:TimeLine) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:FeatureEdit) and not (t)-->(:MappingEdit) return count (distinct t)";
		String q_vm_build = "match (t:TimeLine)-->(f:FeatureEdit), (t)-->(m:MappingEdit) where not (t)-->(:ArtefactEdit) and not (t)-->(:SourceEdit) return count (distinct t)";
		String q_source_build_1 = "match (t:TimeLine)-->(m:MappingEdit) where ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:FeatureEdit) return count (distinct t)";
		String q_vm_source_1 = "match (t:TimeLine)-->(m:FeatureEdit) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) and not (t)-->(:MappingEdit) return count (distinct t)";
		String q_all_spaces = "match (t:TimeLine)-->(a:FeatureEdit), (t)-->(m:MappingEdit) where  ( (t)-->(:ArtefactEdit) or (t)-->(:SourceEdit)) return count (distinct t)";

		GraphDatabaseService s = DBConnection.getService(); 

		try(Transaction tx = s.beginTx())
		{
			Result r= s.execute(all_timelines);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count(distinct t)");
				//output.append("number of time lines: " + val+"\n");
			}
			
			int sum = 0;

			r= s.execute(q_vm_only);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in vm only: " + val+"\n");
			}

			r= s.execute(q_build_only);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in mapping only: " + val+"\n");
			}
			
			r= s.execute(q_source_only_1);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in source only: " + val+"\n");
			}
			
			r= s.execute(q_source_build_1);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in source and mapping: " + val+"\n");
			}
			
			r= s.execute(q_vm_build);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in vm and mapping: " + val+"\n");
			}
			
			r= s.execute(q_vm_source_1);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in vm and source: " + val+"\n");
			}
			
			r= s.execute(q_all_spaces);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Long val = (Long) row.get("count (distinct t)");
				sum += val;
				output.append("update in all spaces: " + val+"\n");
			}
			tx.success();
			System.out.println("total from queries (should match first line):" + sum);
		}
		catch(Exception e)
		{
			System.err.println("failed : "+e.getMessage());
		}
	}
}
