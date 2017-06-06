package uff.dew.svp.engine.flworprocessor.util;

import uff.dew.svp.algebra.basic.TreeNode;
import uff.dew.svp.engine.flworprocessor.Clause;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.javaccparser.SimpleNode;

public class GroupSpec extends Clause {

	protected TreeNode _node;
	protected boolean ascending;
	protected int pathLCL;

	public GroupSpec(SimpleNode node){
		this(node, false);

	}

	public GroupSpec(SimpleNode node, boolean debug){
		this.processSimpleNode(node, debug);
		
	}

	public TreeNode getTreeNode(){
		return this._node;
	}

	public int getPathLcl(){
		return this.pathLCL;
	}

	protected void processSimpleNode(SimpleNode node, boolean debug){
		if (debug)
			this.debugTrace(node);

		String element = node.toString();
		boolean processChild = true;

		if (element == "VarName"){
			// Criacao de node com o nome da variavel
			this._node = new TreeNode(node.getText(), TreeNode.RelationTypeEnum.ROOT);

			try{				
				Query q = Query.getUniqueInstance(true);						
				q.setGroupByClause(true); 
				q.setElementConstructor(false);
				String groupBy = "";
				
				groupBy = q.getGroupBy() + (q.getGroupBy().equals("")? "$"+node.getText() : "/" + "$"+node.getText());	
				//System.out.println(element + "xx - " + groupBy);
				q.setGroupBy(groupBy); // elementos que serao utilizados para agrupamento do resultado final.		

			}
			catch(Exception ex){
				System.out.println(ex.getMessage() + "\r\n" + ex.getStackTrace());
			}
		}
		
		if (processChild & (node.jjtGetNumChildren()>0)){
			for (int i=0; i<node.jjtGetNumChildren(); i++){
				//System.out.println("bug - " + (SimpleNode)node.jjtGetChild(i));
				this.processSimpleNode((SimpleNode)node.jjtGetChild(i), debug);
			}
		}
	}

}
