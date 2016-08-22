package alouw.csc552.hamming;

/*
 * A thread-safe, FIFO, blocking channel. 
 * Channels are independent of all consumer/producers and are responsible
 * only for managing their own state in a thread-safe manner.
 */
public interface HammingNetworkChannel {

	public void putFirst(Integer value) throws InterruptedException;
	public Integer takeFirst() throws InterruptedException;
	public void putLast(Integer value) throws InterruptedException;
	public Integer takeLast() throws InterruptedException;
}