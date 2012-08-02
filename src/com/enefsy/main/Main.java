package com.enefsy.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

/* Facebook dependencies */
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

/* Foursquare depdendencies */
import com.enefsy.foursquare.FoursquareActivity;

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

	/* Foursquare activity object */
	private FoursquareActivity foursquareActivity;
	
	/* Database querying object */
	private DatabaseActivity databaseActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        

        /* Social Platform buttons */
        facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        facebook_button.setOnClickListener(this);
        twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        twitter_button.setOnClickListener(this);
        foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);
        foursquare_button.setOnClickListener(this);
        
        /* Check for available NFC Adapter */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available, using Meadowbrook data", Toast.LENGTH_LONG).show();
//            finish();
//            return;
            /* Construct Database Activity */
            databaseActivity = new DatabaseActivity(this, "11111111111111111112");
        } else {
	        /* See if application was started from an NFC tag */
	        Intent intent = getIntent();
	        if(intent.getType() != null && intent.getType().equals("application/vnd.enefsy.main")) {
	        	Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	            NdefMessage msg = (NdefMessage) rawMsgs[0];
	            NdefRecord uidRecord = msg.getRecords()[0];
	            databaseActivity = new DatabaseActivity(this, new String(uidRecord.getPayload()));
	        } else {
	        	databaseActivity = new DatabaseActivity(this, "11111111111111111112");
	        }
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
                	                parameters.putString("message", "I just checked in using Enefsy!");
                	                parameters.putString("place", databaseActivity.getVenueDataMapValue("facebookid"));
                	                parameters.putString("description", "Enefsy powered Check-in");
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
            String message = "Your message to post";
            try {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setClassName("com.twitter.android","com.twitter.android.PostActivity");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(sharingIntent);
            } catch (Exception e) {
                Intent i = new Intent();
                i.putExtra(Intent.EXTRA_TEXT, message);
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://mobile.twitter.com/compose/tweet"));
                startActivity(i);
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
	        			foursquareActivity.checkIn(databaseActivity.getVenueDataMapValue("foursquareid")); 
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
}
