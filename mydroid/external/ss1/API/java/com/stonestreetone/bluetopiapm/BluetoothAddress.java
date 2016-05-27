/*
 * BluetoothAddress.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */

package com.stonestreetone.bluetopiapm;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The address of a Bluetooth device.
 */
public class BluetoothAddress {
    private final byte[] octets;
    private Integer      hash;

    private static final Pattern pattern = Pattern.compile("^(\\p{XDigit}{2}?)(?::|-)?(\\p{XDigit}{2}?)(?::|-)?(\\p{XDigit}{2}?)(?::|-)?(\\p{XDigit}{2}?)(?::|-)?(\\p{XDigit}{2}?)(?::|-)?(\\p{XDigit}{2}?)$");
    private static final String badFormException = "Bluetooth addresses must be of the form \"00:11:22:33:44:55\", \"00-11-22-33-44-55\", or \"001122334455\"";

    /**
     * Initializes this {@code BluetoothAddress} from an address string.
     * <p>
     * The string must be of one of the following forms:
     * <ul>
     * <li>{@code "00:11:22:33:44:55"}</li>
     * <li>{@code "00-11-22-33-44-55"}</li>
     * <li>{@code "001122334455"}</li>
     * </ul>
     *
     * @param address
     *            String form of the Bluetooth device address.
     * @throws IllegalArgumentException
     *            if the specified address is not in one of the prescribed formats.
     */
    public BluetoothAddress(String address) throws IllegalArgumentException {
        Matcher matcher;
        octets = new byte[6];
        hash = null;

        matcher = pattern.matcher(address);
        if(matcher.matches()) {
            for(int i = 0; i < 6; i++) {
                try {
                    octets[i] = (byte)(Short.parseShort(matcher.group(i + 1), 16));

                    //if((octets[0] & 0x01) == 0x01)
                    //    throw new IllegalArgumentException("Bluetooth device addresses may not be specified as multicast addresses.");
                } catch(IllegalStateException e) {
                    throw new IllegalArgumentException(badFormException);
                }
            }
        } else {
            throw new IllegalArgumentException(badFormException);
        }
    }

    /**
     * Initializes this {@code BluetoothAddress} from a six-byte array.
     * <p>
     * The address octets should be in normal MAC reading order. That is, the following invocations are equivalent:
     * <ul>
     * <li>{@code BluetoothAddress("00:11:22:33:44:55")}</li>
     * <li><code>BluetoothAddress(new byte[] {00, 11, 22, 33, 44, 55})</code></li>
     * </ul>
     *
     * @param address Array of six octets forming the Bluetooth device address.
     * @throws IllegalArgumentException
     *            if the specified address is invalid (wrong length, illegal octets, ...).
     */
    public BluetoothAddress(byte[] address) throws IllegalArgumentException {
        if(address.length != 6)
            throw new IllegalArgumentException("Bluetooth device addresses must be exactly six octets long.");
        //if((address[0] & 0x01) == 0x01)
        //    throw new IllegalArgumentException("Bluetooth device addresses may not be specified as multicast addresses.");

        octets = address.clone();
        hash = null;
    }

    /*pkg*/BluetoothAddress(byte octet1, byte octet2, byte octet3, byte octet4, byte octet5, byte octet6) {
        octets = new byte[] {octet1, octet2, octet3, octet4, octet5, octet6};
        hash = null;
    }

    /**
     * Converts this {@code BluetoothAddress} to a six-octet {@code byte} array.
     *
     * @return A new byte array representing this Bluetooth device address.
     */
    public byte[] toByteArray() {
        return octets.clone();
    }

    /*pkg*/ byte[] internalByteArray() {
        return octets;
    }

    /**
     * Converts this {@code BluetoothAddress} to a string representation.
     *
     * @return A printable representation of this Bluetooth device address.
     */
    @Override
    public String toString() {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X", octets[0], octets[1], octets[2], octets[3], octets[4], octets[5]);
    }

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;

        if((o != null) && (o instanceof BluetoothAddress))
            return Arrays.equals(octets, ((BluetoothAddress)o).octets);
        else
            return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        if(hash == null)
            hash = Arrays.hashCode(octets);

        return hash;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        BluetoothAddress address = new BluetoothAddress(octets);
        address.hash = hash;

        return address;
    }
}
