/*
 * HIDM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;

/**
 * Java wrapper for Human Interface Device Profile Manager API for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public abstract class HIDM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    private final static int      MANAGER_TYPE_HOST   = 1;
    private final static int      MANAGER_TYPE_DEVICE = 2;

    /**
     * Constructor for the HID Manager object.
     *
     * @param eventCallback
     *            Receiver for events.
     *
     * @throws ServerNotReachableException
     *             if the BluetopiaPM server cannot be reached.
     */
    protected HIDM(int type, EventCallback eventCallback) throws ServerNotReachableException {

        try {

            initObjectNative();
            disposed        = false;
            callbackHandler = eventCallback;

        } catch(ServerNotReachableException exception) {

            dispose();
            throw exception;
        }
    }

    /**
     * Disposes of all resources allocated by this Manager object.
     */
    public void dispose() {
        if(!disposed) {
            cleanupObjectNative();
            disposed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if(!disposed) {
                System.err.println("Error: Possible memory leak: Manager object of type " + this.getClass().getName() + " disposed = false");
                dispose();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Registers or unregisters a callback function to be invoked by the HID Manager when various data events occur.
     * @param enableDataCallback boolean which determines if data event callbacks will be registered or unregistered
     * @return A positive (non-zero) value if successful, or a negative return error code.<p>
     */
    public int EnableDataEvents(boolean enableDataCallback) {

        int result = 0;

        if(enableDataCallback == true)
            result = registerDataEventCallbackNative();
        else
            unregisterDataEventCallbackNative();

        return result;
    }

    /**
     * Used to accept or reject (authorize) an incoming HID connection.
     * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
     * @param accept Whether the remote device has accepted the connection request.
     * @param connectionFlagSet The parameters of the connection.  Valid flags include the ability to specify report protocol mode or boot protocol mode.
     * @return Zero if successful, or a negative return error code.
     */
    public int connectionRequestResponse(BluetoothAddress remoteDeviceAddress, boolean accept, EnumSet<ResponseConnectionFlags> connectionFlagSet) {

        long connectionFlagMask = 0;

        for(ResponseConnectionFlags responseConnectionFlag : connectionFlagSet) {

            switch(responseConnectionFlag) {

            case REPORT_MODE:
                connectionFlagMask |= RESPONSE_CONNECTION_FLAG_REPORT_MODE;
            case PARSE_BOOT:
                connectionFlagMask |= RESPONSE_CONNECTION_FLAG_PARSE_BOOT;
            }
        }

        byte[] address = remoteDeviceAddress.internalByteArray();

        return connectionRequestResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], accept, connectionFlagMask);
    }

    /**
     * Creates a connection to a remote HID device.
     * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
     * @param connectionFlagSet The parameters of the connection.  Valid flags include the ability to specify an authenticated or encrypted connection, and the ability to specify report protocol or boot protocol.
     * @param waitForConnection Specifies whether this function should block until the connection status is received.<p><i>Note:</i> the interpretation of the return value of this function depends on whether this value is {@code true} or {@code false}.
     * @return If {@code waitForConnection} is {@code false}: zero if successful, or a negative return error code; the connection status will be returned asynchronously in an {@link EventCallback#deviceConnectionStatusEvent(BluetoothAddress, ConnectionStatus)} event.<p>
     * If {@code waitForConnection} is {@code true}: zero if successful, or a positive connection status value indicating that the connection attempt completed but that the connection was not established.
     */
    public int connectRemoteDevice(BluetoothAddress remoteDeviceAddress, EnumSet<ConnectionFlags> connectionFlagSet, boolean waitForConnection) {

        long connectionFlagMask = 0;

        for(ConnectionFlags connectionFlag : connectionFlagSet) {

            switch(connectionFlag) {

            case REQUIRE_AUTHENTICATION:
                connectionFlagMask |= CONNECTION_FLAG_REQUIRE_AUTHENTICATION;
            case REQUIRE_ENCRYPTION:
                connectionFlagMask |= CONNECTION_FLAG_REQUIRE_ENCRYPTION;
            case REPORT_MODE:
                connectionFlagMask |= CONNECTION_FLAG_REPORT_MODE;
            case PARSE_BOOT:
                connectionFlagMask |= CONNECTION_FLAG_PARSE_BOOT;
            }
        }

        byte[] address = remoteDeviceAddress.internalByteArray();

        return connectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], connectionFlagMask, waitForConnection);
    }

    /**
     * Disconnects from a remote HID device.
     * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
     * @param sendVirtualCableDisconnect {@code True} if the device is to be disconnected via a Virtual Cable Disconnection, or {@code false} if the device is to be disconnected at the Bluetooth link.
     * @return Zero if successful, or a negative return error code.
     */
    public int disconnectDevice(BluetoothAddress remoteDeviceAddress, boolean sendVirtualCableDisconnect) {

        byte[] address = remoteDeviceAddress.internalByteArray();

        return disconnectDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], sendVirtualCableDisconnect);
    }

    /**
     * Returns a list of connected remote devices.
     * @param maximumRemoteDeviceListEntries Specifies an upper limit on the size of the list to be returned, or zero to return all connected devices.
     * @return An array containing the {@link BluetoothAddress}es of currently connected devices.
     */
    public BluetoothAddress[] queryConnectedDevices(long maximumRemoteDeviceListEntries) {

        int connectedDevicesReturned;
        byte[][] remoteDeviceAddresses = new byte[1][];
        BluetoothAddress[] bluetoothAddresses;

        connectedDevicesReturned = queryConnectedDevicesNative(maximumRemoteDeviceListEntries, remoteDeviceAddresses);

        if(connectedDevicesReturned > 0) {

            bluetoothAddresses = new BluetoothAddress[connectedDevicesReturned];

            for(int index = 0; index < connectedDevicesReturned; index++ ) {

                bluetoothAddresses[index] = new BluetoothAddress(remoteDeviceAddresses[0][(index * 6) + 0], remoteDeviceAddresses[0][(index * 6) + 1], remoteDeviceAddresses[0][(index * 6) + 2], remoteDeviceAddresses[0][(index * 6) + 3], remoteDeviceAddresses[0][(index * 6) + 4], remoteDeviceAddresses[0][(index * 6) + 5]);
            }

        } else {

            bluetoothAddresses = null;
        }

        return bluetoothAddresses;
    }

    /**
     * Used to specify a set of flags governing the behavior of incoming connections.
     * @param connectionFlagSet An EnumSet representing the desired flags.
     * @return Zero if successful, or a negative return error code.
     */
    public int changeIncomingConnectionFlags(EnumSet<IncomingConnectionFlags> connectionFlagSet) {

        long connectionFlagMask = 0;

        for(IncomingConnectionFlags connectionFlag : connectionFlagSet) {

            switch(connectionFlag) {

            case REQUIRE_AUTHORIZATION:
                connectionFlagMask |= INCOMING_CONNECTION_FLAG_REQUIRE_AUTHORIZATION;
            case REQUIRE_AUTHENTICATION:
                connectionFlagMask |= INCOMING_CONNECTION_FLAG_REQUIRE_AUTHENTICATION;
            case REQUIRE_ENCRYPTION:
                connectionFlagMask |= INCOMING_CONNECTION_FLAG_REQUIRE_ENCRYPTION;
            case REPORT_MODE:
                connectionFlagMask |= INCOMING_CONNECTION_FLAG_REPORT_MODE;
            case PARSE_BOOT:
                connectionFlagMask |= INCOMING_CONNECTION_FLAG_PARSE_BOOT;
            }
        }

        return changeIncomingConnectionFlagsNative(connectionFlagMask);
    }

    /**
     * Used for sending the specified HID report data to a currently connected remote device.
     * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
     * @param reportData The data report to send.
     * @return Zero if successful, or a negative return error code.
     */
    public int sendReportData(BluetoothAddress remoteDeviceAddress, byte[] reportData) {

        byte[] address = remoteDeviceAddress.internalByteArray();

        return sendReportDataNative(address[0], address[1], address[2], address[3], address[4], address[5], reportData);
    }

    protected void deviceConnectionRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {

            try {
                callbackHandler.deviceConnectionRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deviceConnected(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceConnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deviceConnectionStatus(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatusCode) {

        ConnectionStatus connectionStatus;
        EventCallback    callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {

            switch(connectionStatusCode) {
            case DEVICE_CONNECTION_STATUS_SUCCESS:
                connectionStatus = ConnectionStatus.SUCCESS;
                break;
            case DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT:
                connectionStatus = ConnectionStatus.FAILURE_TIMEOUT;
                break;
            case DEVICE_CONNECTION_STATUS_FAILURE_REFUSED:
                connectionStatus = ConnectionStatus.FAILURE_REFUSED;
                break;
            case DEVICE_CONNECTION_STATUS_FAILURE_SECURITY:
                connectionStatus = ConnectionStatus.FAILURE_SECURITY;
                break;
            case DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                connectionStatus = ConnectionStatus.FAILURE_DEVICE_POWER_OFF;
                break;
            case DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN:
                connectionStatus = ConnectionStatus.FAILURE_UNKNOWN;
                break;
            default:
                connectionStatus = null;
                break;
            }

            try {
                callbackHandler.deviceConnectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), connectionStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deviceDisconnected(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceDisconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void reportDataReceived(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] reportData) {

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.reportDataReceivedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), reportData);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void bootKeyboardKeyPress(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean keyDown, short keyModifierCode, short key) {
        throw new UnsupportedOperationException();
    }

    protected void bootKeyboardKeyRepeat(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, short keyModifierCode, short key) {
        throw new UnsupportedOperationException();
    }

    protected void bootMouse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte cX, byte cY, short buttonStateCode, byte cZ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Interface of functions called for Human Interface Hosts and Devices
     * <p>
     * It is guaranteed that no two event methods will be invoked
     * simultaneously. The implementation of each event handler must be as
     * efficient as possible, as subsequent events will not be sent until the
     * event handler returns. Event handlers must not block and wait on
     * conditions that can only be satisfied by receiving other events.
     * <p>
     * Users should note that events are sent from a thread context owned by the
     * BluetopiaPM API, so standard locking practices should be observed.
     */
    public abstract interface EventCallback {

        /**
         * Invoked when a remote device has initiated a connection request.
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         * @see HIDM#connectionRequestResponse
         */
        public void deviceConnectionRequestEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when a connection to a remote device has been established.
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         */
        public void deviceConnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked to provide information about the status of a connection request.
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         * @param connectionStatus The status of the connection request.
         */
        public void deviceConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus);

        /**
         * Invoked when a connection to a remote device has been terminated.
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         */
        public void deviceDisconnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when HID report mode data is received from a remote device.
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         * @param reportData The raw report data.
         */
        public void reportDataReceivedEvent(BluetoothAddress remoteDeviceAddress, byte[] reportData);
    }

    /**
     * Human Interface Host Manager
     */
    public static final class HumanInterfaceHostManager extends HIDM {

        /**
         * Constructor for a Human Interface Host Object
         *
         * @param hostEventCallback
         *            Callback Interface
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached
         */
        public HumanInterfaceHostManager(HostEventCallback hostEventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_HOST, hostEventCallback);
        }

        /**
         * Used to set the parameters governing keyboard key repeat behavior.<p><i>Note:</i> some operating systems natively support key repeat behavior automatically; for those that do not, this function can be used to instruct the HID Manager to simulate key repeat behavior.
         * @param repeatDelay The initial length of time (in milliseconds) to delay before starting the repeat functionality.
         * @param repeatRate The rate of repeat (in milliseconds.)
         * @return Zero if successful, or a negative return error code.
         */
        public int setKeyboardRepeatRate(long repeatDelay, long repeatRate) {

            return setKeyboardRepeatRateNative(repeatDelay, repeatRate);
        }

        @Override
        protected void bootKeyboardKeyPress(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean keyDown, short keyModifierCode, short key) {

            KeyModifiers  keyModifier;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(keyModifierCode) {

                case HID_HOST_MODIFIER_FLAG_LEFT_CTRL:
                    keyModifier = KeyModifiers.LEFT_CTRL;
                case HID_HOST_MODIFIER_FLAG_LEFT_SHIFT:
                    keyModifier = KeyModifiers.LEFT_SHIFT;
                case HID_HOST_MODIFIER_FLAG_LEFT_ALT:
                    keyModifier = KeyModifiers.LEFT_ALT;
                case HID_HOST_MODIFIER_FLAG_LEFT_GUI:
                    keyModifier = KeyModifiers.LEFT_GUI;
                case HID_HOST_MODIFIER_FLAG_RIGHT_CTRL:
                    keyModifier = KeyModifiers.RIGHT_CTRL;
                case HID_HOST_MODIFIER_FLAG_RIGHT_SHIFT:
                    keyModifier = KeyModifiers.RIGHT_SHIFT;
                case HID_HOST_MODIFIER_FLAG_RIGHT_ALT:
                    keyModifier = KeyModifiers.RIGHT_ALT;
                case HID_HOST_MODIFIER_FLAG_RIGHT_GUI:
                    keyModifier = KeyModifiers.RIGHT_GUI;
                default:
                    keyModifier = null;
                }

                try {
                    ((HostEventCallback)callbackHandler).bootKeyboardKeyPressEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), keyDown, keyModifier, key);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void bootKeyboardKeyRepeat(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, short keyModifierCode, short key) {

            KeyModifiers  keyModifier;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(keyModifierCode) {

                case HID_HOST_MODIFIER_FLAG_LEFT_CTRL:
                    keyModifier = KeyModifiers.LEFT_CTRL;
                case HID_HOST_MODIFIER_FLAG_LEFT_SHIFT:
                    keyModifier = KeyModifiers.LEFT_SHIFT;
                case HID_HOST_MODIFIER_FLAG_LEFT_ALT:
                    keyModifier = KeyModifiers.LEFT_ALT;
                case HID_HOST_MODIFIER_FLAG_LEFT_GUI:
                    keyModifier = KeyModifiers.LEFT_GUI;
                case HID_HOST_MODIFIER_FLAG_RIGHT_CTRL:
                    keyModifier = KeyModifiers.RIGHT_CTRL;
                case HID_HOST_MODIFIER_FLAG_RIGHT_SHIFT:
                    keyModifier = KeyModifiers.RIGHT_SHIFT;
                case HID_HOST_MODIFIER_FLAG_RIGHT_ALT:
                    keyModifier = KeyModifiers.RIGHT_ALT;
                case HID_HOST_MODIFIER_FLAG_RIGHT_GUI:
                    keyModifier = KeyModifiers.RIGHT_GUI;
                default:
                    keyModifier = null;
                }

                try {
                    ((HostEventCallback)callbackHandler).bootKeyboardKeyRepeatEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), keyModifier, key);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void bootMouse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte cX, byte cY, short buttonStateCode, byte cZ) {

            ButtonState   buttonState;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(buttonStateCode) {

                case HID_HOST_LEFT_BUTTON_UP:
                    buttonState = ButtonState.LEFT_BUTTON_UP;
                case HID_HOST_LEFT_BUTTON_DOWN:
                    buttonState = ButtonState.LEFT_BUTTON_DOWN;
                case HID_HOST_RIGHT_BUTTON_UP:
                    buttonState = ButtonState.RIGHT_BUTTON_UP;
                case HID_HOST_RIGHT_BUTTON_DOWN:
                    buttonState = ButtonState.RIGHT_BUTTON_DOWN;
                case HID_HOST_MIDDLE_BUTTON_UP:
                    buttonState = ButtonState.MIDDLE_BUTTON_UP;
                case HID_HOST_MIDDLE_BUTTON_DOWN:
                    buttonState = ButtonState.MIDDLE_BUTTON_DOWN;
                default:
                    buttonState = null;
                }

                try {
                    ((HostEventCallback)callbackHandler).bootMouseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), cX, cY, buttonState, cZ);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Interface of functions called for Human Interface Hosts only
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. The implementation of each event handler should be as
         * efficient as possible, as subsequent events will not be sent until
         * the event handler returns. Event handlers must not block and wait on
         * conditions that can only be satisfied by receiving other events.
         * <p>
         * Users should also note that events are sent from a thread context
         * owned by the BluetopiaPM API. Standard locking practices should be
         * observed.
         */
        public interface HostEventCallback extends EventCallback {

            /**
             * Invoked when HID boot mode keyboard event data is received from a remote device.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param keyDown Indicates whether the key has been pressed.
             * @param keyModifier A bit mask indicating the state of any currently depressed key modifiers.
             * @param key The key being pressed.
             */
            public void bootKeyboardKeyPressEvent(BluetoothAddress remoteDeviceAddress, boolean keyDown, KeyModifiers keyModifier, short key);

            /**
             * Invoked when HID boot mode keyboard event data is received as a result of a continuously depressed key.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param keyModifier A bit mask indicating the state of any currently depressed key modifiers.
             * @param key The key being pressed.
             */
            public void bootKeyboardKeyRepeatEvent(BluetoothAddress remoteDeviceAddress, KeyModifiers keyModifier, short key);

            /**
             * Invoked when HID boot mode mouse event data is received from a remote device.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param cX The cX data from the mouse.
             * @param cY The cY data from the mouse.
             * @param buttonState The button state of the mouse.
             * @param cZ The cZ data from the mouse.
             */
            public void bootMouseEvent(BluetoothAddress remoteDeviceAddress, byte cX, byte cY, ButtonState buttonState, byte cZ);
        }
    }

    private final static long RESPONSE_CONNECTION_FLAG_REPORT_MODE = 0x00000001;
    private final static long RESPONSE_CONNECTION_FLAG_PARSE_BOOT  = 0x00000002;

    /**
     * Used to specify the desired connection parameters when responding to a connection request.
     */
    public enum ResponseConnectionFlags {

        /**
         * Request raw HID report mode data.
         */
        REPORT_MODE,
        /**
         * Request parsed HID boot mode data.
         */
        PARSE_BOOT
    }

    private final static long INCOMING_CONNECTION_FLAG_REQUIRE_AUTHORIZATION  = 0x00000001;
    private final static long INCOMING_CONNECTION_FLAG_REQUIRE_AUTHENTICATION = 0x00000002;
    private final static long INCOMING_CONNECTION_FLAG_REQUIRE_ENCRYPTION     = 0x00000004;
    private final static long INCOMING_CONNECTION_FLAG_REPORT_MODE            = 0x40000000;
    private final static long INCOMING_CONNECTION_FLAG_PARSE_BOOT             = 0x80000000;

    /**
     * Used to dictate the permissible types of incoming connections.
     */
    public enum IncomingConnectionFlags {

        /**
         * Require authorization.
         */
        REQUIRE_AUTHORIZATION,
        /**
         * Require authentication.
         */
        REQUIRE_AUTHENTICATION,
        /**
         * Require encryption.
         */
        REQUIRE_ENCRYPTION,
        /**
         * Require report mode data.
         */
        REPORT_MODE,
        /**
         * Require boot mode data.
         */
        PARSE_BOOT
    }

    private final static long CONNECTION_FLAG_REQUIRE_AUTHENTICATION = 0x00000001;
    private final static long CONNECTION_FLAG_REQUIRE_ENCRYPTION     = 0x00000002;
    private final static long CONNECTION_FLAG_REPORT_MODE            = 0x00000004;
    private final static long CONNECTION_FLAG_PARSE_BOOT             = 0x00000008;

    /**
     * Used to dictate a desired type of connection.
     */
    public enum ConnectionFlags {

        /**
         * Require authentication.
         */
        REQUIRE_AUTHENTICATION,
        /**
         * Require encryption.
         */
        REQUIRE_ENCRYPTION,
        /**
         * Require report mode data.
         */
        REPORT_MODE,
        /**
         * Require boot mode data.
         */
        PARSE_BOOT
    }

    private final static int DEVICE_CONNECTION_STATUS_SUCCESS                  = 0;
    private final static int DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT          = 1;
    private final static int DEVICE_CONNECTION_STATUS_FAILURE_REFUSED          = 2;
    private final static int DEVICE_CONNECTION_STATUS_FAILURE_SECURITY         = 3;
    private final static int DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF = 4;
    private final static int DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN          = 5;

    /**
     * Enumerates the various states of a connection.
     */
    public enum ConnectionStatus {

        /**
         * The connection succeeded.
         */
        SUCCESS,
        /**
         * The connection failed due to a timeout.
         */
        FAILURE_TIMEOUT,
        /**
         * The connection failed due to refusal.
         */
        FAILURE_REFUSED,
        /**
         * The connection failed due to a security failure.
         */
        FAILURE_SECURITY,
        /**
        * The connection failed due to a powered-off device.
        */
        FAILURE_DEVICE_POWER_OFF,
        /**
        * The connection failed for unknown reasons.
        */
        FAILURE_UNKNOWN
    }
    /* Usage bit masks for the keys (CONTROL, SHIFT, ALT, GUI) in the    */
    /* modifier byte.                                                    */
    private final static int HID_HOST_MODIFIER_FLAG_LEFT_CTRL   = 0x01;
    private final static int HID_HOST_MODIFIER_FLAG_LEFT_SHIFT  = 0x02;
    private final static int HID_HOST_MODIFIER_FLAG_LEFT_ALT    = 0x04;
    private final static int HID_HOST_MODIFIER_FLAG_LEFT_GUI    = 0x08;
    private final static int HID_HOST_MODIFIER_FLAG_RIGHT_CTRL  = 0x10;
    private final static int HID_HOST_MODIFIER_FLAG_RIGHT_SHIFT = 0x20;
    private final static int HID_HOST_MODIFIER_FLAG_RIGHT_ALT   = 0x40;
    private final static int HID_HOST_MODIFIER_FLAG_RIGHT_GUI   = 0x80;

    /**
     * Enumerates the modifier keys (CONTROL, SHIFT, ALT, GUI).
     */
    public enum KeyModifiers {
        /**
         * Left control key.
         */
        LEFT_CTRL,
        /**
         * Left shift key.
         */
        LEFT_SHIFT,
        /**
         * Left alt key.
         */
        LEFT_ALT,
        /**
         * Left GUI key.
         */
        LEFT_GUI,
        /**
         * Right control key.
         */
        RIGHT_CTRL,
        /**
         * Right shift key.
         */
        RIGHT_SHIFT,
        /**
         * Right alt key.
         */
        RIGHT_ALT,
        /**
         * Right GUI key.
         */
        RIGHT_GUI,
    }

    /* The following constants are from Universal Serial Bus HID Usage   */
    /* Tables section 10 and indicate the minimum usage codes for a 104  */
    /* boot keyboard.                                                    */
    private final static int HID_HOST_RESERVED                  = 0x00;
    private final static int HID_HOST_KEYBOARD_ERROR_ROLL_OVER  = 0x01;
    private final static int HID_HOST_KEYBOARD_POST_FAIL        = 0x02;
    private final static int HID_HOST_KEYBOARD_ERROR_UNDEFINED  = 0x03;
    private final static int HID_HOST_KEYBOARD_A                = 0x04;
    private final static int HID_HOST_KEYBOARD_B                = 0x05;
    private final static int HID_HOST_KEYBOARD_C                = 0x06;
    private final static int HID_HOST_KEYBOARD_D                = 0x07;
    private final static int HID_HOST_KEYBOARD_E                = 0x08;
    private final static int HID_HOST_KEYBOARD_F                = 0x09;
    private final static int HID_HOST_KEYBOARD_G                = 0x0A;
    private final static int HID_HOST_KEYBOARD_H                = 0x0B;
    private final static int HID_HOST_KEYBOARD_I                = 0x0C;
    private final static int HID_HOST_KEYBOARD_J                = 0x0D;
    private final static int HID_HOST_KEYBOARD_K                = 0x0E;
    private final static int HID_HOST_KEYBOARD_L                = 0x0F;
    private final static int HID_HOST_KEYBOARD_M                = 0x10;
    private final static int HID_HOST_KEYBOARD_N                = 0x11;
    private final static int HID_HOST_KEYBOARD_O                = 0x12;
    private final static int HID_HOST_KEYBOARD_P                = 0x13;
    private final static int HID_HOST_KEYBOARD_Q                = 0x14;
    private final static int HID_HOST_KEYBOARD_R                = 0x15;
    private final static int HID_HOST_KEYBOARD_S                = 0x16;
    private final static int HID_HOST_KEYBOARD_T                = 0x17;
    private final static int HID_HOST_KEYBOARD_U                = 0x18;
    private final static int HID_HOST_KEYBOARD_V                = 0x19;
    private final static int HID_HOST_KEYBOARD_W                = 0x1A;
    private final static int HID_HOST_KEYBOARD_X                = 0x1B;
    private final static int HID_HOST_KEYBOARD_Y                = 0x1C;
    private final static int HID_HOST_KEYBOARD_Z                = 0x1D;
    private final static int HID_HOST_KEYBOARD_1                = 0x1E;
    private final static int HID_HOST_KEYBOARD_2                = 0x1F;
    private final static int HID_HOST_KEYBOARD_3                = 0x20;
    private final static int HID_HOST_KEYBOARD_4                = 0x21;
    private final static int HID_HOST_KEYBOARD_5                = 0x22;
    private final static int HID_HOST_KEYBOARD_6                = 0x23;
    private final static int HID_HOST_KEYBOARD_7                = 0x24;
    private final static int HID_HOST_KEYBOARD_8                = 0x25;
    private final static int HID_HOST_KEYBOARD_9                = 0x26;
    private final static int HID_HOST_KEYBOARD_0                = 0x27;
    private final static int HID_HOST_KEYBOARD_RETURN           = 0x28;
    private final static int HID_HOST_KEYBOARD_ESCAPE           = 0x29;
    private final static int HID_HOST_KEYBOARD_DELETE           = 0x2A;
    private final static int HID_HOST_KEYBOARD_TAB              = 0x2B;
    private final static int HID_HOST_KEYBOARD_SPACE_BAR        = 0x2C;
    private final static int HID_HOST_KEYBOARD_MINUS            = 0x2D; /* '-' */
    private final static int HID_HOST_KEYBOARD_EQUAL            = 0x2E; /* '=' */
    private final static int HID_HOST_KEYBOARD_LEFT_BRACKET     = 0x2F; /* '[' */
    private final static int HID_HOST_KEYBOARD_RIGHT_BRACKET    = 0x30; /* ']' */
    private final static int HID_HOST_KEYBOARD_BACK_SLASH       = 0x31; /* '\' */
    private final static int HID_HOST_KEYBOARD_NON_US_POUND     = 0x32; /* '#' */
    private final static int HID_HOST_KEYBOARD_SEMICOLON        = 0x33; /* ';' */
    private final static int HID_HOST_KEYBOARD_APOSTROPHE       = 0x34; /* ''' */
    private final static int HID_HOST_KEYBOARD_GRAVE_ACCENT     = 0x35; /* '`' */
    private final static int HID_HOST_KEYBOARD_COMMA            = 0x36; /* ',' */
    private final static int HID_HOST_KEYBOARD_DOT              = 0x37; /* '.' */
    private final static int HID_HOST_KEYBOARD_SLASH            = 0x38; /* '/' */
    private final static int HID_HOST_KEYBOARD_CAPS_LOCK        = 0x39;
    private final static int HID_HOST_KEYBOARD_F1               = 0x3A;
    private final static int HID_HOST_KEYBOARD_F2               = 0x3B;
    private final static int HID_HOST_KEYBOARD_F3               = 0x3C;
    private final static int HID_HOST_KEYBOARD_F4               = 0x3D;
    private final static int HID_HOST_KEYBOARD_F5               = 0x3E;
    private final static int HID_HOST_KEYBOARD_F6               = 0x3F;
    private final static int HID_HOST_KEYBOARD_F7               = 0x40;
    private final static int HID_HOST_KEYBOARD_F8               = 0x41;
    private final static int HID_HOST_KEYBOARD_F9               = 0x42;
    private final static int HID_HOST_KEYBOARD_F10              = 0x43;
    private final static int HID_HOST_KEYBOARD_F11              = 0x44;
    private final static int HID_HOST_KEYBOARD_F12              = 0x45;
    private final static int HID_HOST_KEYBOARD_PRINT_SCREEN     = 0x46;
    private final static int HID_HOST_KEYBOARD_SCROLL_LOCK      = 0x47;
    private final static int HID_HOST_KEYBOARD_PAUSE            = 0x48;
    private final static int HID_HOST_KEYBOARD_INSERT           = 0x49;
    private final static int HID_HOST_KEYBOARD_HOME             = 0x4A;
    private final static int HID_HOST_KEYBOARD_PAGE_UP          = 0x4B;
    private final static int HID_HOST_KEYBOARD_DELETE_FORWARD   = 0x4C;
    private final static int HID_HOST_KEYBOARD_END              = 0x4D;
    private final static int HID_HOST_KEYBOARD_PAGE_DOWN        = 0x4E;
    private final static int HID_HOST_KEYBOARD_RIGHT_ARROW      = 0x4F;
    private final static int HID_HOST_KEYBOARD_LEFT_ARROW       = 0x50;
    private final static int HID_HOST_KEYBOARD_DOWN_ARROW       = 0x51;
    private final static int HID_HOST_KEYBOARD_UP_ARROW         = 0x52;
    private final static int HID_HOST_KEYPAD_NUM_LOCK           = 0x53;
    private final static int HID_HOST_KEYPAD_SLASH              = 0x54; /* '/' */
    private final static int HID_HOST_KEYPAD_ASTERISK           = 0x55; /* '*' */
    private final static int HID_HOST_KEYPAD_MINUS              = 0x56; /* '-' */
    private final static int HID_HOST_KEYPAD_PLUS               = 0x57; /* '+' */
    private final static int HID_HOST_KEYPAD_ENTER              = 0x58;
    private final static int HID_HOST_KEYPAD_1                  = 0x59;
    private final static int HID_HOST_KEYPAD_2                  = 0x5A;
    private final static int HID_HOST_KEYPAD_3                  = 0x5B;
    private final static int HID_HOST_KEYPAD_4                  = 0x5C;
    private final static int HID_HOST_KEYPAD_5                  = 0x5D;
    private final static int HID_HOST_KEYPAD_6                  = 0x5E;
    private final static int HID_HOST_KEYPAD_7                  = 0x5F;
    private final static int HID_HOST_KEYPAD_8                  = 0x60;
    private final static int HID_HOST_KEYPAD_9                  = 0x61;
    private final static int HID_HOST_KEYPAD_0                  = 0x62;
    private final static int HID_HOST_KEYPAD_DOT                = 0x63; /* '.' */
    private final static int HID_HOST_KEYBOARD_NON_US_SLASH     = 0x64;
    private final static int HID_HOST_KEYBOARD_APPLICATION      = 0x65;

    private final static int HID_HOST_KEYBOARD_LEFT_CONTROL     = 0xE0;
    private final static int HID_HOST_KEYBOARD_LEFT_SHIFT       = 0xE1;
    private final static int HID_HOST_KEYBOARD_LEFT_ALT         = 0xE2;
    private final static int HID_HOST_KEYBOARD_LEFT_GUI         = 0xE3;
    private final static int HID_HOST_KEYBOARD_RIGHT_CONTROL    = 0xE4;
    private final static int HID_HOST_KEYBOARD_RIGHT_SHIFT      = 0xE5;
    private final static int HID_HOST_KEYBOARD_RIGHT_ALT        = 0xE6;
    private final static int HID_HOST_KEYBOARD_RIGHT_GUI        = 0xE7;

    /* The following constants define the mouse event codes              */
    private final static int HID_HOST_LEFT_BUTTON_UP            = 0x0001;
    private final static int HID_HOST_LEFT_BUTTON_DOWN          = 0x0002;
    private final static int HID_HOST_RIGHT_BUTTON_UP           = 0x0004;
    private final static int HID_HOST_RIGHT_BUTTON_DOWN         = 0x0008;
    private final static int HID_HOST_MIDDLE_BUTTON_UP          = 0x0010;
    private final static int HID_HOST_MIDDLE_BUTTON_DOWN        = 0x0020;


    /**
     * Enumerates the various mouse button states.
     */
    public enum ButtonState {
        /**
         * Left button up.
         */
        LEFT_BUTTON_UP,
        /**
         * Left button down.
         */
        LEFT_BUTTON_DOWN,
        /**
         * Right button up.
         */
        RIGHT_BUTTON_UP,
        /**
         * Right button down.
         */
        RIGHT_BUTTON_DOWN,
        /**
         * Middle button up.
         */
        MIDDLE_BUTTON_UP,
        /**
         * Middle button down.
         */
        MIDDLE_BUTTON_DOWN,
    }

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int registerDataEventCallbackNative();

    private native void unregisterDataEventCallbackNative();

    private native int connectionRequestResponseNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, boolean accept, long connectionFlags);

    private native int connectRemoteDeviceNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, long connectionFlags, boolean waitForConnection);

    private native int disconnectDeviceNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, boolean sendVirtualCableDisconnect);

    private native int queryConnectedDevicesNative(long maximumRemoteDeviceListEntries, byte[][] remoteDeviceAddresses);

    private native int changeIncomingConnectionFlagsNative(long connectionFlagMask);

    private native int sendReportDataNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, byte[] reportData);

    protected native int setKeyboardRepeatRateNative(long repeatDelay, long repeatRate);

}
