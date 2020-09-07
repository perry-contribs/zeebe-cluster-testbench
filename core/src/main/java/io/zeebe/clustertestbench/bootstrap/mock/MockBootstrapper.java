package io.zeebe.clustertestbench.bootstrap.mock;

import static java.util.Objects.requireNonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.api.worker.JobWorker;

/**
 * This class registers mock workers for job types where the workers are not
 * implemented yet
 * 
 */
public class MockBootstrapper {

	private static final Logger logger = LoggerFactory.getLogger(MockBootstrapper.class);

	private final List<String> jobsToMock;

	private final ZeebeClient client;

	private final Map<String, JobWorker> registeredJobWorkers = new HashMap<>();

	public MockBootstrapper(ZeebeClient client, List<String> jobsToMock) {
		this.client = requireNonNull(client);
		this.jobsToMock = requireNonNull(jobsToMock);
	}

	public void registerMockWorkers() throws FileNotFoundException, IOException {
		jobsToMock.forEach(jobType -> registerMockWorker(jobType, new MoveAlongJobHandler()));
		
	//	registerMockWorker("create-zeebe-cluster-in-camunda-cloud-job", new PreexistingClusterConnector());
	}

	private void registerMockWorker(String jobType, JobHandler jobHandler) {
		logger.info(
				"Registering mock job worker " + jobHandler.getClass().getSimpleName() + " for: " + jobType);

		final JobWorker workerRegistration = client.newWorker().jobType(jobType).handler(jobHandler)
				.timeout(Duration.ofSeconds(10)).open();

		registeredJobWorkers.put(jobType, workerRegistration);

		logger.info( "Job worker opened and receiving jobs.");
	}

	public void stop() {
		registeredJobWorkers.values().forEach(JobWorker::close);
	}

	private static class MoveAlongJobHandler implements JobHandler {
		@Override
		public void handle(final JobClient client, final ActivatedJob job) {
			logger.info( job.toString());
			client.newCompleteCommand(job.getKey()).send().join();
		}
	}
}
