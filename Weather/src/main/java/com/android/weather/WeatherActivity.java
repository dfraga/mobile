package com.android.weather;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class WeatherActivity extends Activity implements WeatherProcessListener {

	private Button applyButton;
	private static ProgressDialog progressDialog;

	private final AtomicBoolean processing = new AtomicBoolean(false);
	private static int progress = 0;
	private Bitmap imBitmap;
	private ImageView imageView;

	private final Runnable processErrorAction = new ExceptionActionRunnable() {};
	final Handler progressThreadHandler = new Handler();
	private long lastProgressUpdate = -1;

	private int maxTotal = 100;
	private int partial = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			imageView = (ImageView) findViewById(R.id.imageView);

			applyButton = (Button) findViewById(R.id.execute);
			applyButton.setEnabled(true);
			applyButton.setVisibility(View.VISIBLE);
			applyButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							applyFilter();
						}
					});


			WeatherActivity.progressDialog = new ProgressDialog(WeatherActivity.this.applyButton.getContext());
			WeatherActivity.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			WeatherActivity.progressDialog.setCancelable(false);
			WeatherActivity.progressDialog.setMessage("Procesando....");

			processing.set(false);
			WeatherActivity.progress = 0;
		} catch (final Exception e) {
			// No compatible file manager.
			processException(e);
		}

	}


	private final Runnable processBeginAction = new Runnable() {

		@Override
		public void run() {
			//Toast.makeText(PhotoColorSwitcherActivity.this, "processBeginAction", Toast.LENGTH_SHORT).show();

			maxTotal = 480*480;//XXX = imBitmap.getWidth() * imBitmap.getHeight();
			applyButton.setEnabled(false);
			applyButton.setVisibility(View.INVISIBLE);
			WeatherActivity.progress = 0;

			WeatherActivity.progressDialog.setMax(100);
			WeatherActivity.progressDialog.show();
		}

	};

	private final static Runnable progressThread = new Runnable() {

		@Override
		public synchronized void run() {
			WeatherActivity.progressDialog.setProgress(WeatherActivity.progress);
		}

	};

	private final Runnable processEndAction = new Runnable() {

		@Override
		public void run() {
			//imBitmap.prepareToDraw();
			imageView.setImageBitmap(imBitmap);
			Toast.makeText(WeatherActivity.this, "Photo processing finished.", Toast.LENGTH_LONG).show();

			processing.set(false);
			lastProgressUpdate = -1;

			applyButton.setVisibility(View.VISIBLE);
			applyButton.setEnabled(true);

			WeatherActivity.progressDialog.hide();
		}

	};

	private class ExceptionActionRunnable implements Runnable {
		private Exception e;
		public void setException(final Exception e) {
			this.e = e;
		}

		@Override
		public void run() {
			processException(e);
		}
	};

	private void applyFilter() {
		//		Toast.makeText(WeatherActivity.this, "applyFilter 1", Toast.LENGTH_SHORT).show();
		if (!processing.getAndSet(true)) {
			//			Toast.makeText(WeatherActivity.this, "applyFilter 2", Toast.LENGTH_SHORT).show();
			try{
				//processing = true;
				progressThreadHandler.post(processBeginAction);
				final Thread processT = new MainProcess(this, progressThreadHandler);
				processT.start();

			} catch(final Exception e){
				((ExceptionActionRunnable) processErrorAction).setException(e);
				progressThreadHandler.post(processErrorAction);
				processException(e);
			} finally {
				progressThreadHandler.post(processEndAction);
			}
		}
	}

	public static String getStackTrace(final String prefix, final Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return prefix + "\n" + result.toString();
	}

	@Override
	public void processException(final Throwable e) {
		final String error = WeatherActivity.getStackTrace(("Error: " + e + " - " + e.getMessage()),e);
		Log.e("error",error);
		Toast.makeText(WeatherActivity.this, error, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void setPercentageProgression(final int x, final int y, final int yMax) {
		if(maxTotal > 0) {
			partial = ((x*yMax) + y) * 100;
			WeatherActivity.progress = partial / maxTotal;

			if( WeatherActivity.progress - lastProgressUpdate > 0) {
				lastProgressUpdate = WeatherActivity.progress;
				progressThreadHandler.post(WeatherActivity.progressThread);
			}
		}
		WeatherActivity.progress =  0;
	}

	@Override
	public void openImage(final Bitmap tempBitmap) {

		if (!applyButton.isEnabled()) {
			applyButton.setEnabled(true);
			applyButton.setVisibility(View.VISIBLE);

		}
		imBitmap = tempBitmap;
		imBitmap.prepareToDraw();
		imageView.setImageBitmap(imBitmap);
		imageView.requestLayout();

	}

	@Override
	public Context getContext() {
		return WeatherActivity.this;
	}

}
