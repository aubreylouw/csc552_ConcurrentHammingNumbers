package ajeffrey.teaching.debug;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.Iterator;

import ajeffrey.teaching.io.DevNull;

/**
 * An inferface for printing debugging messages.
 * @author Alan Jeffrey
 * @version 1.0.5
 */
public interface Debug extends DebugStream {

    /**
     * A debugging output stream.
     * This output stream will print messages including
     * thread identifiers, to keep track of safety errors
     * caused by concurrent programming.
     */
    public static final Debug out = new DebugImpl ();

    /**
     * Adds a new debug factory.
     * This factory will be used to build new debug
     * streams when debug messages are printed.
     * Each thread is given its own debug stream to
     * print to.
     * @param factory factory to build new debugging streams
     */
    public void addFactory (DebugStreamFactory factory);

    /**
     * Adds a new print stream
     * @param out an output stream to send messages to
     */
    public void addPrintStream (PrintStream out);

    /**
     * Adds a new output stream
     * @param out an output stream to send messages to
     */
    public void addOutputStream (OutputStream out);

    /**
     * Adds a new print stream
     * @param out an output stream to send messages to
     */
    public void addFile (String fileName) throws FileNotFoundException;

    /**
     * Adds a new print stream
     * @param out an output stream to send messages to
     * @deprecated: use addPrintStream instead
     */
    public void setPrintStream (PrintStream out);

    /**
     * Prints a debugging message.
     * @param msg the message to print
     */
    public void println (String msg);

    /**
     * Prints a debugging message, and adds a break point
     * where execution may pause (if using a step debugger).
     * Note: if you do this in the main GUI thread, you may
     * cause all GUI processing to halt!
     * @param msg the message to print
     */
    public void breakPoint (String msg);

    /**
     * Makes an assertion.
     * If the assertion is false, we add a break point.
     * @param assertion the assertion to check.
     */
    public void assertion (boolean assertion);

}

class DebugImpl implements Debug {

    final protected Object lock = new Object ();
    final protected HashMap perThread = new HashMap ();
    protected DebugStreamFactory[] factories = new DebugStreamFactory[0];

    public void addFactory (final DebugStreamFactory factory) {
	synchronized (lock) {
	    DebugStreamFactory[] newFactories = 
		new DebugStreamFactory[factories.length + 1];
	    System.arraycopy (factories, 0, newFactories, 0, factories.length);
	    newFactories[factories.length] = factory;
	    factories = newFactories;
	    for (final Iterator i = perThread.values ().iterator (); i.hasNext ();) {
	        PerThread next = (PerThread)(i.next ());
		next.setFactories (factories);
	    }
	}	
    }

    public void addPrintStream (final PrintStream out) {
	addFactory (new PrintDebugStreamFactory (out));
    }

    public void addOutputStream (final OutputStream out) {
	addPrintStream (new PrintStream (out, true));
    }

    public void addFile (final String fileName) throws FileNotFoundException {
	addOutputStream (new FileOutputStream (fileName));
    }

    public void setPrintStream (final PrintStream out) {
	addPrintStream (out);
    }
    
    protected PerThread getPerThread () {
	final Thread current = Thread.currentThread ();
	PerThread result = (PerThread)(perThread.get (current));
	if (result == null) {
	    synchronized (lock) {
		result = (PerThread)(perThread.get (current));
		if (result == null) {
		    result = new PerThread (factories);
		    perThread.put (current, result);
		}
	    }
	}
	return result;
    }

    public void println (final String msg) {
	getPerThread ().println (msg);
    }

    public void breakPoint (final String msg) {
	getPerThread ().breakPoint (msg);
    }

    public void assertion (final boolean assertion) {
	if (!assertion) {
	    breakPoint ("FAILED ASSERTION");
	}
    }

}

class PerThread {

    static int threadId = 0;
    static synchronized int newThreadId () { return ++threadId; }

    protected final String threadName = "Thread " + newThreadId ();
    protected DebugStream[] streams = new DebugStream[0];
    protected DebugStreamFactory[] factories;
    protected boolean printing = true;

    protected PerThread (final DebugStreamFactory[] factories) {
	this.factories = factories;
    }

    protected void setFactories (final DebugStreamFactory[] factories) {
	this.factories = factories;
    }

    protected void buildStreams () {
	if (streams.length < factories.length) {
	    final boolean currentPrinting =
		printing;
	    final DebugStream[] newStreams = 
		new DebugStream [factories.length];
	    printing = false;
	    System.arraycopy (streams, 0, newStreams, 0, streams.length);
	    for (int i=streams.length; i < factories.length; i++) {
		newStreams[i] = factories[i].build (threadName);
	    }
	    streams = newStreams;
	    printing = currentPrinting;		
	}
    }

    public void println (final String msg) {
	if (printing) {
	    buildStreams ();
	    for (int i=0; i < streams.length; i++) {
		streams[i].println (msg);
	    }
	}
    }

    public void breakPoint (final String msg) {
	if (printing) {
	    buildStreams ();
	    for (int i=0; i < streams.length; i++) {
		streams[i].breakPoint (msg);
	    }
	}
    }

}

class PrintDebugStreamFactory implements DebugStreamFactory {
    
    final PrintStream out;

    protected PrintDebugStreamFactory (final PrintStream out) {
	this.out = out;
    }

    public DebugStream build (final String threadName) {
	return new PrintDebugStream (out, threadName);
    }

}

class PrintDebugStream implements DebugStream {

    final PrintStream out;
    final String prefix;
    
    protected PrintDebugStream (final PrintStream out, final String threadName) {
	this.out = out;
	this.prefix = threadName + ": ";
    }

    public void println (final String msg) {
	out.println (prefix + msg);
    }

    public void breakPoint (final String msg) {
	out.println (prefix + msg);
    }

}
