package ajeffrey.teaching.debug;

/**
 * An interface for building debugging streams.
 * @author Alan Jeffrey
 * @version 1.0.3
 */
public interface DebugStreamFactory {

    /**
     * Builds a new debugging stream.
     * This stream should only every be used for printing
     * debugging messages by the current thread.
     * @param threadName a human-readable name for the current thread
     */
    public DebugStream build (String threadName);

}
