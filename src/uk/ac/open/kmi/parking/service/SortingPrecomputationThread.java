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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

import uk.ac.open.kmi.parking.MapItem;
import uk.ac.open.kmi.parking.MapItem.SquareDistComparator;
import uk.ac.open.kmi.parking.Parking;

/**
 * this thread computes in the background the car parks that are near the currently visible coordinates
 */
class SortingPrecomputationThread implements Runnable, TileUpdateListener, TileDesirabilityChecker {
//    @SuppressWarnings("unused")
    private static final String TAG = "sorting thread";

    volatile Collection<MapItem> sortedCurrentItems = Collections.emptyList(); // HardwiredParkingList.listParkings();
    volatile List<LatLng> currentSortedOutline = new ArrayList<LatLng>(5);

    private volatile MapRectangle currentCoveredCoordinatesE6 = null;
    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(1000); // todo make this number configurable? also the number of updatedTiles below
    private TileDownloaderThread tileDownloader;

//    private final static int[] SQUARE_WALK_X = new int[] { 0,   -1,  0,  1,  0,   -1, -1,  1,  1,    0, -2,  0,  2,    2,  1, -1, -2, -2, -1,  1,  2,    2, -2, -2,  2};
//    private final static int[] SQUARE_WALK_Y = new int[] { 0,    0,  1,  0, -1,   -1,  1,  1, -1,   -2,  0,  2,  0,   -1, -2, -2, -1,  1,  2,  2,  1,   -2, -2,  2,  2};

    private final static int[] SQUARE_WALK_X = new int[] { 2,   1, 2, 3, 2,   1, 1, 3, 3,   2, 0, 2, 4,   4, 3, 1, 0, 0, 1, 3, 4,   4, 0, 0, 4};
    private final static int[] SQUARE_WALK_Y = new int[] { 2,   2, 3, 2, 1,   1, 3, 3, 1,   0, 2, 4, 2,   1, 0, 0, 1, 3, 4, 4, 3,   0, 0, 4, 4};


    public SortingPrecomputationThread(TileDownloaderThread tileDownloader) {
        this.tileDownloader = tileDownloader;
        tileDownloader.registerTileUpdateListener(this);
        tileDownloader.registerTileDesirabilityChecker(this);
    }

    private int lastTileMinLatE6 = Integer.MIN_VALUE;
    private int lastTileMinLonE6 = Integer.MIN_VALUE;
    private int lastTileMaxLatE6 = Integer.MAX_VALUE;
    private int lastTileMaxLonE6 = Integer.MAX_VALUE;
    private LatLng lastCoords = null;

    private final List<MapTile> updatedTiles = new ArrayList<MapTile>(1000);
    private int maxQueueSize = 0;

