package jonas.oshlt;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.View.*;
import android.view.*;
import com.squareup.okhttp.*;
import java.io.*;
import org.jsoup.nodes.*;
import org.jsoup.*;

public class MainActivity extends Activity {
	Button buttonLogin;
	TextView textStatus;

	final OkHttpClient okClient = new OkHttpClient();

	final String hotspotTrialUrl = "http://wifi.onsurfers.com/wifilogin/trialGen.php";
	final String hotspotpostUrl = "http://wifi.onsurfers.com/wifilogin/hotspotpost.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		buttonLogin = (Button) findViewById(R.id.activity_main_loginButton);
		textStatus = (TextView) findViewById(R.id.activity_main_statusText);

		setTitle(R.string.app_name);

		buttonLogin.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View clicked) {
					buttonLogin.setText("Logging in to WiFi");
					buttonLogin.setEnabled(false);

					textStatus.setText("Please wait...");
					
					new WifiLoginTask().execute();
				}
			});
    }

	private class WifiLoginTask extends AsyncTask<Void, String, String> {
		@Override
		protected String doInBackground(Void[] p1) {
			try {
				final String exampleComUrl = "http://example.com";
				String dataParameter;
				String macAdress;
				
				publishProgress("Connecting... (1/5)");

				//first request gives us a form with some data, which we have to send back to get our key
				Request request = new Request.Builder()
					.url(exampleComUrl)
					.build();
				Response response = okClient.newCall(request).execute();
		
				if (response.request().httpUrl().toString().equals(exampleComUrl)) {
					publishProgress("Already connected");
					return null;
				}
				
				//parse the page and extract form data data
				Document doc = Jsoup.parse(response.body().string());
				FormEncodingBuilder requestBodyBuilder = new FormEncodingBuilder();
				
				for (Element e: doc.select("form > input[type=hidden]")) {
					requestBodyBuilder.add(e.attr("name"), e.attr("value"));
				}
				
				publishProgress("Connecting... (2/5)");
				
				//send it back to get our unique data parameter key
				request = new Request.Builder()
					.url(hotspotpostUrl)
					.post(requestBodyBuilder.build())
					.build();
				response = okClient.newCall(request).execute();
				
				dataParameter = response.request().httpUrl().queryParameter("data");
				
				//next, we need the device mac address
				macAdress = "Your mac here"; //FIXME
				
				//next, put together the url we need to hit, which gives us yet another html form, this time with actual login details
				HttpUrl url = HttpUrl.parse(hotspotTrialUrl).newBuilder()
					.addQueryParameter("data", dataParameter)
					.addQueryParameter("mac", macAdress)
					.build();
				
				//send the request
				publishProgress("Connecting... (3/5)");
				request = new Request.Builder()
					.url(url)
					.build();
				response = okClient.newCall(request).execute();
				
				//get login form
				doc = Jsoup.parse(response.body().string());
				
				requestBodyBuilder = new FormEncodingBuilder();

				for (Element e: doc.select("form > input[type=hidden]")) {
					requestBodyBuilder.add(e.attr("name"), e.attr("value"));
				}
				
				//submit login form
				publishProgress("Connecting... (4/5)");
				request = new Request.Builder()
					.url(doc.select("form#loginStore").attr("action"))
					.post(requestBodyBuilder.build())
					.build();
				response = okClient.newCall(request).execute();
				
				String responseString = response.body().string();
				if (responseString.contains("Usted ya ha descargado gratuitamente")) {
					publishProgress("10 minutes already used this hour, try again later");
				} else {
					//first request gives us a form with some data, which we have to send back to get our key
					publishProgress("Verifying connection... (5/5)");
					request = new Request.Builder()
						.url(exampleComUrl)
						.build();
					response = okClient.newCall(request).execute();

					if (response.request().httpUrl().toString().equals(exampleComUrl) && response.isSuccessful()) {
						publishProgress("Successfully Connected");
						//we're connected :-)
						return null;
					}
					
				}
				
			} catch (IOException e) {
				publishProgress("Error: " + e.getMessage());
			}
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String[] progress) {
			textStatus.setText(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			buttonLogin.setEnabled(true);
			buttonLogin.setText("Login to WiFi");
		}
	}
}
