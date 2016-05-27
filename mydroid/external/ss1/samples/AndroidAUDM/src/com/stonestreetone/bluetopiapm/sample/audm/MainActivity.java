/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Audio Manager API (A2DP and AVRCP 1.0 Profiles) Sample for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.audm;

import java.util.EnumSet;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.AUDM.AudioSinkManager;
import com.stonestreetone.bluetopiapm.AUDM.AudioSinkManager.AudioSinkEventCallback;
import com.stonestreetone.bluetopiapm.AUDM.AudioStreamConfiguration;
import com.stonestreetone.bluetopiapm.AUDM.AudioStreamConnectionState;
import com.stonestreetone.bluetopiapm.AUDM.AudioStreamFormat;
import com.stonestreetone.bluetopiapm.AUDM.AudioStreamState;
import com.stonestreetone.bluetopiapm.AUDM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.AUDM.ConnectionRequestType;
import com.stonestreetone.bluetopiapm.AUDM.ConnectionStatus;
import com.stonestreetone.bluetopiapm.AUDM.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.AUDM.RemoteControlDisconnectReason;
import com.stonestreetone.bluetopiapm.AUDM.RemoteControlPassThroughCommand;
import com.stonestreetone.bluetopiapm.AUDM.RemoteControlPassThroughOperationID;
import com.stonestreetone.bluetopiapm.AUDM.RemoteControlPassThroughResponse;
import com.stonestreetone.bluetopiapm.AUDM.RemoteControlPassThroughState;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.SBC.Decoder;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

public class MainActivity extends SS1SampleActivity
{

    private static final String LOG_TAG = "AUDM_Sample";

    /*package*/ AudioSinkManager sinkManager;

    Decoder audioStreamDecoder;
    Decoder.DecoderConfiguration audioStreamDecoderConfiguration;

    AudioTrack audioTrack;

    int audioTrackStreamType =  AudioManager.STREAM_MUSIC;
    String audioTrackStreamTypeString =  "Music Stream";

    int audioTrackBufferMode = AudioTrack.MODE_STREAM;
    String audioTrackBufferModeString = "Stream Mode";

    int audioTrackPCMSampleSize = AudioFormat.ENCODING_PCM_16BIT;
    String audioTrackPCMSampleSizeString = "16 Bit";

    int countFrames = 0;

    @Override
    protected boolean profileEnable()
    {

        synchronized(this)
        {

            try
            {

                sinkManager = new AudioSinkManager(true, true, sinkEventCallback);

            }
            catch(Exception e)
            {

                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                return false;
            }

            try
            {

                audioStreamDecoder = new Decoder();
                audioStreamDecoderConfiguration = new Decoder.DecoderConfiguration();

            }
            catch(Exception e) {

                // throw error
                return false;
            }

            return true;
        }

    }

