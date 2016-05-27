//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
public class MaxStat<T extends Number> extends RecordStat {
    public MaxStat(String tag, Stat<T> follow) {
        super(tag,follow);
    }
    public MaxStat(T value, String tag, Stat<T> follow) {
        super(value,tag,follow);
    }
    boolean isRecord() {
        return  (mFollowed.getValue().doubleValue() >
                 mValue.doubleValue());
    }

}