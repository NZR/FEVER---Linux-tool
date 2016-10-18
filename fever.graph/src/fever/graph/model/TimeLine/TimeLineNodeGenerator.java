package fever.graph.model.TimeLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import fever.enhancer.Enhancer;
import fever.graph.model.ArtefactEditNode;
import fever.graph.model.CommitNode;
import fever.graph.model.FeatureEditNode;
import fever.graph.model.MappingEditNode;
import fever.graph.model.SourceEditNode;
import fever.graph.utils.DBConnection;
import fever.utils.ParsingUtils;

public class TimeLineNodeGenerator
{
	public static String initial_mapping_file = "/Users/Dante/Documents/workspace/git/SPLR-FEVER/fever/fever.graph/intial_mapping_4.3.txt";
	public static String initial_feature_list = "/Users/Dante/Documents/workspace/git/SPLR-FEVER/fever/fever.graph/initial_features_4.3.txt";
	
	static Map<String, Node> feature_timeLineNodes = new HashMap<String, Node>(); //provides quick access to timeline we created during the exploration.
	static Map<String, List<String>> mappings_to_remove = new HashMap<String, List<String>>();
	
	private MappingTable mt; //contains the mapping between features and file, gets updated as we explore commits
	private FeatureTable ft; //contains the list of existing feature from the variability model, updated as well during exploration.
	
	static Map<String, Node> done_commits = new HashMap<String, Node>();
	
	/**
	 * Method used to test time line creation and queries.
	 * 
	 * @param args
	 * @throws Exception
	 */
	static public void main(String[] args) throws Exception
	{	
		TimeLineNodeGenerator gen = new TimeLineNodeGenerator();
		gen.createTimeLines();
	}
	
	static public void cleanTimeLines(String[] args) throws Exception
	{
		GraphDatabaseService s = DBConnection.getService(); 
		
		try(Transaction t = s.beginTx())
		{
			String query = "match (t:TimeLine) return t";
			Result r = s.execute(query);
			while(r.hasNext())
			{
				Map<String,Object> row = r.next();
				Node n = (Node) row.get("t");
				clearNode(n);
			}
			t.success();
		}
		catch(Exception e)
		{
			System.err.println("Failed to delete a timeline node - check db");
		}
		
	}
	
	
	
	private final void createTimeLines() throws Exception
	{
		this.mt = new MappingTable();
		this.mt.loadMappingFromFile(initial_mapping_file);
		
		this.ft = new FeatureTable();
		this.ft.loadFeatures(initial_feature_list);
		
		List<Node> commits_to_treat = CommitNode.getFirstCommitNode();
		startExtraction(commits_to_treat);
	}
	
	public void startExtraction(List<Node> commits) throws Exception
	{
		List<Node> nexts = extractAll(commits);
		while (!nexts.isEmpty())
		{
			nexts = extractAll(nexts);
		}
	}
	
	public List<Node> extractAll(List<Node> commits) throws Exception
	{
		List<Node> nexts = new ArrayList<Node>();
		for (Node n : commits)
		{
			if (!done_commits.containsKey(String.valueOf(n.getId())))
			{
				List<Node> next_for_this_one = extractCommitInfo(n);
				for (Node new_commit : next_for_this_one)
				{
					if (!nexts.contains(new_commit))
					{
						nexts.add(new_commit);
					}
				}
			}
		}
		return nexts;
	}
	
	public List<Node> extractCommitInfo(Node commit) throws Exception
	{
		try (Transaction tx = DBConnection.getService().beginTx())
		{
			connectFeatureEdits(commit);
			tx.success();
		}
		catch (Exception e)
		{
			throw new Exception("Failed feature edits to the appropriate time line node", e);
		}
		
		String hash = "";
		Transaction tx_mapping = null;
		try 
		{
			tx_mapping = DBConnection.getService().beginTx();
			hash = DBConnection.getPropValue(commit, "hash");
			connectMappingEdits(commit);
			tx_mapping.success();
			tx_mapping.close();
		}
		catch (Exception e)
		{
			if(	tx_mapping != null)
			{
				tx_mapping.failure();
				tx_mapping.close();
			}
			System.err.println("Failed mapping edits to the appropriate time line node, but continuing nonetheless... check commit: "+hash );
			System.err.println("Neo4j Node id : " + String.valueOf(commit.getId()));
		}
		
		try (Transaction tx = DBConnection.getService().beginTx())
		{
			connectFileEdits(commit);
			tx.success();
		}
		catch (Exception e)
		{
			throw new Exception("Failed file edits to the appropriate time line node", e);
		}
		try (Transaction tx = DBConnection.getService().beginTx())
		{
			connectCodeEdit(commit);
			tx.success();
		}
		catch (Exception e)
		{
			throw new Exception("Failed code edits to the appropriate time line node", e);
		}
		try (Transaction tx = DBConnection.getService().beginTx())
		{
			cleanMappingTable(commit);
		}
		catch (Exception e)
		{
			throw new Exception("Failed to clean the mapping table", e);
		}
		
		done_commits.put(String.valueOf(commit.getId()), commit);
		return CommitNode.getNextCommitNodes(commit);
	}
	
