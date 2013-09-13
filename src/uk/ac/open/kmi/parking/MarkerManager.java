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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
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
public class MarkerManager implements OnMarkerClickListener, OnMapClickListener, OnInfoWindowClickListener, InfoWindowAdapter, OnClickListener {
    //  @SuppressWarnings("unused")
    private static final String TAG = "marker manager";

    private ParkingsService parkingsService;
    private GoogleMap map;
    private MapFragment mapFragment;
    private MainActivity activity;
    private View bubbleButtons;
    private ImageView bubbleButtonAvail;
    private ImageView bubbleButtonFull;
    private ImageView bubbleButtonPin;
    private ImageView bubbleButtonDirections;

    private final Map<Parking, Marker> carpark2marker = new HashMap<Parking, Marker>();
    private final Map<Marker, Parking> marker2carpark = new HashMap<Marker, Parking>();
    private final Map<Parking, Object> carpark2avail = new HashMap<Parking, Object>();

    private Polyline outline = null;
//    private Label label;

    private final Set<Parking> tmpObsoleteCarparks = new HashSet<Parking>();

    private final MarkerOptions commonOptions = new MarkerOptions();

    /**
     * constructor
     * @param activity activity
     * @param ps instance of parkingsservice
     * @param map instance of map
     * @param mf map fragment that holds the map
     */
    public MarkerManager(final MainActivity activity, ParkingsService ps, GoogleMap map, MapFragment mf) {
        this.parkingsService = ps;
        this.map = map;
        this.activity = activity;
        this.mapFragment = mf;
        this.bubbleButtons = activity.findViewById(R.id.bubble_buttons);

        this.bubbleButtonAvail = (ImageView) this.bubbleButtons.findViewById(R.id.bubble_report_available);
        this.bubbleButtonFull = (ImageView) this.bubbleButtons.findViewById(R.id.bubble_report_full);
        this.bubbleButtonPin = (ImageView) this.bubbleButtons.findViewById(R.id.bubble_pin);
        this.bubbleButtonDirections = (ImageView) this.bubbleButtons.findViewById(R.id.bubble_directions);

        this.bubbleButtonAvail.setOnLongClickListener(activity.buttonHintHandler);
        this.bubbleButtonFull.setOnLongClickListener(activity.buttonHintHandler);
        this.bubbleButtonPin.setOnLongClickListener(activity.buttonHintHandler);
        this.bubbleButtonDirections.setOnLongClickListener(activity.buttonHintHandler);

        this.bubbleButtonAvail.setOnClickListener(this);
        this.bubbleButtonFull.setOnClickListener(this);
        this.bubbleButtonPin.setOnClickListener(this);
//        this.bubbleButtonDrawer.setOnClickListener(this);
        this.bubbleButtonDirections.setOnClickListener(this);

        map.setOnMarkerClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnInfoWindowClickListener(this);
        map.setInfoWindowAdapter(this);

        this.commonOptions.anchor(.5f, 1f).draggable(false);
    }

