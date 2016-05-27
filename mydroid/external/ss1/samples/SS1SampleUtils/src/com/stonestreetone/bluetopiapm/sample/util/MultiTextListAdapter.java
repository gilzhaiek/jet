package com.stonestreetone.bluetopiapm.sample.util;

import java.util.ArrayList;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

/*package*/ class MultiTextListAdapter extends BaseAdapter {
    private final Context mCxt;
    private final LayoutInflater inflater;

    private final int numberFields;
    private ArrayList<TextData[]> textData;
    private final String[] hintData;
    private final int[] inputTypes;

    public MultiTextListAdapter(Context cxt, int numberTextFields, String[] hints, int[] inputTypes) {
        mCxt = cxt;
        inflater = (LayoutInflater)mCxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        numberFields = numberTextFields;
        textData = new ArrayList<TextData[]>();
        hintData = hints.clone();
        this.inputTypes = inputTypes.clone();
    }
    

    @Override
    public int getCount() {
        return textData.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
        View containerView;
        ImageButton deleteButton;
        
        if(convertView != null) {
            containerView = convertView;
            deleteButton = (ImageButton)containerView.findViewById(R.id.cancelButton);
        }
        else {
            containerView = inflater.inflate(R.layout.multitext_list_item_container, parent, false);
            
            deleteButton = (ImageButton)containerView.findViewById(R.id.cancelButton);
            deleteButton.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    Log.d("ADAPTER", "Clicked");
                    ButtonTag tag = (ButtonTag)v.getTag();
                    Log.d("ADAPTER", tag.listIndex + "");
                    MultiTextListAdapter.this.removeItem(tag.listIndex);
                    
                }
            });
            
            
            LinearLayout containerLayout = (LinearLayout)containerView.findViewById(R.id.multitext_container);
            
            for(int i=0; i<numberFields; i++) {
                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView tv = new SingleWatcherEditText(mCxt);
                tv.setId(i);
                tv.setLayoutParams(params);
                tv.setHint(hintData[i]);
                tv.setInputType(inputTypes[i]);

                containerLayout.addView(tv);
            }
            
        }
        
        deleteButton.setTag(new ButtonTag(index));
        
        for(int i=0; i<numberFields;i++) {
            SingleWatcherEditText tv = (SingleWatcherEditText)containerView.findViewById(i);
            tv.setTextWatcher(textData.get(index)[i]);
            tv.setText(textData.get(index)[i].text);
            
            if(tv.hasFocus()) {
                tv.clearFocus();
                InputMethodManager imm = (InputMethodManager)mCxt.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);
            }

        }
        
        return containerView;
            
    }
    
    public void addItem() {
        Log.d("Multitext", "Add Item Called");
        TextData[] newData = new TextData[numberFields];
        
        for(int i=0;i<numberFields;i++) {
            newData[i] = new TextData();
            newData[i].text = "";
        }

        textData.add(newData);
        notifyDataSetChanged();
    }
    
    public void removeItem(int index) {
        Log.d("ADAPTER", index + ":" + numberFields);
        if(index < textData.size()) {
            textData.remove(index);
            notifyDataSetChanged();
        }
    }
    
    public String[][] getValues() {
        String[][] valueArray = new String[textData.size()][numberFields];
        
        for(int i=0;i<textData.size();i++) {
            for(int j=0;j<numberFields;j++) {
                valueArray[i][j] = textData.get(i)[j].text;
            }
        }
        
        return valueArray;
    }
    
    public void assignValues(String[][] newValues) {
        textData = new ArrayList<TextData[]>();
        
        if(newValues != null) {
            if(newValues[0] != null && newValues[0].length == numberFields) {
                for(int i=0;i<newValues.length;i++) {
                    TextData[] arr = new TextData[numberFields];
                    
                    for(int j=0;j<numberFields;j++) {
                        arr[j] = new TextData();
                        arr[j].text = newValues[i][j];
                    }
                    
                    textData.add(arr);
                }
            }
        }
            
        notifyDataSetChanged();
    }
    
    private class SingleWatcherEditText extends EditText {
        private TextWatcher watcher;
        
        private TextWatcher localWatcher = new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(watcher != null)
                    watcher.onTextChanged(s, start, before, count);
                
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(watcher != null)
                    watcher.beforeTextChanged(s, start, count, after);
                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
               if(watcher != null)
                   watcher.afterTextChanged(s);
                
            }
        };

        public SingleWatcherEditText(Context context) {
            super(context);
            
            addTextChangedListener(localWatcher);
        }
        
        public void setTextWatcher(TextWatcher watcher) {
            this.watcher = watcher;
        }
        
    }
    
    private static class TextData implements TextWatcher {
        public String text;

        @Override
        public void afterTextChanged(Editable s) {
            if(!text.equals(s))
                text = s.toString();
            
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO Auto-generated method stub
            
        }
    }
    
    private class ButtonTag {
        public final int listIndex;
        
        public ButtonTag(int listIndex) {
            this.listIndex = listIndex;
        }
    }
}
