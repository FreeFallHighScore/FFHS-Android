package com.freefallhighscore.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainScreen extends Activity implements SurfaceHolder.Callback, SensorEventListener, OnClickListener {
	/** Called when the activity is first created. */

	//STATE MACHINE:
	public enum GameState {
	    kFFStateJustOpened,
	    kFFStateReadyToDrop,
	    kFFStatePreDropRecording,
	    kFFStateInFreeFall,
	    kFFStateFinishedDropPostroll,
	    kFFStateFinishedDropVideoPlayback,
	    kFFStateFinishedDropScoreView,
	    kFFStateFinishedUpload
	};

	public class Accel {
		Accel(float[] v, long t){
			this.t = t; 
			this.x = v[0];
			this.y = v[1];
			this.z = v[2];
		}
		public long t;
		public float x;
		public float y;
		public float z;
	};
	
	protected GameState state; 
	
	private static final int AUTH_REQ = 1;
	private static final int UPLOAD_VIDEO = 2;
	private static final int TABS_REQ = 3;
	
	protected float currentDrawerLevel;
	
	ClipDrawable drawable;
	private Handler mHandler = new Handler();
	protected TextView go, scoreText, successText;
	private boolean loggedIn;
	
	int slideDirection, slideTarget, slideOrigin, slideDistance;
	Button loginBtn, whatBtn, startBtn, cancelBtn, tempStartFall, tempStopFall, info;
	Button submitBtn, replayBtn, deleteBtn, playAgainBtn;
	
	ImageView mainLogo, sideLogo, logoBlack, circle, record;
	
	//Spinner videoLoadSpinner;
	View drawer; 
	ImageView leftStripes, rightStripes, centerStripes;
    ImageView[] circles = new ImageView[7];
	Animation rotateFwd, rotateRvs;
	RelativeLayout wheel;

	// Accelerometer
	SensorManager sensorManager;
	Sensor accelerometer;

	// Camera variables
	MediaRecorder recorder;
	MediaPlayer player;
	SurfaceHolder holder;

	File OutputFile = new File(Environment.getExternalStorageDirectory().getPath());
	String videoDir = "/DCIM/100MEDIA/Video";
	
	int circlesToHide;
	
	boolean isPaused = true;
	boolean recording = false;
	boolean surfaceCreated = false;
	boolean recorderIsShown = false;
	public static final String TAG = "VIDEOCAPTURE";

	int count = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//TODO: try to restore login
		loggedIn = false;
		
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// UI Elements + Animations
//		circle = (ImageView) findViewById(R.id.circle);

		drawer 		= (View) findViewById(R.id.drawer);
		go 			= (TextView) findViewById(R.id.go);
		mainLogo 	= (ImageView) findViewById(R.id.mainLogo); 
		sideLogo 	= (ImageView) findViewById(R.id.sideLogo);
		leftStripes = (ImageView) findViewById(R.id.left);
		rightStripes= (ImageView) findViewById(R.id.right);
		
		record 		= (ImageView) findViewById(R.id.recording); 
		startBtn 	= (Button) findViewById(R.id.start);
		loginBtn	 = (Button) findViewById(R.id.login_btn);
		loginBtn.setOnClickListener(this);
		whatBtn 	= (Button) findViewById(R.id.what_btn);
		cancelBtn 	= (Button) findViewById(R.id.cancel);
		
		scoreText	= (TextView) findViewById(R.id.score);
		submitBtn	= (Button) findViewById(R.id.submit);
		replayBtn	= (Button) findViewById(R.id.replay);
		deleteBtn	= (Button) findViewById(R.id.delete);
		
		playAgainBtn = (Button) findViewById(R.id.playagain);
		successText = (TextView) findViewById(R.id.success);
		info = (Button) findViewById(R.id.info);
		
		// Configure Rotate Animations 
		wheel = (RelativeLayout) findViewById(R.id.wheel);
        rotateFwd = AnimationUtils.loadAnimation(this, R.anim.rotate_fwd);
        rotateRvs = AnimationUtils.loadAnimation(this, R.anim.rotate_rvs);

        initWheel();
		
		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Univers_65_Bold.ttf");
		whatBtn.setTypeface(tf);
		loginBtn.setTypeface(tf);
		startBtn.setTypeface(tf);
		cancelBtn.setTypeface(tf);
		deleteBtn.setTypeface(tf);
		playAgainBtn.setTypeface(tf);
		info.setVisibility(View.GONE);
		
		submitBtn.setTypeface(tf);
		replayBtn.setTypeface(tf);
		
		go.setTypeface(tf);
		scoreText.setTypeface(tf);
		successText.setTypeface(tf);
		
		centerStripes = (ImageView) findViewById(R.id.center);
		drawable = (ClipDrawable) centerStripes.getDrawable();
		drawable.setLevel(10000);

		state = GameState.kFFStateJustOpened;
		currentDrawerLevel = 0.0f;
		
		freefallDuration = 0; //fake duration
		if(loadClipData()){
			changeState(GameState.kFFStateFinishedDropScoreView);
		}
		else{
			changeState(GameState.kFFStateReadyToDrop);	
		}
	}

	
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			
            
			/*
			drawable.setLevel(drawable.getLevel()  + (slideDistance/10 * slideDirection));
			if(( drawable.getLevel() > slideTarget && slideDirection == -1) || ( drawable.getLevel() < slideTarget && slideDirection == 1)){
				mHandler.postAtTime(this, start + millis);
			}
			else{
				
			}
			*/
		}
	};
	
	//ANIMATION METHODS
	protected void revealFromLeft(Button button){
		button.setEnabled(true);
		revealFromLeft(button, 250);
	}
	
	protected void revealFromLeft(View view){
		revealFromLeft(view, 250);
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
	
	protected void revealFromRight(Button button){
		button.setEnabled(true);
		revealFromRight(button, 250);
	}

	protected void revealFromRight(View view){
		revealFromRight(view, 250);
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

	protected void hideToLeft(Button button){
		button.setEnabled(false);
		hideToLeft(button, 250);
	}
	
	protected void hideToLeft(View view){
		hideToLeft(view, 250);
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

	protected void hideToRight(Button button){
		button.setEnabled(false);
		hideToRight(button, 250);
	}

	protected void hideToRight(View view){
		hideToRight(view, 250);
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

	protected void revealElementFromTop(Button button){
		button.setEnabled(true);
		revealElementFromTop(button, 250);
	}
	
	protected void revealElementFromTop(View view){
		revealElementFromTop(view, 250);
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
	
	protected void revealElementFromBottom(View view){
		revealElementFromTop(view, 250);
	}
	
	protected void revealElementFromBottom(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 1,
				Animation.RELATIVE_TO_PARENT, 0);
		slide.setDuration(duration);
		slide.setFillAfter(true);

		// AnimationSet animations = new AnimationSet(true); 
		view.startAnimation(slide);	
	}


	protected void hideElementToTop(Button button){
		button.setEnabled(false);
		hideElementToTop(button, 250);
	}
	
	protected void hideElementToTop(View view){
		hideElementToTop(view, 250);
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
	
	protected void hideElementToBottom(View view){
		hideElementToTop(view, 250);
	}
	
	protected void hideElementToBottom(View view, int duration){
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 0,
				Animation.RELATIVE_TO_PARENT, 1);
		
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
	protected void initWheel(){
        Integer circleId[] = {
        		/*R.drawable.wheel_0,*/ R.drawable.wheel_1, R.drawable.wheel_2, R.drawable.wheel_3, R.drawable.wheel_4, R.drawable.wheel_5, R.drawable.wheel_6, R.drawable.wheel_7
        };
        for(int i =0; i<circles.length; i++){
        	circles[i] = new ImageView(this); 
        	circles[i].setImageResource(circleId[i]);
        	wheel.addView(circles[i]);
        	Animation rotator;
        	if(i%2==0) rotator = rotateFwd;
        	else rotator = rotateRvs;
        	rotator.setDuration(i*1000);
        	circles[i].startAnimation(rotator);
        }
	}

	public void hideWheelsBasedOnTimer(){
		Log.i("State", "to hide " + circlesToHide );
		for(int i = 0; i < circlesToHide; i++){
			circles[i].clearAnimation();
			circles[i].setVisibility(View.GONE);
			
		}
		for(int i = circlesToHide; i < circles.length; i++){
			circles[i].setVisibility(View.VISIBLE);
		}
	}
	public void bringDrawerToLevel(float newLevel){
		slideDirection = newLevel > currentDrawerLevel ? -1 : 1;
	
		slideOrigin = drawable.getLevel();
		slideDistance = Math.abs(slideTarget-slideOrigin);
		
		TranslateAnimation slide = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f + currentDrawerLevel,
				Animation.RELATIVE_TO_PARENT, -1.0f + newLevel);
		
		slide.setDuration(250);
		slide.setFillAfter(true);

		drawer.startAnimation(slide);
		currentDrawerLevel = newLevel;
	}

	public void buttonPressed(View view){
		drawable.setLevel(drawable.getLevel() + 1);
		Toast.makeText(this, "button pressed", Toast.LENGTH_SHORT).show();
	}

	protected void onResume(){
		super.onResume();
		if(isPaused){
			isPaused = false;
				
			Log.d(TAG, "RESUMING MainScreen");
			
			// register the accelerometer. Let's get lots of updates. We can tone it down if
			// need be.
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
			
			//Log.i("ACCEL", "Turning on ACCEL");
			
			// TODO: we probably shouldn't be re-init-ing the recorder here
			// unless we actually want the camera to be on (if we were on
			// the submit page, we don't)
			
			// TODO FIXME XXX calling initRecorder here overwrites a pre-existing video
			// with an empty file. To repro: record a drop, go to the submit page, click 'back',
			// click 'submit' again and upload. It will be 0-byte file. 
			
			ensureRecorderShown();
		}
	}

	protected void onPause() {
		super.onPause();
		
		isPaused = true;
		// make sure to always unregister the sensor...
		sensorManager.unregisterListener(this);
		
		destroyRecorder(true);
		
		if(state != GameState.kFFStateFinishedDropScoreView){
			changeState(GameState.kFFStateReadyToDrop);
		}
	}

	protected void ensureRecorderShown(){
		Log.i("RECORDER", "CREATE recorder for state " + state);
		if(recorder == null){
			recorder = new MediaRecorder();
			initRecorder();
			Log.i("RECORDER", "	was null, initing");
			recorderIsShown = false; //obviously isn't shown
		}
		if(!recorderIsShown){
			prepareRecorder();
			Log.i("RECORDER", "	wasnt showing, showing");
		}
		Log.i("RECORDER", "CREATE Finished creating record");
	}
	
	protected void destroyRecorder(boolean fullyRelease){
		if(recorder != null){
			Log.i("RECORDER", "DESTROYING recorder for state " + state);
			if(recording){
				recorder.stop();
				recording = false;
				Log.i("RECORDER", "	was recording now stopped");
			}
			else if(recorderIsShown){
				recorder.reset();
				recorderIsShown = false;
				Log.i("RECORDER", "	resetting and removing shown");
			}
			
			//and release the recorder
			if(fullyRelease){
				recorder.release();
				Log.i("RECORDER", "	fully released");
			}
			
			recorder = null;
			
			Log.i("RECORDER", "DESTROYING Finished destroy record");
		}		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "ON ACTIVITY RESULT");
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case AUTH_REQ:
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "You are now authed.", Toast.LENGTH_LONG);
				//loginBtn.setVisibility(View.GONE);
				hideToRight(sideLogo);
				hideToLeft(loginBtn);
				hideToLeft(whatBtn);
				mainLogo.setVisibility(View.VISIBLE);
				revealElementFromTop(mainLogo);
				info.setVisibility(View.VISIBLE);
				revealFromRight(info);
				
				loggedIn = true;
			}
			break;
		case UPLOAD_VIDEO:
			if (resultCode == Activity.RESULT_OK) {
				changeState(GameState.kFFStateFinishedUpload);
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
					revealFromLeft(startBtn);
				}
				if(state == GameState.kFFStateFinishedUpload){
					hideToRight(playAgainBtn);
					hideElementToTop(successText);
					hideElementToTop(scoreText);
					revealFromLeft(startBtn);
				}
				if(state == GameState.kFFStatePreDropRecording){
					hideToRight(cancelBtn);
					hideElementToTop(record);
					hideElementToTop(go);
					showStripes();
					revealElementFromTop(startBtn);

					destroyRecorder(false);
				}
				
				if(state == GameState.kFFStateJustOpened){
					revealFromLeft(startBtn);
				}
				
				clearClipData();
				
				wheel.setVisibility(View.GONE);					
				
				ensureRecorderShown();
				
				if(loggedIn){
					mainLogo.setVisibility(View.VISIBLE);
					revealElementFromTop(mainLogo);
					info.setVisibility(View.VISIBLE);
					revealFromRight(info);
				}
				else{
					mainLogo.setVisibility(View.GONE);
					sideLogo.setVisibility(View.VISIBLE);
					revealFromRight(sideLogo);
					revealFromLeft(whatBtn);
					revealFromLeft(loginBtn);
				}
								
				bringDrawerToLevel(.8f);
			break;
			
			case kFFStatePreDropRecording:
				recording = true;
				recorder.start();	

				if(loggedIn){
					hideElementToTop(mainLogo);
				}
				else{
					hideToLeft(whatBtn);
					hideToLeft(loginBtn);
					hideToRight(sideLogo);
				}				
				hideElementToTop(startBtn);
				
				bringDrawerToLevel(.25f);
				
				cancelBtn.setVisibility(View.VISIBLE);
				revealFromRight(cancelBtn);
				
				go.setVisibility(View.VISIBLE);
				revealElementFromTop(go);
				
				record.setVisibility(View.VISIBLE);
				revealElementFromTop(record);
				
				hideToRight(info);
				wheel.setVisibility(View.VISIBLE);

				hideStripes();
								
			break;

			
			case kFFStateInFreeFall:
				hideElementToTop(record);
				hideElementToTop(go);
				bringDrawerToLevel(0);
				hideToRight(cancelBtn);
				wheel.setVisibility(View.GONE);
				
			break;
			
			case kFFStateFinishedDropPostroll:
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
				if(state == GameState.kFFStateJustOpened){
					hideToLeft(whatBtn,0);
					hideToLeft(loginBtn,0);
					hideToRight(sideLogo,0);
					hideToLeft(startBtn);
					hideElementToTop(mainLogo);
				}

				scoreText.setVisibility(View.VISIBLE);
				revealElementFromTop(scoreText);
				
				//TODO: populate score text accurately
				scoreText.setText("" + (freefallDuration / 1000.0) + "s" );
				showStripes();
				bringDrawerToLevel(.25f);
				
				submitBtn.setVisibility(View.VISIBLE);
				replayBtn.setVisibility(View.VISIBLE);
				deleteBtn.setVisibility(View.VISIBLE);
				
				revealFromLeft(submitBtn);
				revealFromLeft(replayBtn);
				revealFromRight(deleteBtn);
			break;

			case kFFStateFinishedUpload:
				
				submitBtn.setVisibility(View.GONE);
				replayBtn.setVisibility(View.GONE);
				deleteBtn.setVisibility(View.GONE);
				
				hideToLeft(submitBtn);
				hideToLeft(replayBtn);
				hideToRight(deleteBtn);
				
				bringDrawerToLevel(.25f);
				
				playAgainBtn.setVisibility(View.VISIBLE);
				successText.setVisibility(View.VISIBLE);
				revealFromRight(playAgainBtn);
				revealElementFromTop(successText);
				
			break;
		}
		state = toState;
	}
	
	public void startRecording(View view){
		belowThreshold = false;
		distanceAccum = 0;
		startTimestampOfDrop = 0;
		recordStartTimeStamp = lastAccel.t;
		freefallDuration = 0;

		mHandler.removeCallbacks(mUpdateTimeTask);  //Remove old timer
		mHandler.postDelayed(mUpdateTimeTask, 500); //How fast thread updated

		changeState(GameState.kFFStatePreDropRecording);
	}

	private void initRecorder() {
		if(recorder == null){
			recorder = new MediaRecorder();
		}

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
		if(!surfaceCreated){
			return;
		}
		
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
		recorderIsShown= true;
	}


	// Surface Holder Implementation
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		surfaceCreated = true;
		if(state != GameState.kFFStateFinishedDropScoreView){
			ensureRecorderShown();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		surfaceCreated = false;
		destroyRecorder(true);
		
	}


	@Override
	public void onClick(View v) {
		if (v == loginBtn) {
			startActivityForResult(new Intent(this, AuthActivity.class), AUTH_REQ);
		}
	}

	//falling methods
	private void fallBegan() {
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
		
		changeState(GameState.kFFStateFinishedDropPostroll);
		
		t.start();
	}
	
	private void finishRecording(){
		Log.i("RECORD_TAGS", "RECORDING FINISHED");
		
		//recorder.stop();
		//recording = false;
		destroyRecorder(true);
		
		playbackClip();
		
		writeClipData();
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
	
	public void writeClipData() {
		String eol = System.getProperty("line.separator");
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					openFileOutput("FFHSVideoKey", MODE_PRIVATE)));
			writer.write("" + freefallDuration + eol);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean loadClipData() {		
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(
					openFileInput("FFHSVideoKey")));
			String line = input.readLine(); 
			if(line == "clear"){
				return false;
			}
			freefallDuration = (new Long(line)).longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;	
	}
	
	public void clearClipData(){
		String eol = System.getProperty("line.separator");
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					openFileOutput("FFHSVideoKey", MODE_PRIVATE)));
			writer.write("clear" + eol);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	//SCORE VIEW ACTIONS
	public void replayClip(View view){
		playbackClip();
	}
	
	public void deleteClip(View view){
		clearClipData();
		changeState(GameState.kFFStateReadyToDrop);
	}
	
	public void submitClip(View view){
		Intent i = new Intent(getApplicationContext(), UploadActivity.class);
		i.putExtra("videoFileName", getVideoFilePath());
		i.putExtra("freefallDuration", freefallDuration);
		startActivityForResult(i, UPLOAD_VIDEO);
	}
	
	public void cancelDrop(View view){
		changeState(GameState.kFFStateReadyToDrop);
	}
	
	public void playAgain(View view){
		changeState(GameState.kFFStateReadyToDrop);
	}
	
	public void showInfo(View view){
		startActivityForResult(new Intent(this, TabsActivity.class), TABS_REQ);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	//vars for calculating freefall
	protected boolean belowThreshold;
	protected long startTimestampOfDrop;
	protected long recordStartTimeStamp;
	protected float distanceAccum;
	protected long freefallDuration;
	protected Accel lastAccel;

	protected static final float kFFFallTimeThreshold = .12f * 1000.0f;
	protected static final float kFFFallStartMinForceThreshold  = 0.347f * SensorManager.GRAVITY_EARTH;
	protected static final float kFFDistanceDecay = 1.33f * SensorManager.GRAVITY_EARTH;
	protected static final float kFFImpactThreshold  = 6.34f;
	protected static final long kRecordingTimeout = 20 * 1000;
	
	@Override
	public void onSensorChanged(SensorEvent event) {

		//Log.i("ACCEL", "t: " + event.timestamp + " x: " + event.values[0] + "y: " + event.values[1] + " z: " + event.values[2]);
		Accel a = new Accel(event.values, event.timestamp);

		if(this.lastAccel != null){

			//Log.i("ACCEL", "rate " + 1000 / (a.t - lastAccel.t) / 1000000.0f  );
			double accelMagnitude = Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z);
			if(state == GameState.kFFStatePreDropRecording){
				
				//TEST FOR START
				if (accelMagnitude < kFFFallStartMinForceThreshold) {
					if(!belowThreshold){
						Log.i("ACCEL", "Below THRESHOLD" + accelMagnitude);
				        belowThreshold = true;
				        startTimestampOfDrop = a.t;
				    }
				    else {
				    	float timeFalling = (a.t - startTimestampOfDrop)/1000000.0f; 
				    	Log.i("ACCEL", "Time falling " + timeFalling + " / " + kFFFallTimeThreshold);
				        if ( timeFalling > kFFFallTimeThreshold ) {                            
				        	fallBegan(); //IN FREEFALL!
				            return;
				        }
				    }
				}
			    else {
			    	belowThreshold = false;
			    }
				


			   //TEST FOR TIMEOUT
				float timeSinceRecord = (a.t - recordStartTimeStamp)/1000000.0f;
               if( timeSinceRecord > kRecordingTimeout){
            	   //Log.i("ACCEL", "TIMEOUT " + ( timeSinceRecord  + " / " + kRecordingTimeout )  );
                   cancelDrop(cancelBtn);
               }
               else{
               		int thisCirclesToHide =  (int) (circles.length * (timeSinceRecord / kRecordingTimeout) );
               		if(thisCirclesToHide != circlesToHide){
               			circlesToHide = thisCirclesToHide;
               			//Log.i("CIRCLES", "TO HIDE " + thisCirclesToHide);
               			runOnUiThread( new Runnable(){
               				public void run() {
               					hideWheelsBasedOnTimer();
               				}
   	   					});
               		}
               }
			}
			else if(state == GameState.kFFStateInFreeFall){
				
                float dX = this.lastAccel.x - a.x;
                float dY = this.lastAccel.y - a.y;
                float dZ = this.lastAccel.z - a.z;
                
                double deltaForce = Math.sqrt(dX*dX + dY*dY + dZ*dZ);
                
                distanceAccum += deltaForce;
                float deltaT = (a.t - this.lastAccel.t)/1000000000.0f;
                distanceAccum -= deltaT*kFFDistanceDecay;
                distanceAccum = Math.max(0, distanceAccum);
                if(distanceAccum > kFFImpactThreshold){
                	freefallDuration = (long)(a.t - startTimestampOfDrop)/1000000;
                	fallEnded();
                }
                
                //Log.i("ACCEL", "FALLING delta force: " + deltaForce + " distance accum " + distanceAccum + " / " + kFFImpactThreshold);

            }
		}
		this.lastAccel = a;
	}


}