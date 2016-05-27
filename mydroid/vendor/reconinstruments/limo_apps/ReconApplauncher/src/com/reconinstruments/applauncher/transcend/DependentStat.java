//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
abstract public class DependentStat<T extends Number> extends Stat {
    public DependentStat() {
    }
    public DependentStat(String tag, Stat<T> followed) {
        this(followed.getValue(),tag,followed.getInvalidValue(),
             followed);
    }
    public DependentStat(T value,String tag,Stat<T> followed) {
        this(value,tag,followed.getInvalidValue(),followed);
    }
    public DependentStat(T value,String tag,T invalidValue,Stat<T> followed) {
        super(value,tag,invalidValue);
        setFollowed(followed);
    }
    protected Stat<T> mFollowed;
    public void reset() {
        setValue(mFollowed.getValue());
    }
    public void setFollowed(Stat<T> followed) {
        mFollowed = followed;
    }
}