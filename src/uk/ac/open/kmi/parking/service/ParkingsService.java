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

package uk.ac.open.kmi.parking.service;

import java.util.Collection;
import java.util.List;

import uk.ac.open.kmi.parking.MapItem;
import uk.ac.open.kmi.parking.Parking;
import uk.ac.open.kmi.parking.Parking.Availability;
import uk.ac.open.kmi.parking.R;
import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * this class is the background service that loads parking data (parkings, other things for a map, availability and detailed info) in the background and tells the rest of the application.
 * @author Jacek Kopecky
 *
 */
public class ParkingsService implements NearbyCarparkUpdateListener {
//    @SuppressWarnings("unused")
    private static final String TAG = "parkings service";

    /**
     * the granularity at which the server is contacted, any map request will use a tile of this many microdegrees, both in latitude and longitude
     * NOTE that the value must be big enough to cover the largest distance to the nearest car park
     */
    public static final int TILE_SIZE = 30000;

    private SortingPrecomputationThread sortingPrecomputer;
    private NearPrecomputationThread nearPrecomputer;
    private TileDownloaderThread tileDownloader;
    private DetailsAndAvailabilityThread detailsAndAvailabilityDownloader;
    private CarparkAvailabilityRefreshTrigger carparkAvailabilityRefreshTrigger;

    /**
     * holder for a geocoder, should be set up at the beginning of an activity and set to null at the end of it so we don't hold references to the activity
     */
    public Geocoder geocoder = null;

    final RememberedCarparks rememberedCarparks;

    private Thread sortingPrecomputationThread = null;
    private Thread nearPrecomputationThread = null;
    private Thread tileDownloaderThread = null;
    private Thread detailsAndAvailabilityThread = null;

    private boolean threadsStopped = true;

    private boolean showUnconfirmedCarparks = true;

    // when the currently watched car park's tile is dropped out of the TileLoaderThread's cache, the current car park no longer coincides with the one in the map
    // this needs to be solved by not holding the actual Parking but just its ID and getting that from the weak hash map from ParkingsService
    private Uri currentParkingId = null;

    // todo ignore all request above 84n and below 84s: a) there aren't any car parks there, b) above that there's a chance near car park would be farther than in the adjoining tile

    // todo check the stuff below is all done, when so, move this to documentation
        // the map overlay calls parkingService.currentLocation(minmaxlatlone6...) for the zoomed-in part, the location listener calls parkingService.currentLocation(position)
        // the parking service should know what it has loaded in some sort of LRU cache of map tiles (keep all currently needed ones, and a number (fixed?dynamic?how?) of old ones)
        // when it gets an update of location, it should enqueue loading of its and possibly some adjacent tiles
        // the queue should automatically drop not-yet-satisfied requests that are outside of the current (and adjacent) region (when it gets to them)
        // the main activity should register as a listener on the parking service for change of parkings list in its area of interest
        // and around current location
    // (register in onResume, unregister in onPause) so that the static parkingsservice doesn't hold reference to potentially disused mainactivity
        // the main activity should propagate the events to the overlay,
        // but also possibly re-evaluate the nearest car park

        // rather than the above, the parking service should give to the overlay a precomputed (volatile) getSortedCurrentParkings(minmaxlatlone6)

    // the parking service needs to handle watching for availability updates as well:
    //   both the currently watched car park and the near one (it needs to know about them) most often (twice or more a minute?)
    //   the currently visible car parks every now and then (configurable?) - especially when the watched one (if none, the near one) becomes full
    //   the main activity should register as a listener on the parking service (in onResume and unregister in onPause), and propagate avaliability update events to the overlay and to currpark

