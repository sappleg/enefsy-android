/**
 * Modified from FbDialog from Facebook SDK
 * 
 * Lorensius W. L. T <lorenz@londatiga.net>
 */
package com.enefsy.twitter;

import com.enefsy.twitter.TwitterActivity;
import com.enefsy.twitter.TwitterActivity.TwitterDialogListener;

import android.app.Dialog;
import android.app.ProgressDialog;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;

import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;


public class TwitterDialog extends Dialog {
	
    static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                         						ViewGroup.LayoutParams.FILL_PARENT);
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private String mUrl;
    private TwitterDialogListener mListener;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;

    private static final String TAG = "Twitter-WebView";
    
    
    public TwitterDialog(Context context, String url, TwitterDialogListener listener) {
        super(context);
        mUrl 		= url;
        mListener 	= listener;
    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSpinner = new ProgressDialog(getContext());       
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(getContext());
        mContent.setOrientation(LinearLayout.VERTICAL);
        
        setUpTitle();
        setUpWebView();
        
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        double[] dimensions = new double[2];
        
        if (display.getWidth() < display.getHeight()) {
        	dimensions[0]	= 0.87 * display.getWidth();
        	dimensions[1]	= 0.82 * display.getHeight();
        } else {
        	dimensions[0]	= 0.75 * display.getWidth();
        	dimensions[1]	= 0.75 * display.getHeight();        
        }
        
        addContentView(mContent, new FrameLayout.LayoutParams((int) dimensions[0], (int) dimensions[1]));
    }

    
    private void setUpTitle() {

    	requestWindowFeature(Window.FEATURE_NO_TITLE);
                
        mTitle = new TextView(getContext());
        mTitle.setText("Twitter");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(0xFFbbd7e9);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        
        mContent.addView(mTitle);
    }

    
    private void setUpWebView() {
    	
    	CookieSyncManager.createInstance(getContext()); 
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        
        mWebView = new WebView(getContext());
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new TwitterWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(FILL);
        
        mContent.addView(mWebView);
    }

    
    private class TwitterWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	Log.d(TAG, "Redirecting URL " + url);
        	
        	if (url.startsWith(TwitterActivity.TWITTER_TOKEN_URL)) {

        		mListener.onComplete();
        		
        		TwitterDialog.this.dismiss();
        		
        		return true;
        	}  
        	else if (url.startsWith("authorize")) {
        		return false;
        	}
        	
            return true;
        }

        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        	Log.d(TAG, "Page error: " + description);
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(description);
            TwitterDialog.this.dismiss();
        }

        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Loading URL: " + url);
        	
        	if (url.startsWith(TwitterActivity.TWITTER_TOKEN_URL)) {
            	Log.d(TAG, "Redirecting URL " + url);
            	mListener.setRedirectURL(url);
        		mListener.onComplete();
        		TwitterDialog.this.dismiss();        		
            }
        	else {	
	            super.onPageStarted(view, url, favicon);
	            mSpinner.show();
        	}
        }

        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }
            mSpinner.dismiss();
        }
    }
}