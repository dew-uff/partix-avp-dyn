package uff.dew.svp.engine.flworprocessor;

import java.util.ArrayList;

import uff.dew.svp.engine.flworprocessor.util.GroupSpec;
import uff.dew.svp.javaccparser.SimpleNode;

public class GroupByClause extends Clause {
	
	private ArrayList<GroupSpec> groupSpecList;

	public GroupByClause(SimpleNode node){
		this(node, false);
	
	}
	
	public GroupByClause(SimpleNode node, boolean debug){

		this.groupSpecList = new ArrayList<GroupSpec>();
		
		this.processSimpleNode(node, debug);
	}
	
	public ArrayList<GroupSpec> getGroupSpecList() {
		return groupSpecList;
	}

	protected void processSimpleNode(SimpleNode node, boolean debug){
		if (debug)
			this.debugTrace(node);
		
		String element = node.toString();
		boolean processChild = true;
		//System.out.println(node);
		//if (element == "GroupSpec"){
			this.groupSpecList.add(new GroupSpec(node, debug));
			processChild = false;
		//}
		
		if (processChild & (node.jjtGetNumChildren()>0)){
			for (int i=0; i<node.jjtGetNumChildren(); i++){
				this.processSimpleNode((SimpleNode)node.jjtGetChild(i), debug);
			}
		}
	}
}
