package fever.graph.model.TimeLine;

import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.graph.model.MappingEditNode;
import fever.graph.utils.DBConnection;

public class FeatureTimeLineNode
{
	public static final String _name = "name";
	public static final String _label_timeline = "TimeLine";
	
	public static enum Relationships implements RelationshipType
	{
	    FEATURE_CORE_UPDATE, FEATURE_INFLUENCE_UPDATE
	}

	public static void createUniqueTimeLineEntityRel(MappingEditNode me, Node timeLine, RelationshipType relType)
	{
	  boolean exists = false;
	  for(Relationship r : timeLine.getRelationships(relType))
	  {
			if(r.getOtherNode(timeLine).equals(me.n)) 
			{ 
				exists = true;
				break;
			}
	  }
	  if(!exists) 
		  timeLine.createRelationshipTo(me.n,relType);
	}
	
	
	
}