    public void run() {
        //            long nextReportTime = System.currentTimeMillis()+999;
        //            long callCount=0;
        Thread refresher = null;
        long refresherTargetTime = 0;

        for (;;) {
            try {
                int queueSize = this.eventQueue.size();
                if (queueSize > this.maxQueueSize) {
                    this.maxQueueSize = queueSize;
                    //                        Log.i(TAG, "queue size maximum increased to " + this.maxQueueSize);
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue - just under a 100 is OK because of tile update events from newly-loaded supertiles
                }
//                                    long currTime = System.currentTimeMillis();
                //                    if (currTime > (nextReportTime)) {
                //                        Log.d(TAG, "" + (currTime-nextReportTime+999) + "ms: " + callCount + " requests");
                //                        callCount=0;
                //                        nextReportTime=currTime+999;
                //                    }
                //                    callCount++;

                // in case the events are only tile updates, we want to act on the last coordinates
                LatLng coords = null;

                Event event = this.eventQueue.take();
                long startTime = System.currentTimeMillis();
                do {
                    switch (event.type) {
                    case NEW_COORDINATES:
                        coords = event.coords;
                        break;
                    case TILE_UPDATE:
                        this.updatedTiles.add(event.tile);
                        break;
                    case REFRESH:
                        coords = this.lastCoords;
                        //                            Log.d(TAG, "refreshing");
                        break;
                    }
                    event = this.eventQueue.poll();
                } while (event != null);

                if (coords == null) {
                    if (this.lastCoords == null) {
                        // there are no coordinates yet, nothing to do
                        continue;
                    }

                    // we can use the last coordinates, if we received nearby tile updates
                    coords = this.lastCoords;

                    // let's see if some updated tile is close enough to the current ones
                    boolean forceUpdate = false;
                    for (MapTile tile : this.updatedTiles) {
                        if (tile.late6min >= this.lastTileMinLatE6 &&
                                tile.lone6min >= this.lastTileMinLonE6 &&
                                tile.late6min <= this.lastTileMaxLatE6 &&
                                tile.lone6min <= this.lastTileMaxLonE6) {
                            forceUpdate = true;
                            break;
                        }
                    }

                    if (!forceUpdate) {
                        continue;
                    }
                }
                this.updatedTiles.clear();
                this.lastCoords = coords;

                // we simply combine (and sort) the parks in the tiles that contains the coords, and two beyond (at least 25 total)

                // new limits, the 5x5 tiles whose middle one contains the coords
                int tileMinLatTile = (int)Math.floor(coords.latitude / ParkingsService.TILE_SIZE_D) - 2;
                int tileMinLonTile = (int)Math.floor(coords.longitude / ParkingsService.TILE_SIZE_D) - 2;
                int tileMaxLatTile = (int)Math.floor(coords.latitude / ParkingsService.TILE_SIZE_D) + 2;
                int tileMaxLonTile = (int)Math.floor(coords.longitude / ParkingsService.TILE_SIZE_D) + 2;

                this.currentCoveredCoordinatesE6 =
                        new MapRectangle(
                                tileMinLatTile * ParkingsService.TILE_SIZE_E6,
                                tileMinLonTile * ParkingsService.TILE_SIZE_E6,
                                (tileMaxLatTile+1) * ParkingsService.TILE_SIZE_E6-1,
                                (tileMaxLonTile+1) * ParkingsService.TILE_SIZE_E6-1);

                //                        Log.d(TAG, "coords " + coordsE6 + " lead to covered coordinates " + this.currentCoveredCoordinatesE6);

                final boolean onlyConfirmed = !ParkingsService.get(null).getShowUnconfirmedCarparks();

                SquareDistComparator comparator = new SquareDistComparator(coords);
                TreeSet<MapItem> retval = new TreeSet<MapItem>(comparator);
                int count=0; int total=0;

                // this thread also makes sure to update tiles when they expire, by doing a refresh
                long minNextUpdateTime = Long.MAX_VALUE;

                // getting all the tiles, starting from the center of the area
                for (int i=0; i<25; i++) {
                    int latTile = tileMinLatTile + SQUARE_WALK_X[i];
                    int lonTile = tileMinLonTile + SQUARE_WALK_Y[i];

                    MapTile tile = this.tileDownloader.getTile(latTile*ParkingsService.TILE_SIZE_E6, lonTile*ParkingsService.TILE_SIZE_E6);
                    total++;
                    if (tile != null) {
                        if (tile.nextUpdate < minNextUpdateTime) {
                            minNextUpdateTime = tile.nextUpdate;
                        }
                        for (Parking parking : tile.parkings.values()) {
                            if (onlyConfirmed && parking.unconfirmed) {
                                continue;
                            }
                            retval.add(parking);
                        }
                        count++;
                    }
                }

                // drop old refresher
                if (refresher != null && minNextUpdateTime != refresherTargetTime) {
                    refresher.interrupt();
                    refresher = null;
                    refresherTargetTime = 0;
                }

                if (minNextUpdateTime < Long.MAX_VALUE && minNextUpdateTime != refresherTargetTime) {
                    refresherTargetTime = minNextUpdateTime;
                    final long sleepTime = minNextUpdateTime - System.currentTimeMillis();
//                    Log.d(TAG, "next refresh time in " + sleepTime + "ms");
                    if (sleepTime <= 0) {
//                        Log.w(TAG, "next refresh hopefully in progress");
                    } else {
                        refresher = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(sleepTime+Config.EXTRA_SLEEP_TIME); // todo +200 so that even if sleep is shorter (it's not guaranteed to be precise) it's likely not to underrun the time
                                    SortingPrecomputationThread.this.onTimeToRefresh();
                                } catch (InterruptedException e) {
                                    // refresh cancelled, that's OK
                                }
                            }
                        });
                        refresher.start();
                    }
                }

                // setting the current car parks nearest to the map camera, and the outline of the displayed carparks
                // this must be set before listeners are called because they may use it
                ArrayList<MapItem> nearest = new ArrayList<MapItem>(ParkingsService.MAX_CARPARKS_DISPLAYED);
                MapItem firstItemOutside = null;
                int i = 0;
                for (MapItem p: retval) {
                    if ((i++) < ParkingsService.MAX_CARPARKS_DISPLAYED) {
                        nearest.add(p);
                    } else {
                        firstItemOutside = p;
                        break;
                    }
                }
                this.sortedCurrentItems = nearest;

                this.currentSortedOutline.clear();

                // compute the outline of the sorted items
                double outlineLatMin = this.currentCoveredCoordinatesE6.latminE6/1e6d;
                double outlineLatMax = this.currentCoveredCoordinatesE6.latmaxE6/1e6d;
                double outlineLonMin = this.currentCoveredCoordinatesE6.lonminE6/1e6d;
                double outlineLonMax = this.currentCoveredCoordinatesE6.lonmaxE6/1e6d;

                if (firstItemOutside != null) {
                    double currentSortedOutlineDistanceLat = comparator.squareDist(firstItemOutside.point);
                    double currentSortedOutlineDistanceLon = currentSortedOutlineDistanceLat / comparator.lonRatio;
                    double latMin = coords.latitude - currentSortedOutlineDistanceLat;
                    double latMax = coords.latitude + currentSortedOutlineDistanceLat;
                    double lonMin = coords.longitude - currentSortedOutlineDistanceLon;
                    double lonMax = coords.longitude + currentSortedOutlineDistanceLon;
                    if (latMin > outlineLatMin) outlineLatMin = latMin;
                    if (latMax < outlineLatMax) outlineLatMax = latMax;
                    if (lonMin > outlineLonMin) outlineLonMin = lonMin;
                    if (lonMax < outlineLonMax) outlineLonMax = lonMax;
                    Log.d(TAG, "outline until nearest point");
                } else {
                    Log.d(TAG, "max outline");
                }
                this.currentSortedOutline.add(new LatLng(outlineLatMin, outlineLonMin));
                this.currentSortedOutline.add(new LatLng(outlineLatMin, outlineLonMax));
                this.currentSortedOutline.add(new LatLng(outlineLatMax, outlineLonMax));
                this.currentSortedOutline.add(new LatLng(outlineLatMax, outlineLonMin));
                this.currentSortedOutline.add(this.currentSortedOutline.get(0));

                // let listeners know about this change (even if there are no car parks, the coords may have changed)
                synchronized(this) {
                    for (SortedCurrentItemsUpdateListener listener : this.updateListeners) {
                        listener.onSortedCurrentItemsUpdated();
                    }
                }
                Log.d(TAG, "recomputed (with " + count + " out of " + total + " tiles, " + retval.size() + " parkings) in " + (System.currentTimeMillis()-startTime) + "ms");
            } catch (InterruptedException e) {
                //                    Log.i(TAG, "thread interrupted, quitting");
                if (refresher != null) {
                    refresher.interrupt();
                    refresher = null;
                    refresherTargetTime = 0;
                }
                return;
            } catch (Exception e) {
                //                    Log.w(TAG, "thread almost died of exception", e);
            }
        }
    }

    public void onNewCoordinates(LatLng coords) {
        boolean added = this.eventQueue.offer(new Event(coords));
        if (!added) {
            //                Log.e(TAG, "event queue full, cannot add new rectangle!");
        }
    }

    public void onTimeToRefresh() {
        boolean added = this.eventQueue.offer(new Event());
        //            Log.d(TAG, "adding refresh event");
        if (!added) {
            //                Log.e(TAG, "event queue full, cannot add refresh request!");
        }
    }

    public void onTileUpdated(MapTile tile) {
        boolean added = this.eventQueue.offer(new Event(tile));
        if (!added) {
            //                Log.e(TAG, "event queue full, cannot add new tile!");
        }
    }

    public void onAllTileRefresh() {
        onTimeToRefresh();
    }

    private static class Event {
        enum Type { TILE_UPDATE, NEW_COORDINATES, REFRESH };
        final Type type;
        LatLng coords;
        MapTile tile;

        public Event(LatLng coords) {
            this.coords = coords;
            this.type = Type.NEW_COORDINATES;
        }

        public Event(MapTile tile) {
            this.tile = tile;
            this.type = Type.TILE_UPDATE;
        }

        public Event() {
            this.type = Type.REFRESH;
        }
    }

    public boolean isTileDesirable(MapTile tile) {
        MapRectangle currentCoveredE6 = this.currentCoveredCoordinatesE6;
        if (currentCoveredE6 == null) {
            return true;
        } else {
            return
                    tile.late6min >= currentCoveredE6.latminE6 &&
                    tile.late6min <= currentCoveredE6.latmaxE6 &&
                    tile.lone6min >= currentCoveredE6.lonminE6 &&
                    tile.lone6min <= currentCoveredE6.lonmaxE6;
        }
    }

    private final Set<SortedCurrentItemsUpdateListener> updateListeners = new HashSet<SortedCurrentItemsUpdateListener>();

    public synchronized void registerUpdateListener(SortedCurrentItemsUpdateListener listener) {
        this.updateListeners.add(listener);
    }
    public synchronized void unregisterUpdateListener(SortedCurrentItemsUpdateListener listener) {
        this.updateListeners.remove(listener);
    }
}
