package uff.dew.avp.localqueryprocessor.dynamicrangegenerator;

import java.io.PrintStream;
import java.io.Serializable;
/**
 * 
 * @author lima
 */
public abstract class PartitionTuner implements Serializable {
    
	private static final long serialVersionUID = 1L;

	// Called by QueryExecutor when finished testing one size
    public abstract void setSizeResults( PartitionSize size );
    
    // Called by QueryExecutor when demanding a new partition size to use
    public abstract PartitionSize getPartitionSize();
    
    public abstract boolean stillTuning();
    
    public abstract void printTuningStatistics( PrintStream out );
    
    public abstract void reset();
    
    public abstract void setMaxNumberOfKeys(long newTotalTableTuples);
    
    public abstract void setLastNumberOfKeys(int lastNumberOfKeys);
}