package com.android.quicknections;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.android.utils.ColorUtils;

public class QuickNectionsActivity extends Activity {

	private final static Logger LOG = Logger.getLogger(QuickNectionsActivity.class.getName());

	private BluetoothAdapter mBluetoothAdapter;
	private WifiManager mWifiManager;
	private CheckBox checkBlue;
	private CheckBox checkWifi;
	private CheckBox check3G;



	private final static Map<Integer, String> WIFI_STATE_NAMES = new HashMap<Integer, String>();
	{
		QuickNectionsActivity.WIFI_STATE_NAMES.put(WifiManager.WIFI_STATE_ENABLED, "WIFI ENABLED");
		QuickNectionsActivity.WIFI_STATE_NAMES.put(WifiManager.WIFI_STATE_ENABLING, "WIFI ENABLING");
		QuickNectionsActivity.WIFI_STATE_NAMES.put(WifiManager.WIFI_STATE_DISABLING, "WIFI DISABLING");
		QuickNectionsActivity.WIFI_STATE_NAMES.put(WifiManager.WIFI_STATE_DISABLED, "WIFI DISABLED");
		QuickNectionsActivity.WIFI_STATE_NAMES.put(WifiManager.WIFI_STATE_UNKNOWN, "WIFI DISABLED");
	}

	private final static Map<Integer, String> BLUE_STATE_NAMES = new HashMap<Integer, String>();
	{
		QuickNectionsActivity.BLUE_STATE_NAMES.put(BluetoothAdapter.STATE_ON, "BLUETOOTH ENABLED");
		QuickNectionsActivity.BLUE_STATE_NAMES.put(BluetoothAdapter.STATE_TURNING_ON, "BLUETOOTH ENABLING");
		QuickNectionsActivity.BLUE_STATE_NAMES.put(BluetoothAdapter.STATE_TURNING_OFF, "BLUETOOTH DISABLING");
		QuickNectionsActivity.BLUE_STATE_NAMES.put(BluetoothAdapter.STATE_OFF, "BLUETOOTH DISABLED");
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button offButton = (Button) findViewById(R.id.offButtonId);
		offButton.setOnClickListener(new OnButtonClickListener(R.id.offButtonId));
		offButton.setBackgroundColor(Color.TRANSPARENT);
		offButton.setTextColor(Color.WHITE);

		//		final CalendarView calendar = (CalendarView) findViewById(R.id.calendario);
		//		calendar.setFirstDayOfWeek(Calendar.MONDAY);

		checkBlue = (CheckBox) findViewById(R.id.checkBlue);
		checkWifi = (CheckBox) findViewById(R.id.checkWifi);
		check3G = (CheckBox) findViewById(R.id.check3G);

		checkBlue.setOnClickListener(new OnButtonClickListener(R.id.checkBlue));
		checkWifi.setOnClickListener(new OnButtonClickListener(R.id.checkWifi));
		check3G.setOnClickListener(new OnButtonClickListener(R.id.check3G));

		checkBlue.setOnCheckedChangeListener(new OnButtonCheckedListener());
		checkWifi.setOnCheckedChangeListener(new OnButtonCheckedListener());
		check3G.setOnCheckedChangeListener(new OnButtonCheckedListener());

		//Final
		initConnectorAdapterListeners();
	}

	private void initConnectorAdapterListeners() {

		QuickNectionsActivity.LOG.info("@@@@@@ initConnectorAdapterListeners");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			checkBlue.setEnabled(false);
		} else {
			final IntentFilter filter = new IntentFilter(QuickNectionsActivity.EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION);
			registerReceiver(new MBroadCastReceiver(), filter);
		}

		mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		if(mWifiManager == null) {
			checkWifi.setEnabled(false);
		} else {
			final IntentFilter filter = new IntentFilter(QuickNectionsActivity.EXTENDED_WIFI_STATE_CHANGED_ACTION);
			registerReceiver(new MBroadCastReceiver(), filter);
		}

		//TODO 3G,4G...

