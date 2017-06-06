package uff.dew.avp.localqueryprocessor.localquerytask;

/**
 *
 * @author  Alex
 */
public class LQT_Msg_Finish extends LQT_Message {
    
    /** Creates a new instance of LQT_Msg_Finish */
    public LQT_Msg_Finish() {
        super( -1 );
    }
    
    public int getType() {
        return MSG_FINISH;
    }
    
}