    @Override
    protected void profileDisable()
    {

        synchronized(MainActivity.this)
        {

            if(sinkManager != null)
            {

                sinkManager.dispose();
                sinkManager = null;

            }

            if(audioStreamDecoder != null)
            {

                audioStreamDecoder.dispose();
                audioStreamDecoderConfiguration = null;
                audioStreamDecoder = null;

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
        return 2;
    }

    private final AudioSinkEventCallback sinkEventCallback = new AudioSinkEventCallback()
    {

        @Override
        public void audioStreamConnectedEvent(BluetoothAddress remoteDeviceAddress, int mediaMTU, AudioStreamFormat streamFormat)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioStreamConnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.MTULabel)).append(" ").append(mediaMTU);
            sb.append(", ").append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamFormat.sampleFrequency);
            sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamFormat.numberChannels);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void audioStreamConnectionStatusEvent(ConnectionStatus connectionStatus, int mediaMTU, AudioStreamFormat streamFormat)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioStreamConnectionStatusEventLabel)).append(": ");
            sb.append(resourceManager.getString(R.string.MTULabel)).append(" ").append(mediaMTU);
            sb.append(", ").append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamFormat.sampleFrequency);
            sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamFormat.numberChannels);
            sb.append(", ").append(resourceManager.getString(R.string.statusLabel)).append(" ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void audioStreamDisconnectedEvent()
        {

            if (audioTrack != null)
            {

                audioTrack.release();

            }

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioStreamDisconnectedEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void audioStreamFormatChangedEvent(AudioStreamFormat streamFormat)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioStreamFormatChangedEventLabel)).append(": ");
            sb.append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamFormat.sampleFrequency);
            sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamFormat.numberChannels);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void audioStreamStateChangedEvent(AudioStreamState streamState)
        {

            if (streamState == AudioStreamState.STOPPED)
            {

                if (audioTrack != null)
                {

                    audioTrack.release();

                }

            }

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioStreamStateChangedEventLabel)).append(": ");
            sb.append(resourceManager.getString(R.string.stateLabel)).append(" ").append(streamState);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void changeAudioStreamFormatStatusEvent(boolean successful, AudioStreamFormat streamFormat)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.changeAudioStreamFormatStatusEventLabel)).append(": ");
            if (successful)
            {
                sb.append(resourceManager.getString(R.string.successLabel));
            }
            else
            {
                sb.append(resourceManager.getString(R.string.failureLabel));
            }

            sb.append(", ").append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamFormat.sampleFrequency);
            sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamFormat.numberChannels);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void changeAudioStreamStateStatusEvent(boolean successful, AudioStreamState streamState)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.changeAudioStreamStateStatusEventLabel)).append(": ");
            if (successful)
            {

                if (audioTrack != null)
                {

                    if (streamState == AudioStreamState.STOPPED)
                    {

                        audioTrack.release();

                    }

                }

                sb.append(resourceManager.getString(R.string.successLabel));

            }
            else
            {
                sb.append(resourceManager.getString(R.string.failureLabel));
            }

            sb.append(", ").append(resourceManager.getString(R.string.stateLabel)).append(" ").append(streamState);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress, ConnectionRequestType requestType)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.incomingConnectionRequestEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.requestTypeLabel)).append(" ").append(requestType);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void encodedAudioStreamDataEvent(byte[] encodedAudioData)
        {

            //int availableDecodedFrames = 0;

            int countDecodedFrames = 0;

            int sizeDecodedFrames = 0;

            byte[] audioFrameData = new byte[512];

            StringBuilder sb = new StringBuilder();

            int channelConfiguration = 0;

            int bufferSize = 0;

            sb.append(resourceManager.getString(R.string.encodedAudioStreamDataEventLabel)).append(": ");

            if (encodedAudioData != null)
            {
                countDecodedFrames = audioStreamDecoder.decodeSBCFrames(encodedAudioData);

                //availableDecodedFrames = audioStreamDecoder.availableAudioFrames();

                countFrames = countFrames + countDecodedFrames;

                for(int index = 0; index < countDecodedFrames; index++)
                {

                    sizeDecodedFrames = audioStreamDecoder.getNextAudioFrame(audioFrameData, audioStreamDecoderConfiguration);

                    if (sizeDecodedFrames > 0)
                    {

                        if ((audioTrack == null) || (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED))
                        {

                            switch (audioStreamDecoderConfiguration.channelMode)
                            {

                                case MONO:

                                    channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
                                    sb.append("\n").append(resourceManager.getString(R.string.audioTrackChannelConfigurationLabel)).append(": ");
                                    sb.append("Mono");

                                case DUAL_CHANNEL:

                                    channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
                                    sb.append("\n").append(resourceManager.getString(R.string.audioTrackChannelConfigurationLabel)).append(": ");
                                    sb.append("Mono");

                                case STEREO:

                                    channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
                                    sb.append("\n").append(resourceManager.getString(R.string.audioTrackChannelConfigurationLabel)).append(": ");
                                    sb.append("Stereo");

                                case JOINT_STEREO:

                                    channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
                                    sb.append("\n").append(resourceManager.getString(R.string.audioTrackChannelConfigurationLabel)).append(": ");
                                    sb.append("Stereo");

                            }

                            bufferSize = AudioTrack.getMinBufferSize(audioStreamDecoderConfiguration.samplingFrequency, channelConfiguration, audioTrackPCMSampleSize);

                            audioTrack = new AudioTrack(audioTrackStreamType, audioStreamDecoderConfiguration.samplingFrequency, channelConfiguration, audioTrackPCMSampleSize, bufferSize, audioTrackBufferMode);

                            audioTrack.play();

                            displayMessage("");
                            displayMessage(sb);
                            displayMessage(resourceManager.getString(R.string.audioTrackBufferSizeLabel) + ": " + bufferSize);
                            displayMessage(resourceManager.getString(R.string.audioTrackStreamTypeLabel) + ": " + audioTrackStreamType);
                            displayMessage(resourceManager.getString(R.string.audioTrackBufferModeLabel) + ": " + audioTrackBufferMode);
                            displayMessage(resourceManager.getString(R.string.audioTrackPCMSampleSizeLabel) + ": " + audioTrackPCMSampleSizeString);

                        }

                        audioTrack.write(audioFrameData, 0, audioFrameData.length);

                    }

                }

                if ((countFrames % (countDecodedFrames * 200)) == 0 )
                {
                    displayMessage("");
                    displayMessage(sb);

                    displayMessage(resourceManager.getString(R.string.lengthEncodedAudioDataLabel) + ": " + encodedAudioData.length);
                    displayMessage(resourceManager.getString(R.string.sizeEncodedFrameLabel) + ": " + audioStreamDecoderConfiguration.frameLength);
                    displayMessage(resourceManager.getString(R.string.countDecodedFramesLabel) + ": " + countDecodedFrames);
                    displayMessage(resourceManager.getString(R.string.sizeDecodedFrameLabel) + ": " + sizeDecodedFrames);
                    displayMessage(resourceManager.getString(R.string.frequencyLabel) + ": " + audioStreamDecoderConfiguration.samplingFrequency);
                    displayMessage(resourceManager.getString(R.string.blockSizeLabel) + ": " + audioStreamDecoderConfiguration.blockSize);
                    displayMessage(resourceManager.getString(R.string.channelModeLabel) + ": " + audioStreamDecoderConfiguration.channelMode);
                    displayMessage(resourceManager.getString(R.string.allocationMethodLabel) + ": " + audioStreamDecoderConfiguration.allocationMethod);
                    displayMessage(resourceManager.getString(R.string.subBandsLabel) + ": " + audioStreamDecoderConfiguration.subbands);
                    displayMessage(resourceManager.getString(R.string.bitRateLabel) + ": " + audioStreamDecoderConfiguration.bitRate);
                    displayMessage(resourceManager.getString(R.string.bitPoolLabel) + ": " + audioStreamDecoderConfiguration.bitPool);
                    displayMessage(resourceManager.getString(R.string.frameLengthLabel) + ": " + audioStreamDecoderConfiguration.frameLength);

                    countFrames = 0;
                }

            }
            else
            {
                sb.append(resourceManager.getString(R.string.lengthEncodedAudioDataLabel)).append(" ").append("null");
                displayMessage("");
                displayMessage(sb);

            }

        }

        @Override
        public void remoteControlPassThroughCommandConfirmationEvent(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, RemoteControlPassThroughResponse response)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteControlPassThroughCommandConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.transactionIDLabel)).append(" ").append(transactionID);
            sb.append(", ").append(resourceManager.getString(R.string.statusLabel)).append(" ").append(status);
            sb.append(", ").append(resourceManager.getString(R.string.responseCodeLabel)).append(" ").append(response.responseCode);
            sb.append(", ").append(resourceManager.getString(R.string.subunitTypeLabel)).append(" ").append(response.subunitType);
            sb.append(", ").append(resourceManager.getString(R.string.subunitIDLabel)).append(" ").append(response.subunitID);
            sb.append(", ").append(resourceManager.getString(R.string.operationIDLabel)).append(" ").append(response.operationID);
            sb.append(", ").append(resourceManager.getString(R.string.stateFlagLabel)).append(" ").append(response.stateFlag);


        }

        @Override
        public void remoteControlConnectedEvent(BluetoothAddress remoteDeviceAddress)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteControlConnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void remoteControlConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteControlConnectionStatusEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.statusLabel)).append(" ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void remoteControlDisconnectedEvent(BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason)
        {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteControlDisconnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resourceManager.getString(R.string.statusLabel)).append(" ").append(disconnectReason);

            displayMessage("");
            displayMessage(sb);

        }

    };

    private final CommandHandler connectRequestResponse_Handler = new CommandHandler()
    {

        String[] requestTypes = {
                "AUDIO",
                "REMOTE CONTROL"
        };

        boolean acceptConnectionChecked = false;

        @Override
        public void run() {

            int                      result;
            BluetoothAddress         bluetoothAddress;

            ConnectionRequestType    requestType;
            SpinnerValue             requestTypeParameter;

            CheckboxValue            acceptConnectionParameter;

            if(sinkManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                requestTypeParameter        = getCommandParameterView(0).getValueSpinner();

                switch(requestTypeParameter.selectedItem)
                {
                   case 0:
                       default:
                       requestType = ConnectionRequestType.AUDIO;
                       break;
                   case 1:
                       requestType = ConnectionRequestType.REMOTE_CONTROL;
                       break;
                }

                acceptConnectionParameter   = getCommandParameterView(1).getValueCheckbox();

                result = sinkManager.connectionRequestResponse(requestType,bluetoothAddress, acceptConnectionParameter.value);

                displayMessage("");
                displayMessage("connectionRequestResponse() result: " + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Connection Request Type", requestTypes);
            getCommandParameterView(1).setModeCheckbox("Accept Connection:", acceptConnectionChecked);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler connectAudioStream_Handler = new CommandHandler() {

            String[] connectionFlagLabels = {
                    "Authentication",
                    "Encryption"
            };

            boolean[] connectionFlagValues = new boolean[] {false, false};

            boolean waitForConnectionChecked = false;

            @Override
            public void run() {

                int                      result;
                BluetoothAddress         bluetoothAddress;

                ChecklistValue           connectionFlagsParameter;
                EnumSet<ConnectionFlags> connectionFlags;

                CheckboxValue            waitForConnectionParameter;

                if(sinkManager != null) {

                    connectionFlagsParameter = getCommandParameterView(0).getValueChecklist();
                    waitForConnectionParameter = getCommandParameterView(1).getValueCheckbox();

                    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                        showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                        return;
                    }

                    connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                    if(connectionFlagsParameter.checkedItems[0]) {
                        connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                    }
                    if(connectionFlagsParameter.checkedItems[1]) {
                        connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                    }

                    result = sinkManager.connectAudioStream(bluetoothAddress, connectionFlags, waitForConnectionParameter.value);

                    displayMessage("");
                    displayMessage("connectAudioStream() result: " + result);

                }
            }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(1).setModeCheckbox("Wait for connection:", waitForConnectionChecked);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler disconnectAudioStream_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;

            if(sinkManager != null) {

                result = sinkManager.disconnectAudioStream();

                displayMessage("");
                displayMessage("disconnectAudioStream() result: " + result);
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler queryAudioStreamConnected_Handler = new CommandHandler() {

        @Override
        public void run() {

            AudioStreamConnectionState connectionState;

            if(sinkManager != null) {

                connectionState = sinkManager.queryAudioStreamConnected();

                displayMessage("");

                displayMessage("queryAudioStreamConnected() result: " + connectionState);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler queryAudioStreamConnectedDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            BluetoothAddress remoteDeviceAddress;

            if(sinkManager != null) {

                remoteDeviceAddress = sinkManager.queryAudioStreamConnectedDevice();

                displayMessage("");
                if(remoteDeviceAddress != null) {
                    displayMessage("queryAudioStreamConnectedDevice() result: " + remoteDeviceAddress.toString());
                }
                else{
                    displayMessage("queryAudioStreamConnectedDevice() result: null");
                }
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler queryAudioStreamState_Handler = new CommandHandler() {

        @Override
        public void run() {

            AudioStreamState streamState;

            if(sinkManager != null) {

                streamState = sinkManager.queryAudioStreamState();

                displayMessage("");
                displayMessage("queryAudioStreamState() result: " + streamState);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler queryAudioStreamFormat_Handler = new CommandHandler() {

        @Override
        public void run() {

            AudioStreamFormat streamFormat;

            if(sinkManager != null) {

                streamFormat = sinkManager.queryAudioStreamFormat();

                StringBuilder sb = new StringBuilder();

                if (streamFormat != null){
                    sb.append("queryAudioStreamFormat() result: ");
                    sb.append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamFormat.sampleFrequency);
                    sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamFormat.numberChannels);
                }
                else{
                    sb.append("queryAudioStreamFormat() result: null");
                }
                displayMessage("");
                displayMessage(sb);

            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler queryAudioStreamConfiguration_Handler = new CommandHandler() {

        @Override
        public void run() {

            AudioStreamConfiguration streamConfiguration;

            if(sinkManager != null) {

                streamConfiguration = sinkManager.queryAudioStreamConfiguration();

                StringBuilder sb = new StringBuilder();

                if (streamConfiguration != null){
                    sb.append("queryAudioStreamConfiguration() result: ");
                    sb.append(resourceManager.getString(R.string.MTULabel)).append(" ").append(streamConfiguration.mediaMTU);
                    sb.append(", ").append(resourceManager.getString(R.string.frequencyLabel)).append(" ").append(streamConfiguration.streamFormat.sampleFrequency);
                    sb.append(", ").append(resourceManager.getString(R.string.channelsLabel)).append(" ").append(streamConfiguration.streamFormat.numberChannels);
                    sb.append(", ").append(resourceManager.getString(R.string.codecTypeLabel)).append(" ").append(streamConfiguration.mediaCodecType);
                    // TODO add Codec Information
                }
                else{
                    sb.append("queryAudioStreamConfiguration() result: null");
                }

                displayMessage("");
                displayMessage(sb);

            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler changeAudioStreamState_Handler = new CommandHandler() {

        String[] streamStates = {
                "Start",
                "Stop"
        };

        @Override
        public void run() {

            int                      result;

            AudioStreamState         streamState;
            SpinnerValue             streamStateParameter;

            if(sinkManager != null) {

                streamStateParameter = getCommandParameterView(0).getValueSpinner();

                switch(streamStateParameter.selectedItem)
                {
                   case 0:
                       default:
                       streamState = AudioStreamState.STARTED;
                       break;
                   case 1:
                       streamState = AudioStreamState.STOPPED;
                       break;
                }

                result = sinkManager.changeAudioStreamState(streamState);

                displayMessage("");
                displayMessage("changeAudioStreamState() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Connection States", streamStates);
            getCommandParameterView(1).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler changeAudioStreamFormat_Handler = new CommandHandler() {

        @Override
        public void run() {

            if(sinkManager != null) {

                int result;

                long sampleFrequency;
                TextValue frequencyParameter;

                int numberChannels;
                TextValue                numberChannelsParameter;

                frequencyParameter = getCommandParameterView(0).getValueText();
                numberChannelsParameter = getCommandParameterView(1).getValueText();

                try {
                    sampleFrequency = Integer.valueOf(frequencyParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    numberChannels = Integer.valueOf(numberChannelsParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                AudioStreamFormat streamFormat = new AudioStreamFormat(sampleFrequency, numberChannels);
                result = sinkManager.changeAudioStreamFormat(streamFormat);

                displayMessage("");
                displayMessage("changeAudioStreamFormat() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Frequency", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Number of Channels", InputType.TYPE_CLASS_NUMBER);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler changeIncomingFlags_Handler = new CommandHandler() {

        String[] connectionFlagLabels =
        {

            "Authentication",
            "Encryption"

        };

        boolean[] connectionFlagValues = new boolean[] {false, false};

        @Override
        public void run()
        {

            int                      result;

            ChecklistValue           connectionFlagsParameter;
            EnumSet<IncomingConnectionFlags> connectionFlags;

            if(sinkManager != null)
            {

                connectionFlagsParameter = getCommandParameterView(0).getValueChecklist();

                connectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);
                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                }

                result = sinkManager.changeIncomingConnectionFlags(connectionFlags);

                displayMessage("");
                displayMessage("changeIncomingConnectionFlags() result: " + result);

            }

        }

        @Override
        public void selected()
        {

            getCommandParameterView(0).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(1).setModeHidden();

        }

        @Override
        public void unselected() {
        // TODO Auto-generated method stub

        }

    };
    private final CommandHandler sendRemoteControlCommand_Handler = new CommandHandler()
    {

        String[] remoteControlPassThroughOperationIDs = {
            "Play",
            "Pause",
            "Stop",
            "Volume Up",
            "Volume Down",
            "Forward",
            "Backward"
        };

        @Override
        public void run() {

            if(sinkManager != null) {

                int result;

                BluetoothAddress bluetoothAddress;

                RemoteControlPassThroughOperationID remoteControlPassThroughOperationID;
                SpinnerValue remoteControlPassThroughOperationIDParameter;
                RemoteControlPassThroughCommand remoteControlPassThroughCommand;

                long responseTimeout;
                TextValue responseTimeoutParameter;

                remoteControlPassThroughOperationIDParameter = getCommandParameterView(0).getValueSpinner();
                responseTimeoutParameter = getCommandParameterView(1).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                switch(remoteControlPassThroughOperationIDParameter.selectedItem)
                {
                   case 0:
                       default:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.PLAY;
                       break;
                   case 1:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.PAUSE;
                       break;
                   case 2:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.STOP;
                       break;
                   case 3:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.VOLUME_UP;
                       break;
                   case 4:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.VOLUME_DOWN;
                       break;
                   case 5:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.FORWARD;
                       break;
                   case 6:
                       remoteControlPassThroughOperationID = RemoteControlPassThroughOperationID.BACKWARD;
                       break;
                }

                try {
                    responseTimeout = Integer.valueOf(responseTimeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteControlPassThroughCommand = new RemoteControlPassThroughCommand(remoteControlPassThroughOperationID, RemoteControlPassThroughState.PRESSED);
                result = sinkManager.sendRemoteControlCommand(bluetoothAddress, responseTimeout, remoteControlPassThroughCommand);

                displayMessage("");

                StringBuilder sb = new StringBuilder();

                sb.append("RemoteControlPassThroughCommand() button depress result: ");
                sb.append(result);
                displayMessage(sb);

                if (result > 0){
                    remoteControlPassThroughCommand = new RemoteControlPassThroughCommand(remoteControlPassThroughOperationID, RemoteControlPassThroughState.RELEASED);
                    result = sinkManager.sendRemoteControlCommand(bluetoothAddress, responseTimeout, remoteControlPassThroughCommand);

                    sb = new StringBuilder();
                    sb.append("RemoteControlPassThroughCommand() button release result: ");
                    sb.append(result);
                    displayMessage(sb);
                }
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Pass Through Operation ID", remoteControlPassThroughOperationIDs);
            getCommandParameterView(1).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
        }

         @Override
        public void unselected() {
        // TODO Auto-generated method stub

        }

    };
    private final CommandHandler configureAudioTrack_Handler = new CommandHandler()
    {

        String[] streamTypes =
        {

            "Voice Call Stream",
            "System Stream",
            "Ring Stream",
            "Music Stream",
            "Alarm Stream",
            "DTMF Tone Stream",
            "Notification Stream",
            "Default Stream"

        };

        String[] bufferModes =
        {

            "Stream Mode",
            "Static Mode",

        };

        @Override
        public void run()
        {

            displayMessage("");

            StringBuilder sb = new StringBuilder();

            sb.append("configureAudioTrack():");
            displayMessage(sb);

            SpinnerValue streamTypeParameter;

            int bufferMode;
            SpinnerValue bufferModeParameter;

            streamTypeParameter = getCommandParameterView(0).getValueSpinner();
            bufferModeParameter = getCommandParameterView(1).getValueSpinner();

            switch(streamTypeParameter.selectedItem)
            {

                case 0:

                    audioTrackStreamType = AudioManager.STREAM_VOICE_CALL;
                    break;

                case 1:

                    audioTrackStreamType = AudioManager.STREAM_SYSTEM;
                    break;

                case 2:

                    audioTrackStreamType = AudioManager.STREAM_RING;
                    break;

                case 3:

                    default:
                    audioTrackStreamType = AudioManager.STREAM_MUSIC;
                    break;

                case 4:

                    audioTrackStreamType = AudioManager.STREAM_ALARM;
                    break;

                case 5:

                    audioTrackStreamType = AudioManager.STREAM_DTMF;
                    break;

                case 6:

                    audioTrackStreamType = AudioManager.STREAM_NOTIFICATION;
                    break;

                case 7:

                    audioTrackStreamType = AudioManager.USE_DEFAULT_STREAM_TYPE;
                    break;

             }

            switch(bufferModeParameter.selectedItem)
            {

                case 0:

                    default:
                    audioTrackBufferMode = AudioTrack.MODE_STREAM;
                    break;

                case 1:

                    audioTrackBufferMode = AudioTrack.MODE_STATIC;
                    break;

            }

            audioTrackStreamTypeString = streamTypes[streamTypeParameter.selectedItem];
            audioTrackBufferModeString = bufferModes[bufferModeParameter.selectedItem];

            displayMessage("Stream Type: " + streamTypes[streamTypeParameter.selectedItem]);
            displayMessage("Buffer Mode: " + bufferModes[bufferModeParameter.selectedItem]);


        }

        @Override
        public void selected()
        {
            getCommandParameterView(0).setModeSpinner("Stream Types", streamTypes);
            getCommandParameterView(1).setModeSpinner("Buffer Mode", bufferModes);
        }

         @Override
        public void unselected()
        {
        // TODO Auto-generated method stub

        }

    };

    private final Command[] commandList = new Command[]
    {

        new Command("Connect Request Response", connectRequestResponse_Handler),
        new Command("Connect Audio Stream", connectAudioStream_Handler),
        new Command("Disconnect Audio Stream", disconnectAudioStream_Handler),
        new Command("Query Audio Stream Connected", queryAudioStreamConnected_Handler),
        new Command("Query Audio Stream Connected device", queryAudioStreamConnectedDevice_Handler),
        new Command("Query Audio Stream State", queryAudioStreamState_Handler),
        new Command("Query Audio Stream Format", queryAudioStreamFormat_Handler),
        new Command("Query Audio Stream Configuration", queryAudioStreamConfiguration_Handler),
        new Command("Change Audio Stream State", changeAudioStreamState_Handler),
        new Command("Change Audio Stream Format", changeAudioStreamFormat_Handler),
        new Command("Change Incoming Connection Flags", changeIncomingFlags_Handler),
        new Command("Send Remote Control Command", sendRemoteControlCommand_Handler),
        new Command("Configure Audio Track", configureAudioTrack_Handler),

    };

}
