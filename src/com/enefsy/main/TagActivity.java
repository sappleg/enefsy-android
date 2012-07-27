package com.enefsy.main;

//import com.enefsy.main.R;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;

public class TagActivity extends Activity {
	private String uid;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        
        // see if app was started from a tag and show game console
        Intent intent = getIntent();
        if(intent.getType() != null && intent.getType().equals(MimeType.NFC_UID)) {
        	Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord uidRecord = msg.getRecords()[0];
            uid = new String(uidRecord.getPayload());
        }
	}
	
	String getUID(){
		return uid;
	}
}
