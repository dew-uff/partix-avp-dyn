package uff.dew.avp.localqueryprocessor.localquerytask;

/**
 *
 * @author  Alex
 */
public class LQT_Msg_IntervalFinished extends LQT_Message {
    
    private int a_intervalBeginning;
    private int a_intervalEnd;
    
    /** Creates a new instance of LQT_Msg_IntervalFinished */
    public LQT_Msg_IntervalFinished( int idSender, int intervalBeginning, int intervalEnd ) {
        super( idSender );
        a_intervalBeginning = intervalBeginning;
        a_intervalEnd = intervalEnd;
    }
    
    public int getType() {
        return MSG_INTERVALFINISHED;
    }
    
    public int getIntervalBeginning() {
        return a_intervalBeginning;
    }

    public int getIntervalEnd() {
        return a_intervalEnd;
    }
}
