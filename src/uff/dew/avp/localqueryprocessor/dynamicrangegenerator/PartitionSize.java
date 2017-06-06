package uff.dew.avp.localqueryprocessor.dynamicrangegenerator;

import java.io.Serializable;
/**
 * 
 * @author lima
 */
public class PartitionSize implements Comparable<Object>, Serializable {

    private static final long serialVersionUID = 3907217043819280437L;
    private float ratio;
    private int numKeys;
    private int numExpectedExecutions;
    private int numPerformedExecutions;
    private float meanExecTime;
    private long bestExecTime;
    private long worstExecTime;

    private long pureExecTime;
    
    /** Creates a new instance of PartitionSize */
    public PartitionSize(float ratio, int numKeys, int numExpectedExecutions)
            throws IllegalArgumentException {
        if (numKeys < 1)
            throw new IllegalArgumentException("Invalid number of keys: " + numKeys);
        if (ratio <= 0)
            throw new IllegalArgumentException("Invalid size ratio: " + ratio);
        if (numExpectedExecutions <= 0)
            throw new IllegalArgumentException(
                    "Invalid number of expected executions: "
                            + numExpectedExecutions);
        this.ratio = ratio;
        this.numKeys = numKeys;
        meanExecTime = (float) 0.0;
        this.numExpectedExecutions = numExpectedExecutions;
        numPerformedExecutions = 0;
        bestExecTime = 0;
        worstExecTime = 0;
        pureExecTime = 0;
    }

    // To be called when an execution is finished.
    public void setExecTime(long execTime) {
        numPerformedExecutions++;
        if (numPerformedExecutions == 1) {
            meanExecTime = execTime;
            bestExecTime = execTime;
            worstExecTime = execTime;
        } else {
            meanExecTime = (((numPerformedExecutions - 1) * meanExecTime) + execTime) / (float) numPerformedExecutions;
            if (execTime < bestExecTime)
                bestExecTime = execTime;
            else if (execTime > worstExecTime)
                worstExecTime = execTime;
        }
    }

    public void setExecNotMeanTime(long execTimeProcessing) {
        numPerformedExecutions++;
        pureExecTime = execTimeProcessing;
//        System.out.println("pureExecTime (nano) = " + pureExecTime*1000000);
//        System.out.println("pureExecTime (ms) = " + pureExecTime);
    }
    
    public float getRatio() {
        return ratio;
    }

    public int numberOfKeys() {
        return numKeys;
    }

    public float getMeanExecutionTime() throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return meanExecTime;
    }

    public float getEstimatedExecutionTime() throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return ((float) 1.0 / ratio) * meanExecTime;
    }

    public float getBestExecutionTime() throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return bestExecTime;
    }

    public float getBestTotalEstimatedExecutionTime()
            throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return ((float) 1.0 / ratio) * bestExecTime;
    }

    public float getWorstExecutionTime() throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return worstExecTime;
    }

    public float getWorstTotalEstimatedExecutionTime()
            throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return ((float) 1.0 / ratio) * worstExecTime;
    }

    public float getPureExecutionTime() throws IllegalStateException {
        if (numPerformedExecutions == 0)
            throw new IllegalStateException("Some object is trying to get not set Partition Size collected data");
        return (float)pureExecTime;
    }
    
    public int getNumExpectedExecutions() {
        return numExpectedExecutions;
    }

    public int getNumPerformedExecutions() {
        return numPerformedExecutions;
    }

    public int compareTo(Object o) {
        float diff;
        diff = getRatio() - ((PartitionSize) o).getRatio();
        if (diff == 0)
            return 0;
        else if (diff < 0)
            return -1;
        else
            return 1;
    }

    public String toString() {
        return "( " + ratio + ", " + numKeys + ", " + numExpectedExecutions + ", " + numPerformedExecutions + " )";
    }

}

