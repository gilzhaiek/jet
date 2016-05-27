/*
 * < ParameterView.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Generic parameter entry interface used by sample applications for the
 * Stonestreet One Bluetooth Protocol Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * View which contains a set of subviews which can be
 * selected based on the current mode. Each view serves
 * as a method of entry for differing types of parameter data
 * in a Stonestreet One Bluetopia PM sample application.
 */
public class ParameterView extends RelativeLayout {
    private static final int LABEL_VIEW_ID = 2;
    
    /**
     * The current mode being displayed by the ParameterView
     */
    public enum Mode {
        /**
         * Hidden.
         */
        EMPTY,
        
        /**
         * Presents text input.
         */
        TEXT,
        
        /**
         * Displays a spinner to select
         * a single item from a list.
         */
        SPINNER,
        
        /**
         * Displays a dialog to select one or
         * more items from a list.
         */
        CHECKLIST,
        
        /**
         * Displays a checkbox to toggle on or off.
         */
        CHECKBOX,
        
        /**
         * Displays a seek bar to set a value within a defined range
         */
        SEEKBAR,
        
        /**
         * Displays a dialog which allows input of one or more set of text strings.
         */
        MULTITEXT
    }

    /**
     * The parameter value while in text mode.
     */
    public class TextValue {
        /**
         * The input text value.
         */
        public final CharSequence text;

        /*pkg*/ TextValue(CharSequence text) {
            this.text = text;
        }
    }

    /**
     * The parameter value while in spinner mode.
     */
    public class SpinnerValue {
        /**
         * The list of items displayed in the spinner.
         */
        public final CharSequence[] values;
        
        /**
         * This item currently selected.
         */
        public final int selectedItem;

        /*pkg*/ SpinnerValue(CharSequence[] values, int selectedItem) {
            this.values = values.clone();
            this.selectedItem = selectedItem;
        }
    }

    /**
     * The parameter value while in checklist mode.
     */
    public class ChecklistValue {
        /**
         * The list of items displayed in the check list.
         */
        public final CharSequence[] values;
        
        /**
         * Indicates which items in the list are selected. Each item
         * has entry.
         */
        public final boolean[] checkedItems;

        /*pkg*/ ChecklistValue(CharSequence[] values, boolean[] checkedItems) {
            this.values = values.clone();
            this.checkedItems = checkedItems.clone();
        }
    }

    /**
     * The parameter value while in checkbox mode.
     */
    public class CheckboxValue {
        /**
         * Indicates whether the checkbox is checked.
         */
        public final boolean value;

        /*pkg*/ CheckboxValue(boolean value) {
            this.value = value;
        }
    }
    
    /**
     * The parameter value while in seek bar mode.
     */
    public class SeekbarValue {
        /**
         * The current position of the seek bar.
         */
        public final int position;
        
        /**
         * The maximum position value.
         */
        public final int max;
        
        /*pkg*/ SeekbarValue(int position, int max) {
            this.position = position;
            this.max = max;
        }
    }
    
    /**
     * The parameter value while in multitext mode.
     */
    public class MultitextValue {
        /**
         * The set of text entries that were input.
         */
        public final String[][] textData;
        
        /*pkg*/ MultitextValue(String[][] textData) {
            if(textData != null)
                this.textData = textData.clone();
            else
                this.textData = null;
        }
    }
    
    /**
     * Interface to listen for changes while in spinner mode.
     */
    public interface OnSpinnerItemChangedListener {
        /**
         * Called when the value of the parameter is changed in Spinner mode.
         * 
         * @param newItem The index of the new item or -1 if nothing is selected.
         */
        public void onSpinnerItemChange(int newItem);
    }
    
    /**
     * Interface to listen for changes while in check list mode.
     */
    public interface OnChecklistItemCheckedListener {
        /**
         * Called when an item is checked or unchecked in the Checklist mode.
         * 
         * @param item The index of the item checked.
         * @param checked Whether the item has been checked or unchecked.
         */
        public void onItemChecked(int item, boolean checked);
    }
    
    /**
     * Interface to listen for changes while in check box mode.
     */
    public interface OnCheckboxCheckedListener {
        /**
         * Called when an item is checked or unchecked in the Checkbox mode.
         * 
         * @param checked Whether the item has been checked or unchecked.
         */
        public void onChecked(boolean checked);
    }

