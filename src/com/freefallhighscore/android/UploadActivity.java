package com.freefallhighscore.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.freefallhighscore.android.youtube.ByteWrittenListener;
import com.freefallhighscore.android.youtube.OAuthConfig;
import com.freefallhighscore.android.youtube.UploadRequestData;
import com.freefallhighscore.android.youtube.YouTubeClient;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.base.Strings;

public class UploadActivity extends Activity implements OnClickListener {
	
	private static final int AUTH_REQ = 1;

	private static final String TAG = "VIDEOCAPTURE";

	OAuthConfig oauthConfig;
	String videoFileName;
	
	Button backButton;
	TextView fallDurationText;
	Button loginLogoutButton;
	
	EditText videoTitleInput;
	EditText videoDescInput;
	Button uploadButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);
		
		// TODO: better way of persisting thing. Probably Application object
		// TODO: should these two guys be in onResume() instead?
		Intent creator = getIntent();
		oauthConfig = (OAuthConfig)creator.getSerializableExtra(OAuthConfig.class.getCanonicalName());
		videoFileName = creator.getStringExtra("videoFileName");
		
		
		setupViewReferences();
		setOnClickListers(backButton, loginLogoutButton, uploadButton);
		
		updateLoginLogoutButtonDisplay();		
	}

	private void setupViewReferences() {
		backButton = (Button) findViewById(R.id.back);
		fallDurationText = (TextView) findViewById(R.id.fall_duration);
		loginLogoutButton = (Button) findViewById(R.id.login_logout_button);
		
		videoTitleInput = (EditText) findViewById(R.id.video_title_input);
		videoDescInput = (EditText) findViewById(R.id.video_desc_input);
		uploadButton = (Button) findViewById(R.id.upload_button);
	}
	
	// technically, probably slow. But...less boilerplate
	private void setOnClickListers(View... views) {
		for (View v : views) {
			v.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == backButton) {
			finish();
		} else if (v == loginLogoutButton) {
			handleLoginLogoutPress();
		} else if (v == uploadButton) {
			if (validateForm()) {
				doUpload();
			}
		}
		
	}

	private void handleLoginLogoutPress() {
		if (null == oauthConfig) {
			// if we're not logged in, let's go log in. And come back here
			startActivityForResult(new Intent(this, AuthActivity.class), AUTH_REQ);
		} else {
			// otherwise log out.
			// TODO: is there anything else that needs to be done here?
			oauthConfig = null;
			updateLoginLogoutButtonDisplay();
		}
	}

	private boolean validateForm() {
		String title = videoTitleInput.getText().toString();
		String desc = videoDescInput.getText().toString();
		
		Log.d(TAG, "Video Title: " + title);
		Log.d(TAG, "Video Desc: " + desc);
		
		if (Strings.isNullOrEmpty(title) || Strings.isNullOrEmpty(desc)) {
			Log.d(TAG, "Failed validation. Not uploading.");
			Toast.makeText(this, "Title and Description are both required to upload.", Toast.LENGTH_LONG);
			return false;
		}
		
		return true;
	}
	
	private void doUpload() {
		String title = videoTitleInput.getText().toString();
		String desc = videoDescInput.getText().toString();
		
		VideoUploadTask uploadRunnable = new VideoUploadTask(
				title, desc, videoFileName, getString(R.string.devId), oauthConfig);
		
		// TODO: what happens if there's no video here?
		
		// TODO: error handling, and all those goodies. 
		// maybe 
		new Thread(uploadRunnable).start();
		
	}

	private static class VideoUploadTask implements Runnable, ByteWrittenListener {
		
		private String title;
		private String desc;
		private String fileName;
		private String devId;
		private OAuthConfig oauthConfig;
		
		public VideoUploadTask(String title, String desc, String fileName,
				String devId, OAuthConfig oauthConfig) {
			super();
			this.title = title;
			this.desc = desc;
			this.fileName = fileName;
			this.devId = devId;
			this.oauthConfig = oauthConfig;
		}

		@Override
		public void run() {
			HttpTransport transport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();
			try {
				YouTubeClient client = YouTubeClient.buildAuthorizedClient(transport, jsonFactory, oauthConfig, devId);
				
				// TODO: update this section with the path to the video file...
				/*File sdCardDir = Environment.getExternalStorageDirectory();
				File videoFileParentDir = new File(sdCardDir, "com.roblg.android.youtubetest");
				File videoFile = new File(videoFileParentDir, "testvideo.m4v");
				*/
				File videoFile = new File(fileName);
				Log.d(TAG, "File name: " + videoFile.getAbsolutePath());
				Log.d(TAG, "Exists? " + videoFile.exists());
				
				totalBytes = videoFile.length();
				Log.d(TAG, "File length: " + totalBytes);
				UploadRequestData data = new UploadRequestData();
				data.title = this.title;
				
				// TODO: what category should this be?
				data.category = "Sports";
				data.description = this.desc;
				data.fileName = URLEncoder.encode(videoFile.getName(), "UTF-8");
				data.fileData = new FileInputStream(videoFile);
				client.executeUpload(data, this);
			} catch (IOException e) {
				// TODO: better error handling
				throw new RuntimeException("Error creating youtube client or uploading", e);
			}
		}

		private long totalBytes = 0;
		private long bytesWritten = 0;
		
		private double lastWritten = 0.0;
		
		@Override
		public void byteWritten() {
			bytesWritten++;
			
			if (bytesWritten % 1000000 == 0) {
				Log.d(TAG, "Bytes Written: " + bytesWritten);
			}
			
			double percentageWritten = bytesWritten / (totalBytes * 1.0);
			percentageWritten = Math.round(percentageWritten) / 10;
			if (percentageWritten > lastWritten) {
				Log.d(TAG, "WOOHOO: " + percentageWritten);
				lastWritten = percentageWritten;
			}
		}
	}
	
	// TODO: this is super-similar to what's in MainScreen.java
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "ON ACTIVITY RESULT");
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case AUTH_REQ:
			if (resultCode == Activity.RESULT_OK) {
				oauthConfig = (OAuthConfig)data.getSerializableExtra(AuthActivity.OAUTH_RESULT_DATA_KEY);
				updateLoginLogoutButtonDisplay();
				Toast.makeText(this, "You are now authed.", Toast.LENGTH_LONG);
			}
			break;
		}
		
	}
	
	private void updateLoginLogoutButtonDisplay() {
		if (null == oauthConfig) {
			loginLogoutButton.setText("Login");
			// prevent upload if we're not logged in
			uploadButton.setText("Log in to upload");
			uploadButton.setEnabled(false);
		} else {
			loginLogoutButton.setText("Logout");
			// if we're logged in, upload will work 
			uploadButton.setText("Upload");
			uploadButton.setEnabled(true);
		}
	}
	
}
