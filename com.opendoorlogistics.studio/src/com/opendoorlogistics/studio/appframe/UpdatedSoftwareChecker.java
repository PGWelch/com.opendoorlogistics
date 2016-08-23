package com.opendoorlogistics.studio.appframe;

import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.utils.Version;
import com.opendoorlogistics.core.utils.ui.DisappearingPopup;

public class UpdatedSoftwareChecker extends Thread {
	private static final long DELAY_MILLIS_BETWEEN_CHECKS = minutesToMillis(60);
	private static final long MILLIS_BETWEEN_SHOWING_POPUP = minutesToMillis(60 * 24);
	private final Random random = new Random();
	private final JFrame parentFrame;
	private long lastTimeMessageShown = -1;

	public UpdatedSoftwareChecker(JFrame parentFrame) {
		super("CheckForUpdatedSoftwareThread");
		this.parentFrame = parentFrame;
	}

	private static long minutesToMillis(long minutes) {
		return minutes * 60 * 1000;
	}

	@Override
	public void run() {
	
		// sleep for 10 seconds before starting to give appframe a chance to show properly
		uncheckedSleep(10000);
		
		while (true) {
			// check...
			runQuery();

			// randomise how long we sleep for to spread out requests..
			long millisSleep = Math.round(DELAY_MILLIS_BETWEEN_CHECKS * (0.5 + random.nextDouble()));
			uncheckedSleep(millisSleep);
		}
	}

	private void uncheckedSleep(long millisSleep) {
		try {
			sleep(millisSleep);
		} catch (InterruptedException e) {

		}
	}

	private void runQuery() {

		// TO include things like current version number running and OS, in case we want to make the update notifier smarter in the future...
		//String osDescription = Strings.std(System.getProperty("os.arch")) + "_" +Strings.std(System.getProperty("os.name")) +Strings.std(System.getProperty("os.version"));
	
		String uri = "http://www.opendoorlogistics.com/wp-content/uploads/Releases/softwareupdatecheck.txt?myVersion=" + AppConstants.getAppVersion().toString();

		class Connected {
			boolean ok = false;
		}
		Connected connected = new Connected();

		// try 3 times
		for (int i = 0; i < 3 && !connected.ok; i++) {
			CloseableHttpClient httpclient = HttpClients.createDefault();

			try {

				HttpGet httpget = new HttpGet(uri);
				httpget.setHeader("User-Agent", "OpenDoorLogistics Studio, updated software checker");

				// create response handler
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

					public String handleResponse(final HttpResponse response)
							throws ClientProtocolException, IOException {
						int status = response.getStatusLine().getStatusCode();
						if (status >= 200 && status < 300) {

							HttpEntity entity = response.getEntity();
							String s = EntityUtils.toString(entity);
							ObjectMapper mapper = new ObjectMapper();
							JsonNode rootNode = mapper.readValue(s, JsonNode.class);
							JsonNode latestVersion = rootNode.get("latest");
							
							// we can stop trying now!!!
							connected.ok = true;
							if (latestVersion != null) {
								String val = latestVersion.asText();
								if (val != null) {
									Version version = new Version(val);
									onReadVersion(version);
								}
							}
							return null;
						} else {
							throw new RuntimeException();
						}
					}
				};

				httpclient.execute(httpget, responseHandler);


			} catch (Throwable e) {

			} finally {

				try {
					httpclient.close();
				} catch (IOException e) {
					// throw new RuntimeException(e);
				}

			}

			// give small break before retrying or finishing...
			uncheckedSleep(5000);

		}
	}

	void onReadVersion(Version version) {

		if (version.compareTo(AppConstants.getAppVersion()) > 0) {
			long timeMillis = System.currentTimeMillis();
			if (lastTimeMessageShown < 0 || ((lastTimeMessageShown + MILLIS_BETWEEN_SHOWING_POPUP) < timeMillis)) {
				lastTimeMessageShown = timeMillis;
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						StringBuilder builder = new StringBuilder();
						builder.append(
								"A new version of ODL Studio is now available to download from www.opendoorlogistics.com");
						DisappearingPopup.show(parentFrame, builder.toString(), "New release", 5000);
					}
				});
			}
		}
	}
}
