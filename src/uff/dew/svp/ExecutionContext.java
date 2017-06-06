package uff.dew.svp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SubQuery;

public class ExecutionContext {

	private String context;
	private Query queryObj;
	private SubQuery subQueryObj;

	public ExecutionContext(String fragment) throws IOException {
		StringReader strReader = new StringReader(fragment);
		populate(strReader);
	}

	public ExecutionContext() {
	}

	public void save(OutputStream out) throws IOException {
		out.write(context.getBytes());
	}

	public static ExecutionContext restoreFromStream(InputStream in) throws IOException {

		ExecutionContext ec = new ExecutionContext();
		ec.populate(new InputStreamReader(in));
		return ec;
	}

	private void populate(Reader in) throws IOException {
		String query = "";
		StringBuilder sb = new StringBuilder();

		queryObj = new Query();
		subQueryObj = new SubQuery();

		BufferedReader buff = new BufferedReader(in);

		String line;
		while((line = buff.readLine()) != null){    
			sb.append(line + "\n");
			if (!line.toUpperCase().contains("<GROUPBY>") &&!line.toUpperCase().contains("<ORDERBY>") && !line.toUpperCase().contains("<ORDERBYTYPE>") && !line.toUpperCase().contains("<ELMTCONSTRUCT>") && !line.toUpperCase().contains("<AGRFUNC>")) {
				query = query + " " + line;
			}
			else {
				// obter as clausulas do group by, orderby e de funcoes de agregacao
				if (line.toUpperCase().contains("<GROUPBY>")){
					String groupByClause = line.substring(line.indexOf("<GROUPBY>")+"<GROUPBY>".length(), line.indexOf("</GROUPBY>"));
					//System.out.println(groupByClause);
					queryObj.setGroupBy(groupByClause);
				}

				if (line.toUpperCase().contains("<ORDERBY>")){
					String orderByClause = line.substring(line.indexOf("<ORDERBY>")+"<ORDERBY>".length(), line.indexOf("</ORDERBY>"));
					//System.out.println(orderByClause);
					queryObj.setOrderBy(orderByClause);
				}

				if (line.toUpperCase().contains("<ORDERBYTYPE>")){
					String orderByType= line.substring(line.indexOf("<ORDERBYTYPE>")+"<ORDERBYTYPE>".length(), line.indexOf("</ORDERBYTYPE>"));                         
					queryObj.setOrderByType(orderByType);
				}
				if (line.toUpperCase().contains("<ELMTCONSTRUCT>")){
					String elmtConstruct = line.substring(line.indexOf("<ELMTCONSTRUCT>")+"<ELMTCONSTRUCT>".length(), line.indexOf("</ELMTCONSTRUCT>"));                         
					if (!elmtConstruct.equals("") && !elmtConstruct.equals("{}")) {
						if (elmtConstruct.charAt(0) == '{') { // need to remove that
							elmtConstruct = elmtConstruct.substring(1, elmtConstruct.length()-1);
						}
						String[] constr = elmtConstruct.split(","); // separa todos os elements constructors utilizados no return statement.

						if (constr!=null) {

							for (String keyMap: constr) {

								String[] hashParts = keyMap.split("=");

								if (hashParts!=null) {
									queryObj.setElmtConstructors(hashParts[0], hashParts[1]); // o par CHAVE, VALOR
								}
							}

						}
					}                       
				}
				if (line.toUpperCase().contains("<AGRFUNC>")){ // soma 1 para excluir a tralha contida apos a tag <AGRFUNC>

					String aggregateFunctions = line.substring(line.indexOf("<AGRFUNC>")+"<AGRFUNC>".length(), line.indexOf("</AGRFUNC>"));

					if (!aggregateFunctions.equals("") && !aggregateFunctions.equals("{}")) {
						if (aggregateFunctions.charAt(0) == '{') { // need to remove that
							aggregateFunctions = aggregateFunctions.substring(1, aggregateFunctions.length()-1);
						}
						String[] functions = aggregateFunctions.split(","); // separa todas as funcoes de agregacaoo utilizadas no return statement.

						if (functions!=null) {

							for (String keyMap: functions) {

								String[] hashParts = keyMap.split("=");

								if (hashParts!=null) {

									queryObj.setAggregateFunc(hashParts[0], hashParts[1]); // o par CHAVE, VALOR

								}
							}

						}
					}                       
				}                       
			}
		}
		buff.close();

		subQueryObj.setConstructorElement(SubQuery.getConstructorElement(query));
		subQueryObj.setElementAfterConstructor(SubQuery.getElementAfterConstructor(query));
		queryObj.setInputQuery(query);

		context = sb.toString();
	}

	public Query getQueryObj() {
		return queryObj;
	}

	public void setQueryObj(Query query) {
		this.queryObj = query;
	}

	public SubQuery getSubQueryObj() {
		return subQueryObj;
	}

	public void setSubQueryObj(SubQuery sbq) {
		this.subQueryObj = sbq;
	}
}