      // the parking service also needs to handle detailed data about car parks (getting ttl information from http headers)
      //   the details should be kept in parking tiles (to be disposed of as above), or in weak hash map dedicated for parking details? initially the first will be easier
      //   it should load in the background the data for the currently watched car park
      //   also for the one that the user shows a bubble for (again, enqueuing the requests and ignoring the ones that no longer apply)
      //   so overlay and parkings service should tell the thread about those UI events that affect the desirability of detailed info
    //   the detailed view should register as a listener on the parking service, automatically giving priority to its request (if still enqueued) and be called if any update comes
    //     register in onResume and unregister in onPause

    // todo in all caches, get TTL from HTTP headers? currently there is no TTL at all

    // listeners supported by parking service:
    // ParkingAvailabilityChange
    // ParkingDetailsChange
        // ParkingsListChange - SortedCurrentItemsUpdateListener, NearCurrentItemsUpdateListener


    // todo check that for every registerListener there is an unregisterListener, maybe without exceptions

    // todo the service should be in the foreground if watching a car park when parkjam is not active - also when details activity is on

    /**
     * singleton constructor
     */
    private ParkingsService(Context ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("creating parkings service without a context not allowed");
        }
        this.rememberedCarparks = new RememberedCarparks(ctxt);

        this.tileDownloader = new TileDownloaderThread(this.rememberedCarparks);
        this.detailsAndAvailabilityDownloader = new DetailsAndAvailabilityThread(this);
        this.sortingPrecomputer = new SortingPrecomputationThread(this.tileDownloader);
        this.nearPrecomputer = new NearPrecomputationThread(this.tileDownloader);
        this.carparkAvailabilityRefreshTrigger = new CarparkAvailabilityRefreshTrigger(this.detailsAndAvailabilityDownloader, this.rememberedCarparks);

        this.nearPrecomputer.registerUpdateListener(this);

        NearPrecomputationThread.setNearestDistance(ctxt.getResources().getInteger(R.integer.near_car_park_meters));