    /*
     * Attribute "mode" values.
     */
    private final static String[] ModeAttributeValues = new String[Mode.values().length];
    {
        Mode[] values = Mode.values();
        for(int i = 0; i < values.length; i++)
            ModeAttributeValues[i] = values[i].name().toLowerCase();
    }

    /*
     * Internal state members.
     */

    private Mode currentMode;
    private TextView labelView;
    
    private OnSpinnerItemChangedListener mSpinnerItemChanged;
    private OnChecklistItemCheckedListener mChecklistItemChecked;
    private OnCheckboxCheckedListener mCheckboxChecked;
    
    /*
     * Text Mode state variables.
     */
    private EditText textView;

    /*
     * Spinner Mode state variables.
     */
    private Spinner  spinnerView;
    private CharSequence[] spinnerValues;
    private ArrayAdapter<CharSequence> spinnerAdapter;

    /*
     * Checklist Mode state variables.
     */
    private Button   checklistButton;
    private AlertDialog checklistDialog;
    private CharSequence[] checklistValues;
    private boolean[] checklistSelections;

    /*
     * Checkbox Mode state variables.
     */
    private TextView checkboxLabelView;
    private CheckBox checkboxView;
    
    /*
     * SeekBar state variable.
     */
    private SeekBar seekbarView;
    
    /* 
     * Multitext sate variables.
     */
    private Button multitextButton;
    private AlertDialog multitextDialog;
    private MultiTextListAdapter multitextAdapter;
    private String[][] multitextData;

