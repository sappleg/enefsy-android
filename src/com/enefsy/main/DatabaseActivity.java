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
import android.widget.Toast;

@TargetApi(10)
public class DatabaseActivity extends Activity {
	/* Hashmap to contain all venue specific data.
	   The default values are stored for Dublin, CA Starbucks */
	private Map<String, String> venueDataMap;
	
	/* Object representing the AsyncTask itself */
	private GetVenueDataTask mGetVenueDataTask;
	
	/* String for UID */
	private String uid;
	
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
		
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available, using Meadowbrook data", Toast.LENGTH_LONG).show();
//            finish();
//            return;
            
            /* Construct Database Activity */
            uid = "11111111111111111111";
        } else {
	        /* See if application was started from an NFC tag */
	        Intent intent = getIntent();
	        if(intent.getType() != null && intent.getType().equals("application/vnd.enefsy.main")) {
	        	Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	            NdefMessage msg = (NdefMessage) rawMsgs[0];
	            NdefRecord uidRecord = msg.getRecords()[0];
	            uid = new String(uidRecord.getPayload());
	        } else {
	            uid = "11111111111111111111";
	        }
        }

        /* Pre-populate Map containing venue specific data */
        venueDataMap.put("id", uid);
        venueDataMap.put("name", "");
        venueDataMap.put("address", "");
        venueDataMap.put("latitude", "");
        venueDataMap.put("longitude", "");
        venueDataMap.put("facebookid", "");
        venueDataMap.put("twitterhandle", "");
        venueDataMap.put("foursquareid", "");
        venueDataMap.put("googleid", "");
        venueDataMap.put("yelpid", "");

        mGetVenueDataTask = new GetVenueDataTask();
        mGetVenueDataTask.execute();
	}
	
	/* Private inner class to handle the activity of querying the enefsy database */
	private class GetVenueDataTask extends AsyncTask<Void, Void, Void> {
		
		/* Method to execute prior to AsyncTask execution */
		protected void onPreExecute() {	}
		
		protected void onPostExecute(Void unused) {
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Intent intent = new Intent(getApplicationContext(), Main.class);
			intent.putExtra("name", getVenueDataMapValue("name"));
			startActivity(intent);
		}
		
		@Override
		protected Void doInBackground(Void... unused) {

    		/* UID data to send */
    		final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    		nameValuePairs.add(new BasicNameValuePair("id",venueDataMap.get("id")));
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
		String value = venueDataMap.get(key);
		return value;
	}
}