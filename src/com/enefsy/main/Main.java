package com.enefsy.main;

/* Java io package */
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/* Apache DB package */
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/* Facebook package */
import com.enefsy.foursquare.FoursquareApp;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.enefsy.main.MimeType;
import com.enefsy.main.R;

/* Foursquare package */
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.io.DefaultIOHandler;

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
	private String uid = "";
	
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
	
	/* Foursquare Objects/Variables */
	private FoursquareApi foursquareApi;
	private FoursquareApp foursquareClient;
	private static final String FOURSQUARE_CLIENT_ID = "4NOPZVJ4ILTBQLU1AYO2BX2QMUBCJCLL3RFF0UETEZOQW02W";
	private static final String FOURSQUARE_CLIENT_SECRET = "UAE5UZZ0KMDPTWOSYHU1R1UA3JX4NJDHO1HY5HWL3TJHVPQ1";
	private static final String FOURSQUARE_REDIRECT_URL = "http://www.enefsy.com";
	
	/* Diagnostic TextView */
	private TextView mTextView;

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
        
        
        /* Check for available NFC Adapter */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
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
                	                parameters.putString("message", "Heading to bed");
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

        	/* Create a new foursquare app to deal with permissions and access
        	 * tokens */
        	foursquareClient = new FoursquareApp(this, FOURSQUARE_CLIENT_ID, 
        											FOURSQUARE_CLIENT_SECRET);
        	
        	/* If the user hasn't granted us permissions to access their foursquare
        	 * account, show a dialog requesting permissions
        	 */
        	if (!foursquareClient.hasAccessToken())
        		foursquareClient.authorize();
        	
        	/* Create a foursquare API to deal with retrieving and posting info */
			this.foursquareApi = new FoursquareApi(FOURSQUARE_CLIENT_ID, 
												FOURSQUARE_CLIENT_SECRET, 
												FOURSQUARE_REDIRECT_URL,
												foursquareClient.mAccessToken, 
												new DefaultIOHandler());

			/* Check the user in a Foursquare venue given its ID */
			new FoursquareCheckinTask(this.foursquareApi, uid).execute();
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
	
	
    private class FoursquareCheckinTask extends AsyncTask<Uri, Void, Void> {

		private String apiStatusMsg;
		private FoursquareApi foursquareApi;
		private String venueId;
		
		public FoursquareCheckinTask(FoursquareApi foursquareApi, String venueId) {
			super();
			this.foursquareApi = foursquareApi;
			this.venueId = venueId;
		}
		
		@Override
		protected Void doInBackground(Uri...params) {
			try {
				Result<Checkin> result = this.foursquareApi.checkinsAdd(this.venueId, 
												null,"",null,null,null,null,null);

				if (result.getMeta().getCode()==200) {
					apiStatusMsg = "Thanks for checking in via Enefsy!";
				} else {
					apiStatusMsg = result.getMeta().getErrorDetail();
				}
			} catch (FoursquareApiException e) {
				e.printStackTrace();
			}
            return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(Main.this, apiStatusMsg, Toast.LENGTH_LONG).show();
		}
	}

    private class GetVenueData extends AsyncTask<String, Void, String> {

    	@Override
		protected String doInBackground(String... uids) {
    		
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
//				for(int i=0;i<jArray.length();i++){
//   	                JSONObject json_data = jArray.getJSONObject(i);
//   	                Log.i("log_tag","id: "+json_data.getInt("id")+
//   	                        ", name: "+json_data.getString("name")+
//   	                        ", address: "+json_data.getString("address")+
//   	                        ", latitude: "+json_data.getDouble("latitude")+
//   	                        ", longitude: "+json_data.getDouble("longitude")+
//   	                        ", facebookid: "+json_data.getString("facebookid")+
//   	                        ", twitterhandle: "+json_data.getString("twitterhandle")+
//  	                        ", foursquareid: "+json_data.getString("foursquareid")+
//   	                        ", googleid: "+json_data.getString("googleid")+
//   	                        ", yelpid: "+json_data.getString("yelpid")
//   	                );
//				}
				setVenueData(jArray);
			} catch(JSONException e){
				Log.e("log_tag", "Error parsing data " + e.toString());
			}

		return venueData;
		}
    	
        @Override
        protected void onPostExecute(String s) {
            mTextView.setText("Database Query Successful");
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
