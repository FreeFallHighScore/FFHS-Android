package com.freefallhighscore.android;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.freefallhighscore.android.R;
 

public class MainScreen extends Activity {
   /** Called when the activity is first created. */
	
	ClipDrawable drawable;
	private Handler mHandler = new Handler();
	TextView tv;
	int slideDirection;
	
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
//      ImageView circle = (ImageView) findViewById(R.id.circle);
//      Animation rotate = AnimationUtils.loadAn	imation(this, R.anim.rotate);
//      circle.startAnimation(rotate);
      
      //tv = (TextView) findViewById(R.id.tv);
      
      
     Button whatBtn  = (Button) findViewById(R.id.what);
     Button loginBtn = (Button) findViewById(R.id.login);
     Button startBtn = (Button) findViewById(R.id.start);
     ImageView logo = (ImageView) findViewById(R.id.logo); 
     
      Animation slide = AnimationUtils.loadAnimation(this, R.anim.slide);
      Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
      whatBtn.startAnimation(slide);
      loginBtn.startAnimation(slide);
      startBtn.startAnimation(slide);
      logo.startAnimation(slideDown);
      
     Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Univers_65_Bold.ttf");
     whatBtn.setTypeface(tf);
     loginBtn.setTypeface(tf);
     startBtn.setTypeface(tf);
     
     ImageView imageview = (ImageView) findViewById(R.id.center);
     drawable = (ClipDrawable) imageview.getDrawable();
     drawable.setLevel(10000);
     slider(-1);

   }
   
   private Runnable mUpdateTimeTask = new Runnable() {
		long start = SystemClock.uptimeMillis();
		public void run() {
			long millis = SystemClock.uptimeMillis() - start;
			drawable.setLevel(drawable.getLevel()  + (500 * slideDirection));
			if(( drawable.getLevel() > 5000 && slideDirection == -1) || ( drawable.getLevel() < 10000 && slideDirection == 1))
			mHandler.postAtTime(this, start + millis);
			else  mHandler.removeCallbacks(mUpdateTimeTask);
			
		}
	};
	
	public void slider(int direction){
		slideDirection = direction;
		mHandler.removeCallbacks(mUpdateTimeTask);  //Remove old timer
	    mHandler.postDelayed(mUpdateTimeTask, 100); //How fast thread updated
	
	}
   
   public void buttonPressed(View view){
	   drawable.setLevel(drawable.getLevel() + 1);
	   Toast.makeText(this, "button pressed", Toast.LENGTH_SHORT).show();
   }
   
	protected void onResume(){
		super.onResume();
	}
	
}