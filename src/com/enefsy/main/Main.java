package com.enefsy.main;

import java.util.HashMap;
import java.util.Map;

/* Android package */
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.enefsy.facebook.FacebookActivity;
import com.enefsy.twitter.TwitterActivity;
import com.enefsy.foursquare.FoursquareActivity;


@TargetApi(14)
public class Main extends Activity implements OnClickListener {

	/* Image buttons for front screen */
	private ImageButton facebook_button;
	private ImageButton twitter_button;
	private ImageButton foursquare_button;
	private ImageButton google_button;

	private FacebookActivity facebookActivity;
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
        
        /* TextView to hold name of venue */
        mTextView = (TextView) findViewById(R.id.venue_view);
        
        /* Declare Hashmap to contain venue data */
        venueData = new HashMap<String, String>();
        
        /* Social Platform buttons */
        facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        facebook_button.setOnClickListener(this);
        twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        twitter_button.setOnClickListener(this);
        foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);
        foursquare_button.setOnClickListener(this);
        google_button = (ImageButton) findViewById(R.id.google_button);
        google_button.setOnClickListener(this);
        
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
    public void onClick(View v) {
        /**********************************************************************
         * 								FACEBOOK
         *********************************************************************/
        /* The user clicks the Facebook button */
        if (v == facebook_button) {

        	/* Create a new facebook activity */
        	facebookActivity = new FacebookActivity(this);
        	
        	if (!facebookActivity.hasAccessToken()) {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected())
        			facebookActivity.authorizeAndCheckin(getVenueDataMapValue("facebookid"));

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Facebook. Please check your network settings.", Toast.LENGTH_LONG).show();
        	}
        	else {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected())
        			facebookActivity.checkin(getVenueDataMapValue("facebookid"));

            		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Facebook. Please check your network settings.", Toast.LENGTH_LONG).show();
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
        		if (isNetworkConnected()) {
        			twitterActivity.authorizeAndUpdateStatus("I'm at " + getVenueDataMapValue("twitterhandle") + " -- via @enefsy");
        		}
        		/* If no internet connection is available, alert user */
        		else {
        			Toast.makeText(this, "Unable to connect to Twitter. Please check your network settings.", Toast.LENGTH_LONG).show();
        		}
    		}

    		else {
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
					twitterActivity.updateStatus("I'm at " + getVenueDataMapValue("twitterhandle") + " -- via @enefsy");
        		}
        		/* If no internet connection is available, alert user */
        		else {
        			Toast.makeText(this, "Unable to connect to Twitter. Please check your network settings.", Toast.LENGTH_LONG).show();
        		}
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
        			foursquareActivity.authorizeAndCheckIn(getVenueDataMapValue("foursquareid"));
        		
        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
        	} 
        	else { 
        		/* Otherwise the user has already granted us permissions so check them in */
        		/* If the phone is connected to the internet, try to authorize the user */
        		if (isNetworkConnected()) {
	        		try {
	        			foursquareActivity.checkIn(getVenueDataMapValue("foursquareid")); 
	        		} 
	        		catch(Exception e) {
	        			e.printStackTrace();
	        		}
        		}

        		/* If no internet connection is available, alert user */
        		else
        			Toast.makeText(this, "Unable to connect to Foursquare. Please check your network settings.", Toast.LENGTH_LONG).show();
        	}
        }
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
