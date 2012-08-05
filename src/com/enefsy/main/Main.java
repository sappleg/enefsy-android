package com.enefsy.main;

/* Java io package */
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

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

/* Android package */
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/* Facebook dependencies */
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.enefsy.foursquare.FoursquareActivity;
import com.enefsy.main.MimeType;
import com.enefsy.main.R;
import com.enefsy.twitter.TwitterActivity;

/* Foursquare depdendencies */

@TargetApi(14)
public class Main extends Activity implements DialogListener, OnClickListener {

	/* Image buttons for front screen */
	private ImageButton facebook_button;
	private ImageButton twitter_button;
	private ImageButton foursquare_button;

	/* Creates a Facebook Object with the Enefsy Facebook App ID */
	private Facebook facebookClient;
	private AsyncFacebookRunner asyncFacebookClient;
	
    /* NFC Adapter to pull UID message from tag */
	private NfcAdapter mNfcAdapter;
	
	/* String to hold the Unique ID of the venue */
	private String uid = "11111111111111111111";
	
	/* Return String containing venue specific db data */
	private String name = "";
	private String address = "";
	private double latitude = 0.0;
	private double longitude = 0.0;
	private String facebookid = "";
	private String twitterhandle = "";
	private String foursquareid = "";
	private String googleid = "";
	private String yelpid = "";
	
	/* Diagnostic TextView */
	private TextView mTextView;

	/* Foursquare activity object */
	private FoursquareActivity foursquareActivity;
	private TwitterActivity twitterActivity;
	
	/* Progress Dialog for Database Query */
	private ProgressDialog mProgress;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* Diagnostic textview */
        mTextView = (TextView)findViewById(R.id.uid_view);

        /* Social Platform buttons */
        facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        facebook_button.setOnClickListener(this);
        twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        twitter_button.setOnClickListener(this);
        foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);
        foursquare_button.setOnClickListener(this);
        
        /* Construct Progress Dialog for Database Query */
        mProgress = new ProgressDialog(getApplicationContext());
        
        /* Check for available NFC Adapter */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
