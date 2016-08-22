package alouw.csc552.hamming;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ajeffrey.teaching.debug.Debug;

/*
 * An enum factory for the different types of control nodes in a Hamming Network. A control node
 * is optionally capable of shutting down the entire network once the desired solution is computed.
 * 
 * Control nodes can be started/terminated independently of one another.
 * 
 */
public enum HammingNetworkControlNodeFactory {
			
	MERGE_NODE {
		public HammingNetworkNode getInstance(
				final int maxNumbers, HammingNetworkShutdownSignal signal,
				final long duration, final TimeUnit uom) {
			return new ThreeInOrderMergeImpl(maxNumbers, signal, duration, uom);
		}
	},
		
	COLLECT_NODE {
		public HammingNetworkNode getInstance(
				final int maxNumbers, HammingNetworkShutdownSignal signal,
				final long duration, final TimeUnit uom) {
			return new FourOutCopyImpl(maxNumbers, signal, duration, uom);
		}
	},
		
	PRINT_NODE {
		public HammingNetworkNode getInstance(
				final int maxNumbers, HammingNetworkShutdownSignal signal,
				final long duration, final TimeUnit uom) {
			return new PrintNodeImpl(maxNumbers, signal, duration, uom);
		};
	};
	
	public abstract HammingNetworkNode getInstance(
			final int maxNumbers, HammingNetworkShutdownSignal signal,
			final long duration, final TimeUnit uom);
	
	final static long TEARDOWN_TIME_MAX_DURATION = 1;
	final static TimeUnit TEARDOWN_TIME_UOM = TimeUnit.MINUTES;
}

class FourOutCopyImpl implements HammingNetworkNode {
	
	private final int NUM_INPUT_CHANNELS = 1;
	private final int NUM_OUTPUT_CHANNELS = 4;
	
	private final Integer maxNumbers;
	private final HammingNetworkShutdownSignal signal;
	
	private final long runtime_max_duration;
	private final TimeUnit runtime_uom;
	
	private final List<HammingNetworkChannel> inputChannels = new ArrayList<>(NUM_INPUT_CHANNELS);
	private final List<HammingNetworkChannel> outputChannels = new ArrayList<>(NUM_OUTPUT_CHANNELS);

	private final ExecutorService workerPool = Executors.newSingleThreadExecutor(
			new ThreadFactoryWithNamePrefix("FourOutCopyImpl_Worker"));
	
	FourOutCopyImpl (final int maxNumbers, HammingNetworkShutdownSignal signal,
			final long duration, final TimeUnit uom) {
		this.maxNumbers = maxNumbers;
		this.signal =  signal;
		this.runtime_max_duration =  duration;
		this.runtime_uom = uom;
	}
	
