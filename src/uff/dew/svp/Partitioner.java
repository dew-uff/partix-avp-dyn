package uff.dew.svp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import uff.dew.avp.commons.Utilities;
import uff.dew.svp.catalog.Catalog;
import uff.dew.svp.db.DatabaseException;
import uff.dew.svp.db.DatabaseFactory;
import uff.dew.svp.engine.XQueryEngine;
import uff.dew.svp.engine.XQueryEngineException;
import uff.dew.svp.exceptions.PartitioningException;
import uff.dew.svp.fragmentacaoVirtualSimples.DecomposeQuery;
import uff.dew.svp.fragmentacaoVirtualSimples.ExistsJoinOperation;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SimpleVirtualPartitioning;
import uff.dew.svp.fragmentacaoVirtualSimples.SubQuery;

/**
 * User class to access Simple Virtual Partitioning
 * functionality. 
 *
 */
final public class Partitioner 
{
	private ArrayList<String> docQueries;
	private ArrayList<String> docQueriesWithoutFragmentation;
	private String originalQuery;
	private String inputQuery;
	private int nfragments;
	private String colName = "";
	private String xquery;
	private XQueryEngine engine;
	private String subInitialFragments = "";

	private ExecutionContext context;


	/**
	 * Creates a Partitioner object that uses catalog strategy to execute fragmentation
	 * 
	 * @param catalogStream the InputStream object to the file containing the catalog
	 */
	public Partitioner(InputStream catalogStream)
	{
		Catalog.get().populateCatalogFromFile(catalogStream);
		cleanSingletons();
	}

	/**
	 * Creates a Partitioner object that uses database strategy to execute fragmentation
	 * It will pose several queries to database to determine cardinality of elements
	 * 
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param databaseName
	 * @param dbType
	 * @throws DatabaseException
	 */
	public Partitioner(String hostname, int port, String username, String password, 
			String databaseName, String dbType) throws DatabaseException {
		DatabaseFactory.produceSingletonDatabaseObject(hostname,port,username,password,databaseName,dbType);
		Catalog.get().setDatabaseObject(DatabaseFactory.getSingletonDatabaseObject());
		Catalog.get().setDbMode(true);
		cleanSingletons();
	}

	private void cleanSingletons() {
		// need to do this to get rid of garbage from previous execution
		Query.getUniqueInstance(false);
		SimpleVirtualPartitioning.getUniqueInstance(false);
		SubQuery.getUniqueInstance(false);
		DecomposeQuery.getUniqueInstance(false);        
	}

	//Luiz Matos
	//Para os casos em que a coleção possui vários documentos com conteúdo/estrutura diferentes
	public List<String> executePartitioning(String query, int nFragments, String collectionName) 
			throws PartitioningException {

		inputQuery = query;
		nfragments = nFragments;
		colName = collectionName;

		// to mimic PartiX-VP flow.
		svpPressed();

		List<String> fragments = getFragments();

		if (fragments == null) {
			throw new PartitioningException("queries should not be null!");
		}
		try {
			context = new ExecutionContext(fragments.get(0));
		} catch (IOException e) {
			throw new PartitioningException("queries should not be null!");
		}

		return fragments;
	}

	public List<String> executePartitioning(String query, int nFragments) 
			throws PartitioningException {

		inputQuery = query;
		nfragments = nFragments;

		// to mimic PartiX-VP flow.
		svpPressed();

		List<String> fragments = getFragments();

		if (fragments == null) {
			throw new PartitioningException("queries should not be null!");
		}

		try {
			context = new ExecutionContext(fragments.get(0));
		} catch (IOException e) {
			throw new PartitioningException("queries should not be null!");
		}

		return fragments;
	}

	public ExecutionContext getExecutionContext() {
		return context;
	}

