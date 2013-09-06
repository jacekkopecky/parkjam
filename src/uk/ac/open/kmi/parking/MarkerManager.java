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
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
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
public class MarkerManager implements OnMarkerClickListener, OnMapClickListener, OnInfoWindowClickListener, InfoWindowAdapter {
    //  @SuppressWarnings("unused")
    private static final String TAG = "marker manager";

    private ParkingsService parkingsService;
    private GoogleMap map;
    private MapFragment mapFragment;
    private MainActivity activity;

    private final Map<Parking, Marker> carpark2marker = new HashMap<Parking, Marker>();
    private final Map<Marker, Parking> marker2carpark = new HashMap<Marker, Parking>();
    private final Map<Parking, Object> carpark2avail = new HashMap<Parking, Object>();

    private Polyline outline = null;
//    private Label label;

    private final Set<Parking> tmpObsoleteCarparks = new HashSet<Parking>();

    private final MarkerOptions commonOptions = new MarkerOptions();

    /**
     * constructor
     * @param context activity
     * @param ps instance of parkingsservice
     * @param map instance of map
     * @param mf map fragment that holds the map
     */
    public MarkerManager(MainActivity context, ParkingsService ps, GoogleMap map, MapFragment mf) {
        this.parkingsService = ps;
        this.map = map;
        this.activity = context;
        this.mapFragment = mf;

        map.setOnMarkerClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnInfoWindowClickListener(this);
        map.setInfoWindowAdapter(this);

        this.commonOptions.anchor(.5f, 1f).draggable(false);
    }

    private Parking currentBubble = null;
    private Parking desiredCarpark = null;
    private View currentBubbleView = null;
    private Marker currentBubbleMarker = null;


    /**
     * shows the info window for this car park (and centers on the car park, possibly only if the window would otherwise not be completely shown)
     * @param p the selected car park
     */
    public void showBubble(Parking p) {
        if (p == null || (this.currentBubble != null && !p.equals(this.currentBubble))) {
            removeBubble();
        }

        this.parkingsService.setCurrentExplicitCarpark(p);
        this.currentBubble = p;

        Marker m = this.carpark2marker.get(p);
        if (m == null) {
            // set desired bubble car park, assume map is already moved there or something else is done so the carpark will soon be loaded
            // on updates, if we encounter the car park, go there
            this.desiredCarpark = p;
        } else {
            this.desiredCarpark = null;
            this.carpark2marker.get(p).showInfoWindow(); // this will also set this.currentBubbleView and this.currentBubbleMarker
        }
    }

    private void ensureInfoWindowOnScreen(Marker m) {
        Projection proj = this.map.getProjection();
        Point p = proj.toScreenLocation(m.getPosition());
        View mapView = this.mapFragment.getView();
        if (mapView == null) {
            Log.w(TAG, "no view in ensureInfoWindow; fragment not initialized?");
            return;
        }

        int maxY = mapView.getHeight();
        int minY = Parking.getDrawablesHeight();

        int width = Parking.getDrawablesWidth()/2;
        if (this.currentBubbleView != null) {
            minY += this.currentBubbleView.getHeight();
            Log.d(TAG, "min top clearance: " + minY);
            width = this.currentBubbleView.getWidth()/2;
        }

        int minX = width;
        int maxX = mapView.getWidth()-width;

        int dx = 0; // p.x - mapView.getWidth()/2;
        int dy = 0; // p.y - minY;
        if (p.x < minX) {
            dx = p.x - minX;
        } else if (p.x > maxX) {
            dx = p.x - maxX;
        }
        if (p.y < minY) {
            dy = p.y - minY;
        } else if (p.y > maxY) {
            dy = p.y - maxY;
        }

        if (dx != 0 || dy != 0) {
            this.map.animateCamera(CameraUpdateFactory.scrollBy(dx, dy), 500, null);
        }
    }

    /**
     * remove the info window and any other artefacts of a car park marker being selected
     * @return true if something was removed
     */
    public boolean removeBubble() {
//        Log.d(TAG, "removing bubble at ", new Exception());
        if (this.currentBubble == null) {
            return false;
        }

        Marker m = this.carpark2marker.get(this.currentBubble);
        this.currentBubble = null;
        this.parkingsService.setCurrentExplicitCarpark(null);

        if (m != null) {
            if (m.isInfoWindowShown()) {
                m.hideInfoWindow();
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "marker not found for car park " + this.currentBubble);
            return false;
        }
    }

    /**
     * returns the currently selected car park
     * @return the currently selected car park
     */
    public Parking getBubbleCarpark() {
        return this.currentBubble;
    }

