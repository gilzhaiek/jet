/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Phone Book Access Profile Sample for Stonestreet One Bluetooth Protocol
 * Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.pbam;

import java.util.EnumSet;

import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.PBAM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.PBAM.Filter;
import com.stonestreetone.bluetopiapm.PBAM.ListOrder;
import com.stonestreetone.bluetopiapm.PBAM.PathOption;
import com.stonestreetone.bluetopiapm.PBAM.PhonebookAccessClientManager;
import com.stonestreetone.bluetopiapm.PBAM.PhonebookAccessClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.PBAM.SearchAttribute;
import com.stonestreetone.bluetopiapm.PBAM.VCardFormat;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

/**
 * Primary Activity for this sample application.
 *
 * @author Greg Hensley
 */
public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "PBAM_Sample";

    /*package*/ PhonebookAccessClientManager clientManager;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {

            try {
                clientManager = new PhonebookAccessClientManager(clientEventCallback);
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
            if(clientManager != null) {
                clientManager.dispose();
                clientManager = null;
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
        return 6;
    }

    private final ClientEventCallback clientEventCallback = new ClientEventCallback() {

        @Override
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, com.stonestreetone.bluetopiapm.PBAM.ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, int reason) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.disconnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(reason);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void phoneBookSetEvent(BluetoothAddress remoteDeviceAddress, int status, String currentPath) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.phonebookSetEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(status);
            sb.append(", ").append(currentPath);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void phoneBookSizeEvent(BluetoothAddress remoteDeviceAddress, int status, int size) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.phonebookSizeEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(status);
            sb.append(", Size: ").append(size);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void vCardDataEvent(BluetoothAddress remoteDeviceAddress, int status, boolean isFinal, int newMissedCalls, byte[] data) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.phonebookPullEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(",\n      status: ").append(status);
            sb.append(",\n      final: ").append(isFinal);
            sb.append(",\n      missed calls: " ).append(newMissedCalls);
            sb.append(",\n      data: ").append(new String(data));

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void vCardListingEvent(BluetoothAddress remoteDeviceAddress, int status, boolean isFinal, int newMissedCalls, byte[] data) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.vCardListingEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(",\n      status: ").append(status);
            sb.append(",\n      final: ").append(isFinal);
            sb.append(",\n      missed calls: " ).append(newMissedCalls);
            sb.append(",\n      data: ").append(new String(data));

            displayMessage("");
            displayMessage(sb);

        }

    };

    private final CommandHandler connectDevice_Handler = new CommandHandler() {

        String[] connectionFlagLabels = {
                "Authentication",
                "Encryption"
        };

        boolean[] connectionFlagValues = new boolean[] {false, false};

        boolean waitForConnectionChecked = false;

        @Override
        public void run() {
            int                      result;
            TextValue                portNumberParameter;
            ChecklistValue           connectionFlagsParameter;
            CheckboxValue            waitForConnectionParameter;
            BluetoothAddress         bluetoothAddress;
            int                      remotePort;
            EnumSet<ConnectionFlags> connectionFlags;

            if(clientManager != null) {
                portNumberParameter = getCommandParameterView(0).getValueText();
                connectionFlagsParameter = getCommandParameterView(1).getValueChecklist();
                waitForConnectionParameter = getCommandParameterView(2).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    remotePort = Integer.valueOf(portNumberParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                if(connectionFlagsParameter.checkedItems[0])
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                if(connectionFlagsParameter.checkedItems[1])
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);

                result = clientManager.connectRemoteDevice(bluetoothAddress, remotePort, connectionFlags, waitForConnectionParameter.value);

                displayMessage("");
                displayMessage("connectRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(2).setModeCheckbox("Wait for connection:", waitForConnectionChecked);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler disconnectDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(clientManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.disconnect(bluetoothAddress);

                displayMessage("");
                displayMessage("disconnect() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler pullPhonebookSize_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;


            if(clientManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.pullPhoneBookSize(bluetoothAddress);

                displayMessage("");
                displayMessage("pullPhoneBookSize() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final CommandHandler setPhonebook_Handler = new CommandHandler() {
        private final String[] PathOptions = {
            "Root",
            "Up",
            "Down"
        };

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            SpinnerValue     pathOptionParameter;
            TextValue        folderNameParameter;
            PathOption       pathOption;

            if(clientManager != null) {
                pathOptionParameter = getCommandParameterView(0).getValueSpinner();
                folderNameParameter = getCommandParameterView(1).getValueText();

                if(pathOptionParameter.selectedItem == 0)
                    pathOption = PathOption.ROOT;
                else if(pathOptionParameter.selectedItem == 1)
                    pathOption = PathOption.UP;
                else
                    pathOption = PathOption.DOWN;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.setPhoneBook(bluetoothAddress, pathOption, folderNameParameter.text.toString());

                displayMessage("");
                displayMessage("setPhoneBook() result: " + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Path Option", PathOptions);
            getCommandParameterView(1).setModeText("", "Folder Name");
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final CommandHandler abort_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(clientManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.abort(bluetoothAddress);

                displayMessage("");
                displayMessage("abort() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();

        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final CommandHandler pullPhonebook_Handler = new CommandHandler() {

        private final String[] FilterNames = {
            "Adr",
            "Agent",
            "BDay",
            "Categories",
            "Class",
            "Email",
            "Formatted Name",
            "Geo",
            "Key",
            "Label",
            "Logo",
            "Mailer",
            "Name",
            "Nickname",
            "Note",
            "Org",
            "Photo",
            "ProID",
            "Rev",
            "Role",
            "Sort String",
            "Sound",
            "Telephone",
            "Title",
            "Timezone",
            "UID",
            "URL",
            "Version",
            "Timestamp"
        };

        private final boolean[] checkedValues = {
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        };

        private final String[] CardTypes = {
            "vCard21",
            "vCard30"
        };


        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        objectNameParameter;
            ChecklistValue   filterParameter;
            SpinnerValue     formatParameter;
            TextValue        maxCountParameter;
            TextValue        listOffsetParameter;
            int              maxCount;
            int              listOffset;
            EnumSet<Filter>  filterSet;
            VCardFormat      format;

            if(clientManager != null) {
                objectNameParameter = getCommandParameterView(0).getValueText();
                filterParameter = getCommandParameterView(1).getValueChecklist();
                formatParameter = getCommandParameterView(2).getValueSpinner();
                maxCountParameter = getCommandParameterView(3).getValueText();
                listOffsetParameter = getCommandParameterView(4).getValueText();

                filterSet = EnumSet.noneOf(Filter.class);

                if(filterParameter.checkedItems[0])
                    filterSet.add(Filter.ADR);
                if(filterParameter.checkedItems[1])
                    filterSet.add(Filter.AGENT);
                if(filterParameter.checkedItems[2])
                    filterSet.add(Filter.BDAY);
                if(filterParameter.checkedItems[3])
                    filterSet.add(Filter.CATEGORIES);
                if(filterParameter.checkedItems[4])
                    filterSet.add(Filter.CLASS);
                if(filterParameter.checkedItems[5])
                    filterSet.add(Filter.EMAIL);
                if(filterParameter.checkedItems[6])
                    filterSet.add(Filter.FN);
                if(filterParameter.checkedItems[7])
                    filterSet.add(Filter.GEO);
                if(filterParameter.checkedItems[8])
                    filterSet.add(Filter.KEY);
                if(filterParameter.checkedItems[9])
                    filterSet.add(Filter.LABEL);
                if(filterParameter.checkedItems[10])
                    filterSet.add(Filter.LOGO);
                if(filterParameter.checkedItems[11])
                    filterSet.add(Filter.MAILER);
                if(filterParameter.checkedItems[12])
                    filterSet.add(Filter.N);
                if(filterParameter.checkedItems[13])
                    filterSet.add(Filter.NICKNAME);
                if(filterParameter.checkedItems[14])
                    filterSet.add(Filter.NOTE);
                if(filterParameter.checkedItems[15])
                    filterSet.add(Filter.ORG);
                if(filterParameter.checkedItems[16])
                    filterSet.add(Filter.PHOTO);
                if(filterParameter.checkedItems[17])
                    filterSet.add(Filter.PROID);
                if(filterParameter.checkedItems[18])
                    filterSet.add(Filter.REV);
                if(filterParameter.checkedItems[19])
                    filterSet.add(Filter.ROLE);
                if(filterParameter.checkedItems[20])
                    filterSet.add(Filter.SORT_STRING);
                if(filterParameter.checkedItems[21])
                    filterSet.add(Filter.SOUND);
                if(filterParameter.checkedItems[22])
                    filterSet.add(Filter.TEL);
                if(filterParameter.checkedItems[23])
                    filterSet.add(Filter.TITLE);
                if(filterParameter.checkedItems[24])
                    filterSet.add(Filter.TZ);
                if(filterParameter.checkedItems[25])
                    filterSet.add(Filter.UID);
                if(filterParameter.checkedItems[26])
                    filterSet.add(Filter.URL);
                if(filterParameter.checkedItems[27])
                    filterSet.add(Filter.VERSION);
                if(filterParameter.checkedItems[28])
                    filterSet.add(Filter.X_IRMC_CALL_DATETIME);

                if(formatParameter.selectedItem == 0) {
                    format = VCardFormat.VCARD21;
                }
                else
                    format = VCardFormat.VCARD30;

                try{
                    maxCount = Integer.parseInt(maxCountParameter.text.toString());
                    listOffset = Integer.parseInt(listOffsetParameter.text.toString());
                }
                catch(NumberFormatException e) {
                    //TODO COMPLAIN
                    return;
                }

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.pullPhoneBook(bluetoothAddress, objectNameParameter.text.toString(), filterSet, format, maxCount, listOffset);

                displayMessage("");
                displayMessage("pullPhoneBook() result: " + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Object Name");
            getCommandParameterView(1).setModeChecklist("Filters", FilterNames, checkedValues);
            getCommandParameterView(2).setModeSpinner("vCard Format", CardTypes);
            getCommandParameterView(3).setModeText("", "Max List Count", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(4).setModeText("", "List Offset", InputType.TYPE_CLASS_NUMBER);

        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final CommandHandler pullvCardListing_Handler = new CommandHandler() {
        private final String[] OrderTypes = {
            "Indexed",
            "Alphabetical",
            "Phonetical",
            "Default"
        };

        private final String[] AttributeTypes = {
            "Name",
            "Number",
            "Sound",
            "Default"
        };

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        phonebookPathParameter;
            SpinnerValue     listOrderParameter;
            SpinnerValue     searchAttributeParameter;
            TextValue        searchValueParameter;
            TextValue        maxCountParameter;
            TextValue        listOffsetParameter;
            ListOrder        listOrder;
            SearchAttribute  searchAttribute;
            String           searchValue;
            int              maxCount;
            int              listOffset;

            if(clientManager != null) {
                phonebookPathParameter = getCommandParameterView(0).getValueText();
                listOrderParameter = getCommandParameterView(1).getValueSpinner();
                searchAttributeParameter = getCommandParameterView(2).getValueSpinner();
                searchValueParameter = getCommandParameterView(3).getValueText();
                maxCountParameter = getCommandParameterView(4).getValueText();
                listOffsetParameter = getCommandParameterView(5).getValueText();

                switch(listOrderParameter.selectedItem) {
                case 0:
                    listOrder = ListOrder.INDEXED;
                    break;
                case 1:
                    listOrder = ListOrder.ALPHABETICAL;
                    break;
                case 2:
                    listOrder = ListOrder.PHONETICAL;
                    break;
                default:
                    listOrder = ListOrder.DEFAULT;
                }

                switch(searchAttributeParameter.selectedItem) {
                case 0:
                    searchAttribute = SearchAttribute.NAME;
                    break;
                case 1:
                    searchAttribute = SearchAttribute.NUMBER;
                    break;
                case 3:
                    searchAttribute = SearchAttribute.SOUND;
                    break;
                default:
                    searchAttribute = SearchAttribute.DEFAULT;
                }

                try {
                    maxCount = Integer.parseInt(maxCountParameter.text.toString());
                    listOffset = Integer.parseInt(listOffsetParameter.text.toString());
                }
                catch(NumberFormatException e) {
                    //TODO complain
                    return;
                }

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                searchValue = searchValueParameter.text.toString();
                if(searchValue.length() == 0)
                    searchValue = null;

                result = clientManager.pullvCardListing(bluetoothAddress, phonebookPathParameter.text.toString(), listOrder, searchAttribute, searchValue, maxCount, listOffset);

                displayMessage("");
                displayMessage("pullPhoneBook() result: " + result);
            }


        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Phonebook Path");
            getCommandParameterView(1).setModeSpinner("List Order", OrderTypes);
            getCommandParameterView(2).setModeSpinner("SearchAttribute", AttributeTypes);
            getCommandParameterView(3).setModeText("", "SearchValue");
            getCommandParameterView(4).setModeText("", "Max List Count", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(5).setModeText("", "List Offset", InputType.TYPE_CLASS_NUMBER);
        }

        @Override
        public void unselected() {
            getCommandParameterView(5).setModeHidden();
        }

    };

    private final CommandHandler pullvCard_Handler = new CommandHandler() {

        private final String[] FilterNames = {
                "Adr",
                "Agent",
                "BDay",
                "Categories",
                "Class",
                "Email",
                "Formatted Name",
                "Geo",
                "Key",
                "Label",
                "Logo",
                "Mailer",
                "Name",
                "Nickname",
                "Note",
                "Org",
                "Photo",
                "ProID",
                "Rev",
                "Role",
                "Sort String",
                "Sound",
                "Telephone",
                "Title",
                "Timezone",
                "UID",
                "URL",
                "Version",
                "Timestamp"
            };

            private final boolean[] checkedValues = {
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            };

            private final String[] CardTypes = {
                "vCard21",
                "vCard30"
            };

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        objectNameParameter;
            ChecklistValue   filterParameter;
            SpinnerValue     formatParameter;
            EnumSet<Filter>  filterSet;
            VCardFormat      format;

            if(clientManager != null) {
                objectNameParameter = getCommandParameterView(0).getValueText();
                filterParameter = getCommandParameterView(1).getValueChecklist();
                formatParameter = getCommandParameterView(2).getValueSpinner();

                filterSet = EnumSet.noneOf(Filter.class);

                if(filterParameter.checkedItems[0])
                    filterSet.add(Filter.ADR);
                if(filterParameter.checkedItems[1])
                    filterSet.add(Filter.AGENT);
                if(filterParameter.checkedItems[2])
                    filterSet.add(Filter.BDAY);
                if(filterParameter.checkedItems[3])
                    filterSet.add(Filter.CATEGORIES);
                if(filterParameter.checkedItems[4])
                    filterSet.add(Filter.CLASS);
                if(filterParameter.checkedItems[5])
                    filterSet.add(Filter.EMAIL);
                if(filterParameter.checkedItems[6])
                    filterSet.add(Filter.FN);
                if(filterParameter.checkedItems[7])
                    filterSet.add(Filter.GEO);
                if(filterParameter.checkedItems[8])
                    filterSet.add(Filter.KEY);
                if(filterParameter.checkedItems[9])
                    filterSet.add(Filter.LABEL);
                if(filterParameter.checkedItems[10])
                    filterSet.add(Filter.LOGO);
                if(filterParameter.checkedItems[11])
                    filterSet.add(Filter.MAILER);
                if(filterParameter.checkedItems[12])
                    filterSet.add(Filter.N);
                if(filterParameter.checkedItems[13])
                    filterSet.add(Filter.NICKNAME);
                if(filterParameter.checkedItems[14])
                    filterSet.add(Filter.NOTE);
                if(filterParameter.checkedItems[15])
                    filterSet.add(Filter.ORG);
                if(filterParameter.checkedItems[16])
                    filterSet.add(Filter.PHOTO);
                if(filterParameter.checkedItems[17])
                    filterSet.add(Filter.PROID);
                if(filterParameter.checkedItems[18])
                    filterSet.add(Filter.REV);
                if(filterParameter.checkedItems[19])
                    filterSet.add(Filter.ROLE);
                if(filterParameter.checkedItems[20])
                    filterSet.add(Filter.SORT_STRING);
                if(filterParameter.checkedItems[21])
                    filterSet.add(Filter.SOUND);
                if(filterParameter.checkedItems[22])
                    filterSet.add(Filter.TEL);
                if(filterParameter.checkedItems[23])
                    filterSet.add(Filter.TITLE);
                if(filterParameter.checkedItems[24])
                    filterSet.add(Filter.TZ);
                if(filterParameter.checkedItems[25])
                    filterSet.add(Filter.UID);
                if(filterParameter.checkedItems[26])
                    filterSet.add(Filter.URL);
                if(filterParameter.checkedItems[27])
                    filterSet.add(Filter.VERSION);
                if(filterParameter.checkedItems[28])
                    filterSet.add(Filter.X_IRMC_CALL_DATETIME);

                if(formatParameter.selectedItem == 0) {
                    format = VCardFormat.VCARD21;
                }
                else
                    format = VCardFormat.VCARD30;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.pullvCard(bluetoothAddress, objectNameParameter.text.toString(), filterSet, format);

                displayMessage("");
                displayMessage("pullPhoneBook() result: " + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "vCard Name");
            getCommandParameterView(1).setModeChecklist("Filters", FilterNames, checkedValues);
            getCommandParameterView(2).setModeSpinner("vCard Format", CardTypes);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();

        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final CommandHandler setPhoneBookAbsolute_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        pathParameter;

            if(clientManager != null) {
                pathParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = clientManager.setPhoneBookAbsolute(bluetoothAddress, pathParameter.text.toString());

                displayMessage("");
                displayMessage("pullPhoneBook() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Absolute Path");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();

        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };

    private final Command[] commandList = new Command[] {
        new Command("Connect Device", connectDevice_Handler),
        new Command("Disconnect Device", disconnectDevice_Handler),
        new Command("Pull PhoneBook Size", pullPhonebookSize_Handler),
        new Command("Set PhoneBook", setPhonebook_Handler),
        new Command("Abort", abort_Handler),
        new Command("Pull PhoneBook", pullPhonebook_Handler),
        new Command("Pull vCard Listing", pullvCardListing_Handler),
        new Command("Pull vCard", pullvCard_Handler),
        new Command("Set PhoneBook Absolute", setPhoneBookAbsolute_Handler)
    };

}
