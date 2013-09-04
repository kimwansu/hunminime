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
 */

package kr.ac.ulsan.klplab.app.hunminime;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LatinKeyboardView extends KeyboardView
{

    static final int KEYCODE_OPTIONS = -100;
    static final int KEYCODE_SHIFT_LONGPRESS = -101;

    private Keyboard mPhoneKeyboard;
    
    private Context mContext;
    private int mHangulKeyboardType;

    public LatinKeyboardView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        mHangulKeyboardType = Integer.parseInt(sp.getString(SoftKeyboard.PREF_HANGUL_KBD_TYPE,
                                                            "0"));
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        mHangulKeyboardType = Integer.parseInt(sp.getString(SoftKeyboard.PREF_HANGUL_KBD_TYPE,
                                                            "0"));
    }

    public void setPhoneKeyboard(Keyboard phoneKeyboard)
    {
        mPhoneKeyboard = phoneKeyboard;
    }
    
    public void setKeyboardType(int type)
    {
        mHangulKeyboardType = type;
    }
    
    @Override
    protected boolean onLongPress(Key key)
    {
        if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
            getOnKeyboardActionListener().onKey(KEYCODE_SHIFT_LONGPRESS, null);
            invalidate();
            return true;
        } else if (key.codes[0] == '0' && getKeyboard() == mPhoneKeyboard) {
            // Long pressing on 0 in phone number keypad gives you a '+'.
            getOnKeyboardActionListener().onKey('+', null);
            return true;
        } else {
            int ch = getLongPressedKey(key.codes[0]);
            if (ch > -1) {
                getOnKeyboardActionListener().onKey(ch, null);
                return true;
            } else {
                return super.onLongPress(key);
            }
        }
    }

    /**************************** INSTRUMENTATION *******************************/

    static final boolean DEBUG_AUTO_PLAY = false;
    private static final int MSG_TOUCH_DOWN = 1;
    private static final int MSG_TOUCH_UP = 2;

    Handler mHandler2;

    private String mStringToPlay;
    private int mStringIndex;
    private boolean mDownDelivered;
    private Key[] mAsciiKeys = new Key[256];
    private boolean mPlaying;

    @Override
    public void setKeyboard(Keyboard k)
    {
        super.setKeyboard(k);
        if (DEBUG_AUTO_PLAY) {
            findKeys();
            if (mHandler2 == null) {
                mHandler2 = new Handler() {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        removeMessages(MSG_TOUCH_DOWN);
                        removeMessages(MSG_TOUCH_UP);
                        if (mPlaying == false) {
                            return;
                        }

                        switch (msg.what) {
                        case MSG_TOUCH_DOWN:
                            if (mStringIndex >= mStringToPlay.length()) {
                                mPlaying = false;
                                return;
                            }
                            char c = mStringToPlay.charAt(mStringIndex);
                            while (c > 255 || null == mAsciiKeys[(int) c]) {
                                mStringIndex++;
                                if (mStringToPlay.length() <= mStringIndex) {
                                    mPlaying = false;
                                    return;
                                }
                                c = mStringToPlay.charAt(mStringIndex);
                            }
                            int x = mAsciiKeys[c].x + 10;
                            int y = mAsciiKeys[c].y + 26;
                            MotionEvent me =
                                    MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(),
                                                       MotionEvent.ACTION_DOWN, x, y, 0);
                            LatinKeyboardView.this.dispatchTouchEvent(me);
                            me.recycle();
                            sendEmptyMessageDelayed(MSG_TOUCH_UP, 500);
                            // Deliver up in 500ms if nothing else happens
                            mDownDelivered = true;
                            break;
                        case MSG_TOUCH_UP:
                            char cUp = mStringToPlay.charAt(mStringIndex);
                            int x2 = mAsciiKeys[cUp].x + 10;
                            int y2 = mAsciiKeys[cUp].y + 26;
                            mStringIndex++;

                            MotionEvent me2 =
                                    MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(),
                                                       MotionEvent.ACTION_UP, x2, y2, 0);
                            LatinKeyboardView.this.dispatchTouchEvent(me2);
                            me2.recycle();
                            sendEmptyMessageDelayed(MSG_TOUCH_DOWN, 500);
                            // Deliver up in 500ms if nothing else happens
                            mDownDelivered = false;
                            break;
                        }
                    }
                };

            }
        }
    }

    /*
     * @Override private CharSequence adjustCase(CharSequence label) { return
     * super.adjustCase(label); }
     */
    private void findKeys()
    {
        List<Key> keys = getKeyboard().getKeys();
        // Get the keys on this keyboard
        for (int i = 0; i < keys.size(); i++) {
            int code = keys.get(i).codes[0];
            if (code >= 0 && code <= 255) {
                mAsciiKeys[code] = keys.get(i);
            }
        }
    }

    void startPlaying(String s)
    {
        if (!DEBUG_AUTO_PLAY) {
            return;
        }
        if (s == null) {
            return;
        }
        mStringToPlay = s.toLowerCase();
        mPlaying = true;
        mDownDelivered = false;
        mStringIndex = 0;
        mHandler2.sendEmptyMessageDelayed(MSG_TOUCH_DOWN, 10);
    }

    @Override
    public void draw(Canvas c)
    {
        super.draw(c);
    }
    
    @Override
    public void onDraw(Canvas c)
    {
        super.onDraw(c);
        float scale = getResources().getDisplayMetrics().density;
        
        Paint paintCv = new Paint();
        paintCv.setTextAlign(Paint.Align.CENTER);
        paintCv.setTextSize(16 * scale);
        // setHinting : require API 14
//        paintCv.setHinting(Paint.HINTING_ON);
        paintCv.setAntiAlias(true);
        
        Paint paintLp = new Paint();
        paintLp.setTextAlign(Paint.Align.CENTER);
        paintLp.setTextSize(12 * scale);
//        paintLp.setHinting(Paint.HINTING_ON);
        paintLp.setAntiAlias(true);
        paintLp.setColor(Color.LTGRAY);

        int[] color = { Color.RED, Color.GREEN, Color.YELLOW };
        List<Key> keys = getKeyboard().getKeys();
        for (Key key : keys) {
            if (key.label != null) {
                char keyLabel = key.label.charAt(key.label.length() - 1);
                c.drawText(getLongPressedKeyLabel(keyLabel), key.x + key.width - (8 * scale),
                           key.y + (16 * scale), paintLp);
                
                int type = HangulAutomata.getHangulType(keyLabel) / 3;
                if (type >= 0 && type <= 2 && mHangulKeyboardType == 0) {
                    String label = getConvKeyLabel(keyLabel);
                    paintCv.setColor(color[type]);
                    c.drawText(label, key.x + (key.width / 2), key.y + (48 * scale), paintCv);
                }
            }
        }
    }
    
    private String getConvKeyLabel(char key)
    {
        switch (key) {
        case 0x1100: return "\u110f\u114c";
        case 0x1102: return "\u1103\u1110";
        case 0x1105: return "\u1140";
        case 0x1106: return "\u1107\u1111";
        case 0x1109: return "\u110c\u110e";
        case 0x110b: return "\u1112\u1159";

        case 0x1161: return "\u115f\u1163";
        case 0x1165: return "\u115f\u1167";
        case 0x1169: return "\u115f\u116d";
        case 0x116e: return "\u115f\u1172";

        case 0x11a8: return "\u115f\u1160\u11bf\u115f\u1160\u11f0";
        case 0x11ab: return "\u115f\u1160\u11ae\u115f\u1160\u11c0";
        case 0x11af: return "\u115f\u1160\u11eb";
        case 0x11b7: return "\u115f\u1160\u11b8\u115f\u1160\u11c1";
        case 0x11ba: return "\u115f\u1160\u11bd\u115f\u1160\u11be";
        case 0x11bc: return "\u115f\u1160\u11c2\u115f\u1160\u11f9";

        default: return "";
        }
    }
    
    private String getLongPressedKeyLabel(char key)
    {
        int label = getLongPressedKey(key);
        int type = HangulAutomata.getHangulType(label);
        if (label == -2) {
            return "…";
        } else if (type <= HangulAutomata.HANGUL_CHO3) {
            return Character.toString((char)label);
        } else if (type <= HangulAutomata.HANGUL_JUNG3) {
            return "\u115f" + Character.toString((char)label);
        } else if (type <= HangulAutomata.HANGUL_JONG3) {
            return "\u115f\u1160" + Character.toString((char)label);
        } else {
            return "";
        }
    }
    
    private int getLongPressedKey(int key)
    {
        switch (key) {
        case 'q': return '1'; case 'w': return '2'; case 'e': return '3';
        case 'r': return '4'; case 't': return '5'; case 'y': return '6';
        case 'u': return '7'; case 'i': return '8'; case 'o': return '9';
        case 'p': return '0'; case 'a': return '~'; case 's': return '!';
        case 'd': return '@'; case 'f': return '#'; case 'g': return '^';
        case 'h': return '&'; case 'j': return '('; case 'k': return ')';
        case 'l': return '-'; case 'z': return ';'; case 'x': return ':';
        case 'c': return '/'; case 'v': return '"'; case 'b': return '\'';
        case 'n': return '*'; case 'm': return '+';
        
        case '\u11ba': return mHangulKeyboardType == 0 ? '\u11c1' : -2;
        case '\u11af': return mHangulKeyboardType == 0 ? '\u11c0' : -2;
        case '\u11bc': return mHangulKeyboardType == 0 ? '\u11b6' : -2;
        case '\u11ab': return mHangulKeyboardType == 0 ? '\u11ad' : -2;
        case '\u11b7': return mHangulKeyboardType == 0 ? '\u11be' : -2;
        case '\u11a8': return mHangulKeyboardType == 0 ? '\u11b9' : -2;
        
        case '\u1167': return mHangulKeyboardType == 0 ? -1 : ')';
        case '\u1162': return mHangulKeyboardType == 0 ? -1 : '\u1164';
        case '\u1165': return mHangulKeyboardType == 0 ? '1' : '5';
        case '\u1175': return mHangulKeyboardType == 0 ? '4' : '(';
        case '\u1161': return mHangulKeyboardType == 0 ? '5' : '\u1163';
        case '\u1173': return mHangulKeyboardType == 0 ? '2' : '0';
        case '\u1166': return mHangulKeyboardType == 0 ? -1 : '\u1168';
        case '\u1169': return mHangulKeyboardType == 0 ? '7' : '\u116d';
        case '\u116e': return mHangulKeyboardType == 0 ? '8' : '\u1172';
        
        case '\u1105': return mHangulKeyboardType == 0 ? '0' : '6';
        case '\u1103': return mHangulKeyboardType == 0 ? -1 : '7';
        case '\u1106': return mHangulKeyboardType == 0 ? -2 : '8';
        case '\u110e': return mHangulKeyboardType == 0 ? -1 : '9';
        case '\u1102': return mHangulKeyboardType == 0 ? '3' : '1';
        case '\u110b': return mHangulKeyboardType == 0 ? '6' : '2';
        case '\u1100': return mHangulKeyboardType == 0 ? -2 : '3';
        case '\u110c': return mHangulKeyboardType == 0 ? -2 : '4';
        case '\u1109': return mHangulKeyboardType == 0 ? '9' : '\u1140';
        case '\u1112': return mHangulKeyboardType == 0 ? -1 : '\u1159';
        case '\u110f': return mHangulKeyboardType == 0 ? -1 : '\u114c';
        case '\u1110': return mHangulKeyboardType == 0 ? -1 : '-';
        
        case '\u1107': return mHangulKeyboardType == 0 ? -1 : -2;
        case '\u1111': return mHangulKeyboardType == 0 ? -1 : -2;
        
        default: return -1;
        }
        
    }
}
