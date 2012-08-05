/**
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 * 
 * http://www.londatiga.net
 */
package com.enefsy.twitter;

import twitter4j.auth.AccessToken;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Context;


public class TwitterSession {

	private SharedPreferences sharedPref;
	private Editor editor;
	
	private static final String TWITTER_AUTH_KEY = "auth_key";
	private static final String TWITTER_AUTH_SECRET_KEY = "auth_secret_key";
	private static final String TWITTER_USER_NAME = "user_name";
	private static final String SHARED = "Twitter_Preferences";
	
	public TwitterSession(Context context) {
		sharedPref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);	
		editor = sharedPref.edit();
	}

	
	public void storeAccessToken(AccessToken mAccessToken, String username) {
		editor.putString(TWITTER_AUTH_KEY, mAccessToken.getToken());
		editor.putString(TWITTER_AUTH_SECRET_KEY, mAccessToken.getTokenSecret());
		editor.putString(TWITTER_USER_NAME, username);
		editor.commit();
	}
	
	
	public void resetAccessToken() {
		editor.putString(TWITTER_AUTH_KEY, null);
		editor.putString(TWITTER_AUTH_SECRET_KEY, null);
		editor.putString(TWITTER_USER_NAME, null);
		editor.commit();
	}
	
	
	public AccessToken getAccessToken() {

		String token = sharedPref.getString(TWITTER_AUTH_KEY, null);
		String tokenSecret = sharedPref.getString(TWITTER_AUTH_SECRET_KEY, null);
		
		if (token != null && tokenSecret != null) 
			return new AccessToken(token, tokenSecret);
		else
			return null;
	}
}