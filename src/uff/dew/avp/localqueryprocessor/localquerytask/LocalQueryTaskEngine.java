package uff.dew.avp.localqueryprocessor.localquerytask;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.commons.IntervalTree;
import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.SystemResourcesMonitor;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.connection.DBConnectionPoolEngine;
import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTask;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.RangeStatistics;
import uff.dew.avp.localqueryprocessor.queryexecutor.QueryExecutor;
import uff.dew.avp.localqueryprocessor.queryexecutor.QueryExecutorAvp;
import uff.dew.avp.localqueryprocessor.queryexecutor.QueryExecutorSvp;
import uff.dew.svp.FinalResultComposer;

public class LocalQueryTaskEngine extends UnicastRemoteObject implements LocalQueryTask, Runnable {

	private static final long serialVersionUID = 3257288036978274359L;
	private Logger logger = Logger.getLogger(LocalQueryTaskEngine.class);
	// Neighbors constants
	static private final int NUMBER_OF_NEIGHBORS = 4;
	static private final int NEIGHBOR_ABSENT = -1;
	// It is important to keep the order
	static private final int NEIGHBOR_NORTH = 0;
	static private final int NEIGHBOR_EAST = 1;
	static private final int NEIGHBOR_SOUTH = 2;
	static private final int NEIGHBOR_WEST = 3;
	// States
	static private final int ST_INITIAL = 0;
	static private final int ST_PROCESSING_QUERY = 1;
	static private final int ST_IDLE = 2;
	static private final int ST_FINISHING = 3;
	static private final int ST_FINISHED = 4;
	static private final int ST_ERROR = 5;
	private int id;
	private int state;
	private Range range;
	private GlobalQueryTask globalTask;
	private DBConnectionPoolEngine dbpool;
	private String query;
	// indicates if statistics must be get
	private boolean getStatistics;
	// private boolean finishRequested;
	private int queryExecutionStrategy;
	// used to store what needs to be processed, concerning the first interval
	// assigned to this LQT by GQT
	private IntervalTree intervalNotProcessed;
	private LQT_Message_Queue messageQueue;
	private QueryExecutor qe;
	private LocalQueryTaskStatistics lqtstatistics;
	private RangeStatistics statistics;
	// to avoid deadlock
	// private boolean a_blocked; // NOT USED - check
	// For dynamic load balance
	private static final float HELP_WORKLOAD_FRACTION = (float) 0.5;
	// number of LQTs in the grid
	private int numlqts;
	// references to all lqts
	private LocalQueryTask[] lqt;
	// identifiers from all neighbors
	private int[] idNeighbor;
	// to indicate if dynamic load balancing
	private boolean performDynLoadBal;
	private boolean onlyCollectionStrategy; 
	// must be performed
	private int numNodesPerLine;
	private int numGridLines;
	// line occupied by the node if it was in a grid
	private int line;
	// column occupied by the node if it was in a grid
	private int column;
	// indicates if this lqt is responsible to transmit messages came from East
	// to the last line
	private boolean respForEastMsgLastLine;
	// indicates how many help refuses must arriving before repeating help
	// offering private int a_idRangeOwner
	private int numRefusesForHelpReoffer;
	// id of the LQT responsible for the range
	// being processed
	// id of the last help offer made
	private int numLastHelpOffer;
	// number of refuses to the last help offer received
	private int numRefusesReceived;
	// to get system resources
	private SystemResourcesMonitor localMonitor;

	private FinalResultComposer composer;
	private String tempCollectionName;

	private static ArrayList<String> completePartialResultFileNames = new ArrayList<String>();
	private static long timeComposePartialResultFiles = 0;
	private int idQuery;

	private FileOutputStream myFile;
	private PrintStream myStream;
	
