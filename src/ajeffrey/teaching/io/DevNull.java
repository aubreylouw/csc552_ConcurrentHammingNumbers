package ajeffrey.teaching.io;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * An output stream which discards messages
 * @author Alan Jeffrey
 * @version 1.0.3
 */
public interface DevNull {

    /**
     * An output stream which discards messages
     */
    public static final OutputStream out = new DevNullOutputStream ();

    /**
     * A print stream which discards messages
     */
    public static final PrintStream printStream = new PrintStream (out);

    /**
     * A writer which discards messages
     */
    public static final Writer writer = new OutputStreamWriter (out);

    /**
     * A print writer which discards messages
     */
    public static final PrintWriter printWriter = new PrintWriter (writer);

}

class DevNullOutputStream extends OutputStream {

    public void write (final byte[] b) {}

    public void write (final byte[] b, final int off, final int len) {}
	    
    public void write (int b) {}

}