	private void connectCodeEdit(Node commit) throws Exception
	{
		List<SourceEditNode> sourceEdits = CommitNode.getImplementationEdits(commit);
		for (SourceEditNode n : sourceEdits)
		{
			boolean hit = false;
			List<String> f_names = n.getFeatures();
			for (String f_name : f_names)
			{
				if (this.ft.hasFeature(f_name))
				{
					hit = true;
					Node featureTimeLine = getOrCreateTimeLineNodeFor(f_name);
					featureTimeLine.createRelationshipTo(n.n, FeatureTimeLineNode.Relationships.FEATURE_INFLUENCE_UPDATE);
				}
				else if (!f_name.contains("0x") && !f_name.contains("."))
				{
					hit = true;
					Node featureTimeLine = getOrCreateTimeLineNodeFor(f_name);
					featureTimeLine.createRelationshipTo(n.n, FeatureTimeLineNode.Relationships.FEATURE_INFLUENCE_UPDATE);
				}
			}
			if (!hit)
			{
				if (n.n.hasLabel(DynamicLabel.label("reference_edit")))
				{ // this is a reference to something which doesn't appear to be a feature... let's remove that.
				  // System.out.println("cleared a source edit node for lack of feature information (reference)");
					clearNode(n.n);
				}
				
				else for (String f_name : f_names)
				{
					if (f_name.equals(f_name.toUpperCase()))
					{
						hit = true;
						Node featureTimeLine = getOrCreateTimeLineNodeFor(f_name);
						featureTimeLine.createRelationshipTo(n.n, FeatureTimeLineNode.Relationships.FEATURE_INFLUENCE_UPDATE);
					}
				}
			}
			
		}
	}
	
	private static void clearNode(Node n)
	{
		for (Relationship r : n.getRelationships())
		{
			r.delete();
		}
		n.delete();
	}
	
	private void connectFileEdits(Node commit) throws Exception
	{
		List<ArtefactEditNode> nodes = CommitNode.getFileEdits(commit);
		for (ArtefactEditNode n : nodes)
		{
			String test = n.getFileName();
			if(ParsingUtils.isBuildFile(test) || ParsingUtils.isVariabilityFile(test))
			{
				continue; // no time lines on such objects.
			}
			
			String fileName = n.getFileNameWithoutExtension();
			List<String> mapped_feature_names = this.mt.getFeaturesForFile(fileName);
			if (mapped_feature_names != null && !mapped_feature_names.isEmpty())
			{
				for (String mapped_feature_name : mapped_feature_names)
				{
					
					if(mapped_feature_name.endsWith("_guarded_"))
					{
						mapped_feature_name = this.mt.findGuardFeatureForFile(mapped_feature_name);
						if(mapped_feature_name == null || mapped_feature_name.isEmpty())
							continue;
					}
					Node timeLineNode = getOrCreateTimeLineNodeFor(mapped_feature_name);
					if (timeLineNode != null)
					{
						try
						{
							timeLineNode.createRelationshipTo(n.n, FeatureTimeLineNode.Relationships.FEATURE_CORE_UPDATE);
						}
						catch(Exception e)
						{
							System.err.println("Error while connecting file artefact to feature : " + e.getMessage());
							System.err.println("connecting " + timeLineNode.getId() + " to " + n.n.getId());
						}
					}
				}
			}
		}
	}
	
	private final void connectMappingEdits(Node commit) throws Exception
	{
		List<MappingEditNode> mapping_edits = CommitNode.getMappingEdits(commit);
		// we start with removals
		// we update the feature/file mapping tables first we removed mapping, then added ones.
		List<Node> removed_node = new ArrayList<Node>(); 
		List<String> removed_pairs = new ArrayList<String>();
		List<String> added_pairs = new ArrayList<String>();
		
		for (MappingEditNode me : mapping_edits)
		{
			if (!removed_node.contains(me.n) && me.n.getProperty(MappingEditNode._mapping_change).equals("REMOVED"))
			{
				if ( 1 != connectSingleMENode(me, false) )
				{
					removed_node.add(me.n);
					continue;
				}
				
				String feat = me.getMappedFeature();
				String target = me.getMappingTarget();
				String pair = feat + "==" + target;
				removed_pairs.add(pair);
			}
		}
		
		for (MappingEditNode me : mapping_edits)
		{
			if (!removed_node.contains(me.n) && !me.n.getProperty(MappingEditNode._mapping_change).equals("REMOVED"))
			{
				if ( 1 != connectSingleMENode(me, true) )
				{
					removed_node.add(me.n);
					continue;
				}
				String feat = me.getMappedFeature();
				String target = me.getMappingTarget();
				String pair = feat + "==" + target;
				added_pairs.add(pair);
			}
		}
		
		// identify feature mapping added AND removed in the same commit - deal with refactoring here.
		List<String> to_clean = new ArrayList<String>();
		for (String p : added_pairs)
		{
			if (removed_pairs.contains(p))
				to_clean.add(p);
		}
		
		List<MappingEditNode> to_remove = new ArrayList<MappingEditNode>();
		for (String p : to_clean)
		{
			String[] infos = p.split("==");
			String f = infos[0];
			String t = infos[1];
			for (MappingEditNode me : mapping_edits)
			{
				try
				{
					String f1 = me.getMappedFeature();
					String t1 = me.getMappingTarget();
					if (f.equals(f1) && t.equals(t1))
					{
						if (!to_remove.contains(me))
							to_remove.add(me);
					}
				}
				catch(Exception e)
				{
					//the node might have been removed already by a previous op. 
					//if we crash here, then me.getXXXX did not succeed. The node cannot be cleanup
					// we can proceed with the next one.
					System.err.println("We are trying to remove a node twice");
					System.err.println(" trying to remove: feature " + f + " -  mapping " + t);
					System.err.println(" and we encountered a node already removed while doing that.");
				}
			}
		}
		
		for (MappingEditNode me : to_remove)
		{
			if (!removed_node.contains(me.n) && !me.getProp(MappingEditNode._mapping_change).equals("REMOVED"))
			{
				me.n.setProperty(MappingEditNode._mapping_change, "REFACTORED");
				me.n.setProperty(MappingEditNode._target_change, "REFACTORED");
			}
			else
			{
				if(removed_node.contains(me.n))
					clearNode(me.n);
			}
		}
	}
	
