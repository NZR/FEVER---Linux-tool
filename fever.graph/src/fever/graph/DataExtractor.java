package fever.graph;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import fever.GitRepoFactory;
import fever.change.FeatureOrientedChange;
import fever.change.FileChange;
import fever.graph.model.ArtefactEditNode;
import fever.graph.model.CommitNode;
import fever.graph.model.FeatureEditNode;
import fever.graph.model.FeatureOrientedChangeNode;
import fever.graph.model.MappingEditNode;
import fever.graph.model.SourceEditNode;
import fever.graph.utils.DBConnection;
import fever.parsers.CommitInfoExtractor;
import fever.parsers.EvolutionStep;
import fever.parsers.FeatureOrientedChangeExtractor;
import fever.utils.FeatureUtils;

public class DataExtractor
{

	static Map<String, List<String>> commit_parents_list = new ConcurrentHashMap<String,List<String>>();
	static Map<String, Node> files = new HashMap<String,Node>();
	
	
	public DataExtractor() throws Exception
	{
		
	}
	
	
	public void extractCommitInfo(List<String> commits) throws Exception
	{
		Repository r = GitRepoFactory.getRepo();
		List<RevCommit> revCommits = new ArrayList<RevCommit>();
		RevWalk revWalk = new RevWalk(r);
		for(String id : commits)
		{
			ObjectId i = ObjectId.fromString(id);
			RevCommit commit = revWalk.parseCommit(i);
			revCommits.add(commit);
		}
		revWalk.release();
		revWalk.dispose();
		extractRevCommitHistory(revCommits);
	}
	
	public void extractCommitInfo(String fromId, String toId) throws Exception
	{
		Repository r = GitRepoFactory.getRepo();
		List<RevCommit> commits = getRevLog(r, fromId, toId); 

		extractRevCommitHistory(commits);
	}


	private void extractRevCommitHistory(List<RevCommit> commits) throws Exception
	{
		
		System.out.println("now extracting info from "+commits.size()+" commits");
		int info_iterations = 0;
		try
		{
			for (RevCommit m : commits)
			{
				info_iterations++;
				updateParentMap(m);
				Node commit_node = getorCreateCommitNode(m); //create the node to which all changes in this commit will be attached.
				
				System.out.println("Extracting commit : " + m.getId() + " message : " + m.getShortMessage());
				
				int nb_parents = m.getParentCount();
				if ( nb_parents == 1 && ! m.getShortMessage().startsWith("Merg") && !m.getShortMessage().startsWith("Linux ") ) //Linux v3.13-rc1 type of merges to be avoided.
					extractChanges(commit_node, m);	
				
				System.out.println("Change extraction progress: " + ( (info_iterations* 100) /commits.size() ) +"%  - " + info_iterations+ "/" + commits.size());
			}
		}
		catch(Exception e)
		{
			System.err.println("commit fine grained changes extraction aborted - check percentage of already extracted info. This is going to be partial");
			System.err.println(e.getMessage());
			throw new Exception("abort");
		}
		
		System.out.println("Connecting commits to parents ("+commit_parents_list.size() +" to go)");
		for (RevCommit m : commits)
		{
			connectToParent(m);
		}
	}
	

	/**
	 * Extract changes associated with this commit:
	 * 	creates nodes for each touched artefacts and artefact edit nodes.
	 * 
	 * @param nodeId the Neo4J node associated with commit "m"
	 * @param m the JGit commit object
	 * @throws Exception if we can't parse the commit.
	 */
	@SuppressWarnings("unused") 
	private void extractChanges(Node commit_node, RevCommit m) throws Exception
	{

		List<String> commitIds = new ArrayList<String>(); 

		commitIds.add(m.toObjectId().getName());
		
		CommitInfoExtractor cie = new CommitInfoExtractor();
		cie.extractFeatureChanges(commitIds);
		List<EvolutionStep>steps  = cie.getSteps();
		
		try(Transaction tx = DBConnection.getService().beginTx())
		{
			for(EvolutionStep step : steps)
			{
				FileChangeExtractor file_changes = new FileChangeExtractor(commit_parents_list,files);
				List<Node> file_nodes = file_changes.addFileChangesToCommit(step.files, commit_node);
				
				VMChangeExtractor vm_changes = new VMChangeExtractor(commit_parents_list,files);
				List<Node> vm_nodes = vm_changes.addVMChangesToCommit(step.fm_changes, commit_node);
				
				BuildChangeExtractor build_changes = new BuildChangeExtractor(commit_parents_list,files);
				List<Node> build_nodes = build_changes.addBuildChangesToCommit(step.build_changes, commit_node);
				
				CodeChangeExtractor code_changes = new CodeChangeExtractor(commit_parents_list,files);
				List<Node> code_nodes = code_changes.addCodeChangesToCommit(step.impl_changes, commit_node);
			}
			tx.success();
		}
		catch(Exception e)
		{
			throw new Exception("Failed to create  Neo4j nodes - ", e);
		}
	}

