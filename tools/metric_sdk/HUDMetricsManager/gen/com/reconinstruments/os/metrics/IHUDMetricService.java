/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gilzhaiek/Dev/jet/tools/metric_sdk/HUDMetricsManager/src/com/reconinstruments/os/metrics/IHUDMetricService.aidl
 */
package com.reconinstruments.os.metrics;
public interface IHUDMetricService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.reconinstruments.os.metrics.IHUDMetricService
{
private static final java.lang.String DESCRIPTOR = "com.reconinstruments.os.metrics.IHUDMetricService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.reconinstruments.os.metrics.IHUDMetricService interface,
 * generating a proxy if needed.
 */
public static com.reconinstruments.os.metrics.IHUDMetricService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.reconinstruments.os.metrics.IHUDMetricService))) {
return ((com.reconinstruments.os.metrics.IHUDMetricService)iin);
}
return new com.reconinstruments.os.metrics.IHUDMetricService.Stub.Proxy(obj);
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
case TRANSACTION_getMetricValue:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
com.reconinstruments.os.metrics.BaseValue _result = this.getMetricValue(_arg0, _arg1);
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
case TRANSACTION_registerMetricListener:
{
data.enforceInterface(DESCRIPTOR);
com.reconinstruments.os.metrics.IHUDMetricListener _arg0;
_arg0 = com.reconinstruments.os.metrics.IHUDMetricListener.Stub.asInterface(data.readStrongBinder());
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
int _arg3;
_arg3 = data.readInt();
this.registerMetricListener(_arg0, _arg1, _arg2, _arg3);
return true;
}
case TRANSACTION_unregisterMetricListener:
{
data.enforceInterface(DESCRIPTOR);
com.reconinstruments.os.metrics.IHUDMetricListener _arg0;
_arg0 = com.reconinstruments.os.metrics.IHUDMetricListener.Stub.asInterface(data.readStrongBinder());
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
int _arg3;
_arg3 = data.readInt();
this.unregisterMetricListener(_arg0, _arg1, _arg2, _arg3);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.reconinstruments.os.metrics.IHUDMetricService
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
@Override public com.reconinstruments.os.metrics.BaseValue getMetricValue(int groupID, int metricID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.reconinstruments.os.metrics.BaseValue _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(groupID);
_data.writeInt(metricID);
mRemote.transact(Stub.TRANSACTION_getMetricValue, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.reconinstruments.os.metrics.BaseValue.CREATOR.createFromParcel(_reply);
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
@Override public void registerMetricListener(com.reconinstruments.os.metrics.IHUDMetricListener listener, int listenerID, int groupID, int metricID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
_data.writeInt(listenerID);
_data.writeInt(groupID);
_data.writeInt(metricID);
mRemote.transact(Stub.TRANSACTION_registerMetricListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void unregisterMetricListener(com.reconinstruments.os.metrics.IHUDMetricListener listener, int listenerID, int groupID, int metricID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
_data.writeInt(listenerID);
_data.writeInt(groupID);
_data.writeInt(metricID);
mRemote.transact(Stub.TRANSACTION_unregisterMetricListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_getMetricValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_registerMetricListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_unregisterMetricListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public com.reconinstruments.os.metrics.BaseValue getMetricValue(int groupID, int metricID) throws android.os.RemoteException;
public void registerMetricListener(com.reconinstruments.os.metrics.IHUDMetricListener listener, int listenerID, int groupID, int metricID) throws android.os.RemoteException;
public void unregisterMetricListener(com.reconinstruments.os.metrics.IHUDMetricListener listener, int listenerID, int groupID, int metricID) throws android.os.RemoteException;
}
