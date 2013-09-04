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
 * 소스 코드 간소화 : 2012-2013 울산대학교 김 완수
 * Downsizing source by Kim Wansu, University of Ulsan, 2012-2013.
 */

package kr.ac.ulsan.klplab.app.hunminime;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

// FIXME : 설정 화면을 PreferenceActivity 대신에 PreferenceFragment로 바꿀 필요가 있음.
public class ImePreferences extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        // TODO Auto-generated method stub
        return true;
    }
}