		paint();
	}

	private synchronized void paint() {
		QuickNectionsActivity.LOG.info("@@@@@@ paint");
		paintBlue();
		paintWifi();
	}


	private class OnButtonCheckedListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(final CompoundButton buttonView,
				final boolean isChecked) {
			paint();
		}

	}

	private class OnButtonClickListener implements OnClickListener {
		private final int buttonId;

		@SuppressWarnings("unused")
		private OnButtonClickListener(){
			this.buttonId = 0;
		}

		public OnButtonClickListener(final int buttonId) {
			super();
			this.buttonId = buttonId;
		}

		@Override
		public synchronized void onClick(final View view) {
			switch (this.buttonId) {
			case R.id.checkBlue:
			{
				QuickNectionsActivity.LOG.info("@@@@@@ click on button - checkBlue");
				final boolean realConnectedState = (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON);
				if(checkBlue.isSelected()) {
					if(realConnectedState) {
						mBluetoothAdapter.disable();
					}
				} else {
					if(!realConnectedState) {
						mBluetoothAdapter.enable();
					}
				}
			}
			break;
			case R.id.checkWifi:
			{
				QuickNectionsActivity.LOG.info("@@@@@@ click on button - checkWifi");
				final boolean realConnectedState = mWifiManager.isWifiEnabled();
				if(checkWifi.isSelected()) {
					if(realConnectedState) {
						mWifiManager.setWifiEnabled(false);
					}
				} else {
					if(!realConnectedState) {
						mWifiManager.setWifiEnabled(true);
					}
				}
			}
			break;
			case R.id.check3G:
			{
				QuickNectionsActivity.LOG.info("@@@@@@ click on button - check3G");
				//TODO Edge,4G,3G,2G,GPRS
			}
			break;

			case R.id.offButtonId:
			{
				QuickNectionsActivity.LOG.info("@@@@@@ click on button - offButton");
				finish();
			}
			break;

			default:
				break;
			}

			paint();

			//XXX test colores
			final int baseColor = defaultColors[ counter++ % defaultColors.length ];
			final int backgroundColor = ColorUtils.getAlphaColor(100, baseColor);
			final int shadowColor = ColorUtils.getInvertedColor(backgroundColor);
			specialCheckState(check3G, null, backgroundColor, shadowColor);
		}
	}

	private int counter = 0;
	private final int[] defaultColors = new int[] {Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GRAY, Color.GREEN,
			Color.LTGRAY, Color.MAGENTA, Color.RED, Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.YELLOW};

	private int paintBlue() {
		if(!checkBlue.isEnabled()) {
			return -1;
		}

		boolean checked = false;
		int baseColor = ColorUtils.getInvertedColor(checkBlue.getTextColors().getDefaultColor());

		final int state = mBluetoothAdapter.getState();
		QuickNectionsActivity.LOG.info("@@@@@@ blue state: " + state);
		switch (state) {
		case BluetoothAdapter.STATE_ON:
			baseColor = Color.GREEN;
			checked = true;
			break;

		case BluetoothAdapter.STATE_TURNING_ON:
			baseColor = Color.CYAN;
			checked = true;
			break;

		case BluetoothAdapter.STATE_TURNING_OFF:
			baseColor = Color.GRAY;
			checked = false;
			break;

		default:
			//OFF
			baseColor = Color.BLACK;
			checked = false;
			break;
		}

		final int backgroundColor = ColorUtils.getAlphaColor(100, baseColor);
		final int shadowColor = ColorUtils.getInvertedColor(backgroundColor);

		specialCheckState(checkBlue, checked, backgroundColor, shadowColor);
		return state;
	}

	private int paintWifi() {
		if(!checkWifi.isEnabled()) {
			return -1;
		}

		boolean checked = false;
		int baseColor = ColorUtils.getInvertedColor(checkWifi.getTextColors().getDefaultColor());

		final int state = mWifiManager.getWifiState();
		QuickNectionsActivity.LOG.info("@@@@@@ wifi state: " + state);

		switch (state) {
		case WifiManager.WIFI_STATE_ENABLED:
			baseColor = Color.GREEN;
			checked = true;
			break;

		case WifiManager.WIFI_STATE_ENABLING:
			baseColor = Color.CYAN;
			checked = true;
			break;

		case WifiManager.WIFI_STATE_DISABLING:
			baseColor = Color.GRAY;
			checked = false;
			break;

		default:
			//OFF
			baseColor = Color.RED;
			checked = false;
			break;
		}

		final int backgroundColor = ColorUtils.getAlphaColor(100, baseColor);
		final int shadowColor = ColorUtils.getInvertedColor(backgroundColor);

		specialCheckState(checkWifi, checked, backgroundColor, shadowColor);
		return state;
	}

	private void specialCheckState (final CheckBox checkBox, final Boolean checked, final int backgroundColor, final int shadowColor) {
		checkBox.setBackgroundColor(backgroundColor);
		checkBox.setShadowLayer(1.0f, -1.0f, -1.0f, shadowColor);
		if(checked != null) {
			checkBox.setChecked(checked);
		}
	}

	private static final String EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION = BluetoothAdapter.ACTION_STATE_CHANGED;
	private static final String EXTENDED_WIFI_STATE_CHANGED_ACTION = WifiManager.EXTRA_WIFI_STATE;

	private class MBroadCastReceiver extends BroadcastReceiver {

		public MBroadCastReceiver(){
			super();
		}

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			String stateName = null;
			QuickNectionsActivity.LOG.info("@@@@@@ action: " + action + " - intent " + intent);
			if (action.equals (QuickNectionsActivity.EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION)) {
				final int state = paintBlue();
				stateName = QuickNectionsActivity.BLUE_STATE_NAMES.get(state);
			} else if (action.equals(QuickNectionsActivity.EXTENDED_WIFI_STATE_CHANGED_ACTION)) {
				final int state = paintWifi();
				stateName = QuickNectionsActivity.WIFI_STATE_NAMES.get(state);
			}

			if(stateName != null) {
				Toast.makeText(QuickNectionsActivity.this, stateName, Toast.LENGTH_SHORT).show();
			}
		}
	}


}
