package com.android.photoColorSwitcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.utils.ColorUtils;
import com.android.utils.FloatPicker;

public class PhotoColorSwitcherActivity extends Activity {

	private final static Logger LOG = Logger.getLogger(PhotoColorSwitcherActivity.class.getName());

	protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;
	protected static final int REQUEST_CODE_GET_CONTENT = 2;
	private Button applyButton;
	private Button openButton;
	private Button saveButton;

	private ProgressDialog progressDialog;

	private boolean processing = false;
	private int progress = 0;
	private Bitmap imBitmap;
	private ImageView imageView;
	private FloatPicker huePicker;
	private FloatPicker saturationPicker;
	private FloatPicker valuePicker;

	private final Runnable processErrorAction = new ExceptionActionRunnable() {};
	final Handler progressThreadHandler = new Handler();
	private long lastProgressUpdate = -1;

	private int maxTotal = 100;
	private int partial = 0;

	private String filePath;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			imageView = (ImageView) findViewById(R.id.imageView);

			applyButton = (Button) findViewById(R.id.apply_filter);
			applyButton.setEnabled(false);
			applyButton.setVisibility(View.INVISIBLE);
			applyButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							applyFilter();
						}
					});
			openButton = (Button) findViewById(R.id.get_content);
			openButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							getContent();
						}
					});
			saveButton = (Button) findViewById(R.id.save_content);
			saveButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							saveContent();
						}
					});


			progressDialog = new ProgressDialog(PhotoColorSwitcherActivity.this.applyButton.getContext());
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.setMessage("Procesando....");

			huePicker = (FloatPicker) findViewById(R.id.huePicker);
			saturationPicker = (FloatPicker) findViewById(R.id.saturationPicker);
			valuePicker = (FloatPicker) findViewById(R.id.valuePicker);

			huePicker.setVisibility(View.INVISIBLE);
			saturationPicker.setVisibility(View.INVISIBLE);
			valuePicker.setVisibility(View.INVISIBLE);

			processing = false;
			progress = 0;
		} catch (final Exception e) {
			// No compatible file manager.
			processException(e);
		}
		getContent();
	}

	public void getContent() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(intent, PhotoColorSwitcherActivity.REQUEST_CODE_GET_CONTENT);
		} catch (final ActivityNotFoundException e) {
			Toast.makeText(this, "No compatible file manager was found.", Toast.LENGTH_SHORT).show();
		}
	}

	private final Runnable processBeginAction = new Runnable() {

		@Override
		public void run() {
			//Toast.makeText(PhotoColorSwitcherActivity.this, "processBeginAction", Toast.LENGTH_SHORT).show();

			maxTotal = imBitmap.getWidth() * imBitmap.getHeight();
			openButton.setEnabled(false);
			applyButton.setEnabled(false);
			saveButton.setEnabled(false);
			openButton.setVisibility(View.INVISIBLE);
			applyButton.setVisibility(View.INVISIBLE);
			saveButton.setVisibility(View.INVISIBLE);
			progress = 0;

			progressDialog.setMax(100);
			progressDialog.show();
		}

	};

	private final Runnable progressThread = new Runnable() {

		@Override
		public synchronized void run() {
			progressDialog.setProgress(progress);
		}

	};

	private final Runnable processEndAction = new Runnable() {

		@Override
		public void run() {
			//imBitmap.prepareToDraw();
			imageView.setImageBitmap(imBitmap);
			Toast.makeText(PhotoColorSwitcherActivity.this, "Photo processing finished.", Toast.LENGTH_LONG).show();

			processing = false;
			lastProgressUpdate = -1;
			huePicker.resetValue();
			saturationPicker.resetValue();
			valuePicker.resetValue();

			openButton.setVisibility(View.VISIBLE);
			applyButton.setVisibility(View.VISIBLE);
			saveButton.setVisibility(View.VISIBLE);
			openButton.setEnabled(true);
			applyButton.setEnabled(true);
			saveButton.setEnabled(true);

			progressDialog.hide();
			//			if(progressDialog.isShowing()) {
			//				progressDialog.cancel();
			//			}
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
		if(imBitmap != null) {
			if (!processing) {

				final Thread processT = new Thread(){
					@Override
					public void run() {
						try{
							processing = true;
							progressThreadHandler.post(processBeginAction);
							final Bitmap tempBitmap = imBitmap.copy(imBitmap.getConfig(), true);

							final float heueValue = huePicker.getValue();
							final float saturationValue = saturationPicker.getValue();
							final float valueValue = valuePicker.getValue();

							for(int x=0; x < imBitmap.getWidth(); x++)
							{
								for(int y=0; y < imBitmap.getHeight(); y++)
								{
									final int baseColor = imBitmap.getPixel(x, y);

									int filteredColor = ColorUtils.getHuePhasedColor(baseColor, heueValue);
									filteredColor = ColorUtils.getSaturationPhasedColor(filteredColor, saturationValue);
									filteredColor = ColorUtils.getValuePhasedColor(filteredColor, valueValue);
									tempBitmap.setPixel(x, y, filteredColor);

									progress = percentageProgression(x, y, imBitmap.getHeight());
									if( progress - lastProgressUpdate > 0) {
										lastProgressUpdate = progress;
										progressThreadHandler.post(progressThread);
									}

								}
							}
							imBitmap = tempBitmap;
						} catch(final Exception e){
							((ExceptionActionRunnable) processErrorAction).setException(e);
							progressThreadHandler.post(processErrorAction);
						} finally {
							progressThreadHandler.post(processEndAction);
						}
					}
				};
				processT.start();
			}
		}
	}

	private int percentageProgression(final int x, final int y, final int yMax) {
		if(maxTotal > 0) {
			partial = ((x*yMax) + y) * 100;
			return partial / maxTotal;
		}
		return 0;
	}

	public static String getStackTrace(final String prefix, final Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return prefix + "\n" + result.toString();
	}

	private void processException(final Throwable e) {
		final String error = PhotoColorSwitcherActivity.getStackTrace(("Error: " + e + " - " + e.getMessage()),e);
		PhotoColorSwitcherActivity.LOG.log(Level.SEVERE, error);
		Toast.makeText(PhotoColorSwitcherActivity.this, error, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		try {
			if(data != null && requestCode == PhotoColorSwitcherActivity.REQUEST_CODE_GET_CONTENT && resultCode == Activity.RESULT_OK) {
				String filePath = null;
				final Uri uri = data.getData();
				final Cursor c = getContentResolver().query(uri, new String[] {MediaStore.MediaColumns.DATA,
						MediaStore.MediaColumns.MIME_TYPE,
						MediaStore.MediaColumns.DISPLAY_NAME,
						MediaStore.MediaColumns.SIZE
				}, null, null, null);
				if (c != null && c.moveToFirst()) {
					final int id = c.getColumnIndex(MediaColumns.DATA);
					if (id != -1) {
						filePath = c.getString(id);
					}
					// displayName = c.getString(2);
					// fileSize = c.getLong(3);
				}
				if (filePath != null) {
					//Toast.makeText(this, filePath, Toast.LENGTH_LONG).show();
					openImage(filePath);
				}
			}
		} catch (final Exception e) {
			// No compatible file manager was found.
			processException(e);
		}
	}

	private void openImage(final String filePath) {
		final File imgFile = new  File(filePath);
		if(imgFile.exists()){
			this.filePath = filePath;

			if (!applyButton.isEnabled()) {
				applyButton.setEnabled(true);
				applyButton.setVisibility(View.VISIBLE);

				huePicker.setVisibility(View.VISIBLE);
				saturationPicker.setVisibility(View.VISIBLE);
				valuePicker.setVisibility(View.VISIBLE);
			}
			imBitmap = BitmapFactory.decodeFile(filePath);
			imageView.setImageBitmap(imBitmap);

		}

	}

	private void saveContent() {
		String prefixPath = filePath.substring(0, filePath.lastIndexOf("."));
		String sufixPath = ".png";

		for(int i = 0; i< Integer.MAX_VALUE; i++) {
			String tempFilePath = prefixPath + "_" + i + sufixPath;
			final File imgFile = new  File(tempFilePath);
			if(!imgFile.exists()){
				Toast.makeText(this, filePath, Toast.LENGTH_LONG).show();
				//Save imBitmap

				try {
					Toast.makeText(this, "Saved file: " + tempFilePath, Toast.LENGTH_LONG).show();
					FileOutputStream out = new FileOutputStream(tempFilePath);
					imBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
					out.flush();
					out.close();
				} catch (Exception e) {
					processException(e);
				}

				//Actualizar galeria
				MediaScannerConnection.scanFile(getApplicationContext(), new String[] {tempFilePath}, null, null);

				break;
			}
		}
	}

}
