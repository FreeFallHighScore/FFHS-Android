package com.freefallhighscore.android;

import java.io.File;
import java.io.IOException;


import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainScreen extends Activity implements SurfaceHolder.Callback{
   /** Called when the activity is first created. */
	
	ClipDrawable drawable;
	private Handler mHandler = new Handler();
	TextView go;
	int slideDirection, slideTarget, slideOrigin, slideDistance;
	Button whatBtn, loginBtn, startBtn, cancelBtn;
	ImageView logo, logoWhite, circle, record;
	View drawer;
	Animation rotate;
	
	
	// Camera variables
	MediaRecorder recorder;
	SurfaceHolder holder;
	
	File OutputFile = new File(Environment.getExternalStorageDirectory().getPath());
	String video= "/DCIM/100MEDIA/Video";
	
	boolean recording = false;
	public static final String TAG = "VIDEOCAPTURE";
	int count= 0;
	
   @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Camera Init
		recorder = new MediaRecorder();
		initRecorder();
		setContentView(R.layout.main);

		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// UI Elements + Animations
      
        circle = (ImageView) findViewById(R.id.circle);
        rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        
      	go = (TextView) findViewById(R.id.go);
		startBtn = (Button) findViewById(R.id.start);
		cancelBtn = (Button) findViewById(R.id.cancel);
		logo = (ImageView) findViewById(R.id.logo); 
		logoWhite = (ImageView) findViewById(R.id.logo_white);
		drawer = (View) findViewById(R.id.drawer);
		record =(ImageView) findViewById(R.id.recording); 
		
	      
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Univers_65_Bold.ttf");
        startBtn.setTypeface(tf);
        cancelBtn.setTypeface(tf);
        go.setTypeface(tf);
		     
		ImageView imageview = (ImageView) findViewById(R.id.center);
		drawable = (ClipDrawable) imageview.getDrawable();
		drawable.setLevel(10000);
		
		    
		slide(startBtn,-100,0,0,0,1000);
		slide(logo,0,0,-100,0,1000);
		slide(drawer,0,0,-100,-20,1000);
		cropSlide(-1, 80);
		
		

   }
   
   private Runnable mUpdateTimeTask = new Runnable() {
		long start = SystemClock.uptimeMillis();
		public void run() {
			long millis = SystemClock.uptimeMillis() - start;
			drawable.setLevel(drawable.getLevel()  + (slideDistance/10 * slideDirection));
			if(( drawable.getLevel() > slideTarget && slideDirection == -1) || ( drawable.getLevel() < slideTarget && slideDirection == 1))
			mHandler.postAtTime(this, start + millis);
			else  mHandler.removeCallbacks(mUpdateTimeTask);
		}
	};
	
	public void cropSlide(int direction, int target){
		slideDirection = direction;
		slideTarget  = (int) (10000.0 - target/100.0*7500.0);
		Log.i("State", "target: " +slideTarget);
		slideOrigin = drawable.getLevel();
		slideDistance = Math.abs(slideTarget-slideOrigin);
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
	
	public void slide(View view, float fromX, float toX, float fromY, float toY, int duration){
        TranslateAnimation slide = new TranslateAnimation(
        		Animation.RELATIVE_TO_PARENT, fromX/100,
        		Animation.RELATIVE_TO_PARENT, toX/100,
        		Animation.RELATIVE_TO_PARENT, fromY/100,
        		Animation.RELATIVE_TO_PARENT, toY/100);
        slide.setDuration(duration);
        slide.setFillAfter(true);

       // AnimationSet animations = new AnimationSet(true); 
        view.startAnimation(slide);
	}
	
	public void startDrop(View view){
		cropSlide(1, 30);
		slide(startBtn,0,-100,0,0,1000);
		slide(logo,0,0,0,-100,1000);
		slide(drawer,0,0,0,-70,1000);
		cancelBtn.setVisibility(0);
		slide(cancelBtn,100,0,0,0,1000);
		
		go.setVisibility(0);
		slide(go,0,0,-100,0,1000);
		
		record.setVisibility(0);
		slide(record,0,0,-100,0,1000);
		
		circle.setVisibility(0);
		circle.startAnimation(rotate);
		slide(circle,0,0,100,0,1000);
		
		
		
	}
	
	public void cancelDrop(View view){
		
	
	}
	
	private void initRecorder() {
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		CamcorderProfile cpHigh = CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH);
		recorder.setProfile(cpHigh);

		recorder.setOutputFile(OutputFile.getAbsolutePath()+video+count+".mp4");
		//recorder.setOutputFile("/sdcard/videocapture_example.mp4");
		recorder.setMaxDuration(50000); // 50 seconds
		recorder.setMaxFileSize(50000000); // Approximately 5 megabytes
	}
	
	private void prepareRecorder() {
		recorder.setPreviewDisplay(holder.getSurface());

		try {
			recorder.prepare();
			Log.i("State", "Recorder Prepared");

		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}
	}
	
	
	// Surface Holder Implimentation
	
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		prepareRecorder();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		if (recording) {
			recorder.stop();
			recording = false;
		}
		recorder.release();
		finish();
	}
	
	
}