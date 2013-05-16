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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * a single parking with all information we have about it
 * @author Jacek Kopecky
 *
 */
public class Parking extends DrawableOverlayItem {
    @SuppressWarnings("unused")
    private static final String TAG = "parking";

    private static final long CUTOFF_MILLIS = 14*60*60*1000;

    /**
     * precomputed floating-point latitude
     */
    public final double latitude;
    /**
     * precomputed floating-point longitude
     */
    public final double longitude;

    @SuppressWarnings("javadoc")
    public static enum Availability { FULL, AVAILABLE, UNKNOWN };

    /**
     * whether the car park is seen as avaiable
     */
    private Availability availabilityEffective;

    /**
     * whether the car park was reported as avaiable
     */
    private Availability availabilityReported;

    /**
     * timestamp (possibly null) of the last report about this car park's availability
     */
    private Long availabilityTimestamp;

    private boolean availabilityReportOutdated = false;

    /**
     * resource where this car park's availability can be updated
     */
    public String availabilityResource;

    /**
     * resource where this car park's information can be updated
     */
    public String updateResource;

    /**
     * the system time of the last update of the car park's details, in milliseconds
     */
    public long lastDetailsUpdate = 0;
    /**
     * the system time of the earliest non-forced next update of the car park's details, in milliseconds
     * default 0 because the constructor doesn't fill in details
     */
    public long nextDetailsUpdate = 0;
    /**
     * the system time of the last update of the park's availability, in milliseconds
     */
    public long lastAvailUpdate = 0;
    /**
     * the system time of the earliest non-forced next update of the park's availability, in milliseconds
     */
    public long nextAvailUpdate = 0;
    /**
     * the RDF triples with details of the parking
     */
    public Model details;

    /**
     * the property that was used to get the name, will be ignored in presentation
     */
    public Property titleProperty = null;

    private boolean hasAnyTitle;

    /**
     * @return true if this car park has any title at all, whether from the data or geocoded
     */
    public boolean hasAnyTitle() {
        return this.hasAnyTitle;
    }

    /**
     * @return true if this car park has a title from the data
     */
    public boolean hasExplicitTitle() {
        return this.titleProperty != null && this.titleProperty != Onto.PARKING_geocodedTitle;
    }

    /**
     * this is true when the car park is unconfirmed - it was submitted by a user but not approved yet
     */
    public boolean unconfirmed;

    // todo this can be more hard-wired and faster
    private static Drawable drawFull = null;
    private static Drawable drawAvailable = null;
    private static Drawable drawUnknown = null;
    private static View drawHighlightA = null;
    private static View drawHighlightF = null;
    private static View drawHighlightU = null;
    private static Rect boundsFull = null;
    private static Rect boundsAvailable = null;
    private static Rect boundsUnknown = null;

    /**
     * constructor, fills all the fields
     * @param point location
     * @param title car park name
     * @param id RDF id
     * @param availabilityResource the resource where this car park's availability can be updated
     * @param updateResource the resource where new properties for this car park should be sent
     * @param available availability status
     * @param timestamp timestamp (may be null) of the availability status
     * @param availTTL time-to-live (in ms) for the availability information
     * @param titleProperty the property that was used to get the name, will be ignored in presentation, is null when the server gave us no title
     * @param unconfirmed whether the car park is submitted recently and not yet approved
     */
    public Parking(GeoPoint point, String title, Uri id, String availabilityResource, String updateResource, Availability available, Long timestamp, long availTTL, Property titleProperty, boolean unconfirmed) {
        super(point, title, id);
        this.latitude = point.getLatitudeE6() / 1000000.;
        this.longitude = point.getLongitudeE6() / 1000000.;
        this.availabilityEffective = available;
        this.availabilityReported = available;
        this.availabilityTimestamp = timestamp;
        this.availabilityResource = availabilityResource;
        this.updateResource = updateResource;
        this.titleProperty = titleProperty;
        this.unconfirmed = unconfirmed;

        this.hasAnyTitle = titleProperty != null;

        checkIfOutdatedInfo();

        this.lastAvailUpdate = System.currentTimeMillis();
        this.nextAvailUpdate = this.lastAvailUpdate + availTTL;

        // there might be race conditions if two parkings with the same ID are created in parallel, when the one that remains in the map would be under the other's ID object and could potentially disappear when that object is GCd
        // but since parkings are only created in tiledownloaderthread in sequence, this is not an issue so there's no synchronization here
        knownParkings.remove(id); // necessary so that the new entry created below uses this particular ID object
        knownParkings.put(id, new WeakReference<Parking>(this));
    }

    /* *
     * constructor, fills all the fields
     * @param lat latitude
     * @param lon longitude
     * @param title car park name
     * @param id RDF id
     * @param available availability status
     * /
    public Parking(double lat, double lon, String title, Uri id, boolean available) {
        super(new GeoPoint((int)(lat*1e6), (int)(lon*1e6)), title, "subtitle not used at the moment", id);
        this.latitude = lat;
        this.longitude = lon;
        this.available = available;
    }*/

