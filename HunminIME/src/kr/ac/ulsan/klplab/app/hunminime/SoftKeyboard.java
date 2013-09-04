/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * 
 * 소스 코드 간소화 및 한글 오토마타 설정 옵션 추가 : 2012-2013 울산대학교 김 완수
 * Downsizing source and added Hangul automata option by Kim Wansu, University of Ulsan, 2012-2013.
 * 
 * 아래의 문제 해결 이외에 오토마타 수정을 할 때에는 handleCharacter와 handleBackspace 함수를
 * 확인해서 올바르게 작동하지 않으면 수정 필요.
 * 
 * 수정 한 부분
 * ----------------
 * onConfigurationChanged(Configuration conf) : 화면 회전시 키보드가 죽는 문제에 임시 방편으로 대처
 * changeKeyboardMode() : 위의 문제 해결 시 화면 회전시에 키보드 종류가 초기화되는 문제 해결
 * loadSettings() : 설정 항목의 변경으로 인해 수정
 * dump(FileDescriptor fd, PrintWriter fout, String[] args) : 멤버 변수의 추가/삭제로 인해 수정
 * handleBackspace() : 오토마타 수정으로 인해 수정
 * handleCharacter(int primaryCode, int[] keyCodes) : 오토마타 수정으로 인해 수정
 * ※ 오토마타 수정으로 인해 항상 중간 상태(mPredicting)를 가지도록 수정하였음. 원래 상태일 경우에는
 *    간혹 한 글자를 입력할 때 마다 중간 상태의 글자가 중복 삽입되는 문제가 있음.
 * 
 * 수정이 필요한 곳
 * ----------------
 * onConfigurationChanged(Configuration conf)
 * : 화면 회전시 키보드가 죽는 문제를 임시 방편으로 대처했음. 제대로 고칠 수 있는 경우 수정 필요.
 * 
 * mCurrentKeyboard 멤버 변수를 사용하는 곳
 * : 위의 문제를 처리할 때 화면 회전 시 키보드 종류가 초기화되는 것을 보정할 때 사용. 위의 문제를
 *   해결하면 삭제하면 됨.
 * 
 */

