package com.enefsy.main;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class DatabaseActivity extends Activity {
	//Progress dialog for retrieving data from enefsy database
	private ProgressDialog mProgress;
	
	/* Hashmap to contain all venue specific data.
	   The default values are stored for Dublin, CA Starbucks */
	private Map<String, String> venueDataMap;
	
	public DatabaseActivity(String uid) {
		mProgress = new ProgressDialog(this);

        /* Pre-populate Map containing venue specific data */
        venueDataMap.put("id", uid);
        venueDataMap.put("name", "Starbucks");
        venueDataMap.put("address", "4930 Dublin Boulevard, Dublin, CA 94568");
        venueDataMap.put("latitude", "37.704025");
        venueDataMap.put("longitude", "-121.884941");
        venueDataMap.put("facebookid", "233762670072788");
        venueDataMap.put("twitterhandle", "@Starbucks");
        venueDataMap.put("foursquareid", "4ac0508af964a5202e9420e3");
        venueDataMap.put("googleid", "100031254040654670562");
        venueDataMap.put("yelpid", "starbucks-coffee-dublin-2");

		new GetVenueDataTask().execute();
	}
	
	//Private inner class to handle the activity of querying the enefsy database
	private class GetVenueDataTask extends AsyncTask<Uri, Void, Void> {
		
		protected void onPreExecute() {
			mProgress.setMessage("Connecting to Enefsy ...");
			mProgress.show();		
		}
		
		protected void onPostExecute(Void result) {
			mProgress.dismiss();
		}
		
		@Override
		protected Void doInBackground(Uri... params) {

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
		return venueDataMap.get(key);
	}
}