        // threads are started and stopped as the main activity starts/stops - using Thread.interrupt(), and should the classes know about it? - should they unregister listeners?
    }

    private static ParkingsService instance;
    /**
     * there should only ever be one instance of parkings service
     * todo this should change when ParkingsService becomes Android service
     * @param ctxt context for initialization (mustn't be held)
     * @return a singleton parkings service
     */
    public static synchronized ParkingsService get(Context ctxt) {
        if (instance == null) {
            instance = new ParkingsService(ctxt);
        }
        return instance;
    }

    /**
     * returns (quickly) a precomputed collection of drawable overlay items for the given map
     * todo extract this into a DrawableOverlayItemContainer interface? there should be multiple impls for this because we want parkings, businesses, events etc.
     * @param mapCenter center of the map
     * @param longitudeSpan width of the map
     * @param latitudeSpan height of the map
     * @return a collection of drawable overlay items, sorted north-to-south
     */
    public Collection<MapItem> getSortedCurrentItems(LatLng mapCenter, int longitudeSpan, int latitudeSpan) {
        if (!this.threadsStopped) {
            // forward the location to the sorting precomputation thread
            this.sortingPrecomputer.onNewCoordinates(
                    new MapRectangle(
                            (int)(mapCenter.latitude*1e6)-latitudeSpan/2,
                            (int)(mapCenter.longitude*1e6)-longitudeSpan/2,
                            (int)(mapCenter.latitude*1e6)+latitudeSpan/2,
                            (int)(mapCenter.longitude*1e6)+longitudeSpan/2));
        }
        return getSortedCurrentItems();
    }

    /**
     * returns (quickly) the current a precomputed collection of drawable overlay items
     * @return a collection of drawable overlay items, sorted north-to-south
     */
    public Collection<MapItem> getSortedCurrentItems() {
        return this.sortingPrecomputer.sortedCurrentItems;
    }

    /**
     * register a listener for updates of the current sorted items collection
     * @param listener the listener
     */
    public synchronized void registerSortedCurrentItemsUpdateListener(SortedCurrentItemsUpdateListener listener) {
        this.sortingPrecomputer.registerUpdateListener(listener);
    }

    /**
     * unregister a listener for updates of the current sorted items collection
     * @param listener the listener
     */
    public synchronized void unregisterSortedCurrentItemsUpdateListener(SortedCurrentItemsUpdateListener listener) {
        this.sortingPrecomputer.unregisterUpdateListener(listener);
    }

    /**
     * register a listener for updates of the current near items collection
     * @param listener the listener
     */
    public synchronized void registerNearbyCurrentItemsUpdateListener(NearbyCarparkUpdateListener listener) {
        this.nearPrecomputer.registerUpdateListener(listener);
    }

    /**
     * unregister a listener for updates of the current near items collection
     * @param listener the listener
     */
    public synchronized void unregisterNearbyCurrentItemsUpdateListener(NearbyCarparkUpdateListener listener) {
        this.nearPrecomputer.unregisterUpdateListener(listener);
    }

    /**
     * register a listener for updates of details of car parks
     * @param listener the listener
     */
    public synchronized void registerCarparkDetailsUpdateListener(CarparkDetailsUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.registerDetailsUpdateListener(listener);
    }

    /**
     * register a listener for updates of details of car parks
     * @param listener the listener
     */
    public synchronized void unregisterCarparkDetailsUpdateListener(CarparkDetailsUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.unregisterDetailsUpdateListener(listener);
    }

    /**
     * register a listener for updates of details of car parks
     * @param listener the listener
     */
    public synchronized void registerCarparkAvailabilityUpdateListener(CarparkAvailabilityUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.registerAvailabilityUpdateListener(listener);
    }

    /**
     * register a listener for updates of details of car parks
     * @param listener the listener
     */
    public synchronized void unregisterCarparkAvailabilityUpdateListener(CarparkAvailabilityUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.unregisterAvailabilityUpdateListener(listener);
    }

    /**
     * this starts the service with all its background activities
     */
    public void startService() {
//        Log.d(TAG, "starting threads");
        synchronized(this) {
            if (this.delayedProcessKiller != null) {
                this.delayedProcessKiller.interrupt();
                this.delayedProcessKiller = null;
//                Log.d(TAG, "cancelling stopping of threads");
                return;
            }
        }
        if (!this.threadsStopped) {
            Log.e(TAG, "startService called while the threads are not stopped");
            return;
        }

        this.tileDownloaderThread = new Thread(this.tileDownloader);
        this.detailsAndAvailabilityThread = new Thread(this.detailsAndAvailabilityDownloader);
        this.sortingPrecomputationThread = new Thread(this.sortingPrecomputer);
        this.nearPrecomputationThread = new Thread(this.nearPrecomputer);

//            this.tileDownloaderThread.setDaemon(true);
//            this.sortingPrecomputationThread.setDaemon(true);
//            this.nearPrecomputationThread.setDaemon(true);

        this.tileDownloaderThread.start();
        this.detailsAndAvailabilityThread.start();
        this.sortingPrecomputationThread.start();
        this.nearPrecomputationThread.start();
        this.carparkAvailabilityRefreshTrigger.startThread();

        this.tileDownloader.refreshAllListeners();

        this.threadsStopped = false;
    }

    private Thread delayedProcessKiller;

    /**
     * this stops the service with all its background activities
     * @param ctxt for saving remembered carparks
     */
    public void stopService(Context ctxt) {
        saveRememberedCarparks(ctxt);
//        Log.d(TAG, "stopping threads: thread status: " + this.tileDownloaderThread.getState() + " " + this.detailsAndAvailabilityThread.getState() + " " + this.sortingPrecomputationThread.getState() + " " + this.nearPrecomputationThread.getState());
        if (this.threadsStopped) {
//            Log.e(TAG, "stopService called while the threads are stopped");
        } else {
            synchronized (this) {
                if (this.delayedProcessKiller == null) {
                    this.delayedProcessKiller = new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                // it's possible we miss the interrupt right after sleep ends but before we would do the interrupts - but then the delayedProcessKiller will be null (guarded by synchronization)
                                synchronized (ParkingsService.this) {
                                    if (ParkingsService.this.delayedProcessKiller != null) {
                                        ParkingsService.this.tileDownloaderThread.interrupt();
                                        ParkingsService.this.tileDownloaderThread = null;
                                        ParkingsService.this.detailsAndAvailabilityThread.interrupt();
                                        ParkingsService.this.detailsAndAvailabilityThread = null;
                                        ParkingsService.this.sortingPrecomputationThread.interrupt();
                                        ParkingsService.this.sortingPrecomputationThread = null;
                                        ParkingsService.this.nearPrecomputationThread.interrupt();
                                        ParkingsService.this.nearPrecomputationThread = null;
                                        ParkingsService.this.carparkAvailabilityRefreshTrigger.stopThread();

                                        ParkingsService.this.threadsStopped = true;
                                        ParkingsService.this.delayedProcessKiller = null;
                                    }
                                }
                            } catch (InterruptedException e) {
//                                Log.d(TAG, "threads not stopped after all");
                            }
                        }
                    };
                    this.delayedProcessKiller.start();
                }
            }
        }
    }

    /**
     * returns the current car park (either the nearest one or the watched one)
     * @return the current car park
     */
    public Parking getCurrentCarpark() {
        if (this.currentParkingId != null) {
            return Parking.getParking(this.currentParkingId);
        }
        // todo if no car park is selected, make the nearest one the current one, but only if pinned area has nearest car park in it
        // return this.nearPrecomputer.currentNearestCarpark;
        return null;
    }

    /**
     * sets the current car park (not synchronized, should be called from the UI thread)
     * @param currentParking the car park that will now the the current one
     */
    public void setCurrentExplicitCarpark(Parking currentParking) {
        this.currentParkingId = currentParking == null ? null : currentParking.id;
        this.carparkAvailabilityRefreshTrigger.updateBubble(this.currentParkingId);
        updateParkingDetails(currentParking);
    }

    /**
     * update the current location, useful in the follow-nearest mode
     * @param location the current location
     */
    public void onLocationChanged(Location location) {
        this.nearPrecomputer.onNewCoordinates(location);
        // todo maybe only do this if the pinned area has the nearest car park in it
    }

    /**
     * submit the new carpark, switch back to mode before adding car park
     * @param point where the new carpark is located
     * @param listener who should be notified when the car park is submitted and ready
     */
    public void submitNewCarpark(LatLng point, CarparkDetailsUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.submitCarpark(point, listener);
    }

    /**
     * request that the given URI be loaded as car park and sent to the listener (or null will be sent in case of failure to find the car park)
     * @param id the car park
     * @param listener who to notify when the car park is loaded (or null will be sent on failure)
     */
    public void loadExtraCarpark(Uri id, CarparkDetailsUpdateListener listener) {
        this.detailsAndAvailabilityDownloader.loadExtraCarparkAndTriggerTileRefresh(id, listener);
    }

    /**
     * triggers one update of the given parking's details
     * @param p the parking
     */
    public void updateParkingDetails(Parking p) {
        if (p!=null) {
            this.detailsAndAvailabilityDownloader.updateParkingDetails(p);
        }
    }

    /**
     * triggers submission of availability information
     * @param p the parking whose availability we should submit
     * @param binaryAvailability what the availability should be (explicit in case the parking's availability is updated by another thread)
     */
    public void submitAvailability(Parking p, boolean binaryAvailability) {
        long timestamp = System.currentTimeMillis();
        p.setAvailability(binaryAvailability ? Availability.AVAILABLE : Availability.FULL, timestamp);
        this.detailsAndAvailabilityDownloader.submitAvailability(p, binaryAvailability, timestamp);
    }

    /**
     * triggers submission of car park property
     * @param p the parking whose property we should submit
     * @param prop the RDF property to submit
     * @param value the literal value to submit
     */
    public void submitProperty(Parking p, Property prop, String value) {
        this.detailsAndAvailabilityDownloader.submitProperty(p, prop, value);
    }

    public void onNearbyCarparkUpdated() {
        // todo maybe only do this if nearby car park is shown in the pinned area?
        Parking p = this.nearPrecomputer.currentNearestCarpark;
        this.carparkAvailabilityRefreshTrigger.updateCurrpark(p == null ? null : p.id);
        updateParkingDetails(p);
    }

    /**
     * sets the current car park selected in the details activity to have frequent availability updates
     * @param p the parking, or null if the details view is closed
     */
    public void updateParkingAvailabilityDetailsView(Parking p) {
        this.carparkAvailabilityRefreshTrigger.updateDetailsView(p == null ? null : p.id);
    }

    /**
     * @return true if some car parks were already loaded successfully by the app
     */
    public boolean loadedSomeCarparks() {
        return this.tileDownloader.loadedSomeCarparks;
    }

    void triggerTileRefresh(LatLng point) {
        this.tileDownloader.refreshTile(point, null, null, null);
    }

    void triggerTileRefresh(LatLng point, Uri id, Model data, CarparkDetailsUpdateListener listener) {
        this.tileDownloader.refreshTile(point, id, data, listener);
    }

    /**
     * checks whether the given car park is one of the ones remembered by the app and not yet indexed by the server
     * @param p the carpark to check
     * @return true if the car park is (as far as the app knows) not yet indexed by the server
     */
    public boolean isRememberedAddedCarpark(Parking p) {
        return this.rememberedCarparks.containsAddedCarpark(p.id.toString());
    }

    /**
     * checks whether the car park is remembered as pinned
     * @param p the car park
     * @return true if the car park is pinned
     */
    public boolean isPinnedCarpark(Parking p) {
        return this.rememberedCarparks.isPinnedCarpark(p);
    }

    /**
     * sets whether pinned car parks should be updated (when the pinned drawer is visible)
     * @param enable whether the availaibility should be updated periodically
     */
    public void setPinnedUpdating(boolean enable) {
        this.carparkAvailabilityRefreshTrigger.setPinnedUpdating(enable);
    }

