package com.android.utils;

import java.math.BigDecimal;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatPicker extends RelativeLayout {
 
	private static final boolean DEBUG_MODE = false;
	private static final String LOG_IDENTIFIER = "FloatPicker";

	private static final int LABEL_ID = 1;
	private static final int SUBTRACT_BUTTON_ID = 2;
	private static final int TEXT_ID = 3;
	private static final int ADD_BUTTON_ID = 4;

	private static final int DEFAULT_LABEL_WIDTH = 150;
	private static final int DEFAULT_TEXTAREA_WIDTH = 75;
	private static final int DEFAULT_LABEL_HEIGHT = 25;
	private static final int DEFAULT_TEXTAREA_HEIGTH = 25;
	private static final int DEFAULT_BUTTON_HEIGTH = 25;
	private static final int DEFAULT_BUTTON_WIDTH = 25;

	private static final int DEFAULT_TEXT_SIZE = 10;

	private static final float DEFAULT_MINIMUM_VALUE = 0f;
	private static final float DEFAULT_MAXIMUM_VALUE = 100f;
	private static final float DEFAULT_INCREMENT = 1f;
	private static final int DEFAULT_DECIMAL_PLACES = 0;

	private static final int DEFAULT_REPEAT_RATE = 200;
	private static final int DEFAULT_REPEAT_ACCELERATION = 50;
	private static final int MINIMUM_REPEATE_RATE = 25;

	private static final int REPEAT_SUBTRACT = -1;
	private static final int REPEAT_ADD = 1;
	private static final int REPEAT_STOPPED = 0;

	private float value;
	private float defautlValue;

	private float incrementStep;
	private final int decimalPlaces;
	private float max;
	private float min;

	private String pickerLabelText;
	private TextView pickerLabel;
	private TextView editText;
	private Button buttonAdd;
	private Button buttonSubtract;

	private final int labelWidth;
	private final int labelHeight;

	private final int buttonWidth;
	private final int buttonHeight;

	private final int editTextWidth;
	private final int editTextHeight;

	private final int textSize;
	private final int startingRepeatRate;

	private int currentRepeatRate = DEFAULT_REPEAT_RATE;
	private final Handler repeatHandler = new Handler();
	private final int repeatAcceleration;
	private ChangeListener changeListener;
	private final ButtonRepeater buttonRepeater = new ButtonRepeater();

	private int repeatState = 0;

	/**
	 * Default constructor called from Android.
	 * Populates the local variables based on XML attributes configured in layout.
	 * Assigns default values if not specified.
	 * 
	 * @param context  Android Context
	 * @param attributeSet XML attributes
	 */
	public FloatPicker(final Context context, final AttributeSet attributeSet) {
		super(context, attributeSet);

		try {
			min = Float.parseFloat(attributeSet.getAttributeValue(null, "minimum_value"));
		} catch (final Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute minimum_value, using default value. Exception: " + e);
			min = DEFAULT_MINIMUM_VALUE;
		}

		try {
			max = Float.parseFloat(attributeSet.getAttributeValue(null, "maximum_value"));
		} catch (final Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute maximum_value, using default value. Exception: " + e);
			max = DEFAULT_MAXIMUM_VALUE;
		}

		try {
			defautlValue = Float.parseFloat(attributeSet.getAttributeValue(null, "default_value"));
		} catch (final Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute default_value, using default value. Exception: " + e);
			defautlValue = DEFAULT_MINIMUM_VALUE;
		}
		value = defautlValue;

		try {
			incrementStep = Float.parseFloat(attributeSet.getAttributeValue(null, "increment_amount"));
		} catch (final Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute increment_amount, using default value. Exception: " + e);
			incrementStep = DEFAULT_INCREMENT;
		}

		decimalPlaces = attributeSet.getAttributeIntValue(null, "decimal_places", DEFAULT_DECIMAL_PLACES);
		startingRepeatRate = attributeSet.getAttributeIntValue(null, "starting_repeat_rate", DEFAULT_REPEAT_RATE);
		repeatAcceleration = attributeSet.getAttributeIntValue(null, "repeat_acceleration", DEFAULT_REPEAT_ACCELERATION);

		buttonWidth = attributeSet.getAttributeIntValue(null, "button_width", DEFAULT_BUTTON_WIDTH);
		buttonHeight = attributeSet.getAttributeIntValue(null, "button_height", DEFAULT_BUTTON_HEIGTH);

		labelWidth = attributeSet.getAttributeIntValue(null, "label_width", DEFAULT_LABEL_WIDTH);
		labelHeight = attributeSet.getAttributeIntValue(null, "label_height", DEFAULT_LABEL_HEIGHT);

		editTextWidth = attributeSet.getAttributeIntValue(null, "edittext_width", DEFAULT_TEXTAREA_WIDTH);
		editTextHeight = attributeSet.getAttributeIntValue(null, "edittext_height", DEFAULT_TEXTAREA_HEIGTH);

		textSize = attributeSet.getAttributeIntValue(null, "text_size", DEFAULT_TEXT_SIZE);

		pickerLabelText = attributeSet.getAttributeValue(null, "picker_label");
		if(pickerLabelText == null) { pickerLabelText = ""; }

		create(context, attributeSet);

		return;
	}

	/**
	 * Create all the sub widgets and arrange them appropriately.
	 * 
	 * @param context  Android Context
	 * @param attributeSet XML attributes
	 */
	private void create(final Context context, final AttributeSet attributeSet) {

		final TriggerAction addAction = new AddButtonTrigger();
		final TriggerAction subtractAction = new SubtractButtonTrigger();

		buildPickerLabel(context, attributeSet);

		buttonSubtract = buildButton(context, attributeSet, Color.RED, SUBTRACT_BUTTON_ID, REPEAT_SUBTRACT, subtractAction);
		buttonAdd = buildButton(context, attributeSet, Color.GREEN, ADD_BUTTON_ID, REPEAT_ADD, addAction);

		editText = buildEditText(context, attributeSet);

		RelativeLayout.LayoutParams lp;

		lp = new RelativeLayout.LayoutParams(labelWidth, labelHeight);
		lp.addRule(RelativeLayout.CENTER_VERTICAL);
		lp.addRule(RelativeLayout.ALIGN_LEFT);
		addView(pickerLabel, lp);

		lp = new RelativeLayout.LayoutParams(buttonWidth, buttonHeight);
		lp.addRule(RelativeLayout.CENTER_VERTICAL);
		lp.addRule(RelativeLayout.RIGHT_OF, LABEL_ID);
		addView(buttonSubtract, lp);

		lp = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
		lp.addRule(RelativeLayout.CENTER_VERTICAL);
		lp.addRule(RelativeLayout.RIGHT_OF, SUBTRACT_BUTTON_ID);
		addView(editText, lp);

		lp = new RelativeLayout.LayoutParams(buttonWidth, buttonHeight);
		lp.addRule(RelativeLayout.CENTER_VERTICAL);
		lp.addRule(RelativeLayout.RIGHT_OF, TEXT_ID);
		addView(buttonAdd, lp);

	}


	/**
	 * Build a button, and wire the events.
	 * 
	 * Defaults to centered gravity (text is centered inside button).
	 * 
	 * Button text size, button text (label) are configurable through XML.
	 * 
	 * @param context
	 * @param attributeSet Attributes passed from XML configuration.
	 * @param label Label for the button.
	 * @param buttonId Android layout ID of the button.
	 * @param repeatDir Direction to repeat in - REPEAT_ADD or REPEAT_SUBTRACT.
	 * @parm action What TriggerAction to call when button is clicked.
	 * @return Button
	 */
	private Button buildButton(final Context context, final AttributeSet attributeSet, final int color, final int buttonId, final int repeatDir, final TriggerAction action) {

		final Button button = new Button(context, attributeSet);
		button.setTextSize(textSize*2);
		button.setBackgroundColor(color);
		button.setGravity(Gravity.CENTER_VERTICAL);
		button.setId(buttonId);

		// on a long click, start repeating
		button.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(final View arg0) {
				startRepeating(repeatDir);
				return false;
			}
		});

		// when the button is touched
		button.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {

				// immediately trigger the action when they tap the button
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					action.click();
				}

				// on release, stop repeating
				if (event.getAction() == MotionEvent.ACTION_UP) {
					startRepeating(0);
				}
				return false;
			}
		});

		return button;
	}

	/**
	 * Build the text field.
	 * Defaults to centered gravity (text is centered inside widget).
	 * Text size is configurable from XML.
	 * 
	 * Displays a rounded version of the internal 'value', based on
	 * the configured number of decimal places.
	 * 
	 * @param context
	 * @param attributeSet Attributes passed from XML configuration.
	 * @return Text field for value display
	 */
	private TextView buildEditText(final Context context, final AttributeSet attributeSet) {

		if(DEBUG_MODE) {
			Log.d(LOG_IDENTIFIER, "Setting editTextSize to = " + textSize);
		}

		final TextView editText = new TextView(context, attributeSet);
		editText.setTextSize(textSize);
		editText.setId(TEXT_ID);
		editText.setBackgroundColor(Color.WHITE);
		editText.setTextColor(Color.BLACK);
		editText.setGravity(Gravity.CENTER);
		editText.setText(getRoundedValue(defautlValue));
		editText.setFocusable(true);
		editText.setClickable(true);
		editText.setLongClickable(false);
		editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

		editText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				final EditText et = new EditText(context);
				et.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
				Toast.makeText(context, "TODO: workaround para InputMethodManager", Toast.LENGTH_SHORT).show();
				final InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.showSoftInput(v, InputMethodManager.SHOW_FORCED, new EditResultReceiver(getHandler(), et));
				et.requestFocus();

			}
		});

		return editText;
	}

	private class EditResultReceiver extends ResultReceiver {
		private final EditText et;
		public EditResultReceiver(final Handler handler, final EditText et) {
			super(handler);
			this.et = et;
		}

		@Override
		public void onReceiveResult(final int resultCode, final Bundle resultData) {
			final String selected = String.valueOf(et.getTag());
			Toast.makeText(et.getContext(), "Input result" + selected, Toast.LENGTH_LONG).show();
			Float newValue = null;
			try {
				newValue = Float.parseFloat(selected);
			} catch (final Exception e) {
			}
			setValue(newValue != null ? newValue : value);
		}
	}

	private void buildPickerLabel(final Context context, final AttributeSet attributeSet) {

		pickerLabel = new TextView(context, attributeSet);
		pickerLabel.setTextSize(textSize);
		pickerLabel.setId(LABEL_ID);
		pickerLabel.setGravity(Gravity.CENTER);
		pickerLabel.setFocusable(false);
		pickerLabel.setClickable(false);

		setText(pickerLabelText);
	}

	/**
	 * Add to the value by the increment step.
	 */
	protected void add() {
		final float previous = value;
		value = Math.min(max, value + incrementStep);

		if (value != previous) {
			updateAndAlertListener();
		}

		return;
	}

	/**
	 * Subtract from the value by increment step.
	 */
	protected void subtract() {
		final float previous = value;
		value = Math.max(min, value - incrementStep);

		if (value != previous) {
			updateAndAlertListener();
		}

		return;
	}

	/**
	 * Called after the value is changed by the add and subtract methods.
	 * Updates the text field with the human friendly version and fires event listener.
	 */
	private void updateAndAlertListener() {

		editText.setText(getRoundedValue(value));

		if (changeListener != null) {
			changeListener.onValueChange(this, value);
		}
	}

	public void resetValue() {
		value = defautlValue;
		editText.setText(getRoundedValue(value));
	}

	/**
	 * Gets human friendly rounded version of 'value'.
	 * 
	 * @return String Rounded version of 'value' as a String.
	 */
	public String getRoundedValue(final float value) {
		final BigDecimal bigDecimal = new BigDecimal(value).setScale(decimalPlaces, BigDecimal.ROUND_HALF_EVEN);
		return bigDecimal.toString();
	}

	/**
	 * Get the current value. Note this will be the actual float
	 * value which may have slight rounding differences compared
	 * to what is on screen.
	 * 
	 * Use getRoundedValue() to get the pretty value.
	 * 
	 * @return value.
	 */
	public float getValue() {
		return value;
	}

	/**
	 * Set the value. Filters based on minimum and maximum options.
	 * 
	 * @param newValue  New float value to use.
	 * @return Resulting value.
	 */
	public float setValue(float newValue) {
		if (newValue > max) {
			newValue = max;
		}
		if (newValue < min) {
			newValue = min;
		}
		value = newValue;
		editText.setText(String.valueOf(value));
		editText.invalidate();

		if (changeListener != null) {
			changeListener.onValueChange(this, value);
		}

		return value;
	}

	public void setText(final String text) {
		pickerLabelText = text;
		pickerLabel.setText(pickerLabelText);
		if (pickerLabelText != null && pickerLabelText.length() > 0) {
			pickerLabel.setVisibility(View.VISIBLE);
		} else {
			pickerLabel.setVisibility(View.INVISIBLE);
		}
	}
	/**
	 * Interface for objects to be notified of changes to the value.
	 * 
	 */
	public interface ChangeListener {
		public void onValueChange(FloatPicker fpw, float value);
	}

	/**
	 * Set the listener to be notified of changes to the value.
	 * 
	 * @param listener
	 *            listener to be called.
	 */
	public void onValueChange(final ChangeListener listener) {
		changeListener = listener;
		return;
	}

	/**
	 * Start auto repeating an increment/or decrement. Will stop when this is
	 * called with a mode of 0.
	 * 
	 * @param mode Corresponds to REPEAT_ADD, REPEAT_SUBTRACT, or REPEAT_STOPPED
	 */
	private void startRepeating(final int mode) {
		repeatState = mode;
		if (repeatState != 0) {
			repeatHandler.postDelayed(buttonRepeater, currentRepeatRate);
		}
		return;
	}

	private class ButtonRepeater implements Runnable {
		public void run() {
			trigger();

			if(DEBUG_MODE) {
				Log.d(LOG_IDENTIFIER, "currentRepeatRate = " + currentRepeatRate);
			}

			if (repeatState != REPEAT_STOPPED) {
				repeatHandler.postDelayed(buttonRepeater, currentRepeatRate);
				currentRepeatRate -= repeatAcceleration;
				if (currentRepeatRate < MINIMUM_REPEATE_RATE) {
					currentRepeatRate = MINIMUM_REPEATE_RATE;
					//when the floor is hit, go 2x
					trigger();
				}
			} else { // Restore to default repeat rate
				currentRepeatRate = startingRepeatRate;
			}
			return;
		}

		/**
		 * Changes the value in the direction based on repeatState.
		 */
		private void trigger() {
			if (repeatState == REPEAT_ADD) {
				add();
			}
			if (repeatState == REPEAT_SUBTRACT) {
				subtract();
			}
		}
	}

	/**
	 * Interface provides a pass through to button click events.
	 * Used during button setup.
	 */
	public interface TriggerAction {
		public void click();
	}

	/**
	 * Triggers the add() method.
	 */
	private class AddButtonTrigger implements TriggerAction {
		public void click() {
			add();
		}
	}

	/**
	 * Triggers the subtract() method.
	 */
	private class SubtractButtonTrigger implements TriggerAction {
		public void click() {
			subtract();
		}
	}


}
