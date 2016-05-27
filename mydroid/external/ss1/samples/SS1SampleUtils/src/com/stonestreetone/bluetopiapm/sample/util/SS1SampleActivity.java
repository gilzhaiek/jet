package com.stonestreetone.bluetopiapm.sample.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;

import com.stonestreetone.bluetopiapm.BluetoothAddress;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Base activity for creation of a sample application
 * using the Stonestreet One BluetopiaPM Protocol Stack.
 */
public abstract class SS1SampleActivity extends Activity {
    protected final String LOG_TAG = "SS1SampleActivity";
    private boolean profileEnabled = false;
    private Command[] commandList;
    private ParameterView[] profileParameterList;
    private ParameterView[] commandParameterList;
    
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

    protected static interface CommandHandler extends Runnable {

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

    public static class Command implements Map<String, Object> {

        private final String name;
        private final CommandHandler handler;

        public final static String KEY_NAME = "name";
        public final static String KEY_HANDLER = "handler";
        
//        public Command() {
//            this.name = null;
//            this.handler = null;
//        }

        /**
         * Create a new command that can be run by the application.
         * 
         * @param name The name of the command to be displayed in the selector.
         * @param handler The {@link CommandHandler} which will be called to
         *                handle operations for the command
         */
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

    protected Resources resourceManager;

    /*package*/ Handler uiThreadMessageHandler;

    /*package*/ Button        bluetoothPowerButton;
    /*package*/ TextView      bluetoothStatusText;
    /*package*/ Spinner       commandSpinner;
    /*package*/ Button        executeCommandButton;
    /*package*/ EditText      deviceAddressText;
    /*package*/ TextView      outputLogView;
    /*package*/ ScrollView    logOutputScrollView;

