package com.enefsy.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PromptActivity extends Activity implements OnClickListener {
	
	/* Button to invoke QR code scan */
	private Button qr_button;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prompt);

        /* QR code scanning button */
        qr_button = (Button) findViewById(R.id.qr_button);
        qr_button.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        /**********************************************************************
         * 								QR Scanner
         *********************************************************************/
        /* If the user clicks on the QR Scanner button */
        if (v == qr_button) {
        	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        	intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        	startActivityForResult(intent, 0);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
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
    	        	//setTextView("Please scan Enefsy QR Code\nPress Scan QR Code button to retry");
    	        }
    		} else if (resultCode == RESULT_CANCELED) {
    			// Handle cancel
    			//setTextView("Failed scan\nPress Scan QR Code button to retry");
    		}
    	}
    }
}