package kr.ac.ulsan.klplab.app.hunminime;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Debug;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class SoftKeyboard extends InputMethodService implements
                                                    KeyboardView.OnKeyboardActionListener
{
    public static final boolean DEBUG = false;
    static final boolean TRACE = false;
    public static final String PRJ_NAME = "HunminIME";
    public static final String DEF_CHARSET = "UTF-8";

    private static final String PREF_VIBRATE_ON = "vibrate_on";
    private static final String PREF_SOUND_ON = "sound_on";
    public static final String PREF_HANGUL_KBD_TYPE = "hangul_keyboard_type";
    
    private static final String PREF_AUTOMATA_TYPE = "hangul_automata_type";
    public static final int AUTO_CORRECTION = 0;
    public static final int TYPICAL = 1;
    public static final int SEPARATE = 2;
    public static final int RAW_UNIT = 3;
    
    private static final String PREF_BACKSPACE_OPTION = "backspace_option";
    public static final int ALWAYS_BY_UNIT = 0;
    public static final int BY_UNIT_ONLY_COMPOSING = 1;
    public static final int BY_SYLLABLE = 2;
    
    private static final String PREF_DELETE_BY_BASE_UNIT = "delete_by_base_unit";
    private static final String PREF_INFINITE_COMPOSE_UNIT = "infinite_compose_unit";

    // How many continuous deletes at which to start deleting at a higher speed.
    private static final int DELETE_ACCELERATE_AT = 20;
    // Key events coming any faster than this are long-presses.
    private static final int QUICK_PRESS = 200;

    private static final int KEYCODE_ENTER = 10;
    private static final int KEYCODE_SPACE = ' ';
    private static final int KEYCODE_ARROW_LEFT = -20;
    private static final int KEYCODE_ARROW_RIGHT = -21;
    private static final int KEYCODE_ARROW_UP = -22;
    private static final int KEYCODE_ARROW_DOWN = -23;

    // Contextual menu positions
    private static final int POS_SETTINGS = 0;
    private static final int POS_METHOD = 1;

    private LatinKeyboardView mInputView;
    private List<LatinKeyboardView> mInputViewList;

    private AlertDialog mOptionsDialog;
    KeyboardSwitcher mKeyboardSwitcher;
    private String mLocale;

    private StringBuilder mComposing = new StringBuilder();

    private boolean mPredicting;
    private boolean mPredictionOn;
    private boolean mCapsLock;
    private boolean mVibrateOn;
    private boolean mSoundOn;
    private int mHangulKeyboardType;
    private int mHangulAutomataType;
    private int mBackspaceOption;
    private boolean mDeleteByBaseUnit;
    private boolean mInfiniteComposeUnit;

    // Indicates whether the suggestion strip is to be on in landscape
    private int mDeleteCount;
    private long mLastKeyTime;

    private Vibrator mVibrator;
    private long mVibrateDuration;

    private AudioManager mAudioManager;
    private final float FX_VOLUME = 1.0f;
    private boolean mSilentMode;

    private String mWordSeparators;
    private String mSentenceSeparators;

    HangulAutomata mHangulAutomata = new HangulAutomata();

    // FIXME : onConfigurationChanged()가 올바르게 작동하지 않는 것을 보정하는 임시 방편용 변수
    // 현재 키보드 상태를 저장한다.
    private int mCurrentKeyboard;

    @Override
    public void onCreate()
    {
        super.onCreate();
        // setStatusIcon(R.drawable.ime_qwerty);
        mKeyboardSwitcher = new KeyboardSwitcher(this);
        initSuggest(getResources().getConfiguration().locale.toString());

        mVibrateDuration = getResources().getInteger(R.integer.vibrate_duration_ms);

        // register to receive ringer mode changes for silent mode
        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);

        mInputViewList = new ArrayList<LatinKeyboardView>(2);
    }

    private void initSuggest(String locale)
    {
        mLocale = locale;

        mWordSeparators = getResources().getString(R.string.word_separators);
        mSentenceSeparators = getResources().getString(R.string.sentence_separators);
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(mReceiver);
        mInputViewList.clear();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration conf)
    {
        if (!TextUtils.equals(conf.locale.toString(), mLocale)) {
            initSuggest(conf.locale.toString());
        }
        // FIXME : super.onConfigurationChanged(conf)를 넣어야 하지만 화면 회전시 키보드가 죽음.
        // 해결되면 mCurrentKeyboard는 지워도 됨.
        // super.onConfigurationChanged(conf);
        onCreateInputView();
    }

    private void loadSettings()
    {
        // 설정값을 불러옮.
        // Get the settings preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mVibrateOn = sp.getBoolean(PREF_VIBRATE_ON, true);
        mSoundOn = sp.getBoolean(PREF_SOUND_ON, true);
        mHangulKeyboardType = Integer.parseInt(sp.getString(PREF_HANGUL_KBD_TYPE, "0"));
        mInputView.setKeyboardType(mHangulKeyboardType);
        mHangulAutomataType = Integer.parseInt(sp.getString(PREF_AUTOMATA_TYPE, "0"));
        mInfiniteComposeUnit = sp.getBoolean(PREF_INFINITE_COMPOSE_UNIT, false);
        mBackspaceOption = Integer.parseInt(sp.getString(PREF_BACKSPACE_OPTION, "0"));
        mDeleteByBaseUnit = sp.getBoolean(PREF_DELETE_BY_BASE_UNIT, true);
    }

    @Override
    public View onCreateInputView()
    {
        LatinKeyboardView view;
        view = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
        view.setOnKeyboardActionListener(this);
        mInputViewList.add(view);

        mInputView = mInputViewList.get(0);
        mKeyboardSwitcher.setInputView(mInputView);
        mKeyboardSwitcher.makeKeyboards(mHangulKeyboardType);

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(mComposing, 1);
        }
        mComposing.setLength(0);
        mHangulAutomata.reset();

        // mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT_HANGUL, 0);
        // FIXME : 키보드가 죽는 문제를 해결하면 주석처리한 걸로 바꾸면 됨.
        if (mKeyboardSwitcher.getKeyboardMode() == 0) {
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT_HANGUL, 0);
            mCurrentKeyboard = KeyboardSwitcher.MODE_TEXT_HANGUL;