	public LocalQueryTaskEngine(int id, GlobalQueryTask globalTask, DBConnectionPoolEngine dbpool, String query, int queryExecutionStrategy,
			int numlqts, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, boolean getStatistics, Range range, RangeStatistics statistics, String tempCollectionName, int idQuery) throws RemoteException {
		this.id = id;
		state = ST_INITIAL;
		this.globalTask = globalTask;
		this.dbpool = dbpool;
		this.query = query;
		this.queryExecutionStrategy = queryExecutionStrategy;
		intervalNotProcessed = null;
		this.numlqts = numlqts;
		performDynLoadBal = performDynamicLoadBalancing;
		this.onlyCollectionStrategy = onlyCollectionStrategy;
		this.tempCollectionName = tempCollectionName;
		this.idQuery = idQuery;
		this.range = range;
		this.getStatistics = getStatistics;
		this.statistics = statistics;

		messageQueue = new LQT_Message_Queue();
		lqt = null;
		idNeighbor = null;
		numLastHelpOffer = 0;
		qe = null;
		// a_blocked = false;
		// initialize Local Query Tasks array
		lqt = new LocalQueryTask[this.numlqts];
		for (int i = 0; i < this.numlqts; i++)
			lqt[i] = null;
		lqtstatistics = null;

		//System.out.println("LocalQueryTaskEngine constructor ...");
	}

	/** Creates a new instance of Job */
	public LocalQueryTaskEngine(int id, GlobalQueryTask globalTask,	DBConnectionPoolEngine dbpool, String query, Range range,
			int queryExecutionStrategy, int numlqts, boolean localResultComposition, boolean performDynamicLoadBalancing,
			boolean onlyCollectionStrategy, boolean getStatistics, SystemResourcesMonitor localMonitor) throws RemoteException {
		this.id = id;
		state = ST_INITIAL;
		this.globalTask = globalTask;
		this.dbpool = dbpool;
		this.query = query;
		this.range = range;
		this.getStatistics = getStatistics;
		this.queryExecutionStrategy = queryExecutionStrategy;
		intervalNotProcessed = null;
		this.numlqts = numlqts;
		performDynLoadBal = performDynamicLoadBalancing;
		this.onlyCollectionStrategy = onlyCollectionStrategy;

		messageQueue = new LQT_Message_Queue();
		lqt = null;
		idNeighbor = null;
		numLastHelpOffer = 0;
		qe = null;
		// a_blocked = false;
		// initialize Local Query Tasks array
		lqt = new LocalQueryTask[this.numlqts];
		for (int i = 0; i < this.numlqts; i++)
			lqt[i] = null;
		statistics = null;
		this.localMonitor = localMonitor;
		//System.out.println("LocalQueryTaskEngine constructor ...");
	}

	public void start() throws RemoteException {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			// Calculate logical grid
			if (performDynLoadBal)
				enterGrid();

			// Create interval tree
			this.intervalNotProcessed = new IntervalTree(range.getFirstValue(), range.getOriginalLastValue());

			if(getStatistics)
			{
				lqtstatistics = new LocalQueryTaskStatistics(performDynLoadBal);
				lqtstatistics.setIntervalLimits(range.getFirstValue(), range.getOriginalLastValue());
				lqtstatistics.begin();
			} else
			{
				lqtstatistics = null;
			}

			// Set result composer
			//implementar algo que garanta o paralelismo entre processamento e composição

			// Create and start QueryExecutor
			switch (this.queryExecutionStrategy) {
			case GlobalQueryTask.QE_STRATEGY_SVP: {
				qe = new QueryExecutorSvp(this, dbpool, query, lqtstatistics, onlyCollectionStrategy, tempCollectionName, idQuery);
				break;
			}
			case GlobalQueryTask.QE_STRATEGY_AVP: {
				qe = new QueryExecutorAvp(this, dbpool, query, range, statistics, lqtstatistics, onlyCollectionStrategy, tempCollectionName, idQuery);
				break;
			}

			default:
				throw new IllegalArgumentException("LocalQueryTaskException (run): Invalid query execution strategy (" + queryExecutionStrategy + ")!");
			}

			// change state
			state = ST_PROCESSING_QUERY;

			// chama método run() da classe QueryExecutor.java
			qe.start();

			do {
				LQT_Message msg;

				msg = messageQueue.getMessage();
				//System.out.println("LQT msg.getType: " + msg.getType());

				switch (msg.getType()) {
				case LQT_Message.MSG_FINISH: {
					treatFinishMessage();
					break;
				}
				case LQT_Message.MSG_HELPOFFER: {
					treatHelpOffer((LQT_Msg_HelpOffer) msg);
					break;
				}
				case LQT_Message.MSG_HELPACCEPTED: {
					treatHelpAcceptedMessage((LQT_Msg_HelpAccepted) msg);
					break;
				}
				case LQT_Message.MSG_HELPNOTACCEPTED: {
					treatHelpNotAcceptedMessage((LQT_Msg_HelpNotAccepted) msg);
					break;
				}
				case LQT_Message.MSG_INTERVALFINISHED: {
					treatIntervalFinishedMessage((LQT_Msg_IntervalFinished) msg);
					break;
				}
				default:
					throw new IllegalArgumentException("LocalQueryTaskEngine.run(): invalid message type: "	+ msg.getType());
				}
			} while (state != ST_FINISHING);

			if (lqtstatistics != null)
				lqtstatistics.endQueryProcessing();

			// Finish QueryExecutor
			qe.finish();

			// wait local result composer to finish its work
			// this is separated from the "finish()" call to take advantage of
			// multitasking
			//			if (localResultComposition) {
			//				synchronized (resultComposer) {
			//					while (!resultComposer.finished())
			//						resultComposer.wait();
			//				}
			//			}

			if (lqtstatistics != null)
				lqtstatistics.end();
			
			state = ST_FINISHED;
			//System.out.println("LQT do QE: " + qe.getCompleteFileName());
			this.setCompletePartialResultFileNames(qe.getCompleteFileName());

			// Notify Task
			globalTask.localQueryTaskFinished(id, lqtstatistics, qe.getException());
			
			try {
				myFile= new FileOutputStream(AVPConst.PARTIAL_RESULTS_FILE_PATH+Utilities.createLBalanceStatisticsFileName(this.id, (this.queryExecutionStrategy==1)?"AVP":"SVP", this.idQuery));
				
				myStream = new PrintStream(myFile);
				lqtstatistics.print(myStream);
				
				
				myStream.flush();
				myStream = null;
				myFile.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			logger.error(Messages.getString("localQueryTaskEngine.exception", e.getMessage()));
			e.printStackTrace();
			try {
				this.globalTask.localQueryTaskFinished(id, lqtstatistics, null, e);
				state = ST_ERROR;
			} catch (RemoteException re) {
				e.printStackTrace();
			}
		}
	}

