package uff.dew.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import uff.dew.avp.AVPConst;
import uff.dew.svp.ExecutionContext;
import uff.dew.svp.FinalResultComposer;
import uff.dew.svp.db.DatabaseException;

public class FinalResultComposerTest {
    
    public void testExecuteFinalCompositionRegular() {
        
        try {
            FileOutputStream fos = new FileOutputStream(AVPConst.FINAL_RESULT_DIR + "finalResult.xml");
            
            FinalResultComposer frc = new FinalResultComposer(fos);
            frc.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);
            frc.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));

            // necessary
            frc.cleanup();
            File partialsDir = new File(AVPConst.PARTIAL_RESULTS_DIRECTORY);
            File[] partials = partialsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith("partial_") && name.endsWith(".xml")) {
                        return true;
                    }
                    return false;
                }
            });
            
            Arrays.sort(partials);
            
            for (File partial : partials) {
                FileInputStream fis = new FileInputStream(partial);
                frc.loadPartial(fis);
                fis.close();
            }
            System.out.println(frc.toString());
            frc.combinePartialResults();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DatabaseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testExecuteFinalCompositionRegularForceTempCollectionMode() {
        
        try {
            FileOutputStream fos = new FileOutputStream(AVPConst.FINAL_RESULT_DIR + "finalResult.xml");
            
            FinalResultComposer frc = new FinalResultComposer(fos);
            frc.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);
            frc.setForceTempCollectionExecutionMode(true); 
            frc.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));

            // necessary
            frc.cleanup();
            File partialsDir = new File(AVPConst.PARTIAL_RESULTS_DIRECTORY);
            File[] partials = partialsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith("partial_") && name.endsWith(".xml")) {
                        return true;
                    }
                    return false;
                }
            });
            
            for (File partial : partials) {
                FileInputStream fis = new FileInputStream(partial);
                frc.loadPartial(fis);
                fis.close();
            }
            frc.combinePartialResults();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DatabaseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testExecuteFinalCompositionAggregation() {
        
        try {
            FileOutputStream fos = new FileOutputStream(AVPConst.FINAL_RESULT_DIR + "finalResult.xml");
            FinalResultComposer frc = new FinalResultComposer(fos);
            frc.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);
            frc.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));

            // necessary!
            frc.cleanup();            
            File partialsDir = new File(AVPConst.PARTIAL_RESULTS_DIRECTORY);
            File[] partials = partialsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith("partial_") && name.endsWith(".xml")) {
                        return true;
                    }
                    return false;
                }
            });
            
            for (File partial : partials) {
                FileInputStream fis = new FileInputStream(partial);
                frc.loadPartial(fis);
                fis.close();
            }
            
            frc.combinePartialResults(); //Composicao do resultado final
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DatabaseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
 public void testExecuteFinalOnlyTempCollectionMode() {
        
        try {
            FileOutputStream fos = new FileOutputStream(AVPConst.FINAL_RESULT_DIR + "finalResult.xml");
            
            FinalResultComposer frc = new FinalResultComposer(fos);
            frc.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);

            frc.setForceOnlyTempCollectionExecutionMode(true); 
            frc.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));

            frc.combinePartialResults();
            
            fos.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DatabaseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
}
