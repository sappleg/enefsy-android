package com.enefsy.main;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

@TargetApi(10)
public class DatabaseActivity extends Activity {
	/* Hashmap to contain all venue specific data.
	   The default values are stored for Dublin, CA Starbucks */
	private Map<String, String> venueDataMap;
	
    /* NFC Adapter to pull UID message from tag */
	private NfcAdapter mNfcAdapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        
        /* Check for available NFC Adapter */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        /* Declare a Hashmap to hold venue specific data */
		venueDataMap = new HashMap<String, String>();
		
        /* Pre-populate Map containing venue specific data */
		venueDataMap.put("uid", "");
        venueDataMap.put("name", "");
        venueDataMap.put("address", "");
        venueDataMap.put("latitude", "");
        venueDataMap.put("longitude", "");
        venueDataMap.put("facebookid", "");
        venueDataMap.put("twitterhandle", "");
        venueDataMap.put("foursquareid", "");
        venueDataMap.put("googleid", "");
        venueDataMap.put("yelpid", "");
	    
        /* Get current intent object */
		Intent intent = getIntent();
		
		/* Create boolean object to represent a QR code read */
		boolean qr_read = intent.getBooleanExtra("qr_read", false);
	    
		/* See if application was started from an NFC tag */
	    if (intent.getType() != null && intent.getType().equals("application/vnd.enefsy.main") && mNfcAdapter != null && qr_read == false) {
	     	Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	        NdefMessage msg = (NdefMessage) rawMsgs[0];
	        NdefRecord uidRecord = msg.getRecords()[0];
	        venueDataMap.put("uid", new String(uidRecord.getPayload()));
	        new GetVenueDataTask().execute();
	    } else if (qr_read == true) {
	    	venueDataMap.put("uid", intent.getStringExtra("uid"));
	        new GetVenueDataTask().execute();
	    } else {
	    	Intent newIntent = new Intent(getApplicationContext(), Main.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    	newIntent.putExtra("queried", false);
			startActivity(newIntent);
	    }
	}
	
	/* Private inner class to handle the activity of querying the enefsy database */
	private class GetVenueDataTask extends AsyncTask<Void, Void, Void> {
		
		/* Method to execute prior to AsyncTask execution */
		protected void onPreExecute() {
			
		}
		
		protected void onPostExecute(Void unused) {
			Intent intent = new Intent(getApplicationContext(), Main.class);
			intent.putExtra("queried", true);
			intent.putExtra("uid", getVenueDataMapValue("uid"));
			intent.putExtra("name", getVenueDataMapValue("name"));
			intent.putExtra("address", getVenueDataMapValue("address"));
			intent.putExtra("latitude", getVenueDataMapValue("latitude"));
			intent.putExtra("longitude", getVenueDataMapValue("longitude"));
			intent.putExtra("facebookid", getVenueDataMapValue("facebookid"));
			intent.putExtra("twitterhandle", getVenueDataMapValue("twitterhandle"));
			intent.putExtra("foursquareid", getVenueDataMapValue("foursquareid"));
			intent.putExtra("googleid", getVenueDataMapValue("googleid"));
			intent.putExtra("yelpid", getVenueDataMapValue("yelpid"));
			startActivity(intent);
			
			finish();
		}
		
		@Override
		protected Void doInBackground(Void... unused) {

    		/* UID data to send */
    		final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    		nameValuePairs.add(new BasicNameValuePair("id",venueDataMap.get("uid")));
    		nameValuePairs.add(new BasicNameValuePair("username","enefsy"));
    		nameValuePairs.add(new BasicNameValuePair("password","GTgolfer1990"));
    		String venueData = "";
    		InputStream is = null;
		    	
			/* HTTP post */
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://enefsy.com/getVenues.php");
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
			}catch(Exception e){
				Log.e("log_tag", "Error in http connection " + e.toString());
			}
			
			/* convert response to string */
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
    	               sb.append(line + "\n");
				}
				is.close();
    	 
				venueData = sb.toString();
			}catch(Exception e){
				Log.e("log_tag", "Error converting result " + e.toString());
			}
    	 
			/* parse JSON data */
			try{
				JSONArray jArray = new JSONArray(venueData);
				setVenueData(jArray);
			} catch(JSONException e){
				Log.e("log_tag", "Error parsing data " + e.toString());
			}
			
			return null;
		}
		
		protected void setVenueData(JSONArray jArray) {
			try {
				JSONObject json_data = jArray.getJSONObject(0);
				venueDataMap.put("name", json_data.getString("name"));
				venueDataMap.put("address", json_data.getString("address"));
				venueDataMap.put("latitude", json_data.getString("latitude"));
				venueDataMap.put("longitude", json_data.getString("longitude"));
				venueDataMap.put("facebookid", json_data.getString("facebookid"));
				venueDataMap.put("twitterhandle", json_data.getString("twitterhandle"));
				venueDataMap.put("foursquareid", json_data.getString("foursquareid"));
				venueDataMap.put("googleid", json_data.getString("googleid"));
				venueDataMap.put("yelpid", json_data.getString("yelpid"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getVenueDataMapValue(String key) {
		return venueDataMap.get(key);
	}
}