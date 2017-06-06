package uff.dew.svp.engine.flworprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import uff.dew.svp.algebra.basic.PatternTree;
import uff.dew.svp.algebra.basic.Predicate;
import uff.dew.svp.algebra.basic.TreeNode;
import uff.dew.svp.algebra.operators.AbstractOperator;
import uff.dew.svp.algebra.operators.JoinOperator;
import uff.dew.svp.algebra.operators.SelectOperator;
import uff.dew.svp.algebra.operators.SortOperator;
import uff.dew.svp.algebra.operators.functions.FunctionOperator;
import uff.dew.svp.catalog.Catalog;
import uff.dew.svp.engine.flworprocessor.util.ComparisonExpr;
import uff.dew.svp.engine.flworprocessor.util.OrderSpec;
import uff.dew.svp.engine.flworprocessor.util.Variable;
import uff.dew.svp.exceptions.AlgebraParserException;
import uff.dew.svp.exceptions.FragmentReductionException;
import uff.dew.svp.exceptions.OptimizerException;
import uff.dew.svp.fragmentacaoVirtualSimples.ExistsJoinOperation;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SimpleVirtualPartitioning;
import uff.dew.svp.javaccparser.SimpleNode;

public class FLWOR extends Clause{

	protected ArrayList<Variable> vars;
	protected ArrayList<String> debugStrings;
	protected ArrayList<Long> debugTimes;
	protected ArrayList<AbstractOperator> debugPlans;	

	private OrderByClause orderBy;
	private GroupByClause groupBy;

	private String label = "";
	private static Vector<String> varNames = new Vector<String>();
	private static Vector<String> elemNames = new Vector<String>();
	
	public FLWOR() {
		super();
		// Inicializao dos ArrayLists
		this.vars = new ArrayList<Variable>();		
		this.debugStrings = new ArrayList<String>();
		this.debugTimes = new ArrayList<Long>();	
		this.debugPlans = new ArrayList<AbstractOperator>();
	}

	public void compile(SimpleNode node) throws OptimizerException, FragmentReductionException, AlgebraParserException, IOException {
		this.compile(node, false);
	}

	public void compile(SimpleNode node, boolean debug) throws OptimizerException, FragmentReductionException, AlgebraParserException, IOException {
		// Criao do plano de execuo do FLWOR
		this.generateExecutionPlan(node, debug);

	}

	/*
	 * Montagem do plano de execuo da XQuery global atravs das etapas:
	 * 	1- Plano algbrico sobre as views globais
	 *  2- Localizao das views globais
	 *  3- Reduo dos fragmentos inteis
	 *  4- Otimizao do plano de execuo e localizao das operaes
	 */
	protected void generateExecutionPlan(final SimpleNode node, final boolean debug) 
			throws OptimizerException, FragmentReductionException, AlgebraParserException, IOException{

		//Montagem do plano sobre as views globais
		this.processSimpleNode(node, debug);
		if (debug){
			addDebugInfo();
		}		
	}

	protected void addDebugInfo() {
		this.debugStrings.add(this.toString());
		this.debugTimes.add(new Long(System.nanoTime()));
		try{
			this.debugPlans.add(this.operator.clone());
		}
		catch (CloneNotSupportedException cloneExc) {

		}
	}		

	public String getDebugString(final int index){
		String returnStr = "";
		if ((this.debugStrings != null) && (index < this.debugStrings.size())){
			returnStr = (String)this.debugStrings.get(index);
		}

		return returnStr;
	}

	public long getDebugTime(final int index){
		long returnTime = 0;

		if ((this.debugTimes != null) && (index < this.debugTimes.size())){
			returnTime = this.debugTimes.get(index).longValue();
		}

		return returnTime;
	}

	public AbstractOperator getDebugPlan(final int index){
		if ((this.debugPlans != null) && (index < this.debugPlans.size())){
			return this.debugPlans.get(index);
		}
		else{
			return null;
		}
	}

	/*
	 * Execucao do FLWOR atraves das suas sub-queries
	 */

