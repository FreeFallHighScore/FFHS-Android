package com.freefallhighscore.android.youtube;

import android.content.Context;

import com.freefallhighscore.android.R;

public class OAuthConfig {
	
	private static OAuthConfig INSTANCE = null;
	
	private final String oauthClientId;
	private final String oauthClientSecret;
	private final String oauthRedirectUri;
	
	private String oauthAuthCode;
	private String oauthAccessToken;
	private String oauthRefreshToken;
	
	private OAuthConfig(String oauthClientId, 
			String oauthClientSecret,
			String oauthRedirectUri, 
			String oauthAuthCode) {
		this.oauthClientId = oauthClientId;
		this.oauthClientSecret = oauthClientSecret;
		this.oauthRedirectUri = oauthRedirectUri;
		this.oauthAuthCode = oauthAuthCode;
	}
	
	public static OAuthConfig getInstance(Context context) {
		if (null == INSTANCE) {
			Context ac = context.getApplicationContext();
			INSTANCE = new OAuthConfig(ac.getString(R.string.oauth_clientId), ac.getString(R.string.oauth_secret), ac.getString(R.string.oauth_redirect_uri), null);
		}
		return INSTANCE;
	}
	
	
	// Read-Only Properties
	public String getOauthClientId() {
		return oauthClientId;
	}
	public String getOauthClientSecret() {
		return oauthClientSecret;
	}
	public String getOauthRedirectUri() {
		return oauthRedirectUri;
	}

	// Read-Write Properties. They should be synchronized
	public synchronized String getOauthAuthorizationCode() {
		return oauthAuthCode;
	}
	public synchronized void setOauthAuthorizationCode(String oauthAuthCode) {
		this.oauthAuthCode = oauthAuthCode;
	}
	public synchronized String getOauthAccessToken() {
		return oauthAccessToken;
	}
	public synchronized void setOauthAccessToken(String oauthAccessToken) {
		this.oauthAccessToken = oauthAccessToken;
	}
	public synchronized String getOauthRefreshToken() {
		return oauthRefreshToken;
	}
	public synchronized void setOauthRefreshToken(String oauthRefreshToken) {
		this.oauthRefreshToken = oauthRefreshToken;
	}
	
	/**
	 * 'Authorized' means we've got an OAuth code from the little webview login
	 * business. So our app is authorized
	 * @return
	 */
	public synchronized boolean isAuthorized() {
		return null != getOauthAuthorizationCode();
	}
	
	/**
	 * If we have an access token, it means we've already exchanged our oauthAuthorizationCode
	 * for a token that allows us to make API calls.  
	 * @return
	 */
	public synchronized boolean hasAccessToken() {
		return null != oauthAccessToken;
	}

	public synchronized void reset() {
		this.oauthAuthCode = null;
		this.oauthAccessToken = null;
		this.oauthRefreshToken = null;
		
	}
	
}
