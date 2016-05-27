/*
 * Copyright (C) 2011 Recon Instruments.
 * Most of code is take from Android SDK's SoftKeyboard sample
 * with modification to support text inputting with 4 ARROW keys + selection keys 
 */

package com.reconinstruments.softkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;


import java.util.ArrayList;
import java.util.List;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;
    static final boolean SHOW_SUGGESTION_VIEW = false;				//true to turn on suggestion view for the keyboard input
    
    private KeyboardView mInputView;
    private CandidateView mCandidateView = null;
    private CompletionInfo[] mCompletions;
    
    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;
    
    private LimoKeyboard mSymbolsKeyboard;
    private LimoKeyboard mSymbolsShiftedKeyboard;
    private LimoKeyboard mQwertyKeyboard;
    
    private LimoKeyboard mCurKeyboard;   
    
    private int mRowIndex = 0;
    private int mKeyIndex = 0;    
    static final int KeyCode_A = 97;
    static final int KeyCode_Space = 32;
    static final int KeyCode_Enter = 10;
    static final int NUM_KEYS = 34;
    static final int NUM_ROWS = 4;
    static final int[] KeyNumPerRow= { 10, 10, 9, 5 };
    static final boolean SHIFT_IS_STICKY = true;			//if true, the shift will be kept on for character Qwerty keyboard till it is click again.(
    														//i.e. the Cap status will be kept.
    
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();       
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.        	
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LimoKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LimoKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LimoKeyboard(this, R.xml.symbols_shift);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() 
    {
    	if( SHOW_SUGGESTION_VIEW )
    	{
	        mCandidateView = new CandidateView(this);
	        mCandidateView.setService(this);
	        return mCandidateView;
    	}
    	else
    	{    	
    		return null;
    	}
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        
        resetKey( mKeyIndex );
        mKeyIndex = 0;      
        
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = mQwertyKeyboard;
        
        resetKey( mKeyIndex );
        mKeyIndex = 0;
        
        if (mInputView != null) {
            mInputView.closing();
        }              
    }
    
    @Override public void onFinishInputView(boolean finishingInput)
    {
    	super.onFinishInputView(finishingInput);        	
    }
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        resetKey( mKeyIndex );
        mKeyIndex = 0;

        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
                        
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }
        
        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            	if( mInputView != null && isInputViewShown() )
            	{
	            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
	            	mKeyIndex -= 1;
	            	mKeyIndex = mKeyIndex >= 0 ? mKeyIndex : 0;	            	
	            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
	            	mInputView.invalidateAllKeys();
	            	mRowIndex = getRowIndex( mKeyIndex );
	            	return true;
            	}
            break;
            
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            	if( mInputView != null && isInputViewShown() )
            	{
            		mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
	            	mKeyIndex += 1;
	            	mKeyIndex = mKeyIndex >= NUM_KEYS ? NUM_KEYS - 1 : mKeyIndex;
	            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
	            	mInputView.invalidateAllKeys();
	            	mRowIndex = getRowIndex( mKeyIndex );
	            	return true;
            	}
            break;
            
            case KeyEvent.KEYCODE_DPAD_UP:
            	if( mInputView != null && isInputViewShown() )
            	{            	
            		if( mRowIndex > 0 )
            		{
		            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
		            	mKeyIndex = moveUpRow( mKeyIndex, mRowIndex );
		            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
		            	mInputView.invalidateAllKeys();
		            	mRowIndex -= 1;
		            	return true;
            		}
            	}
            break;
            
            case KeyEvent.KEYCODE_DPAD_DOWN:
            	if( mInputView != null && isInputViewShown() )            	
            	{
            		if( mRowIndex < NUM_ROWS - 1 )
            		{
		            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
		            	mKeyIndex = moveDownRow( mKeyIndex, mRowIndex );
		            	mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
		            	mInputView.invalidateAllKeys();
		            	mRowIndex += 1;
		            	return true;
            		}
            	}
            break;
            
            case KeyEvent.KEYCODE_DPAD_CENTER:
            	if( mInputView != null && isInputViewShown() )
            	{          
            		int actualKeyCode = mCurKeyboard.getKeys().get(mKeyIndex).codes[0];
            		
            		if( actualKeyCode == Keyboard.KEYCODE_DELETE )
            		{
            			handleBackspace();
            			return true;
            		}
            		else if( actualKeyCode == Keyboard.KEYCODE_SHIFT )
            		{
            			handleShift();
            			return true;
            		}
            		else if( actualKeyCode >= KeyCode_A && actualKeyCode <= KeyCode_A + 25 )
            		{
                		handleCharacter( actualKeyCode, null );
    	            	return true;
            			
            		}
            		else if( actualKeyCode ==  KeyCode_Space )
            		{
            			keyDownUp( KeyEvent.KEYCODE_SPACE );
            			
            		}
            		else if( actualKeyCode == KeyCode_Enter )
            		{
            			keyDownUp( KeyEvent.KEYCODE_ENTER );
            		}     
            		else if( actualKeyCode == Keyboard.KEYCODE_MODE_CHANGE )
            		{
            			handleKeyboardModeChange();
            			return true;
            		}
            		else if( actualKeyCode == Keyboard.KEYCODE_CANCEL )
            		{
            			handleClose();
            			return true;
            		}
            		else
            		{
            			handleSymbol( actualKeyCode, null );
            		}
            		
            	}
            break;            	
            
            	
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);                    
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        
        return super.onKeyUp(keyCode, event);
    }
    
    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
            //mInputView.setShifted(mCapsLock);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) 
    {
    	
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
 
    	
/*    	
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent( 0, 0, KeyEvent.ACTION_DOWN, keyEventCode, 0, KeyEvent.META_SHIFT_ON));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCode, 0, KeyEvent.META_SHIFT_ON));
*/
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) 
    {
    	if (primaryCode == Keyboard.KEYCODE_DELETE) 
    	{
            handleBackspace();
        } 
    	else if (primaryCode == Keyboard.KEYCODE_SHIFT) 
        {
            handleShift();
        } 
    	else if (primaryCode == Keyboard.KEYCODE_CANCEL) 
        {
            handleClose();
            return;
        } 
    	else if (primaryCode == LimoKeyboardView.KEYCODE_OPTIONS) 
    	{
            // Show a menu or somethin'
        } 
    	else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) 
        {
    		handleKeyboardModeChange( );
        }
        else
        {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        if( SHIFT_IS_STICKY == false )
        {
        	updateShiftKeyState(getCurrentInputEditorInfo());
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }
    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        if( SHIFT_IS_STICKY == false )
        {
        	updateShiftKeyState(getCurrentInputEditorInfo());
        }
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) 
        {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } 
        else if (currentKeyboard == mSymbolsKeyboard) 
        {
        	mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
            mCurKeyboard = mSymbolsShiftedKeyboard;
            mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
            if( mInputView != null && isInputViewShown() )
            {
            	mInputView.invalidateAllKeys();
            }
        } 
        else if (currentKeyboard == mSymbolsShiftedKeyboard) 
        {
        	mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
            mCurKeyboard = mSymbolsKeyboard;
            mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
            if( mInputView != null && isInputViewShown() )
            {
            	mInputView.invalidateAllKeys();
            }

        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            if( SHIFT_IS_STICKY == false )
            {
            	updateShiftKeyState(getCurrentInputEditorInfo());
            }
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
    }

    private void handleSymbol(int primaryCode, int[] keyCodes) 
    {       
        mComposing.append((char) primaryCode);
        getCurrentInputConnection().setComposingText(mComposing, 1);      
        updateCandidates();
    }

    
    private void handleClose() {
        commitTyped(getCurrentInputConnection());        
        requestHideSelf(0);
        resetKey( mKeyIndex );
        mKeyIndex = 0;
        mInputView.closing();       
        
    }
    
    private void handleKeyboardModeChange()
    {
		//clear the pressed status of previous keyboard
		mCurKeyboard.getKeys().get(mKeyIndex).pressed = false;
		
        Keyboard current = mInputView.getKeyboard();
        if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
            current = mQwertyKeyboard;
        } else {
            current = mSymbolsKeyboard;
        }
        mInputView.setKeyboard(current);
        if (current == mSymbolsKeyboard) {
            current.setShifted(false);
        }
        mCurKeyboard = (LimoKeyboard)current;
        
        //set the pressed status of newly set keyboard
        mCurKeyboard.getKeys().get(mKeyIndex).pressed = true;
        mInputView.invalidateAllKeys();

    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            if( SHIFT_IS_STICKY == false )
            {
            	updateShiftKeyState(getCurrentInputEditorInfo());
            }

        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }
    
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }
    
    private void resetKey( int keyIndex )
    {
    	if( mInputView != null  && mCurKeyboard != null )
    	{
        	mCurKeyboard.getKeys().get(keyIndex).pressed = false;
        	mInputView.invalidateAllKeys();
    		
    	}
    }
    
    /**
     * 
     * @param keyIndex
     * return the rowIndex 
     */
    private int getRowIndex( int keyIndex )
    {
    	for( int i = 0; i < NUM_ROWS; ++i )
    	{
    		int startIdx = 0;
    		for( int j = 0; j < i; ++j )
    		{
    			startIdx += KeyNumPerRow[j];
    		}
    		
    		if( keyIndex >= startIdx && keyIndex < startIdx + KeyNumPerRow[i] )
    			return i;
    	}
    	
    	//should never reach here
    	return 3;
    }
    
    private int moveUpRow( int keyIndex, int currRow )
    {
		int startIdx = 0;
		for( int j = 0; j < currRow; ++j )
		{
			startIdx += KeyNumPerRow[j];
		}
		int offset = keyIndex - startIdx;
		
		int upRow = currRow - 1;		
		int lastStartIdx = 0;
		for( int j = 0; j < upRow; ++j )
		{
			lastStartIdx += KeyNumPerRow[j];
		}
		
		lastStartIdx += ( offset < KeyNumPerRow[upRow] ? offset : KeyNumPerRow[upRow] - 1 );
		
		return lastStartIdx;
    }
    
    private int moveDownRow( int keyIndex, int currRow )
    {
		int startIdx = 0;
		for( int j = 0; j < currRow; ++j )
		{
			startIdx += KeyNumPerRow[j];
		}
		int offset = keyIndex - startIdx;
		
		int downRow = currRow + 1;		
		int nextStartIdx = 0;
		for( int j = 0; j < downRow; ++j )
		{
			nextStartIdx += KeyNumPerRow[j];
		}
		
		nextStartIdx += ( offset < KeyNumPerRow[downRow] ? offset : KeyNumPerRow[downRow] - 1 );
		
		return nextStartIdx;
    }

}
