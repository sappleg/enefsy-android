package com.enefsy.facebook;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Context;

/**
 * Manage access token and user name. Uses shared preferences to store access token
 * and user name.
 * 
 * @author Lorensius W. L T <lorenz@londatiga.net>
 *
 */
public class FacebookSession {

	private SharedPreferences sharedPref;
	private Editor editor;
	
	private static final String SHARED = "Facebook_Preferences";
	private static final String FBOOK_ACCESS_TOKEN = "access_token";
	
	public FacebookSession(Context context) {
		sharedPref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
		editor = sharedPref.edit();
	}

	
	/**
	 * Save access token and user name
	 * 
	 * @param accessToken Access token
	 * @param username User name
	 */
	public void storeAccessToken(String accessToken) {
		editor.putString(FBOOK_ACCESS_TOKEN, accessToken);
		editor.commit();
	}

	
	/**
	 * Reset access token and user name
	 */
	public void resetAccessToken() {
		editor.putString(FBOOK_ACCESS_TOKEN, null);
		editor.commit();
	}
	
	
	/**
	 * Get access token
	 * 
	 * @return Access token
	 */
	public String getAccessToken() {
		return sharedPref.getString(FBOOK_ACCESS_TOKEN, null);
	}
}