    /**
     * Creates a parameter view with the specified attributes.
     * 
     * @param context The current context.
     * @param attrs The attributes to set.
     */
    public ParameterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(attrs);
        setupViews();
    }

    /**
     * Creates a ParameterView showing the specified mode.
     * 
     * @param context The current context.
     * @param style The mode to set.
     */
    public ParameterView(Context context, Mode style) {
        super(context);

        currentMode = ((style != null) ? style : Mode.EMPTY);

        setupViews();
    }
    
    /**
     * Creates a ParameterView that is hidden by default.
     *  
     * @param context The current context.
     */
    public ParameterView(Context context) {
        super(context);
        
        currentMode = Mode.EMPTY;
        
        setupViews();
    }
    
    
    
    /**
     * Assign a listener for changes when in spinner mode.
     * 
     * @param listener The listener to call.
     */
    public synchronized void setOnSpinnerItemChangedListener(OnSpinnerItemChangedListener listener) {
        mSpinnerItemChanged = listener;
    }
    
    /**
     * Assign a listener for changes when in check list mode.
     * 
     * @param listener The listener to call.
     */
    public synchronized void setOnChecklistItemCheckedListener(OnChecklistItemCheckedListener listener) {
        mChecklistItemChecked = listener;
    }
    
    /**
     * Assign a listener for changes when in check box mode.
     * 
     * @param listener The listener to call.
     */
    public synchronized void setOnCheckboxItemCheckedListener(OnCheckboxCheckedListener listener) {
        mCheckboxChecked = listener;
    }
    
    
    /**
     * Present text entry for this parameter.
     * 
     * @param text The default value.
     * @param hint The hint to display when the input is empty.
     */
    public synchronized void setModeText(CharSequence text, CharSequence hint) {
        setModeText(text, hint, InputType.TYPE_CLASS_TEXT);
    }

    /**
     * Present text entry for this parameter.
     * 
     * @param text The default value.
     * @param hint The hint to display when the input is empty.
     * @param inputType The class of input. Useful for numeric input.
     */
    public synchronized void setModeText(CharSequence text, CharSequence hint, int inputType) {
        currentMode = Mode.TEXT;
        setupViews();

        textView.setText(text);
        textView.setHint(hint);
        textView.setInputType(inputType);
    }

    /**
     * Obtain the current value in text mode.
     * 
     * @return The current text value.
     */
    public synchronized TextValue getValueText() {
        return new TextValue(textView.getText());
    }

    /**
     * Display a single item list selection for this parameter,
     * 
     * @param prompt The text to display which describes the input.
     * @param values The list of items to populate the list.
     */
    public synchronized void setModeSpinner(CharSequence prompt, CharSequence[] values) {
        currentMode = Mode.SPINNER;
        setupViews();

        labelView.setText(prompt + ":");
        
        spinnerValues = values.clone();
        spinnerAdapter = new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_item, spinnerValues);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerView.setPrompt(prompt);
        spinnerView.setAdapter(spinnerAdapter);
    }

    /**
     * Obtain the current value in spinner mode.
     * 
     * @return The current spiner value.
     */
    public synchronized SpinnerValue getValueSpinner() {
        return new SpinnerValue(spinnerValues, spinnerView.getSelectedItemPosition());
    }

    /**
     * Display a list of items of which one or more can be selected for this parameter.
     * 
     * @param subject The description of the items.
     * @param values The list of items to display.
     * @param checkedValues The default checked value for each item.
     */
    public synchronized void setModeChecklist(CharSequence subject, CharSequence[] values, boolean[] checkedValues) {
        if((values == null) || (checkedValues == null) || (values.length != checkedValues.length))
            throw new IllegalArgumentException("'values' and 'checkedValues' arrays must be of equal length.");

        AlertDialog.Builder builder;

        currentMode = Mode.CHECKLIST;
        setupViews();

        if(subject == null)
            subject = "Parameter Options";

        checklistButton.setText("Select " + subject);

        checklistValues = values.clone();
        checklistSelections = checkedValues.clone();

        builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(checklistValues, checklistSelections, checklistDialog_onMultiChoice);
        builder.setPositiveButton(android.R.string.ok, null);
        checklistDialog = builder.create();
    }

    /**
     * Obtain the current value in check list mode.
     * 
     * @return The current check list value.
     */
    public synchronized ChecklistValue getValueChecklist() {
        return new ChecklistValue(checklistValues, checklistSelections);
    }

    /**
     * Display a check box to toggle on and off for this parameter.
     * @param label
     * @param value
     */
    public synchronized void setModeCheckbox(CharSequence label, boolean value) {
        currentMode = Mode.CHECKBOX;
        setupViews();

        checkboxLabelView.setText(label);
        checkboxView.setChecked(value);
    }

    /**
     * Obtain the current value in check box mode.
     * 
     * @return The current check box value.
     */
    public synchronized CheckboxValue getValueCheckbox() {
        return new CheckboxValue(checkboxView.isChecked());
    }
    
    /**
     * Display a seek bar to select within a defined range of input for this parameter.
     * 
     * @param label The label to describe the input.
     * @param max The maximum value that can be selected.
     * @param position The current/default value.
     */
    public synchronized void setModeSeekbar(CharSequence label, int max, int position) {
        currentMode = Mode.SEEKBAR;
        setupViews();
        
        labelView.setText(label);
        
        seekbarView.setMax(max);
        seekbarView.setProgress(position);
    }
    
    /**
     * Obtain the current value in seek bar mode.
     * 
     * @return The current seek bar value.
     */
    public synchronized SeekbarValue getValueSeekbar() {
        return new SeekbarValue(seekbarView.getProgress(), seekbarView.getMax());
    }
    
    /**
     * Display a dialog which allows multiple sets of text strings to be entered for this parameter.
     * Each set can have any number of strings, and any number of sets can be added. For instance, you can create a multitext parameter to
     * input the title and artist if n number of songs. A text field for title and artists is created for every additional set requested.
     * 
     * @param subject The description of the input.
     * @param numberFields The number of items in <i>each<i> set of strings. For instance, to input name and artists for any number of songs, this value would be 2.
     * @param hints The hints to display for each item. Should be the same length as numberFields.
     */
    public synchronized void setModeMultitext(String subject, int numberFields, String[] hints) {
        int[] inputTypes = new int[numberFields];
        
        for(int i=0;i<numberFields;i++)
            inputTypes[i] = InputType.TYPE_CLASS_TEXT;
        
        setModeMultitext(subject, numberFields, hints, inputTypes);
    }
    
    /**
     * Display a dialog which allows multiple sets of text strings to be entered for this parameter.
     * Each set can have any number of strings, and any number of sets can be added. For instance, you can create a multitext parameter to
     * input the title and artist if n number of songs. A text field for title and artists is created for every additional set requested.
     * 
     * @param subject The description of the input.
     * @param numberFields The number of items in <i>each<i> set of strings. For instance, to input name and artists for any number of songs, this value would be 2.
     * @param hints The hints to display for each item. Should be the same length as numberFields.
     * @param inputTypes The input type for each of the items in a set.
     */
    public synchronized void setModeMultitext(String subject, int numberFields, String[] hints, int[] inputTypes) {
        currentMode = Mode.MULTITEXT;
        setupViews();
        
        if(hints != null && hints.length == numberFields) {
            multitextData = null;
        
            if(subject == null)
                subject = "Enter selections";
        
            multitextButton.setText("Enter " + subject);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    multitextData = multitextAdapter.getValues();
                    
                }
            });
            
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    multitextAdapter.assignValues(multitextData);
                    
                }
            });
            
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.multitext_dialog, null, false);
            ListView inputList = (ListView)dialogView.findViewById(R.id.textList);
            Button addButton = (Button)dialogView.findViewById(R.id.addButton);
            
            multitextAdapter = new MultiTextListAdapter(getContext(), numberFields, hints, inputTypes);
            
            inputList.setAdapter(multitextAdapter);
            
            addButton.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    multitextAdapter.addItem();
                    
                }
            });
            
            builder.setView(dialogView);
            multitextDialog = builder.create();
            

            
        }
    }
    
    /**
     * Obtain the current value in multitext mode.
     * 
     * @return The current multitext value.
     */
    public synchronized MultitextValue getValueMultitext() {
        return new MultitextValue(multitextData);
    }

    /**
     * Hide the parameter view from view.
     */
    public synchronized void setModeHidden() {
        currentMode = Mode.EMPTY;
        setupViews();
    }

    private void parseAttributes(AttributeSet attrs) {
        String mode = attrs.getAttributeValue(null, "mode");

        if(mode != null) {
            if(mode.equalsIgnoreCase(Mode.EMPTY.name())) {
                currentMode = Mode.EMPTY;
            } else if(mode.equalsIgnoreCase(Mode.TEXT.name())) {
                currentMode = Mode.TEXT;
            } else if(mode.equalsIgnoreCase(Mode.SPINNER.name())) {
                currentMode = Mode.SPINNER;
            } else if(mode.equalsIgnoreCase(Mode.CHECKLIST.name())) {
                currentMode = Mode.CHECKLIST;
            } else if(mode.equalsIgnoreCase(Mode.CHECKBOX.name())) {
                currentMode = Mode.CHECKBOX;
            } else if(mode.equalsIgnoreCase(Mode.SEEKBAR.name())) {
                currentMode = Mode.SEEKBAR;
            } else {
                StringBuilder msg = new StringBuilder("'mode' attribute must be specified as one of: ");
                for(String s : ModeAttributeValues)
                    msg.append(s).append(' ');
                throw new RuntimeException(msg.toString());
            }
        } else {
            currentMode = Mode.EMPTY;
        }
    }

    private void setupViews() {
        LayoutParams layout;
        
        /*
         * Set up shared views
         */
        if(labelView == null) {
            labelView = new TextView(getContext());
            labelView.setId(LABEL_VIEW_ID);
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_TOP, TRUE);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            labelView.setLayoutParams(layout);
            addView(labelView);
        }
        
        switch(currentMode) {
        case SEEKBAR:
        case SPINNER:
            labelView.setVisibility(View.VISIBLE);
            break;
        case CHECKBOX:
        case CHECKLIST:
        case EMPTY:
        case TEXT:
        default:
            labelView.setVisibility(View.GONE);
            break;
        
        }

        /*
         * Set up text mode.
         */
        if(textView == null) {
            textView = new EditText(getContext());
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_TOP, TRUE);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            textView.setLayoutParams(layout);
            addView(textView);
        }
        textView.setVisibility(((currentMode == Mode.TEXT) ? VISIBLE : GONE));

        /*
         * Set up spinner mode.
         */
        if(spinnerView == null) {
            spinnerView = new Spinner(getContext());
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            layout.addRule(BELOW, labelView.getId());
            spinnerView.setLayoutParams(layout);
            spinnerView.setOnItemSelectedListener(spinnerView_onItemSelected);
            addView(spinnerView);
        }
        spinnerView.setVisibility(((currentMode == Mode.SPINNER) ? VISIBLE : GONE));

        /*
         * Set up checklist mode.
         */
        if(checklistButton == null) {
            checklistButton = new Button(getContext());
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_TOP, TRUE);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            checklistButton.setLayoutParams(layout);
            checklistButton.setOnClickListener(checklistButton_onClick);
            addView(checklistButton);
        }
        checklistButton.setVisibility(((currentMode == Mode.CHECKLIST) ? VISIBLE : GONE));

        /*
         * Set up checkbox mode.
         */
        if(checkboxView == null) {
            checkboxView = new CheckBox(getContext());
            layout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(CENTER_VERTICAL, TRUE);
            layout.addRule(ALIGN_PARENT_RIGHT, TRUE);
            checkboxView.setLayoutParams(layout);
            checkboxView.setId(1);
            checkboxView.setOnCheckedChangeListener(checkboxView_onCheckedChanged);
            addView(checkboxView);

            checkboxLabelView = new TextView(getContext());
            layout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(CENTER_VERTICAL, TRUE);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            layout.addRule(LEFT_OF, checkboxView.getId());
            checkboxLabelView.setLayoutParams(layout);
            addView(checkboxLabelView);
        }
        checkboxView.setVisibility(((currentMode == Mode.CHECKBOX) ? VISIBLE : GONE));
        checkboxLabelView.setVisibility(((currentMode == Mode.CHECKBOX) ? VISIBLE : GONE));
        
        /*
         * Set up seekbar mode.
         */
        if(seekbarView == null) {
            seekbarView = new SeekBar(getContext());
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            layout.addRule(BELOW, labelView.getId());
            seekbarView.setLayoutParams(layout);
            addView(seekbarView);
        }
        seekbarView.setVisibility(((currentMode == Mode.SEEKBAR) ? VISIBLE : GONE));
        
        if(multitextButton == null) {
            multitextButton = new Button(getContext());
            layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.addRule(ALIGN_PARENT_TOP, TRUE);
            layout.addRule(ALIGN_PARENT_LEFT, TRUE);
            multitextButton.setLayoutParams(layout);
            multitextButton.setOnClickListener(multitextButton_onClick);
            addView(multitextButton);
        }
        multitextButton.setVisibility(((currentMode == Mode.MULTITEXT) ? VISIBLE : GONE));
        
    }

    OnClickListener checklistButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if((currentMode == Mode.CHECKLIST) && (checklistDialog != null))
                checklistDialog.show();
        }
    };
    
    OnClickListener multitextButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if((currentMode == Mode.MULTITEXT) && (multitextDialog != null)) {
                multitextDialog.show();
            
                //Make sure the window is focusable, otherwise the soft keyboard will not show up
                multitextDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
        }
    };

    OnMultiChoiceClickListener checklistDialog_onMultiChoice = new OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            synchronized(ParameterView.this) {
                if(which >= checklistSelections.length)
                    throw new IndexOutOfBoundsException("Illegal state: selection of list item " + which + " but only " + checklistSelections.length);

                checklistSelections[which] = isChecked;
                
                if(mChecklistItemChecked != null) {
                    mChecklistItemChecked.onItemChecked(which, isChecked);
                }
            }
        }
    };
    
    OnItemSelectedListener spinnerView_onItemSelected = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            synchronized(ParameterView.this) {
                if(mSpinnerItemChanged != null)
                    mSpinnerItemChanged.onSpinnerItemChange(position);
            }
            
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            synchronized(ParameterView.this) {
                if(mSpinnerItemChanged != null)
                    mSpinnerItemChanged.onSpinnerItemChange(-1);
            }
            
        }
        
    };
    
    OnCheckedChangeListener checkboxView_onCheckedChanged = new OnCheckedChangeListener() {
        
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            synchronized(ParameterView.this) {
                if(mCheckboxChecked != null)
                    mCheckboxChecked.onChecked(isChecked);
            }
            
        }
    };

}
