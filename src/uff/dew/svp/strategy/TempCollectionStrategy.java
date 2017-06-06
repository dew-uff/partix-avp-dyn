package uff.dew.svp.strategy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQResultSequence;

import uff.dew.avp.AVPConst;
import uff.dew.svp.db.Database;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SubQuery;

public class TempCollectionStrategy implements CompositionStrategy {

	private Database db;

	private boolean started = false;
	private OutputStream output = null;
	private File tempDir;
	private int count = 0;
	private SubQuery subQueryObj;
	private Query queryObj;

	public TempCollectionStrategy(Database db, Query q, SubQuery sbq, OutputStream output) {
		this.db = db;
		this.output = output;
		this.subQueryObj = sbq;
		this.queryObj = q;
	}

	@Override
	public void loadPartial(InputStream partial) throws IOException {

		if (!started) {
			started = true;
			count = 0;
			tempDir = createTempDirectory();
		}
		//System.out.println(partial.toString());
		copyToLocalFs(partial,tempDir);
	}

	@Override
	public void combinePartials() throws IOException {

		try {
			// if not explicitly flagged to delete, it will raise an exception if 
			// the collection exists
			if (tempDir.list().length > 0) {
				db.createCollectionWithContent(AVPConst.TEMP_COLLECTION_NAME,  tempDir.getAbsolutePath());
			}
			deleteTempDirectory(tempDir);
		} catch (XQException e) {
			throw new IOException("Temporary Collection already exists", e);
		}

		SubQuery sbq = subQueryObj;

		// construct the query to get the result from the temp collection
		String finalQuery = constructFinalQuery();

		String header = sbq.getConstructorElement() + "\r\n";
		output.write(header.getBytes());

		try {
			XQResultSequence rs = db.executeQuery(finalQuery);

			while (rs.next()) {
				String item = rs.getItemAsString(null);
				output.write(item.getBytes());
				output.write("\r\n".getBytes());
				output.flush();
			}
			db.freeResources(rs);
		}
		catch (XQException e) {
			e.printStackTrace();
			throw new IOException(e);
		}

		String footer = sbq.getConstructorElement().replace("<", "</");
		output.write(footer.getBytes());

	}

