package alouw.csc552.hamming;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * A factory for producing a worker node. Worker nodes apply the function argument to all values read from an 
 * input channel and write the result to an output channel.
 */
public class HammingNetworkWorkerNodeFactory {

	public HammingNetworkNode getInstance(
			final Procedure function, String name, 
			final long duration, final TimeUnit uom) {
		return new MultiplicationNodeImpl(function, name, duration, uom);
	}
}

class MultiplicationNodeImpl implements HammingNetworkNode {

	private final int NUM_INPUT_CHANNELS = 1;
	private final int NUM_OUTPUT_CHANNELS = 1;
	
	private final long runtime_max_duration;
	private final TimeUnit runtime_uom;
	
	private final List<HammingNetworkChannel> inputChannels = new ArrayList<>(NUM_INPUT_CHANNELS);
	private final List<HammingNetworkChannel> outputChannels = new ArrayList<>(NUM_OUTPUT_CHANNELS);

	private final ExecutorService workerPool = Executors.newSingleThreadExecutor(
			new ThreadFactoryWithNamePrefix("MultiplicationNodeImpl_" + this.name + "_Worker"));

	private final Procedure function;
	private final String name;
	
	MultiplicationNodeImpl(final Procedure function, String name,
			final long duration, final TimeUnit uom) {
		this.function = function;
		this.name = name;
		this.runtime_max_duration =  duration;
		this.runtime_uom = uom;
	}
	
	@Override
	public void start() {
		
		inputChannels.stream().forEach(i -> {
			workerPool.execute(new Runnable() {
				public void run() {
					for (;;) {
						try {
							final Integer value = function.apply(i.takeFirst());
							outputChannels.stream().forEach(o -> {
								try {
									o.putLast(value);
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									return;
								}
							});
						} catch (InterruptedException e1) {
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
			e.printStackTrace();
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