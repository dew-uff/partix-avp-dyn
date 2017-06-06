package uff.dew.avp.localqueryprocessor.localquerytask;

/**
 *
 * @author  lima
 */
public class LQT_Msg_HelpAccepted extends LQT_Msg_Help {
    
    // Sender is the one who is accepting help
    
    /** Creates a new instance of LQT_Msg_HelpAcceptance */
    public LQT_Msg_HelpAccepted( int idSender, int offerNumber ) {
        super( idSender, offerNumber );
    }
    
    public int getType() {
        return MSG_HELPACCEPTED;
    }
}