	public int getId() throws RemoteException {
		return id;
	}

	public void finish() throws RemoteException {
		if ((state != ST_FINISHING) && (state != ST_FINISHED)) {
			LQT_Msg_Finish msg = new LQT_Msg_Finish();
			messageQueue.addMessage(msg);
		}
	}

	public void intervalFinished(int idLQT, int intervalBeginning, int intervalEnd) throws RemoteException {
		if ((state != ST_FINISHING) && (state != ST_FINISHED)) {
			LQT_Msg_IntervalFinished msg = new LQT_Msg_IntervalFinished(idLQT, intervalBeginning, intervalEnd);
			this.messageQueue.addMessage(msg);
		}
	}

	public void offerHelp(int idSender, int idHelper, int senderPosition, int offerNumber) throws RemoteException {
		if ((state != ST_FINISHING) && (state != ST_FINISHED)) {
			LQT_Msg_HelpOffer msg = new LQT_Msg_HelpOffer(idSender, idHelper, senderPosition, offerNumber);
			messageQueue.addMessage(msg);
			if (getStatistics) {
				lqtstatistics.helpOfferMsgReceived();
			}
		}
	}

	public void refuseHelp(int idSender, int offerNumber)
			throws RemoteException {
		if ((state != ST_FINISHING) && (state != ST_FINISHED)) {
			LQT_Msg_HelpNotAccepted msg = new LQT_Msg_HelpNotAccepted(idSender,
					offerNumber);
			messageQueue.addMessage(msg);
		}
	}

	public void acceptHelp(int idSender, int offerNumber) throws RemoteException {
		if ((state != ST_FINISHING) && (state != ST_FINISHED)) {
			LQT_Msg_HelpAccepted msg = new LQT_Msg_HelpAccepted(idSender, offerNumber);
			messageQueue.addMessage(msg);
		}
	}

	// To be called by Query Executor when the entire range has been processed
	public synchronized void rangeProcessed() throws RemoteException, InterruptedException {
		// a_blocked = true;
		if (state != ST_PROCESSING_QUERY) {
			notifyAll();
			// a_blocked = false;
			throw new IllegalStateException("LocalQueryTaskEngine.rangeProcessed(): called while in state "	+ state);
		}

		if(range.getFirstValue() < range.getCurrentLastValue()) {
			getLQT(id).intervalFinished(id,	range.getFirstValue(), range.getCurrentLastValue());
			if(getStatistics) {
				if(id == range.getIdOwner())
					statistics.ownedRangeProcessed();
				else
					statistics.ownedRangeProcessed();
			}
		}
		state = ST_IDLE;
		if(statistics != null)
			statistics.idle();
		if(performDynLoadBal)
		{
			sendOfferHelpToNeighbors();
			if(getStatistics) {
				statistics.newBroadcast();
			}
		}
		// a_blocked = false;
		notifyAll();
	}

