package uff.dew.avp.localqueryprocessor.queryexecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.connection.DBConnectionPoolEngine;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.svp.FinalResultComposer;
import uff.dew.svp.SubQueryExecutionException;
import uff.dew.svp.db.DatabaseException;

abstract public class QueryExecutor implements Runnable
{
	private Logger logger = Logger.getLogger(QueryExecutor.class);
	// Valid States
	public static final int ST_STARTING_RANGE = 0;
	public static final int ST_PROCESSING_RANGE = 1;
	public static final int ST_RANGE_PROCESSED = 2;
	public static final int ST_WAITING_NEW_RANGE = 3;
	public static final int ST_FINISHING = 4;
	public static final int ST_FINISHED = 5;
	public static final int ST_ERROR = 5;

	// Protected attributes
	protected int state;
	protected Range range;
	protected LocalQueryTaskStatistics lqtStatistics;
	protected boolean onlyCollectionStrategy;
	private FinalResultComposer composer;
	private String tempCollectionName;
	
	// Private attributes
	private LocalQueryTask lqt;
	private DBConnectionPoolEngine dbpool;
	private String query;
	private Exception exception = null;
	private String completeFileName;
	private String resultsTimeFileName;
	private long timeProcessingSubqueries = 0;
	private long timeExecSubquery = 0;
	private long timeBuildingPartialResult = 0;
	private long timeInsertingTempCollection = 0;
	private int idQuery;
	
	public QueryExecutor(LocalQueryTask lqt, DBConnectionPoolEngine dbpool,	String query, LocalQueryTaskStatistics lqtStatistics, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) {
		this.lqt = lqt;
		this.dbpool = dbpool;
		this.query = query;
		this.lqtStatistics = lqtStatistics;
		this.onlyCollectionStrategy = onlyCollectionStrategy;
		this.state = ST_STARTING_RANGE;
		setTempCollectionName(tempCollectionName);
		this.idQuery = idQuery;
	}

	public QueryExecutor(LocalQueryTask lqt, DBConnectionPoolEngine dbpool,	String query, Range range, LocalQueryTaskStatistics lqtStatistics, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) {
		this.lqt = lqt;
		this.dbpool = dbpool;
		this.query = query;
		this.range = range;
		this.lqtStatistics = lqtStatistics;
		this.onlyCollectionStrategy = onlyCollectionStrategy;
		this.state = ST_STARTING_RANGE;
		setTempCollectionName(tempCollectionName);
		this.idQuery = idQuery;
	}

	/* Abstract method: getQueryLimits( int []limits )
	 *  Determines next query limits.
	 *  If the interval to be processed is not finished, the method returns "true" and
	 *      "limits" vector stores new limits.
	 *  If the interval to be processed is finished, the method returns "false" and
	 *      "limits" vector contents are undetermined.
	 * "Limits" vector is allocated by the caller. Method must only modify its values.
	 */
	protected abstract boolean getQueryLimits(String query, int[] limits);

	/* Abstract method: executeSubQuery();
	 * Each subclass can present different behavior while executing sub-query.
	 * When AVP is employed, for example, query execution time is always needed.
	 * Parameter dbconn must already be set and prepared statement must be with the appropriate arguments set
	 */

	protected abstract void executeSubQuery(String query, int[] limit)
			throws RemoteException, SubQueryExecutionException, DatabaseException;

	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			DBConnectionPoolEngine dbconn;

			int[] limit = new int[2];
			String xquery = "";
			FileWriter writer;
			PrintWriter saida = null;
			File file;

			//Usado para gravar os resultados do experimento 
			resultsTimeFileName = Utilities.createTimeResultFileNameQE( lqt.getId(), lqt.getQueryExecutionStrategy()==1?"AVP":"SVP", this.idQuery);
			file = new File(AVPConst.CQP_CONF_DIR_NAME+resultsTimeFileName);
			writer = new FileWriter(file, true);
			saida = new PrintWriter(writer);
			saida.println("InitialInterval;EndInterval;Time-Exec-Subquery;Time-Building-partialResult;Time-Inserting-tempCollection;Time-Processing-Subquery-QE");

			//dbconn = dbpool.reserveConnection(); // Request a new Database Connection

			xquery = query;
			//System.out.println("Query chegando do LQT no QE: " + xquery);

			//Troca os valores dos intervalos iniciais por ? para depois serem substituídos pelos subintervalos do AVP
			if (this instanceof QueryExecutorAvp) {
				xquery = xquery.replaceFirst(String.valueOf(range.getFirstValue()),"\\?");
				xquery = xquery.replaceFirst(String.valueOf(range.getOriginalLastValue()),"\\?");
			}

