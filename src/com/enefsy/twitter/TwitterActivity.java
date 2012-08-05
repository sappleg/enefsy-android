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
	private String mConsumerKey;
	private String mSecretKey;
	private ProgressDialog mProgressDialog;
	private TwitterDialogListener mListener;
	private Context context;
	private boolean mInit = true;

	public static final String TWITTER_AUTH_URL = "http://twitter.com/oauth/authorize";
	public static final String TWITTER_REQUEST_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_CALLBACK_URL = "http://www.enefsy.com";
	private static final String TAG = "TwitterActivity";

	
	public TwitterActivity(Context context, String consumerKey, String secretKey) {

		this.context = context;
		
		mTwitter = new TwitterFactory().getInstance();
		mSession = new TwitterSession(context);
		mProgressDialog	= new ProgressDialog(context);
		
		mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		mConsumerKey = consumerKey;
		mSecretKey = secretKey;
	
		mHttpOauthConsumer = new CommonsHttpOAuthConsumer(mConsumerKey, mSecretKey);
		mHttpOauthprovider = new DefaultOAuthProvider(TWITTER_REQUEST_URL,
													 TWITTER_TOKEN_URL,
													 TWITTER_AUTH_URL);
		
		mAccessToken = mSession.getAccessToken();		
//		configureToken();
	}
	
	
	public void setListener(TwitterDialogListener listener) {
		mListener = listener;
	}
	
	
	@SuppressWarnings("deprecation")
	public void configureToken() {

		if (mAccessToken != null) {
			if (mInit) {
				mTwitter.setOAuthConsumer(mConsumerKey, mSecretKey);
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
	
	
	public String getUsername() {
		return mSession.getUsername();
	}
	
	
	public void updateStatus(String status) throws Exception {
		try {
			mTwitter.updateStatus(status);
		} 
		catch (TwitterException e) {
			throw e;
		}
	}
	
	
	public void authorize() {

		mProgressDialog.setMessage("Initializing ...");
		mProgressDialog.show();
		
		new Thread() {
			@Override
			public void run() {
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
//				processToken();
			}
		}.start();
	}

	
	public void processToken(String url)  {

		mProgressDialog.setMessage("Finalizing ...");
		mProgressDialog.show();
		
		final String verifier = getVerifier(url);

		new Thread() {
			
			@Override
			public void run() {
				int what = 1;
				
				try {
					mHttpOauthprovider.retrieveAccessToken(mHttpOauthConsumer, verifier);
		
					mAccessToken = new AccessToken(mHttpOauthConsumer.getToken(), mHttpOauthConsumer.getTokenSecret());
				
					configureToken();
				
					User user = mTwitter.verifyCredentials();
				
			        mSession.storeAccessToken(mAccessToken, user.getName());
			        
			        what = 0;
				} catch (Exception e){
					Log.d(TAG, "Error getting access token");
					
					e.printStackTrace();
				}
				
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();
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

		final TwitterDialogListener listener = new TwitterDialogListener() {
			
			String redirectUrl;
			
			@Override
			public void setRedirectURL(String url) {
				this.redirectUrl = url;
			}

			@Override
			public void onComplete() {
				processToken(this.redirectUrl);
			}
			
			@Override
			public void onError(String value) {
				mListener.onError("Failed opening authorization page");
			}

		};
		
		new TwitterDialog(context, url, listener).show();
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

	
	public interface TwitterDialogListener {
		public void onComplete();			
		void setRedirectURL(String url);
		public void onError(String value);
	}
}