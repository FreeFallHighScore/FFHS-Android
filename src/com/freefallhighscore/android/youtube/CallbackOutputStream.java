package com.freefallhighscore.android.youtube;

import java.io.IOException;
import java.io.OutputStream;

public class CallbackOutputStream extends OutputStream {

	private final OutputStream wrappedStream;
	private final ByteWrittenListener listener;
	
 	public CallbackOutputStream(OutputStream wrappedStream, ByteWrittenListener listener) {
		this.wrappedStream = wrappedStream;
		this.listener = listener;
	}
	
	@Override
	public void write(int b) throws IOException {
		wrappedStream.write(b);
		listener.byteWritten();
	}

}