	@Override
	public void cleanup() {
		try {
			db.deleteCollection(AVPConst.TEMP_COLLECTION_NAME);
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

		Query q = queryObj;
		SubQuery sbq = subQueryObj;


		String finalResultXquery = "";
		String orderByClause = "";
		String variableName = "$ret";

		//System.out.println("q.getInputQuery() = " + q.getInputQuery());
		// possui funcoes de agregacao na clausula LET.
		if (q.getAggregateFunctions() != null  && q.getAggregateFunctions().size() > 0) { 
			// finalResultXquery =
			//" \r\n"
			finalResultXquery = " <results> { \r\n"
					//+ " let $c := collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/" //Alterado por Luiz Matos, para compatibilidade c/ consultas TPC-H
					+ " for $c in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "")
					//+  "\r\n"
					+ "/record\r\n"
					// + " where $c/element()/name()!='idOrdem'"

                     //Luiz Matos - usado para permitir inserir o LET das variaveis que constam no GROUP BY
                     //Artifício adotado foi adicionar na query final o LET $varname := $var/Element das $varname que constam no group by
//                     if (q.getInputQuery().toUpperCase().contains("GROUP BY") && !q.getOrderBy().trim().equals("")) {//testa se tem GROUP BY e ORDER BY
//                    	 
//                    	 String auxQ = q.getInputQuery().substring(q.getInputQuery().toUpperCase().indexOf("GROUP BY")+8, q.getInputQuery().toUpperCase().indexOf("ORDER BY"));
//                    	 
//                    	 int posComma = auxQ.indexOf(",");
                    	 
//                    	 while (posComma != -1) {
//                    		 String auxVar = auxQ.substring(0, posComma);
//                    		 if (q.getInputQuery().toUpperCase().contains("LET " + auxVar))
//                    			 System.out.println(q.getInputQuery().substring(q.getInputQuery().indexOf("LET " + auxVar), q.getInputQuery().indexOf(ch))));
//                    				 q.getOrderBy().substring(0, posAux) + q.getOrderBy().substring(posAux+3, q.getOrderBy().length());
//                    		 q.setOrderBy(orderBy);
//                    		 posAux = q.getOrderBy().indexOf("/VP");
//                    	 }
//                     }

			+ "  return \r\n\t" + "<"
			+ sbq.getElementAfterConstructor().replaceAll("[</>]", "")
			//+ "<record>".replaceAll("[</>]", "")
			+ ">";

			//System.out.println("LETS - " + q.getLetClauses());
			//System.out.println("sera? = " + q.getGroupBy());
			
			Set<String> keys = q.getAggregateFunctions().keySet();
			for (String function : keys) {
				String expression = q.getAggregateFunctions().get(function);
				String elementsAroundFunction = "";
				//System.out.println("expression = " + expression);
				if (expression.indexOf(":") != -1) {
					elementsAroundFunction = expression.substring(expression.indexOf(":") + 1, expression.length());
					expression = expression.substring(0, expression.indexOf(":"));
				}
				//System.out.println("elementsAroundFunction = " + elementsAroundFunction);
				//System.out.println("expression = " + expression);
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
							finalResultXquery = finalResultXquery + "\r\n\t\t" + " <" + openElement + ">";
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
					finalResultXquery = finalResultXquery + "{ " + expression + "}";

					for (int i = elm.length - 1; i >= 0; i--) {
						String closeElement = elm[i];

						if (!closeElement.equals("") && !closeElement.equals(sbq.getElementAfterConstructor().replaceAll("[</>]", ""))) {
							//System.out.println("FinalResult.getFinalResult(); armazenar o el:::"+closeElement);
							finalResultXquery = finalResultXquery + "\r\n\t\t"  + " </" + closeElement + ">";
						}
					}

				}
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

			finalResultXquery = finalResultXquery + "\r\n\t" + sbq.getElementAfterConstructor().replace("<", "</");
			//finalResultXquery = finalResultXquery + "\r\n\t" + "</record>";
			finalResultXquery = finalResultXquery + "} </results>";

			System.out.println("FinalResult.getFinalResult(): consulta final eh: \n" + finalResultXquery);

		} //fim do 1o. if la de cima

		else if (!q.getOrderBy().trim().equals("")) { // se a consulta original possui order by, acrescentar na consulta final o order by original.

			String[] orderElements = q.getOrderBy().trim().split("\\$");
			for (int i = 0; i < orderElements.length; i++) {
				String subOrder = ""; // caminho apos a definicao da variavel. Ex.: $order/shipdate. subOrder recebe shipdate.
				int posSlash = orderElements[i].trim().indexOf("/");

				if (posSlash != -1) {
					subOrder = orderElements[i].trim().substring(posSlash + 1, orderElements[i].length());
					if (subOrder.charAt(subOrder.length() - 1) == '/') {
						subOrder = subOrder.substring(0, subOrder.length() - 1);
					}
				}

				if (!subOrder.equals("")) {
					orderByClause = orderByClause + (orderByClause.equals("")?"": ", ") + variableName + "/key/" + subOrder;
				}
			} //fim for

			finalResultXquery = " for $ret in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "") + "/"
					+ "orderby"
					+ " order by " + orderByClause + " return $ret/element/" 
					+ sbq.getElementAfterConstructor().replaceAll("[</>]", "");

			System.out.println("finalresult.java 2:"+ finalResultXquery);
		} else { // se a consulta original nao possui order by, acrescentar na consulta final a ordenacao de acordo com a ordem dos elementos nos documentos pesquisados.
			orderByClause = "number($ret/idOrdem)";

			finalResultXquery =  " for $ret in collection('"+AVPConst.TEMP_COLLECTION_NAME+"')/partialResult"
					+ " let $c := $ret/"
					+ sbq.getConstructorElement().replaceAll("[</>]", "")
					+ "/element()" // where $ret/element()/name()!='idOrdem'"
					+ " order by " + orderByClause + " ascending"
					+ " return $c";

			System.out.println("finalresult.java 1:"+ finalResultXquery);
		}

		return finalResultXquery;
	}

	private void copyToLocalFs(InputStream partial, File dir) throws IOException {
		String tempFile = dir.getAbsolutePath() + "/partial_" + count++ + ".xml";
		FileOutputStream fos = new FileOutputStream(tempFile);
		final int BUFFER_SIZE = 2048;
		BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
		byte[] buffer = new byte[BUFFER_SIZE];
		int bufCount = -1;
		while ((bufCount = partial.read(buffer,0,BUFFER_SIZE)) != -1) {
			bos.write(buffer,0,bufCount);
		}
		bos.flush();
		bos.close();
	}

	private static File createTempDirectory() throws IOException {
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()), new File(AVPConst.FINAL_RESULT_DIR));

		System.out.println("temp.getAbsolutePath() = " + temp.getAbsolutePath());

		if( !(temp.delete()) )
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if( !(temp.mkdir()) )
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	private static void deleteTempDirectory(File tempDir) throws IOException {

		File[] innerFiles = tempDir.listFiles();
		for(File f : innerFiles) {
			f.delete();
		}

		tempDir.delete();
	}

	@Override
	public void loadPartial(String collectionName)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean existsCollection(String collectionName) throws XQException {
		return db.existsCollection(collectionName);
	}

	@Override
	public String combinePartials2(String tempCollection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
