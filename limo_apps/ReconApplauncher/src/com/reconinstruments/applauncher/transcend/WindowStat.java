//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.util.Log;
public class WindowStat<T extends Number> extends DependentStat {
    protected CircularArray mCircularArray;
    public WindowStat(String s, Stat<T> followed, int size) {
        super(s,followed);
        mCircularArray = new CircularArray(size,mInvalidValue);
    }
    public WindowStat(T value, String s, T invalidvalue, Stat<T> followed,int size) {
        super(value,s,invalidvalue,followed);
        mCircularArray = new CircularArray(size,mInvalidValue);
    }
    public WindowStat(T value, String s, Stat<T> followed,int size) {
        super(value,s,followed);
        mCircularArray = new CircularArray(size,mInvalidValue);
    }
    @Override
    public void update() {
        T newestValue = (T)mFollowed.getValue();
        mCircularArray.push(newestValue);
        setValue(newestValue);
    }
    @Override
    public void reset(){
        super.reset();
        int size = mCircularArray.getSize();
        mCircularArray = new CircularArray(size,mResetValue);
    }

    public T readPrevious(int i) {
        return (T)mCircularArray.readPrevious(i);
    }
    
}