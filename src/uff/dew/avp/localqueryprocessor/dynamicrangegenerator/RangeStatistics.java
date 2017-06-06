package uff.dew.avp.localqueryprocessor.dynamicrangegenerator;

import java.io.Serializable;
/**
 * 
 * @author lima
 */
public class RangeStatistics implements Serializable {
    
	private static final long serialVersionUID = 3978143257505314360L;
	private float a_meanNumTuplesPerValue;
    private long a_numTableTuples; // number of tuples on the table
    private float a_maxSizeRatio; // informed during object creation
    
    /** Creates a new instance of RelationStatistics */
    public RangeStatistics( float meanNumTuplesPerValue, 
                            long numTableTuples,
                            float maxSizeRatio ) {
        a_meanNumTuplesPerValue = meanNumTuplesPerValue;
        a_numTableTuples = numTableTuples;
        a_maxSizeRatio = maxSizeRatio;
    }
    
    public float getMeanNumTuplesPerValue() {
        return a_meanNumTuplesPerValue;
    }
    
    public void setMeanNumTuplesPerValue(float newMeanNumTuplesPerValue) {
        this.a_meanNumTuplesPerValue = newMeanNumTuplesPerValue;
    }
    
    public long getTotalNumberOfTuples() {
        return a_numTableTuples;
    }
    
    public void setTotalNumberOfTuples(long newTotalTableTuples) {
        this.a_numTableTuples = newTotalTableTuples;
    }
    
	public float getMaxSizeRatio() {
        return a_maxSizeRatio;
    }

	public void setMaxSizeRatio(float newMaxSizeRatio) {
        this.a_maxSizeRatio = newMaxSizeRatio;
    }
	
	public void ownedRangeProcessed() {
		// TODO Auto-generated method stub
		
	}

	public void idle() {
		// TODO Auto-generated method stub
		
	}

	public void newBroadcast() {
		// TODO Auto-generated method stub
		
	}
}

