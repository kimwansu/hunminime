/*
 * Original : stjoo
 * stjoo74@gmail.com
 * 
 * 한글 오토마타 로직 수정 : 2012-2013 울산대학교 김 완수
 * Modified Hangul Automata Logic by Kim Wansu, University of Ulsan, 2012-2013.
 */

package kr.ac.ulsan.klplab.app.hunminime;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.Arrays;

import android.util.Log;


public class HangulAutomata
{
    private static final int NONE = -1; // 한글 아님
    private static final int CHO = 0; // 초성
    private static final int JUNG = 1; // 중성
    private static final int JONG = 2; // 종성
    private static final int CONV = 3; // 변환 키

    public static final int CONV_ADD = 0x3130; // 한글 낱자 획추가 키
    private static final int CONV_A = 0x1161; // 'ㅏ'키
    private static final int CONV_I = 0x1175; // 'ㅣ'키
    private static final int NOT_COMPOSITE = 0x318F; // 조합 안됨 표시

    public static final int HANGUL_NONE = -1; // 한글이 없는 상태
    public static final int HANGUL_CHO1 = 0; // 초성 1개 입력된 상태
    public static final int HANGUL_CHO2 = 1; // 초성 2개 입력된 상태
    public static final int HANGUL_CHO3 = 2; // 초성 3개 입력된 상태
    public static final int HANGUL_JUNG1 = 3; // 중성 1개 입력된 상태
    public static final int HANGUL_JUNG2 = 4; // 중성 2개 입력된 상태
    public static final int HANGUL_JUNG3 = 5; // 중성 3개 입력된 상태
    public static final int HANGUL_JONG1 = 6; // 종성 1개 입력된 상태
    public static final int HANGUL_JONG2 = 7; // 종성 2개 입력된 상태
    public static final int HANGUL_JONG3 = 8; // 종성 3개 입력된 상태
    public static final int HANGUL_SYLLABLE = 9; // 완성된 글자
    
