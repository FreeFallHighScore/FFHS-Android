package com.freefallhighscore.android.youtube;

import java.io.IOException;

import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant;
import com.google.api.client.googleapis.json.JsonCContent;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.xml.atom.AtomParser;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.xml.XmlNamespaceDictionary;

// based on:
// http://code.google.com/p/google-api-java-client/source/browse/youtube-jsonc-sample/src/main/java/com/google/api/client/sample/youtube/YouTubeClient.java?repo=samples
public class YouTubeClient {
	
	static final XmlNamespaceDictionary DICTIONARY = new XmlNamespaceDictionary()
		.set("", "http://www.w3.org/2005/Atom")
		.set("media", "http://search.yahoo.com/mrss/")
		.set("batch", "http://schemas.google.com/gdata/batch")
		.set("yt", "http://gdata.youtube.com/schemas/2007")
		.set("gd", "http://schemas.google.com/g/2005");
	
	private static final String TAG = "VIDEOCAPTURE";

	private final JsonFactory jsonFactory;
	private final HttpTransport transport;
	private final HttpRequestFactory requestFactory;
	
	/**
	 * @param transport the transport that's going to send data
	 * @param devId the developer id
	 * @param authHeaderValue the Authorization header value
	 */
	private YouTubeClient(YouTubeProtectedResource requestInitializer) {
		this.jsonFactory = requestInitializer.getJsonFactory();
		this.transport = requestInitializer.getTransport();
		requestFactory = this.transport.createRequestFactory(requestInitializer);
	}
	
	public static YouTubeClient buildAuthorizedClient(OAuthConfig oauthConfig, String devId) throws IOException {
		
		HttpTransport transport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		
		if (null == oauthConfig.getOauthAccessToken()) {
			// if we dont have any access token, we need to authorize
			authorizeAndUpdateOAuth(transport, jsonFactory, oauthConfig);
		}
		
		YouTubeProtectedResource initializer = new YouTubeProtectedResource( 
				transport, 
				jsonFactory, 
				oauthConfig, 
				devId);
		
		return new YouTubeClient(initializer);
	}
	
	private static void authorizeAndUpdateOAuth(HttpTransport transport, JsonFactory jsonFactory, OAuthConfig oauthConfig) throws IOException {
		GoogleAuthorizationCodeGrant authRequest = new GoogleAuthorizationCodeGrant(transport,
				jsonFactory, oauthConfig.getOauthClientId(), oauthConfig.getOauthClientSecret(),
				oauthConfig.getOauthAuthorizationCode(), oauthConfig.getOauthRedirectUri());
		authRequest.useBasicAuthorization = false;
		AccessTokenResponse authResponse  = authRequest.execute();
		oauthConfig.setOauthAccessToken(authResponse.accessToken);
		oauthConfig.setOauthRefreshToken(authResponse.refreshToken);
	}
	
	public YouTubeProfile getYouTubeProfile() throws IOException {
		YouTubeUrl url = YouTubeUrl.forProfile();
		
		HttpRequest request = requestFactory.buildGetRequest(url);
		// blech... because YouTubeProtectedResource extends GoogleAccessProtectedResource and that class
		// make initialize final... we have to set up the stupid headers here
		request.headers = new GoogleHeaders();
		try {
			HttpResponse resp = request.execute();
			return resp.parseAs(YouTubeProfile.class);
		} catch (HttpResponseException e) {
			// TODO: better error handling
			Log.e(TAG, "An error occurred retrieving profile from Youtube", e);
			System.err.println(e.response.parseAsString());
			return null;
		}
	}
	
	public String executeUpload(UploadRequestData data, ByteWrittenListener writeListener) throws IOException {
		// our upload URL is always the same. -- uploading to the logged-in user's feed
		YouTubeUrl url = YouTubeUrl.forUploadRequest();
		HttpRequest request = requestFactory.buildPostRequest(url, getUploadContent(data));
		
		// blech... because YouTubeProtectedResource extends GoogleAccessProtectedResource and that class
		// make initialize final... we have to set up the stupid headers here 
		request.headers = new GoogleHeaders();
		
		// here, HttpRequest already has headers that have the app name and the developer id, but
		// I think we need to add a slug too...
		GoogleHeaders.class.cast(request.headers).slug = data.fileName;
		System.out.println(request.content);
		System.out.println(request.headers);
		
		HttpResponse initialUploadResponse = null;
		String uploadUrl = null;
		try {
			initialUploadResponse = request.execute();
			System.out.println("Resp: " + initialUploadResponse.parseAsString());
			System.out.println("Resp Headers: " + initialUploadResponse.headers.toString());	
			uploadUrl = initialUploadResponse.headers.location;
			System.out.println(uploadUrl);
		} catch (HttpResponseException e) {
			// TODO better error handling
			Log.e(TAG, "An error occurred updating Youtube", e);
			System.out.println(e.response.parseAsString());
		}
		
		if (null != uploadUrl) {
			// ProgressTrackingFileContent content = new ProgressTrackingFileContent(listener);
			ProgressTrackingInputStreamContent content = new ProgressTrackingInputStreamContent();
			content.listener = writeListener;
			content.inputStream = data.fileData;
			
			String fileExtension = MimeTypeMap.getFileExtensionFromUrl(data.fileName);
			Log.d(TAG, "File Extension: " + fileExtension);
			content.type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			Log.d(TAG, "Inferred MIME Type:" + content.type);
			
			HttpRequest actualRequest = requestFactory.buildPutRequest(new GenericUrl(uploadUrl), content);
			actualRequest.headers = new GoogleHeaders();
			try {
				actualRequest.execute().parseAsString();
			} catch (HttpResponseException e) {
				// TODO: better error handling
				Log.e(TAG, "An error occurred uploading to Youtube", e);
				System.err.println(e.response.parseAsString());
			}
		} else {
			System.out.println("URL is null...skipping upload");
		}
		
		return null;
	}
	
	private HttpContent getUploadContent(UploadRequestData data) {
		JsonCContent content = new JsonCContent();
		content.jsonFactory = this.jsonFactory;
		content.data = data;
		return content;
	}
	
	private static class YouTubeProtectedResource extends GoogleAccessProtectedResource {
		
		private final String devId;
		private final OAuthConfig oauthConfig;

		public YouTubeProtectedResource(HttpTransport transport, JsonFactory jsonFactory, OAuthConfig oauthConfig, String devId) {
			super(oauthConfig.getOauthAccessToken(), transport, jsonFactory, oauthConfig.getOauthClientId(), oauthConfig.getOauthClientSecret(), oauthConfig.getOauthRefreshToken());
			this.devId = devId;
			this.oauthConfig = oauthConfig;
		}
		
		// AccessProtectedResource makes initialize() final, so we're taking advantage
		// of the fact that it also functions as a request interceptor...
		@Override
		public void intercept(HttpRequest request) throws IOException {
			// we need GoogleHeaders here, but initialize() is final... so whoever is making the 
			// request has to modify it before executing... grody. 
			GoogleHeaders headers = (GoogleHeaders)request.headers;
			headers.setApplicationName("roblg.com-youtubetest/1.0");
			headers.gdataVersion = "2";
			headers.setDeveloperId(devId);
			request.headers = headers;
			
			// seems like the JSON parser is added by default, but getting
			// the profile uses Atom. It's also a little weird to add the
			// response parser on the request, but that's how it works
			final AtomParser atomParser = new AtomParser();
			atomParser.namespaceDictionary = DICTIONARY;
			request.addParser(atomParser);
			
			// let the superclass do its business
			super.intercept(request);
		}
		
	}
	
	
}