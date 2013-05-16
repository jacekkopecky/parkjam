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

import uk.ac.open.kmi.parking.DrawableOverlayItem;
import uk.ac.open.kmi.parking.Parking;

/**
 * this thread computes in the background the car parks that are near the currently visible coordinates
 */
class SortingPrecomputationThread implements Runnable, TileUpdateListener, TileDesirabilityChecker {
    @SuppressWarnings("unused")
    private static final String TAG = "sorting thread";
    volatile Collection<DrawableOverlayItem> sortedCurrentItems = Collections.emptyList(); // HardwiredParkingList.listParkings();
    private volatile MapRectangle currentCoveredCoordinatesE6 = null;
    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(1000); // todo make this number configurable? also the number of updatedTiles below
    private TileDownloaderThread tileDownloader;

    public SortingPrecomputationThread(TileDownloaderThread tileDownloader) {
        this.tileDownloader = tileDownloader;
        tileDownloader.registerTileUpdateListener(this);
        tileDownloader.registerTileDesirabilityChecker(this);
    }

    // todo E0 is a bad mnemonic, it's actually E1, or rather E6/TILE_SIZE
    private int lastTileMinLatE0 = Integer.MIN_VALUE;
    private int lastTileMinLonE0 = Integer.MIN_VALUE;
    private int lastTileMaxLatE0 = Integer.MAX_VALUE;
    private int lastTileMaxLonE0 = Integer.MAX_VALUE;
    private MapRectangle lastCoordsE6 = null;

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
                //                    long currTime = System.currentTimeMillis();
                //                    if (currTime > (nextReportTime)) {
                //                        Log.d("parkme", "" + (currTime-nextReportTime+999) + "ms: " + callCount + " requests");
                //                        callCount=0;
                //                        nextReportTime=currTime+999;
                //                    }
                //                    callCount++;

                Event event = this.eventQueue.take();
                // in case the events are only tile updates, we want to act on the last coordinates
                MapRectangle coordsE6 = this.lastCoordsE6;
                boolean forceUpdate = false;
                do {
                    switch (event.type) {
                    case NEW_COORDINATES:
                        coordsE6 = event.coordsE6;
                        break;
                    case TILE_UPDATE:
                        this.updatedTiles.add(event.tile);
                        break;
                    case REFRESH:
                        forceUpdate = true;
                        //                            Log.d(TAG, "refreshing");
                        break;
                    }
                    event = this.eventQueue.poll();
                } while (event != null);

                if (coordsE6 == null) {
                    continue;
                }

                //                    long startTime = System.currentTimeMillis();

                forceUpdate |= this.lastCoordsE6 == null;
                if (!forceUpdate) {
                    for (MapTile tile : this.updatedTiles) {
                        int tileLatE0 = tile.late6min / ParkingsService.TILE_SIZE;
                        int tileLonE0 = tile.lone6min / ParkingsService.TILE_SIZE;
                        if (tileLatE0 >= this.lastTileMinLatE0 &&
                                tileLonE0 >= this.lastTileMinLonE0 &&
                                tileLatE0 <= this.lastTileMaxLatE0 &&
                                tileLonE0 <= this.lastTileMaxLonE0) {
                            forceUpdate = true;
                            break;
                        }
                    }
                }
                this.updatedTiles.clear();

                // we simply combine (and sort) the parks in the visible tiles and one beyond (at least 9 total)
                // there is a buffer zone in which the current-precomputed is still acceptable

                // the thread doesn't have to worry about combining/downloading too much - it will never be called if the map is zoomed too far out

                this.lastCoordsE6 = coordsE6;
                // new limits, the tiles that are at least partially visible
                int tileMinLatE0 = coordsE6.latmin / ParkingsService.TILE_SIZE;  if (coordsE6.latmin < 0) tileMinLatE0--;
                int tileMinLonE0 = coordsE6.lonmin / ParkingsService.TILE_SIZE; if (coordsE6.lonmin < 0) tileMinLonE0--;
                int tileMaxLatE0 = coordsE6.latmax / ParkingsService.TILE_SIZE; if (coordsE6.latmax < 0) tileMaxLatE0--;
                int tileMaxLonE0 = coordsE6.lonmax / ParkingsService.TILE_SIZE; if (coordsE6.lonmax < 0) tileMaxLonE0--;

