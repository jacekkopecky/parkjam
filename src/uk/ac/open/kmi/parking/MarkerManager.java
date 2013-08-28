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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.open.kmi.parking.service.ParkingsService;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * this class manages the markers shown on the map, and listens to the events that affect them
 * @author Jacek Kopecky
 *
 */
public class MarkerManager implements OnMarkerClickListener {
//  @SuppressWarnings("unused")
    private static final String TAG = "marker manager";

    private ParkingsService parkingsService;
    private GoogleMap map;
    private Context context;

    private final Map<MapItem, Marker> carpark2marker = new HashMap<MapItem, Marker>();
    private final Map<MapItem, Object> carpark2avail = new HashMap<MapItem, Object>();

    private Polyline outline = null;

    private final Set<MapItem> tmpObsoleteCarparks = new HashSet<MapItem>();

    private final MarkerOptions commonOptions = new MarkerOptions();

    /**
     * constructor
     * @param context activity
     * @param ps instance of parkingsservice
     * @param map instance of map
     */
    public MarkerManager(Context context, ParkingsService ps, GoogleMap map) {
        this.parkingsService = ps;
        this.map = map;
        this.context = context;

        this.commonOptions.anchor(.5f, 1f).draggable(false);
    }

    /**
     * update all the car park markers on the map.
     * must be called from the UI thread; or anyway, from a single tread
     */
    public void update() {
        Log.d(TAG, "update"); final long starttime = System.currentTimeMillis();
        // todo switch sorting thread to simply copy all the car parks and not sort them at all

        // get current sorted car parks - need not be sorted any more
        this.tmpObsoleteCarparks.clear();
        this.tmpObsoleteCarparks.addAll(this.carpark2marker.keySet());
        Collection<MapItem> carparks = this.parkingsService.getSortedCurrentItems();
        List<LatLng> currentOutline = this.parkingsService.getCurrentSortedOutline();

        int added = 0, removed = 0, updated = 0;
        for (MapItem p : carparks) {
            this.tmpObsoleteCarparks.remove(p);
            Marker m = this.carpark2marker.get(p);
            if (m != null) {
                // update the availability (save old availabilities and only change icon on actual change)
                if (updateCarparkMarker(p, m)) {
                    updated++;
                }
            } else {
                // if we don't have a marker for it, create one
                // todo create marker, save it
                BitmapDescriptor bd = p.getBitmapDescriptor();
                this.commonOptions.icon(bd)
                    .position(p.point)
                    .title(p.getTitle())
                    .snippet(ParkingDetailsActivity.getAvailabilityDescription(this.context, (Parking)p, false));
                m = this.map.addMarker(this.commonOptions);
                this.carpark2marker.put(p, m);
                this.carpark2avail.put(p, bd);
                added++;
            }
        }

        if (!currentOutline.isEmpty()) {
            if (this.outline == null) {
                this.outline = this.map.addPolyline(new PolylineOptions().addAll(currentOutline).color(0xffc763ad).width(1.5f));
            } else {
                this.outline.setPoints(currentOutline);
            }
        }

        // for each marker whose car park we don't know any more, remove it
        for (MapItem p: this.tmpObsoleteCarparks) {
            this.carpark2marker.remove(p).remove();
            this.carpark2avail.remove(p);
            removed++;
        }
        this.tmpObsoleteCarparks.clear();
        Log.d(TAG, "update took " + (System.currentTimeMillis() - starttime) + "ms, add/del/upd: " + added + "/" + removed + "/" + updated);
    }

    private boolean updateCarparkMarker(MapItem p, Marker m) {
        BitmapDescriptor bd = p.getBitmapDescriptor();
        if (this.carpark2avail.get(p) != bd) {
            m.setIcon(bd);
            this.carpark2avail.put(p, bd);
            return true;
        }
        return false;
    }

    public boolean onMarkerClick(Marker m) {
        // todo update the title and such data of the marker to the details of the car park of this marker
        return false;
    }

    /**
     * update the marker of this given car park
     * @param p car park that was updated
     */
    public void updateAvailability(Parking p) {
        // update the car park's marker's icon only
        Marker m = this.carpark2marker.get(p);
        if (m != null) {
            updateCarparkMarker(p, m);
        } else {
            Log.e(TAG, "no marker found for car park " + p);
        }
    }
}
