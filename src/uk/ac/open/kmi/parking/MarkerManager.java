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
//    private Label label;

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
                // if we don't have a marker for it, create one, save it
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
                this.outline = this.map.addPolyline(new PolylineOptions().addAll(currentOutline).color(0xffc763ad).width(1f));
//                this.label = new Label(currentOutline, this.map, carparks.size());
            } else {
                this.outline.setPoints(currentOutline);
//                this.label.move(currentOutline, carparks.size());
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

//    private static class Label {
//        private static final String STRING_MAX = "MAX CARPARKS";
//        private static final String STRING_NUM = " CARPARKS";
//        private final Polyline characters[] = new Polyline[15];
//        private final PolylineOptions opts = new PolylineOptions();
//
//        public Label(List<LatLng> currentOutline, GoogleMap map, int count) {
//            this.opts.visible(false).color(0xffc763ad).width(1f).add(new LatLng(0f,0f));
//            for (int i=0; i<this.characters.length; i++) {
//                this.characters[i] = map.addPolyline(this.opts);
//            }
//            this.move(currentOutline, count);
//        }
//
//        public void move(List<LatLng> currentOutline, int count) {
//            double latmin = Math.min(currentOutline.get(0).latitude, currentOutline.get(2).latitude);
//            double lonmin = Math.min(currentOutline.get(0).longitude, currentOutline.get(2).longitude);
//            double lonmax = Math.max(currentOutline.get(0).longitude, currentOutline.get(2).longitude);
//
//            double scaleLon = (lonmax-lonmin)/72d;
//            double scaleLat = scaleLon * Math.cos(latmin);
//
//            String result = STRING_MAX;
//            if (count < ParkingsService.MAX_CARPARKS_DISPLAYED) {
//                result = Integer.toString(count) + STRING_NUM;
//            }
//
//            double originLat = latmin - 5d*scaleLat;
//            double originLon = (lonmax+lonmin)/2d+1.5d*scaleLat*result.length();
//
////            Log.d(TAG, String.format("latmin: %f  lonmin: %f  lonmax: %f  scaleLon: %f  scaleLat: %f  originLat: %f  originLon: %f  string '%s'", latmin, lonmin, lonmax, scaleLon, scaleLat, originLat, originLon, result));
//
//            final int resultEnd = result.length() - 1;
//            for (int i=0; i<this.characters.length; i++) {
//                if (i>resultEnd) {
//                    this.characters[i].setVisible(false);
//                    Log.d(TAG, "no character " + i);
//                } else {
//                    originLon -= 3d*scaleLon;
//                    List<LatLng> charPoints = Character.getChar(result.charAt(resultEnd-i), originLat, originLon, scaleLat, scaleLon);
//                    if (charPoints != null) {
//                        this.characters[i].setPoints(charPoints);
//                        this.characters[i].setVisible(true);
//                        Log.d(TAG, "showing character '" + result.charAt(resultEnd-i) + "' at originLon " + originLon);
//                    } else {
//                        this.characters[i].setVisible(false);
//                        Log.d(TAG, "hiding character " + i);
//                    }
//                }
//            }
//
//        }
//
//    }
//
//    private static abstract class Character {
//
//        private static LatLng n(double lat, double lon, double scaleLat, double scaleLon, int x, int y) {
//            return new LatLng(lat+y*scaleLat, lon+x*scaleLon);
//        }
//
//        private static List<LatLng> character_0(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 0, 1));
//            return l;
//        }
//
//        private static List<LatLng> character_1(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 2, 0));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 3));
//            return l;
//        }
//
//        private static List<LatLng> character_2(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 2, 0));
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 3));
//            return l;
//        }
//
//        private static List<LatLng> character_3(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 3));
//            return l;
//        }
//
//        private static List<LatLng> character_4(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 2, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_5(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 2, 4));
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 1, 3));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 0, 1));
//            return l;
//        }
//
//        private static List<LatLng> character_6(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 2, 4));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 1, 3));
//            l.add(n(la, lo, sa, so, 0, 2));
//            return l;
//        }
//
//        private static List<LatLng> character_7(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 2, 4));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 1, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_8(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 0, 1));
//            return l;
//        }
//
//        private static List<LatLng> character_9(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 1, 1));
//            l.add(n(la, lo, sa, so, 2, 2));
//            return l;
//        }
//
//        private static List<LatLng> character_A(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 2, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_C(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 2, 0));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 4));
//            return l;
//        }
//
//        private static List<LatLng> character_K(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 2, 4));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 2, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_M(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 2, 4));
//            l.add(n(la, lo, sa, so, 2, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_P(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 0, 2));
//            return l;
//        }
//
//        private static List<LatLng> character_R(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 2, 0));
//            return l;
//        }
//
//        private static List<LatLng> character_S(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 1));
//            l.add(n(la, lo, sa, so, 1, 0));
//            l.add(n(la, lo, sa, so, 2, 1));
//            l.add(n(la, lo, sa, so, 2, 2));
//            l.add(n(la, lo, sa, so, 0, 2));
//            l.add(n(la, lo, sa, so, 0, 3));
//            l.add(n(la, lo, sa, so, 1, 4));
//            l.add(n(la, lo, sa, so, 2, 3));
//            return l;
//        }
//
//        private static List<LatLng> character_X(double la, double lo, double sa, double so) {
//            List<LatLng> l = new ArrayList<LatLng>(9);
//            l.add(n(la, lo, sa, so, 0, 4));
//            l.add(n(la, lo, sa, so, 2, 0));
//            l.add(n(la, lo, sa, so, 1, 2));
//            l.add(n(la, lo, sa, so, 0, 0));
//            l.add(n(la, lo, sa, so, 2, 4));
//            return l;
//        }
//
//        static List<LatLng> getChar(char c, double lat, double lon, double scaleLat, double scaleLon) {
//            switch (c) {
//            case '0': return character_0(lat, lon, scaleLat, scaleLon);
//            case '1': return character_1(lat, lon, scaleLat, scaleLon);
//            case '2': return character_2(lat, lon, scaleLat, scaleLon);
//            case '3': return character_3(lat, lon, scaleLat, scaleLon);
//            case '4': return character_4(lat, lon, scaleLat, scaleLon);
//            case '5': return character_5(lat, lon, scaleLat, scaleLon);
//            case '6': return character_6(lat, lon, scaleLat, scaleLon);
//            case '7': return character_7(lat, lon, scaleLat, scaleLon);
//            case '8': return character_8(lat, lon, scaleLat, scaleLon);
//            case '9': return character_9(lat, lon, scaleLat, scaleLon);
//            case 'A': return character_A(lat, lon, scaleLat, scaleLon);
//            case 'C': return character_C(lat, lon, scaleLat, scaleLon);
//            case 'K': return character_K(lat, lon, scaleLat, scaleLon);
//            case 'M': return character_M(lat, lon, scaleLat, scaleLon);
//            case 'P': return character_P(lat, lon, scaleLat, scaleLon);
//            case 'R': return character_R(lat, lon, scaleLat, scaleLon);
//            case 'S': return character_S(lat, lon, scaleLat, scaleLon);
//            case 'X': return character_X(lat, lon, scaleLat, scaleLon);
//            default: return null;
//            }
//        }
//    }


}
