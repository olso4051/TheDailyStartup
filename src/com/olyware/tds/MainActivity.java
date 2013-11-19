package com.olyware.tds;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdView;

public class MainActivity extends Activity {

	private ProgressBar progress;
	private EditText inputText;
	private Button newIdea, exists, improve, submit;
	private TextView idea;// , founder;
	private ListView improveList, existsList;
	private AdView botBanner;
	private ArrayList<String> improveItems, existsItems;
	private ArrayAdapter<String> improveAdapter, existsAdapter;
	private String userName, ideaText;
	private long ideaID = -1;
	private boolean error = false;
	private DefaultHttpClient httpclient;
	private HttpPost httppost;
	private HttpGet httpget;
	private ArrayList<NameValuePair> nameValuePairs;
	private HttpResponse response;
	private SharedPreferences sharedPrefs;
	private Context ctx;

	private class PushIt extends AsyncTask<String, Integer, Integer> {
		@Override
		protected Integer doInBackground(String... type) {
			String action;
			if (type[0].equals(getString(R.string.submit_idea)))
				action = "idea";
			else if (type[0].equals(getString(R.string.submit_exists)))
				action = "exists";
			else if (type[0].equals(getString(R.string.submit_improve)))
				action = "expand";
			else
				action = "idea";
			publishProgress(0);
			httpclient = new DefaultHttpClient();
			httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			httppost = new HttpPost("http://thedailystartup.net/bin/process.php");
			try {
				nameValuePairs = new ArrayList<NameValuePair>(4);
				nameValuePairs.add(new BasicNameValuePair("t", inputText.getText().toString()));
				nameValuePairs.add(new BasicNameValuePair("a", action));
				nameValuePairs.add(new BasicNameValuePair("u", userName));
				nameValuePairs.add(new BasicNameValuePair("i", String.valueOf(ideaID)));
				publishProgress(1);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				publishProgress(2);
				response = httpclient.execute(httppost);
				publishProgress(3);
				return 0;
			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progress.setProgress(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0)
				Toast.makeText(ctx, getString(R.string.success), Toast.LENGTH_SHORT).show();
			else if (result == 1)
				Toast.makeText(ctx, getString(R.string.failed), Toast.LENGTH_SHORT).show();
			progress.setVisibility(View.INVISIBLE);
			submit.setEnabled(true);
			super.onPostExecute(result);
		}
	}

