package com.reconinstruments.offsetkeyboard;

import android.content.Context;
import android.graphics.Color;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

public class OffsetKeyboardView extends TableLayout {

	private static final String TAG = "OffsetKeyboardView";
	
	private static final int NUM_CHARS = 35;
	private static final int NUM_KEYBOARDS = 3;
	private static final int ROW_SIZE = 7;
	
	char[][] chars = new char[NUM_CHARS][NUM_KEYBOARDS];
	int currentList = 0;
	
	public OffsetKeyboardView(Context context) {
		super(context);
		
		populateKeyboard();
		initView();
	}
	
	public OffsetKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        populateKeyboard();
        initView();
    }

	public void toggleCharList() {
		currentList = (currentList + 1) % NUM_KEYBOARDS;
		redrawCharList();
	}
	
	private void initView() {
		TableRow tr = null;
		for(int i=0; i<chars.length; i++) {
			if(i % ROW_SIZE == 0) {
				if(tr != null)
					this.addView(tr);
				tr = new TableRow(this.getContext());
				tr.setGravity(Gravity.CENTER_HORIZONTAL);
			}
			
			KeyView k = new KeyView(getContext());
			
			TableRow.LayoutParams params = new TableRow.LayoutParams(
					TableRow.LayoutParams.WRAP_CONTENT, 
					TableRow.LayoutParams.WRAP_CONTENT);
			params.setMargins(12, 0, 12, 0);
			
			tr.addView(k, params);
		}
		
		if(tr.getChildCount() > 0)
			this.addView(tr);
		
		redrawCharList();
		((TableRow) this.getChildAt(2)).getChildAt(3).setBackgroundResource(R.drawable.key_selector);
	}
	
	private void populateKeyboard() {
		// Uppercase list
		chars[0][0] = (char) 0x2190; // left arrow
		chars[1][0] = (char) 0x2192; // right arrow
		chars[2][0] = (char) 0x1; // lower case
		chars[3][0] = (char) ' '; // space
		int i = 4;
		for(char c= 'A'; c<= 'Z'; c++) {
			Log.v(TAG, "" + (char) c);
			chars[i][0] = c;
			i++;
		}
		chars[30][0] = '@';
		chars[31][0] = ',';
		chars[32][0] = '.';
		chars[33][0] = '\n'; // enter
		chars[34][0] = (char) 0x21D0; // arrow (delete)
		
		// Lowercase list
		chars[0][1] = (char) 0x2190; // left arrow
		chars[1][1] = (char) 0x2192; // right arrow
		chars[2][1] = (char) 0x2; // numbers
		chars[3][1] = ',';
		chars[4][1] = '.';
		chars[5][1] = '?';
		chars[6][1] = (char) ' '; // space
		int j = 7;
		for(char c= 'a'; c<= 'z'; c++) {
			chars[j][1] = c;
			j++;
		}
		chars[33][1] = 0x3; // arrow (enter)
		chars[34][1] = (char) 0x21D0; // arrow (delete)
		
		// Numbers & Symbols List
		chars[0][2] = (char) 0x2190; // left arrow
		chars[1][2] = (char) 0x2192; // right arrow
		chars[2][2] = (char) 0x3; // enter
		chars[3][2] = (char) 0x005F; // underscore
		
		int k = 4;
		for(char c = '0'; c <='9'; c++) {
			chars[k][2] = c;
			k++;
		}
		chars[14][2] = '#';
		chars[15][2] = '$';
		chars[16][2] = '(';
		chars[17][2] = ')';
		chars[18][2] = '-';
		chars[19][2] = '/';
		chars[20][2] = '<';
		chars[21][2] = '>';
		chars[22][2] = '=';
		chars[23][2] = '?';
		chars[24][2] = '!';
		chars[25][2] = '[';
		chars[26][2] = ']';
		chars[27][2] = '%';
		chars[28][2] = (char) 0x0022; // "
		chars[29][2] = (char) 0x0027; // '
		chars[30][2] = ':';
		chars[31][2] = '+';
		chars[32][2] = '*';
		chars[33][2] = '\n'; // arrow (enter)
		chars[34][2] = 0x21D0; // arrow (delete)
	}
	
	private void redrawCharList() {
		int charIndex = 0;
		for(int i=0; i<this.getChildCount(); i++) {
			TableRow tr = (TableRow) this.getChildAt(i);
			for(int j=0; j<tr.getChildCount(); j++) {
				boolean isWhite = (i==2 || j == 3);
				
				KeyView tv = (KeyView) tr.getChildAt(j);
				
				switch(chars[charIndex][currentList]) {
				case 0x2190:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.LEFT_ARROW, isWhite));
					break;
				
				case 0x2192:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.RIGHT_ARROW, isWhite));
					break;
					
				case 0x1:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.LOWER_CASE, isWhite));
					break;
					
				case 0x2:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.NUMBER, isWhite));
					break;
					
				case 0x3:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.UPPER_CASE, isWhite));
					break;
					
				case 0x21D0:
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.DELETE, isWhite));
					break;
					
				case '\n':
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.ENTER, isWhite));
					break;
					
				case ' ':
					tv.setDrawableResource(KeyView.drawableLookup(KeyView.SPACE, isWhite));
					break;
				
				default:
					tv.setText(Character.toString(chars[charIndex][currentList]));
					tv.setTextColor(isWhite ? KeyView.WHITE : KeyView.GREY);
				}
				
				charIndex++;
			}
		}
	}
	
	public void shiftLeft() {
		// move the last element to the front
		char[][] newCharList = new char[NUM_CHARS][NUM_KEYBOARDS];
		System.arraycopy(chars, NUM_CHARS-1, newCharList, 0, 1);
		System.arraycopy(chars, 0, newCharList, 1, NUM_CHARS-1);
		
		chars = newCharList;
		
		redrawCharList();
	}
	
	public void shiftRight() {
		// move the first element to the back
		char[][] newCharList = new char[NUM_CHARS][NUM_KEYBOARDS];
		System.arraycopy(chars, 0, newCharList, NUM_CHARS-1, 1);
		System.arraycopy(chars, 1, newCharList, 0, NUM_CHARS-1);
		
		chars = newCharList;
		
		redrawCharList();
	}
	
	public void shiftDown() {
		// Move the first ROW_SIZE elements to the end
		char[][] newCharList = new char[NUM_CHARS][NUM_KEYBOARDS];
		System.arraycopy(chars, 0, newCharList, newCharList.length - ROW_SIZE, ROW_SIZE);
		System.arraycopy(chars, ROW_SIZE, newCharList, 0, newCharList.length - ROW_SIZE);
		
		chars = newCharList;
		
		redrawCharList();
	}
	
	public void shiftUp() {
		// Move the last ROW_SIZE elements to the front
		char[][] newCharList = new char[NUM_CHARS][NUM_KEYBOARDS];
		System.arraycopy(chars, chars.length - ROW_SIZE, newCharList, 0, ROW_SIZE);
		System.arraycopy(chars, 0, newCharList, ROW_SIZE, chars.length - ROW_SIZE);
		
		chars = newCharList;
		
		redrawCharList();
	}
	
	public char getSelectedChar() {
		return chars[17][currentList];
	}
}
