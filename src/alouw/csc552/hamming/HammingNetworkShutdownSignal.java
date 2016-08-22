package alouw.csc552.hamming;

/*
 * An object for encapsulating the kill signal for a Hamming Network.
 */
public class HammingNetworkShutdownSignal {
	
	private volatile HammingNetwork network;
	
	public HammingNetworkShutdownSignal () {};
	
	public void attachNetwork(HammingNetwork network) {
		this.network = network;
	}
	
	public void sendShutdown() {
		this.network.shutdown();
	}
}