	private class GetIdea extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected Integer doInBackground(Void... voids) {
			httpclient = new DefaultHttpClient();
			httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			try {
				/*nameValuePairs = new ArrayList<NameValuePair>(3);
				nameValuePairs.add(new BasicNameValuePair("t", inputText.getText().toString()));
				nameValuePairs.add(new BasicNameValuePair("a", "unknown"));
				nameValuePairs.add(new BasicNameValuePair("i", String.valueOf(ideaID)));
				String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
				httpget = new HttpGet("http://thedailystartup.net/getidea.php" + "?" + paramString);*/
				httpget = new HttpGet("http://thedailystartup.net/getidea.php");
				response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String fullResult = EntityUtils.toString(entity);
					JSONObject response = new JSONObject(fullResult);
					error = response.getBoolean("error");
					JSONObject result = response.getJSONObject("result");
					JSONObject ideaData = result.getJSONObject("idea_data");
					ideaText = ideaData.getString("text");
					ideaID = ideaData.getLong("idea_id");
					JSONArray existsData = result.getJSONArray("exists_data");
					if (existsData.length() > 0) {
						existsItems.clear();
						for (int i = 0; i < existsData.length(); i++)
							existsItems.add(((JSONObject) existsData.get(i)).getString("text"));
					} else {
						existsItems.clear();
						existsItems.add(getString(R.string.exists_item));
					}
					existsAdapter = new ArrayAdapter<String>(ctx, R.layout.list_text_item, R.id.text, existsItems);
					JSONArray improveData = result.getJSONArray("improve_data");
					if (improveData.length() > 0) {
						improveItems.clear();
						for (int i = 0; i < improveData.length(); i++)
							improveItems.add(((JSONObject) improveData.get(i)).getString("text"));
					} else {
						improveItems.clear();
						improveItems.add(getString(R.string.improve_item));
					}
					improveAdapter = new ArrayAdapter<String>(ctx, R.layout.list_text_item, R.id.text, improveItems);
				}
				return 0;
			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				// success
				exists.setEnabled(true);
				improve.setEnabled(true);
				idea.setText("\"" + ideaText + "\"");
				existsList.setAdapter(existsAdapter);
				improveList.setAdapter(improveAdapter);
			} else if (result == 1) {
				// failed to get idea
				idea.setText(getString(R.string.failed_get));
			}
			super.onPostExecute(result);
		}
	}

	private class MyAdListener implements AdListener {
		@Override
		public void onFailedToReceiveAd(Ad ad, ErrorCode errorCode) {
			Toast.makeText(ctx, getString(R.string.error_receive_ad), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onReceiveAd(Ad ad) {
			// nothing
		}

		@Override
		public void onDismissScreen(Ad arg0) {
			// nothing
		}

		@Override
		public void onLeaveApplication(Ad arg0) {
			// nothing
		}

		@Override
		public void onPresentScreen(Ad arg0) {
			// nothing
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ctx = this;

		new GetIdea().execute();
		idea = (TextView) findViewById(R.id.idea);
		idea.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				idea.setText(getString(R.string.idea));
				new GetIdea().execute();
			}
		});
		progress = (ProgressBar) findViewById(R.id.progressBar1);
		inputText = (EditText) findViewById(R.id.input_text);
		newIdea = (Button) findViewById(R.id.new_idea);
		newIdea.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				inputText.setHint(getString(R.string.idea_hint));
				submit.setText(getString(R.string.submit_idea));
				newIdea.setVisibility(View.INVISIBLE);
			}
		});
		exists = (Button) findViewById(R.id.exists);
		exists.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				inputText.setHint(getString(R.string.exists_hint));
				submit.setText(getString(R.string.submit_exists));
				newIdea.setVisibility(View.VISIBLE);
			}
		});
		improve = (Button) findViewById(R.id.improve);
		improve.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				inputText.setHint(getString(R.string.improve_hint));
				submit.setText(getString(R.string.submit_improve));
				newIdea.setVisibility(View.VISIBLE);
			}
		});
		submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (inputText.getText().toString().length() < 70) {
					submit.setEnabled(false);
					progress.setVisibility(View.VISIBLE);
					new PushIt().execute(submit.getText().toString());
				} else {
					Toast.makeText(ctx, getString(R.string.too_long), Toast.LENGTH_SHORT).show();
				}
			}
		});
		improveList = (ListView) findViewById(R.id.improve_list);
		improveItems = new ArrayList<String>();
		improveItems.add(getString(R.string.improve_item));
		improveAdapter = new ArrayAdapter<String>(this, R.layout.list_text_item, R.id.text, improveItems);
		improveList.setAdapter(improveAdapter);

		existsList = (ListView) findViewById(R.id.exists_list);
		existsItems = new ArrayList<String>();
		existsItems.add(getString(R.string.exists_item));
		existsAdapter = new ArrayAdapter<String>(this, R.layout.list_text_item, R.id.text, existsItems);
		existsList.setAdapter(existsAdapter);

		/*founder = (TextView) findViewById(R.id.founder);
		founder.setTextColor(Color.GRAY);
		founder.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showOlywareMarket();
			}
		});*/

		botBanner = (AdView) findViewById(R.id.ad);
		botBanner.setAdListener(new MyAdListener());

		AdRequest adRequest = new AdRequest();
		// adRequest.addTestDevice(getString(R.string.test_device_id));
		// adRequest.addKeyword("sporting goods");
		botBanner.loadAd(adRequest);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		userName = sharedPrefs.getString("username", getString(R.string.anonymous));
		if (userName.equals(getString(R.string.anonymous)))
			getUsername();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_account_change:
			getUsername();
			return true;
		case R.id.action_more:
			showOlywareMarket();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	public void getUsername() {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = sharedPrefs.edit();

		AccountManager manager = AccountManager.get(this);
		Account[] accounts = manager.getAccountsByType("com.google");
		List<String> possibleEmails = new LinkedList<String>();

		for (Account account : accounts) {
			possibleEmails.add(account.name);
		}
		possibleEmails.add(getString(R.string.anonymous));

		final String emails[] = possibleEmails.toArray(new String[possibleEmails.size()]);
		int selected = possibleEmails.indexOf(userName);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.select_user_name));
		builder.setSingleChoiceItems(emails, selected, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				userName = emails[item];
			}
		});

		builder.setPositiveButton(getString(R.string.select), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				editor.putString("username", userName).commit();
			}
		});
		builder.show();

		/*if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null){
			String email = possibleEmails.get(0); String[] parts = email.split("@");
			if(parts.length > 0 && parts[0] != null)
				return parts[0];
			else
				return getString(R.string.anonymous);
			}
		else
			return getString(R.string.anonymous);*/
	}

	public void showOlywareMarket() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://search?q=pub:Olyware"));
		startActivity(intent);
	}
}
