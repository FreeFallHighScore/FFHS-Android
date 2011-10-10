package com.freefallhighscore.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.freefallhighscore.android.youtube.OAuthConfig;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;

public class AuthActivity extends Activity {
	
	
	private static final String YOUTUBE_SCOPE = "http://gdata.youtube.com";
	
	WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_main);
		
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
					
					// then return 'OK'!
					Intent result = new Intent();
					setResult(Activity.RESULT_OK, result);
					finish();
				}
			}
		});
	}
	
}
