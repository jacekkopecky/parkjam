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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * reimplemented so as not to lose location pin
 * @author Jacek Kopecky
 *
 */
class MyLocationOverlay extends Overlay {
    @SuppressWarnings("unused")
    private final static String TAG = "my location overlay";
    private GeoPoint location;
    private boolean currentLocationOnScreen;
    private float accuracyAdjusted;
    private MapView mapView;
    private View pointView;
    private AnimationDrawable pointViewImg;

    private static Paint paintOuter, paintFill;

    static {
        paintOuter = new Paint();
        paintOuter.setStyle(Paint.Style.STROKE);
        paintOuter.setStrokeWidth(2.5f);
        paintOuter.setColor(Color.BLUE);
        paintOuter.setAntiAlias(true);
        paintOuter.setAlpha(96);
        paintFill = new Paint();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(Color.BLUE);
        paintFill.setAlpha(32);
    }

    private final Point tmpPoint = new Point();

    public MyLocationOverlay(MapView mv, View point) {
        this.mapView = mv;
        this.pointView = point;
        this.pointViewImg = (AnimationDrawable) point.getBackground();
    }

    public void setCurrentLocationOnScreen(boolean v) {
        this.currentLocationOnScreen = v;
    }

    public void onLocationChanged(Location loc) {
        if (loc == null) {
            this.location = null;
            this.pointViewImg.stop();
            this.mapView.removeView(this.pointView);
        } else {
            this.location = new GeoPoint((int)(loc.getLatitude()*1e6), (int)(loc.getLongitude()*1e6));

            // need to move the map in case the location was on the visible map and isn't now (record in draw() using getLatitudeSpan etc.)
            if (this.currentLocationOnScreen && !checkCurrentLocationOnScreen(this.mapView, this.mapView.getProjection())) {
                this.currentLocationOnScreen = true;
                this.mapView.getController().animateTo(this.location);
            }

            this.accuracyAdjusted = loc.getAccuracy();
            // this.accuracyAdjusted = (float)(loc.getAccuracy()/Math.cos(loc.getLatitude()/180.*Math.PI)); // todo would the adjustment be wrong? google doesn't do it...

            this.mapView.removeView(this.pointView);
            MapView.LayoutParams layoutParams = new MapView.LayoutParams(
                    this.pointViewImg.getIntrinsicWidth(),
                    this.pointViewImg.getIntrinsicHeight(),
                    this.location,
                    MapView.LayoutParams.CENTER);
            this.pointView.setLayoutParams(layoutParams);
            this.mapView.addView(this.pointView);
            this.pointViewImg.start();

        }
    }

    @Override
    public void draw(Canvas canvas, MapView map, boolean shadow) {
        if (shadow || this.location == null) {
            return;
        }

        Projection proj = map.getProjection();

        // check whether the current location is on the visible map
        this.currentLocationOnScreen = checkCurrentLocationOnScreen(map, proj);

        proj.toPixels(this.location, this.tmpPoint);
        float radius = proj.metersToEquatorPixels(this.accuracyAdjusted);
        canvas.drawCircle(this.tmpPoint.x, this.tmpPoint.y, radius, paintFill);
        canvas.drawCircle(this.tmpPoint.x, this.tmpPoint.y, radius, paintOuter);
    }

    private boolean checkCurrentLocationOnScreen(MapView map, Projection proj) {
        GeoPoint nw = proj.fromPixels(0, 0);
        GeoPoint se = proj.fromPixels(map.getWidth(), map.getHeight());
        if (this.location.getLatitudeE6() > se.getLatitudeE6() && this.location.getLatitudeE6() < nw.getLatitudeE6() &&
                this.location.getLongitudeE6() > nw.getLongitudeE6() && this.location.getLongitudeE6() < se.getLongitudeE6()) {
            return true;
        }
        return false;
    }


}
