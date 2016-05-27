package com.reconinstruments.mfitester;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ParameterView extends RelativeLayout {

	public enum Mode {
		EMPTY, TEXT, SPINNER, CHECKLIST, CHECKBOX,
	}

	public class TextValue {
		public final CharSequence text;

		/* pkg */TextValue(CharSequence text) {
			this.text = text;
		}
	}

	public class SpinnerValue {
		public final CharSequence[] values;
		public final int selectedItem;

		/* pkg */SpinnerValue(CharSequence[] values, int selectedItem) {
			this.values = values.clone();
			this.selectedItem = selectedItem;
		}
	}

	public class ChecklistValue {
		public final CharSequence[] values;
		public final boolean[] checkedItems;

		/* pkg */ChecklistValue(CharSequence[] values, boolean[] checkedItems) {
			this.values = values.clone();
			this.checkedItems = checkedItems.clone();
		}
	}

	public class CheckboxValue {
		public final boolean value;

		/* pkg */CheckboxValue(boolean value) {
			this.value = value;
		}
	}

	/*
	 * Attribute "mode" values.
	 */
	private final static String[] ModeAttributeValues = new String[Mode
			.values().length];
	{
		Mode[] values = Mode.values();
		for (int i = 0; i < values.length; i++)
			ModeAttributeValues[i] = values[i].name().toLowerCase();
	}

	/*
	 * Internal state members.
	 */

	private Mode currentMode;

	/*
	 * Text Mode state variables.
	 */
	private EditText textView;

	/*
	 * Spinner Mode state variables.
	 */
	private Spinner spinnerView;
	private CharSequence[] spinnerValues;
	private ArrayAdapter<CharSequence> spinnerAdapter;

	/*
	 * Checklist Mode state variables.
	 */
	private Button checklistButton;
	private AlertDialog checklistDialog;
	private CharSequence[] checklistValues;
	private boolean[] checklistSelections;

	/*
	 * Checkbox Mode state variables.
	 */
	private TextView checkboxLabelView;
	private CheckBox checkboxView;

	/**
	 * @param context
	 * @param attrs
	 */
	public ParameterView(Context context, AttributeSet attrs) {
		super(context, attrs);

		parseAttributes(attrs);
		setupViews();
	}

	public ParameterView(Context context, Mode style) {
		super(context);

		currentMode = ((style != null) ? style : Mode.EMPTY);

		setupViews();
	}

	public synchronized void setModeText(CharSequence text, CharSequence hint) {
		setModeText(text, hint, InputType.TYPE_CLASS_TEXT);
	}

	public synchronized void setModeText(CharSequence text, CharSequence hint,
			int inputType) {
		currentMode = Mode.TEXT;
		setupViews();

		textView.setText(text);
		textView.setHint(hint);
		textView.setInputType(inputType);
	}

	public synchronized TextValue getValueText() {
		return new TextValue(textView.getText());
	}

	public synchronized void setModeSpinner(CharSequence prompt,
			CharSequence[] values) {
		currentMode = Mode.SPINNER;
		setupViews();

		spinnerValues = values.clone();
		spinnerAdapter = new ArrayAdapter<CharSequence>(getContext(),
				android.R.layout.simple_spinner_item, spinnerValues);
		spinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerView.setPrompt(prompt);
		spinnerView.setAdapter(spinnerAdapter);
	}

	public synchronized SpinnerValue getValueSpinner() {
		return new SpinnerValue(spinnerValues,
				spinnerView.getSelectedItemPosition());
	}

	public synchronized void setModeChecklist(CharSequence subject,
			CharSequence[] values, boolean[] checkedValues) {
		if ((values == null) || (checkedValues == null)
				|| (values.length != checkedValues.length))
			throw new IllegalArgumentException(
					"'values' and 'checkedValues' arrays must be of equal length.");

		AlertDialog.Builder builder;

		currentMode = Mode.CHECKLIST;
		setupViews();

		if (subject == null)
			subject = "Parameter Options";

		checklistButton.setText("Select " + subject);

		checklistValues = values.clone();
		checklistSelections = checkedValues.clone();

		builder = new AlertDialog.Builder(getContext());
		builder.setMultiChoiceItems(checklistValues, checklistSelections,
				checklistDialog_onMultiChoice);
		builder.setPositiveButton(android.R.string.ok, null);
		checklistDialog = builder.create();
	}

	public synchronized ChecklistValue getValueChecklist() {
		return new ChecklistValue(checklistValues, checklistSelections);
	}

	public synchronized void setModeCheckbox(CharSequence label, boolean value) {
		currentMode = Mode.CHECKBOX;
		setupViews();

		checkboxLabelView.setText(label);
		checkboxView.setChecked(value);
	}

	public synchronized CheckboxValue getValueCheckbox() {
		return new CheckboxValue(checkboxView.isChecked());
	}

	public synchronized void setModeHidden() {
		currentMode = Mode.EMPTY;
		setupViews();
	}

	private void parseAttributes(AttributeSet attrs) {
		String mode = attrs.getAttributeValue(null, "mode");

		if (mode != null) {
			if (mode.equalsIgnoreCase(Mode.EMPTY.name())) {
				currentMode = Mode.EMPTY;
			} else if (mode.equalsIgnoreCase(Mode.TEXT.name())) {
				currentMode = Mode.TEXT;
			} else if (mode.equalsIgnoreCase(Mode.SPINNER.name())) {
				currentMode = Mode.SPINNER;
			} else if (mode.equalsIgnoreCase(Mode.CHECKLIST.name())) {
				currentMode = Mode.CHECKLIST;
			} else if (mode.equalsIgnoreCase(Mode.CHECKBOX.name())) {
				currentMode = Mode.CHECKBOX;
			} else {
				StringBuilder msg = new StringBuilder(
						"'mode' attribute must be specified as one of: ");
				for (String s : ModeAttributeValues)
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
		 * Set up text mode.
		 */
		if (textView == null) {
			textView = new EditText(getContext());
			layout = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			layout.addRule(ALIGN_PARENT_TOP, TRUE);
			layout.addRule(ALIGN_PARENT_LEFT, TRUE);
			textView.setLayoutParams(layout);
			addView(textView);
		}
		textView.setVisibility(((currentMode == Mode.TEXT) ? VISIBLE : GONE));

		/*
		 * Set up spinner mode.
		 */
		if (spinnerView == null) {
			spinnerView = new Spinner(getContext());
			layout = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			layout.addRule(ALIGN_PARENT_TOP, TRUE);
			layout.addRule(ALIGN_PARENT_LEFT, TRUE);
			spinnerView.setLayoutParams(layout);
			addView(spinnerView);
		}
		spinnerView.setVisibility(((currentMode == Mode.SPINNER) ? VISIBLE
				: GONE));

		/*
		 * Set up checklist mode.
		 */
		if (checklistButton == null) {
			checklistButton = new Button(getContext());
			layout = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			layout.addRule(ALIGN_PARENT_TOP, TRUE);
			layout.addRule(ALIGN_PARENT_LEFT, TRUE);
			checklistButton.setLayoutParams(layout);
			checklistButton.setOnClickListener(checklistButton_onClick);
			addView(checklistButton);
		}
		checklistButton
				.setVisibility(((currentMode == Mode.CHECKLIST) ? VISIBLE
						: GONE));

		/*
		 * Set up checkbox mode.
		 */
		if (checkboxView == null) {
			checkboxView = new CheckBox(getContext());
			layout = new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			layout.addRule(ALIGN_PARENT_TOP, TRUE);
			layout.addRule(ALIGN_PARENT_RIGHT, TRUE);
			checkboxView.setLayoutParams(layout);
			checkboxView.setId(1);
			addView(checkboxView);

			checkboxLabelView = new TextView(getContext());
			layout = new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			layout.addRule(ALIGN_PARENT_TOP, TRUE);
			layout.addRule(ALIGN_PARENT_LEFT, TRUE);
			layout.addRule(LEFT_OF, checkboxView.getId());
			checkboxLabelView.setLayoutParams(layout);
			addView(checkboxLabelView);
		}
		checkboxView.setVisibility(((currentMode == Mode.CHECKBOX) ? VISIBLE
				: GONE));
		checkboxLabelView
				.setVisibility(((currentMode == Mode.CHECKBOX) ? VISIBLE : GONE));
	}

	OnClickListener checklistButton_onClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if ((currentMode == Mode.CHECKLIST) && (checklistDialog != null))
				checklistDialog.show();
		}
	};

	OnMultiChoiceClickListener checklistDialog_onMultiChoice = new OnMultiChoiceClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			synchronized (ParameterView.this) {
				if (which >= checklistSelections.length)
					throw new IndexOutOfBoundsException(
							"Illegal state: selection of list item " + which
									+ " but only " + checklistSelections.length);

				checklistSelections[which] = isChecked;
			}
		}
	};

}
