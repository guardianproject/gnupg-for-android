package info.guardianproject.gpg;

import java.io.IOException;
import java.io.InputStream;

import android.os.Handler;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

public class StreamDisplay extends Thread {
	public static final String TAG = "StreamDisplay";

	InputStream i;
	TextView display;
	ScrollView scrollView;
	Handler handler;

	StreamDisplay(InputStream i, TextView display, ScrollView scrollView, Handler handler) {
		this.i = i;
		this.display = display;
		this.scrollView = scrollView;
		this.handler = handler;
	}

	@Override
	public void run() {
		try {
			byte[] readBuffer = new byte[512];
			int readCount = -1;
			while ((readCount = i.read(readBuffer)) > 0) {
				final String readString = new String(readBuffer, 0, readCount);
				handler.post(new Runnable() {
					public void run() {
						CharSequence currentText = display.getText();
						display.setText(currentText + readString);
						scrollView.scrollTo(0, display.getHeight() + 100);
					}
				});
				handler.postDelayed(new Runnable() {
					public void run() {
						scrollView.scrollTo(0, display.getHeight());
					}
				}, 300);
			}
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
	}
}