    /**
     * updates the bubble, if shown for this car park
     * @param p the car park that was updated
     */
    public void updateDetails(Parking p) {
        Log.d(TAG, "updateDetails for parking " + p);
        if (this.currentBubble == null) {
            return;
        }
        if (p == null || this.currentBubble.equals(p)) {
            // update parking instance in case it's stale
            this.currentBubble = Parking.getParking(this.currentBubble.id);
            if (this.currentBubble == null) {
                removeBubble();
                Log.d(TAG, "removed bubble because the parking disappeared");
                return;
            }
            fillBubbleHeading();
            fillBubbleDetails(true);
            this.currentBubbleMarker.showInfoWindow();
            adjustBubbleHeight();

        }
    }

    /**
     * update all the car park markers on the map.
     * must be called from the UI thread; or anyway, from a single tread
     */
    public void update() {
//        Log.d(TAG, "update"); final long starttime = System.currentTimeMillis();

        // get current sorted car parks - need not be sorted any more
        this.tmpObsoleteCarparks.clear();
        this.tmpObsoleteCarparks.addAll(this.carpark2marker.keySet());
        Collection<Parking> carparks = this.parkingsService.getSortedCurrentCarparks();
        List<LatLng> currentOutline = this.parkingsService.getCurrentSortedOutline();

//        int added = 0, removed = 0, updated = 0;
        for (Parking p : carparks) {
            this.tmpObsoleteCarparks.remove(p);
            Marker m = this.carpark2marker.get(p);
            if (m != null) {
                // update the availability (save old availabilities and only change icon on actual change)
                if (updateCarparkMarker(p, m)) {
//                    updated++;
                }
            } else {
                // if we don't have a marker for it, create one, save it
                BitmapDescriptor bd = p.getBitmapDescriptor();
                this.commonOptions.icon(bd)
                    .position(p.point)
                    .title(p.getTitle())
                    .snippet(ParkingDetailsActivity.getAvailabilityDescription(this.activity, p, false));
                m = this.map.addMarker(this.commonOptions);
                this.carpark2marker.put(p, m);
                this.marker2carpark.put(m, p);
                this.carpark2avail.put(p, bd);
//                added++;
            }
            if (p.equals(this.desiredCarpark)) {
                this.desiredCarpark = null;
                final Marker mm = m;
                final Parking pp = p;
                View view = this.mapFragment.getView();
                if (view == null) {
                    Log.w(TAG, "no view in update(); fragment not initialized?");
                    return;
                }
                view.post(new Runnable() {
                    public void run() {
                        showBubble(pp);
                        ensureInfoWindowOnScreen(mm);
                    }
                });
            }
        }

        if (this.tmpObsoleteCarparks.contains(this.currentBubble)) {
            removeBubble();
        }

        // for each marker whose car park we don't know any more, remove it
        for (Parking p: this.tmpObsoleteCarparks) {
            this.carpark2avail.remove(p);
            Marker m = this.carpark2marker.remove(p);
            this.marker2carpark.remove(m);
            m.remove();
//            removed++;
        }
        this.tmpObsoleteCarparks.clear();

        if (!currentOutline.isEmpty()) {
            if (this.outline == null) {
                this.outline = this.map.addPolyline(new PolylineOptions().addAll(currentOutline).color(0xffc763ad).width(1f));
//                this.label = new Label(currentOutline, this.map, carparks.size());
            } else {
                this.outline.setPoints(currentOutline);
//                this.label.move(currentOutline, carparks.size());
            }
        }

        // todo if the bubble is being shown, update that information as well

//        Log.d(TAG, "update took " + (System.currentTimeMillis() - starttime) + "ms, add/del/upd: " + added + "/" + removed + "/" + updated);
    }

    private boolean updateCarparkMarker(Parking p, Marker m) {
        BitmapDescriptor bd = p.getBitmapDescriptor();
        if (this.carpark2avail.get(p) != bd) {
            m.setIcon(bd);
            this.carpark2avail.put(p, bd);
            return true;
        }
        return false;
    }

    public boolean onMarkerClick(Marker m) {
        showBubble(this.marker2carpark.get(m));
        ensureInfoWindowOnScreen(m);
        return true;
    }

    /**
     * update the marker of this given car park
     * @param p car park that was updated
     */
    public void updateAvailability(Parking p) {
        // todo also update availability description in bubble if that is shown
        // update the car park's marker's icon only
        Marker m = this.carpark2marker.get(p);
        if (m != null) {
            updateCarparkMarker(p, m);
//            if (!m.isInfoWindowShown()) {
//                removeBubble();
//            }
        } else {
            Log.e(TAG, "no marker found for car park " + p);
        }

        if (p.equals(this.currentBubble)) {
            this.currentBubble = p; // in case the instance has been updated
            updateDetails(p);
        }
    }

    public void onMapClick(LatLng arg0) {
        removeBubble();
//        Log.i(TAG, "on map click, current markers window showing: " + (this.currentBubble == null ? "none" : this.carpark2marker.get(this.currentBubble).isInfoWindowShown()));
    }

    public void onInfoWindowClick(Marker m) {
        checkMarkerIsCurrentCarpark("click on info window", m);
        this.activity.showDetailsForCarpark(this.currentBubble);
    }

