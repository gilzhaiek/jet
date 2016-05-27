//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
public class LowPassStat<T extends Number> extends DependentStat {
    protected float mCoeff;
    public LowPassStat(String s, Stat<T> followed,float coeff) {
        super(s,followed);
        setCoeff(coeff);
    }
    public LowPassStat(T value, String s, T invalidvalue, Stat<T> followed,
                       float coeff) {
        super(value,s,invalidvalue,followed);
        setCoeff(coeff);
    }
    public LowPassStat(T value, String s, Stat<T> followed,float coeff) {
        super(value,s,followed);
        setCoeff(coeff);
    }
    public void setCoeff(float coeff) {
        mCoeff = coeff;
    }
    @Override
    public void update() {
        T newestValue = (T)mFollowed.getValue();
        if (!isValid(newestValue)) {
            setValue(newestValue);
        }
        else {
            setValue(mValue.floatValue()*(1-mCoeff) +
                     newestValue.floatValue() * mCoeff);
        }
    }
}