	// Calculate grid information and obtain references to neighbors.
	private void enterGrid() throws RemoteException {
		//System.out.println("Montando grid ...");
		// Neighbors will be calculated as if nodes were in a grid.
		idNeighbor = new int[NUMBER_OF_NEIGHBORS];
		numNodesPerLine = (int) Math.ceil(Math.sqrt(numlqts));
		numGridLines = (int) Math.ceil((float) numlqts / (float) numNodesPerLine);
		// line occupied by the node if it was in a grid
		line = (int) Math.floor(id / numNodesPerLine);
		// column occupied by the node if it was in a grid
		column = id % numNodesPerLine;

		// Calculate neighbors North
		if (line > 0)
			idNeighbor[NEIGHBOR_NORTH] = (line - 1) * numNodesPerLine + column;
		else
			idNeighbor[NEIGHBOR_NORTH] = NEIGHBOR_ABSENT;

		// East
		if (column < (numNodesPerLine - 1)) {
			idNeighbor[NEIGHBOR_EAST] = line * numNodesPerLine + (column + 1);
			if (idNeighbor[NEIGHBOR_EAST] >= numlqts) {
				// Grid is not a square.
				idNeighbor[NEIGHBOR_EAST] = NEIGHBOR_ABSENT;
			}
		} else
			idNeighbor[NEIGHBOR_EAST] = NEIGHBOR_ABSENT;

		// South
		if (line < (numGridLines - 1)) {
			idNeighbor[NEIGHBOR_SOUTH] = (line + 1) * numNodesPerLine + column;
			if (idNeighbor[NEIGHBOR_SOUTH] >= numlqts) {
				// Grid is not a square.
				idNeighbor[NEIGHBOR_SOUTH] = NEIGHBOR_ABSENT;
				respForEastMsgLastLine = false;
			} else if ((idNeighbor[NEIGHBOR_SOUTH] == numlqts - 1)
					&& (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT))
				respForEastMsgLastLine = true;
			else
				respForEastMsgLastLine = false;
		} else {
			idNeighbor[NEIGHBOR_SOUTH] = NEIGHBOR_ABSENT;
			respForEastMsgLastLine = false;
		}

		// West
		if (column > 0)
			idNeighbor[NEIGHBOR_WEST] = line * numNodesPerLine + (column - 1);
		else
			idNeighbor[NEIGHBOR_WEST] = NEIGHBOR_ABSENT;

		// obtain references to neighbors
		/*
		 * LocalQueryTask []neighborlqt = new LocalQueryTask[
		 * NUMBER_OF_NEIGHBORS ]; a_globalTask.getLQTReferences( idNeighbor,
		 * neighborlqt ); for( int i = 0; i < neighborlqt.length; i++ ) if(
		 * neighborlqt[i] != null ) a_lqt[ idNeighbor[i] ] = neighborlqt[i];
		 */

		// own reference
		lqt[id] = this;

		// determine how many help refuse messages must arrive before repeating
		// help offering
		if ((column == 0) || (column == numNodesPerLine - 1)) {
			if ((column == 0) && (id == numlqts - 1))
				numRefusesForHelpReoffer = numGridLines - 1;
			else
				numRefusesForHelpReoffer = numGridLines;
		} else if (id == numlqts - 1) {
			numRefusesForHelpReoffer = (2 * numGridLines) - 1;
		} else {
			numRefusesForHelpReoffer = 2 * numGridLines;
			if (numlqts - (numNodesPerLine * (numGridLines - 1)) == 1) {
				// Only one node in the last line. One less message is needed.
				numRefusesForHelpReoffer--;
			}
		}
	}

	private LocalQueryTask getLQT(int id) throws RemoteException {
		if (lqt[id] == null)
			lqt[id] = this.globalTask.getLQTReference(id);
		return lqt[id];
	}