    /**
     * on click of any bubble button handle the action
     * @param v the button
     */
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.bubble_pin:
            this.activity.pinCarpark(this.currentBubble, !this.parkingsService.isPinnedCarpark(this.currentBubble));
            break;
        case R.id.bubble_report_available:
            this.activity.reportAvailability(this.currentBubble, true);
            break;
        case R.id.bubble_report_full:
            this.activity.reportAvailability(this.currentBubble, false);
            break;
        case R.id.bubble_directions:
            this.activity.openNavigationTo(this.currentBubble);
            break;
        default:
            Log.w(TAG, "unknown button pressed: " + v.getId());
        }
    }

    private Parking currentBubble = null;
    private Parking desiredCarpark = null;
    private View currentBubbleView = null;
    private Marker currentBubbleMarker = null;

    private long ensureInfoWindowOnScreenUntil = 0;

    private boolean markersEnabled = true;


    /**
     * shows the info window for this car park (and centers on the car park, possibly only if the window would otherwise not be completely shown)
     * @param p the selected car park
     */
    public void showBubble(Parking p) {
        Log.d(TAG, "show bubble for parking " + p);
        if (p == null || (this.currentBubble != null && !p.equals(this.currentBubble))) {
            removeBubble();
        }

        this.ensureInfoWindowOnScreenUntil = System.currentTimeMillis()+1500; // todo this should be a constant somewhere

        this.parkingsService.setCurrentExplicitCarpark(p);
        this.currentBubble = p;
        updatePinnedStatus();
        this.bubbleButtons.setVisibility(View.VISIBLE);
//        updateButtons(p);

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

    private void doEnsureInfoWindowOnScreen() {
        if (this.ensureInfoWindowOnScreenUntil < System.currentTimeMillis()) {
            return;
        }
        if (this.currentBubbleMarker == null) {
            return; // marker disappeared
        }
        Projection proj = this.map.getProjection();
        Point p = proj.toScreenLocation(this.currentBubbleMarker.getPosition());
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
//            Log.d(TAG, "min top clearance: " + minY);
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
//        this.bubbleButtons.animate().translationYBy(this.bubbleButtons.getHeight()).setListener(new AnimatorListenerAdapter() {
//            public void onAnimationEnd(Animator animation) {
//                MarkerManager.this.bubbleButtons.setVisibility(View.GONE);
//                MarkerManager.this.bubbleButtons.setTranslationY(0);
//            }
//            public void onAnimationCancel(Animator animation) {
//                MarkerManager.this.bubbleButtons.setVisibility(View.GONE);
//                MarkerManager.this.bubbleButtons.setTranslationY(0);
//            }
//        });


        if (this.currentBubble == null) {
            return false;
        }
        this.bubbleButtons.setVisibility(View.INVISIBLE);

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
//        Log.d(TAG, "updateDetails for parking " + p);
        if (this.currentBubble == null) {
            return;
        }
        if (p == null || this.currentBubble.equals(p)) {
            // update parking instance in case it's stale
            this.currentBubble = Parking.getParking(this.currentBubble.id);
            if (this.currentBubble == null) {
                removeBubble();
//                Log.d(TAG, "removed bubble because the parking disappeared");
                return;
            }
            fillBubbleHeading();
            fillBubbleDetails();
            this.currentBubbleMarker.showInfoWindow();
//            updateButtons(p);
        }
    }

    /**
     * update all the car park markers on the map.
     * must be called from the UI thread
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
            m.setVisible(this.markersEnabled);
            if (this.markersEnabled && p.equals(this.desiredCarpark)) {
                this.desiredCarpark = null;
                final Parking pp = p;
                View view = this.mapFragment.getView();
                if (view == null) {
                    Log.w(TAG, "no view in update(); fragment not initialized?");
                    return;
                }
                view.post(new Runnable() {
                    public void run() {
                        showBubble(pp);
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
            if (p.equals(this.currentBubble)) {
                removeBubble();
            }
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
        return true;
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
//            Log.d(TAG, "no marker found for car park " + p);
            return;
        }

        if (p.equals(this.currentBubble)) {
            if (!m.isInfoWindowShown()) {
                m.showInfoWindow();
            }

            this.currentBubble = p; // in case the instance has been updated
            updateDetails(p);
        }
    }

    /**
     * update the UI to match the pinned status of the given car park
     * @param p the car park
     */
    public void updatePinnedStatus(Parking p) {
        if (p.equals(this.currentBubble)) {
            updatePinnedStatus();
        }
    }

    private void updatePinnedStatus() {
        if (this.parkingsService.isPinnedCarpark(this.currentBubble)) {
            this.bubbleButtonPin.setImageResource(R.drawable.btn_pin_active);
        } else {
            this.bubbleButtonPin.setImageResource(R.drawable.btn_pin_inactive);
        }
    }

//    private void updateButtons(Parking p) {
//        if (p.isAvailabilityReportOutdated()) {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_32px);
//        } else if (p.getEffectiveAvailability() == Parking.Availability.AVAILABLE) {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_haloy_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_32px);
//        } else {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_haloy_32px);
//        }
//    }
//
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

    public View getInfoWindow(Marker m) {
        checkMarkerIsCurrentCarpark("getInfoWindow", m);

        // if it's the same marker as before, just return the same window
        if (m.equals(this.currentBubbleMarker)) {
            fillBubbleHeading(); // this updates the availability so that the time indication (like 5m ago) stays current-ish
//            Log.d(TAG, "returning previous info window");
            scheduleEnsureInfoWindowOnScreen();
            return this.currentBubbleView;
        }

        this.currentBubbleMarker = m;
        if (this.currentBubbleView == null) {
            this.currentBubbleView = this.activity.getLayoutInflater().inflate(R.layout.bubble, null);
            ((TableLayout)this.currentBubbleView.findViewById(R.id.bubble)).setColumnShrinkable(0, true);
        }
        fillBubbleHeading();
        fillBubbleDetails();

        scheduleEnsureInfoWindowOnScreen();

        return this.currentBubbleView;
    }

    private void scheduleEnsureInfoWindowOnScreen() {
        this.mapFragment.getView().post(new Runnable() {
            public void run() {
                doEnsureInfoWindowOnScreen();
            }
        });
    }

    public View getInfoContents(Marker m) {
        return null;
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

    /**
     * the caller of this method needs to adjust bubble height
     * @param empty
     */
    private void fillBubbleDetails() {
        ViewGroup details = (ViewGroup)this.currentBubbleView.findViewById(R.id.bubble_details);
        details.removeAllViews();
        ParkingDetailsActivity.createDetailsEntries(this.activity, details, this.currentBubble, null, this.activity.showUnconfirmedProperties, false);

        // adjust bubble size
        final View mapView = this.mapFragment.getView();
        if (mapView == null) {
            Log.w(TAG, "no map view! fragment not initialized?");
            return;
        }

        // make sure the view is laid out
        this.currentBubbleView.measure(View.MeasureSpec.makeMeasureSpec(mapView.getWidth(), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        this.currentBubbleView.layout(0, 0, this.currentBubbleView.getMeasuredWidth(), this.currentBubbleView.getMeasuredHeight());
//        Log.d(TAG, "measured: " + this.currentBubbleView.getMeasuredWidth() + "x" + this.currentBubbleView.getMeasuredHeight());

        View detailsScrollView = this.currentBubbleView.findViewById(R.id.bubble_details_scrollview);
        View detailsTopSeparator = this.currentBubbleView.findViewById(R.id.bubble_top_separator);
        View detailsEllipsis = this.currentBubbleView.findViewById(R.id.bubble_ellipsis);
        final int maxDetailsHeight = mapView.getHeight()/4;
        final int height = details.getHeight();
        if (height == 0) {
            detailsTopSeparator.setVisibility(View.GONE);
        } else {
            detailsTopSeparator.setVisibility(View.VISIBLE);
        }

        LayoutParams lpS = detailsScrollView.getLayoutParams();
        if (height <= maxDetailsHeight) {
            lpS.height = height;
            detailsEllipsis.setVisibility(View.GONE);
        } else {
            lpS.height = maxDetailsHeight;
            detailsEllipsis.setVisibility(View.VISIBLE);
        }
        detailsScrollView.setLayoutParams(lpS);
    }

    /**
     * checks whether the outline on the map is too small to be useful
     */
    void checkTooFarOut() {
        if (this.outline == null) {
            return;
        }
        Projection proj = this.map.getProjection();
        List<LatLng> outlinePoints = this.outline.getPoints();
        Point p1 = proj.toScreenLocation(outlinePoints.get(0));
        Point p2 = proj.toScreenLocation(outlinePoints.get(2));
        // compute the pixel distance of two opposite corners of the outline
        int dist = diagonal(p1.x-p2.x, p1.y-p2.y);

        // if the distance is too small (less than a 1/16th of the square of the diagonal dimension of the map view)
        View mapView = this.mapFragment.getView();
        int mapDiag = diagonal(mapView.getWidth(), mapView.getHeight());

        boolean tooFarOut = dist < mapDiag/4;
        this.activity.setTooFarOut(tooFarOut);
        this.setMarkersEnabled(!tooFarOut);
    }

    private int diagonal(double x, double y) {
        return (int)Math.sqrt(x*x+y*y);
    }

    private void setMarkersEnabled(boolean enabled) {
        if (this.markersEnabled == enabled) {
            return;
        }
        this.markersEnabled  = enabled;
        if (!enabled) removeBubble();
        update();
    }

}
