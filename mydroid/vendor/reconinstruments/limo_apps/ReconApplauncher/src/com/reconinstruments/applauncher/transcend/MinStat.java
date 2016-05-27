//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
public class MinStat<T extends Number> extends RecordStat {
    public MinStat(String tag, Stat<T> follow) {
        super(tag,follow);
    }
    public MinStat(T value, String tag, Stat<T> follow) {
        super(value,tag,follow);
    }
    boolean isRecord() {
        return  (mFollowed.getValue().doubleValue() <
                 mValue.doubleValue());
    }
}