/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Personal Area Network Manager Sample for Stonestreet One Bluetooth Protocol
 * Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.panm;

import java.util.EnumSet;

import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.PANM;
import com.stonestreetone.bluetopiapm.PANM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.PANM.ConnectionStatus;
import com.stonestreetone.bluetopiapm.PANM.CurrentConfiguration;
import com.stonestreetone.bluetopiapm.PANM.EventCallback;
import com.stonestreetone.bluetopiapm.PANM.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.PANM.ServiceType;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "PANM_Sample";

    /*package*/ PANM panManager;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {

            try {
                panManager = new PANM(eventCallback);
                return true;
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                return false;
            }
        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this) {
            if(panManager != null) {
                panManager.dispose();
                panManager = null;
            }
        }
    }

    @Override
    protected Command[] getCommandList() {
        return commandList;
    }

    @Override
    protected int getNumberProfileParameters() {
        return 0;
    }

    @Override
    protected int getNumberCommandParameters() {
        return 4;
    }

    private final EventCallback eventCallback = new EventCallback() {

        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.serviceTypeLabel)).append(" ").append(serviceType);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType, ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.serviceTypeLabel)).append(" ").append(serviceType);
            sb.append(", ").append(resourceManager.getString(R.string.statusLabel)).append(" ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.disconnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.serviceTypeLabel)).append(" ").append(serviceType);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.incomingConnectionRequestEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }


    };

    private final CommandHandler connectRequestResponse_Handler = new CommandHandler() {

        private final boolean acceptConnectionChecked = false;

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            CheckboxValue    acceptConnectionParameter;

            if(panManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                acceptConnectionParameter = getCommandParameterView(0).getValueCheckbox();

                result = panManager.connectionRequestResponse(bluetoothAddress, acceptConnectionParameter.value);

                displayMessage("");
                displayMessage("connectionRequestResponse() result: " + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Accept Connection:", acceptConnectionChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

    };

    private final CommandHandler connectRemoteDevice_Handler = new CommandHandler() {

        private final String[] serviceTypes = {
                "Personal Area Network User",
                "Network Access Point",
                "Group Adhoc Network"
        };

        private final String[] connectionFlagLabels = {
                "Authentication",
                "Encryption"
        };

        private final boolean[] connectionFlagValues = new boolean[] {false, false};

        private final boolean waitForConnectionChecked = false;

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;

            ServiceType              localServiceType;
            ServiceType              remoteServiceType;

            SpinnerValue             localServiceTypeParameter;
            SpinnerValue             remoteServiceTypeParameter;

            ChecklistValue           connectionFlagsParameter;
            EnumSet<ConnectionFlags> connectionFlags;

            CheckboxValue            waitForConnectionParameter;

            if(panManager != null) {
                localServiceTypeParameter  = getCommandParameterView(0).getValueSpinner();
                remoteServiceTypeParameter = getCommandParameterView(1).getValueSpinner();
                connectionFlagsParameter   = getCommandParameterView(2).getValueChecklist();
                waitForConnectionParameter = getCommandParameterView(3).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                switch(localServiceTypeParameter.selectedItem)
                {
                   case 0:
                   default:
                       localServiceType = ServiceType.PERSONAL_AREA_NETWORK_USER;
                       break;
                   case 1:
                       localServiceType = ServiceType.NETWORK_ACCESS_POINT;
                       break;
                   case 2:
                       localServiceType = ServiceType.GROUP_ADHOC_NETWORK;
                       break;
                }

                switch(remoteServiceTypeParameter.selectedItem) {
                   case 0:
                   default:
                       remoteServiceType = ServiceType.PERSONAL_AREA_NETWORK_USER;
                       break;
                   case 1:
                       remoteServiceType = ServiceType.NETWORK_ACCESS_POINT;
                       break;
                   case 2:
                       remoteServiceType = ServiceType.GROUP_ADHOC_NETWORK;
                       break;
                }

                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                if(connectionFlagsParameter.checkedItems[0])
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                if(connectionFlagsParameter.checkedItems[1])
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);

                result = panManager.connectRemoteDevice(bluetoothAddress, localServiceType, remoteServiceType, connectionFlags, waitForConnectionParameter.value);

                displayMessage("");
                displayMessage("connectRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Local Service Type", serviceTypes);
            getCommandParameterView(1).setModeSpinner("Remote Service Type", serviceTypes);
            getCommandParameterView(2).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(3).setModeCheckbox("Wait for connection:", waitForConnectionChecked);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

    };

    private final CommandHandler disconnectRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(panManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = panManager.disconnectRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler queryConnectedDevices_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                index = 0;
            BluetoothAddress   bluetoothAddress;
            BluetoothAddress[] bluetoothAddresses;

            if(panManager != null) {
                bluetoothAddresses = panManager.queryConnectedDevices();

                StringBuilder sb = new StringBuilder();

                sb.append("queryConnectedDevices() result: ");

                if(bluetoothAddresses != null) {
                    sb.append(bluetoothAddresses.length).append("\n");

                    for(index = 0; index < bluetoothAddresses.length; index++) {
                        bluetoothAddress = bluetoothAddresses[index];
                        sb.append("   ").append(bluetoothAddress.toString()).append("\n");
                    }
                } else {
                    sb.append("null");
                }

                displayMessage("");
                displayMessage(sb);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler queryCurrentConfiguration_Handler = new CommandHandler() {

        @Override
        public void run() {
            boolean              firstFlag;
            CurrentConfiguration currentConfiguration;

            if(panManager != null) {
                currentConfiguration = panManager.queryCurrentConfiguration();

                StringBuilder sb = new StringBuilder();

                sb.append("queryCurrentConfiguration() result: ");

                if(currentConfiguration != null) {
                    sb.append("Success\n");
                    sb.append("  Incoming Connection Flags:\n");

                    if(currentConfiguration.incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION))
                        sb.append("    ").append(resourceManager.getString(R.string.authorizationRequiredLabel)).append("\n");

                    if(currentConfiguration.incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION))
                        sb.append("    ").append(resourceManager.getString(R.string.authenticationRequiredLabel)).append("\n");

                    if(currentConfiguration.incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION))
                        sb.append("    ").append(resourceManager.getString(R.string.encryptionRequiredLabel)).append("\n");

                    sb.append("  Service Types:\n");

                    if(currentConfiguration.serviceTypes.contains(ServiceType.PERSONAL_AREA_NETWORK_USER))
                        sb.append("    ").append(resourceManager.getString(R.string.personalAreaNetworkUserLabel)).append("\n");

                    if(currentConfiguration.serviceTypes.contains(ServiceType.NETWORK_ACCESS_POINT))
                        sb.append("    ").append(resourceManager.getString(R.string.authenticationRequiredLabel)).append("\n");

                    if(currentConfiguration.serviceTypes.contains(ServiceType.GROUP_ADHOC_NETWORK))
                        sb.append("    ").append(resourceManager.getString(R.string.groupAdhocNetworkLabel)).append("\n");
                } else {
                    sb.append("Failure\n");
                }

                displayMessage("");
                displayMessage(sb);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler changeIncomingConnectionFlags_Handler = new CommandHandler() {

        private final String[] incomingConnectionFlagLabels = {
            "Authorization",
            "Authentication",
            "Encryption"
        };

        private final boolean[] incomingConnectionFlagValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            int                              result;

            ChecklistValue                   incomingConnectionFlagsParameter;
            EnumSet<IncomingConnectionFlags> incomingConnectionFlags;

            if(panManager != null) {
                incomingConnectionFlagsParameter = getCommandParameterView(0).getValueChecklist();

                incomingConnectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);

                if(incomingConnectionFlagsParameter.checkedItems[0])
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                if(incomingConnectionFlagsParameter.checkedItems[1])
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                if(incomingConnectionFlagsParameter.checkedItems[2])
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

                result = panManager.changeIncomingConnectionFlags(incomingConnectionFlags);

                displayMessage("");
                displayMessage("changeIncomingConnectionFlags() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Incoming Connection Flags", incomingConnectionFlagLabels, incomingConnectionFlagValues);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
        // TODO Auto-generated method stub

    }

};

    private final Command[] commandList = new Command[] {
        new Command("Connect Request Response", connectRequestResponse_Handler),
        new Command("Connect Remote Device", connectRemoteDevice_Handler),
        new Command("Disconnect Remote Device", disconnectRemoteDevice_Handler),
        new Command("Query Connected Devices", queryConnectedDevices_Handler),
        new Command("Query Current Configuration", queryCurrentConfiguration_Handler),
        new Command("Change Incoming Connection Flags", changeIncomingConnectionFlags_Handler),
    };

}
