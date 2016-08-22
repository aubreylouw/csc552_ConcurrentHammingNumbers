package ajeffrey.teaching.debug;

/**
 * An inferface for printing debugging messages.
 * @author Alan Jeffrey
 * @version 1.0.3
 */
public interface DebugStream {

    /**
     * Prints a debugging message.
     * @param msg the message to print
     */
    public void println (String msg);

    /**
     * Prints a debugging message and sets a break point.
     * Note that this should not be done in the main GUI thread!
     * @param msg the message to print
     */
    public void breakPoint (String msg);

}
