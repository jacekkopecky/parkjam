/*
   Copyright 2013 Jacek Kopecky (jacek@jacek.cz)

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

import android.location.Location;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * handles tracking of current location on the map; enabled by the user clicking "my location" on the map, disabled by any other movement of the map
 * @author Jacek Kopecky
 *
 */
public class MyLocationTracker implements
    OnCameraChangeListener,
    OnMyLocationButtonClickListener,
    LocationListener {

    private GoogleMap map;
    private boolean tracking = false;
    private long inAnimationUntil = 0;

    /**
     * @param map the map to follow
     */
    public MyLocationTracker(GoogleMap map) {
        // TODO Auto-generated constructor stub
        map.setOnMyLocationButtonClickListener(this);
        map.setOnCameraChangeListener(this);
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        this.map = map;
    }

    public boolean onMyLocationButtonClick() {
        this.tracking = true;
        this.map.getUiSettings().setMyLocationButtonEnabled(false);
        MyLocationTracker.this.inAnimationUntil = System.currentTimeMillis() + 1200;
        return false;
    }

    public void onCameraChange(CameraPosition arg0) {
        if (this.inAnimationUntil < System.currentTimeMillis()) {
            this.tracking = false;
            this.map.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    public void onLocationChanged(Location loc) {
        if (this.tracking) {
            this.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude())), 1000, new CancelableCallback() {

                public void onFinish() {
                    MyLocationTracker.this.inAnimationUntil = System.currentTimeMillis() + 200;
                }

                public void onCancel() { /* noop */ }
            });
        }
    }

}