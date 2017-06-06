package uff.dew.svp.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQResultSequence;

import uff.dew.svp.db.Database;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SubQuery;

public class OnlyTempCollectionStrategy implements CompositionStrategy {

	private Database db;
	private OutputStream output = null;
	private boolean started = false;
	private int count = 0;
	private SubQuery subQueryObj;
	private Query queryObj;
	private String tempCollectionName;

	public OnlyTempCollectionStrategy(Database db, Query q, SubQuery sbq, OutputStream output, String tempCollectionName) {
		this.db = db;
		this.subQueryObj = sbq;
		this.queryObj = q;
		this.output = output;
		this.tempCollectionName = tempCollectionName;
	}

	public void loadPartial(String collectionName) throws IOException {
		try {
			if (!started) {
				started = true;
				count = 0;
				db.createCollection(collectionName);
			}
		} catch (XQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String combinePartials2(String tempCollection) throws IOException {
		String auxTime;
		SubQuery sbq = this.subQueryObj;

		// construct the query to get the result from the temp collection
		String finalQuery = constructFinalQuery();

		//System.out.println(finalQuery);
		String header = "";
		output.write(header.getBytes());

		try {
			//medir tempo execucao
			long startExecTime = System.nanoTime();
			XQResultSequence rs = db.executeQuery(finalQuery);
			long elapsedExecTime = (System.nanoTime() - startExecTime)/1000000; // em ms

			auxTime = String.valueOf(elapsedExecTime)+";";

			//medir tempo escrita
			long startWriteTime = System.nanoTime();
			while (rs.next()) {
				String item = rs.getItemAsString(null);
				output.write(item.getBytes());
				output.write("\r\n".getBytes());
				output.flush();
			}

			db.freeResources(rs);

			String footer = "";
			output.write(footer.getBytes());

			long elapsedWriteTime = (System.nanoTime() - startWriteTime)/1000000; // em ms
			auxTime = auxTime + String.valueOf(elapsedWriteTime);

		}
		catch (XQException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
		return auxTime;
	}

	@Override
	public void cleanup() {
		try {
			//db.deleteCollection(AVPConst.TEMP_COLLECTION_NAME);
			db.deleteCollection(tempCollectionName);
		} catch (XQException e) {
			// well. what can i do?
			e.printStackTrace();
		}
	}

	/**
	 * From FinalResult.getFinalResult (adapted)
	 * 
	 * @return
	 * @throws IOException
	 */
	private String constructFinalQuery() throws IOException {

		//System.out.println("tempCollectionName = " + tempCollectionName);
		Query q = this.queryObj;
		SubQuery sbq = this.subQueryObj;

		String finalResultXquery = "";
		String orderByClause = "";
		String groupByClause = "";
		String variableName = "$ret";

		//System.out.println("q.getInputQuery() = " + q.getInputQuery());
		// possui funcoes de agregacao com variaveis na clausula LET.
		if (q.getAggregateFunctions() != null  && q.getAggregateFunctions().size() > 0) { 
			finalResultXquery = "<results> { \r\n"
					//+ " let $c := collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/" //Alterado por Luiz Matos, para compatibilidade c/ consultas TPC-H
					//+ " for $c in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/"
					+ " for $c in collection('"+tempCollectionName+"')/partialResult/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "")
					+ "/" + sbq.getElementAfterConstructor().replaceAll("[</>]", "") + "\r\n";

			//Luiz Matos - usado para inserir o LET das variaveis que constam no GROUP BY
			//Artifício adotado foi adicionar na query final o LET $varname := $var/element das $varname que constam no group by
			if (!q.getOrderBy().trim().equals("") || !q.getGroupBy().trim().equals("")) {//verifica se tem ORDER BY ou GROUP BY na consulta original
				String subOrder[] = q.getOrderBy().split("/"); //separa $var1/$var2/$varN
				String subGroup[] = q.getGroupBy().split("/"); //separa $var1/$var2/$varN

				if(!q.getOrderBy().trim().equals("")) { //checa se tem order by 
					for(String varName : subOrder) {

						if (varName.toUpperCase().contains("AVG(") || varName.toUpperCase().contains("COUNT("))
							varName = varName.substring(varName.indexOf("$"), varName.indexOf(")"));
						finalResultXquery += " " + generateLetClauses(varName); //obtém clausula LET da variável do order by
					}
				}//fim if

				if(!q.getGroupBy().trim().equals("")) { //checa se tem group by 
					for(String varName : subGroup)  {
						if (!finalResultXquery.toUpperCase().contains("LET " + varName.toUpperCase()) && !varName.toUpperCase().contains("AVG(") && !varName.toUpperCase().contains("COUNT(")) //obtém clausula LET da variável do group by somente se a mesma não já tiver sido inserida por conta de um order by	
							finalResultXquery += " " + generateLetClauses(varName); //obtém clausula LET da variável do group by
					}//fim for
					finalResultXquery += " group by " + q.getGroupBy().replace("/", ", ") + "\r\n"; //insere primeiro o group by
				}
				if (!q.getOrderBy().trim().equals("")) {//verifica se tem ORDER BY na consulta original para inseri-lo depois do group by
					finalResultXquery += " order by " + q.getOrderBy().replace("/", ", ") + " " + q.getOrderByType() + "\r\n";
				}
			}//fim if
			finalResultXquery += " return \r\n" + " <"
					+ sbq.getElementAfterConstructor().replaceAll("[</>]", "")
					+ ">";

			//Adicionando elementos que nao possuem funcao de agregacao
			Set<String> keys2 = q.getElmtConstructors().keySet();
			for (String construc : keys2) {
				String value = q.getElmtConstructors().get(construc);
				finalResultXquery += "\r\n    <" + construc.trim() + ">{" + value + "}</" + construc.trim() + ">";
			}

			//Adicionando elementos que possuem funcao de agregacao
			Set<String> keys = q.getAggregateFunctions().keySet();
			for (String function : keys) {
				String expression = q.getAggregateFunctions().get(function);
				String elementsAroundFunction = "";
				//System.out.println("expression = " + expression);
				if (expression.indexOf(":") != -1) {
					elementsAroundFunction = expression.substring(expression.indexOf(":") + 1, expression.length());
					expression = expression.substring(0, expression.indexOf(":"));
				}
				//				System.out.println("elementsAroundFunction = " + elementsAroundFunction);
				//				System.out.println("expression = " + expression);

				// o elemento depois do return possui sub-elementos.
				if (elementsAroundFunction.indexOf("/") != -1) { 
					// elementsAroundFunction =
					// elementsAroundFunction.substring(elementsAroundFunction.indexOf("/")+1,
					// elementsAroundFunction.length());
					String[] elm = elementsAroundFunction.split("/");

					for (String openElement : elm) {
						//System.out.println("--FinalResult.getFinalResult():"+elm.length+","+sbq.getElementAfterConstructor().replaceAll("[</>]", "") +",el:"+openElement);
						//
						if (!openElement.equals("") && !openElement.equals(sbq.getElementAfterConstructor().replaceAll("[</>]", ""))) {
							//System.out.println("FinalResult.getFinalResult(); armazenar o el:::"+openElement);
							finalResultXquery = finalResultXquery + "\r\n    " + "<" + openElement + "> ";
						}
					}
					//System.out.println("expression");
					elm = elementsAroundFunction.split("/");
					String subExpression = expression.substring(expression.indexOf("$"), expression.length());
					//System.out.println("FinalResult.getFinalResult(); subExpression o el:::"+subExpression);

					if (subExpression.indexOf("/") != -1) { // agregacao com caminho xpath. Ex.: count($c/total)
						subExpression = subExpression.substring(subExpression.indexOf("/") + 1, subExpression.length());
						// System.out.println("FinalResult.getFinalResult(); depois alterar o el:::"+subExpression+",el:"+elementsAroundFunction);
						expression = expression.replace("$c/" + subExpression, "$c/" + elementsAroundFunction + ")");

						// System.out.println("FinalResult.getFinalResult(); depois alterar o expression:::"+expression);

					} else { // agregacao sem caminho xpath. Ex.: count($c)
						String variable = expression.substring(expression.indexOf('$'),expression.indexOf(')'));
						//System.out.println("variable = " + variable);
						expression = expression.replace(variable, "$c" + elementsAroundFunction);
						//System.out.println("elementsAroundFunction = " + elementsAroundFunction);
					}

					if (expression.indexOf("count(") >= 0) {
						expression = expression.replace("count(", "sum("); // pois deve-se somar os valores ja previamente computados nos resultados parciais.
					} 
					//Aqui deve entrar a insercao do calculo com a soma dos sums DIV soma dos counts da variable
					else if (expression.indexOf("avg(") >= 0) {
						String elementsAroundFunctionSum = elementsAroundFunction.replace("/avg_", "/sum_");
						String elementsAroundFunctionCount = elementsAroundFunction.replace("/avg_", "/count_"); 
						expression = expression.replace("avg(", "sum(");
						expression = "sum($c" + elementsAroundFunctionSum + ") div sum($c" + elementsAroundFunctionCount + ")";
						//						expression = expression.replace("/avg_", "/sum_");
						//						expression = expression + " div "
						//sum($tax):/sum_tax?sum($tax):/count_tax
					}
					//System.out.println("expression = " + expression);
					finalResultXquery = finalResultXquery + "{" + expression + "}";

					for (int i = elm.length - 1; i >= 0; i--) {
						String closeElement = elm[i];

						if (!closeElement.equals("") && !closeElement.equals(sbq.getElementAfterConstructor().replaceAll("[</>]", ""))) {
							//System.out.println("FinalResult.getFinalResult(); armazenar o el:::"+closeElement);
							finalResultXquery = finalResultXquery + " </" + closeElement + ">";
						}
					}

				} 	// o elemento depois do return NAO possui sub-elementos.
				else { // apos o elemento depois do return estah a funcao de agregacao. ex.: return <resp> count($c) </resp>
					elementsAroundFunction = "";
					expression = expression.replace("$c)", "$c/" + sbq.getElementAfterConstructor().replaceAll("[</>]", "") + ")");
					//System.out.println("FinalResult.getFinalResult(); entrei!!!!!!!!!!!"+expression+","+sbq.getElementAfterConstructor());

					String subExpression = expression.substring(expression.indexOf("$"), expression.length());

					if (subExpression.indexOf("/") != -1) { // agregacao com caminho xpath. Ex.: count($c/total)
						subExpression = subExpression.substring(subExpression.indexOf("/") + 1, subExpression.length());
						//System.out.println("FinalResult.getFinalResult(); depois alterar o el:::"+subExpression+",el:"+elementsAroundFunction);
						expression = expression.replace("$c/" + subExpression, "$c/" + sbq.getElementAfterConstructor().replaceAll("[</>]", "") + ")");

						// System.out.println("FinalResult.getFinalResult(); depois alterar o expression:::"+expression);

					} else { // agregacao sem caminho xpath. Ex.: count($c)
						expression = expression.replace("$c", "$c/" + sbq.getElementAfterConstructor().replaceAll("[</>]", ""));
					}

					if (expression.indexOf("count(") >= 0) {
						expression = expression.replace("count(", "sum("); // pois deve-se somar os valores ja previamente computados nos resultados parciais.
					} 
					//Aqui deve entrar a insercao do calculo com a soma dos sums DIV soma dos counts da variable
					else if (expression.indexOf("avg(") >= 0) {
						String elementsAroundFunctionSum = elementsAroundFunction.replace("/avg_", "/sum_");
						String elementsAroundFunctionCount = elementsAroundFunction.replace("/avg_", "/count_"); 
						expression = expression.replace("avg(", "sum(");
						expression = "sum($c" + elementsAroundFunctionSum + ") div sum($c" + elementsAroundFunctionCount + ")";
						//						expression = expression.replace("/avg_", "/sum_");
						//						expression = expression + " div "
						//sum($tax):/sum_tax?sum($tax):/count_tax
					}

					finalResultXquery = finalResultXquery + "{ " + expression + "}";

				} //fim else

				/*
				 * System.out.println("FinalResult.getFinalResult(), EXPRESSION:"
				 * +expression + ", ELEROUND:"+elementsAroundFunction);
				 * finalResultXquery = finalResultXquery + "\r\n\t\t" + "{ <" +
				 * elementsAroundFunction + ">" + "\r\n\t" + expression + "} </"
				 * + elementsAroundFunction + "> ";
				 */

			} // fim for

			finalResultXquery = finalResultXquery + "\r\n " + sbq.getElementAfterConstructor().replace("<", "</");
			finalResultXquery = finalResultXquery + "\r\n} </results>";

			//System.out.println("FinalResult.getFinalResult(): consulta final eh: \n" + finalResultXquery);

		} //fim do 1o. if la de cima
		//acionado no caso de possuir funcoes de agregacao com variaveis na clausula LET.

		// NÃO possui funcoes de agregacao com variaveis na clausula LET, mas tem ORDER BY
		else if (!q.getOrderBy().trim().equals("")) { // se a consulta original possui order by, acrescentar na consulta final o order by original.

			finalResultXquery = "<results> { \r\n"
					//+ " let $c := collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/" //Alterado por Luiz Matos, para compatibilidade c/ consultas TPC-H
					//+ " for $c in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/"
					+ " for $c in collection('"+tempCollectionName+"')/partialResult/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "")
					+ "/" + sbq.getElementAfterConstructor().replaceAll("[</>]", "") + "\r\n";

			//Luiz Matos - usado para inserir o LET das variaveis que constam no GROUP BY ou ORDER BY
			//Artifício adotado foi adicionar na query final o LET $varname := $var/element das $varname que constam no group by ou order by
			if (!q.getOrderBy().trim().equals("") || !q.getGroupBy().trim().equals("")) {//verifica se tem ORDER BY ou GROUP BY na consulta original
				String orderElements[] = q.getOrderBy().split("/"); //separa $var1/$var2/$varN
				String subGroup[] = q.getGroupBy().split("/"); //separa $var1/$var2/$varN

				//for (int i = 0; i < orderElements.length; i++) {
				for(String varName : orderElements)  {
					
					//String subOrder = ""; // caminho apos a definicao da variavel. Ex.: $order/shipdate. subOrder recebe shipdate.
					//int posSlash = orderElements[i].trim().indexOf("/");
					//int posSlash = varName.trim().indexOf("/");

					varName = varName.substring(varName.indexOf("$"), varName.length());
					//System.out.println(" ******* " + varName);
					//System.out.println(generateLetClauses(varName));
					finalResultXquery += " " + generateLetClauses(varName); //obtém clausula LET da variável do order by
				
					
					//if (posSlash != -1) {
						//subOrder = orderElements[i].trim().substring(posSlash + 1, orderElements[i].length());
					//orderElements = varName.trim().substring(varName.indexOf("$"), varName.length());
						
//						if (orderElements.charAt(orderElements.length() - 1) == '/') {
//							orderElements = orderElements.substring(0, orderElements.length() - 1);
//						}
					//}

					if (!orderElements.equals("")) {
						orderByClause = orderByClause + (orderByClause.equals("")?"": ", ") + variableName + "/key/" + orderElements;
					}
					
				} //fim for

				if(!q.getGroupBy().trim().equals("")) { //checa se tem group by 
					for(String varName : subGroup)  {
						if (!finalResultXquery.toUpperCase().contains("LET " + varName.toUpperCase()) && !varName.toUpperCase().contains("AVG(") && !varName.toUpperCase().contains("COUNT(")) //obtém clausula LET da variável do group by somente se a mesma não já tiver sido inserida por conta de um order by	
							finalResultXquery += " " + generateLetClauses(varName); //obtém clausula LET da variável do group by
					}//fim for
					finalResultXquery += " group by " + q.getGroupBy().replace("/", ", ") + "\r\n"; //insere primeiro o group by
				}
				if (!q.getOrderBy().trim().equals("")) {//verifica se tem ORDER BY na consulta original para inseri-lo depois do group by
					finalResultXquery += " order by " + q.getOrderBy().replace("/", ", ") + " " + q.getOrderByType() + "\r\n";
				}
			}//fim if
			finalResultXquery += " return \r\n" + " <"
					+ sbq.getElementAfterConstructor().replaceAll("[</>]", "")
					+ ">";

			//			String[] orderElements = q.getOrderBy().trim().split("\\$");
			//			for (int i = 0; i < orderElements.length; i++) {
			//				String subOrder = ""; // caminho apos a definicao da variavel. Ex.: $order/shipdate. subOrder recebe shipdate.
			//				int posSlash = orderElements[i].trim().indexOf("/");
			//
			//				if (posSlash != -1) {
			//					subOrder = orderElements[i].trim().substring(posSlash + 1, orderElements[i].length());
			//					if (subOrder.charAt(subOrder.length() - 1) == '/') {
			//						subOrder = subOrder.substring(0, subOrder.length() - 1);
			//					}
			//				}
			//
			//				if (!subOrder.equals("")) {
			//					orderByClause = orderByClause + (orderByClause.equals("")?"": ", ") + variableName + "/key/" + subOrder;
			//				}
			//			} //fim for
			//
			//			//finalResultXquery = " for $ret in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/"
			//			finalResultXquery = " for $ret in collection('"+tempCollectionName+"')/partialResult/"
			//					+ sbq.getConstructorElement().replaceAll("[</>]", "") + "/"
			//					+ "orderby"
			//					+ " order by " + orderByClause + " return $ret/element/" 
			//					+ sbq.getElementAfterConstructor().replaceAll("[</>]", "");

			//System.out.println("finalresult.java 2:"+ finalResultXquery);
		}// fim else if
		// acionado quando NAO possui funcoes de agregacao com variavel na clausula let, mas tem ORDER BY

		else { // se a consulta original nao possui order by, acrescentar na consulta final a ordenacao de acordo com a ordem dos elementos nos documentos pesquisados.
			orderByClause = "number($ret/idOrdem)";

			//finalResultXquery =  " for $ret in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult"
			finalResultXquery =  " for $ret in collection('"+tempCollectionName+"')/partialResult"
					+ " let $c := $ret/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "")
					+ "/element()" // where $ret/element()/name()!='idOrdem'"
					+ " order by " + orderByClause + " ascending"
					+ " return $c";

			//System.out.println("finalresult.java 1:"+ finalResultXquery);
		}
		//System.out.println(finalResultXquery);
		return finalResultXquery;
	}

	//Luiz Matos - retorna clausula LET que contem a variavel utilizada no order by ou group by
	private String generateLetClauses(String varName) {
		//System.out.println("varName OnlyTempCollectionStrategy = " + varName);

		//		if (varName.toUpperCase().contains("AVG(")) //checa se a funcao de agregacao do order by ou group by eh AVG, para montar a query final com SUM e COUNT
		//		{	
		//			//obtem trecho entre RETURN e o final da consulta
		//			String remainQuery = this.queryObj.getInputQuery().substring(this.queryObj.getInputQuery().toUpperCase().indexOf("RETURN "), this.queryObj.getInputQuery().length());
		//			System.out.println("remainQuery = " + remainQuery);
		//			
		//			varName = varName.substring(varName.indexOf("$"), varName.indexOf(")"));
		//			
		//			//posicao da variavel no trecho após o return
		//			int posVarSum = remainQuery.indexOf("sum("+varName+")");
		//			System.out.println("posVarSum = " + posVarSum);
		//			//obtem o trecho entre a variavel e o final da consulta
		//			String remainQuery2 = remainQuery.substring(posVarSum, remainQuery.length());
		//			//obtem o trecho entre o início do trecho anterior (variavel) ate o fechamento do seu elemento
		//			String remainQuery3 = remainQuery2.substring(0, remainQuery2.indexOf(">"));
		//			//obtem o elemento em torno do varName na consulta - Ex., de <l_return>{varName}</l_return> tera l_return
		//			String elementAroundSum = remainQuery3.substring(remainQuery3.indexOf("<")+1, remainQuery3.length());
		//			
		//			//posicao da variavel no trecho após o return
		//			int posVarCount = remainQuery.indexOf("count("+varName+")");
		//			System.out.println("posVarCount = " + posVarCount);
		//			//obtem o trecho entre a variavel e o final da consulta
		//			remainQuery2 = remainQuery.substring(posVarCount, remainQuery.length());
		//			//obtem o trecho entre o início do trecho anterior (variavel) ate o fechamento do seu elemento
		//			remainQuery3 = remainQuery2.substring(0, remainQuery2.indexOf(">"));
		//			//obtem o elemento em torno do varName na consulta - Ex., de <l_return>{varName}</l_return> tera l_return
		//			String elementAroundCount = remainQuery3.substring(remainQuery3.indexOf("<")+1, remainQuery3.length());
		//			
		//			
		//			System.out.println("retorno = " + "let " + varName + " := $c" + elementAroundSum + "\r\n" + "let " + varName + " := $c" + elementAroundCount + "\r\n");
		//			//return "let " + varName + " := $c" + elementAroundSum + "\r\n" + "let " + varName + " := $c" + elementAroundCount + "\r\n";
		//			return "";
		//		} else if (varName.toUpperCase().contains("COUNT(")) //checa se a funcao de agregacao do order by ou group by eh COUNT, para montar a query final com SUM
		//		{	
		//			//to do
		//			return "";
		//		
		//		} else { //se tiver SUM ou nenhuma funcao
		//obtem trecho entre RETURN e o final da consulta
		//		if (!varName.toUpperCase().contains("AVG(") && !varName.toUpperCase().contains("COUNT("))
		//		{
		String remainQuery = this.queryObj.getInputQuery().substring(this.queryObj.getInputQuery().toUpperCase().indexOf("RETURN "), this.queryObj.getInputQuery().length());
		//System.out.println("remainQuery = " + remainQuery);

		//posicao da variavel no trecho após o return
		int posVar = remainQuery.indexOf(varName);
		//System.out.println("posVar = " + posVar);
		//obtem o trecho entre a variavel e o final da consulta
		String remainQuery2 = remainQuery.substring(posVar, remainQuery.length());
		//obtem o trecho entre o início do trecho anterior (variavel) ate o fechamento do seu elemento
		String remainQuery3 = remainQuery2.substring(0, remainQuery2.indexOf(">"));
		//obtem o elemento em torno do varName na consulta - Ex., de <l_return>{varName}</l_return> tera l_return
		String elementAround = remainQuery3.substring(remainQuery3.indexOf("<")+1, remainQuery3.length());
		//		}
		if (varName.toUpperCase().contains("SUM("))
			varName = varName.substring(varName.indexOf("$"), varName.indexOf(")"));

		//System.out.println("retorno = " + "let " + varName + " := $c" + elementAround + "\r\n");
		return "let " + varName + " := $c" + elementAround + "\r\n";
		//	}



	}

	@Override
	public void loadPartial(InputStream partial) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean existsCollection(String collectionName) throws XQException {
		return db.existsCollection(collectionName);
	}

	@Override
	public void combinePartials() throws IOException {
		// TODO Auto-generated method stub

	}

}