	@Override
	public void start() {		
		inputChannels.stream().forEach(i -> {
			
			try {
				i.putLast(Integer.valueOf(1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			
			workerPool.execute(new Runnable() {
				public void run() {
					String me = Thread.currentThread().getName();
					
					if (Main.DEBUG) Debug.out.breakPoint(me + " starting");
					
					for (;;) {
						try {
							if (Main.DEBUG) Debug.out.breakPoint(me + " taking.....");
							final Integer value = i.takeFirst();
							if (Main.DEBUG) Debug.out.breakPoint(me + " took " + value + " from " + i.toString());

							outputChannels.stream().forEach(o -> {
								try {
									o.putLast(value);
								} catch (Exception e) {
									Thread.currentThread().interrupt();
									return;
								}
							});
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				}
			});
		});
		
		try {
			workerPool.awaitTermination(this.runtime_max_duration, this.runtime_uom);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	@Override
	public void shutdown() throws InterruptedException {
		this.workerPool.shutdownNow();
		this.workerPool.awaitTermination(HammingNetworkControlNodeFactory.TEARDOWN_TIME_MAX_DURATION,
				HammingNetworkControlNodeFactory.TEARDOWN_TIME_UOM);
	}

	@Override
	public synchronized void addInputChannel(HammingNetworkChannel input) {
		if (this.inputChannels.size() == NUM_INPUT_CHANNELS) throw new IllegalArgumentException("Max of one input channels");
		inputChannels.add(input);
	}

	@Override
	public synchronized void addOutputChannel(HammingNetworkChannel output) {
		if (this.outputChannels.size() == NUM_OUTPUT_CHANNELS) throw new IllegalArgumentException("Max of four output channels");
		outputChannels.add(output);
	}

	@Override
	public void run() {
		start();
	}
}

class ThreeInOrderMergeImpl implements HammingNetworkNode {
	
	private final int NUM_INPUT_CHANNELS = 3;
	private final int NUM_OUTPUT_CHANNELS = 1;
	
	private final Integer maxNumbers;
	private final HammingNetworkShutdownSignal signal;

	private final long runtime_max_duration;
	private final TimeUnit runtime_uom;
	
	private final List<HammingNetworkChannel> inputChannels = new ArrayList<>(NUM_INPUT_CHANNELS);
	private final List<HammingNetworkChannel> outputChannels = new ArrayList<>(NUM_OUTPUT_CHANNELS);

	private final ExecutorService workerPool = Executors.newFixedThreadPool(NUM_INPUT_CHANNELS, 
			new ThreadFactoryWithNamePrefix("ThreeInOrderMerge_Worker"));
	
	private final ConcurrentLinkedQueue<Integer> minValuesRead = new ConcurrentLinkedQueue<>();
	private final CyclicBarrier mergeBarrier = new CyclicBarrier(NUM_INPUT_CHANNELS, new MergeTask());
	
	ThreeInOrderMergeImpl(final int maxNumbers, HammingNetworkShutdownSignal signal,
			final long duration, final TimeUnit uom) {
		this.maxNumbers = maxNumbers;
		this.signal =  signal;
		this.runtime_max_duration =  duration;
		this.runtime_uom = uom;
	}
	
	@Override
	public void start() {
		inputChannels.stream().forEach(i ->  {
			workerPool.execute(new Runnable() {
				public void run() {
					String me = Thread.currentThread().getName();
					if (Main.DEBUG) Debug.out.breakPoint(me + " starting");
					
					for (;;) { 
									
						try {
							final Integer value = i.takeFirst();
							minValuesRead.add(value);
							i.putFirst(value);
							if (Main.DEBUG) Debug.out.breakPoint(me + " waiting with value " + value + " from " + i.toString());
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
						
						try {
							mergeBarrier.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						} catch (BrokenBarrierException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				}
			});
		});
		
		try {
			workerPool.awaitTermination(this.runtime_max_duration, this.runtime_uom);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	@Override
	public void shutdown() throws InterruptedException {
		this.workerPool.shutdownNow();
		this.workerPool.awaitTermination(HammingNetworkControlNodeFactory.TEARDOWN_TIME_MAX_DURATION,
				HammingNetworkControlNodeFactory.TEARDOWN_TIME_UOM);
	}

	@Override
	public void addInputChannel(HammingNetworkChannel input) {
		if (this.inputChannels.size() == NUM_INPUT_CHANNELS) throw new IllegalArgumentException("Max of three input channels");
		inputChannels.add(input);
	}

	@Override
	public void addOutputChannel(HammingNetworkChannel output) {
		if (this.outputChannels.size() == NUM_OUTPUT_CHANNELS) throw new IllegalArgumentException("Max of one output channels");
		outputChannels.add(output);	
	}
	
	class MergeTask implements Runnable {

		@Override
		public void run() {
			
			String me = "MERGE_TASK_" + Thread.currentThread().getName();
			
			// step 1: determine the minimum value on offer and remove that value from all source channels
			final Integer minValueRead = minValuesRead.stream().min(Integer::compareTo).get();
					
			if (Main.DEBUG) Debug.out.breakPoint(me + " CONSIDERS  "+ minValueRead + " the MINIMUM");
			
			// step 2: remove the head of any input queue equals to minValueRead
			for (HammingNetworkChannel channel : inputChannels) {
				try {
					final Integer readValue = channel.takeFirst();
					if (readValue.compareTo(minValueRead) > 0) channel.putFirst(readValue);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
						
			//step 3: write to output channels
			outputChannels.stream().forEach(o -> {
				try {
					o.putLast(minValueRead);
				} catch (Exception e) {
					Thread.currentThread().interrupt();
					return;
				}
			});
			
			//step 4: cleaup
			minValuesRead.clear();
			
			//step 5: reset the barrier
		}
	}
	
	@Override
	public void run() {
		start();
	}
}

class PrintNodeImpl implements HammingNetworkNode {
	
	private final int NUM_INPUT_CHANNELS = 1;
	
	private final Integer maxNumbers;
	private final AtomicInteger countNumbers = new AtomicInteger();
	private final HammingNetworkShutdownSignal signal;
	
	private final long runtime_max_duration;
	private final TimeUnit runtime_uom;
	
	private final List<HammingNetworkChannel> inputChannels = new ArrayList<>(NUM_INPUT_CHANNELS);

	private final ExecutorService workerPool = Executors.newSingleThreadExecutor(
			new ThreadFactoryWithNamePrefix("PrintNodeImpl_Worker"));
	
	PrintNodeImpl(final int maxNumbers, HammingNetworkShutdownSignal signal,
			final long duration, final TimeUnit uom) {
		this.maxNumbers = maxNumbers;
		this.signal =  signal;
		this.runtime_max_duration =  duration;
		this.runtime_uom = uom;
		this.countNumbers.set(0);
	}
	
	@Override
	public void start() {
		
		inputChannels.stream().forEach(i -> {
			workerPool.execute(new Runnable() {
				public void run() {
					String me = Thread.currentThread().getName();
					
					for (;;) {
						try {
							
							if (Main.DEBUG) Debug.out.breakPoint(me + " taking from "+ i.toString());
							
							final Integer value = i.takeFirst();
							
							if (Main.DEBUG) Debug.out.breakPoint(me + " took " + value + " from "+ i.toString());
							
							if (Integer.valueOf(countNumbers.incrementAndGet()).compareTo(maxNumbers) > 0) {
								signal.sendShutdown();
								return;
							} else {
								System.out.println(value);
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				}
			});
		});
		
		try {
			workerPool.awaitTermination(this.runtime_max_duration, this.runtime_uom);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	@Override
	public void shutdown() throws InterruptedException {
		this.workerPool.shutdownNow();
		this.workerPool.awaitTermination(HammingNetworkControlNodeFactory.TEARDOWN_TIME_MAX_DURATION,
				HammingNetworkControlNodeFactory.TEARDOWN_TIME_UOM);
	}

	@Override
	public synchronized void addInputChannel(HammingNetworkChannel input) {
		if (this.inputChannels.size() == NUM_INPUT_CHANNELS) throw new IllegalArgumentException("Max of one input channels");
		inputChannels.add(input);
	}

	@Override
	public synchronized void addOutputChannel(HammingNetworkChannel output) {
		throw new UnsupportedOperationException("A print node does not have output channels");
	}
	
	@Override
	public void run() {
		start();
	}
}