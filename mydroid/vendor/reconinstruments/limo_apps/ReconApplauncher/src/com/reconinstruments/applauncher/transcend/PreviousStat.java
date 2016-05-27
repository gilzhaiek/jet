//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.util.Log;

public class PreviousStat<T extends Number> extends WindowStat {
    public PreviousStat(String s, Stat<T> followed) {
        super(s,followed,2);
    }
    public PreviousStat(T value, String s, T invalidvalue, Stat<T> followed) {
        super(value,s,invalidvalue,followed,2);
    }
    public PreviousStat(T value, String s, Stat<T> followed) {
        super(value,s,followed,2);
    }
    @Override
    public void update() {
        T newestValue = (T)mFollowed.getValue();
        mCircularArray.push(newestValue);
        setValue(readPrevious(1));
    }
    public T getDelta() {
        Number val2 = mCircularArray.readPrevious(0);
        Number val1 = mCircularArray.readPrevious(1);
        if (mResetValue instanceof Integer) {
            if (!isValid(val2) || !isValid(val1)) {
                return (T)(new Integer(0));
            }
            return (T)(new Integer(val2.intValue() - val1.intValue()));
        } else if (mResetValue instanceof Float) {
            if (!isValid(val2) || !isValid(val1)) {
                return (T)(new Float(0));
            }
            return (T)(new Float(val2.floatValue() - val1.floatValue()));
        } else if (mResetValue instanceof Long) {
            if (!isValid(val2) || !isValid(val1)) {
                return (T)(new Long(0));
            }
            return (T)(new Long(val2.longValue() - val1.longValue()));
        }
        return (T)(new Short((short)0));
    }
}