package com.stonestreetone.bluetopiapm.sample.util;

/**
 * Container class for utility functions for Stonestreet One
 * BluetopiaPM API sample application.
 */
public class Utils {
    /**
     * Takes a list of Enum values (from {EnumName}.values() ) and creates a list of all
     * the enum values' names.
     *
     * @param enums The list of enum values.
     * @return The list of corresponding names.
     */
    public static String[] getEnumStringSet(Enum<?>[] enums) {
        String[] enumStrings = new String[enums.length];

        for(int i=0; i<enums.length; i++)
            enumStrings[i] = enums[i].toString();

        return enumStrings;
    }

    /**
     * Converts a string representation of a hexadecimal value into a byte array
     *
     * @param hexString The string representation of the hex value.
     * @return The byte array representation of the hex data.
     *
     * @throws IllegalArgumentException Thrown if the given string is not a valid hexadecimal value.
     */
    public static byte[] hexToByteArray(CharSequence hexString) {
        byte[] byteArray;

        if(hexString.length() >= 2 && hexString.charAt(0) == '0' && hexString.charAt(1) == 'x')
            hexString = hexString.subSequence(2, hexString.length()-1);

        int length = hexString.length();

        if(length >= 2 && length % 2 == 0) {
            byteArray = new byte[length/2];

            for(int i=0;i<length;i+=2) {
                int highNibble = Character.digit(hexString.charAt(i), 16);
                int lowNibble = Character.digit(hexString.charAt(i+1), 16);

                if(highNibble >= 0 && lowNibble >= 0)
                    byteArray[i/2] = (byte)((highNibble << 4) + lowNibble);
                else
                    throw new IllegalArgumentException("Invalid Hex Characters");
            }

            return byteArray;
        }
        else
            throw new IllegalArgumentException("Invalid Hex String Length");
    }

    /**
     *
     * Converts a byte array into a hexadecimal character array representation of that array.
     *
     * @param bytes - byte array
     *
     * @return - array of hexadecimal characters translated from the byte array parameter
     *
     */
    public static char[] bytesToCharArray(byte[] bytes) {

        char[] hexadecimal = {

               '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };

        char[] hexNibbles = new char[bytes.length * 2];

        int byteToInteger;

        for(int index = 0; index < bytes.length; index++) {

            byteToInteger               = bytes[index] & 0x000000FF;

            hexNibbles[(index * 2)]     = hexadecimal[byteToInteger / 16];
            hexNibbles[(index * 2) + 1] = hexadecimal[byteToInteger % 16];
        }

        return hexNibbles;
    }
}
