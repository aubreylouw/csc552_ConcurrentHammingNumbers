package alouw.csc552.hamming;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * A configurable network of Hamming Nodes. Internally, the network is represented as a <string, node> hashmap.
 * Each node is connected to one or more other nodes via Hamming Channels.
 * 
 * A network will run until either:
 *  (a) generating the solution exceeds the configuration time allowance;
 *  (b) a control node detects that the desired # of Hamming numbers is calculated
 *  
 * In case of (b) above, the control node signals via a HammingNetworkShutdownSignal object that the network
 * should initiate a tear-down of both component nodes and the network itself.
 * 
 */
public class HammingNetwork{
	
	private static final HammingNetworkWorkerNodeFactory workerFactory = new HammingNetworkWorkerNodeFactory();
	private static final HammingNetworkChannelFactory channelFactory = new HammingNetworkChannelFactory();
	final static long TEARDOWN_TIME_MAX_DURATION = 1;
	final static TimeUnit TEARDOWN_TIME_UOM = TimeUnit.MINUTES;
	
	private final ExecutorService networkPool = Executors.newCachedThreadPool(
			new ThreadFactoryWithNamePrefix("HammingNetwork"));
	
	private final Map<String, HammingNetworkNode> network = new HashMap<>();
	private final HammingNetworkShutdownSignal signal = new HammingNetworkShutdownSignal();
	
	private volatile Integer maxNumbers;
	private volatile long duration;
	private volatile TimeUnit duration_uom;
	private final AtomicBoolean configured = new AtomicBoolean();
	
	public static final HammingNetwork INSTANCE = new HammingNetwork();

	private HammingNetwork() {this.configured.set(false);};
	
	public void configure(final int threshold, final long duration, final TimeUnit uom) {
		
		// define network parameters
		this.maxNumbers = Integer.valueOf(threshold);
		this.duration =  duration;
		this.duration_uom = uom;
		
		// create a shutdown signal
		this.signal.attachNetwork(this);
		
		// define the 3 multiply nodes
		network.put("mult2", workerFactory.getInstance(new Procedure() {
			public Integer apply(Integer value) {
				return Integer.valueOf(value.intValue() * 2);
			}
		}, "times2", duration, uom));
		
		network.put("mult3", workerFactory.getInstance(new Procedure() {
			public Integer apply(Integer value) {
				return Integer.valueOf(value.intValue() * 3);
			}
		}, "times3", duration, uom));
		
		network.put("mult5", workerFactory.getInstance(new Procedure() {
			public Integer apply(Integer value) {
				return Integer.valueOf(value.intValue() * 5);
			}
		}, "times5", duration, uom));
						
		// create the three control nodes
		network.put("copy4", HammingNetworkControlNodeFactory.COLLECT_NODE.getInstance(
				this.maxNumbers, this.signal, this.duration, this.duration_uom));
		network.put("merge3", HammingNetworkControlNodeFactory.MERGE_NODE.getInstance(
				this.maxNumbers, this.signal, this.duration, this.duration_uom));
		network.put("print1", HammingNetworkControlNodeFactory.PRINT_NODE.getInstance(
				this.maxNumbers, this.signal, this.duration, this.duration_uom));
				
		// connect the network starting from the terminal point and working backwards
		connectTwoNodes("copy4" , "print1");
		connectTwoNodes("copy4" , "mult2");
		connectTwoNodes("copy4" , "mult3");
		connectTwoNodes("copy4" , "mult5");
		connectTwoNodes("merge3", "copy4");
		connectTwoNodes("mult2" , "merge3");
		connectTwoNodes("mult3" , "merge3");
		connectTwoNodes("mult5" , "merge3");
		
		//the network is now configured
		this.configured.set(true);
	}

	public void start() {	
		
		if (!this.configured.get()) throw new IllegalStateException("This network is not configured");
		
		network.values().stream().forEach(n -> networkPool.execute(n));
		try {
			networkPool.awaitTermination(this.duration, this.duration_uom);
		} catch (InterruptedException e) {
			return;
		}
	}
	
	public void shutdown()  {
		network.values().stream().forEach(n -> {
			try {
				n.shutdown();
			} catch (Exception e) {
				return;
			}
		});
		networkPool.shutdownNow();
		try {
			networkPool.awaitTermination(TEARDOWN_TIME_MAX_DURATION, TEARDOWN_TIME_UOM);
		} catch (InterruptedException e) {
			return;
		}
	}
	
	private void connectTwoNodes(final String sourceName, final String targetName) {
		final HammingNetworkChannel channel = channelFactory.getChannelInstance(
				sourceName + "_to_" + targetName);
		network.get(sourceName).addOutputChannel(channel);
		network.get(targetName).addInputChannel(channel);
	}
}