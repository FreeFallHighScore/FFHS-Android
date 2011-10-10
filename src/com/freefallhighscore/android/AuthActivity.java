package com.freefallhighscore.android;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.freefallhighscore.android.youtube.OAuthConfig;
import com.freefallhighscore.android.youtube.YouTubeClient;
import com.freefallhighscore.android.youtube.YouTubeProfile;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

public class AuthActivity extends Activity {
	
	private static final String TAG = "VIDEOCAPTURE";  
	private static final String YOUTUBE_SCOPE = "http://gdata.youtube.com";
	
	WebView webView;
	ProgressDialog workingDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_main);
		
		workingDialog = new ProgressDialog(AuthActivity.this);
		workingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		workingDialog.setMessage("Working...");
		// dlg.setCancelable(false); // TODO: un-cancelable dialogs are scary.
		
		
		// the base URI - http://www.google.com/blah/blah/blah -- no query params
		/*Uri authBaseUri = Uri.parse(getString(R.string.oauth_auth_request_url));
		authBaseUri.buildUpon()
			.appendQueryParameter("client_id", oauthClientId)
			.appendQueryParameter("redirect_uri", oauthRedirectUri)
			.appendQueryParameter("response_type", "code");
		*/
		webView = (WebView)findViewById(R.id.auth_web_view);
		
		final OAuthConfig oauthConfig = OAuthConfig.getInstance(this);
		
		String authorizeUrl = new GoogleAuthorizationRequestUrl(oauthConfig.getOauthClientId(), oauthConfig.getOauthRedirectUri(), YOUTUBE_SCOPE).build();		 
		webView.loadUrl(authorizeUrl);
		
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (url.startsWith(oauthConfig.getOauthRedirectUri()) && null == oauthConfig.getOauthAuthorizationCode()) {
					Uri redirectUri = Uri.parse(url);
					webView.stopLoading();
					
					// update the oauth config with the authorization code
					OAuthConfig.getInstance(AuthActivity.this).setOauthAuthorizationCode(redirectUri.getQueryParameter("code"));
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// hide webview
							webView.setVisibility(View.GONE);
							
							// show dialog, then try to see if the user has a youtube account
							workingDialog.show();
							new Thread(new YouTubeAccountTest(workingDialog)).start();
						}
					});
					
				}
			}
		});
		
		
	}
	
	/**
	 * When we're done doing the auth, set up the result 
	 * and return it to our caller
	 */
	private void doneWithAuth() {
		// then return 'OK'!
		Intent result = new Intent();
		setResult(Activity.RESULT_OK, result);
		finish();
	}
	
	/**
	 * A simple runnable to test whether or not the person who just gave permissions
	 * to the app has a youtube account.  
	 */
	private class YouTubeAccountTest implements Runnable {

		// REMINDER: all access to progressDlg needs to happen in the UI thread
		private final ProgressDialog progressDlg;
		
		public YouTubeAccountTest(ProgressDialog dlg) {
			this.progressDlg = dlg;
		}
		
		@Override
		public void run() {
			try {
				YouTubeClient client = YouTubeClient.buildAuthorizedClient(OAuthConfig.getInstance(AuthActivity.this), getString(R.string.devId));
				YouTubeProfile profile = client.getYouTubeProfile();
				// if these two calls were successful, we're golden!
				// finish it up. 
				
				// Note: this would be a good place to cache that username...
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						doneWithAuth();
					}
				});
			} catch (HttpResponseException e) {
				// show webview with youtube channel create
				HttpResponse resp = e.response;
				if (resp.statusCode == 401 && "NoLinkedYouTubeAccount".equals(resp.statusMessage)) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							webView.loadUrl("https://m.youtube.com/create_channel");
							webView.setVisibility(View.VISIBLE);
							// TODO: what do we do when they're done with this URL?
							progressDlg.hide();
						}
					});
					
				} else {
					Log.e(TAG, "ERROR", e);
					// TODO: handle this
				}
			} catch (IOException e) {
				// EH? WTF?
			}
			
		}
		
	}
	
}