                // check that the new request isn't already satisfied
                // the stuff with % is about whether more than a half of the border tile is visible (so we don't really necessarily have space next to it)
                if (forceUpdate ||
                        ((tileMinLatE0 == this.lastTileMinLatE0) && (coordsE6.latmin - tileMinLatE0*ParkingsService.TILE_SIZE)*2 < ParkingsService.TILE_SIZE) ||
                        (tileMinLatE0 < this.lastTileMinLatE0) ||
                        (tileMinLatE0 > (this.lastTileMinLatE0+2)) ||
                        ((tileMinLonE0 == this.lastTileMinLonE0) && (coordsE6.lonmin - tileMinLonE0*ParkingsService.TILE_SIZE)*2 < ParkingsService.TILE_SIZE) ||
                        (tileMinLonE0 < this.lastTileMinLonE0) ||
                        (tileMinLonE0 > (this.lastTileMinLonE0+2)) ||
                        ((tileMaxLatE0 == this.lastTileMaxLatE0) && (coordsE6.latmax - tileMaxLatE0*ParkingsService.TILE_SIZE)*2 > ParkingsService.TILE_SIZE) ||
                        (tileMaxLatE0 > this.lastTileMaxLatE0) ||
                        (tileMaxLatE0 < (this.lastTileMaxLatE0-2)) ||
                        ((tileMaxLonE0 == this.lastTileMaxLonE0) && (coordsE6.lonmax - tileMaxLonE0*ParkingsService.TILE_SIZE)*2 > ParkingsService.TILE_SIZE) ||
                        (tileMaxLonE0 > this.lastTileMaxLonE0) ||
                        (tileMaxLonE0 < (this.lastTileMaxLonE0-2))) {
                    // need to recompute

                    // add an extra buffer around the (at least partially) visible tiles
                    this.lastTileMinLatE0 = --tileMinLatE0;
                    this.lastTileMinLonE0 = --tileMinLonE0;
                    this.lastTileMaxLatE0 = ++tileMaxLatE0;
                    this.lastTileMaxLonE0 = ++tileMaxLonE0;

                    this.currentCoveredCoordinatesE6 =
                            new MapRectangle(
                                    tileMinLatE0 * ParkingsService.TILE_SIZE,
                                    tileMinLonE0 * ParkingsService.TILE_SIZE,
                                    tileMaxLatE0 * ParkingsService.TILE_SIZE,
                                    tileMaxLonE0 * ParkingsService.TILE_SIZE);

                    //                        Log.d(TAG, "coords " + coordsE6 + " lead to covered coordinates " + this.currentCoveredCoordinatesE6);

                    final boolean onlyConfirmed = !ParkingsService.get(null).getShowUnconfirmedCarparks();

                    TreeSet<DrawableOverlayItem> retval = new TreeSet<DrawableOverlayItem>(new DrawableOverlayItem.Comparator());
                    //                        int count=0; int total=0;
                    long minNextUpdateTime = Long.MAX_VALUE;
                    // todo this should go from the center, not from the corner
                    for (int latE0 = tileMinLatE0; latE0 <= tileMaxLatE0; latE0++) {
                        for (int lonE0 = tileMinLonE0; lonE0 <= tileMaxLonE0; lonE0++) {
                            MapTile tile = this.tileDownloader.getTile(latE0*ParkingsService.TILE_SIZE, lonE0*ParkingsService.TILE_SIZE);
                            //                                total++;
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
                                //                                    count++;
                            }
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
                        //                            Log.d(TAG, "next refresh time in " + sleepTime + "ms");
                        if (sleepTime <= 0) {
                            //                                Log.w(TAG, "next refresh hopefully in progress");
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

                    this.sortedCurrentItems = retval;
                    // let listeners know about this change, but only if there's anything to display - we don't expect removing items
                    if (!retval.isEmpty()) {
                        synchronized(this) {
                            for (SortedCurrentItemsUpdateListener listener : this.updateListeners) {
                                listener.onSortedCurrentItemsUpdated();
                            }
                        }
                    }
                    //                        Log.d(TAG, "recomputed (with " + count + " out of " + total + " tiles, " + retval.size() + " parkings) in " + (System.currentTimeMillis()-startTime) + "ms");
                } else {
                    // otherwise no need to do anything
                    //                        Log.i("parkme", "no recomputation for " + eventE6);
                }
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

    public void onNewCoordinates(MapRectangle coords) {
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
        MapRectangle coordsE6;
        MapTile tile;

        public Event(MapRectangle coords) {
            this.coordsE6 = coords;
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
                    tile.late6min >= currentCoveredE6.latmin &&
                    tile.late6min <= currentCoveredE6.latmax &&
                    tile.lone6min >= currentCoveredE6.lonmin &&
                    tile.lone6min <= currentCoveredE6.lonmax;
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
