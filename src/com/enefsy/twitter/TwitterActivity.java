/**
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 * 
 * http://www.londatiga.net
 */

package com.enefsy.twitter;

import java.net.MalformedURLException;
import java.net.URLDecoder;

import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import java.net.URL;


public class TwitterActivity {

	private Twitter mTwitter;
	private TwitterSession mSession;
	private AccessToken mAccessToken;
	private CommonsHttpOAuthConsumer mHttpOauthConsumer;
	private DefaultOAuthProvider mHttpOauthprovider;
	private ProgressDialog mProgressDialog;
	private TwitterDialogListener mListener;
	private Context context;
	private boolean mInit = true;
	private String tokenedUrl;
	private String status;

	private static final String TWITTER_CONSUMER_KEY = "7yqQQggvKFcb8U3CYmiOQ";
	private static final String TWITTER_SECRET_KEY = "61MYne9XJphKQefGnZTWIBvLmZiT8AMV948DkjZYY";
	public static final String TWITTER_AUTH_URL = "http://twitter.com/oauth/authorize";
	public static final String TWITTER_REQUEST_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_CALLBACK_URL = "http://www.enefsy.com";

	private static final String TAG = "TwitterActivity";
	
	
	public TwitterActivity(Context context) {

		this.context = context;
		
		mTwitter = new TwitterFactory().getInstance();
		mSession = new TwitterSession(context);
		mProgressDialog	= new ProgressDialog(context);		
		mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		mListener = new TwitterDialogListener() {
					
			@Override
			public void onComplete() { 
				processToken();
			}
			
			@Override
			public void onError(String value) { }

			@Override
			public void setRedirectURL(String url) {
				tokenedUrl = url;
			}
		};
		
		mHttpOauthConsumer = new CommonsHttpOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_SECRET_KEY);
		mHttpOauthprovider = new DefaultOAuthProvider(TWITTER_REQUEST_URL, TWITTER_TOKEN_URL, TWITTER_AUTH_URL);
		
		mAccessToken = mSession.getAccessToken();		
	}
	
	
	public void configureToken() {

		if (mAccessToken != null) {

			if (mInit) {
				mTwitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_SECRET_KEY);
				mInit = false;
			}
			
			mTwitter.setOAuthAccessToken(mAccessToken);
		}
	}
	
	
	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}
	
	
	public void resetAccessToken() {
		if (mAccessToken != null) {
			mSession.resetAccessToken();
			mAccessToken = null;
		}
	}
		
	
	public void updateStatus(String status){
		try {
			this.status = status;
			
//			mTwitter.updateStatus(status);
			new TwitterUpdateStatusTask().execute();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void authorize() {		
		new TwitterAuthorizeTask().execute();
	}

	
	public void processToken()  {
		new TwitterProcessTokenTask().execute();
	}
	
	
	private String getVerifier(String callbackUrl) {
		String verifier	 = "";
		
		try {
			callbackUrl = callbackUrl.replace("twitterapp", "http");

			URL url = new URL(callbackUrl);
			String query = url.getQuery();
		
			String array[] = query.split("&");

			for (String parameter : array) {
	             String v[] = parameter.split("=");
	             
	             if (URLDecoder.decode(v[0]).equals(oauth.signpost.OAuth.OAUTH_VERIFIER)) {
	            	 verifier = URLDecoder.decode(v[1]);
	            	 break;
	             }
	        }
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return verifier;
	}
	
	
	private void showLoginDialog(String url) {
		new TwitterDialog(context, url, mListener).show();
	}
	
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mProgressDialog.dismiss();
			
			if (msg.what == 1) {
				if (msg.arg1 == 1)
					mListener.onError("Error getting request token");
				else
					mListener.onError("Error getting access token");
			} 
			else {
				if (msg.arg1 == 1)
					showLoginDialog((String) msg.obj);
				else
					mListener.onComplete();
			}
		}
	};

	
	private class TwitterAuthorizeTask extends AsyncTask<Uri, Void, Void> {
		
		protected void onPreExecute() {
			mProgressDialog.setMessage("Opening Twitter...");
			mProgressDialog.show();		
		}
		
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
		}
		
		@Override
		protected Void doInBackground(Uri... params) {

            try {
        				
				Log.i(TAG, "Getting access token");
				
				
				String authUrl = "";
				int what = 1;
				
				try {
					authUrl = mHttpOauthprovider.retrieveRequestToken(mHttpOauthConsumer, TWITTER_TOKEN_URL);	
					what = 0;
					Log.d(TAG, "Request token url " + authUrl);
				} 
				catch (Exception e) {
					Log.d(TAG, "Failed to get request token");
					e.printStackTrace();
				}
				
				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0, authUrl));				
				
           }
           catch (Exception e) {
        	   e.printStackTrace();
           }
            
            return null;
		}		
	}
	
	
	private class TwitterProcessTokenTask extends AsyncTask<Uri, Void, Void> {
		
		protected void onPreExecute() {
			mProgressDialog.setMessage("Saving settings...");
			mProgressDialog.show();		
		}
		
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
		}
		
		@Override
		protected Void doInBackground(Uri... params) {

			try {
				final String verifier = getVerifier(tokenedUrl);
	
				int what = 1;
				
				try {
					mHttpOauthprovider.retrieveAccessToken(mHttpOauthConsumer, verifier);
					mAccessToken = new AccessToken(mHttpOauthConsumer.getToken(), mHttpOauthConsumer.getTokenSecret());
					configureToken();
					User user = mTwitter.verifyCredentials();
			        mSession.storeAccessToken(mAccessToken, user.getName());			        
			        what = 0;
				} 
				catch (Exception e){
					Log.d(TAG, "Error getting access token");	
					e.printStackTrace();
				}
				
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
			catch (Exception e) {
        	   e.printStackTrace();
			}
		
			return null;
		}		
	}
	
	
	private class TwitterUpdateStatusTask extends AsyncTask<Uri, Void, Void> {
		
		protected void onPreExecute() {
			mProgressDialog.setMessage("Tweeting...");
			mProgressDialog.show();		
			configureToken();
		}
		
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
		}
		
		@Override
		protected Void doInBackground(Uri... params) {

			/*FIXME: This is simply a placeholder for now since I could not
					make the TwitterFactory.updateStatus(status) method work inside
					of an AsyncTask, but I still want to display a progress dialog
					while updating the user's status
			*/
			try {
				mTwitter.updateStatus(status);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}		
	}
	
	
	
	public interface TwitterDialogListener {
		public void onComplete();
		void setRedirectURL(String url);
		public void onError(String value);
	}
}