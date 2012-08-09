package com.enefsy.main;

import java.util.HashMap;
import java.util.Map;

import com.enefsy.facebook.FacebookActivity;
import com.enefsy.foursquare.FoursquareActivity;
import com.enefsy.twitter.TwitterActivity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class ShareFragment extends Fragment implements OnClickListener {
	
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
	
	
}