	private int connectSingleMENode(MappingEditNode me, boolean add) throws Exception
	{
		String f_name = me.getMappedFeature();
		String file_name = me.getMappingTarget();
		if (this.ft.hasFeature(f_name))
		{
			if (add)
				this.mt.addEntry(f_name, file_name);
			else
				this.mt.removeEntry(f_name, file_name);
			Node timeLine = getOrCreateTimeLineNodeFor(f_name);
			FeatureTimeLineNode.createUniqueTimeLineEntityRel(me, timeLine, FeatureTimeLineNode.Relationships.FEATURE_CORE_UPDATE);
		}
		else if (f_name.endsWith("_guarded_"))
		{
			String guard_feature = this.mt.findGuardFeatureForFile(f_name);
			if (guard_feature != null)
			{
				if (add)
					this.mt.addEntry(f_name, file_name);
				else
					this.mt.removeEntry(f_name, file_name);
				
				Node timeLine = getOrCreateTimeLineNodeFor(guard_feature);
				FeatureTimeLineNode.createUniqueTimeLineEntityRel(me, timeLine, FeatureTimeLineNode.Relationships.FEATURE_CORE_UPDATE);
				me.n.setProperty("feature", guard_feature); // replace place holder with actual feature name.
				me.n.setProperty("type", "FEATURE"); // it's a feature now, not a symbol
			}
			else
			{
				me.n.setProperty("feature", "unmapped"); // replace place holder with actual feature name.
				me.n.setProperty("type", "SYMBOL"); // it's a feature now, not a symbol				
			}
		}
		else if (file_name.endsWith(".o") && f_name.toUpperCase().equals(f_name))
		{
			if (add)
				this.mt.addEntry(f_name, file_name);
			else
				this.mt.removeEntry(f_name, file_name);
			Node timeLine = getOrCreateTimeLineNodeFor(f_name);
			FeatureTimeLineNode.createUniqueTimeLineEntityRel(me, timeLine, FeatureTimeLineNode.Relationships.FEATURE_CORE_UPDATE);
		}
		else
		{ 	
			return -1;
		}
		
		return 1;
	}
	
	private final void cleanMappingTable(Node commit) throws Exception
	{
		List<MappingEditNode> mapping_edits = CommitNode.getMappingEdits(commit);
		for (MappingEditNode me : mapping_edits)
		{
			String f_name = me.getMappedFeature();
			String file_name = me.getMappingTarget();
			if (!me.isNew())
			{
				mt.removeEntry(f_name, file_name);
			}
		}
	}
	
	private final void connectFeatureEdits(Node commit) throws Exception
	{
		List<FeatureEditNode> feature_edits = CommitNode.getFeatureEdits(commit);
		for (FeatureEditNode fe : feature_edits)
		{
			String f_name = fe.getTouchedFeatureName();
			String change = DBConnection.getPropValue(fe.n, "change");
			if (change.equals("ADDED") && !this.ft.hasFeature(f_name))
			{
				ft.addFeature(f_name);
			}
			Node timeLine = getOrCreateTimeLineNodeFor(f_name);
			timeLine.createRelationshipTo(fe.n, FeatureTimeLineNode.Relationships.FEATURE_CORE_UPDATE);
		}
	}
	
	private Node getOrCreateTimeLineNodeFor(String name) throws Exception
	{
		Node node = feature_timeLineNodes.get(name);
		if (node == null)
		{
			try (Transaction tx = DBConnection.getService().beginTx())
			{
				node = DBConnection.getService().createNode();
				node.addLabel(DynamicLabel.label(FeatureTimeLineNode._label_timeline));
				node.setProperty(FeatureTimeLineNode._name, name);
				tx.success();
			}
			catch (Exception e)
			{
				throw new Exception("failed to create new timeline node : ", e);
			}
			feature_timeLineNodes.put(name, node);
		}
		return node;
	}
}