	protected void processSimpleNode(SimpleNode node, boolean debug) throws AlgebraParserException, IOException{
		if (debug){
			this.debugTrace(node);
		}

		final String element = node.toString();
		boolean processChild = true;
		//------------------------------------------
		//FOR Clause:
		if ("ForClause".equals(element)){

			Query q = Query.getUniqueInstance(true);
			q.setElementConstructor(false); // Indica que no se trata da estrutura XML do resultado a ser mostrada para o usurio
			q.setGroupByClause(false); // Indica que nao se trata da clausula groupBy
			q.setOrderByClause(false); // Indica que no se trata da clausula orderBy
			q.setXpath(""); // Apagar o caminho do ultimo FOR/LET, antes de comear a armazenar o caminho do FOR/LET seguinte. Cada sub-elemento XML  verificado para obter a cardinalidade deste em todo o documento.			
			q.setLastReadCardinality(-1); // Resetar a cardinalidade, pois se refere a um novo caminho Xpath para um novo FOR.
			//q.setAddedPredicate(false);		
			q.setAncestralPath("");

			final ForClause forClause = new ForClause();
			forClause.compileForLet(node, debug);				
			this.insertForLet(forClause);			
			processChild = false;				
		}
		//------------------------------------------
		//LET Clause:
		else if ("LetClause".equals(element)){
			LetClause letClause = new LetClause();
			Query q = Query.getUniqueInstance(true);
			q.setElementConstructor(false); // Indica que no se trata da estrutura XML do resultado a ser mostrada para o usurio
			q.setGroupByClause(false); // Indica que nao se trata da clausula groupBy
			q.setOrderByClause(false); // Indica que no se trata da clausula orderBy
			q.setXpath(""); // Apagar o caminho do ltimo FOR/LET, antes de comear a armazenar o caminho do FOR/LET seguinte.
			//q.setAddedPredicate(false);
			q.setAncestralPath("");
			q.setLastReadCardinality(-1); // Resetar a cardinalidade, pois se refere a um novo caminho Xpath para um novo LET.
			letClause.compileForLet(node, debug);
			this.insertForLet(letClause);					

			Hashtable<String, String> letClauses = (Hashtable<String, String>) q.getLetClauses();
			String lastReadLetVariable = q.getLastReadLetVariable();			
			q.setLastReadSimplePathExpr(q.getXpath());			

			if (!letClauses.containsKey(lastReadLetVariable)){ // verifica se a varivel deste LET j existe na hashtable
				String lastReadForLetVariable = q.getLastReadForLetVariable(); // a varivel a qual o LET referencia, um nvel acima				
				String letExpression = lastReadForLetVariable+"/"+q.getXpath();
				q.setLetClauses(lastReadLetVariable,letExpression);
			}			

			processChild = false;		
		}
		//------------------------------------------
		//WHERE Clause:
		else if ("WhereClause".equals(element)){
			WhereClause where = new WhereClause(node, debug);
			for (int i=0; i<where.getComparisons().size(); i++){
				this.insertComparison(where.getComparisons().get(i));

				Query q = Query.getUniqueInstance(true);
				q.setElementConstructor(true); // Evita consultas por cardinalidade para os predicados de seleo especificados pelo usurio na consulta de entrada.
				q.setGroupByClause(false); // Indica que nao se trata da clausula groupBy
				q.setOrderByClause(false); // Indica que nao se trata da clausula orderBy
				q.setXpath(""); // Apagar o caminho do ltimo FOR, antes de comear a armazenar o caminho do FOR seguinte.				
				q.setLastReadCardinality(-1);
			}

			processChild = false;
		}
		//------------------------------------------
		//GROUP BY Clause:
		else if ("GroupByClause".equals(element)){
			groupBy = new GroupByClause(node, debug);			
			processChild = false;
			Query q = Query.getUniqueInstance(true);
			q.setGroupByClause(true);
			q.setOrderByClause(false);
			q.setElementConstructor(false); 
		}
		//------------------------------------------
		//ORDER BY Clause:
		else if ("OrderByClause".equals(element)){
			orderBy = new OrderByClause(node, debug);			
			processChild = false;
			Query q = Query.getUniqueInstance(true);
			q.setOrderByClause(true);
			q.setGroupByClause(false);
			q.setElementConstructor(false); 
		}
		//------------------------------------------
		//RETURN Clause:
		else if ("ElmtConstructor".equals(element)) {
			//System.out.println("PEGUEI RETURN EH AGORA");
			Query q = Query.getUniqueInstance(true);
			try{				
				q = Query.getUniqueInstance(true);
				q.setElementConstructor(true); // Evita consultas por cardinalidade para os caminhos indicados no resultado da xquery.				
				q.setGroupByClause(false); // Indica que nao se trata da clausula groupBy
				q.setOrderByClause(false); // Indica que nao se trata da clausula orderBy
			}
			catch(Exception ex){
				//System.out.println(ex.getMessage() + "\r\n" + ex.getStackTrace());
			}			

			ReturnClause ret = new ReturnClause(node, debug);
			AbstractOperator constructReturn = ret.getOperator(), constructReturn2 = ret.getOperator();		
			
			//Detectou groupBy, entao seta true
			if (groupBy != null) {
				q = Query.getUniqueInstance(true);
				q.setGroupByClause(true);
				q.setElementConstructor(false);
			}
			
			// Inclusao do operador SORT abaixo do Construct, caso haja ORDER BY na consulta
			if (orderBy != null){

				q = Query.getUniqueInstance(true);
				q.setOrderByClause(true);
				q.setElementConstructor(false);

				SortOperator sort = new SortOperator();
				for (int i=0; i<orderBy.getOrderSpecList().size(); i++){
					int lcl = orderBy.getOrderSpecList().get(i).getPathLcl();
					String pred = lcl + " ";				

					if (orderBy.getOrderSpecList().get(i).isAscending())
						pred += "ASCENDING";
					else
						pred += "DESCENDING";
					sort.getPredicateList().add(pred);					
				}

				constructReturn.addChild(sort);
				constructReturn = sort;
			}

			//System.out.println("ret.getFunctionOperatorsList.size = " + ret.getFunctionOperatorsList().size());
			// Processamento dos operadores de Funcoes de Agregacao do retorno
			for (int i=0; i < ret.getFunctionOperatorsList().size(); i++) {
				//System.out.println(ret.getFunctionOperatorsList().size());
				AbstractOperator sel = ret.getFunctionOperatorsList().get(i);
				// Substituicao da varivel pelo NodeId (LCL)
				TreeNode rootNode = sel.getApt().getAptNode().getRootNode();
				int nodeId = this.getVarNodeId(rootNode.getLabel());
				if (nodeId == -1) {
					throw new AlgebraParserException("Variable " + rootNode.getLabel() + " does not exist");
				}				

				//if (sel.getName() == "Aggregate_Sum") {
					//System.out.println("SUM !!!");
				//}
				//Luiz Matos
				/* Identifica se trata-se de "Aggregate_Average" para fazer a reescrita das subconsultas da seguinte maneira:
				 * Se na consulta original temos "<avg_x>avg($var)</avg_x>", isso será substituido nas subconsultas por:
				 * <sum_x>sum($var)</sum_x>
				 * <count_x>count($var)</count_x>
				 */
				//System.out.println("sel.getName() = " + sel.getName());

				if (sel.getName() == "Aggregate_Average") {
	
					String varName = "";

					//System.out.println("sel.getName() " + sel.getName());
					//System.out.println("rootNode.getLabel() " + "$" + rootNode.getLabel());
					varName = "$" + rootNode.getLabel();
					//System.out.println("varname = " + varName);
					
					varNames.add(varName);

//					PatternTree op = constructReturn2.getApt();
//					TreeNode veja = constructReturn2.getApt().getAptNode();
//					
					//System.out.println("lalalala " + constructReturn2.getApt().getAptNode().getChildrenNodes().size());
					//for (int l = 0; l < constructReturn2.getApt().getAptNode().getChildrenNodes().size(); l++) {
					//						if (constructReturn2.getApt().getAptNode().getChildrenNodes().get(i).getLabel().contains("avg")) {
					//							label = constructReturn2.getApt().getAptNode().getChildrenNodes().get(i).getLabel();
					//							System.out.println("Vejamos = " + constructReturn2.getApt().getAptNode().getChildrenNodes().get(i).getLabel());
					//
					//							System.out.println("<sum"+label.substring(label.indexOf("avg")+3)+">sum($"+rootNode.getLabel()+")</sum"+label.substring(label.indexOf("avg")+3)+">");
					//							System.out.println("<count"+label.substring(label.indexOf("avg")+3)+">count($"+rootNode.getLabel()+")</count"+label.substring(label.indexOf("avg")+3)+">");
					//							System.out.println("");
					//				}
					//}
					//System.out.println("vejaaaaa = " + veja.getChildrenNodes());
					//	for(int x = 0 ; x < veja.getChildrenNodes().size(); x++) {

					//System.out.println("lala = " + veja.getChildrenNodes().get(i).getLabel());
					//					if (veja.getChildrenNodes().get(i).getLabel().contains("avg")) {
					//						System.out.println("label = " + veja.getChildrenNodes().get(i).getLabel());
					//						label = veja.getChildrenNodes().get(i).getLabel();
					//
					//						System.out.println("<sum"+label.substring(label.indexOf("avg")+3)+">sum("+rootNode.getLabel()+")</sum"+label.substring(label.indexOf("avg")+3)+">");
					//						System.out.println("<count"+label.substring(label.indexOf("avg")+3)+">count("+rootNode.getLabel()+")</count"+label.substring(label.indexOf("avg")+3)+">");


					//					System.out.println("op.getRefOperator().getOperatorId() = " + op.getRefOperator().getOperatorId());
					//					System.out.println("op.getAptId = " + op.getAptId());

					//					System.out.println("op2.getLabel() = " + op2.getLabel());
					//System.out.println("LAMS VEJA " + constructReturn2.toString());

					//							
					//							getAptId());
					//					
					//					pegar ID de quem tem op.getAptId como refAPT pois ele vai ser parent ID de quem eu quero
					//					co


				} // fim if que checa se eh "Aggregate_Average"
				//System.out.println("saindo do FLWOR ");
				//System.out.println("");
				rootNode.setLabel(nodeId);
				rootNode.setIsKeyNode(true);

				constructReturn.addChild(sel);
				constructReturn = sel;
			} //fim for do processamento dos operadores de Funcoes de Agregacao do retorno

			for (int l = 0; l < constructReturn2.getApt().getAptNode().getChildrenNodes().size(); l++) {
				//q.setElmtConstructors(constructReturn2.getApt().getAptNode().getChildrenNodes().get(l).getLabel(), varName);
				
				if (constructReturn2.getApt().getAptNode().getChildrenNodes().get(l).getLabel().toUpperCase().contains("AVG")) {
					label = constructReturn2.getApt().getAptNode().getChildrenNodes().get(l).getLabel();
					elemNames.add(label);
					//System.out.println("");
				}
			}
			for(int i = 0; i < varNames.size(); i++) {
				
				//System.out.println("varNames.size() = " + varNames.size() + " - i = " + i);
				//System.out.println("<sum"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">{sum("+varNames.elementAt(i)+")}</sum"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">");
				String sum = "<sum"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">{sum("+varNames.elementAt(i)+")}</sum"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">";
				//System.out.println("<count"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">{count("+varNames.elementAt(i)+")}</count"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">");
				String count = "<count"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">{count("+varNames.elementAt(i)+")}</count"+elemNames.elementAt(i).substring(elemNames.elementAt(i).indexOf("avg")+3)+">";

				//Testar se a consulta ja possui SUM e/ou COUNT para a mesma varName
				//Se ja possuir, nao pode inserir novamente para usar no AVG, pois isso vai dar inconsistencia na consulta final
				//System.out.println("*** ---- " + q.getInputQuery());
				if (!q.getInputQuery().contains(sum) && !q.getInputQuery().contains(count))
					q.setAvgSubqueryReplacement(sum+count);
				else if (q.getInputQuery().contains(sum) && !q.getInputQuery().contains(count))
					q.setAvgSubqueryReplacement(count);
				else if (!q.getInputQuery().contains(sum) && q.getInputQuery().contains(count))
					q.setAvgSubqueryReplacement(sum);
				else
					q.setAvgSubqueryReplacement("");//Caso ja tenha SUM e COUNT, nao  insere nada na subconsulta relacionado ao AVG, somente no cabecalho.
					
			} //fim for
		
			// Processamento dos Selects para preparao do Construct/Sort
			for (int i=0; i<ret.getSelectOperatorsList().size(); i++){
				AbstractOperator sel = ret.getSelectOperatorsList().get(i);

				// Substituio da varivel pelo NodeId (LCL)
				TreeNode n = sel.getApt().getAptNode().getRootNode();
				
				//System.out.println("!!! " + constructReturn2.getApt().getAptNode().getChildrenNodes().get(i).getLabel() + "- " + "$"+n.getLabel());
				q.setElmtConstructors(constructReturn2.getApt().getAptNode().getChildrenNodes().get(i).getLabel(), "$"+n.getLabel());//para ser usado na montagem da consulta final

				int nodeId = this.getVarNodeId(n.getLabel());
				if (nodeId == -1){
					throw new AlgebraParserException("Variable " + n.getLabel() + " does not exist");
				}
				n.setLabel(nodeId);
				n.setIsKeyNode(true);

				constructReturn.addChild(sel);
				constructReturn = sel;
			} //fim for
			
			// Incluso dos Selects referentes ao ORDER BY, se houver
			if (orderBy != null){			
				q = Query.getUniqueInstance(true);
				q.setOrderByClause(true);
				q.setElementConstructor(false);

				for (int i=0; i<orderBy.getOrderSpecList().size(); i++){
					OrderSpec s = orderBy.getOrderSpecList().get(i);
					TreeNode n = s.getTreeNode().getRootNode();
					int nodeId = this.getVarNodeId(n.getLabel());
					n.setLabel(nodeId);
					n.setIsKeyNode(true);

					// 0- Buscar se j existe operador de Select para a varivel do order by
					AbstractOperator op = constructReturn;
					AbstractOperator opEncontrado = null;
					while (op.getParentOperator() != null){
						TreeNode nn = op.getApt().getAptRootNode();
						if ((nn != null) && (nn.getLabelLCLid() == nodeId)){
							opEncontrado = op;
							break;
						}
						op = op.getParentOperator();
					}

					// 1- Se j existir, faremos um merge das rvores
					if (opEncontrado != null){
						opEncontrado.getApt().mergeTree(n);
					}
					else{
						// 2- Se no existir, criamos um novo operador Select para esta varivel
						SelectOperator sel = new SelectOperator();
						sel.getApt().setAptNode(n);
						constructReturn.addChild(sel);
						constructReturn = sel;				
					}
				}
			}


			//Inclusao dos operadores de selecao abaixo do ultimo nivel dos operadores de construcao
			constructReturn.addChild(this.operator);


			this.operator = constructReturn.getRootOperator();

			processChild = false;

		}

		if (processChild & (node.jjtGetNumChildren()>0)){
			for (int i=0; i<node.jjtGetNumChildren(); i++){
				this.processSimpleNode((SimpleNode)node.jjtGetChild(i), debug);
			}
		}
		
		varNames.clear(); //limpa Vector com as variaveis
		elemNames.clear();

	}

