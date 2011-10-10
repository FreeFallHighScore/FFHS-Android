package com.freefallhighscore.android.youtube;

import com.google.api.client.googleapis.GoogleUrl;

// good example of getting auth token for user: http://code.google.com/p/google-api-java-client/source/browse/picasa-atom-android-sample/src/com/google/api/client/sample/picasa/PicasaAndroidSample.java?repo=samples

public class YouTubeUrl extends GoogleUrl {
	
	static final String UPLOAD_ROOT = 
	    "http://uploads.gdata.youtube.com/resumable/feeds/api/users/default/uploads?alt=jsonc&v=2";
	
	/*"https://gdata.youtube.com/feeds/api/videos?" +
	"q=GoogleDevelopers" + 
    "&max-results=1" + 
    "&v=2" + 
    "&alt=jsonc";*/ 
	
	YouTubeUrl(String encodedUrl) {
		super(encodedUrl);
		// this.alt = "jsonc";
	}
	
	/**
	 * Returns a YouTubeUrl suitable for uploading to the logged-in user's account. Making 
	 * a POST to this URL will return a redirect to another URL that the file contents should
	 * be sent to.
	 * @return
	 */
	public static YouTubeUrl forUploadRequest() {
		YouTubeUrl url = new YouTubeUrl(UPLOAD_ROOT);
		return url;
	}
	
	public static YouTubeUrl forProfile() {
		// why no 'alt=jsonc'? Because profiles don't support it. Lame.
		YouTubeUrl url = new YouTubeUrl("https://gdata.youtube.com/feeds/api/users/default?v=2");
		return url;
	}
	
}
