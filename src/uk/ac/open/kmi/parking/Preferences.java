/*
   Copyright 2012 Jacek Kopecky (jacek@jacek.cz)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package uk.ac.open.kmi.parking;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * ParkJam preferences
 */
public class Preferences extends PreferenceActivity {
    static final String PREFERENCE_NOSLEEP = "preferenceNoSleep";
    static final String PREFERENCE_FULLSCREEN = "preferenceFullscreen";
    static final String PREFERENCE_SHADOW = "preferenceShadow";
    static final String PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES = "preferenceShowUnconfirmedProperties";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference versionPref = findPreference("preferenceInfoVersion");
        versionPref.setSummary(getVersionName());
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

}