//    /**
//     * @return true if we remember any pinned car parks (even if they are elsewhere in the world)
//     */
//    public boolean hasPinnedCarparks() {
//        return this.rememberedCarparks.hasPinnedCarparks();
//    }
//
    /**
     * @return the pinned car parks that are in the currently loaded area
     */
    public List<Parking> listKnownPinnedCarparks() {
        return this.rememberedCarparks.listKnownPinnedCarparks();
    }

    /**
     * pins or unpins the given car park
     * @param p the car park
     * @param pin whether it should be pinned (true) or unpinned (false)
     * @param ctxt context for saving the list of pinned car parks
     */
    public void pinCarpark(Parking p, boolean pin, Context ctxt) {
        if (pin) {
            this.rememberedCarparks.rememberPinnedCarpark(p, ctxt);
        } else {
            this.rememberedCarparks.removePinnedCarpark(p, ctxt);
        }
    }

    /**
     * save any changes in remembered car parks (because some changes happen in parkings service that isn't a context yet // todo this should go away when ParkingsService is an android Service
     * @param ctxt context for the data
     */
    public void saveRememberedCarparks(Context ctxt) {
        this.rememberedCarparks.saveToStorage(ctxt);
    }

    /**
     * set whether unconfirmed carparks should be shown
     * @param show true when unconfirmed car parks should be shown
     */
    public void setShowUnconfirmedCarparks(boolean show) {
        if (this.showUnconfirmedCarparks != show) {
            this.showUnconfirmedCarparks = show;
            this.tileDownloader.refreshAllListeners();
        }
    }

    /**
     * @return whether unconfirmed carparks should be shown
     */
    public boolean getShowUnconfirmedCarparks() {
        return this.showUnconfirmedCarparks;
    }

    String formatUnknownCarparkTitle(String id) {
        return "Car park " + id; // todo this should use the resource
    }
}
