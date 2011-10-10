package com.freefallhighscore.android;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainScreen extends Activity implements SurfaceHolder.Callback, SensorEventListener, OnClickListener {
	/** Called when the activity is first created. */

	//STATE MACHINE:
	public enum GameState {
	    kFFStateJustOpened,
	    kFFStateReadyToDrop,
	    kFFStatePreDropRecording,
	    //kFFStatePreDropCancelling,
	    kFFStatePreDropCanceled,
	    kFFStateInFreeFall,
	    kFFStateFinishedDropPostroll,
	    //kFFStateFinishedDropVideoPlaybackFirstLoop,
	    kFFStateFinishedDropVideoPlayback,
	    kFFStateFinishedDropScoreView,
	};

	protected GameState state; 
	
	private static final int AUTH_REQ = 1;
	private static final int UPLOAD_VIDEO = 2;
	
	protected float currentDrawerLevel;
	protected float freefallDuration;
	
	ClipDrawable drawable;
	private Handler mHandler = new Handler();
	protected TextView go, scoreText;
	int slideDirection, slideTarget, slideOrigin, slideDistance;
	Button whatBtn, loginBtn, startBtn, cancelBtn, tempStartFall, tempStopFall;
	Button submitBtn, replayBtn, deleteBtn;
	
	ImageView logo, logoWhite, logoBlack, circle, record;
	
	//Spinner videoLoadSpinner;
	View drawer; 
	ImageView leftStripes, rightStripes, centerStripes;
	Animation rotate;

	// Accelerometer
	SensorManager sensorManager;
	Sensor accelerometer;

	// Camera variables
	MediaRecorder recorder;
	MediaPlayer player;
	SurfaceHolder holder;

	File OutputFile = new File(Environment.getExternalStorageDirectory().getPath());
	String videoDir = "/DCIM/100MEDIA/Video";

	boolean recording = false;
	public static final String TAG = "VIDEOCAPTURE";

	int count= 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// UI Elements + Animations
//		circle = (ImageView) findViewById(R.id.circle);
		rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);

		drawer 		= (View) findViewById(R.id.drawer);
		go 			= (TextView) findViewById(R.id.go);
		logo 		= (ImageView) findViewById(R.id.logo); 
		logoWhite 	= (ImageView) findViewById(R.id.logo_white);
		leftStripes = (ImageView) findViewById(R.id.left);
		rightStripes= (ImageView) findViewById(R.id.right);
		
		record 		= (ImageView) findViewById(R.id.recording); 
		startBtn 	= (Button) findViewById(R.id.start);
		loginBtn	 = (Button) findViewById(R.id.login_btn);
		loginBtn.setOnClickListener(this);
		
		cancelBtn 	= (Button) findViewById(R.id.cancel);
		
		scoreText	= (TextView) findViewById(R.id.score);
		submitBtn	= (Button) findViewById(R.id.submit);
		replayBtn	= (Button) findViewById(R.id.replay);
		deleteBtn	= (Button) findViewById(R.id.delete);
		
		// temporary buttons - to be replaced by accelerometer business
		tempStartFall = (Button) findViewById(R.id.tempStartFall); 
		tempStartFall.setOnClickListener(this);
		
		tempStopFall = (Button) findViewById(R.id.tempStopFall); 
		tempStopFall.setOnClickListener(this);
		
		//videoLoadSpinner = (Spinner)findViewById(R.id.videoPlaybackSpinner);

		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Univers_65_Bold.ttf");
		startBtn.setTypeface(tf);
		cancelBtn.setTypeface(tf);
		go.setTypeface(tf);
		scoreText.setTypeface(tf);
		
		centerStripes = (ImageView) findViewById(R.id.center);
		drawable = (ClipDrawable) centerStripes.getDrawable();
		drawable.setLevel(10000);

		state = GameState.kFFStateJustOpened;
		currentDrawerLevel = 0.0f;
		
		freefallDuration = 1.646f; //fake duration
		
		changeState(GameState.kFFStateReadyToDrop);
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

	//ANIMATION METHODS
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
	
	protected void revealFromLeft(View view){
		revealFromLeft(view, 1500);
	}
	
	protected void revealFromLeft(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, -1,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);
	}
	
	protected void revealFromRight(View view){
		revealFromRight(view, 500);
	}

	protected void revealFromRight(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 1,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);
	}
	
	protected void hideToLeft(View view){
		hideToLeft(view, 500);
	}
	
	protected void hideToLeft(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, -1,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);
	}
	
	protected void hideToRight(View view){
		hideToRight(view, 500);
	}
	
	protected void hideToRight(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 1,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);
	}
	
	protected void revealElementFromTop(View view){
		revealElementFromTop(view, 500);
	}
	
	protected void revealElementFromTop(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, -1,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);	
	}

	protected void hideElementToTop(View view){
		hideElementToTop(view, 500);
	}
	
	protected void hideElementToTop(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, -1);
		
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);	
	}
	
	protected void hideStripes(){
		rightStripes.setVisibility(View.GONE);
		leftStripes.setVisibility(View.GONE);
		centerStripes.setVisibility(View.GONE);
	}
	
	protected void showStripes(){
		rightStripes.setVisibility(View.VISIBLE);
		leftStripes.setVisibility(View.VISIBLE);
		centerStripes.setVisibility(View.VISIBLE);
	}
