package alouw.csc552.hamming;

import java.util.concurrent.LinkedBlockingDeque;

/*
 * A factory for producing Hamming Channels. 
 */
public class HammingNetworkChannelFactory {

	public HammingNetworkChannel getChannelInstance(String name) {
		return new HammingNetworkBlockingChannelImpl(name);
	}
}

/*
 * Thread safety policy: each channel delegates thread-safety to a blocking deque.
 */
class HammingNetworkBlockingChannelImpl implements HammingNetworkChannel {
	
	private final String name;
	private final LinkedBlockingDeque<Integer> deque;
	
	HammingNetworkBlockingChannelImpl(String name) {
		this.deque = new LinkedBlockingDeque<Integer>();
		this.name = name;
	}

	@Override
	public void putFirst(Integer value) throws InterruptedException {
		this.deque.putFirst(value);
	}

	@Override
	public Integer takeFirst() throws InterruptedException {
		return this.deque.takeFirst();
	}

	@Override
	public void putLast(Integer value) throws InterruptedException {
		this.deque.putLast(value);
	}

	@Override
	public Integer takeLast() throws InterruptedException {
		return this.deque.takeLast();
	}
	
	public String toString() {
		return this.name;
	}
}