    // 낱자 조합 테이블(1과 2의 조합 결과가 3임)
    private static final int[][] COMPOSITE_TABLE = {
        // 초성
        {0x1100, 0x1100, 0x1101}, {0x1103, 0x1103, 0x1104}, {0x1107, 0x1107, 0x1108},
        {0x1109, 0x1109, 0x110A}, {0x110C, 0x110C, 0x110D}, {0x1102, 0x1100, 0x1113},
        {0x1102, 0x1102, 0x1114}, {0x1102, 0x1103, 0x1115}, {0x1102, 0x1107, 0x1116},
        {0x1103, 0x1100, 0x1117}, {0x1105, 0x1102, 0x1118}, {0x1105, 0x1105, 0x1119},
        {0x1105, 0x1112, 0x111A}, {0x1105, 0x110B, 0x111B}, {0x1106, 0x1107, 0x111C},
        {0x1106, 0x110B, 0x111D}, {0x1107, 0x1100, 0x111E}, {0x1107, 0x1102, 0x111F},
        {0x1107, 0x1103, 0x1120}, {0x1107, 0x1109, 0x1121}, {0x1121, 0x1100, 0x1122},
        {0x1121, 0x1103, 0x1123}, {0x1121, 0x1107, 0x1124}, {0x1121, 0x1109, 0x1125},
        {0x1107, 0x110A, 0x1125}, {0x1121, 0x110C, 0x1126}, {0x1107, 0x110C, 0x1127},
        {0x1107, 0x110E, 0x1128}, {0x1107, 0x1110, 0x1129}, {0x1107, 0x1111, 0x112A},
        {0x1107, 0x110B, 0x112B}, {0x1108, 0x110B, 0x112C}, {0x1109, 0x1100, 0x112D},
        {0x1109, 0x1102, 0x112E}, {0x1109, 0x1103, 0x112F}, {0x1109, 0x1105, 0x1130},
        {0x1109, 0x1106, 0x1131}, {0x1109, 0x1107, 0x1132}, {0x1132, 0x1100, 0x1133},
        {0x110A, 0x1109, 0x1134}, {0x1109, 0x110A, 0x1134}, {0x1109, 0x110B, 0x1135},
        {0x1109, 0x110C, 0x1136}, {0x1109, 0x110E, 0x1137}, {0x1109, 0x110F, 0x1138},
        {0x1109, 0x1110, 0x1139}, {0x1109, 0x1111, 0x113A}, {0x1109, 0x1112, 0x113B},
        {0x113C, 0x113C, 0x113D}, {0x113E, 0x113E, 0x113F}, {0x110B, 0x1100, 0x1141},
        {0x110B, 0x1103, 0x1142}, {0x110B, 0x1106, 0x1143}, {0x110B, 0x1107, 0x1144},
        {0x110B, 0x1109, 0x1145}, {0x110B, 0x1140, 0x1146}, {0x110B, 0x110B, 0x1147},
        {0x110B, 0x110C, 0x1148}, {0x110B, 0x110E, 0x1149}, {0x110B, 0x1110, 0x114A},
        {0x110B, 0x1111, 0x114B}, {0x110C, 0x110B, 0x114D}, {0x114E, 0x114E, 0x114F},
        {0x1150, 0x1150, 0x1151}, {0x110E, 0x110F, 0x1152}, {0x110E, 0x1112, 0x1153},
        {0x1111, 0x1107, 0x1156}, {0x1111, 0x110B, 0x1157}, {0x1112, 0x1112, 0x1158},
        {0x1100, 0x1103, 0x115A}, {0x1102, 0x1109, 0x115B}, {0x1102, 0x110C, 0x115C},
        {0x1102, 0x1112, 0x115D}, {0x1103, 0x1105, 0x115E}, {0x1103, 0x1106, 0xA960},
        {0x1103, 0x1107, 0xA961}, {0x1103, 0x1109, 0xA962}, {0x1103, 0x110C, 0xA963},
        {0x1105, 0x1100, 0xA964}, {0xA964, 0x1100, 0xA965}, {0x1105, 0x1101, 0xA965},
        {0x1105, 0x1103, 0xA966}, {0xA966, 0x1103, 0xA967}, {0x1105, 0x1104, 0xA967},
        {0x1105, 0x1106, 0xA968}, {0x1105, 0x1107, 0xA969}, {0xA969, 0x1107, 0xA96A},
        {0x1105, 0x1108, 0xA96A}, {0xA969, 0x110B, 0xA96B}, {0x1105, 0x1109, 0xA96C},
        {0x1105, 0x110C, 0xA96D}, {0x1105, 0x110F, 0xA96E}, {0x1106, 0x1100, 0xA96F},
        {0x1106, 0x1103, 0xA970}, {0x1106, 0x1109, 0xA971}, {0x1121, 0x1110, 0xA972},
        {0x1107, 0x110F, 0xA973}, {0x1107, 0x1112, 0xA974}, {0x110A, 0x1107, 0xA975},
        {0x110B, 0x1105, 0xA976}, {0x110B, 0x1112, 0xA977}, {0x110D, 0x1112, 0xA978},
        {0x1110, 0x1110, 0xA979}, {0x1111, 0x1112, 0xA97A}, {0x1112, 0x1109, 0xA97B},
        {0x1159, 0x1159, 0xA97C},
        
        // 중성
        {0x1169, 0x1161, 0x116A}, {0x1169, 0x1162, 0x116B}, {0x1169, 0x1175, 0x116C},
        {0x116E, 0x1165, 0x116F}, {0x116E, 0x1166, 0x1170}, {0x116E, 0x1175, 0x1171},
        {0x1173, 0x1175, 0x1174}, {0x1161, 0x1169, 0x1176}, {0x1161, 0x116E, 0x1177},
        {0x1163, 0x1169, 0x1178}, {0x1163, 0x116D, 0x1179}, {0x1165, 0x1169, 0x117A},
        {0x1165, 0x116E, 0x117B}, {0x1165, 0x1173, 0x117C}, {0x1167, 0x1169, 0x117D},
        {0x1167, 0x116E, 0x117E}, {0x1169, 0x1165, 0x117F}, {0x1169, 0x1166, 0x1180},
        {0x1169, 0x1168, 0x1181}, {0x1169, 0x1169, 0x1182}, {0x1169, 0x116E, 0x1183},
        {0x116D, 0x1163, 0x1184}, {0x116D, 0x1164, 0x1185}, {0x116D, 0x1167, 0x1186},
        {0x116D, 0x1169, 0x1187}, {0x116D, 0x1175, 0x1188}, {0x116E, 0x1161, 0x1189},
        {0x116E, 0x1162, 0x118A}, {0x116F, 0x1173, 0x118B}, {0x116E, 0x1168, 0x118C},
        {0x116E, 0x116E, 0x118D}, {0x1172, 0x1161, 0x118E}, {0x1172, 0x1165, 0x118F},
        {0x1172, 0x1166, 0x1190}, {0x1172, 0x1167, 0x1191}, {0x1172, 0x1168, 0x1192},
        {0x1172, 0x116E, 0x1193}, {0x1172, 0x1175, 0x1194}, {0x1173, 0x116E, 0x1195},
        {0x1173, 0x1173, 0x1196}, {0x1174, 0x116E, 0x1197}, {0x1175, 0x1161, 0x1198},
        {0x1175, 0x1163, 0x1199}, {0x1175, 0x1169, 0x119A}, {0x1175, 0x116E, 0x119B},
        {0x1175, 0x1173, 0x119C}, {0x1175, 0x119E, 0x119D}, {0x119E, 0x1165, 0x119F},
        {0x119E, 0x116E, 0x11A0}, {0x119E, 0x1175, 0x11A1}, {0x119E, 0x119E, 0x11A2},
        {0x1161, 0x1173, 0x11A3}, {0x1163, 0x116E, 0x11A4}, {0x1167, 0x1163, 0x11A5},
        {0x1169, 0x1163, 0x11A6}, {0x1169, 0x1164, 0x11A7}, {0x1169, 0x1167, 0xD7B0},
        {0x1182, 0x1175, 0xD7B1}, {0x116D, 0x1161, 0xD7B2}, {0x116D, 0x1162, 0xD7B3},
        {0x116D, 0x1165, 0xD7B4}, {0x116E, 0x1167, 0xD7B5}, {0x1171, 0x1175, 0xD7B6},
        {0x1172, 0x1162, 0xD7B7}, {0x1172, 0x1169, 0xD7B8}, {0x1173, 0x1161, 0xD7B9},
        {0x1173, 0x1165, 0xD7BA}, {0x1173, 0x1166, 0xD7BB}, {0x1173, 0x1169, 0xD7BC},
        {0x1199, 0x1169, 0xD7BD}, {0x1175, 0x1164, 0xD7BE}, {0x1175, 0x1167, 0xD7BF},
        {0x1175, 0x1168, 0xD7C0}, {0x119A, 0x1175, 0xD7C1}, {0x1175, 0x116D, 0xD7C2},
        {0x1175, 0x1172, 0xD7C3}, {0x1175, 0x1175, 0xD7C4}, {0x119E, 0x1161, 0xD7C5},
        {0x119E, 0x1166, 0xD7C6},
        
        // 종성
        {0x11A8, 0x11A8, 0x11A9}, {0x11A8, 0x11BA, 0x11AA}, {0x11AB, 0x11BD, 0x11AC},
        {0x11AB, 0x11C2, 0x11AD}, {0x11AF, 0x11A8, 0x11B0}, {0x11AF, 0x11B7, 0x11B1},
        {0x11AF, 0x11B8, 0x11B2}, {0x11AF, 0x11BA, 0x11B3}, {0x11AF, 0x11C0, 0x11B4},
        {0x11AF, 0x11C1, 0x11B5}, {0x11AF, 0x11C2, 0x11B6}, {0x11B8, 0x11BA, 0x11B9},
        {0x11BA, 0x11BA, 0x11BB}, {0x11A8, 0x11AF, 0x11C3}, {0x11AA, 0x11A8, 0x11C4},
        {0x11AB, 0x11A8, 0x11C5}, {0x11AB, 0x11AE, 0x11C6}, {0x11AB, 0x11BA, 0x11C7},
        {0x11AB, 0x11EB, 0x11C8}, {0x11AB, 0x11C0, 0x11C9}, {0x11AE, 0x11A8, 0x11CA},
        {0x11AE, 0x11AF, 0x11CB}, {0x11B0, 0x11BA, 0x11CC}, {0x11AF, 0x11AA, 0x11CC},
        {0x11AF, 0x11AB, 0x11CD}, {0x11AF, 0x11AE, 0x11CE}, {0x11CE, 0x11C2, 0x11CF},
        {0x11AF, 0x11AF, 0x11D0}, {0x11B1, 0x11A8, 0x11D1}, {0x11B1, 0x11BA, 0x11D2},
        {0x11B2, 0x11BA, 0x11D3}, {0x11B2, 0x11C2, 0x11D4}, {0x11B2, 0x11BC, 0x11D5},
        {0x11B3, 0x11BA, 0x11D6}, {0x11AF, 0x11BB, 0x11D6}, {0x11AF, 0x11EB, 0x11D7},
        {0x11AF, 0x11BF, 0x11D8}, {0x11AF, 0x11F9, 0x11D9}, {0x11B7, 0x11A8, 0x11DA},
        {0x11B7, 0x11AF, 0x11DB}, {0x11B7, 0x11B8, 0x11DC}, {0x11B7, 0x11BA, 0x11DD},
        {0x11DD, 0x11BA, 0x11DE}, {0x11B7, 0x11BB, 0x11DE}, {0x11B7, 0x11EB, 0x11DF},
        {0x11B7, 0x11BE, 0x11E0}, {0x11B7, 0x11C2, 0x11E1}, {0x11B7, 0x11BC, 0x11E2},
        {0x11B8, 0x11AF, 0x11E3}, {0x11B8, 0x11C1, 0x11E4}, {0x11B8, 0x11C2, 0x11E5},
        {0x11B8, 0x11BC, 0x11E6}, {0x11BA, 0x11A8, 0x11E7}, {0x11BA, 0x11AE, 0x11E8},
        {0x11BA, 0x11AF, 0x11E9}, {0x11BA, 0x11B8, 0x11EA}, {0x11F0, 0x11A8, 0x11EC},
        {0x11EC, 0x11A8, 0x11ED}, {0x11F0, 0x11A9, 0x11ED}, {0x11F0, 0x11F0, 0x11EE},
        {0x11F0, 0x11BF, 0x11EF}, {0x11F0, 0x11BA, 0x11F1}, {0x11F0, 0x11EB, 0x11F2},
        {0x11C1, 0x11B8, 0x11F3}, {0x11C1, 0x11BC, 0x11F4}, {0x11C2, 0x11AB, 0x11F5},
        {0x11C2, 0x11AF, 0x11F6}, {0x11C2, 0x11B7, 0x11F7}, {0x11C2, 0x11B8, 0x11F8},
        {0x11A8, 0x11AB, 0x11FA}, {0x11A8, 0x11B8, 0x11FB}, {0x11A8, 0x11BE, 0x11FC},
        {0x11A8, 0x11BF, 0x11FD}, {0x11A8, 0x11F9, 0x11FE}, {0x11AB, 0x11AB, 0x11FF},
        {0x11AB, 0x11AF, 0xD7CB}, {0x11AB, 0x11BE, 0xD7CC}, {0x11AE, 0x11AE, 0xD7CD},
        {0xD7CD, 0x11B8, 0xD7CE}, {0x11AE, 0x11B8, 0xD7CF}, {0x11AE, 0x11BA, 0xD7D0},
        {0xD7D0, 0x11A8, 0xD7D1}, {0x11AE, 0x11BD, 0xD7D2}, {0x11AE, 0x11BE, 0xD7D3},
        {0x11AE, 0x11C0, 0xD7D4}, {0x11B0, 0x11A8, 0xD7D5}, {0x11AF, 0x11A9, 0xD7D5},
        {0x11B0, 0x11C2, 0xD7D6}, {0x11D0, 0x11BF, 0xD7D7}, {0x11B1, 0x11C2, 0xD7D8},
        {0x11B2, 0x11AE, 0xD7D9}, {0x11B2, 0x11C1, 0xD7DA}, {0x11AF, 0x11F0, 0xD7DB},
        {0x11D9, 0x11C2, 0xD7DC}, {0x11AF, 0x11BC, 0xD7DD}, {0x11B7, 0x11AB, 0xD7DE},
        {0xD7DE, 0x11AB, 0xD7DF}, {0x11B7, 0x11B7, 0xD7E0}, {0x11DC, 0x11BA, 0xD7E1},
        {0x11B7, 0x11BD, 0xD7E2}, {0x11B8, 0x11AE, 0xD7E3}, {0x11E3, 0x11C1, 0xD7E4},
        {0x11B8, 0x11B5, 0xD7E4}, {0x11B8, 0x11B7, 0xD7E5}, {0x11B8, 0x11B8, 0xD7E6},
        {0x11B9, 0x11AE, 0xD7E7}, {0x11B8, 0x11BD, 0xD7E8}, {0x11B8, 0x11BE, 0xD7E9},
        {0x11BA, 0x11B7, 0xD7EA}, {0x11EA, 0x11BC, 0xD7EB}, {0x11BB, 0x11A8, 0xD7EC},
        {0x11BB, 0x11AE, 0xD7ED}, {0x11BA, 0x11EB, 0xD7EE}, {0x11BA, 0x11BD, 0xD7EF},
        {0x11BA, 0x11BE, 0xD7F0}, {0x11BA, 0x11C0, 0xD7F1}, {0x11BA, 0x11C2, 0xD7F2},
        {0x11EB, 0x11B8, 0xD7F3}, {0xD7F3, 0x11BC, 0xD7F4}, {0x11F0, 0x11B7, 0xD7F5},
        {0x11F0, 0x11C2, 0xD7F6}, {0x11BD, 0x11B8, 0xD7F7}, {0xD7F7, 0x11B8, 0xD7F8},
        {0x11BD, 0x11BD, 0xD7F9}, {0x11C1, 0x11BA, 0xD7FA}, {0x11C1, 0x11C0, 0xD7FB}
    };
    