	private void xqueryPressed() throws PartitioningException {

		Query q = Query.getUniqueInstance(true);

		/* Define o tipo de consulta (collection() ou doc()) e, caso seja sobre uma colecao 
		 * retorna as sub-consultas geradas, armazenando-as no objeto docQueries.
		 */
		q.setInputQuery(inputQuery);
		docQueries = q.setqueryExprType(inputQuery);

		if ( docQueries!=null && docQueries.size() > 0 ) { // Eh diferente de null, quando consulta de entrada for sobre uma colecao

			docQueriesWithoutFragmentation = docQueries;                                
		}
		else if (q.getqueryExprType()!=null && q.getqueryExprType().equals("document")) { // consulta de entrada sobre um documento. 
			q.setInputQuery(inputQuery);
		}
		else if (q.getqueryExprType()!=null && q.getqueryExprType().equals("collection")) { // consulta de entrada sobre uma colecao.
			throw new PartitioningException("Erro ao gerar sub-consultas para a colecao indicada. Verifique a consulta de entrada.");
		}
	}

	private void svpPressed() throws PartitioningException {

		xqueryPressed();

		try {
			SimpleVirtualPartitioning svp = SimpleVirtualPartitioning.getUniqueInstance(false);
			Query q = Query.getUniqueInstance(true);
			if (q.getqueryExprType()!=null && q.getqueryExprType().equals("collection")){

				if (docQueries!=null){

					originalQuery = inputQuery;
					for (String docQry : docQueries) {

						inputQuery = docQry;

						executeXQuery();                                            
					}

					inputQuery = originalQuery;
				}
			}
			else {
				svp.setNumberOfNodes(nfragments);
				svp.setNewDocQuery(true);                                                       
				executeXQuery();                            
			}
		}
		catch (XQueryEngineException e) {
			throw new PartitioningException(e);
		}
	}


	private void executeXQuery() throws XQueryEngineException {

		ExistsJoinOperation ej = new ExistsJoinOperation(inputQuery);
		ej.verifyInputQuery();
		Query q = Query.getUniqueInstance(true);
		q.setLastReadCardinality(-1);
		q.setJoinCheckingFinished(false);               

		if ((xquery == null) || (!xquery.equals(inputQuery))){ 
			xquery = inputQuery; //  consulta de entrada                  
		}       

		if ( q.getqueryExprType()!= null && !q.getqueryExprType().contains("collection") ) { // se a consulta de entrada nao contem collection, execute a fragmentacao virtual.

			engine = new XQueryEngine();

			engine.execute(xquery, false); // Para debugar o parser, passe o segundo parametro como true.                

			q.setJoinCheckingFinished(true);

			if (q.isExistsJoin()){
				q.setOrderBy("");  
				q.setGroupBy(""); //impede que $varname do groupby seja inserido de maneira duplicada no cabecalho da subquery
				q.clearAvgSubqueryReplacement(); //impede que as substrings de substituicao de AVG sejam duplicadas nas subqueries
				engine.execute(xquery, false); // Executa pela segunda vez, porem desta vez fragmenta apenas um dos joins
			}               
		}
		else {  // se contem collection         

			// Efetua o parser da consulta para identificar os elementos contidos em funcoes de agregacao ou order by, caso existam.
			q.setOrderBy("");
			q.setGroupBy("");
			engine = new XQueryEngine();
			engine.execute(originalQuery, false);

			if (q.getPartitioningPath()!=null && !q.getPartitioningPath().equals("")) {
				SimpleVirtualPartitioning svp = new SimpleVirtualPartitioning();
				svp.setCardinalityOfElement(q.getLastCollectionCardinality());
				svp.setNumberOfNodes(nfragments);                       
				svp.getSelectionPredicateToCollection(q.getVirtualPartitioningVariable(), q.getPartitioningPath(), xquery);                                         
				q.setAddedPredicate(true);
			}
		}
	}

