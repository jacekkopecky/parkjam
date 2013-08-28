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

import android.net.Uri;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

/**
 * an item in the parkjam overlay class
 * @author Jacek Kopecky
 *
 */
public abstract class MapItem {

    /**
     * the RDF ID of the car park
     */
    public final Uri id;

    /**
     * the location of this item
     */
    public final LatLng point;

    /**
     * the title of this item (allowed to change)
     */
    protected String title;

    /**
     * constructor
     * @param point the location of the item
     * @param title the main name of the title
     * @param id the identifier of the item
     */
    public MapItem(LatLng point, String title, Uri id) {
        this.point = point;
        this.title = title;
        this.id = id;
    }

    /**
     * @return the drawable to be used for this item, should have the proper bounds
     */
    public abstract BitmapDescriptor getBitmapDescriptor();

    /**
     * @return the view used to highlight this item (by drawing and animating it where the item is); its bounds should be the same as getDrawable's
     */
//    public abstract View getHighlight();

    /**
     * return the (current) title of this item
     * @return the title of this item
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * comparator that sorts drawables north-to-south, west-to-east
     */
    public static class NW2SEComparator implements java.util.Comparator<MapItem> {

        public int compare(MapItem object1, MapItem object2) {
            if (object1 == null) {
                if (object2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (object2 == null) {
                return 1;
            }
            LatLng p1 = object1.point;
            LatLng p2 = object2.point;
            if (p1.latitude > p2.latitude) {
                return -1;
            } else if (p1.latitude == p2.latitude) {
                if (p1.longitude > p2.longitude) {
                    return -1;
                } else if (p1.longitude == p2.longitude) {
                    int h1 = object1.hashCode();
                    int h2 = object2.hashCode();
                    return h1 < h2 ? -1 : (h1 == h2 ? 0 : 1);
                } else {
                    return 1;
                }
            } else {
                return 1;
            }
        }
    }

    /**
     * comparator that sorts drawables by maximum{lat,long} distance from the center
     */
    public static class SquareDistComparator implements java.util.Comparator<MapItem> {

        private final LatLng center;

        /**
         * @param center the center along the distance from which the objects will be compared
         */
        public SquareDistComparator(LatLng center) {
            this.center = center;
        }

        public int compare(MapItem object1, MapItem object2) {
            if (object1 == null) {
                if (object2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (object2 == null) {
                return 1;
            }
            if (object1 == object2 || object1.equals(object2)) return 0;

            double o1dist = squareDist(this.center, object1.point);
            double o2dist = squareDist(this.center, object2.point);
            if (o1dist > o2dist) {
                return 1;
            } else if (o1dist == o2dist) {
                int h1 = object1.hashCode();
                int h2 = object2.hashCode();
                if (h1 == h2) {
                    h1 = System.identityHashCode(object1);
                    h2 = System.identityHashCode(object2);
                }
                return h1 < h2 ? -1 : (h1 == h2 ? 0 : 1);
            } else {
                return -1;
            }
        }
    }

    /**
     * computes the square distance (maximum of the lat and long distances) between two points
     * @param center first point
     * @param item second point
     * @return the square distance
     */
    public static double squareDist(LatLng center, LatLng item) {
        return Math.max(Math.abs(item.latitude-center.latitude), Math.abs(item.longitude-center.longitude));
    }

    @Override
    public String toString() {
        return "parking " + this.title;
    }
}