    private Parking checkMarkerIsCurrentCarpark(String where, Marker m) {
        if (this.currentBubble == null) {
            Log.e(TAG, where + ": no current bubble carpark!");
        } else
        if (!m.equals(this.carpark2marker.get(this.currentBubble))) {
            Log.e(TAG, where + ": current bubble carpark: " + this.currentBubble + ", marker is " + m + " but current bubble carpark has marker " + this.carpark2marker.get(this.currentBubble));
        }
        return this.marker2carpark.get(m);
    }

    public View getInfoContents(Marker m) {
        return null;
    }

    public View getInfoWindow(Marker m) {
        checkMarkerIsCurrentCarpark("getInfoWindow", m);
        if (m.equals(this.currentBubbleMarker)) {
            fillBubbleHeading(); // this updates the availability so that the time indication (like 5m ago) stays current-ish
            Log.d(TAG, "returning previous info window");
            return this.currentBubbleView;
        }

        this.currentBubbleMarker = m;
        this.currentBubbleView = this.activity.getLayoutInflater().inflate(R.layout.bubble, null);
        ((TableLayout)this.currentBubbleView.findViewById(R.id.bubble)).setColumnShrinkable(0, true);
        fillBubbleHeading();
        fillBubbleDetails(false);

        // set up resizing and repositioning
        adjustBubbleHeight();

        return this.currentBubbleView;
    }

    private void fillBubbleHeading() {
        if (this.currentBubbleView == null) {
            Log.w(TAG, "no bubble view in fillBubbleHeading");
            return;
        }
        if (this.currentBubble == null) {
            Log.w(TAG, "no bubble in fillBubbleHeading");
            return;
        }
        ((TextView)this.currentBubbleView.findViewById(R.id.bubble_title)).setText(this.currentBubble.getTitle());
        ((TextView)this.currentBubbleView.findViewById(R.id.bubble_availability)).setText(ParkingDetailsActivity.getAvailabilityDescription(this.activity, this.currentBubble, false));
    }

    private void fillBubbleDetails(boolean empty) {
        ViewGroup layout = (ViewGroup)this.currentBubbleView.findViewById(R.id.bubble_details);
        if (empty) {
            layout.removeAllViews();
        }
        ParkingDetailsActivity.createDetailsEntries(this.activity, layout, this.currentBubble, null, true /* todo this.showUnconfirmedProperties */, false);

        // adjusting bubble height needs to be done by the caller of fillBubbleDetails
//        adjustBubbleHeight();
    }

    private void adjustBubbleHeight() {
        final View view = this.mapFragment.getView();
        if (view == null) {
            Log.w(TAG, "no view in adjustBubbleHeight; fragment not initialized?");
            return;
        }
        view.post(new Runnable() {
            public void run() {
                // adjust size
//                Log.d(TAG, "adjusting size");
                View details = MarkerManager.this.currentBubbleView.findViewById(R.id.bubble_details);
                View detailsScrollView = MarkerManager.this.currentBubbleView.findViewById(R.id.bubble_details_scrollview);
                View detailsTopSeparator = MarkerManager.this.currentBubbleView.findViewById(R.id.bubble_top_separator);
                View detailsEllipsis = MarkerManager.this.currentBubbleView.findViewById(R.id.bubble_ellipsis);
                LayoutParams lpS = detailsScrollView.getLayoutParams();
                final int maxDetailsHeight = view.getHeight()/4;
                final int height = details.getHeight();
                if (height == 0) {
                    detailsTopSeparator.setVisibility(View.GONE);
//                    Log.d(TAG, "adjusting size height 0 ");
                } else {
                    detailsTopSeparator.setVisibility(View.VISIBLE);
//                    Log.d(TAG, "adjusting size height non-0");
                }
                int oldHeight = detailsScrollView.getHeight();
//                Log.d(TAG, "old height: " + oldHeight);
                lpS.height = height < maxDetailsHeight ? height : maxDetailsHeight;
                if (lpS.height < height) {
                    detailsEllipsis.setVisibility(View.VISIBLE);
                } else {
                    detailsEllipsis.setVisibility(View.GONE);
                }
//                Log.d(TAG, "adjusting size height " + lpS.height);
                detailsScrollView.setLayoutParams(lpS);
                if (lpS.height != oldHeight) {
                    Log.d(TAG, "scheduling new showInfoWindow");
                    MarkerManager.this.currentBubbleMarker.showInfoWindow();
                    view.post(new Runnable() {
                        public void run() {
                            ensureInfoWindowOnScreen(MarkerManager.this.currentBubbleMarker);
                        }
                    });
                }
            }
        });
    }

    // todo do a bit of this around here:
//  this.currentBubble = Parking.getParking(this.currentBubble.id);
//  if (this.currentBubble == null) {
//      setItem(null);
//      return;
//  }


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