    // 획추가키 변환 테이블
    private static final int[][] CONVERT_TABLE = {
        // 초성
        {0x1100, 0x110F}, {0x110F, 0x114C}, {0x114C, 0x1100},
        {0x1102, 0x1103}, {0x1103, 0x1110}, {0x1110, 0x1102},
        {0x1105, 0x1140}, {0x1140, 0x1105},
        {0x1106, 0x1107}, {0x1107, 0x1111}, {0x1111, 0x1106},
        {0x1109, 0x110C}, {0x110C, 0x110E}, {0x110E, 0x1109},
        {0x110B, 0x1112}, {0x1112, 0x1159}, {0x1159, 0x110B},
        
        // 중성
        {0x1161, 0x1163}, {0x1163, 0x1161},
        {0x1162, 0x1164}, {0x1164, 0x1162},
        {0x1165, 0x1167}, {0x1167, 0x1165},
        {0x1166, 0x1168}, {0x1166, 0x1168},
        {0x1169, 0x116D}, {0x116D, 0x1169},
        {0x116E, 0x1172}, {0x1172, 0x116E},
        
        // 종성
        {0x11A8, 0x11BF}, {0x11BF, 0x11F0}, {0x11F0, 0x11A8},
        {0x11AB, 0x11AE}, {0x11AE, 0x11C0}, {0x11C0, 0x11AB},
        {0x11AF, 0x11EB}, {0x11EB, 0x11AF},
        {0x11B7, 0x11B8}, {0x11B8, 0x11C1}, {0x11C1, 0x11B7},
        {0x11BA, 0x11BD}, {0x11BD, 0x11BE}, {0x11BE, 0x11BA},
        {0x11BC, 0x11C2}, {0x11C2, 0x11F9}, {0x11F9, 0x11BC},
    };