/*
	public void cropSlide(int direction, int target){
		slideDirection = direction;
		slideTarget  = (int) (10000.0 - target/100.0*7500.0);
		Log.i("State", "target: " +slideTarget);
		slideOrigin = drawable.getLevel();
		slideDistance = Math.abs(slideTarget-slideOrigin);
		mHandler.removeCallbacks(mUpdateTimeTask);  //Remove old timer
		mHandler.postDelayed(mUpdateTimeTask, 100); //How fast thread updated
	}
*/

	public void bringDrawerToLevel(float newLevel){
		slideDirection = newLevel > currentDrawerLevel ? -1 : 1;
		slideTarget  = (int) (10000.0 - newLevel*7500.0);
		Log.i("State", "target: " +slideTarget);
		slideOrigin = drawable.getLevel();
		slideDistance = Math.abs(slideTarget-slideOrigin);
		
		mHandler.removeCallbacks(mUpdateTimeTask);  //Remove old timer
		mHandler.postDelayed(mUpdateTimeTask, 100); //How fast thread updated
		
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f + currentDrawerLevel,
				Animation.RELATIVE_TO_PARENT, -1.0f + newLevel);
		
		slide.setDuration(500);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		drawer.startAnimation(slide);
		currentDrawerLevel = newLevel;
	}

	public void buttonPressed(View view){
		drawable.setLevel(drawable.getLevel() + 1);
		Toast.makeText(this, "button pressed", Toast.LENGTH_SHORT).show();
	}

	protected void onResume(){
		super.onResume();
		Log.d(TAG, "RESUMING MainScreen");
		// register the accelerometer. Let's get lots of updates. We can tone it down if
		// need be.
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		
		// TODO: we probably shouldn't be re-init-ing the recorder here
		// unless we actually want the camera to be on (if we were on
		// the submit page, we don't)
		
		// TODO FIXME XXX calling initRecorder here overwrites a pre-existing video
		// with an empty file. To repro: record a drop, go to the submit page, click 'back',
		// click 'submit' again and upload. It will be 0-byte file. 
		
		// Camera Init
		recorder = new MediaRecorder();
		initRecorder();
		
	}

	protected void onPause() {
		super.onPause();

		// make sure to always unregister the sensor...
		sensorManager.unregisterListener(this);

		recorder.release();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "ON ACTIVITY RESULT");
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case AUTH_REQ:
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "You are now authed.", Toast.LENGTH_LONG);
				loginBtn.setVisibility(View.GONE);
			}
			break;
		case UPLOAD_VIDEO:
			if (resultCode == Activity.RESULT_OK) {
				// TODO: show success message here instead of the 'just opened' state
				changeState(GameState.kFFStateJustOpened);
				
			}
			break;
		}
		
	}

	public void changeState(GameState toState){
		switch(toState){
			case kFFStateJustOpened:
			break;    
			
			case kFFStateReadyToDrop:
				if(state == GameState.kFFStateFinishedDropScoreView){
					hideToLeft(submitBtn);
					hideToLeft(replayBtn);
					hideToRight(deleteBtn);
					hideElementToTop(scoreText);
				}
				//slide(startBtn,-100,0,0,0,1000);
				revealFromLeft(startBtn, 750);
				revealFromLeft(loginBtn, 1000); //TODO make conditional on login status
				
				slide(logo,0,0,-100,0,1000);
				slide(drawer,0,0,-100,-20,1000);
				
				bringDrawerToLevel(.8f);
				
				tempStartFall.setVisibility(View.GONE);
				
			break;
			
			case kFFStatePreDropRecording:
				//cropSlide(1, 30);
				//slide(logo,0,0,0,-100,1000);
				hideElementToTop(logo);
				bringDrawerToLevel(.3f);
				slide(drawer,0,0,0,-70,1000);
				//slide(startBtn,0,-100,0,0,1000);
				hideToLeft(loginBtn);
				hideToLeft(startBtn);
				
				cancelBtn.setVisibility(View.VISIBLE);
				//slide(cancelBtn,100,0,0,0,1000);
				revealFromRight(cancelBtn);
				
				go.setVisibility(View.VISIBLE);
				//slide(go,0,0,-100,0,1000);
				revealElementFromTop(go);
				
				record.setVisibility(View.VISIBLE);
				//slide(record,0,0,-100,0,1000);
				revealElementFromTop(record);
				
				//TODO: add drop circle
				//circle.setVisibility(0);
				//circle.startAnimation(rotate);
				//slide(circle,0,0,100,0,1000);
				
				// TODO replace these controls with the accelerometer
				tempStartFall.setVisibility(View.VISIBLE);
			break;
				
			//case kFFStatePreDropCancelling:
			//break;
			
			case kFFStatePreDropCanceled:
				slide(logo,0,0,-100,0,1000);
				//slide(drawer,0,0,-100,-20,1000);				
				//cropSlide(-1, 80);
				bringDrawerToLevel(.8f);
				
				hideElementToTop(record);
				hideElementToTop(go);
				
				hideToRight(cancelBtn);
				revealFromLeft(loginBtn);
				revealFromLeft(startBtn);
				
				tempStartFall.setVisibility(View.GONE);
				
			break;
			
			case kFFStateInFreeFall:
				hideElementToTop(record);
				hideElementToTop(go);
				bringDrawerToLevel(0);
				hideStripes();
				hideToRight(cancelBtn);
			break;
			
			case kFFStateFinishedDropPostroll:
				//slide(drawer,0,0,0,-20,1000);				
				//cropSlide(-1, 80);
				showStripes();
				
			break;
						
			case kFFStateFinishedDropVideoPlayback:
				hideStripes();
				if(state == GameState.kFFStateFinishedDropScoreView){
					hideToLeft(submitBtn);
					hideToLeft(replayBtn);
					hideToRight(deleteBtn);
					bringDrawerToLevel(0.0f);
					hideElementToTop(scoreText);
				}				
			break;
			
			case kFFStateFinishedDropScoreView:
				scoreText.setVisibility(View.VISIBLE);
				revealElementFromTop(scoreText);
				
				//TODO: populate score text accurately
				
				showStripes();
				bringDrawerToLevel(.2f);
				submitBtn.setVisibility(View.VISIBLE);
				replayBtn.setVisibility(View.VISIBLE);
				deleteBtn.setVisibility(View.VISIBLE);
				revealFromLeft(submitBtn);
				revealFromLeft(replayBtn);
				revealFromRight(deleteBtn);
			break;
			
		}
		state = toState;
	}
	
	public void startRecording(View view){
		changeState(GameState.kFFStatePreDropRecording);
		recording = true;
		recorder.start();	
	}

	private void initRecorder() {
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		CamcorderProfile cpHigh = CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH);
		recorder.setProfile(cpHigh);

		recorder.setOutputFile(getVideoFilePath());
		//recorder.setOutputFile("/sdcard/videocapture_example.mp4");
		recorder.setMaxDuration(1200000); // 120 seconds
		recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
	}

	private String getVideoFilePath() {
		String videoDirPath = OutputFile.getAbsolutePath() + videoDir;
		File videoDir = new File(videoDirPath);
		if (!videoDir.exists()) {
			// if the video directory doesn't exist yet, create
			// it. We want all the parent directories of the dir
			// to exist as well
			videoDir.mkdirs();
		}
		return videoDir.getAbsolutePath() + count + ".mov";
	}
	
	private void prepareRecorder() {
		recorder.setPreviewDisplay(holder.getSurface());

		try {
			recorder.prepare();
			Log.i("State", "Recorder Prepared");

		} catch (IllegalStateException e) {
			Log.e(TAG, "Error preparing recorder", e);
			finish();
		} catch (IOException e) {
			Log.e(TAG, "Error preparing recorder", e);
			finish();
		}
	}


	// Surface Holder Implementation
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		prepareRecorder();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		if (recording) {
			recorder.stop();
			recording = false;
		}
		recorder.release();
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View v) {
		if (v == tempStartFall) {
			fakeStartPress();
		}
		else if(v == tempStopFall){
			fakeStopPress();
		} else if (v == loginBtn) {
			startActivityForResult(new Intent(this, AuthActivity.class), AUTH_REQ);
		}
	}

	private void fakeStartPress() {
		tempStartFall.setVisibility(View.GONE);
		tempStopFall.setVisibility(View.VISIBLE);
		//videoLoadSpinner.setVisibility(View.VISIBLE);
		fallBegan();
	}
	
	private void fakeStopPress() {
		tempStopFall.setVisibility(View.GONE);
		//videoLoadSpinner.setVisibility(View.VISIBLE);
		fallEnded();
	}

	//falling methods
	private void fallBegan() {
		//TODO: START TIMING
		
		//TODO: Animate-hide all interface elements
		changeState(GameState.kFFStateInFreeFall);
	}
	
	private void fallEnded() {
		//TODO: find a way to wait .5s before stopping the record		
		Log.i("RECORD_TAGS", "FALL ENDED");
		
		Thread t = new Thread(new Runnable() {
			public void run() {
				try{
					Thread.sleep(500);	
				}
				catch(InterruptedException e){
					//IGNORE
				}
				finishRecording();
			}
		});
		
		t.start();
	}
	
	private void finishRecording(){
		Log.i("RECORD_TAGS", "RECORDING FINISHED");
		
		recorder.stop();
		recording = false;

		playbackClip();
		
		runOnUiThread( new Runnable(){
			public void run() {
				changeState(GameState.kFFStateFinishedDropPostroll);
			}
		});
	}
	
	protected void playbackClip(){
		// synchronous: bad
		// player = MediaPlayer.create(getApplicationContext(), Uri.parse(getVideoFilePath()));
		player = new MediaPlayer();
		
		try {
			player.setDataSource(getVideoFilePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		player.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.d(TAG, "Length of video: " + mp.getDuration() / 1000.0);
				// hide the spinner
				//videoLoadSpinner.setVisibility(View.GONE);
				Log.i("RECORD_TAGS", "VIDEO LOADED + PLAY");
				// set a completion listener, and release the player
				// when we're done playing
				mp.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						mp.reset();
						mp.release();
						Log.i("RECORD_TAGS", "VIDEO FINISHED");
						runOnUiThread( new Runnable(){
							public void run() {
								changeState(GameState.kFFStateFinishedDropScoreView);
							}
						});
					}
				});
				
				mp.start();
				
				runOnUiThread( new Runnable(){
					public void run() {
						changeState(GameState.kFFStateFinishedDropVideoPlayback);
					}
				});
			}
		});
		
		player.setDisplay(holder);
		player.prepareAsync();		
	}
	
	//SCORE VIEW ACTIONS
	public void replayClip(View view){
		//TODO: replay clip
		playbackClip();
	}
	
	public void deleteClip(View view){
		//TODO: delete clip
		recorder.reset();
		initRecorder();
		prepareRecorder();
		changeState(GameState.kFFStateReadyToDrop);
	}
	
	public void submitClip(View view){
		Intent i = new Intent(getApplicationContext(), UploadActivity.class);
		i.putExtra("videoFileName", getVideoFilePath());
		startActivityForResult(i, UPLOAD_VIDEO);
	}
	
	public void cancelDrop(View view){
		recorder.stop();
		recording = false;
		initRecorder();
		prepareRecorder();
		changeState(GameState.kFFStatePreDropCanceled);
	}
	
	
}