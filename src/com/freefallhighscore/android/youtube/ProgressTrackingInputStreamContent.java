package com.freefallhighscore.android.youtube;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.api.client.http.AbstractInputStreamContent;

/**
 * Heavily based on package com.google.api.client.http.InputStreamContent. I would have just
 * subclassed that, but it's final... 
 */
public class ProgressTrackingInputStreamContent extends AbstractInputStreamContent {

	public InputStream inputStream;
	public ByteWrittenListener listener;
	
	@Override
	public long getLength() throws IOException {
		// we don't know the length
		return -1;
	}

	@Override
	public boolean retrySupported() {
		return false;
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return inputStream;
	}
	
	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (null != listener) {
			super.writeTo(new CallbackOutputStream(out, listener));
		} else {
			super.writeTo(out);
		}
	}

}