//            finish();
//            return;
        }
        
        /* See if application was started from an NFC tag */
        Intent intent = getIntent();
        if(intent.getType() != null && intent.getType().equals(MimeType.NFC_DEMO)) {
        	Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord uidRecord = msg.getRecords()[0];
            uid = new String(uidRecord.getPayload());
        }
        
        /* pull data from enefsy database */
        if(uid != "") {
			new GetVenueData().execute();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookClient.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onError(DialogError e) {
        System.out.println("Error: " + e.getMessage());
    }
    
    @Override
    public void onCancel() {
		// TODO Auto-generated method stub
    }
    
    @Override
    public void onClick(View v) {
        /**********************************************************************
         * 								FACEBOOK
         *********************************************************************/
        if (v == facebook_button) {
        	facebookClient = new Facebook("287810317993311");
        	asyncFacebookClient = new AsyncFacebookRunner(facebookClient);

       		/* Attributes to check if user has granted permissions for facebook */
       	    final SharedPreferences mPrefs;
       	    
            try {
        		/* Last updated: 7/24/2012
                 * This is derived from the facebook developer's site on integrating with a 
                 * native android app: https://developers.facebook.com/docs/mobile/android/sso/
            	 * If the user clicks the facebook button, checks to see if user has granted
		    	 * permission to the app to publish streams and checkins to profile. If so,
		    	 * that permission is saved in an access token and can be references without 
		    	 * the user's permission in the future
		         */

            	mPrefs = getPreferences(MODE_PRIVATE);
                String access_token = mPrefs.getString("access_token", null);
                long expires = mPrefs.getLong("access_expires", 0);

                if(access_token != null) {
                    facebookClient.setAccessToken(access_token);
                }
                if(expires != 0) {
                    facebookClient.setAccessExpires(expires);
                }

                facebookClient.authorize(Main.this, new String[] { "publish_checkins, publish_stream" },
                	new DialogListener() {
                    	@Override
                        public void onComplete(Bundle values) {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString("access_token", facebookClient.getAccessToken());
                            editor.putLong("access_expires", facebookClient.getAccessExpires());
                            editor.commit();
                            
                	        if (!values.containsKey("post_id")) {
                	            try {
                	            	//RequestListener listener = new RequestListener();
                	            	Object state = new Object();
                	            	// The following code will make an automatic status update
                	                Bundle parameters = new Bundle();
                	                parameters.putString("message", "");
                	                parameters.putString("place", facebookid);
                	                parameters.putString("description", "test test test");
                	                asyncFacebookClient.request("me/feed", parameters, "POST", 
                	                		new PostRequestListener(), state);
                	                
                	                Context context = getApplicationContext();
                	                CharSequence text = "Thanks for posting to Facebook!";
                	                int duration = Toast.LENGTH_LONG;

                	                Toast toast = Toast.makeText(context, text, duration);
                	                toast.show();
                	            }
                	            catch (Exception e) {
                	                // TODO: handle exception
                	                System.out.println(e.getMessage());
                	            }
                	        }		
                        }
            
                        @Override
                        public void onFacebookError(FacebookError error) {}
            
                        @Override
                        public void onError(DialogError e) {}
            
                        @Override
                        public void onCancel() {}
                    });

            /* If the user can't post directly to facebook or doesn't grant our app permissions */
            } catch (Exception e1) {

            	/* Try redirecting to the native facebook app */
            	try {
            		Intent intent = new Intent("android.intent.category.LAUNCHER");
              		intent.setClassName("com.facebook.katana", "com.facebook.katana.LoginActivity");
              		startActivity(intent);
              		
           		/* Otherwise redirect to the mobile facebook site */
            	} catch (Exception e2) {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse("https://m.facebook.com/?_rdr"));
                    startActivity(intent);
            	}
            }
        }
        
        /**********************************************************************
         * 								TWITTER
         *********************************************************************/
        /* The user clicks the Twitter button */
        else if (v == twitter_button) {

        	/* Create a new twitter activity */
        	twitterActivity	= new TwitterActivity(this);

    		if (!twitterActivity.hasAccessToken()) {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected())
        			twitterActivity.authorize();

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
    		}

    		else {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
	    			twitterActivity.configureToken();
					twitterActivity.updateStatus("I'm at @Tullys_Shops -- via @enefsy #PleasantonCATullys");
        		}

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Twitter. Please check your network settings.", Toast.LENGTH_LONG).show();
    		}        	
        }
        
        
        /**********************************************************************
         * 								FOURSQUARE
         *********************************************************************/
        /* If the user clicks on the foursquare button */
        else if (v == foursquare_button) {

        	/* Create a new foursquare activity */
        	foursquareActivity = new FoursquareActivity(this);
        	
        	/* If the user hasn't granted us permissions to access their foursquare
        	 * account, show a dialog requesting permissions
        	 */
        	if (!foursquareActivity.hasAccessToken()) {
        		
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected())
        			foursquareActivity.authorize();
        		
        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
        	}
        	
        	/* Otherwise the user has already granted us permissions so check them in */
        	else {

        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
	        		foursquareActivity.initializeApi();
	        		try {
	        			foursquareActivity.checkIn(foursquareid); 
	        		} catch(Exception e) {
	        			e.printStackTrace();
	        		}
        		}

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
        	}
        }
    }
    
    
	@Override
	public void onComplete(Bundle values) {
		// TODO Auto-generated method stub
	}

	
	@Override
	public void onFacebookError(FacebookError e) {
		// TODO Auto-generated method stub
	}
	
	
	/* This returns a boolean indicating whether or not the phone has an active network connection */
	public boolean isNetworkConnected() {

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
	   
		if (networkInfo != null && networkInfo.isConnected())
			return true;
		else
			return false;

	}


    private class GetVenueData extends AsyncTask<Uri, Void, Void> {
    	
		protected void onPreExecute() {
			mProgress.setMessage("Connecting to Enefsy...");
			//mProgress.show();		
		}

    	@Override
		protected Void doInBackground(Uri... params) {
    		
    		//the uid data to send
    		final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    		nameValuePairs.add(new BasicNameValuePair("id",uid));
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
    	
    	
        protected void onPostExecute(Void result) {
            mTextView.setText("Database Query Successful");
            mProgress.dismiss();
        }
        
        
        protected void setVenueData(JSONArray jArray) {
        	try {
				JSONObject json_data = jArray.getJSONObject(0);
				name = json_data.getString("name");
				address = json_data.getString("address");
				latitude = json_data.getDouble("latitude");
				longitude = json_data.getDouble("longitude");
				facebookid = json_data.getString("facebookid");
				twitterhandle = json_data.getString("twitterhandle");
				foursquareid = json_data.getString("foursquareid");
				googleid = json_data.getString("googleid");
				yelpid = json_data.getString("yelpid");
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
    }
}