//            mInputView.setHangulMode(true);
        } else {
            mKeyboardSwitcher.setKeyboardMode(mCurrentKeyboard, 0);
//            mInputView.setHangulMode(mCurrentKeyboard == KeyboardSwitcher.MODE_TEXT_HANGUL);
        }
        return mInputView;
    }
    
    @Override
    public void onUpdateExtractingVisibility(EditorInfo ei)
    {
        ei.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        super.onUpdateExtractingVisibility(ei);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting)
    {
        // In landscape mode, this method gets called without the input view
        // being created.
        if (mInputView == null) {
            return;
        }
        mInputView.closing();

        loadSettings();
        mKeyboardSwitcher.makeKeyboards(mHangulKeyboardType);
        mHangulAutomata.reset(mHangulKeyboardType, mHangulAutomataType,
                mBackspaceOption, mDeleteByBaseUnit, mInfiniteComposeUnit);

        mPredictionOn = false;
        mCapsLock = false;

        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
            mKeyboardSwitcher
                    .setKeyboardMode(KeyboardSwitcher.MODE_TEXT_ENG, attribute.imeOptions);
            mCurrentKeyboard = KeyboardSwitcher.MODE_TEXT_ENG;
            mKeyboardSwitcher.toggleSymbols();
            break;
        case EditorInfo.TYPE_CLASS_PHONE:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_PHONE, attribute.imeOptions);
            mCurrentKeyboard = KeyboardSwitcher.MODE_PHONE;
            break;
        case EditorInfo.TYPE_CLASS_TEXT:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT_HANGUL,
                                              attribute.imeOptions);
            mCurrentKeyboard = KeyboardSwitcher.MODE_TEXT_HANGUL;
            mPredictionOn = true;
            // Make sure that passwords are not displayed in candidate view
            int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
            if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT_ENG,
                                                  attribute.imeOptions);
                mCurrentKeyboard = KeyboardSwitcher.MODE_TEXT_ENG;
                mPredictionOn = false;
            }
            if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_EMAIL,
                                                  attribute.imeOptions);
                mCurrentKeyboard = KeyboardSwitcher.MODE_EMAIL;
            } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                mPredictionOn = true;
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_URL, attribute.imeOptions);
                mCurrentKeyboard = KeyboardSwitcher.MODE_URL;
            } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_IM, attribute.imeOptions);
                mCurrentKeyboard = KeyboardSwitcher.MODE_IM;
            } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                ;
            }
            updateShiftKeyState(attribute);
            break;
        default:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT_HANGUL,
                                              attribute.imeOptions);
            mCurrentKeyboard = KeyboardSwitcher.MODE_TEXT_HANGUL;
            updateShiftKeyState(attribute);
        }

        mComposing.setLength(0);
        mPredicting = true;
        mDeleteCount = 0;
        setCandidatesViewShown(false);
        mInputView.setProximityCorrectionEnabled(true);
        if (TRACE) {
            Debug.startMethodTracing("latinime");
        }
    }

    @Override
    public void onFinishInput()
    {
        super.onFinishInput();

        mComposing.setLength(0);
        mHangulAutomata.reset();

        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override
    public void hideWindow()
    {
        if (TRACE) {
            Debug.stopMethodTracing();
        }
        if (mOptionsDialog != null && mOptionsDialog.isShowing()) {
            mOptionsDialog.dismiss();
            mOptionsDialog = null;
        }

        super.hideWindow();
        mHangulAutomata.reset();
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets)
    {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (event.getRepeatCount() == 0 && mInputView != null) {
                if (mInputView.handleBack()) {
                    return true;
                }
            }
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            // Enable shift key and DPAD to do selections
            if (mInputView != null && mInputView.isShown() && mInputView.isShifted()) {
                event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
                                     event.getKeyCode(), event.getRepeatCount(),
                                     event.getDeviceId(), event.getScanCode(),
                                     KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.sendKeyEvent(event);
                }
                return true;
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void commitTyped(InputConnection inputConnection)
    {
        // 문자 조합이 끝났을 때 호출
        if (mPredicting) {
            if (mComposing.length() > 0) {
                if (inputConnection != null) {
                    inputConnection.commitText(mComposing, 1);
                }
            }
        }
    }

    private boolean isHangulMode()
    {
        if (mKeyboardSwitcher == null) {
            return false;
        }
        return mKeyboardSwitcher.isHangulMode();
    }

    public void updateShiftKeyState(EditorInfo attr)
    {
        InputConnection ic = getCurrentInputConnection();
        if (attr != null && mInputView != null && mKeyboardSwitcher.isAlphabetMode() && ic != null) {
            int caps = 0;
            if (isHangulMode()) {
                mInputView.setShifted(mCapsLock);
            } else {
                mInputView.setShifted(mCapsLock || caps != 0);
            }
        }
    }

    private boolean isAlphabet(int code)
    {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isHangul(int code)
    {
        if (code == 0x3130) {
            return true;
        } else {
            return HangulAutomata.getHangulType(code) == -1 ? false : true;
        }
    }

    // Implementation of KeyboardViewListener
    public void onKey(int primaryCode, int[] keyCodes)
    {
        long when = SystemClock.uptimeMillis();
        if (primaryCode != Keyboard.KEYCODE_DELETE || when > mLastKeyTime + QUICK_PRESS) {
            mDeleteCount = 0;
        }
        mLastKeyTime = when;
        InputConnection ic = getCurrentInputConnection();
        switch (primaryCode) {
        case Keyboard.KEYCODE_DELETE:
            handleBackspace();
            mDeleteCount++;
            break;
        case Keyboard.KEYCODE_SHIFT:
            handleShift();
            break;
        case Keyboard.KEYCODE_CANCEL:
            if (mOptionsDialog == null || !mOptionsDialog.isShowing()) {
                handleClose();
            }
            break;
        case LatinKeyboardView.KEYCODE_OPTIONS:
            showOptionsMenu();
            break;
        case LatinKeyboardView.KEYCODE_SHIFT_LONGPRESS:
            if (mCapsLock) {
                handleShift();
            } else {
                toggleCapsLock();
            }
            break;
        case Keyboard.KEYCODE_MODE_CHANGE:
            changeKeyboardMode();
            break;
        case KEYCODE_ARROW_LEFT:
            ic.commitText(mComposing, 1);
            mComposing.setLength(0);
            mHangulAutomata.reset();
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
            break;
        case KEYCODE_ARROW_RIGHT:
            ic.commitText(mComposing, 1);
            mComposing.setLength(0);
            mHangulAutomata.reset();
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
            break;
        case KEYCODE_ARROW_UP:
            ic.commitText(mComposing, 1);
            mComposing.setLength(0);
            mHangulAutomata.reset();
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
            break;
        case KEYCODE_ARROW_DOWN:
            ic.commitText(mComposing, 1);
            mComposing.setLength(0);
            mHangulAutomata.reset();
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
            break;
        case 0x115f:
        case 0x1160:
            String result = mHangulAutomata.deleteUnit(primaryCode - 0x115f);
            mComposing.setLength(0);
            mComposing.append(result);
            ic.setComposingText(mComposing, 1);
            break;
        default:
            if (isHangul(primaryCode)) {
                handleCharacter(primaryCode, keyCodes);
            } else {
                mHangulAutomata.reset();
                handleSeparator(primaryCode);
            }
        }
    }

    public void onText(CharSequence text)
    {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        ic.beginBatchEdit();
        if (mPredicting) {
            commitTyped(ic);
        }
        ic.commitText(text, 1);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleSeparator(int primaryCode)
    {
        // Handle separator
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.beginBatchEdit();
        }

        if (mPredicting) {
            // In certain languages where single quote is a separator, it's
            // better
            // not to auto correct, but accept the typed word. For instance,
            // in Italian dov' should not be expanded to dove' because the
            // elision
            // requires the last vowel to be removed.
            commitTyped(ic);

            mComposing.setLength(0);
            mHangulAutomata.reset();
        } else if (isHangulMode() && mComposing.length() > 0) {
            if (ic != null) {
                ic.commitText(mComposing, 1);
            }

            mComposing.setLength(0);
            mHangulAutomata.reset();
        }

        if (mInputView.isShifted()) {
            primaryCode = Character.toUpperCase(primaryCode);
        }

        sendKeyChar((char) primaryCode);

        updateShiftKeyState(getCurrentInputEditorInfo());
        if (ic != null) {
            ic.endBatchEdit();
        }
    }

    private void handleClose()
    {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void handleShift()
    {
        if (mKeyboardSwitcher.isAlphabetMode()) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else {
            mKeyboardSwitcher.toggleShift();
        }
    }

    private void checkToggleCapsLock()
    {
        if (mInputView.getKeyboard().isShifted()) {
            toggleCapsLock();
        }
    }

    private void toggleCapsLock()
    {
        mCapsLock = !mCapsLock;
        if (mKeyboardSwitcher.isAlphabetMode()) {
            ((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
        }
    }

    private boolean isPredictionOn()
    {
        boolean predictionOn = mPredictionOn;
        return predictionOn;
    }

    private boolean isCursorTouchingWord()
    {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return false;
        }
        CharSequence toLeft = ic.getTextBeforeCursor(1, 0);
        CharSequence toRight = ic.getTextAfterCursor(1, 0);
        if (!TextUtils.isEmpty(toLeft) && !isWordSeparator(toLeft.charAt(0))) {
            return true;
        }
        if (!TextUtils.isEmpty(toRight) && !isWordSeparator(toRight.charAt(0))) {
            return true;
        }
        return false;
    }

    protected String getWordSeparators()
    {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code)
    {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char) code));
    }

    public boolean isSentenceSeparator(int code)
    {
        return mSentenceSeparators.contains(String.valueOf((char) code));
    }
    
    public int getmHangulKeyboardType()
    {
        return mHangulKeyboardType;
    }

    public void swipeRight()
    {
        // if (LatinKeyboardView.DEBUG_AUTO_PLAY) {
        // ClipboardManager cm = ((ClipboardManager)
        // getSystemService(CLIPBOARD_SERVICE));
        // CharSequence text = cm.getText();
        // if (!TextUtils.isEmpty(text)) {
        // mInputView.startPlaying(text.toString());
        // }
        // }
    }

    public void swipeLeft()
    {
        // handleBackspace();
    }

    public void swipeDown()
    {
        // handleClose();
    }

    public void swipeUp()
    {
        // launchSettings();
    }

    public void onPress(int primaryCode)
    {
        vibrate();
        playKeyClick(primaryCode);
    }

    public void onRelease(int primaryCode)
    {
        // vibrate();
    }

    // receive ringer mode changes to detect silent mode
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateRingerMode();
        }
    };

    // update flags for silent mode
    private void updateRingerMode()
    {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        } else {
            mSilentMode = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private void playKeyClick(int primaryCode)
    {
        // if mAudioManager is null, we don't have the ringer state yet
        // mAudioManager will be set by updateRingerMode
        if (mAudioManager == null) {
            if (mInputView != null) {
                updateRingerMode();
            }
        }
        if (mSoundOn && !mSilentMode) {
            // FIXME: Volume and enable should come from UI settings
            // FIXME: These should be triggered after auto-repeat logic
            int sound = AudioManager.FX_KEYPRESS_STANDARD;
            switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                sound = AudioManager.FX_KEYPRESS_DELETE;
                break;
            case KEYCODE_ENTER:
                sound = AudioManager.FX_KEYPRESS_RETURN;
                break;
            case KEYCODE_SPACE:
                sound = AudioManager.FX_KEYPRESS_SPACEBAR;
                break;
            }
            mAudioManager.playSoundEffect(sound, FX_VOLUME);
        }
    }

    private void vibrate()
    {
        if (!mVibrateOn) {
            return;
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(mVibrateDuration);
    }

    private void launchSettings()
    {
        handleClose();
        Intent intent = new Intent();
        intent.setClass(SoftKeyboard.this, ImePreferences.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showOptionsMenu()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setIcon(R.drawable.ic_dialog_keyboard);
        builder.setNegativeButton(android.R.string.cancel, null);
        CharSequence itemSettings = getString(R.string.english_ime_settings);
        CharSequence itemInputMethod = getString(R.string.inputMethod);

        builder.setItems(new CharSequence[] { itemSettings, itemInputMethod },
                         new DialogInterface.OnClickListener() {

                             public void onClick(DialogInterface di, int position)
                             {
                                 di.dismiss();
                                 switch (position) {
                                 case POS_SETTINGS:
                                     launchSettings();
                                     break;
                                 case POS_METHOD:
                                     ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                             .showInputMethodPicker();
                                     break;
                                 }
                             }
                         });
        builder.setTitle(getResources().getString(R.string.english_ime_name));
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mInputView.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

    private void changeKeyboardMode()
    {
        // FIXME : 화면 회전 문제 해결 시 'mCurrentKeyboard = ' 부분만 지우면 됨.
        mCurrentKeyboard = mKeyboardSwitcher.toggleSymbols();
        if (mKeyboardSwitcher.isAlphabetMode()) {
            if (mCapsLock) {
                ((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
            }
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args)
    {
        super.dump(fd, fout, args);

        final Printer p = new PrintWriterPrinter(fout);
        p.println("LatinIME state :");
        p.println("  Keyboard mode = " + mKeyboardSwitcher.getKeyboardMode());
        p.println("  mCapsLock=" + mCapsLock);
        p.println("  mComposing=" + mComposing.toString());
        p.println("  mPredictionOn=" + mPredictionOn);
        p.println("  mPredicting=" + mPredicting);
        p.println("  mSoundOn=" + mSoundOn);
        p.println("  mVibrateOn=" + mVibrateOn);
        p.println("  mCurrentKeyboard=" + mCurrentKeyboard);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd)
    {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart,
                                candidatesEnd);
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        final int length = mComposing.length();
        if (length > 0 && (mPredicting || isHangulMode())
            && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            if (!(isHangulMode() && candidatesEnd == -1)) {
                mComposing.setLength(0);
                mHangulAutomata.reset();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.finishComposingText();
                }
            }
        }
    }

    private void printComposing()
    {
        Log.v(PRJ_NAME, "mComposing= \'" + encodingStr(mComposing.toString()) + "\'");
    }

    private String encodingStr(String str)
    {
        String ret = null;
        try {
            ret = new String(str.getBytes(), DEF_CHARSET);
        } catch (UnsupportedEncodingException e) {
            Log.v(PRJ_NAME, "UnsupportedEncodingException");
        }

        return ret;
    }

    private void handleBackspace()
    {
        // 백스페이스 키를 눌렀을 때 호출된다.
        // 오토마타 로직이 변경되면 같이 변경되어야 한다.
        boolean deleteChar = false;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        int length = mComposing.length();

        //이전 단어를 불러온다.
        if (length <= 0) {
            int textLength = 0;
            boolean flag = true;
            String s = new String();
            for (int i = 1; true; i++) {
                s = ic.getTextBeforeCursor(i, 0).toString();
                if (s.length() == i - 1) {
                    textLength = i - 1;
                    break;
                }
                
                if (!isHangul(s.charAt(i - 1))) {
                    length = 0;
                    flag = false;
                    break;
                }
            }
            
            if (flag) {
                int startPos = -1;
                for (int i = textLength - 1; i >= 0; i--) {
                    if (!isHangul(s.charAt(i))) {
                        startPos = i;
                        break;
                    }
                }
                ic.beginBatchEdit();
                ic.setComposingRegion(startPos + 1, textLength);
                s = s.substring(startPos + 1, textLength);
                mHangulAutomata.setStringBuilder(s);
                mComposing.setLength(0);
                mComposing.append(s);
                ic.setComposingText(mComposing, 1);
                ic.endBatchEdit();
                length = mComposing.length();
            }
        }        
        if (length > 0) {
            String result = mHangulAutomata.deleteCharacter();
            mComposing.setLength(0);
            mComposing.append(result);
            ic.setComposingText(mComposing, 1);
        } else {
            deleteChar = true;
        }

        if (DEBUG) {
            printComposing();
        }

        updateShiftKeyState(getCurrentInputEditorInfo());
        if (deleteChar) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            if (mDeleteCount > DELETE_ACCELERATE_AT) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            }
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes)
    {
        // 일반 키를 눌렀을 때 호출된다.
        // 오토마타 로직이 바뀌면 같이 변경되어야 한다.
        if (isAlphabet(primaryCode) && isPredictionOn() && !isCursorTouchingWord()) {
            if (!mPredicting) {
                mPredicting = true;
                mComposing.setLength(0);
                mHangulAutomata.reset();
            }
            mPredicting = true;
        }

        if (mInputView.isShifted()) {
            primaryCode = Character.toUpperCase(primaryCode);
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        if (mPredicting) {
            String ret = mHangulAutomata.appendCharacter(primaryCode);
            if (isHangulMode()) {
                mComposing.setLength(0);
                mComposing.append(ret);
            }
            ic.setComposingText(mComposing, 1);

            if (DEBUG) {
                printComposing();
            }
        }
    }
}
