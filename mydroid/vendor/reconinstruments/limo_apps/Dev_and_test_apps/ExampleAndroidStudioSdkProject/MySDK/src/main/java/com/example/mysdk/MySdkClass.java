package com.example.mysdk;


import android.content.Context;

import com.example.mysdk2.MyOtherSdkClass;

/**
 * My example testing class.
 */
public class MySdkClass {

    public MySdkClass(Context context) {
        new MyOtherSdkClass();
    }


    /**
     * Some function that does something.
     *
     * @param param my param
     */
    public void someFunction(int param) {
    }

}
