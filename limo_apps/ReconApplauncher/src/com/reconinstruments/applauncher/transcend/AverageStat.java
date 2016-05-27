//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
public class AverageStat<T extends Number> extends DependentStat {
    protected int N = 0;
    public AverageStat(String s, Stat<T> followed) {
        super(s,followed);
    }
    public AverageStat(T value, String s, Stat<T> followed) {
        super(value,s,followed);
    }
    @Override
    public void update() {
        if (!mFollowed.isValid()) return;
        float newVal = mFollowed.getValue().floatValue();
        float oldVal = getValue().floatValue();
        setValue((newVal + oldVal * (float)N)/(N + 1f));
        N++;
    }
    public void reset() {
        super.reset();
        N = 0;
    }
    @Override
    public void saveState(SharedPreferences.Editor editor) {
        super.saveState(editor);
        editor.putInt(mTag+"_N",N);
    }
    @Override
    public void loadState(SharedPreferences sp) {
        super.loadState(sp);
        N = sp.getInt(mTag+"_N",N);
    }
    public Bundle putToBundle(Bundle b) {
        b = super.putToBundle(b);
        b.putInt(mTag+"_N",N);
        return b;
    }
}