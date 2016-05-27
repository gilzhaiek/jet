/*
 * SBC.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;


/**
 * Java wrapper for the SBC codec API for the Stonestreet One Bluetooth Protocol
 * Stack.
 */
public abstract class SBC {

    protected boolean disposed;

    static {
        System.loadLibrary("btpmj");
    }

    protected SBC() {
        disposed = false;
    }

    protected void dispose() {
        disposed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if(!disposed) {
                System.err.println("Error: Possible memory leak: Manager object of type '" + this.getClass().getName() + "' not 'dispose()'ed correctly.");
                dispose();
            }
        } finally {
            super.finalize();
        }
    }

    //TODO Non-public until the API is finalized.
    public static class Encoder extends SBC {

        private final long encoderHandle;
        private EncoderConfiguration currentConfig;
        private int encodedBufferSize;

        public Encoder(EncoderConfiguration configuration) throws Exception {
            super();

            if(configuration == null)
                throw new IllegalArgumentException("configuration must not be null");

            int channelMode      = 0;
            int allocationMethod = 0;

            switch(configuration.channelMode) {
            case MONO:
                channelMode = CHANNEL_MODE_MONO;
                break;
            case DUAL_CHANNEL:
                channelMode = CHANNEL_MODE_DUAL_CHANNEL;
                break;
            case STEREO:
                channelMode = CHANNEL_MODE_STEREO;
                break;
            case JOINT_STEREO:
                channelMode = CHANNEL_MODE_JOINT_STEREO;
                break;
            }

            switch(configuration.allocationMethod) {
            case LOUDNESS:
                allocationMethod = ALLOCATION_METHOD_LOUNDNESS;
                break;
            case SNR:
                allocationMethod = ALLOCATION_METHOD_SNR;
                break;
            }

            encoderHandle = initializeEncoderNative(configuration.samplingFrequency, configuration.blockSize, channelMode, allocationMethod, configuration.subbands, configuration.maximumBitRate);

            if(encoderHandle > 0) {
                currentConfig = configuration;
                encodedBufferSize = calculateEncoderFrameLengthNative(configuration.samplingFrequency, configuration.blockSize, channelMode, allocationMethod, configuration.subbands, configuration.maximumBitRate);
            }
            else
                throw new Exception("Unable to inialize SBC encoder.");
        }

        @Override
        public void dispose() {
            cleanupEncoderNative(encoderHandle);

            super.dispose();
        }

        /**
         * Calculates the required buffer size for encoded data under the
         * current encoder configuration.
         *
         * @return The required buffer size (in bytes) or a negative error code.
         */
        public int getEncodedFrameSize() {
            return encodedBufferSize;
        }

        /**
         * Calculates the amount of raw PCM data that will be consumed by
         * each encoded SBC Frame.
         * <br>
         * Note: This function returns the number of PCM samples consumed
         * NOT the size in bytes.
         *<br>
         * @return The number of PCM Samples in each SBC Frame.
         */
        public int getInputFrameSamples() {
            return currentConfig.blockSize * currentConfig.subbands;
        }

        /**
         * Calculates the amount of raw PCM data that will be consumed by
         * each encoded SBC Frame.
         * <br>
         * @return The size, in bytes, consumed by each SBC Frame.
         */
        public int getInputFrameSize() {
            return getInputFrameSamples() * 2 * ((currentConfig.channelMode == ChannelMode.MONO)?1:2);
        }

        public EncoderConfiguration getConfiguration() {
            return currentConfig;
        }

        public int setConfiguration(EncoderConfiguration configuration) {
            int result;

            if(configuration == null)
                throw new IllegalArgumentException("configuration must not be null");

            int channelMode      = 0;
            int allocationMethod = 0;

            switch(configuration.channelMode) {
            case MONO:
                channelMode = CHANNEL_MODE_MONO;
                break;
            case DUAL_CHANNEL:
                channelMode = CHANNEL_MODE_DUAL_CHANNEL;
                break;
            case STEREO:
                channelMode = CHANNEL_MODE_STEREO;
                break;
            case JOINT_STEREO:
                channelMode = CHANNEL_MODE_JOINT_STEREO;
                break;
            }

            switch(configuration.allocationMethod) {
            case LOUDNESS:
                allocationMethod = ALLOCATION_METHOD_LOUNDNESS;
                break;
            case SNR:
                allocationMethod = ALLOCATION_METHOD_SNR;
                break;
            }

            result = changeEncoderConfigurationNative(encoderHandle, configuration.samplingFrequency, configuration.blockSize, channelMode, allocationMethod, configuration.subbands, configuration.maximumBitRate);

            if(result > 0) {
                currentConfig = configuration;
                encodedBufferSize = result;
            }

            return result;
        }

        /**
         * This function encodes raw PCM data to SBC encoded frames.
         * <br>
         * Note, this function consumes all of the data provided, even if
         * a complete frame is not created. The next call to this function
         * will continue filling the previously started frame. For this
         * reason, {@code encodedData} MUST be large enough to at least
         * hold one more frame then the number expected from the size
         * of the PCM input.
         * <br>
         * @param rawData PCM data to encode.
         * @param encodedData Buffer to store completed SBC Frames.
         *
         * @return The number of SBC frames that were successfully encoded and included in the
         *              buffer if successful, and a negative return error code if there was an error.
         */
        public int encodeData(byte[] rawData, byte[] encodedData) {
            int sampleLength = 2*((currentConfig.channelMode == ChannelMode.MONO)?1:2);

            //Check to make sure our buffer is big enough
            if(((rawData.length % getInputFrameSize() == 0) && (encodedData.length < rawData.length / getInputFrameSize() * getEncodedFrameSize())) ||
                    ((rawData.length % getInputFrameSize() != 0) && (encodedData.length <= rawData.length / getInputFrameSize() * getEncodedFrameSize())))
                throw new IllegalArgumentException("Encoded Data Buffer not large enough");

            //Make sure the data contains no partial samples
            if(rawData.length % sampleLength != 0)
                throw new IllegalArgumentException("Data contains incomplete PCM samples");


            return encodeDataNative(encoderHandle, rawData, encodedData, sampleLength);
        }

        /* The following structure type represents the Encoder Configuration */
        /* information required by the Subband Codec when encoding data.     */
        public static class EncoderConfiguration {
            public final int              samplingFrequency;
            public final int              blockSize;
            public final ChannelMode      channelMode;
            public final AllocationMethod allocationMethod;
            public final int              subbands;
            public final int              maximumBitRate;

            public EncoderConfiguration(int samplingFrequency, int blockSize, ChannelMode channelMode, AllocationMethod allocationMethod, int subbands, int maxBitRate) {
                if((samplingFrequency != 16000) && (samplingFrequency != 32000) && (samplingFrequency != 44100) && (samplingFrequency != 48000))
                    throw new IllegalArgumentException("Sampling Frequency must be one of 16000, 32000, 44100, or 48000 Hz");
                if((blockSize != 4) && (blockSize != 8) && (blockSize != 12) && (blockSize != 16))
                    throw new IllegalArgumentException("Block Size must be one of 4, 8, 12, or 16 frames");
                if((subbands != 4) && (subbands != 8))
                    throw new IllegalArgumentException("Subbands must be one of 4 or 8");

                this.samplingFrequency = samplingFrequency;
                this.blockSize         = blockSize;
                this.channelMode       = channelMode;
                this.allocationMethod  = allocationMethod;
                this.subbands          = subbands;
                this.maximumBitRate    = maxBitRate;
            }
        }
    }

    /**
     * SBC frame decoder.
     */
    public static class Decoder extends SBC {

        private final long decoderHandle;

        /**
         * Creates a new SBC frame decoder.
         *
         * @throws OutOfMemoryError
         *             if the decoder initialization fails due to a lack of
         *             resources.
         */
        public Decoder() throws OutOfMemoryError {
            super();

            decoderHandle = initializeDecoderNative();

            if(decoderHandle == 0)
                throw new OutOfMemoryError("Unable to inialize SBC decoder.");
        }

        @Override
        public void dispose() {
            cleanupDecoderNative(decoderHandle);

            super.dispose();
        }

        /**
         * Decodes a blob of SBC frame data.
         * <p>
         * All the provided data will be consumed by the decoder and may result
         * in zero or more decoded audio frames.
         *
         * @param sbcFrameData
         * @return The number of fully decoded audio frames. If an error occurs,
         *         returns one of {@link SBC#SBC_ERROR_INVALID_PARAMETER SBC_ERROR_INVALID_PARAMETER},
         *         {@link SBC#SBC_ERROR_INSUFFICIENT_RESOURCES SBC_ERROR_INSUFFICIENT_RESOURCES},
         *         {@link SBC#SBC_ERROR_NOT_INITIALIZED SBC_ERROR_NOT_INITIALIZED}, or
         *         {@link SBC#SBC_ERROR_UNKNOWN_ERROR SBC_ERROR_UNKNOWN_ERROR}.
         */
        public int decodeSBCFrames(byte[] sbcFrameData) {
            return decodeDataNative(decoderHandle, sbcFrameData);
        }

        /**
         * Gets the number of available decoded audio frames. Retrieve the
         * actual frame data with {@link #getNextAudioFrame}.
         *
         * @return The number of available decoded audio frames.
         */
        public int availableAudioFrames() {
            return availableAudioFramesNative(decoderHandle);
        }

        /**
         * Retrieves the next decoded audio frame, if one is available.
         *
         * @param audioFrameData
         *            Buffer which will be populated with the audio frame. Must
         *            be large enough to hold the entire frame.
         * @param config
         *            Optional. If provided, the encoder configuration used to
         *            encode this audio frame is stored in this object.
         * @return The size of the decoded audio frame or zero if no audio frame
         *         is available. If {@code audioFrameData} is not large enough,
         *         returns (-1 * [frame size]).
         */
        public int getNextAudioFrame(byte[] audioFrameData, DecoderConfiguration config) {
            int result;
            int[] configValues;

            if(config != null) {
                configValues = new int[8];
                result = getAudioFrameNative(decoderHandle, audioFrameData, configValues);

                if(result > 0) {
                    config.set(configValues[0], configValues[1], configValues[2], configValues[3], configValues[4], configValues[5], configValues[6], configValues[7]);
                }
            } else {
                result = getAudioFrameNative(decoderHandle, audioFrameData, null);
            }

            return result;
        }

        /**
         * SBC configuration information parsed from an SBC frame and used in
         * the decoding process. Used with the {@link Decoder#getNextAudioFrame}
         * method.
         */
        public static class DecoderConfiguration {
            /**
             * Audio sample frequency, specified in Hz. One of {@code 16000},
             * {@code 32000}, {@code 44100}, or {@code 48000}.
             */
            public int               samplingFrequency;

            /**
             * Encoder block size. One of {@code 4}, {@code 8}, {@code 12}, or
             * {@code 16}.
             */
            public int               blockSize;

            /**
             * Channel mode of the audio stream.
             */
            public ChannelMode       channelMode;

            /**
             * Bit allocation method used by the encoder.
             */
            public AllocationMethod  allocationMethod;

            /**
             * Number of subbands used by the encoder. Either {@code 4} or {@code 8}.
             */
            public int               subbands;

            /**
             * Bits of compressed data per second of audio.
             */
            public int               bitRate;

            /**
             * Bit Pool value used by the encoder.
             */
            public int               bitPool;

            /**
             * Length of each encoded SBC frame in bytes.
             */
            public int               frameLength;

            /**
             * Construct an empty {@code DecoderConfiguration} object.
             */
            public DecoderConfiguration() { }

            /*package*/ DecoderConfiguration(int freq, int bSize, ChannelMode cMode, AllocationMethod aMethod, int bands, int rate, int pool, int length) {
                samplingFrequency = freq;
                blockSize = bSize;
                channelMode = cMode;
                allocationMethod = aMethod;
                subbands = bands;
                bitRate = rate;
                bitPool = pool;
                frameLength = length;
            }

            /*package*/ DecoderConfiguration(int freq, int bSize, int cMode, int aMethod, int bands, int rate, int pool, int length) {
                set(freq, bSize, cMode, aMethod, bands, rate, pool, length);
            }

            /*package*/ void set(int freq, int bSize, int cMode, int aMethod, int bands, int rate, int pool, int length) {
                samplingFrequency = freq;
                blockSize = bSize;

                switch(cMode) {
                case CHANNEL_MODE_MONO:
                    channelMode = ChannelMode.MONO;
                    break;
                case CHANNEL_MODE_DUAL_CHANNEL:
                    channelMode = ChannelMode.DUAL_CHANNEL;
                    break;
                case CHANNEL_MODE_STEREO:
                    channelMode = ChannelMode.STEREO;
                    break;
                case CHANNEL_MODE_JOINT_STEREO:
                    channelMode = ChannelMode.JOINT_STEREO;
                    break;
                default:
                    throw new IllegalArgumentException();
                }

                switch(aMethod) {
                case ALLOCATION_METHOD_LOUNDNESS:
                    allocationMethod = AllocationMethod.LOUDNESS;
                    break;
                case ALLOCATION_METHOD_SNR:
                    allocationMethod = AllocationMethod.SNR;
                    break;
                default:
                    throw new IllegalArgumentException();
                }

                subbands = bands;
                bitRate = rate;
                bitPool = pool;
                frameLength = length;
            }
        }
    }

        /* Success Return Codes.                                             */
    private static final int SBC_PROCESSING_COMPLETE = 0;
    private static final int SBC_PROCESSING_DATA     = 1;

        /* Error Return Codes.                                               */
    /**
     * Error code indicating that a method parameter was not valid.
     */
    public static final int SBC_ERROR_INVALID_PARAMETER      = -1;
    /**
     * Error code indicating that insufficient system resources were available
     * to perform the requested operation.
     */
    public static final int SBC_ERROR_INSUFFICIENT_RESOURCES = -2;
    /**
     * Error code indicating that the SBC codec was not properly initialized.
     */
    public static final int SBC_ERROR_NOT_INITIALIZED        = -3;
    /**
     * Error code indicating that an unexpected critical failure occurred.
     */
    public static final int SBC_ERROR_UNKNOWN_ERROR          = -4;

    private static final int CHANNEL_MODE_MONO         = 0;
    private static final int CHANNEL_MODE_DUAL_CHANNEL = 1;
    private static final int CHANNEL_MODE_STEREO       = 2;
    private static final int CHANNEL_MODE_JOINT_STEREO = 3;

    /**
     * The available Channel Modes supported by the SBC codec.
     */
    public enum ChannelMode {
        /**
         * Monaural audio. One channel.
         */
        MONO,

        /**
         * Dual channel audio. Two channels.
         */
        DUAL_CHANNEL,

        /**
         * Stereo audio. Two channels.
         */
        STEREO,

        /**
         * Joint-stereo audio. Two channels.
         */
        JOINT_STEREO
    }

    private static final int ALLOCATION_METHOD_LOUNDNESS = 0;
    private static final int ALLOCATION_METHOD_SNR       = 1;

    /**
     * The bit allocation methods supported by the SBC codec.
     */
    public enum AllocationMethod {
        /**
         * Loudness-based bit allocation.
         */
        LOUDNESS,

        /**
         * Signal/Noise Ratio bit allocation.
         */
        SNR
    }


    /* Native method declarations */

    /*package*/ native int  calculateEncoderBitPoolSizeNative(int samplingFrequency, int blockSize, int channelMode, int allocationMethod, int subbands, int maximumBitRate);
    /*package*/ native int  calculateEncoderFrameLengthNative(int samplingFrequency, int blockSize, int channelMode, int allocationMethod, int subbands, int maximumBitRate);
    /*package*/ native int  calculateEncoderBitRateNative(int samplingFrequency, int blockSize, int channelMode, int allocationMethod, int subbands, int maximumBitRate);
    /*package*/ native int  calculateDecoderFrameSizeNative(byte[] bitStreamData);

    /*package*/ native long initializeEncoderNative(int samplingFrequency, int blockSize, int channelMode, int allocationMethod, int subbands, int maximumBitRate);
    /*package*/ native void cleanupEncoderNative(long encoderHandle);
    /*package*/ native int  changeEncoderConfigurationNative(long encoderHandle, int samplingFrequency, int blockSize, int channelMode, int allocationMethod, int subbands, int maximumBitRate);
    /*package*/ native int  encodeDataNative(long encoderHandle, byte[] data, byte[] encodedData, int sampleLength);

    /*package*/ native long initializeDecoderNative();
    /*package*/ native void cleanupDecoderNative(long decoderHandle);
    /*package*/ native int  decodeDataNative(long decoderHandle, byte[] data);
    /*package*/ native int  availableAudioFramesNative(long decoderHandle);
    /*package*/ native int  getAudioFrameNative(long decoderHandle, byte[] data, int[] config);

}
