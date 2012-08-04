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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class DatabaseActivity extends Activity {
	//Progress dialog for retrieving data from enefsy database
//	private ProgressDialog mProgress;
	
	/* Hashmap to contain all venue specific data.
	   The default values are stored for Dublin, CA Starbucks */
	private Map<String, String> venueDataMap;
	
	/* Object representing the AsyncTask itself */
	private GetVenueDataTask mGetVenueDataTask;
	
	/* String for UID */
	private String uid;
	
	/* Text View to hold name of venue */
	//private TextView mTextView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        
        /* Temp hardcode of uid */
        uid = "11111111111111111111";
		/* Set content view */
//		setContentView(R.layout.main);
		
//		mProgress = new ProgressDialog(this);
		venueDataMap = new HashMap<String, String>();
		
        /* Declare textview to show venue name */
//        mTextView = (TextView)findViewById(R.id.uid_view);

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
	
	//Private inner class to handle the activity of querying the enefsy database
	private class GetVenueDataTask extends AsyncTask<Void, Void, Void> {
		
		protected void onPreExecute() {
//			mProgress.setMessage("Connecting to Enefsy ...");
//			mProgress.show();
		}
		
		protected void onPostExecute(Void unused) {
//			mProgress.dismiss();
			
//			setTextView(getVenueDataMapValue("name"));
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Intent intent = new Intent(getApplicationContext(), Main.class);
			startActivity(intent);
		}
		
		@Override
		protected Void doInBackground(Void... unused) {

    		//the uid data to send
    		final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    		nameValuePairs.add(new BasicNameValuePair("id",venueDataMap.get("id")));
    		String venueData = "";
    		InputStream is = null;
		    	
			//http post
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
			
			//convert response to string
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
    	 
			//parse JSON data
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
	
//	public void setTextView(String s) {
//		Main.this.setTextView(getVenueDataMapValue("name"));
//	}
}