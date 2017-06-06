package uff.dew.avp.localqueryprocessor.localquerytask;

/**
 *
 * @author  Alex
 */
public class LQT_Msg_HelpNotAccepted extends LQT_Msg_Help {
    
    /** Creates a new instance of LQT_Msg_HelpNotAccepted */
    public LQT_Msg_HelpNotAccepted( int idSender, int offerNumber ) {
        super( idSender, offerNumber );
    }
    
    public int getType() {
        return MSG_HELPNOTACCEPTED;
    }
    
}