	private List<String> getFragments() {

		Query q = Query.getUniqueInstance(true);
		SubQuery sbq = SubQuery.getUniqueInstance(true);       

		if ( sbq.getSubQueries()!=null && sbq.getSubQueries().size() > 0 ) {

			List<String> results = new ArrayList<String>(sbq.getSubQueries().size());


			//Aqui ocorre a montagem do header e a remoção do ORDER BY das subconsultas/fragmentos
			for ( String initialFragments : sbq.getSubQueries() ) {

				StringBuilder result = new StringBuilder();
				
				//Remove ORDER BY, se existir
				if (q.isOrderByClause()) {
					initialFragments = initialFragments.substring(0, initialFragments.toUpperCase().indexOf("ORDER BY ")) + initialFragments.substring(initialFragments.toUpperCase().indexOf("RETURN "), initialFragments.length());
				}

				subInitialFragments = initialFragments;

				//Luiz Matos - usado para permitir o processamento de variáveis do tipo $varname e não somente $varname/element no order by
				//Artifício adotado foi adicionar na query original a string "/VP" após $varname e removê-la agora quando ocorrer esses casos
				if (q.getOrderBy().contains("/VP")) {
					int posAux = q.getOrderBy().indexOf("/VP");
					while (posAux != -1) {
						String orderBy = q.getOrderBy().substring(0, posAux) + q.getOrderBy().substring(posAux+3, q.getOrderBy().length());
						q.setOrderBy(orderBy);
						posAux = q.getOrderBy().indexOf("/VP");
					} //fim while
				} //fim for

				result.append("<GROUPBY>" + q.getGroupBy() + "</GROUPBY>\r\n");
				result.append("<ORDERBY>" + q.getOrderBy() + "</ORDERBY>\r\n");
				result.append("<ORDERBYTYPE>" + q.getOrderByType() + "</ORDERBYTYPE>\r\n");
				//result.append("<AGRFUNC>" + (q.getAggregateFunctions()!=null?q.getAggregateFunctions():"") + "</AGRFUNC>#\r\n");
				result.append("<ELMTCONSTRUCT>" + q.getElmtConstructors() + "</ELMTCONSTRUCT>\r\n");
				result.append("<AGRFUNC>" + q.getAggregateReturn() + "</AGRFUNC>#\r\n");
				//				result.append("<AGRFUNC>{sum($quantity)=sum($quantity):record/sum_qty, sum($extendedprice)=sum($extendedprice):record/sum_base_price, sum($discountprice)=sum($discountprice):record/sum_disc_price, " +
				//				              " sum($charge)=sum($charge):record/sum_charge, avg($quantity)=sum($quantity):record/sum_qty, count($quantity)=count($quantity):record/count_qty, " +
				//	                          " avg($extendedprice)=sum($extendedprice):record/sum_price, count($extendedprice)=count($extendedprice):record/count_price, " +	
				//	                          " avg($discount)=sum($discount):record/sum_disc, count($discount)=count($discount):record/count_disc, " +
				//	                          " count($lineitem)=count($lineitem):record/count_order }</AGRFUNC>#\r\n");

				//				result.append("<AGRFUNC>{sum($quantity)=sum($quantity):record/sum_qty, sum($extendedprice)=sum($extendedprice):record/sum_base_price, sum($discountprice)=sum($discountprice):record/sum_disc_price, " +
				//	              " sum($charge)=sum($charge):record/sum_charge, avg($quantity)=avg($quantity):record/avg_qty, avg($extendedprice)=avg($extendedprice):record/avg_price, avg($discount)=avg($discount):record/avg_disc, " +
				//                  " count($lineitem)=count($lineitem):record/count_order }</AGRFUNC>#\r\n");

				//result.append("<AGRFUNC>{ avg($partsize)=avg($partsize):record/avg_size }</AGRFUNC>#\r\n");



				//result.append("<AGRFUNC>{count($tax)=count($tax):/count_tax, sum($tax)=sum($tax):/sum_tax, avg($tax)=avg($tax):/avg_tax}</AGRFUNC>#\r\n");

				//Luiz Matos - usado para os casos em que a coleção possui vários documentos com conteúdo/estrutura diferentes
				//Artifício que adiciona "collectioName/" antes do nome do documento informado em cada funcao DOC()
				int posDoc = initialFragments.toUpperCase().indexOf("DOC(");
				int nextPosDoc = subInitialFragments.toUpperCase().indexOf("DOC(");
				if (colName != "" && posDoc != -1) {
					while (nextPosDoc != -1) {
						initialFragments = initialFragments.substring(0, posDoc+5) + colName + "/" + initialFragments.substring(posDoc+5, initialFragments.length());
						subInitialFragments = initialFragments.substring(posDoc+5, initialFragments.length());
						nextPosDoc = subInitialFragments.toUpperCase().indexOf("DOC(");
						posDoc = (posDoc+5) + nextPosDoc;
					}
				} //fim if

				//Luiz Matos
				//Artíficio que remove trecho com AVG e adiciona trecho com SUM e COUNT nas subconsultas
				if (q.getInputQuery().substring(q.getInputQuery().toUpperCase().indexOf("RETURN ")).toUpperCase().contains("AVG_")) { //checa se a consulta original tem AVG no return
					subInitialFragments = initialFragments.trim(); //copia o conteudo de initialFragments para subInitialFragments

					int posAvg = initialFragments.toUpperCase().trim().indexOf("<AVG_");
					int posEndAvg = initialFragments.toUpperCase().trim().indexOf("</AVG_");

					subInitialFragments = initialFragments.substring(posEndAvg+6, initialFragments.length()).trim();
					//int nextPosAvg = initialFragments.toUpperCase().trim().indexOf("<AVG_");
					int nextPosAvg = -1;
					if (subInitialFragments.toUpperCase().trim().indexOf("<AVG_") != -1) //checa se tem outro AVG
						nextPosAvg = posEndAvg + 6 + subInitialFragments.toUpperCase().trim().indexOf("<AVG_");

					int endTag = subInitialFragments.trim().indexOf(">");

					//					System.out.println("initialFragments = " + initialFragments);
					//					System.out.println("subInitialFragments - endTag = " + subInitialFragments + " ; " + endTag);
					//					System.out.println("posAvg - posEndAvg - nextPosAvg = " + posAvg + " ; " + posEndAvg + " ; " + nextPosAvg);

					//	while (nextPosAvg != -1) {

					for(int i = 0; i < q.getAvgSubqueryReplacement().size(); i++) {//insere trecho com SUM e COUNT que substituirá trecho com AVG

						initialFragments = initialFragments.substring(0, posAvg) + q.getAvgSubqueryReplacement().elementAt(i) + initialFragments.substring(posEndAvg+6+(endTag+1), initialFragments.length()).trim();
						posAvg = initialFragments.toUpperCase().trim().indexOf("<AVG_");;
						posEndAvg = initialFragments.toUpperCase().trim().indexOf("</AVG_");

						if (posEndAvg != -1) {//caso possua mais de um AVG
							subInitialFragments = initialFragments.substring(posEndAvg+6, initialFragments.length()).trim();

							nextPosAvg = posEndAvg + 6 + subInitialFragments.toUpperCase().trim().indexOf("<AVG_");
							//posAvg = (posAvg+6) + nextPosAvg;
							//						posAvg =  initialFragments.toUpperCase().trim().indexOf("<AVG_");
							//						posEndAvg = initialFragments.toUpperCase().trim().indexOf("</AVG_");
							endTag = subInitialFragments.trim().indexOf(">");
							//						if (posAvg == -1) {  //para o último AVG
							//							initialFragments = initialFragments.substring(0, posAvg) + q.getAvgSubqueryReplacement().elementAt(i) + initialFragments.substring(nextPosAvg, initialFragments.length()).trim();
							//
							//						}
							//							System.out.println("subInitialFragments - endTag = " + subInitialFragments + " ; " + endTag);
							//							System.out.println("posAvg  posEndAvg - nextPosAvg = " + posAvg + " - " + posEndAvg + " - " + nextPosAvg);
							//							System.out.println("");
						} else {//caso possua um unico AVG
							//								subInitialFragments = initialFragments.substring(posEndAvg+6, initialFragments.length()).trim();
							//								endTag = subInitialFragments.trim().indexOf(">");
							break; 
						}

					}//fim for

				} // fim if
				//Elimina funcoes/expressoes repetidas
				
				result.append(initialFragments);
				results.add(result.toString());

			}//fim for principal

			q.clearAvgSubqueryReplacement();

			return results;//Contém as subconsultas/fragmentos
		}
		else {

			if ( this.docQueriesWithoutFragmentation != null && this.docQueriesWithoutFragmentation.size() > 0 ) { // para consulta que nao foram fragmentadas pois nao ha relacionamento de 1 para n.

				return this.docQueriesWithoutFragmentation;
			}
			else { // nao gerou fragmentos e nao ha consultas de entrada. Ocorreu algum erro durante o parser da consulta. 
				return null;
			}
		}
	}
}