    private int mCurrentState = NONE; // 현재 오토마타 상태

    private StringBuilder mStringBuilder = new StringBuilder(); // 문자열 버퍼
    private int mHangulCharBuffer[] = new int[3]; // 한글 조합 버퍼
    private int mComposingBuffer[][] = new int[3][3]; // 한글 낱자 조합 버퍼
    
    private int mHangulKeyboardType;
    private int mHangulAutomataType = 0;
    private int mBackspaceOption = 0;
    private boolean mDeleteByBaseUnit = true;
    private boolean mIsInfiniteComposeUnit = false;
    
    private boolean mDeleteSyllable = false;

    public HangulAutomata()
    {
        reset();
    }

    /**
     * 한글 오토마타 상태 초기화
     */
    public void reset()
    {
        mCurrentState = NONE;
        mStringBuilder = new StringBuilder();
        clear();
    }
    
    public void reset(int keyboardType, int automataType, int backspaceOption,
            boolean isDeleteByBaseUnit, boolean isInfiniteComposeUnit)
    {
        reset();
        mHangulKeyboardType = keyboardType;
        mHangulAutomataType = automataType;
        mBackspaceOption = backspaceOption;
        mDeleteSyllable = mBackspaceOption == SoftKeyboard.BY_SYLLABLE ? true : false;
        mDeleteByBaseUnit = isDeleteByBaseUnit;
        mIsInfiniteComposeUnit = isInfiniteComposeUnit;
    }
    
