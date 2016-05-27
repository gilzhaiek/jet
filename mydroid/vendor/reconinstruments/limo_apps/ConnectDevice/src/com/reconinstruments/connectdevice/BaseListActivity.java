package com.reconinstruments.connectdevice;

import android.app.ListActivity;
import android.os.Bundle;

public class BaseListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.baselist);
    }
}