	private synchronized void treatFinishMessage() throws InterruptedException {
		// a_blocked = true;
		if (state != ST_IDLE) {
			// a_blocked = false;
			//throw new IllegalStateException("LocalQueryTaskEngine.treatFinishMessage(): request to finish while object state = " + state + "!");
			wait();
		}
		state = ST_FINISHING;
		// a_blocked = false;
		notifyAll();
	}

	private synchronized void treatIntervalFinishedMessage(LQT_Msg_IntervalFinished msg) throws RemoteException {
		// a_blocked = true;
		if (intervalNotProcessed.isEmpty()) {
			notifyAll();
			// a_blocked = false;
			throw new IllegalThreadStateException("LocalQueryTaskEngine.treatIntervalFinishedMessage(): 'interval finished' message arrived but it has already been completely processed!");
		}
		intervalNotProcessed.removeInterval(msg.getIntervalBeginning(), msg.getIntervalEnd());
		if (intervalNotProcessed.isEmpty()) {
			//System.out.println("Intervalo terminou ...");
			// interval under responsibility of this LQT is already finished
			this.globalTask.localIntervalFinished(id);
		}
		logger.debug(Messages.getString("localQueryTaskEngine.intervalProcessed", new Object[] {msg.getIntervalBeginning(), msg.getIntervalEnd(), msg.getIdSender() }));
		System.out.println("To be processed: ");
		// TODO: Como imprimir isso
		intervalNotProcessed.print(System.out);
		// a_blocked = false;

		//state = ST_FINISHING; //by Luiz Matos only SVP

		notifyAll();
	}

	private synchronized void treatHelpAcceptedMessage(LQT_Msg_HelpAccepted msg) throws RemoteException {
		//System.out.println("Entrei em treatHelpAcceptedMessage no ofertante de ajuda ..");
		if (state == ST_IDLE) {
			// Still idle. Help.
			Range newRange = getLQT(msg.getIdSender()).getPartOfRange();
			if (newRange != null) {
				range.reset(newRange.getIdOwner(), newRange.getFirstValue(), newRange.getOriginalLastValue());
				state = ST_PROCESSING_QUERY;
				qe.newRange(range);
				if (lqtstatistics != null) {
					lqtstatistics.notIdle();
					lqtstatistics.helpOfferGiven();
				}
			}
			//System.out.println(" processing interval( " + range.getFirstValue() + " - " + range.getOriginalLastValue() + ") which is originally from " + range.getIdOwner());
			// TODO: Completar aqui o internationalization
			logger.debug(Messages.getString("localQueryTaskEngine.helping", msg.getIdSender()));
			if (newRange != null)
				logger.debug(" processing interval( " + range.getFirstValue() + " - " + range.getOriginalLastValue() + ") which is originally from " + range.getIdOwner());
			else
				logger.debug(" but he had no more intervals to process");
		} else {
			if (getStatistics) {
				lqtstatistics.helpAcceptanceMsgDespised();
			}
			logger.debug(msg.getIdSender()	+ " accepted a help offer but I cannot help any more");
		}
		notifyAll();
		// Create interval tree
		intervalNotProcessed = new IntervalTree(range.getFirstValue(), range.getOriginalLastValue()); // checar, LUIZ MATOS

	}

	private synchronized void treatHelpNotAcceptedMessage(LQT_Msg_HelpNotAccepted msg) throws RemoteException {
		// a_blocked = true;
		logger.debug(Messages.getString("localQueryTaskEngine.refuseToHelp", new Object[] { msg.getOfferNumber(), msg.getIdSender() }));
		if (msg.getOfferNumber() == numLastHelpOffer) {
			numRefusesReceived++;
			if (numRefusesReceived == numRefusesForHelpReoffer) {
				logger.debug(Messages.getString("localQueryTaskEngine.resendingOffer"));
				if (state == ST_IDLE) {
					sendOfferHelpToNeighbors();
					if (getStatistics) {
						lqtstatistics.broadcastRestarted();
					}
				}
			} else if (numRefusesReceived > numRefusesForHelpReoffer) {
				// a_blocked = false;
				throw new IllegalThreadStateException(
						"LocalQueryTaskEngine.treatHelpNotAcceptedMessage(): "
								+ numRefusesForHelpReoffer
								+ " refuses were expected but "
								+ numRefusesReceived + " were received!");
			}
		} else {
			logger.debug(Messages.getString("localQueryTaskEngine.ignored"));
		}
		// a_blocked = false;
		notifyAll();
	}

