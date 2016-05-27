/*
 * < MainActivity.java >
 * Copyright 2011 Stonestreet One. All Rights Reserved.
 *
 * Hands-free Profile Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.reconinstruments.mfitester;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.ServerEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.ServiceRecordInformation;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.reconinstruments.mfitester.ParameterView;
import com.reconinstruments.mfitester.ParameterView.ChecklistValue;
import com.reconinstruments.mfitester.ParameterView.SpinnerValue;
import com.reconinstruments.mfitester.ParameterView.TextValue;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "SPPM_Sample";

    /**
     * This needs to be incremented every time the following list of keys are
     * modified or the data in them is stored in a new format. It should be set
     * to '1' for a newly created sample app. Note that there is an equivalent
     * version value for the profile-specific data defined later in the file in
     * the profile-specific section.
     */
    private static final int STATE_VERSION = 1;

    private static final String SAVED_STATE_KEY_STATE_VERSION         = "stateVersion";
    private static final String SAVED_STATE_KEY_PROFILE_STATE_VERSION = "profileStateVersion";
    private static final String SAVED_STATE_KEY_SELECTED_COMMAND      = "command";
    private static final String SAVED_STATE_KEY_DEVICE_ADDRESS        = "remoteAddress";
    private static final String SAVED_STATE_KEY_PARAMETER_1           = "parameter1";
    private static final String SAVED_STATE_KEY_PARAMETER_2           = "parameter2";
    private static final String SAVED_STATE_KEY_PARAMETER_3           = "parameter3";

    /**
     * Message type to be sent by the {@link MainActivity}
     * {@link #uiThreadMessageHandler}.
     * <p>
     * Expects one {@code Object} parameter, which will be converted to a String
     * (by way of {@code toString}) and displayed on in the on-screen log and
     * copied to the system log.
     */
    /* pkg */static final int   MESSAGE_DISPLAY_MESSAGE   = 1;

    private static interface CommandHandler extends Runnable {

        /**
         * Indicate that the command associated with this handler has been
         * selected. The handler should signal the GUI thread to update the user
         * interface as necessary. Usually, this means activating/deactivating
         * input fields, changing input formats and hints, etc.
         */
        public void selected();

        /**
         * Indicate that the command associated with this handler was previously
         * selected but that the current selection is about to change. It is
         * recommended that Handlers can use this notification to save user
         * state.
         */
        public void unselected();
    }

    private static class Command implements Map<String, Object> {

        private final String name;
        private final CommandHandler handler;

        public final static String KEY_NAME = "name";
        public final static String KEY_HANDLER = "handler";

        public Command(String name, CommandHandler handler) {
            this.name    = name;
            this.handler = handler;
        }

        public String getName() {
            return name;
        }

        public CommandHandler getHandler() {
            return handler;
        }

        @Override
        public void clear() {
        }

        @Override
        public boolean containsKey(Object key) {
            String stringKey = key.toString();

            if((stringKey == KEY_NAME) || (stringKey == KEY_HANDLER)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean containsValue(Object value) {
            if((value instanceof String) && (name.equals(value.toString()))) {
                return true;
            } else if(handler.equals(value)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            HashSet<Entry<String,Object>> set = new HashSet<Map.Entry<String,Object>>(2);

            set.add(new SimpleImmutableEntry<String,Object>(KEY_NAME, name));
            set.add(new SimpleImmutableEntry<String,Object>(KEY_HANDLER, handler));

            return set;
        }

        @Override
        public Object get(Object key) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Set<String> keySet() {
            HashSet<String> set = new HashSet<String>(2);

            set.add(KEY_NAME);
            set.add(KEY_HANDLER);

            return set;
        }

        @Override
        public Object put(String key, Object value) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> map) {
        }

        @Override
        public Object remove(Object key) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public Collection<Object> values() {
            ArrayList<Object> array = new ArrayList<Object>(2);

            array.add(name);
            array.add(handler);

            return array;
        }
    }

    /*package*/ Resources resourceManager;

    /*package*/ Handler uiThreadMessageHandler;

    /*package*/ Button        bluetoothPowerButton;
    /*package*/ TextView      bluetoothStatusText;
    /*package*/ Spinner       commandSpinner;
    /*package*/ Button        executeCommandButton;
    /*package*/ EditText      deviceAddressText;
    /*package*/ TextView      outputLogView;
    /*package*/ ScrollView    logOutputScrollView;

    /*package*/ ParameterView parameter1View;
    /*package*/ ParameterView parameter2View;
    /*package*/ ParameterView parameter3View;
    /*package*/ ParameterView parameter4View;
    /*package*/ ParameterView parameter5View;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SimpleAdapter        commandSpinnerAdapter;

        resourceManager = getResources();

        uiThreadMessageHandler = new Handler(uiThreadHandlerCallback);

        setContentView(R.layout.main);

        bluetoothPowerButton = (Button)findViewById(R.id.bluetoothToggleButton);
        bluetoothStatusText  = (TextView)findViewById(R.id.bluetoothStatusText);
        commandSpinner       = (Spinner)findViewById(R.id.commandDropDown);
        executeCommandButton = (Button)findViewById(R.id.executeCommandButton);
        deviceAddressText    = (EditText)findViewById(R.id.remoteDeviceText);
        outputLogView        = (TextView)findViewById(R.id.logOutputText);
        logOutputScrollView  = (ScrollView)findViewById(R.id.logOutputScroller);

        parameter1View       = (ParameterView)findViewById(R.id.parameter1View);
        parameter2View       = (ParameterView)findViewById(R.id.parameter2View);
        parameter3View       = (ParameterView)findViewById(R.id.parameter3View);
        parameter4View       = (ParameterView)findViewById(R.id.parameter4View);
        parameter5View       = (ParameterView)findViewById(R.id.parameter5View);

        if(outputLogView != null) {
            outputLogView.setText("");
        }

        commandSpinnerAdapter = new SimpleAdapter(this, Arrays.asList(commandList), android.R.layout.simple_spinner_item, new String[] {"name"}, new int[] {android.R.id.text1});
        commandSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        commandSpinner.setAdapter(commandSpinnerAdapter);
        commandSpinner.setOnItemSelectedListener(commandSpinner_onItemSelected);

        bluetoothPowerButton.setOnClickListener(bluetoothPowerButton_onClick);

        executeCommandButton.setOnClickListener(executeCommandButton_onClick);

    }

    @Override
    protected void onStart() {
        super.onStart();

        BluetoothAdapter bluetoothAdapter;

        /*
         * Register a receiver for a Bluetooth "State Changed" broadcast event.
         */
        registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            if(bluetoothAdapter.isEnabled()) {
                bluetoothStatusText.setText(R.string.bluetoothEnabledStatusText);

                displayMessage(getResources().getString(R.string.localAddressLogMessagePrefix) + bluetoothAdapter.getAddress());
                profileEnable();
            } else {
                bluetoothStatusText.setText(R.string.bluetoothDisabledStatusText);
            }
        }

        // Load interface state from the previous run
        loadState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(bluetoothBroadcastReceiver);

        profileDisable();
    }

    private final Handler.Callback uiThreadHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Boolean          handled;
            final String     stringValue;

            handled = false;

            switch(msg.what) {
            case MESSAGE_DISPLAY_MESSAGE:
                if(msg.obj != null) {
                    stringValue = msg.obj.toString();

                    outputLogView.append("\n");
                    outputLogView.append(stringValue);

                    logOutputScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            logOutputScrollView.smoothScrollBy(0, outputLogView.getBottom());
                        }
                    });

                    if(Log.isLoggable(LOG_TAG, Log.INFO) && (stringValue.length() > 0)) {
                        Log.i(LOG_TAG, stringValue);
                    }
                }

                handled = true;
                break;
            }

            return handled;
        }
    };

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    showToast(context, R.string.bluetoothEnablingToastMessage);

                    if(bluetoothStatusText != null) {
                        bluetoothStatusText.setText(R.string.bluetoothEnablingStatusText);
                    }

                    break;

                case BluetoothAdapter.STATE_ON:
                    showToast(context, R.string.bluetoothEnabledToastMessage);

                    if(bluetoothStatusText != null) {
                        bluetoothStatusText.setText(R.string.bluetoothEnabledStatusText);
                    }

                    profileEnable();

                    break;

                case BluetoothAdapter.STATE_TURNING_OFF:
                    showToast(context, R.string.bluetoothDisablingToastMessage);

                    if(bluetoothStatusText != null) {
                        bluetoothStatusText.setText(R.string.bluetoothDisablingStatusText);
                    }

                    profileDisable();

                    break;

                case BluetoothAdapter.STATE_OFF:
                    showToast(context, R.string.bluetoothDisabledToastMessage);

                    if(bluetoothStatusText != null) {
                        bluetoothStatusText.setText(R.string.bluetoothDisabledStatusText);
                    }

                    break;

                case BluetoothAdapter.ERROR:
                default:
                    showToast(context, R.string.bluetoothUnknownStateToastMessage);
                }
            }
        }
    };

    private final OnClickListener bluetoothPowerButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()) {
                    if(bluetoothAdapter.disable() == false) {
                        showToast(v.getContext(), R.string.errorBluetoothNotSupportedToastMessage);
                    }
                } else {
                    bluetoothAdapter.enable();
                }
            } else {
                showToast(v.getContext(), R.string.errorBluetoothNotSupportedToastMessage);
            }
        }
    };

    private final OnClickListener executeCommandButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Command        command;
            CommandHandler handler;

            if(serialPortClientManager == null) {
                showToast(v.getContext(), R.string.errorEnableBluetoothToastMessage);
            }

            if(commandSpinner != null) {
                //displayMessage("Selected item " + commandSpinner.getSelectedItem() + ", id " + commandSpinner.getSelectedItemId() + ", pos " + commandSpinner.getSelectedItemPosition());
                command = (Command)(commandSpinner.getSelectedItem());

                if((handler = command.getHandler()) != null) {
                    handler.run();
                }
            }
        }
    };

    private final OnItemSelectedListener commandSpinner_onItemSelected = new OnItemSelectedListener() {

        Command previousCommand = null;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Command command = (Command)parent.getItemAtPosition(position);

            if(command != previousCommand) {
                if(previousCommand != null) {
                    previousCommand.getHandler().unselected();
                }

                previousCommand = command;
            }

            if(command != null) {
                command.getHandler().selected();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            if(previousCommand != null) {
                previousCommand.getHandler().unselected();
                previousCommand = null;
            }

            displayMessage("Nothing selected");
        }
    };

    private void loadState() {
        int                      stateVersion;
        SharedPreferences        preferences;
        SharedPreferences.Editor editor;

        preferences  = getPreferences(MODE_PRIVATE);
        stateVersion = preferences.getInt(SAVED_STATE_KEY_STATE_VERSION, -1);

        if(stateVersion != STATE_VERSION) {
            editor = preferences.edit();
            editor.clear();

            editor.putInt(SAVED_STATE_KEY_STATE_VERSION, STATE_VERSION);
            editor.apply();

            showToast(this, R.string.errorPreferencesOutOfDate);
        }

        /*
         * Load interface state.
         */

        // Selected command
        commandSpinner.setSelection(preferences.getInt(SAVED_STATE_KEY_SELECTED_COMMAND, 0));

        // Remote device address
        deviceAddressText.setText(preferences.getString(SAVED_STATE_KEY_DEVICE_ADDRESS, ""));

        // TODO Cycle through listed handlers allowing each to load private state data.
    }

    /**
     * Save the current state of the application interface.
     */
    private void saveState() {
        SharedPreferences        preferences;
        SharedPreferences.Editor editor;

        preferences = getPreferences(MODE_PRIVATE);
        editor      = preferences.edit();

        /*
         * Save interface state.
         */

        // Selected command
        editor.putInt(SAVED_STATE_KEY_SELECTED_COMMAND, commandSpinner.getSelectedItemPosition());

        // Remote device address
        editor.putString(SAVED_STATE_KEY_DEVICE_ADDRESS, deviceAddressText.getText().toString().trim());

        // TODO Cycle through listed handlers allowing each to save private state data.

        editor.apply();
    }

    /*package*/ BluetoothAddress getRemoteDeviceAddress() {
        String           addressString;
        BluetoothAddress remoteAddress = null;

        if(deviceAddressText != null) {
            addressString = deviceAddressText.getText().toString().trim();

            try {
                remoteAddress = new BluetoothAddress(addressString);
            } catch(IllegalArgumentException e) { }
        }

        return remoteAddress;
    }

    /*package*/ void displayMessage(CharSequence string) {
        Message.obtain(uiThreadMessageHandler, MESSAGE_DISPLAY_MESSAGE, string).sendToTarget();
    }

    /*package*/ static void showToast(Context context, int resourceID) {
        Toast.makeText(context, resourceID, context.getResources().getInteger(R.integer.toastErrorDuration)).show();
    }

    /*package*/ static void showToast(Context context, String message) {
        Toast.makeText(context, message, context.getResources().getInteger(R.integer.toastErrorDuration)).show();
    }

    /*==========================================================
     * Profile-specific content below this line.
     */

    /*package*/ SerialPortClientManager serialPortClientManager;
    /*package*/ SerialPortServerManager serialPortServerManager;

    private final void profileEnable() {
        synchronized(this) {

            try {
                serialPortClientManager = new SerialPortClientManager(clientEventCallback);
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                BluetoothAdapter.getDefaultAdapter().disable();
            }

        }
    }

    private final void profileDisable() {
        synchronized(MainActivity.this) {
            if(serialPortClientManager != null) {
                serialPortClientManager.dispose();
                serialPortClientManager = null;
            }

            if(serialPortServerManager != null) {
                serialPortServerManager.dispose();
                serialPortServerManager = null;
            }
        }
    }

    private final ClientEventCallback clientEventCallback = new ClientEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.dataReceivedEventLabel)).append(":");
            sb.append(" Data Length: ").append(dataLength);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void disconnectedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.disconnectedEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.idpsStatusEventLabel)).append(":");
            sb.append("\n  State: " + idpsState.name());
            sb.append("\n  Status: " + idpsStatus.name());

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.lineStatusChangedEventLabel)).append(":");

            if(lineStatus.isEmpty()) {
                sb.append("  No errors");
            } else {
                if(lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
                    sb.append("  OVERRUN_ERROR");
                }
                if(lineStatus.contains(LineStatus.PARITY_ERROR)) {
                    sb.append("  PARITY_ERROR");
                }
                if(lineStatus.contains(LineStatus.FRAMING_ERROR)) {
                    sb.append("  FRAMING_ERROR");
                }
            }

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.nonSessionDataConfirmationEventLabel)).append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.nonSessionDataReceivedEventLabel)).append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.portStatusChangedEventLabel)).append(":");
            sb.append("  Enabled flags:");

            if(portStatus.isEmpty()) {
                sb.append("    NONE");
            } else {
                if(portStatus.contains(PortStatus.RTS_CTS)) {
                    sb.append("    RTS_CTS");
                }
                if(portStatus.contains(PortStatus.DTR_DSR)) {
                    sb.append("    DTR_DSR");
                }
                if(portStatus.contains(PortStatus.RING_INDICATOR)) {
                    sb.append("    RING_INDICATOR");
                }
                if(portStatus.contains(PortStatus.CARRIER_DETECT)) {
                    sb.append("    CARRIER_DETECT");
                }
            }

            sb.append("  Break Signal:  ").append((breakSignal == true) ? "Enabled" : "Disabled");
            sb.append("  Break Timeout: ").append(breakTimeout);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionCloseEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionDataConfirmationEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionDataReceivedEventLabel)).append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionOpenRequestEventLabel)).append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.transmitBufferEmptyEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectionStatusEvent(ConnectionStatus status, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(":");

            sb.append("  Connection Status: ");
            switch(status) {
            case SUCCESS:
                sb.append("SUCCESS");
                break;
            case FAILURE_TIMEOUT:
                sb.append("FAILURE_TIMEOUT");
                break;
            case FAILURE_REFUSED:
                sb.append("FAILURE_REFUSED");
                break;
            case FAILURE_SECURITY:
                sb.append("FAILURE_SECURITY");
                break;
            case FAILURE_DEVICE_POWER_OFF:
                sb.append("FAILURE_DEVICE_POWER_OFF");
                break;
            case FAILURE_UNKNOWN:
                sb.append("FAILURE_UNKNOWN");
                break;
            }

            sb.append("  Connection Type: ");
            switch(type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            }

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void unhandledControlMessageReceivedEvent(short arg0,byte[] arg1) {
            // TODO Auto-generated method stub
        }
    };

    private final ServerEventCallback serverEventCallback = new ServerEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.dataReceivedEventLabel)).append(":");
            sb.append(" Data Length: ").append(dataLength);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void disconnectedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.disconnectedEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.idpsStatusEventLabel)).append(":");

            switch(idpsState) {
            case START_IDENTIFICATION_REQUEST:
                sb.append("  State: START_IDENTIFICATION_REQUEST");
                break;
            case START_IDENTIFICATION_PROCESS:
                sb.append("  State: START_IDENTIFICATION_PROCESS");
                break;
            case IDENTIFICATION_PROCESS:
                sb.append("  State: IDENTIFICATION_PROCESS");
                break;
            case IDENTIFICATION_PROCESS_COMPLETE:
                sb.append("  State: IDENTIFICATION_PROCESS_COMPLETE");
                break;
            case START_AUTHENTICATION_PROCESS:
                sb.append("  State: START_AUTHENTICATION_PROCESS");
                break;
            case AUTHENTICATION_PROCESS:
                sb.append("  State: AUTHENTICATION_PROCESS");
                break;
            case AUTHENTICATION_PROCESS_COMPLETE:
                sb.append("  State: AUTHENTICATION_PROCESS_COMPLETE");
                break;
            }

            switch(idpsStatus) {
            case SUCCESS:
                sb.append("  Status: SUCCESS");
                break;
            case ERROR_RETRYING:
                sb.append("  Status: ERROR_RETRYING");
                break;
            case TIMEOUT_HALTING:
                sb.append("  Status: TIMEOUT_HALTING");
                break;
            case GENERAL_FAILURE:
                sb.append("  Status: GENERAL_FAILURE");
                break;
            case PROCESS_FAILURE:
                sb.append("  Status: PROCESS_FAILURE");
                break;
            case PROCESS_TIMEOUT_RETRYING:
                sb.append("  Status: PROCESS_TIMEOUT_RETRYING");
                break;
            }

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.lineStatusChangedEventLabel)).append(":");

            if(lineStatus.isEmpty()) {
                sb.append("  No errors");
            } else {
                if(lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
                    sb.append("  OVERRUN_ERROR");
                }
                if(lineStatus.contains(LineStatus.PARITY_ERROR)) {
                    sb.append("  PARITY_ERROR");
                }
                if(lineStatus.contains(LineStatus.FRAMING_ERROR)) {
                    sb.append("  FRAMING_ERROR");
                }
            }

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.nonSessionDataConfirmationEventLabel)).append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.nonSessionDataReceivedEventLabel)).append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.portStatusChangedEventLabel)).append(":");
            sb.append("  Enabled flags:");

            if(portStatus.isEmpty()) {
                sb.append("    NONE");
            } else {
                if(portStatus.contains(PortStatus.RTS_CTS)) {
                    sb.append("    RTS_CTS");
                }
                if(portStatus.contains(PortStatus.DTR_DSR)) {
                    sb.append("    DTR_DSR");
                }
                if(portStatus.contains(PortStatus.RING_INDICATOR)) {
                    sb.append("    RING_INDICATOR");
                }
                if(portStatus.contains(PortStatus.CARRIER_DETECT)) {
                    sb.append("    CARRIER_DETECT");
                }
            }

            sb.append("  Break Signal:  ").append((breakSignal == true) ? "Enabled" : "Disabled");
            sb.append("  Break Timeout: ").append(breakTimeout);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionCloseEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionDataConfirmationEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionDataReceivedEventLabel)).append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionOpenRequestEventLabel)).append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.transmitBufferEmptyEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.connectedEventLabel));
            sb.append("  Address: ").append(remoteDeviceAddress.toString());

            sb.append("  Type:    ");
            switch(type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            }

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.connectionRequestEventLabel));
            sb.append("  Address: ").append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void unhandledControlMessageReceivedEvent(short arg0,byte[] arg1) {
            // TODO Auto-generated method stub
        }
    };

    private final CommandHandler disconnectRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                result;
            TextValue          flushTimeoutParameter;
            int                flushTimeoutInteger;
            SPPM               manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {

                flushTimeoutParameter = parameter2View.getValueText();

                try {
                    flushTimeoutInteger = Integer.parseInt(flushTimeoutParameter.text.toString(),16);
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = -1;

                result = manager.disconnectRemoteDevice(flushTimeoutInteger);

                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: " + result);

            } else {
                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeText("", "Flush Timeout", InputType.TYPE_CLASS_NUMBER);
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler readData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int    result;
            byte[] dataBuffer;
            String dataString = null;
            SPPM   manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null)
            {
                // TODO need to add support for testing readData overload for partial buffers

                dataBuffer = new byte[1024];
                result = manager.readData(dataBuffer, SPPM.TIMEOUT_IMMEDIATE);

                displayMessage("");
                displayMessage("readData() result: " + result);

                for (int index=0;index < result; index++)
                {

                    dataString = dataString + (char)dataBuffer[index];

                }

                if (dataString != null)
                {

                    displayMessage(dataString);

                }

            }
            else
            {
                displayMessage("");
                displayMessage("readData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler writeData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int    result;
            SPPM   manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                result = manager.writeData("This is an SPP data test message".getBytes(), 5000);

                displayMessage("");
                displayMessage("writeData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("writeData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler writeDataArbitrary_Handler = new CommandHandler() {

        String[] specialCharacters =
        {
            "Carriage Return(^M)",
            "Substitute(^Z)",
            "None"
        };

        @Override
        public void run()
        {
            int result;
            SPPM manager;

            SpinnerValue specialCharacterParameter;
            String arbitraryDataString = null;

            TextValue writeTimeoutParameter;
            int writeTimeout;

            if(parameter4View.getValueCheckbox().value)
            {

                manager = serialPortClientManager;

            }
            else
            {

                manager = serialPortServerManager;

            }

            if(manager != null)
            {

                specialCharacterParameter = parameter2View.getValueSpinner();

                switch (specialCharacterParameter.selectedItem)
                {

                    case 0:

                        arbitraryDataString = parameter1View.getValueText().text.toString() + (char)13;
                        break;

                    case 1:

                        arbitraryDataString = parameter1View.getValueText().text.toString() + (char)26;
                        break;

                    case 2:

                        arbitraryDataString = parameter1View.getValueText().text.toString();
                        break;


                }

                writeTimeoutParameter = parameter3View.getValueText();

                try
                {

                    writeTimeout = Integer.valueOf(writeTimeoutParameter.text.toString());

                }
                catch(NumberFormatException e)
                {

                    // TODO complain
                    return;

                }

                result = manager.writeData(arbitraryDataString.getBytes(), writeTimeout);

                displayMessage("");
                displayMessage("writeData() result: " + result);

            }
            else
            {

                displayMessage("");
                displayMessage("writeData() result: The selected Manager is not initialized");

            }

        }

        @Override
        public void selected()
        {
            parameter1View.setModeText("", "Arbitrary Data");
            parameter2View.setModeSpinner("Terminate String with Special Character", specialCharacters);
            parameter3View.setModeText("5000", "Write Timeout");
            parameter4View.setModeCheckbox("Client (uncheck for Server)", true);
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendLineStatus_Handler = new CommandHandler() {

        String[] lineStatuses = {
                "Overrun Error",
                "Parity Error",
                "Framing Error",
        };

        boolean[] lineStatusValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            ChecklistValue           lineStatusParameter;
            EnumSet<SPPM.LineStatus> lineStatus;
            int                      result;
            SPPM                     manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                lineStatusParameter = parameter2View.getValueChecklist();

                lineStatus = EnumSet.noneOf(SPPM.LineStatus.class);

                if(lineStatusParameter.checkedItems[0]) {
                    lineStatus.add(LineStatus.OVERRUN_ERROR);
                }
                if(lineStatusParameter.checkedItems[1]) {
                    lineStatus.add(LineStatus.PARITY_ERROR);
                }
                if(lineStatusParameter.checkedItems[2]) {
                    lineStatus.add(LineStatus.FRAMING_ERROR);
                }

                result = manager.sendLineStatus(lineStatus);

                displayMessage("");
                displayMessage("sendLineStatus() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendLineStatus() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeChecklist("Line Statuses", lineStatuses, lineStatusValues);
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendPortStatus_Handler = new CommandHandler() {

        String[] portStatuses = {
                "RTS/CTS",
                "DTR/DSR",
                "Ring Indicator",
                "Carrier Detect",
        };

        boolean[] portStatusValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            ChecklistValue           portStatusParameter;
            EnumSet<SPPM.PortStatus> portStatus;
            boolean                  breakSignal;
            int                      breakTimeout;
            int                      result;
            SPPM                     manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                portStatusParameter = parameter2View.getValueChecklist();

                portStatus = EnumSet.noneOf(SPPM.PortStatus.class);

                if(portStatusParameter.checkedItems[0]) {
                    portStatus.add(PortStatus.RTS_CTS);
                }
                if(portStatusParameter.checkedItems[1]) {
                    portStatus.add(PortStatus.DTR_DSR);
                }
                if(portStatusParameter.checkedItems[2]) {
                    portStatus.add(PortStatus.RING_INDICATOR);
                }
                if(portStatusParameter.checkedItems[3]) {
                    portStatus.add(PortStatus.CARRIER_DETECT);
                }

                breakSignal = parameter2View.getValueCheckbox().value;

                try {
                    breakTimeout = Integer.valueOf(parameter3View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendPortStatus(portStatus, breakSignal, breakTimeout);

                displayMessage("");
                displayMessage("sendPortStatus() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendPortStatus() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeChecklist("Port Statuses", portStatuses, portStatusValues);
            parameter3View.setModeCheckbox("Break Signal:", false);
            parameter4View.setModeText("", "Break Timeout");
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler queryRemoteDeviceServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            ServiceRecordInformation[] serviceRecordInformation;

            BluetoothAddress bluetoothAddress;

            if(serialPortClientManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                serviceRecordInformation = serialPortClientManager.queryRemoteDeviceServices(bluetoothAddress);

                displayMessage("");

                if (serviceRecordInformation != null) {
                    displayMessage("queryRemoteDeviceServices():");

                    for(ServiceRecordInformation serviceRecord: serviceRecordInformation) {
                        displayMessage("");
                        displayMessage("Service Record Handle     : " + serviceRecord.serviceRecordHandle);
                        displayMessage("Service Class             : " + serviceRecord.serviceClassID.toString());
                        displayMessage("Service Name              : " + serviceRecord.serviceName);
                        displayMessage("Service RFCOMM Port Number: " + serviceRecord.rfcommPortNumber);
                    }
                } else {
                    displayMessage("queryRemoteDeviceServices(): returned null");
                }

            }

        }

        @Override
        public void selected() {
            parameter1View.setModeHidden();
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler configureMFiSettings_Handler = new CommandHandler() {

        @Override
        public void run() {

            int  result;

            if(serialPortClientManager != null) {
                result = serialPortClientManager.configureMFiSettings(
                                512,
                                800,
                                null,
                                new MFiAccessoryInfo(SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
                                        "Apple Accessory",
                                        0x00010000,
                                        0x00010000,
                                        "Stonestreet One",
                                        "BluetopiaPM",
                                        "SN:012346",
                                        (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3)),
                                new String[] {"com.stonestreetone.demo", "another.protocol.name"},
                                "12345ABCDE",
                                null);

                displayMessage("");
                displayMessage("configureMFiSettings() result: " + result);
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeHidden();
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler queryConnectionType_Handler = new CommandHandler() {

        @Override
        public void run() {
            ConnectionType type;
            SPPM           manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                type = manager.queryConnectionType();

                displayMessage("");

                if(type != null) {
                    switch(type) {
                    case SPP:
                        displayMessage("queryConnectionType() result: SPP");
                        break;
                    case MFI:
                        displayMessage("queryConnectionType() result: MFi");
                        break;
                    }
                } else {
                    displayMessage("queryConnectionType() result: Not currently connected");
                }
            } else {
                displayMessage("");
                displayMessage("queryConnectionType() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler openSessionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int       result;
            int       sessionID;
            boolean   accept;
            SPPM               manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    sessionID = Integer.valueOf(parameter2View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                accept = parameter3View.getValueCheckbox().value;

                result = manager.openSessionRequestResponse(sessionID, accept);

                displayMessage("");
                displayMessage("openSessionRequestResponse() result: " + result);
            } else {
                displayMessage("");
                displayMessage("openSessionRequestResponse() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeText("", "Session ID", InputType.TYPE_CLASS_NUMBER);
            parameter3View.setModeCheckbox("Accept?", true);
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendSessionData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int  result;
            int  sessionID;
            SPPM manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    sessionID = Integer.valueOf(parameter2View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendSessionData(sessionID, "This is a session data test message".getBytes());

                displayMessage("");
                displayMessage("sendSessionData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendSessionData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeText("", "Session ID", InputType.TYPE_CLASS_NUMBER);
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendNonSessionData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int  lingoID;
            int  commandID;
            int  transactionID;
            int  result;
            SPPM manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    lingoID = Integer.valueOf(parameter2View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    commandID = Integer.valueOf(parameter3View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    transactionID = Integer.valueOf(parameter4View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendNonSessionData(lingoID, commandID, transactionID, "This is a non-session data test message".getBytes());

                displayMessage("");
                displayMessage("sendNonSessionData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendNonSessionData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeText("", "Lingo ID", InputType.TYPE_CLASS_NUMBER);
            parameter3View.setModeText("", "Command ID", InputType.TYPE_CLASS_NUMBER);
            parameter4View.setModeText("", "Transaction ID", InputType.TYPE_CLASS_NUMBER);
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler cancelPacket_Handler = new CommandHandler() {

        @Override
        public void run() {
            int       result;
            TextValue packetIDParameter;
            int       packetID;
            SPPM      manager;

            if(parameter1View.getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                packetIDParameter = parameter2View.getValueText();

                try {
                    packetID = Integer.valueOf(packetIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.cancelPacket(packetID);

                displayMessage("");
                displayMessage("cancelPacket() result: " + result);
            } else {
                displayMessage("");
                displayMessage("cancelPacket() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Client: (uncheck for Server)", true);
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler connectRemoteDevice_Handler = new CommandHandler() {

        String[] connectionFlagLabels = {
                "Require Authentication",
                "Require Encryption",
                "MFi Required",
        };

        boolean[] connectionFlagValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;
            int portNumber;
            EnumSet<ConnectionFlags> connectionFlags;
            ChecklistValue           connectionFlagsParameter;
            boolean waitForConnection;

            if(serialPortClientManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    portNumber = Integer.valueOf(parameter1View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                connectionFlagsParameter = parameter2View.getValueChecklist();
                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);

                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                }
                if(connectionFlagsParameter.checkedItems[2]) {
                    connectionFlags.add(ConnectionFlags.MFI_REQUIRED);
                }

                waitForConnection = parameter3View.getValueCheckbox().value;

                result = serialPortClientManager.connectRemoteDevice(bluetoothAddress, portNumber, connectionFlags, waitForConnection);

                displayMessage("");
                displayMessage("connectRemoteDevice() result: " + result);
            } else {
                displayMessage("");
                displayMessage("connectRemoteDevice() result: The Client Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            parameter2View.setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            parameter3View.setModeCheckbox("Wait for connection:", false);
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler connectionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int     result;
            boolean accept;

            if(serialPortServerManager != null) {
                accept = parameter1View.getValueCheckbox().value;

                result = serialPortServerManager.connectionRequestResponse(accept);

                displayMessage("");
                displayMessage("connectionRequestResponse() result: " + result);
            } else {
                displayMessage("");
                displayMessage("connectionRequestResponse() result: There is not active server port");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeCheckbox("Accept?", true);
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler openServer_Handler = new CommandHandler() {

        String[] incomingConnectionFlags = {
                "Require Authorization",
                "Require Athentication",
                "Require Encryption",
                "MFI Allowed",
                "MFi Required",
        };

        boolean[] incomingConnectionFlagsValues = new boolean[] {false, false, false, false, false};

        @Override
        public void run() {
            int                              portNumber;
            UUID[]                           serviceClasses = null;
            String                           serviceName    = null;
            ChecklistValue                   flagsParameter;
            EnumSet<IncomingConnectionFlags> flags;

            if(serialPortServerManager == null) {
                try {
                    portNumber = Integer.valueOf(parameter1View.getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                flagsParameter = parameter2View.getValueChecklist();
                flags          = EnumSet.noneOf(IncomingConnectionFlags.class);

                if(flagsParameter.checkedItems[0]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                }
                if(flagsParameter.checkedItems[1]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(flagsParameter.checkedItems[2]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                }
                if(flagsParameter.checkedItems[3]) {
                    flags.add(IncomingConnectionFlags.MFI_ALLOWED);
                }
                if(flagsParameter.checkedItems[4]) {
                    flags.add(IncomingConnectionFlags.MFI_REQUIRED);
                }

                // FIXME support custom service class and name
                if(parameter3View.getValueText().text.length() > 0) {
                    try {
                        serviceClasses = new UUID[1];

                        String uuidText = parameter3View.getValueText().text.toString();

                        if(uuidText.contains("-")) {
                            serviceClasses[0] = UUID.fromString(parameter3View.getValueText().text.toString());
                        } else if(uuidText.length() == 32) {
                            serviceClasses[0] = new UUID(Long.parseLong(uuidText.substring(0, 16), 16), Long.parseLong(uuidText.substring(16), 16));
                        } else {
                            serviceClasses = null;
                        }
                    } catch(IllegalArgumentException e) {
                        serviceClasses = null;
                    }

                    if(serviceClasses == null) {
                        displayMessage("UUID is malformed. It should follow the format: 00112233-4455-6677-8899-AABBCCDDEEFF");
                        return;
                    }
                }

                if(parameter4View.getValueText().text.length() > 0) {
                    serviceName = parameter4View.getValueText().text.toString();
                }

                displayMessage("");

                try {
                    if((serviceClasses != null) || (serviceName != null)) {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags, serviceClasses, null, serviceName);
                    } else {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags);
                    }
                    displayMessage("Open Server result: Success");
                } catch(ServerNotReachableException e) {
                    displayMessage("Open Server result: Unable to communicate with Platform Manager service");
                } catch(BluetopiaPMException e) {
                    displayMessage("Open Server result: Unable to register server port (already in use?)");
                }
            } else {
                displayMessage("");
                displayMessage("Open Server result: The sample already has an active SPP server manager");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeText("", "Port Number", InputType.TYPE_CLASS_NUMBER);
            parameter2View.setModeChecklist("Incoming Connection Flags", incomingConnectionFlags, incomingConnectionFlagsValues);
            parameter3View.setModeText("", "(Optional) Service Class UUID");
            parameter4View.setModeText("", "(Optional) Service Name");
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler closeServer_Handler = new CommandHandler() {

        @Override
        public void run() {
            if(serialPortServerManager != null) {
                serialPortServerManager.dispose();
                serialPortServerManager = null;

                displayMessage("");
                displayMessage("Close Server result: Success");
            } else {
                displayMessage("");
                displayMessage("Close Server result: No server is currently active.");
            }
        }

        @Override
        public void selected() {
            parameter1View.setModeHidden();
            parameter2View.setModeHidden();
            parameter3View.setModeHidden();
            parameter4View.setModeHidden();
            parameter5View.setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final Command[] commandList = new Command[] {

            new Command("Connect Remote Device", connectRemoteDevice_Handler),
            new Command("Open Server Port", openServer_Handler),
            new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Disconnect Remote Device", disconnectRemoteDevice_Handler),
            new Command("Close Server Port", closeServer_Handler),
            new Command("Query Connection Type", queryConnectionType_Handler),
            new Command("Read Data", readData_Handler),
            new Command("Write Data", writeData_Handler),
            new Command("Write Arbitrary Data", writeDataArbitrary_Handler),
            new Command("Send Line Status", sendLineStatus_Handler),
            new Command("Send Port Status", sendPortStatus_Handler),
            //xxx
            new Command("Query Remote Device Services", queryRemoteDeviceServices_Handler),
            new Command("(MFi) Configure MFi", configureMFiSettings_Handler),
            new Command("(MFi) Open Session Request Response", openSessionRequestResponse_Handler),
            new Command("(MFi) Send Session Data", sendSessionData_Handler),
            new Command("(MFi) Send Non-Session Data", sendNonSessionData_Handler),
            new Command("(MFi) Cancel Packet", cancelPacket_Handler),
    };
}
