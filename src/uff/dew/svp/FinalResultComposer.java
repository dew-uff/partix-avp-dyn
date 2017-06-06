package uff.dew.svp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.xml.xquery.XQException;

import uff.dew.svp.db.Database;
import uff.dew.svp.db.DatabaseException;
import uff.dew.svp.db.DatabaseFactory;
import uff.dew.svp.fragmentacaoVirtualSimples.Query;
import uff.dew.svp.fragmentacaoVirtualSimples.SubQuery;
import uff.dew.svp.strategy.CompositionStrategy;
import uff.dew.svp.strategy.ConcatenationStrategy;
import uff.dew.svp.strategy.OnlyTempCollectionStrategy;
import uff.dew.svp.strategy.TempCollectionStrategy;

public class FinalResultComposer {

	private OutputStream output;
	private Database database;

	private CompositionStrategy compositionStrategy;

	private boolean forceTempCollectionMode = false;
	private boolean forceOnlyTempCollectionMode = false;//Luiz Matos
	private String tempCollectionName; //Luiz Matos, para receber o nome da colecao temporaria

	public FinalResultComposer(OutputStream output) {
		this.output = output;
	}

	/**
	 * Sets the local database information where the subquery will be executed
	 * 
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param databaseName
	 * @param type
	 * @throws DatabaseException
	 */
	public void setDatabaseInfo(String hostname, int port, String username, String password,
			String databaseName, String type) throws DatabaseException {
		database = DatabaseFactory.getDatabase(hostname, port, username, password, databaseName, type);
		//        Catalog.get().setDatabaseObject(DatabaseFactory.getSingletonDatabaseObject());
	}

	/**
	 * Set execution context
	 * 
	 * @param context
	 */
	public void setExecutionContext(ExecutionContext context) {
		Query q = context.getQueryObj();
		SubQuery sbq = context.getSubQueryObj();
		String orderby = q.getOrderBy();
		String groupby = q.getGroupBy();

		Hashtable<String,String> aggrFunctions = q.getAggregateFunctions();
		//System.out.println("q.getInputQuery() = " + q.getInputQuery());
		
		if (isForceTempCollectionExecutionMode()) {
			compositionStrategy = new TempCollectionStrategy(database, q, sbq, output);
		}
		else if (isForceOnlyTempCollectionExecutionMode()) { 
			compositionStrategy = new OnlyTempCollectionStrategy(database, q, sbq, output, tempCollectionName);
		}
		else if (orderby != null && !orderby.equals("")) {
			compositionStrategy = new TempCollectionStrategy(database, q, sbq, output);
		} 
		else if (aggrFunctions != null && aggrFunctions.size() > 0) {
			compositionStrategy = new TempCollectionStrategy(database, q, sbq, output);
		}
		else {
			compositionStrategy = new ConcatenationStrategy(output, sbq);
		}
	}

	/**
	 * 
	 * @param is
	 */
	public void loadPartial(InputStream is) throws IOException {
		compositionStrategy.loadPartial(is);
	}

	//Luiz Matos
	public void loadPartial(String tempCollectionName) throws IOException {
		compositionStrategy.loadPartial(tempCollectionName);
	}

	public String combinePartialResults() throws IOException {
		//System.out.println("getTempCollectionName() = " + getTempCollectionName());
		return compositionStrategy.combinePartials2(getTempCollectionName());
	}

	/**
	 * used in tests to clean resources
	 */
	public void cleanup() {
		compositionStrategy.cleanup();
	}

	/**
	 * used to check if temp collection exists
	 * @author lzomatos
	 */
	public boolean existsCollection(String collectionName) throws XQException {
		return compositionStrategy.existsCollection(collectionName);
	}
	
	public void setForceTempCollectionExecutionMode(boolean flag) {
		this.forceTempCollectionMode = flag;
	}
	
	public boolean isForceTempCollectionExecutionMode() {
		return forceTempCollectionMode;
	}

	public void setForceOnlyTempCollectionExecutionMode(boolean flag) {
		this.forceOnlyTempCollectionMode = flag;
	}
	
	public boolean isForceOnlyTempCollectionExecutionMode() {
		return forceOnlyTempCollectionMode;
	}
	
	public void setTempCollectionName(String tempCollectionName) {
		this.tempCollectionName = tempCollectionName;
	}
	
	public String getTempCollectionName() {
		return tempCollectionName;
	}
}
