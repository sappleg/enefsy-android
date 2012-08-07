package com.enefsy.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.enefsy.facebook.FacebookError;


import com.enefsy.facebook.AsyncFacebookRunner.RequestListener;

public class PostRequestListener implements RequestListener {

		
	@Override
	public void onComplete(String response, Object state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onIOException(IOException e, Object state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, Object state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMalformedURLException(MalformedURLException e, Object state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFacebookError(FacebookError e, Object state) {
		// TODO Auto-generated method stub

	}

}
