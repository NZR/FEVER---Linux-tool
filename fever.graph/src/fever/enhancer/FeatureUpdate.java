package fever.enhancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.utils.DBConnection;

/**
 * This class represents a feature edit entity in the Neo4j database. 
 * Create one of those to extract changes to feature dependencies and code include updates
 * @author Dante
 *
 */
public class FeatureUpdate
{
	public Node feature_edit = null; 
	
	public boolean Bool = false;
	public boolean Tristate = false;
	public boolean String_var = false;
	public boolean Visible = false;

	List<String> added_features 	= new ArrayList<String>();
	List<String> removed_features 	= new ArrayList<String>(); 
	
	public FeatureUpdate(Node n)
	{
		//get ready for info retrieval. 
		feature_edit = n;
	}
	
	
	public void extractInfo() throws Exception
	{
		
		Long id = feature_edit.getId();
		
		Node featureDesc_is=  null;
		Node featureDesc_was=  null;
		
		GraphDatabaseService s = DBConnection.getService();
		try(Transaction t = s.beginTx())
		{
			
			String q = "match (fe:FeatureEdit)-[:IS]->(fd:FeatureDesc) where ID(fe)="+id+" return fe.name,fd;"; 
			
			Result rs = s.execute(q);
			
			while ( rs.hasNext() )
		    {
		        Map<String,Object> row = rs.next();
		        featureDesc_is= (Node) row.get("fd"); 
		    }
			
		
			q = "match (fe:FeatureEdit)-[:WAS]->(fd:FeatureDesc) where ID(fe)="+id+" return fe.name,fd;";
			rs = s.execute(q);
			
			while ( rs.hasNext() )
		    {
		        Map<String,Object> row = rs.next();
		        featureDesc_was= (Node) row.get("fd"); 
		    }
			t.success();
		}
		catch(Exception e)
		{
			System.err.println("transaction failed : " + e.getMessage());
		}
		
		FeatureDescParser p_new = null;
		FeatureDescParser p_old = null;
		
		if(featureDesc_is != null)
		{
			setFeatureAttributes(featureDesc_is);
			p_new = new FeatureDescParser(featureDesc_is);
		}
		if(featureDesc_was != null)
		{
			if(featureDesc_is == null)
				setFeatureAttributes(featureDesc_was);
			
			p_old = new FeatureDescParser(featureDesc_was);
		}
		
		if(p_new != null && p_old != null)
		{
			for(String f :p_new.getAddedFeatures(p_old) )
			{
				if(!added_features.contains(f))
				{
					added_features.add(f);
				}
			}
			
			for(String f :p_new.getRemovedFeatures(p_old) )
			{
				if(!removed_features.contains(f))
				{
					removed_features.add(f);
				}
			}
		}
		else if (p_new == null )
		{
			for(String f :p_old.getFeatures() )
			{
				if(!removed_features.contains(f))
				{
					removed_features.add(f);
				}
			}
		}
		else if (p_old == null)
		{
			for(String f :p_new.getFeatures() )
			{
				if(!added_features.contains(f))
				{
					added_features.add(f);
				}
			}
		}
		else
		{
			//not sure to be honest... not feature descriptors for this change, this is probably a mistake.
		}
			
	}


	private void setFeatureAttributes(Node featureDesc_is) throws Exception
	{
		String prompt = DBConnection.getPropValue(featureDesc_is, "visibility");
		String type = DBConnection.getPropValue(featureDesc_is, "type");
		if(type.equals("BOOLEAN"))
			Bool = true;
		else if (type.equals("TRISTATE"))
			Tristate = true;
		else 
			String_var = true;
		
		if(prompt.equals("visible"))
			Visible = true;
	}
	
}
