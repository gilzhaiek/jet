//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
abstract public class RecordStat<T extends Number> extends DependentStat {
    public RecordStat(String tag,Stat<T> follow) {
        super(tag,follow);
    }
    public RecordStat(T value, String tag, Stat<T> follow) {
        super(value,tag,follow);
    }
    public void update() {
        if (!mFollowed.isValid()) {
            mWasRecord = false;
            return;
        } else if (!isValid()) {
            setValue(mFollowed.getValue());
        }
        if (isRecord()) {
            setValue(mFollowed.getValue());
            mWasRecord = true;
        }
        else {
            mWasRecord = false;
        }
    }
    abstract boolean isRecord();
    private boolean mWasRecord;
    public boolean wasRecord() {return mWasRecord;};
}