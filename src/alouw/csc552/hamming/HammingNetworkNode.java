package alouw.csc552.hamming;

/* 
 * A node in a Hamming network. All nodes are independent and can communicate with the
 * network only via pre-defined channels.
 * 
 * The start method blocks until the node is done processing.
 */
public interface HammingNetworkNode extends Runnable{

	/* start the node for processing */
	public void start();
	
	/* shut the node down */
	public void shutdown() throws InterruptedException;
	
	/* the node will read from this channel */
	public void addInputChannel(HammingNetworkChannel input);
	
	/* the node will write to this channel */
	public void addOutputChannel(HammingNetworkChannel output);
	
}