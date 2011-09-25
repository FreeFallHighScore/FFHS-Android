package com.freefallhighscore.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.freefallhighscore.android.R;

public class SplashActivity extends Activity {
	   /** Called when the activity is first created. */
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.splash);
	      Thread splashThread = new Thread() {
	         @Override
	         public void run() {
	            try {
	               int waited = 0;
	               while (waited < 1000) {
	                  sleep(100);
	                  waited += 100;
	               }
	            } catch (InterruptedException e) {
	               // do nothing
	            } finally {
	               finish();
	               Intent i = new Intent();
	               i.setClassName("com.freefallhighscore.android",
	                              "com.freefallhighscore.android.MainScreen");
	               startActivity(i);
	            }
	         }
	      };
	      splashThread.start();
	   }
}