    /**
     * 한글 오토마타 조합 버퍼 비우기
     */
    public void clear()
    {
        Arrays.fill(mHangulCharBuffer, -1);
        for (int i = 0; i < 3; i++) {
            Arrays.fill(mComposingBuffer[i], -1);
        }
    }
    
    public void setStringBuilder(String s)
    {
        mStringBuilder = new StringBuilder(s);
        revertLastChar();
    }

    /**
     * 입력된 글자 종류, 오토마타 모드에 따라 적절한 처리를 하여 반환한다.
     * @param primaryCode 키 입력
     * @return 한글 조합 버퍼
     */
    public String appendCharacter(int primaryCode)
    {
        int c = categorize(primaryCode);

        if (SoftKeyboard.DEBUG) {
            Log.v(SoftKeyboard.PRJ_NAME, "****************************");
            Log.v(SoftKeyboard.PRJ_NAME, "append state= " + mCurrentState + ", code =\""
                                         + encodingCode(primaryCode) + "\", character=" + c);
        }

        if (c == CONV) {
            if (mCurrentState == NONE || mComposingBuffer[mCurrentState][0] == -1) {
                primaryCode = -1;
            } else {
                int idx = getBufferState(mComposingBuffer[mCurrentState]) - 1;
                int state = mCurrentState;
                mComposingBuffer[state][idx] = getConvertChar(mComposingBuffer[state][idx]);
            }
        } else {
            if (mHangulAutomataType == SoftKeyboard.RAW_UNIT) {
                if (mCurrentState != NONE) {
                    for (int i = 0; i < 3; i++) {
                        if (mComposingBuffer[mCurrentState][i] == -1) {
                            break;
                        }
                        mStringBuilder.append((char)mComposingBuffer[mCurrentState][i]);
                    }
                    Arrays.fill(mComposingBuffer[mCurrentState], -1);
                }
                
                if (isCompositePair(primaryCode)) {
                    mComposingBuffer[c][0] = resolveCompositePair(primaryCode)[0];
                    mComposingBuffer[c][1] = resolveCompositePair(primaryCode)[1];
                } else {
                    mComposingBuffer[c][0] = primaryCode;
                }
                
                mCurrentState = c;
                return mStringBuilder.toString() + generateCompletedChar();
            }
            
            if (c == mCurrentState) {
                // 낱자를 조합한다. 만약 버퍼 길이(3)가 넘어가면 앞부분을 자른다.
                int idx = getBufferState(mComposingBuffer[c]) - 1;
                if (idx < 0) {
                    idx = 0;
                }
                
                if (isCompositeVowel(mComposingBuffer[c][idx]) && primaryCode == CONV_I
                        && mHangulKeyboardType == 0) {
                    mComposingBuffer[c][idx] += 1;
                } else if (mComposingBuffer[c][idx] == CONV_A && primaryCode == CONV_A) {
                    mComposingBuffer[c][idx] = 0x119E;
                } else {
                    int bufferState = getBufferState(mComposingBuffer[c]);
                    if (bufferState + (isCompositePair(primaryCode) ? 2 : 1) > 3) {
                        if (isCompositePair(primaryCode)) {
                            if (!mIsInfiniteComposeUnit) {
                                mStringBuilder.append((char)mComposingBuffer[c][0]);
                                if (bufferState == 3) {
                                    mStringBuilder.append((char)mComposingBuffer[c][1]);
                                }
                            }
                            
                            mComposingBuffer[c][0] = mComposingBuffer[c][bufferState - 1];
                            mComposingBuffer[c][1] = resolveCompositePair(primaryCode)[0];
                            mComposingBuffer[c][2] = resolveCompositePair(primaryCode)[1];
                        } else {
                            if (!mIsInfiniteComposeUnit) {
                                mStringBuilder.append((char)mComposingBuffer[c][0]);
                            }
                            
                            mComposingBuffer[c][0] = mComposingBuffer[c][1];
                            mComposingBuffer[c][1] = mComposingBuffer[c][2];
                            mComposingBuffer[c][2] = primaryCode;
                        }
                    } else {
                        if (isCompositePair(primaryCode)) {
                            mComposingBuffer[c][1] = resolveCompositePair(primaryCode)[0];
                            mComposingBuffer[c][2] = resolveCompositePair(primaryCode)[1];
                        } else {
                            mComposingBuffer[c][bufferState] = primaryCode;
                        }
                    }
                }
            } else {
                // 기존에 조합된 낱자들을 정리해서 문자열 버퍼로 넘기고 새 글자를 삽입한다.
                if ((mHangulAutomataType == SoftKeyboard.AUTO_CORRECTION
                        && mHangulCharBuffer[c] != -1)
                        || (mHangulAutomataType != SoftKeyboard.AUTO_CORRECTION
                        && mCurrentState > c)) {
                    mStringBuilder.append(generateCompletedChar());
                    clear();
                }

                if (isCompositePair(primaryCode)) {
                    mComposingBuffer[c][0] = resolveCompositePair(primaryCode)[0];
                    mComposingBuffer[c][1] = resolveCompositePair(primaryCode)[1];
                } else {
                    mComposingBuffer[c][0] = primaryCode;
                }
                mCurrentState = c;
            }
        }
        
        if (mBackspaceOption == SoftKeyboard.BY_UNIT_ONLY_COMPOSING) {
            mDeleteSyllable = false;
        }
        
        String result = mStringBuilder.toString() + generateCompletedChar();
        if (mHangulAutomataType == SoftKeyboard.SEPARATE) {
            return Normalizer.normalize(result, Normalizer.Form.NFD);
        } else {
            return result;
        }
    }

