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

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

/**
 * an item in the parkjam overlay class
 * @author Jacek Kopecky
 *
 */
public abstract class DrawableOverlayItem {

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
    public DrawableOverlayItem(LatLng point, String title, Uri id) {
        this.point = point;
        this.title = title;
        this.id = id;
    }

    /**
     * @return the drawable to be used for this item, should have the proper bounds
     */
    public abstract Drawable getDrawable();

    /**
     * @return the view used to highlight this item (by drawing and animating it where the item is); its bounds should be the same as getDrawable's
     */
    public abstract View getHighlight();

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
    public static class Comparator implements java.util.Comparator<DrawableOverlayItem> {

        public int compare(DrawableOverlayItem object1, DrawableOverlayItem object2) {
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

    @Override
    public String toString() {
        return "parking " + this.title;
    }
}