			do {
				// Wait for new query interval arrival. Used for dynamic load balancing.
				waitForNewRange(); //Volta essa instrução quando for usar load balance 

				// new interval arrived or a request to finish operations was received
				if (state != ST_FINISHING) {

					if (this instanceof QueryExecutorAvp && range == null) {
						throw new IllegalThreadStateException("LocalQueryTaskEngine exception: no interval to process and no request to finish received!");
					}

					while (state != ST_RANGE_PROCESSED) {						
						//int countArgs;

						//System.out.println("state - " + state);
						// Each algorithm can determine new limits in a different way
					   getQueryLimits(query, limit);

						if (state != ST_RANGE_PROCESSED) {
							logger.debug(Messages.getString("queryExecutor.vp",new Object[]{limit[0],limit[1] ,(limit[1] - limit[0])}));

							//Chamada métodos diferentes, dependendo da estratégia, pois no caso do SVP as subconsultas já possuem o intervalo (definido pela algoritmo da Carla). No caso do AVP no lugar dos intervalos temos ? que são substituídos pelos subintervalos definidos pelo algoritmo do Alexandre
							//if (this instanceof QueryExecutorSvp) {
								//partialResult = dbconn.executeXQuery(query, limit, this.numberOfExecutions);
//								executeSubQuery(xquery, limit); //método implementado na classe QueryExecutorSvp.java
//
//							}
//							else if (this instanceof QueryExecutorAvp) {
								executeSubQuery(xquery, limit); //método implementado na classe QueryExecutorAvp.java
							//}

							lqt.setCompletePartialResultFileNames(this.getCompleteFileName());

							if (file.exists()) {
								//escreve no arquivo de tempos do LQT
								//System.out.println("QE: " + limit[0]+";"+limit[1]+";"+this.getTimeProcessingSubqueries()+";"+Utilities.getFileLength(this.getCompleteFileName()));
								saida.println(limit[0] + ";" + limit[1] + ";" + this.getTimeExecSubquery() + ";" + this.getTimeBuildingPartialResult() + ";" + this.getTimeInsertingTempCollection() + ";" + this.getTimeProcessingSubqueries());
								
							}
						} //fim if

					} //fim while

//					saida.close();
//					writer.close();
					
					synchronized (this) {
						// reset query interval
						range = null;
						state = ST_WAITING_NEW_RANGE; //Volta essa instrução quando for usar load balance e retira a de baixo
						//state = ST_FINISHING;

						// send message to Local Query Task
						lqt.rangeProcessed();
						notifyAll();
					}

				} // if( a_state != ST_FINISHING )
			} while (state != ST_FINISHING);

			//dbconn.closePreparedStetement();
			//dbpool.disposeConnection(dbconn);

			state = ST_FINISHED;
			
			saida.close();
			writer.close();

		} catch (Exception e) {
			System.err.println("Local Query Task Exception: " + e.getMessage());
			e.printStackTrace();
			exception = e;
			state = ST_ERROR;			
		}
	}

	// to be called by LocalQueryTask when assigning a new interval due to dynamic load balancing
	public synchronized void newRange(Range newRange) {
		// Sets a new interval to be processed. Used for dynamic load balancing.
		if (state != ST_WAITING_NEW_RANGE) {
			throw new IllegalThreadStateException("QueryExecutor Exception: attempt to change query interval when state was " + state + " !");
		} else {
			range = newRange;
			state = ST_STARTING_RANGE;
			notifyAll();
		}
	}

	// to be called by LocalQueryTask to indicate LocalQueryTask must finish
	public synchronized void finish() {
		state = ST_FINISHING;
		notifyAll();
	}

	private synchronized void waitForNewRange() throws InterruptedException {
		while (state == ST_WAITING_NEW_RANGE) {
			wait();
		}
		notifyAll();
	}

	public Exception getException() {
		return exception;
	}

	public String getCompleteFileName() {
		return completeFileName;
	}

	public void setCompleteFileName(String completeFileName) {
		this.completeFileName = completeFileName;

	}

	public long getTimeProcessingSubqueries() {
		return timeProcessingSubqueries;
	}

	public void setTimeProcessingSubqueries(long timeProcessingSubqueries) {
		this.timeProcessingSubqueries = timeProcessingSubqueries;
	}
	
	public long getTimeExecSubquery() {
		return timeExecSubquery;
	}
	
	public void setTimeExecSubquery(long timeExecSubquery) {
		this.timeExecSubquery = timeExecSubquery;
	}
	
	public long getTimeBuildingPartialResult() {
		return timeBuildingPartialResult;
	}
	
	public void setTimeBuildingPartialResult(long timeBuildingPartialResult) {
		this.timeBuildingPartialResult = timeBuildingPartialResult;
	}
	
	public long getTimeInsertingTempCollection() {
		return timeInsertingTempCollection;
	}
	
	public void setTimeInsertingTempCollection(long timeInsertingTempCollection) {
		this.timeInsertingTempCollection = timeInsertingTempCollection;
	}
	
	public void setTempCollectionName(String tempCollectionName) {
		this.tempCollectionName = tempCollectionName;
	}
	
	public String getTempCollectionName() {
		return tempCollectionName;
	}
}
