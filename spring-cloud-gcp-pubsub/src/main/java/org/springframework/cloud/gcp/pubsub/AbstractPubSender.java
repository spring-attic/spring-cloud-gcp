package org.springframework.cloud.gcp.pubsub;

import com.google.auth.Credentials;

import org.springframework.cloud.gcp.pubsub.converters.PubSubHeaderMapper;
import org.springframework.context.Lifecycle;

/**
 * @author Vinicius Carvalho
 */
public abstract class AbstractPubSender implements ReactivePubSubSender, Lifecycle{

	protected final String project;

	protected final Credentials credentials;

	protected volatile boolean running = false;

	protected final String BASE_NAME;

	protected final String BASE_TOPIC_NAME;

	protected PubSubHeaderMapper headerMapper;

	private final String PUBSUB_ENDPOINT = "pubsub.googleapis.com";

	private final int PUBUSUB_PORT = 443;


	public AbstractPubSender(String project, Credentials credentials) {
		this.project = project;
		this.credentials = credentials;
		this.BASE_NAME = String.format("projects/%s/",this.project);
		this.BASE_TOPIC_NAME = BASE_NAME + "topics/";
	}

	@Override
	public void start() {
		this.running = true;
		doStart();
	}

	@Override
	public void stop() {

		doStop();
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	abstract void doStart();

	abstract void doStop();

	public PubSubHeaderMapper getHeaderMapper() {
		return headerMapper;
	}

	public void setHeaderMapper(PubSubHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}
}