	protected AbstractOperator buildJoin(AbstractOperator op1, AbstractOperator op2) 
			throws AlgebraParserException{

		// Tratamento de mais de um FOR na XQuery (Join de Selects)
		JoinOperator join = new JoinOperator();
		join.addChild(op1);
		join.addChild(op2);
		join.generateApt();
		return join;
	}

	protected void insertForLet(ForLetClause forlet) throws AlgebraParserException{

		if (this.operator == null)
			this.operator = forlet.getOperator();  // Select Operator
		else{
			// Verifica se o nodo raiz nao eh referente a uma outra variavel
			TreeNode n = forlet.getOperator().getApt().getAptRootNode();
			String label = n.getLabel();
			//Se comecar com "$" faz referencia a outra varivel
			if (label.charAt(0) == '$'){

				int varId = this.getVarNodeId(label.substring(1, label.length()));
				// Inclui a arvore abaixo do nodo da variavel
				TreeNode varNode = this.operator.findNodeInPlanById(varId);				

				try {
					Query q = Query.getUniqueInstance(true);
					// Obtem o nome do documento, o nome da colecao e o 
					// caminho xpath da variavel a qual o LET faz referencia.
					String documentName = q.getDocumentNameByVariableName(label);
					String collectionName = q.getCollectionNameByVariableName(label);
					String xpath = q.getXpathByVariableName(label);
					q.setLastReadForLetVariable(label);				

					String completePathLet = xpath + (!q.getXpath().equals("")?"/"+q.getXpath():q.getXpath());
					String subXpath = "";
					String LETPath = (!q.getXpath().equals("")?"/"+q.getXpath():q.getXpath()); // caminho da variavel LET desconsiderando o caminho a variavel de referencia

					Catalog catalog = Catalog.get();

					// cardinalityFor: indica a cardinalidade do ultimo elemento do caminhoXpath indica no FOR ao qual este LET referencia.
					int cardinalityFor = catalog.getCardinality(xpath, documentName, collectionName);

					/* Se cardinalityFor no for maior que 1, o predicado de seleo deve ser adicionado no LET, caso contrrio, no adicione.
					 * Ex.1:    for $c in doc('loja','informacoesLoja')/Loja/Itens/Item
  							   let $a := $c/ResenhaClientes/Resenha
  						Supondo que /Loja/Itens/Item j foi fragmentado como /Loja/Itens/Item[position >= vMin and position < vMax]
  						o LET no precisa de fragmentao, pois o caminho da varivel ao qual se refere j foi fragmentado.

  					   Ex.2:    for $c in doc('loja','informacoesLoja')/Loja/Itens
  							     let $a := $c/Item/ResenhaClientes/Resenha
  						Neste caso, o LET precisa ser fragmentado, pois o atributo de fragmentao, cuja cardinalidade  maior que 1 
  						est especificado nele e no no FOR.  						

					 * */
					if (cardinalityFor <= 1 && !q.isAddedPredicate()) { 

						while ( q.getLastReadCardinality()<=1 ) {

							if (completePathLet.indexOf("/")!=-1) {
								subXpath = subXpath + (!subXpath.equals("")?"/":"") + completePathLet.substring(0,completePathLet.indexOf("/"));
								completePathLet = completePathLet.substring(completePathLet.indexOf("/")+1, completePathLet.length());
							}						
							else {
								subXpath = subXpath + (!subXpath.equals("")?"/":"") + completePathLet;
							}

							int cardinality = catalog.getCardinality(subXpath, documentName, collectionName);

							//GABRIEL
							// it seems that an error due to wrong parameters in get cardinality is ok
							// in this case, acts like a 'break' for the loop.
							if (cardinality == -1 || cardinality == 0)
								throw new Exception();

							if (cardinality > 1){

								if (LETPath.indexOf(completePathLet)!=-1) {
									LETPath = LETPath.substring(0, LETPath.indexOf(completePathLet)-1); // obtem o caminho do let ate o ultimo elemento cuja cardinalidade foi verificada.
								}

								String OriginalPath = xpath + (!q.getXpath().equals("")?"/"+q.getXpath():q.getXpath());

								/* Se o caminho do let est correto, adicione os predicados.
								 * Ex.:   for $order in doc('loja','informacoesLoja')/Loja/Itens
								 * 		  let $l := $order/Item/ResenhaClientes ...
								 * Adicione, pois OriginalPath  Loja/Itens/Item/ResenhaClientes, considerando o elemento Item como escolhido
								 * para a fragmentacao, o subcaminho Loja/Itens/Item ser fragmentado. Pois 
								 * No entanto, se tivssemos:
								 * 		  for $order in doc('loja','informacoesLoja')/Loja/Itens
								 * 		  let $l := $order/Item/ResenhaClientes/Cliente/Compra/Item/Preco
								 * O sistema reconhece que o Item para a fragmentacao  Loja/Itens/Item e no 
								 * Loja/Itens/Item/ResenhaClientes/Cliente/Compra/Item.  
								 */

								if ( OriginalPath.indexOf(xpath + LETPath) != -1 && !q.isAddedPredicate() ) { // Se ainda no adicionou nenhum predicado no caminho xpath da varivel em questao.								

									SimpleVirtualPartitioning svp = SimpleVirtualPartitioning.getUniqueInstance(true);								
									svp.setCardinalityOfElement(cardinality);								

									// cria os sub-intervalos para a varivel relacionada ao caminho xpath lido.								
									svp.getSelectionPredicate(-1,q.getFragmentationVariable());								

									q.setAddedPredicate(true); // Evita a adio do mesmo predicado nos sub-elementos restantes com cardinalidade maior que 1.							

									int posLetVariable = q.getInputQuery().indexOf(q.getLastReadLetVariable());
									String pathReplaceTo = q.getInputQuery().substring(posLetVariable,q.getInputQuery().length()); // substring da variavel Let ate o final
									pathReplaceTo = pathReplaceTo.substring(0, pathReplaceTo.indexOf(q.getXpath())-1); // substring da variavel Let ate a posicao do caminho xpath do let
									pathReplaceTo = pathReplaceTo + LETPath;

									// Acrescenta o predicado ao caminho xpath
									svp.addVirtualPredicates(pathReplaceTo, q.getFragmentationVariable(), cardinality);
								}	
							}	

							q.setLastReadCardinality(cardinality);
						}	// fim while
					} // fim if cardinalityFor
				} catch (Exception e) {
					// TODO: handle exception
				}

				n.setLabel(varNode.getNodeId());
				AbstractOperator op = forlet.getOperator();
				op.addChild(this.operator);
				this.operator = op;
			}
			else{ // Outra view global = join
				this.operator = this.buildJoin(this.operator, forlet.getOperator());
			}
		}

		Variable var = forlet.getVariable();
		if (var != null){
			this.vars.add(var);
		}
	}