	private synchronized void sendOfferHelpToNeighbors() throws RemoteException {
		// a_blocked = true;
		numLastHelpOffer++;
		if (idNeighbor[NEIGHBOR_NORTH] != NEIGHBOR_ABSENT)
			getLQT(idNeighbor[NEIGHBOR_NORTH]).offerHelp(id, id, NEIGHBOR_SOUTH, numLastHelpOffer);
		if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
			getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id, id, NEIGHBOR_WEST, numLastHelpOffer);
		if (idNeighbor[NEIGHBOR_SOUTH] != NEIGHBOR_ABSENT)
			getLQT(idNeighbor[NEIGHBOR_SOUTH]).offerHelp(id, id, NEIGHBOR_NORTH, numLastHelpOffer);
		if (idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
			getLQT(idNeighbor[NEIGHBOR_WEST]).offerHelp(id, id, NEIGHBOR_EAST, numLastHelpOffer);
		numRefusesReceived = 0;
		logger.debug(Messages.getString("localQueryTaskEngine.helpOffer", numLastHelpOffer));
		// a_blocked = false;
		notifyAll();
	}

	private synchronized void treatHelpOffer(LQT_Msg_HelpOffer msg)	throws RemoteException {
		// a_blocked = true;
		//System.out.println("Entrei em treatHelpOffer no aceitante da ajuda ..");
		if (state == ST_IDLE) { //se ocioso, propaga a oferta de ajuda para os demais
			logger.debug(Messages.getString("localQueryTaskEngine.propagateOffer", new Object[] {msg.getOfferNumber(), msg.getIdHelper(), msg.getIdSender() }));
			propagateHelpOffer(msg);
			if (getStatistics) {
				lqtstatistics.helpOfferForwarded();
			}
		} else if (state == ST_PROCESSING_QUERY) { //se ocupado, aceita a oferta de ajuda
			//System.out.println("msg.getOfferNumber(): " + msg.getOfferNumber() + " msg.getIdHelper(): " + msg.getIdHelper()	+ " msg.getIdSender(): " + msg.getIdSender());
			logger.debug(Messages.getString("localQueryTaskEngine.acceptOffer",	new Object[] { msg.getOfferNumber(), msg.getIdHelper(),	msg.getIdSender() }));
			getLQT(msg.getIdHelper()).acceptHelp(this.id, msg.getOfferNumber());
			if (getStatistics) {
				lqtstatistics.helpOfferAcceptanceSent();
			}
			logger.debug(Messages.getString("localQueryTaskEngine.acceptanceMsg", msg.getIdHelper()));
		}
		// a_blocked = false;
		notifyAll();
	}

