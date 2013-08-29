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

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * a single parking with all information we have about it
 * @author Jacek Kopecky
 *
 */
public class Parking {
//    @SuppressWarnings("unused")
    private static final String TAG = "parking";

    private static final long CUTOFF_MILLIS = 14*60*60*1000;

    @SuppressWarnings("javadoc")
    public static enum Availability { FULL, AVAILABLE, UNKNOWN };

    /**
     * the RDF ID of the car park
     */
    public final Uri id;

    /**
     * the location of this car park
     */
    public final LatLng point;

    /**
     * the title of this car park (allowed to change)
     */
    protected String title;

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

    private static BitmapDescriptor drawFull = null;
    private static BitmapDescriptor drawAvailable = null;

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
    public Parking(LatLng point, String title, Uri id, String availabilityResource, String updateResource, Availability available, Long timestamp, long availTTL, Property titleProperty, boolean unconfirmed) {
        this.point = point;
        this.title = title;
        this.id = id;
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

    /**
     * @return the bitmap to be used for this car park's marker
     */
    public BitmapDescriptor getBitmapDescriptor() {
        if (drawFull == null) {
            throw new IllegalStateException("must use Parking.setDrawables() before using Parking instances in an overlay");
        }
        BitmapDescriptor retval = null;
        switch (this.availabilityEffective) {
        case AVAILABLE:
            retval = drawAvailable;
            break;
        case FULL:
            retval = drawFull;
            break;
        case UNKNOWN:
            Log.w(TAG, "car park with unknown availability has getBitmap called on it");
            break;
        }
        return retval;
    }

    /**
     * sets the drawables to be used for full and available parkings
     * @param full drawable for full parkings
     * @param available drawable for available parkings
     */
    public static void setDrawables(Bitmap full, Bitmap available) {
        drawFull = BitmapDescriptorFactory.fromBitmap(full);
        drawAvailable = BitmapDescriptorFactory.fromBitmap(available);
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


    /**
     * return the (current) title of this car park
     * @return the title of this carpark
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * comparator that sorts drawables north-to-south, west-to-east
     */
    public static class NW2SEComparator implements java.util.Comparator<Parking> {

        public int compare(Parking object1, Parking object2) {
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
    public static class SquareDistComparator implements java.util.Comparator<Parking> {

        private final LatLng center;
        /**
         * ratio of longitude length to latitude - at equator, that's 1; at higher latitudes it becomes smaller as the longitude lines get closer together
         */
        public final double lonRatio;

        /**
         * @param center the center along the distance from which the objects will be compared
         */
        public SquareDistComparator(LatLng center) {
            this.center = center;
            this.lonRatio = Math.cos(center.latitude*(Math.PI/180d));
        }

        public int compare(Parking object1, Parking object2) {
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

            double o1dist = squareDist(object1.point);
            double o2dist = squareDist(object2.point);
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

        /**
         * computes the square distance (maximum of the lat and long distances, compensating for longitude meaning less distance in higher latitudes) of the point to the given center
         * @param point the point
         * @return the square distance in lengths of latitude
         */
        public double squareDist(LatLng point) {
            return Math.max(Math.abs(point.latitude-this.center.latitude), Math.abs(point.longitude-this.center.longitude)*this.lonRatio);
        }
    }

    @Override
    public String toString() {
        return "parking " + this.title;
    }
}