	protected void insertComparison(ComparisonExpr comp) throws IOException{

		// Verificao se a comparao era predicado de juno
		if (comp.isJoinComparison()){
			// Inserir predicado de juno no operador Join
			this.insertJoinPredicate(comp);
		}
		// Se for comparao utilizando uma funo tipo agregaao (count, avg)
		else if (comp.isFunctionComparison()){
			this.insertFunctionFilter(comp);
		}
		else{
			this.insertTreeNodeSimplePath(comp.getTreeNode().getRootNode());
		}
	}

	protected void insertTreeNodeSimplePath(TreeNode node){
		// Varivel da comparacao
		String varName = node.getLabel();

		// Busca do NodeId referente  varivel
		int nodeId = this.getVarNodeId(varName);

		// Busca do nodo onde a comparacao sera inserida
		TreeNode nodePosition = this.operator.findNodeInPlanById(nodeId);

		// Remocao do Root node com a variavel
		node = node.getChild(0);
		node.setParentNode(null);

		// Inclusao do nodo de comparacao no plano
		nodePosition.addChild(node);
	}

	protected void insertJoinPredicate(ComparisonExpr comp) throws IOException{

		String pred = comp.getJoinPredicate();

		TreeNode n1 = comp.getTreeNode().getRootNode();
		TreeNode n2 = comp.getTreeNodeJoin().getRootNode();

		// Busca dos key nodes referentes aos LCLs encontrados
		int lclKN1 = this.getVarNodeId(n1.getLabel());
		int lclKN2 = this.getVarNodeId(n2.getLabel());	

		// Busca do operador Join que possui os LCLs dos Key Nodes encontrados
		ArrayList<AbstractOperator> joins = this.operator.getOperatorsListByType("Join");
		for (int i=0; i< joins.size(); i++){
			AbstractOperator join = joins.get(i);
			TreeNode joinNode = join.getApt().getAptRootNode();

			int inseridos = 0;
			// Inclusao dos nodos do predicado de juncao no operador Join
			if (joinNode.getChild(0).getLabelLCLid() == lclKN1){
				joinNode.getChild(0).addChild(n1.getChild(0));
				inseridos++;
			}
			else if (joinNode.getChild(0).getLabelLCLid() == lclKN2){
				joinNode.getChild(0).addChild(n2.getChild(0));				
				inseridos++;
			}
			if (joinNode.getChild(1).getLabelLCLid() == lclKN1){
				joinNode.getChild(1).addChild(n1.getChild(0));				
				inseridos++;
			}
			else if (joinNode.getChild(1).getLabelLCLid() == lclKN2){
				joinNode.getChild(1).addChild(n2.getChild(0));				
				inseridos++;
			}						

			// Inclusao do predicado de juncao
			if (inseridos == 2){
				join.getPredicateList().add(pred);
			}
			else if (inseridos == 1){
				// Verificacao se um dos filhos aponta para outro Join
				TreeNode refNode0 = join.findNodeInChildrenById(joinNode.getChild(0).getLabelLCLid());
				TreeNode refNode1 = join.findNodeInChildrenById(joinNode.getChild(1).getLabelLCLid());
				if ((refNode0.getLabel().equals("Join_root")) || (refNode1.getLabel().equals("Join_root"))){
					// Como um dos filhos eh outro Join, o predicado eh deste Join
					join.getPredicateList().add(pred);
				}
			}
		}		

		Query q = Query.getUniqueInstance(true);
		if ( !q.isJoinCheckingFinished() ){ // Se nao concluiu a verificao de qual juncao possui menor cardinalidade.

			int idVar1 = Integer.parseInt(pred.substring(0, pred.indexOf("=")));
			int idVar2 = Integer.parseInt(pred.substring(pred.indexOf("=")+1, pred.length()));

			TreeNode nc1 = n1.findNode(idVar1);
			TreeNode nc2 = n2.findNode(idVar2);		

			//ej.verifyJoins(n1.getLabel(),( nc1!=null ? nc1.getLabel(): ""), n2.getLabel(), ( nc2!=null ? nc2.getLabel(): ""));
			String completePath1 = nc1.getLabel();
			String completePath2 = nc2.getLabel();
			String atr1 = nc1.getLabel();
			String atr2 = nc2.getLabel();

			while ( nc1.getParentNode() !=null && !nc1.getParentNode().getLabel().contains("(") && !nc1.getParentNode().getLabel().contains("Join_root")) {

				nc1 = nc1.getParentNode();
				completePath1 = nc1.getLabel() + "/" + completePath1;
			}

			while ( nc2.getParentNode() !=null && !nc2.getParentNode().getLabel().contains("(") && !nc2.getParentNode().getLabel().contains("Join_root")) {

				nc2 = nc2.getParentNode();
				completePath2 = nc2.getLabel() + "/" + completePath2;
			}		

			completePath1 = "$" + n1.getLabel() + "/" +completePath1;
			completePath2 = "$" + n2.getLabel() + "/" +completePath2;						

			ExistsJoinOperation ej= new ExistsJoinOperation(q.getInputQuery());
			ej.verifyJoins(completePath1, completePath2, n1.getLabel(), n2.getLabel(), atr1, atr2);
		}	
	}

