/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gilzhaiek/Dev/jet/sdk/connectivity/HUDConnectivityLib/src/com/reconinstruments/hudconnectivitylib/IHUDConnectivityService.aidl
 */
package com.reconinstruments.hudconnectivitylib;
public interface IHUDConnectivityService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.reconinstruments.hudconnectivitylib.IHUDConnectivityService
{
private static final java.lang.String DESCRIPTOR = "com.reconinstruments.hudconnectivitylib.IHUDConnectivityService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.reconinstruments.hudconnectivitylib.IHUDConnectivityService interface,
 * generating a proxy if needed.
 */
public static com.reconinstruments.hudconnectivitylib.IHUDConnectivityService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.reconinstruments.hudconnectivitylib.IHUDConnectivityService))) {
return ((com.reconinstruments.hudconnectivitylib.IHUDConnectivityService)iin);
}
return new com.reconinstruments.hudconnectivitylib.IHUDConnectivityService.Stub.Proxy(obj);
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
case TRANSACTION_DEBUG_ONLY_start:
{
data.enforceInterface(DESCRIPTOR);
this.DEBUG_ONLY_start();
reply.writeNoException();
return true;
}
case TRANSACTION_DEBUG_ONLY_stop:
{
data.enforceInterface(DESCRIPTOR);
this.DEBUG_ONLY_stop();
reply.writeNoException();
return true;
}
case TRANSACTION_register:
{
data.enforceInterface(DESCRIPTOR);
com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener _arg0;
_arg0 = com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener.Stub.asInterface(data.readStrongBinder());
this.register(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregister:
{
data.enforceInterface(DESCRIPTOR);
com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener _arg0;
_arg0 = com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener.Stub.asInterface(data.readStrongBinder());
this.unregister(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_connect:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.connect(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isPhoneConnected:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPhoneConnected();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_hasWebConnection:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.hasWebConnection();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_sendWebRequest:
{
data.enforceInterface(DESCRIPTOR);
com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest _arg0;
if ((0!=data.readInt())) {
_arg0 = com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse _result = this.sendWebRequest(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.reconinstruments.hudconnectivitylib.IHUDConnectivityService
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
@Override public void DEBUG_ONLY_start() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_DEBUG_ONLY_start, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void DEBUG_ONLY_stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_DEBUG_ONLY_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void register(com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_register, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unregister(com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregister, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void connect(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_connect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isPhoneConnected() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPhoneConnected, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean hasWebConnection() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_hasWebConnection, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse sendWebRequest(com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest request) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((request!=null)) {
_data.writeInt(1);
request.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendWebRequest, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_DEBUG_ONLY_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_DEBUG_ONLY_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_register = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_unregister = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_connect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_isPhoneConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_hasWebConnection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_sendWebRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
public void DEBUG_ONLY_start() throws android.os.RemoteException;
public void DEBUG_ONLY_stop() throws android.os.RemoteException;
public void register(com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener listener) throws android.os.RemoteException;
public void unregister(com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener listener) throws android.os.RemoteException;
public void connect(java.lang.String address) throws android.os.RemoteException;
public boolean isPhoneConnected() throws android.os.RemoteException;
public boolean hasWebConnection() throws android.os.RemoteException;
public com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse sendWebRequest(com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest request) throws android.os.RemoteException;
}