    @Override
    public Drawable getDrawable() {
        if (drawFull == null) {
            throw new IllegalStateException("must use Parking.setDrawables() before using Parking instances in an overlay");
        }
        Drawable retval = null;
        switch (this.availabilityEffective) {
        case AVAILABLE:
            retval = drawAvailable;
            retval.setBounds(boundsAvailable);
            break;
        case FULL:
            retval = drawFull;
            retval.setBounds(boundsFull);
            break;
        case UNKNOWN:
            retval = drawUnknown;
            retval.setBounds(boundsUnknown);
            break;
        }
        return retval;
    }

    @Override
    public View getHighlight() {
        if (drawFull == null) {
            throw new IllegalStateException("must use Parking.setDrawables() before using Parking instances in an overlay");
        }
        switch (this.availabilityEffective) {
        case AVAILABLE:
            return drawHighlightA;
        case FULL:
            return drawHighlightF;
        default:
            return drawHighlightU;
        }
    }

    /**
     * sets the drawables to be used for full and available parkings
     * @param full drawable for full parkings
     * @param fullBounds the bounds for the drawable for full parkings - this can be used to put the origin point in the center (an icon), or at the bottom middle (a pin in the map)
     * @param available drawable for available parkings
     * @param availableBounds the bounds for the drawable for available parkings
     * @param unknown drawable for parkings with unknown availability
     * @param unknownBounds the bounds for the drawable for unknown-availability parkings
     * @param highlightA view for highlighting the current parking if available
     * @param highlightF view for highlighting the current parking if full
     * @param highlightU view for highlighting the current parking if unknown
     */
    public static void setDrawables(Drawable full, Rect fullBounds, Drawable available, Rect availableBounds, Drawable unknown, Rect unknownBounds, View highlightA, View highlightF, View highlightU) {
        drawFull = full;
        drawAvailable = available;
        drawUnknown = unknown;
        drawHighlightA = highlightA;
        drawHighlightF = highlightF;
        drawHighlightU = highlightU;
        boundsFull = fullBounds;
        boundsAvailable = availableBounds;
        boundsUnknown = unknownBounds;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Parking) {
            return ((Parking)o).id.equals(this.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    private static WeakHashMap<Uri, WeakReference<Parking>> knownParkings = new WeakHashMap<Uri, WeakReference<Parking>>();

    /**
     * returns parking by its ID if such a parking is known to us
     * @param id the URI of the requested parking
     * @return the parking or null
     */
    public static Parking getParking(Uri id) {
//        if (id == null) {
//            return null;
//        }
        WeakReference<Parking> ref = knownParkings.get(id);
        Parking retval;
        if (ref != null) {
            retval = ref.get();
        } else {
            retval = null;
        }

        if (retval == null) {
//            Log.d(TAG, "parking not found " + id, new Exception());
//            Log.d(TAG, "known parkings size: " + knownParkings.size());
        }
        return retval;
    }

    /**
     * @return the effective availability
     */
    public Availability getEffectiveAvailability() {
        return this.availabilityEffective;
    }

    /**
     * @return the reported availability
     */
    public Availability getReportedAvailability() {
        return this.availabilityReported;
    }

    /**
     * @return the reported availability
     */
    public Long getReportedAvailabilityTimestamp() {
        return this.availabilityTimestamp;
    }

    /**
     * @return true when the car park's last reported availability is too old and disregarded (effective availability may - but need not - differ from reported availability)
     */
    public boolean isAvailabilityReportOutdated() {
        return this.availabilityReportOutdated;
    }

    /**
     * @param availability the reported availability to set
     * @param timestamp the time stamp of the reported availability
     */
    public void setAvailability(Availability availability, Long timestamp) {
        if (this.availabilityTimestamp == null ||
                (timestamp != null && this.availabilityTimestamp <= timestamp)) {
            this.availabilityEffective = availability;
            this.availabilityReported = availability;
            this.availabilityTimestamp = timestamp;
        }

        checkIfOutdatedInfo();
    }

    /**
     *  if the data is too old, say state AVAILABLE; if report is in the future, treat is as current
     */
    private void checkIfOutdatedInfo() {
        this.availabilityReportOutdated = false;
        if (this.availabilityTimestamp != null) {
            long currentTime = System.currentTimeMillis();
            long obsoletionTime = currentTime - CUTOFF_MILLIS;
            if (this.availabilityTimestamp < obsoletionTime) {
                this.availabilityEffective = Availability.AVAILABLE;
                this.availabilityReportOutdated = true;
            } else if (this.availabilityTimestamp > currentTime) {
                this.availabilityTimestamp = currentTime;
            }
        }
    }

    /**
     * change the title of this car park
     * @param title the new title
     * @param titleProperty the property from which the title comes
     */
    public synchronized void setTitle(String title, Property titleProperty) {
        this.hasAnyTitle = true;
        this.title = title;
        this.titleProperty = titleProperty;
    }
}
