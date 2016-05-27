/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gilzhaiek/Dev/jet/sdk/connectivity/HUDConnectivityLib/src/com/reconinstruments/hudconnectivitylib/IHUDConnectivityListener.aidl
 */
package com.reconinstruments.hudconnectivitylib;
public interface IHUDConnectivityListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener
{
private static final java.lang.String DESCRIPTOR = "com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener interface,
 * generating a proxy if needed.
 */
public static com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener))) {
return ((com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener)iin);
}
return new com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onDeviceName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onDeviceName(_arg0);
return true;
}
case TRANSACTION_onConnectionStateChanged:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onConnectionStateChanged(_arg0);
return true;
}
case TRANSACTION_onNetworkEvent:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.onNetworkEvent(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void onDeviceName(java.lang.String deviceName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(deviceName);
mRemote.transact(Stub.TRANSACTION_onDeviceName, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onConnectionStateChanged(int connectionState) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(connectionState);
mRemote.transact(Stub.TRANSACTION_onConnectionStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onNetworkEvent(int networkEvent, boolean hasNetworkAccess) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(networkEvent);
_data.writeInt(((hasNetworkAccess)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_onNetworkEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onDeviceName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onConnectionStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onNetworkEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void onDeviceName(java.lang.String deviceName) throws android.os.RemoteException;
public void onConnectionStateChanged(int connectionState) throws android.os.RemoteException;
public void onNetworkEvent(int networkEvent, boolean hasNetworkAccess) throws android.os.RemoteException;
}
