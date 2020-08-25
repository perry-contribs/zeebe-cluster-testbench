package io.zeebe.clustertestbench.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.impl.ZeebeObjectMapper;
import io.zeebe.clustertestbench.testdriver.api.TestDriver;
import io.zeebe.clustertestbench.testdriver.api.TestReport;
import io.zeebe.clustertestbench.testdriver.api.TestReport.TestResult;
import io.zeebe.clustertestbench.testdriver.impl.TestReportDTO;

public class RecordTestResultWorker implements JobHandler {
	
	private static final DateTimeFormatter INSTANT_FORMATTER =
		    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
		                     .withLocale( Locale.US )
		                     .withZone( ZoneId.systemDefault() );

	private static final String APPLICATION_NAME = "Zeebe Cluster Testbench - Publish Test Results Worker";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "secrets";
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final String CREDENTIALS_FILE_PATH = "secrets/credentials.json";

	private static final String RANGE = "Sheet1!A1:J";

	private final String spreadSheetId;

	public RecordTestResultWorker(String spreadSheetId) {
		super();
		this.spreadSheetId = spreadSheetId;
	}

	@Override
	public void handle(JobClient client, ActivatedJob job) throws Exception {
		final Input input = job.getVariablesAsType(Input.class);

		List<Object> rowData = buildRowDataForSheet(input);

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();

		ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));
		service.spreadsheets().values().append(spreadSheetId, RANGE, body).setValueInputOption("USER_ENTERED")
				.execute();

		client.newCompleteCommand(job.getKey()).send();
	}

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

		// TODO replace the user based authorization with a service based authorization
		// https://developers.google.com/identity/protocols/oauth2/service-account
		// User based authorizations have two flaws: they will spawn up a browser where
		// the user can authenticate; they will expire after some time. Both are not
		// ideal for machine to machine processes
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	private static List<Object> buildRowDataForSheet(Input input) {
		List<Object> result = new ArrayList<>();

		result.add(input.getDockerImage());
		result.add(input.getClusterPlan());
		result.add(input.getClusterId());

		TestReport testReport = input.getTestReport();
		result.add(testReport.getTestResult().name());
		result.add(testReport.getFailureCount());
		result.add(returnValueIfApplicable(testReport, new ZeebeObjectMapper().toJson(testReport.getFailureMessages())));
		result.add(convertMillisToString(testReport.getStartTime()));
		result.add(returnValueIfApplicable(testReport, convertMillisToString(testReport.getTimeOfFirstFailure())));
		result.add(convertMillisToString(testReport.getEndTime()));
		result.add(new ZeebeObjectMapper().toJson(testReport.getMetaData()));

		return result;
	}
	
	private static String returnValueIfApplicable(TestReport testReport, Object value) {		
		return testReport.getTestResult() == TestResult.PASSED ? "n/a" : value.toString();		
	}

	private static String convertMillisToString(long millis) {
		Instant instant = Instant.ofEpochMilli(millis);

		return INSTANT_FORMATTER.format(instant);
	}

	private static final class Input {
		private String dockerImage;
		private String clusterPlan;
		private String clusterId;

		private TestReportDTO testReport;

		public String getDockerImage() {
			return dockerImage;
		}

		public void setDockerImage(String dockerImage) {
			this.dockerImage = dockerImage;
		}

		public String getClusterPlan() {
			return clusterPlan;
		}

		public void setClusterPlan(String clusterPlan) {
			this.clusterPlan = clusterPlan;
		}

		public String getClusterId() {
			return clusterId;
		}

		public void setClusterId(String clusterId) {
			this.clusterId = clusterId;
		}

		@JsonProperty(TestDriver.VARIABLE_KEY_TEST_REPORT)
		public TestReportDTO getTestReport() {
			return testReport;
		}

		@JsonProperty(TestDriver.VARIABLE_KEY_TEST_REPORT)
		public void setTestReport(TestReportDTO testReport) {
			this.testReport = testReport;
		}

	}
}