	/**
	 * Creates a Neo4j node in the database based on the RevCommit passed as argument.
	 * @param m the commit from which info will be extracted
	 * @return the id of the created node. 
	 * @throws Exception
	 */
	private Node getorCreateCommitNode(RevCommit m) throws Exception
	{
		Node n = null;
		try (Transaction tx = DBConnection.getService().beginTx())
		{
			Result result = DBConnection.getService().execute( "match (n {"+CommitNode._hash+": '"+getCommitId(m)+"'}) return n" );
			Iterator<Node> nodes = result.columnAs( "n" );
			for(Node partial : IteratorUtil.asIterable(nodes))
			{
				if(n == null)
					n = partial;
			}
			
			if(n == null)
			{
				n = DBConnection.getService().createNode();
				n.addLabel( DynamicLabel.label("Commit"));
				n.setProperty(CommitNode._hash, getCommitId(m));
				n.setProperty(CommitNode._author, m.getAuthorIdent().getName());
				n.setProperty(CommitNode._commiter, m.getCommitterIdent().getName());
				n.setProperty(CommitNode._message, m.getFullMessage());
				Date expiry = new Date( Long.valueOf(m.getCommitTime()) * 1000 );
				n.setProperty(CommitNode._time, expiry.toString());
	}
			tx.success();
		}
		catch(Exception e)
		{
			System.err.println("failed to add node to Neo4j database: " + e.getMessage());
		}

		return n;
	}	
	
	/**
	 * Connect Neo4j commit node "m" with its parent commits, as stated in the commit_parent table.
	 *  
	 * This methods recover the commit node associated with "m" based on the commit id.
	 * It then does the same for the parent commits associated with ``m'' in the commit_parents table.
	 * Connect the commit to its parent(s) with a ``previous'' relationship, and the parent(s) to the commit with a ``next'' relationship.
	 *  
	 * @param m the commit to connect
	 * @throws Exception
	 */
	private void connectToParent(RevCommit m) throws Exception
	{
		GraphDatabaseService service = DBConnection.getService();
		try (Transaction tx = service.beginTx())
		{
			Result result = service.execute( "match (n {"+CommitNode._hash+": '"+getCommitId(m)+"'}) return n" );
			Iterator<Node> nodes = result.columnAs( "n" );

			for(Node commit_node : IteratorUtil.asIterable(nodes))
			{
				List<String> parent_ids = commit_parents_list.get(getCommitId(m));
				for(String s : parent_ids)
				{
					if(!commit_parents_list.keySet().contains(s))
					{	//unknown parent, the node is not going to exist. If we attempt to create the rel, the transaction will be aborted. Let's ignore this.
						continue;
					}
					Result candidate_parents_results = service.execute( "match (n {"+CommitNode._hash+" : '"+s+"'}) return n" );
					Iterator<Node> candidate_parent_nodes = candidate_parents_results.columnAs( "n" );

					for(Node p : IteratorUtil.asIterable(candidate_parent_nodes))
					{
						try
						{
							Relationship new_rel = p.createRelationshipTo(commit_node,CommitNode.Relationships.NEXT);
							if(new_rel == null)
								System.err.println("Couldn't connect commit " + getCommitId(m) + " with " +s);
						}
						catch(Exception e)
						{	//shut up
							System.err.println("Failted to connect commit node to parent - silently continuing : \nCommits (child) : "+getCommitId(m) + " with (parent) : " +s);
						}
					}
				}
			}
			result.close();
			tx.success();
		}
		catch(Exception e)
		{
			System.err.println("failed to create relationship between commits and their parents." + e.getMessage());
		}
	}
	
	
	/**
	 * Adds the current commit and its parent info to the commit_parents map. This is used later on to create the relationship
	 * between commits and their parents/children.
	 * 
	 * @param m the commit to add to the list.
	 */
	private void updateParentMap(RevCommit m)
	{
		String c_id = getCommitId(m);
		List<String> parents = new ArrayList<String>();
		for (RevCommit p : m.getParents())
		{
			String p_id = getCommitId(p);
			parents.add(p_id);
		}
		commit_parents_list.put(c_id, parents);
	}
	
	/**
	 * builds the commit id used in Neo4j from the ``RevCommit'' Git id. 
	 * 
	 * @param m the RevCommit for which to get a Neo4j id.
	 * 
	 * @return Returns the 10 first characters of the commit's hash tag.
	 */
	private static String getCommitId(RevCommit m )
	{
		return m.getId().abbreviate(10).name();
	}
	
	/**
	 * List all commits between commits "startRangeId" and "endRangeId".
	 * 
	 * @param repository  the repo in question
	 * @param startRangeId the first commit to extract from
	 * @param endRangeId the end commit
	 * @return a list of RevCommit, empty list otherwise
	 * @throws Exception if data extraction fails.
	 */
	public static List<RevCommit> getRevLog(Repository repository, String startRangeId, String endRangeId) throws Exception
	{
		List<RevCommit> list = new ArrayList<RevCommit>();
		try
		{
			ObjectId endRange = repository.resolve(endRangeId);
			ObjectId startRange = repository.resolve(startRangeId);
			RevWalk rw = new RevWalk(repository);

			rw.markStart(rw.parseCommit(endRange));
			if (!startRange.equals(ObjectId.zeroId()))
			{
				rw.markUninteresting(rw.parseCommit(startRange));
			}
			
			Iterable<RevCommit> revlog = rw;
			for (RevCommit rev : revlog)
			{
				list.add(rev);
			}
			rw.dispose();
			rw.release();
		} 
		catch (Throwable t)
		{
			System.err.println("Failure." + t.getMessage());
			throw new Exception(t);
		}
		return list;
	}
}
