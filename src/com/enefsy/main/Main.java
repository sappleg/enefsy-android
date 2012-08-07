package com.enefsy.main;

import java.util.HashMap;
import java.util.Map;

/* Android package */
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/* Facebook dependencies */
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

/* Foursquare dependency */
import com.enefsy.foursquare.FoursquareActivity;

/* Twitter dependency */
import com.enefsy.twitter.TwitterActivity;

@TargetApi(14)
public class Main extends Activity implements DialogListener, OnClickListener {

	/* Image buttons for front screen */
	private ImageButton facebook_button;
	private ImageButton twitter_button;
	private ImageButton foursquare_button;
	
	/* Button to invoke QR code scan */
	private Button qr_button;

	/* Creates Facebook Objects with the Enefsy Facebook App ID */
	private Facebook facebookClient;
	private AsyncFacebookRunner asyncFacebookClient;

	/* Foursquare activity object */
	private FoursquareActivity foursquareActivity;
	private TwitterActivity twitterActivity;
	
	/* TextView to hold name of venue */
	private TextView mTextView;
	
	/* Hashmap to contain all venue specific data.
	   The default values are stored for Dublin, CA Starbucks */
	private Map<String, String> venueData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /* Declare textview to show venue name */
        mTextView = (TextView)findViewById(R.id.uid_view);
        
        /* Declare Hashmap to contain venue data */
        venueData = new HashMap<String, String>();
        
        /* Social Platform buttons */
        facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        facebook_button.setOnClickListener(this);
        twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        twitter_button.setOnClickListener(this);
        foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);
        foursquare_button.setOnClickListener(this);
        
        /* QR code scanning button */
        qr_button = (Button) findViewById(R.id.qr_button);
        qr_button.setOnClickListener(this);
        
        /* Get data from database activity */
        Intent i = getIntent();
        boolean queried = i.getBooleanExtra("queried", false);
        if (queried) {
	        venueData.put("uid", i.getStringExtra("uid"));
	        venueData.put("name", i.getStringExtra("name"));
	        venueData.put("address", i.getStringExtra("address"));
	        venueData.put("latitude", i.getStringExtra("latitude"));
	        venueData.put("longitude", i.getStringExtra("longitude"));
	        venueData.put("facebookid", i.getStringExtra("facebookid"));
	        venueData.put("twitterhandle", i.getStringExtra("twitterhandle"));
	        venueData.put("foursquareid", i.getStringExtra("foursquareid"));
	        venueData.put("googleid", i.getStringExtra("googleid"));
	        venueData.put("yelpid", i.getStringExtra("yelpid"));
	        
	        setTextView(getVenueDataMapValue("name"));
        } else {
            setTextView("Tap phone to NFC Tag\nOr scan QR code");
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
//        facebookClient.authorizeCallback(requestCode, resultCode, data);
    	if (requestCode == 0) {
    		if (resultCode == RESULT_OK) {
    			String contents = intent.getStringExtra("SCAN_RESULT");
    	        String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
    	        if (format.equals("QR_CODE")) {
    	        	// Handle successful scan
    	        	Intent newIntent = new Intent(getApplicationContext(),LaunchActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	        	newIntent.putExtra("uid", contents);
    	        	newIntent.putExtra("qr_read", true);
    	        	startActivity(newIntent);
    	        } else {
    	        	setTextView("Please scan Enefsy QR Code\nPress Scan QR Code button to retry");
    	        }
    		} else if (resultCode == RESULT_CANCELED) {
    			// Handle cancel
    			setTextView("Failed scan\nPress Scan QR Code button to retry");
    		}
    	}
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
                	                String tmp = getVenueDataMapValue("facebookid");
//                	                parameters.putString("place", databaseActivity.getVenueDataMapValue("facebookid"));
                	                parameters.putString("place", tmp);
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

        	/* Create a new twitter activity */
        	twitterActivity	= new TwitterActivity(this);

    		if (!twitterActivity.hasAccessToken()) {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected())
        			twitterActivity.authorize();

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Twitter. Please check your network settings.", Toast.LENGTH_LONG).show();
    		}

    		else {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
	    			twitterActivity.configureToken();
					twitterActivity.updateStatus("I'm at " + getVenueDataMapValue("twitterhandle") + " -- via @enefsy");
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
        	} else { /* Otherwise the user has already granted us permissions so check them in */
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
	        		foursquareActivity.initializeApi();
	        		try {
	        			foursquareActivity.checkIn(getVenueDataMapValue("foursquareid")); 
	        		} catch(Exception e) {
	        			e.printStackTrace();
	        		}
        		}

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
        	}
        }
        
        /**********************************************************************
         * 								QR Scanner
         *********************************************************************/
        /* If the user clicks on the QR Scanner button */
        else if (v == qr_button) {
        	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        	intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        	startActivityForResult(intent, 0);
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
	
	public void setTextView(String s) {
		mTextView.setText(s);
	}
	
	public String getVenueDataMapValue(String key) {
		return venueData.get(key);
	}
}