    /**
     * 일반 모드에서 백스페이스 처리
     * @return 이전 글자
     */
    public String deleteCharacter()
    {
        if (SoftKeyboard.DEBUG) {
            Log.v(SoftKeyboard.PRJ_NAME, "****************************");
            Log.v(SoftKeyboard.PRJ_NAME, "delete state= " + mCurrentState + " ["
                                         + encodingCode(mHangulCharBuffer[0])
                                         + encodingCode(mHangulCharBuffer[1])
                                         + encodingCode(mHangulCharBuffer[2]) + "]");
        }

        int state = mCurrentState;
        if (state == NONE) {
            if (mStringBuilder.length() > 0) {
                state = revertLastChar();
            }
        }
        
        if (state == NONE) {
            return "";
        }
        
        int weight = getBufferState(mComposingBuffer[state]);
        
        if (weight > 0) {
            if (isCompositeVowel(mComposingBuffer[state][weight - 1] - 1)
                    && mHangulKeyboardType == 0
                    && mDeleteByBaseUnit && !mDeleteSyllable) {
                mComposingBuffer[state][weight - 1]--;
            } else if (mComposingBuffer[state][weight - 1] == 0x119E
                    && mDeleteByBaseUnit && !mDeleteSyllable) {
                mComposingBuffer[state][weight - 1] = 0x1161;
            } else {
                if (mDeleteSyllable) {
                    for (int i = 0; i < 3; i++) {
                        Arrays.fill(mComposingBuffer[i], -1);
                    }
                } else {
                    if (mDeleteByBaseUnit) {
                        mComposingBuffer[state][weight - 1] = -1;
                    } else {
                        Arrays.fill(mComposingBuffer[state], -1);
                    }
                    
                    if (mBackspaceOption == SoftKeyboard.BY_UNIT_ONLY_COMPOSING
                            && isEmptyBuffer(mComposingBuffer)) {
                        mDeleteSyllable = true;
                    }
                }
                if (mComposingBuffer[state][0] == -1) {
                    for (int i = 2; i >= 0; i--) {
                        if (mComposingBuffer[i][0] != -1) {
                            mCurrentState = i;
                            break;
                        }
                    }
                }
            }
            
            if (mCurrentState == CHO && mComposingBuffer[0][0] == -1) {
                revertLastChar();
            }
        } else {
            revertLastChar();
        }
        
        return mStringBuilder.toString() + generateCompletedChar();
    }
    
    public String deleteUnit(int category)
    {
        Arrays.fill(mComposingBuffer[category], -1);
        mCurrentState = NONE;
        
        return mStringBuilder.toString() + generateCompletedChar();
    }
    
    /**
     * 문자열 버퍼에서 완성/미완성 한글 한 글자 분량을 반환한다.
     */
    private int revertLastChar()
    {
        int length = mStringBuilder.length();
        if (length == 0) {
            return NONE;
        }
        
        int lastCh = mStringBuilder.charAt(length - 1);

        clear();
        if (getHangulType(lastCh) == HANGUL_SYLLABLE) {
            String last = Character.toString((char)lastCh);
            String nfd = Normalizer.normalize(last, Normalizer.Form.NFD);
            for (int i = 0; i < nfd.length(); i++) {
                int weight = getHangulType(nfd.charAt(i)) % 3 + 1;
                int[] temp = resolveCompositePair(nfd.charAt(i));
                switch (weight) {
                case 1:
                    mComposingBuffer[i][0] = nfd.charAt(i);
                    break;
                case 2:
                    mComposingBuffer[i][0] = temp[0];
                    mComposingBuffer[i][1] = temp[1];
                    break;
                case 3:
                    mComposingBuffer[i][0] = resolveCompositePair(temp[0])[0];
                    mComposingBuffer[i][1] = resolveCompositePair(temp[0])[1];
                    mComposingBuffer[i][2] = temp[1];
                    break;
                }
                mHangulCharBuffer[i] = nfd.charAt(i);
            }
            mStringBuilder.setLength(length - 1);
        } else {
            int newLength = length - 1;
            int state = 9999;
            for (; newLength >= 0; newLength--) {
                int ch = mStringBuilder.charAt(newLength);
                if (getHangulType(ch) == HANGUL_SYLLABLE) {
                    break;
                }
                int type = getHangulType(ch) / 3;
                if (mComposingBuffer[type][0] != -1 || type >= state) {
                    break;
                }
                state = type;
                
                int weight = getHangulType(ch) % 3 + 1;
                int[] temp = resolveCompositePair(ch);
                switch (weight) {
                case 1:
                    mComposingBuffer[type][0] = ch;
                    break;
                case 2:
                    mComposingBuffer[type][0] = temp[0];
                    mComposingBuffer[type][1] = temp[1];
                    break;
                case 3:
                    mComposingBuffer[type][0] = resolveCompositePair(temp[0])[0];
                    mComposingBuffer[type][1] = resolveCompositePair(temp[0])[1];
                    mComposingBuffer[type][2] = temp[1];
                    break;
                }
            }
            mStringBuilder.setLength(newLength + 1);
        }
        
        for (int i = 2; i >= 0; i--) {
            if (mComposingBuffer[i][0] != -1) {
                mCurrentState = i;
                break;
            }
        }
        
        return mCurrentState;
    }

