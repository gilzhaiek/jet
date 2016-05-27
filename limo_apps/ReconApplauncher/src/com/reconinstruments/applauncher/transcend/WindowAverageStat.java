//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
public class WindowAverageStat<T extends Number> extends WindowStat {
    protected int validPoints = 0;
    protected float mSum;
    public WindowAverageStat(String s, Stat<T> followed, int size) {
        super(s,followed,size);
    }
    public WindowAverageStat(T value, String s, Stat<T> followed,int size) {
        super(value,s,followed,size);
    }
    @Override
    public void update() {
        T newestValue = (T)mFollowed.getValue();
        T oldestValue = (T)mCircularArray.readPrevious(-1);
        if (isValid(oldestValue)) {
            mSum -= oldestValue.floatValue();
            validPoints--;
        }
        if (isValid(newestValue)) {
            mSum += newestValue.floatValue();
            validPoints++;
        }
        mCircularArray.push(newestValue);
        if (validPoints > 0) {
            setValue(mSum / (float)validPoints);
        }
        else {
            setValue(mInvalidValue);
        }
    }
    @Override
    public void reset(){
        super.reset();
        validPoints = 0;
        mSum = 0;
    }
}