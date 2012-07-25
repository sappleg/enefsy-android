package com.enefsy.main;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class Main extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ImageButton facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        ImageButton twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        ImageButton foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);
        
        facebook_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                        Intent intent = new Intent("android.intent.category.LAUNCHER");
                        intent.setClassName("com.facebook.katana", "com.facebook.katana.LoginActivity");
                        startActivity(intent);
                } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setData(Uri.parse("https://m.facebook.com/?_rdr"));
                    startActivity(intent);
                }
            }
        });

        twitter_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
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
        });
        
        foursquare_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setData(Uri.parse("http://m.foursquare.com/user"));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