    /**
     * 조합 버퍼의 값을 완성된 글자로 변환한다.
     * @return 완성된 글자
     */
    private String generateCompletedChar()
    {
        boolean isBlank = true;
        
        for (int i = 0; i < 3; i++) {
            mHangulCharBuffer[i] = getCompositePairBuf(mComposingBuffer[i]);
            if (mHangulCharBuffer[i] != -1) {
                isBlank = false;
            }
        }
        
        if (isBlank) {
            mCurrentState = NONE;
            return "";
        }

        if (isSyllable(mHangulCharBuffer)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (mHangulCharBuffer[i] != -1) {
                    sb.append((char) mHangulCharBuffer[i]);
                }
            }
            return Normalizer.normalize(sb.toString(), Normalizer.Form.NFC);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (mHangulCharBuffer[i] != -1) {
                if (mHangulCharBuffer[i] != NOT_COMPOSITE) {
                    result.append((char) mHangulCharBuffer[i]);
                } else {
                    for (int j = 0; j < 3; j++) {
                        if (mComposingBuffer[i][j] != -1) {
                            result.append((char) mComposingBuffer[i][j]);
                        }
                    }
                }
            }
        }
        
        boolean flag = false;
        for (int i = 0; i < 3; i++) {
            if (mHangulCharBuffer[i] == -1) {
                flag = true;
            }
        }
        if (flag) {
            return Normalizer.normalize(result.toString(), Normalizer.Form.NFC);
        } else {
            return result.toString();
        }
    }

    public static String encodingCode(int code)
    {
        String str = null;
        if (code != -1) {
            int[] codes = { code };
            String c = new String(codes, 0, 1);
            try {
                str = new String(c.getBytes(), SoftKeyboard.DEF_CHARSET);
            } catch (UnsupportedEncodingException e) {
                Log.v(SoftKeyboard.PRJ_NAME, "UnsupportedEncodingException");
            }
        } else {
            str = new String(" ");
        }

        return str;
    }

    /**
     * 입력한 글자를 분류한다.
     * @param ch 글자
     * @return 분류 결과
     */
    private static int categorize(int ch)
    {
        if ((ch >= 0x1100 && ch <= 0x115e) || (ch >= 0xa960 && ch <= 0xa97c)) {
            return CHO;
        } else if ((ch >= 0x1161 && ch <= 0x11a7) || (ch >= 0xd7b0 && ch <= 0xd7c6)) {
            return JUNG;
        } else if ((ch >= 0x11a8 && ch <= 0x11ff) || (ch >= 0xd7cb && ch <= 0xd7fb)) {
            return JONG;
        } else if (ch == CONV_ADD) {
            return CONV;
        } else {
            return NONE;
        }
    }

    /**
     * 한글 글자가 어느 종류인지 확인한다.
     * @param ch 한글 낱자
     * @return 한글 낱자의 종류
     */
    public static int getHangulType(int ch)
    {
        int ret = HANGUL_NONE;
        if (ch >= 0x1100 && ch <= 0x1112) {
            if (ch == 0x1101 || ch == 0x1104 || ch == 0x1108 || ch == 0x110a || ch == 0x110d) {
                ret = HANGUL_CHO2;
            } else {
                ret = HANGUL_CHO1;
            }
        } else if (ch >= 0x1113 && ch <= 0x115f) {
            if (ch == 0x113c || ch == 0x113e || ch == 0x1140 || ch == 0x114c || ch == 0x114e
                || ch == 0x1150 || ch == 0x1154 || ch == 0x1155 || ch == 0x1159) {
                ret = HANGUL_CHO1;
            } else if ((ch >= 0x1122 && ch <= 0x1126) || ch == 0x1133 || ch == 0x1134) {
                ret = HANGUL_CHO3;
            } else {
                ret = HANGUL_CHO2;
            }
        } else if (ch >= 0xa960 && ch <= 0xa97c) {
            if (ch == 0xa965 || ch == 0xa967 || ch == 0xa96a || ch == 0xa96b || ch == 0xa972
                || ch == 0xa975 || ch == 0xa978) {
                ret = HANGUL_CHO3;
            } else {
                ret = HANGUL_CHO2;
            }
        } else if (ch >= 0x1160 && ch <= 0x1175) {
            if ((ch >= 0x116a && ch <= 0x116c) || (ch >= 0x116f && ch <= 0x1171) || ch == 0x1174) {
                ret = HANGUL_JUNG2;
            } else {
                ret = HANGUL_JUNG1;
            }
        } else if (ch >= 0x1176 && ch <= 0x11a7) {
            if (ch == 0x118b || ch == 0x1197) {
                ret = HANGUL_JUNG3;
            } else if (ch == 0x119e) {
                ret = HANGUL_JUNG1;
            } else {
                ret = HANGUL_JUNG2;
            }
        } else if (ch >= 0xd7b0 && ch <= 0xd7c6) {
            if (ch == 0xd7b1 || ch == 0xd7b6 || ch == 0xd7bd || ch == 0xd7c1) {
                ret = HANGUL_JUNG3;
            } else {
                ret = HANGUL_JUNG2;
            }
        } else if (ch >= 0x11a8 && ch <= 0x11c2) {
            if (ch == 0x11a9 || ch == 0x11aa || ch == 0x11ac || ch == 0x11ad
                || (ch >= 0x11b0 && ch <= 0x11b6) || ch == 0x11b9 || ch == 0x11bb) {
                ret = HANGUL_JONG2;
            } else {
                ret = HANGUL_JONG1;
            }
        } else if (ch >= 0x11c3 && ch <= 0x11ff) {
            if (ch == 0x11cc || (ch >= 0x11d1 && ch <= 0x11d6) || ch == 0x11de || ch == 0x11ed) {
                ret = HANGUL_JONG3;
            } else if (ch == 0x11eb || ch == 0x11f0 || ch == 0x11f9) {
                ret = HANGUL_JONG1;
            } else {
                ret = HANGUL_JONG2;
            }
        } else if (ch >= 0xd7cb && ch <= 0xd7fb) {
            if (ch == 0xd7ce || ch == 0xd7d1 || (ch >= 0xd7d5 && ch <= 0xd7da) || ch == 0xd7dc
                || ch == 0xd7df || ch == 0xd7e1 || ch == 0xd7e4 || ch == 0xd7e7
                || (ch >= 0xd7eb && ch <= 0xd7ed) || ch == 0xd7f4 || ch == 0xd7f8) {
                ret = HANGUL_JONG3;
            } else {
                ret = HANGUL_JONG2;
            }
        } else if (ch >= 0xac00 && ch <= 0xd7a3) {
            ret = HANGUL_SYLLABLE;
        }

        return ret;
    }

    /**
     * 버퍼에 있는 한글 글자가 현대 한글 범위 안에 있는지 확인한다.
     * @param buf 한글 조합 버퍼
     * @return 현대 한글 범위인지 여부
     */
    private static boolean isSyllable(int[] buf)
    {
        // 한글 조합 버퍼에 있는 낱자들이 현대 한글 범위 안에 있는지 확인한다.
        boolean cho, jung, jong;

        cho = (buf[0] >= 0x1100 && buf[0] <= 0x1112) ? true : false;
        jung = (buf[1] >= 0x1161 && buf[1] <= 0x1175) ? true : false;
        jong = ((buf[2] >= 0x11a8 && buf[2] <= 0x11c2) || buf[2] == -1) ? true : false;

        return cho && jung && jong;
    }

    /**
     * 해당 낱자가 겹낱자인지 확인한다.
     * @param ch 낱자
     * @return 겹낱자인지 여부
     */
    private static boolean isCompositePair(int ch)
    {
        if (getHangulType(ch) == HANGUL_CHO2 || getHangulType(ch) == HANGUL_CHO3
            || getHangulType(ch) == HANGUL_JUNG2 || getHangulType(ch) == HANGUL_JUNG3
            || getHangulType(ch) == HANGUL_JONG2 || getHangulType(ch) == HANGUL_JONG3) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 두 낱자를 조합한다.
     * @param ch1 낱자 1
     * @param ch2 낱자 2
     * @return 조합 결과
     */
    private static int getCompositePair(int ch1, int ch2)
    {
        // 두 초성 글자를 반환한다.(ㄱ + ㄱ → ㄲ)
        // 일반 모드에서 사용.
        for (int[] item : COMPOSITE_TABLE) {
            if (item[0] == ch1 && item[1] == ch2) {
                return item[2];
            }
        }
        return NOT_COMPOSITE;
    }

    /**
     * 조합 버퍼 안에 있는 낱자를 확인하여 조합할 수 있는 경우 조합 결과를 반환한다.
     * @param buf 낱자 조합 버퍼
     * @return 조합 결과
     */
    private static int getCompositePairBuf(int[] buf)
    {
        int count = 0;
        for (int i : buf) {
            count += (i != -1) ? 1 : 0;
        }
        switch (count) {
        case 1:
            return buf[0];
        case 2:
            return getCompositePair(buf[0], buf[1]);
        case 3:
        case 4:
            return getCompositePair(getCompositePair(buf[0], buf[1]), buf[2]);
        default:
            return -1;
        }
    }

    /**
     * 겹낱자를 분해한다.
     * @param ch 겹낱자
     * @return 분해된 낱자
     */
    public static int[] resolveCompositePair(int ch)
    {
        int[] ret = { -1, -1 };
        for (int[] item : COMPOSITE_TABLE) {
            if (item[2] == ch) {
                ret[0] = item[0];
                ret[1] = item[1];
                break;
            }
        }
        return ret;
    }

    /**
     * 현재 조합 버퍼의 글자가 몇 개 있는지 반환한다.
     * 무한 낱자 조합 모드에서 사용한다.
     * @param buf 한글 조합 버퍼
     * @return 조합 버퍼에 들어있는 글자 수
     */
    private static int getBufferState(int[] buf)
    {
        int count = 0;
        for (int i : buf) {
            count += (i != -1) ? 1 : 0;
        }
        return count;
    }
    
    private static boolean isEmptyBuffer(int[][] buf)
    {
        boolean result = true;
        for (int[] i : buf) {
            for (int j : i) {
                if (j != -1) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * 'ㅣ'와 조합 가능한 모음을 조합할 수 있는지 확인한다.
     * @param input 입력되어있는 모음
     * @return 변환 가능 여부
     */
    private static boolean isCompositeVowel(int input)
    {
        switch (input) {
        case 0x1161:
        case 0x1163:
        case 0x1165:
        case 0x1167:
            return true;
        default:
            return false;
        }
    }
    
    /**
     * 현재 입력된 낱자에 획을 추가한 글자로 바꾼다.
     * @param ch 입력된 낱자
     * @return 변환된 낱자
     */
    private static int getConvertChar(int ch)
    {
        for (int[] item : CONVERT_TABLE) {
            if (item[0] == ch) {
                return item[1];
            }
        }
        return ch;
    }
}
