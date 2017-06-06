package uff.dew.avp;

public class AVPConst {
	/* Informacoes do BD */
	public static final String DB_CONF_LOCALHOST = "127.0.0.1";
	//public static final String DB_CONF_SERVERHOST = "192.168.0.21";
	//public static final String DB_CONF_SERVERHOST = "10.1.23.10";

	public static final int DB_CONF_PORT = 1984;
	public static final String DB_CONF_USERNAME = "admin";
	public static final String DB_CONF_PASSWORD = "admin";
	public static final String DB_CONF_TYPE = "BASEX";
	public static final String DB_CONF_DATABASE = "tpch";
	public static final String DB_TYPE_BASEX = "BASEX";
	public static final String DB_TYPE_SEDNA = "SEDNA";

	/* Informacoes do Catalogo */
	public static final String CATALOG_FILE_PATH = "input/";
	public static final String TPCH_CATALOG_FILE_NAME = "TPCHcatalog.xml";
	public static final String XMARK_CATALOG_FILE_NAME = "XMarkcatalog.xml";
	public static final String DBLP_CATALOG_FILE_NAME = "DBLPcatalog.xml";
	public static final String UNIPROTKB_CATALOG_FILE_NAME = "UniProtKBcatalog.xml";
	public static final String XBENCHTCMD_CATALOG_FILE_NAME = "XBenchTCMDcatalog.xml";

	/* Informacoes de Diretorios */
	public static final String QUERIES_FILE_PATH = "queries/"+AVPConst.DB_CONF_DATABASE+"/";
	public static final String SUBQUERIES_FILE_PATH = "output/";
	public static final String SUBQUERIES_FILE_NAME = "subquery";
	public static final String CONTEXT_FILE_PATH = "output/";
	public static final String CONTEXT_FILE_NAME = "context";
	
	//LOCAL
//	public static final String FINAL_RESULT_DIR = "output/"; //informar diretorio local
//	public static final String PARTIAL_RESULTS_FILE_PATH = "Z:/"; //informar diretorio compartilhado em rede
//	public static final String PARTIAL_RESULTS_DIRECTORY = "Z:/"; //informar diretorio compartilhado em rede
//	public static final String CQP_CONF_FILE_NAME = "Z:/CQP.conf";
//	public static final String CQP_CONF_DIR_NAME = "Z://";
//	public static final String NQP_CONF_FILE_NAME = "Z:/NQP.conf";
//	public static final String NQP_CONF_DIR_NAME = "Z://";

	//SUNHPC
//	public static final String FINAL_RESULT_DIR = "/prj/prjdocx/lamatos/temp/tpch/"; //informar diretorio local
//	public static final String PARTIAL_RESULTS_FILE_PATH = "/prj/prjdocx/lamatos/temp/"; //informar diretorio compartilhado em rede
//	public static final String PARTIAL_RESULTS_DIRECTORY = "/prj/prjdocx/lamatos/temp/"; //informar diretorio compartilhado em rede
//	public static final String CQP_CONF_FILE_NAME = "/prj/prjdocx/lamatos/temp/CQP.conf";
//	public static final String CQP_CONF_DIR_NAME = "/prj/prjdocx/lamatos/temp/";
//	public static final String NQP_CONF_FILE_NAME = "/prj/prjdocx/lamatos/temp/NQP.conf";
//	public static final String NQP_CONF_DIR_NAME = "/prj/prjdocx/lamatos/temp/";

	
	//OSCAR
	public static final String FINAL_RESULT_DIR = "output/"; //informar diretorio local
	public static final String PARTIAL_RESULTS_FILE_PATH = "/var/usuarios/luizmatos/nfs/compartilhado/temp/"+AVPConst.DB_CONF_DATABASE+"/"; //informar diretorio compartilhado em rede
	public static final String PARTIAL_RESULTS_DIRECTORY = "/var/usuarios/luizmatos/nfs/compartilhado/temp/"+AVPConst.DB_CONF_DATABASE+"/"; //informar diretorio compartilhado em rede
	public static final String CQP_CONF_FILE_NAME = "/var/usuarios/luizmatos/nfs/compartilhado/temp/CQP.conf";
	public static final String CQP_CONF_DIR_NAME = "/var/usuarios/luizmatos/nfs/compartilhado/temp/";
	public static final String NQP_CONF_FILE_NAME = "/var/usuarios/luizmatos/nfs/compartilhado/temp/NQP.conf";
	public static final String NQP_CONF_DIR_NAME = "/var/usuarios/luizmatos/nfs/compartilhado/temp/";

	/* Informacoes Gerais */
	public static final String TEMP_COLLECTION_NAME = "tmpPartialResults";
	public static final boolean COMPRESS_DATA = false;

}