	protected void insertFunctionFilter(ComparisonExpr comp){

		// Criao de um operador para a funo
		FunctionOperator funcOp = FunctionOperator.buildFunction(comp.getFunctionName());

		TreeNode node = comp.getTreeNode().getRootNode();		
		String label = node.getLabel();		

		// Atualizao do nodo raz para trocar o nome da varivel pelo seu LCL
		node.setLabel(this.getVarNodeId(node.getLabel()));
		node.setIsKeyNode(true);

		// Incluso do predicado do TreeNode no predicado do operador
		TreeNode lastNode = node;
		while(lastNode.hasChield()){
			lastNode = lastNode.getChild(0);
			lastNode.setMatchSpec(TreeNode.MatchSpecEnum.ZERO_MORE); 
		}
		Predicate pred = lastNode.getPredicate();
		String lastNodeLCL = lastNode.getLCL(); 
		lastNode.setPredicate(null); // remoo do predicado do nodo

		funcOp.getApt().setAptNode(node);
		funcOp.getPredicateList().add(lastNodeLCL + pred.toString());  // inclusao do predicado no operador		

		// Inclusao do Operador da funcao no plano algebrico global
		funcOp.addChild(this.operator);
		this.operator = funcOp;

		try {
			Query q = Query.getUniqueInstance(true);					
			Hashtable<String, String> letClause = q.getLetClauses();
			Hashtable<String, String> forClause = q.getForClauses();

			if ( letClause != null && letClause.get("$"+label)!= null  ) {
				//String path = letClause.get("$"+label);
				q.setAggregateFunctions(comp.getFunctionName(), "$"+label, pred.toString());
			}
			else if ( forClause != null && forClause.get("$"+label)!= null  ) {
				//String path = forClause.get("$"+label);
				q.setAggregateFunctions(comp.getFunctionName() + pred.toString(), "$"+label, pred.toString());
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	protected int getVarNodeId(String varName){
		int returnInt = -1;
		if (this.vars != null){
			for(int i=0; i<this.vars.size();i++){
				Variable var = (Variable)this.vars.get(i);
				if (var.getVarName().equals(varName)){
					returnInt = var.getNodeId();
					break;
				}
			}
		}
		return returnInt;
	}	


}