    /** Called when the activity is first created. 
     *  <p>NOTE: If you need to override this, make sure you call super.onCreate()
     *           before you attempt to access any information from this class.
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SimpleAdapter        commandSpinnerAdapter;

        resourceManager = getResources();

        uiThreadMessageHandler = new Handler(uiThreadHandlerCallback);

        setContentView(R.layout.ss1_sample_main);

        bluetoothPowerButton = (Button)findViewById(R.id.bluetoothToggleButton);
        bluetoothStatusText  = (TextView)findViewById(R.id.bluetoothStatusText);
        commandSpinner       = (Spinner)findViewById(R.id.commandDropDown);
        executeCommandButton = (Button)findViewById(R.id.executeCommandButton);
        deviceAddressText    = (EditText)findViewById(R.id.remoteDeviceText);
        outputLogView        = (TextView)findViewById(R.id.logOutputText);
        logOutputScrollView  = (ScrollView)findViewById(R.id.logOutputScroller);
        
        LinearLayout commandParamContainer = (LinearLayout)findViewById(R.id.commandParameterViewContainer);
        LinearLayout profileParamContainer = (LinearLayout)findViewById(R.id.profileParameterViewContainer);
        
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        
        profileParameterList = new ParameterView[getNumberProfileParameters()];
        
        for(int i=0;i<profileParameterList.length;i++) {
            profileParameterList[i] = new ParameterView(this);
            profileParameterList[i].setLayoutParams(params);
            profileParamContainer.addView(profileParameterList[i]);
        }
        
        commandParameterList = new ParameterView[getNumberCommandParameters()];
        
        for(int i=0;i<commandParameterList.length;i++) {
            commandParameterList[i] = new ParameterView(this);
            commandParameterList[i].setLayoutParams(params);
            commandParamContainer.addView(commandParameterList[i]);
        }

        if(outputLogView != null) {
            outputLogView.setText("");
        }
        
        commandList = getCommandList();

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
                profileEnabled = profileEnable();
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

    private final Handler.Callback uiThreadHandlerCallback = new Handler.Callback()
    {

        @Override
        public boolean handleMessage(Message msg)
        {

            Boolean          handled;
            final String     stringValue;

            handled = false;

            switch(msg.what)
            {

                case MESSAGE_DISPLAY_MESSAGE:

                    if(msg.obj != null)
                    {

                        stringValue = msg.obj.toString();

                        outputLogView.append("\n");
                        outputLogView.append(stringValue);

                        logOutputScrollView.post
                        (

                            new Runnable()
                            {

                                @Override
                                public void run()
                                {

                                    logOutputScrollView.smoothScrollBy(0, outputLogView.getBottom());

                                }

                            }

                        );

                        if(Log.isLoggable(LOG_TAG, Log.INFO) && (stringValue.length() > 0))
                        {

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

                    profileEnabled = profileEnable();

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

            if(!profileEnabled) {
                showToast(v.getContext(), R.string.errorEnableBluetoothToastMessage);
                return;
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

    /**
     * Get the {@link BluetoothAddress} represented in the Remote Device Address text field
     * 
     * @return The BluetoothAddress.
     */
    protected BluetoothAddress getRemoteDeviceAddress() {
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

    /**
     * Post a message to the output display area.
     * 
     * @param string The message to post.
     */
    protected void displayMessage(CharSequence string) {
        Message.obtain(uiThreadMessageHandler, MESSAGE_DISPLAY_MESSAGE, string).sendToTarget();
    }

    /**
     * Show a toast message.
     * 
     * @param context The current context.
     * @param message The string resource ID of the message to display.
     */
    protected static void showToast(Context context, int resourceID) {
        Toast.makeText(context, resourceID, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a toast message.
     * 
     * @param context The current context.
     * @param message The message to display.
     */
    protected static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Get the profile-generic parameter view at the specified index.
     * 
     * @param index The index of the requested parameter.
     * 
     * @return The requested parameter.
     */
    protected ParameterView getProfileParameterView(int index) {
        if(profileParameterList != null && index >= 0 && index < profileParameterList.length)
            return profileParameterList[index];
        else
            throw new IllegalArgumentException("Index is not valid");
    }
    
    /**
     * Get the command-specific parameter view at the specified index.
     * 
     * @param index The index of the requested parameter.
     * 
     * @return The requested parameter.
     */
    protected ParameterView getCommandParameterView(int index) {
        if(commandParameterList != null && index >= 0 && index < commandParameterList.length)
            return commandParameterList[index];
        else
            throw new IllegalArgumentException("Index is not valid");
    }
    
    /**
     * Set whether the BluetoothAddress input field is visible or not.
     * 
     * @param visible {@code true} if the field should be visible.
     */
    protected void setBluetoothAddressVisibility(boolean visible) {
        deviceAddressText.setVisibility(visible?View.VISIBLE:View.GONE);
    }
    
    protected void resetCommands() {
        SimpleAdapter commandSpinnerAdapter;
        
        commandList = getCommandList();
        commandSpinnerAdapter = new SimpleAdapter(this, Arrays.asList(commandList), android.R.layout.simple_spinner_item, new String[] {"name"}, new int[] {android.R.id.text1});
        commandSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        commandSpinner.setAdapter(commandSpinnerAdapter);
    }
    
    /**
     * Indicates that the application is requesting to set up the
     * Bluetooth profile for which the sample has been created.
     * 
     * @return {@code true} if the profile was successfully enabled or {@code false} otherwise.
     */
    protected abstract boolean profileEnable();

    /**
     * Indicates that the application is requesting to tear down the
     * Bluetooth profile for which the sample has been created.
     */
    protected abstract void profileDisable();
    
    /**
     * Called by this parent class in order to obtain the profile-specific commands that can be run.
     * 
     * @return The supported sample commands by the underlying profile.
     */
    protected abstract Command[] getCommandList();
    
    /**
     * Called by this parent class in order to determine how many
     * profile-generic (i.e. not related to any particular command)
     * parameters need to be created.
     * 
     * @return The number of non-command-specific parameters needed.
     */
    protected abstract int getNumberProfileParameters();
    
    /**
     * Called by this parent class in order to determine how many
     * command-specific parameters need to completed.
     * <p>
     * Any subclass should return the <i>maximum</i> amount of parameters
     * required for any of its commands. Any parameters not required for a given command
     * can be temporarily hidden via {@link ParameterView#setModeHidden()}.
     * 
     * @return The <i>maximum</i> number of command-specific parameters required by the underlying profile.
     */
    protected abstract int getNumberCommandParameters();
}