	// Must be called by a synchronized method
	private void propagateHelpOffer(LQT_Msg_HelpOffer msg) throws RemoteException {
		switch (msg.getSenderPosition()) {
		case NEIGHBOR_NORTH: {
			if (idNeighbor[NEIGHBOR_SOUTH] != NEIGHBOR_ABSENT)
				getLQT(idNeighbor[NEIGHBOR_SOUTH]).offerHelp(id, msg.getIdHelper(), NEIGHBOR_NORTH,	msg.getOfferNumber());
			if ((idNeighbor[NEIGHBOR_WEST] == NEIGHBOR_ABSENT)	&& (idNeighbor[NEIGHBOR_EAST] == NEIGHBOR_ABSENT)) {
				// This node is the only one in the last line. Send a refusing
				// message.
				getLQT(msg.getIdHelper()).refuseHelp(id, msg.getOfferNumber());
				logger.debug(Messages.getString(
						"localQueryTaskEngine.refusing", new Object[] {
								msg.getOfferNumber(), msg.getIdHelper(),
								msg.getIdSender() }));
			} else {
				if (column < numNodesPerLine / 2) {
					// Node is in the west part. Start propagating to east.
					if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
						getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id,
								msg.getIdHelper(), NEIGHBOR_WEST,
								msg.getOfferNumber());
					if (this.idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
						getLQT(idNeighbor[NEIGHBOR_WEST]).offerHelp(id,
								msg.getIdHelper(), NEIGHBOR_EAST,
								msg.getOfferNumber());
				} else {
					// Node is in the east part. Start propagating to west.
					if (idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
						getLQT(idNeighbor[NEIGHBOR_WEST]).offerHelp(id,
								msg.getIdHelper(), NEIGHBOR_EAST,
								msg.getOfferNumber());
					if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
						getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id,
								msg.getIdHelper(), NEIGHBOR_WEST,
								msg.getOfferNumber());
				}
			}
			break;
		}
		case NEIGHBOR_SOUTH: {
			if (idNeighbor[NEIGHBOR_NORTH] != NEIGHBOR_ABSENT)
				getLQT(idNeighbor[NEIGHBOR_NORTH])
				.offerHelp(id, msg.getIdHelper(), NEIGHBOR_SOUTH,
						msg.getOfferNumber());
			if (column < numNodesPerLine / 2) {
				// Node is in the west part. Start propagating to east.
				if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
					getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id,
							msg.getIdHelper(), NEIGHBOR_WEST,
							msg.getOfferNumber());
				if (this.idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
					getLQT(idNeighbor[NEIGHBOR_WEST]).offerHelp(id,
							msg.getIdHelper(), NEIGHBOR_EAST,
							msg.getOfferNumber());
			} else {
				// Node is in the east part. Start propagating to west.
				if (idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
					getLQT(idNeighbor[NEIGHBOR_WEST]).offerHelp(id,
							msg.getIdHelper(), NEIGHBOR_EAST,
							msg.getOfferNumber());
				if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
					getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id,
							msg.getIdHelper(), NEIGHBOR_WEST,
							msg.getOfferNumber());
			}
			break;
		}
		case NEIGHBOR_WEST: {
			if (idNeighbor[NEIGHBOR_EAST] != NEIGHBOR_ABSENT)
				getLQT(idNeighbor[NEIGHBOR_EAST]).offerHelp(id,
						msg.getIdHelper(), NEIGHBOR_WEST, msg.getOfferNumber());
			else {
				// No one else to propagate the offer. Send a refusing message.
				getLQT(msg.getIdHelper()).refuseHelp(id, msg.getOfferNumber());
				logger.debug(Messages.getString(
						"localQueryTaskEngine.refusing", new Object[] {
								msg.getOfferNumber(), msg.getIdHelper(),
								msg.getIdSender() }));
			}
			break;
		}
		case NEIGHBOR_EAST: {
			// If this node is reponsible for east messages arriving at last
			// line, propagate to south
			if (respForEastMsgLastLine)
				getLQT(idNeighbor[NEIGHBOR_SOUTH])
				.offerHelp(id, msg.getIdHelper(), NEIGHBOR_NORTH,
						msg.getOfferNumber());

			if (this.idNeighbor[NEIGHBOR_WEST] != NEIGHBOR_ABSENT)
				getLQT(this.idNeighbor[NEIGHBOR_WEST]).offerHelp(id,
						msg.getIdHelper(), NEIGHBOR_EAST, msg.getOfferNumber());
			else {
				// No one else to propagate the offer. Send a refusing message.
				getLQT(msg.getIdHelper()).refuseHelp(id, msg.getOfferNumber());
				logger.debug(Messages.getString(
						"localQueryTaskEngine.refusing", new Object[] {
								msg.getOfferNumber(), msg.getIdHelper(),
								msg.getIdSender() }));
			}
			break;
		}
		default:
			throw new IllegalArgumentException("LocalQueryTaskEngine.propagateHelpOffer(): invalid neighbor position ("	+ msg.getSenderPosition() + ").");
		}
	}

	// getPartOfRange() - called by who is giving help
	public Range getPartOfRange() throws RemoteException {
		Range range = this.range.cutTail(HELP_WORKLOAD_FRACTION);
		if (getStatistics) {
			lqtstatistics.helpOfferReceived();
		}
		return range;
	}

	public ArrayList<String> getCompletePartialResultFileNames() throws RemoteException {
		return LocalQueryTaskEngine.completePartialResultFileNames;
	}

	public void setCompletePartialResultFileNames(String completePartialResultFileName) throws RemoteException {
		LocalQueryTaskEngine.completePartialResultFileNames.add(completePartialResultFileName);
	}

	public long getTimeComposePartialResultFiles() throws RemoteException {
		return LocalQueryTaskEngine.timeComposePartialResultFiles;
	}

	public void setTimeComposePartialResultFiles(long timeComposePartialResultFiles) throws RemoteException {
		LocalQueryTaskEngine.timeComposePartialResultFiles = timeComposePartialResultFiles;
	}

	public int getQueryExecutionStrategy() throws RemoteException {
		return this.queryExecutionStrategy;
	}
}
