package uff.dew.avp.connection;

/**
 *
 * @author  lima
 */

import java.rmi.RemoteException;
import java.util.LinkedList;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.svp.db.Database;
import uff.dew.svp.db.DatabaseException;
import uff.dew.svp.db.DatabaseFactory;
 
public class DBConnectionPoolEngine {
	
	private Logger logger = Logger.getLogger(DBConnectionPoolEngine.class);
	
    private String hostName;
    private int port;
    private String dbLogin;
    private String dbPassword;
    private String databaseName;
    private String dbmsType;
    private int iniConnectionPoolSize;
    private LinkedList<Database> connPool;
        
    /** Creates a new instance of DBConnectionPoolEngine */
    /** adapted by Luiz Matos */
    public DBConnectionPoolEngine(String hostName, int port, String dbLogin, String dbPassword, String databaseName, int dbmsX, int iniPoolSize) throws RemoteException, DatabaseException {
        super();
        this.hostName = hostName;
        this.port = port;
        this.dbLogin = dbLogin;
        this.dbPassword = dbPassword;
        this.databaseName = databaseName;
        if (dbmsX == 1)
        	this.dbmsType = AVPConst.DB_TYPE_SEDNA;
        else if (dbmsX == 2)
        	this.dbmsType = AVPConst.DB_TYPE_BASEX;
        this.iniConnectionPoolSize = iniPoolSize;
        createConnectionPool();
		logger.info("DBConnection Pool created!");		
    }
    
	public void shutdown() throws Throwable {
		//UnicastRemoteObject.unexportObject(this,true);
		finalize();
	}
	
//    protected void finalize() throws Throwable {
//        closeConnectionPool();
//        super.finalize();		
//    }
	   
    public synchronized void createConnectionPool() throws RemoteException, DatabaseException {
        connPool = new LinkedList<Database>();
        if( iniConnectionPoolSize > 0 ) {
            for( int i = 0; i < iniConnectionPoolSize; i++ ) {
            	DatabaseFactory.produceSingletonDatabaseObject(hostName, port, dbLogin, dbPassword, databaseName, dbmsType);
            	Database conn = DatabaseFactory.getSingletonDatabaseObject();
            	connPool.addLast( conn );
            }
        }
        notifyAll();
    }

    //TODO: bolar forma de fechar conexões
//    public synchronized void closeConnectionPool() throws RemoteException {
//    	Database conn;		
//        while( connPool.size() > 0 ) {
//            conn = (Database) connPool.removeFirst();
//            conn.close();
//            conn = null;
//        }
//		logger.debug(Messages.getString("dbconnectionpool.closed"));		
//        notifyAll();
//    }

    public synchronized Database reserveConnection() throws RemoteException, DatabaseException {
    	Database conn;
        if( connPool.size() > 0 )
            conn = (Database) connPool.removeFirst();
        else
        	DatabaseFactory.produceSingletonDatabaseObject(hostName, port, dbLogin, dbPassword, databaseName, dbmsType);
            conn = DatabaseFactory.getSingletonDatabaseObject();
        notifyAll();
        return conn;
    }

    //TODO: bolar forma de liberar conexão
//    public synchronized void disposeConnection(Database conn) throws RemoteException, XQException {
//        if( !conn.isClosed() ) {
//            conn.clear();
//            connPool.addFirst( conn );
//        }
//        notifyAll();
//    }
    
}
