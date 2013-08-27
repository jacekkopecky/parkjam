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

package uk.ac.open.kmi.parking.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import uk.ac.open.kmi.parking.Parking;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * this class takes care of storing car parks that are pinned or added by the user (until indexed by the server).
 * added car parks are not persistently stored at the moment, they should be visible within an hour at most anyway
 */
class RememberedCarparks {

    private static final String TAG = "remembered carparks";

    // if this changes, may need to add migration code for the old version
    private static final String PREFERENCE_PINNED_CARPARKS_COUNT = "preference.main.pinned-carparks-count";
    private static final String PREFERENCE_PINNED_CARPARK_PREFIX = "preference.main.pinned-carpark-";

    private Map<String, ParkingLite> addedCarparks = new HashMap<String, ParkingLite>();
    private List<Uri> pinnedCarparks = new LinkedList<Uri>();

    public RememberedCarparks(Context ctxt) {
        // load from storage; don't hold ctxt; in storage, only hold id and point, not listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        int pinnedCarparkCount = prefs.getInt(PREFERENCE_PINNED_CARPARKS_COUNT, 0);
        for (int i=0; i<pinnedCarparkCount; i++) {
            String pinnedId = prefs.getString(PREFERENCE_PINNED_CARPARK_PREFIX + i, null);
            if (pinnedId == null) {
                Log.e(TAG, "count is " + pinnedCarparkCount + " but value " + i + " not found");
            } else {
                this.pinnedCarparks.add(Uri.parse(pinnedId));
            }
        }

    }

    public void rememberPinnedCarpark(Parking p, Context ctxt) {
        if (!isPinnedCarpark(p)) {
            this.pinnedCarparks.add(0, p.id);
        }
        saveToStorage(ctxt);
    }

    public boolean isPinnedCarpark(Parking p) {
        return this.pinnedCarparks.contains(p.id);
    }

    public void removePinnedCarpark(Parking p, Context ctxt) {
        this.pinnedCarparks.remove(p.id);
        saveToStorage(ctxt);
    }

    private List<Parking> lastKnownPinnedCarparks = null;

    public List<Parking> listKnownPinnedCarparks() {
        List<Parking> retval = new ArrayList<Parking>(this.pinnedCarparks.size());
        for (Uri uri : this.pinnedCarparks) {
            Parking p = Parking.getParking(uri);
            if (p != null) {
                retval.add(p);
            }
        }
        this.lastKnownPinnedCarparks = retval;
        return retval;
    }

    List<Parking> listLastKnownPinnedCarparks() {
        if (this.lastKnownPinnedCarparks == null) {
            return listKnownPinnedCarparks();
        } else {
            return this.lastKnownPinnedCarparks;
        }
    }

//    public boolean hasPinnedCarparks() {
//        return !this.pinnedCarparks.isEmpty();
//    }
//

    public void rememberAddedCarpark(String id, LatLng p, CarparkDetailsUpdateListener l) {
        this.addedCarparks.put(id, new ParkingLite(id, p, l));
    }

    public Collection<ParkingLite> listAddedCarparks() {
        return this.addedCarparks.values();
    }

    public void removeAddedCarpark(ParkingLite p) {
        this.addedCarparks.remove(p.id);
    }

    public void removeAllAddedCarparks(Collection<ParkingLite> parkings) {
        for (ParkingLite p : parkings) {
            this.addedCarparks.remove(p.id);
        }
    }

    public boolean containsAddedCarpark(String id) {
        return this.addedCarparks.containsKey(id);
    }

    public void saveToStorage(Context ctxt) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        Editor editor = prefs.edit();
        int pinnedCarparkCount = this.pinnedCarparks.size();
        editor.putInt(PREFERENCE_PINNED_CARPARKS_COUNT, pinnedCarparkCount);

        for (int i=0; i<pinnedCarparkCount; i++) {
            editor.putString(PREFERENCE_PINNED_CARPARK_PREFIX + i, this.pinnedCarparks.get(i).toString());
        }
        if (!editor.commit()) {
            Log.e(TAG, "error saving pinned carparks");
        }
    }

    public static class ParkingLite {
        public String id;
        public Uri uri;
        public LatLng point;
        public CarparkDetailsUpdateListener listener;

        public ParkingLite(String i, LatLng p, CarparkDetailsUpdateListener l) {
            this.id = i;
            this.uri = Uri.parse(i);
            this.point = p;
            this.listener = l;
        }
    }
}
