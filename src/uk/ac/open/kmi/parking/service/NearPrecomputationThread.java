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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import uk.ac.open.kmi.parking.Parking;
import android.location.Location;

/**
 * this thread computes in the background the car parks that are near the current location of the user
 * todo nearest tiles don't seem to stay in LRUCache - can be solved by periodically doing get() on LRUCache with the near tiles
 */
class NearPrecomputationThread implements Runnable, TileUpdateListener, TileDesirabilityChecker {
    @SuppressWarnings("unused")
    private static final String TAG = "near-precompute thread";
    private Collection<Parking> nearCurrentItems = Collections.emptyList();
    volatile Parking currentNearestCarpark = null;
    private volatile MapRectangle currentCoveredCoordinatesE6 = null;
    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(1000); // todo make this number configurable? also the number of updatedTiles below
    private TileDownloaderThread tileDownloader;

    private static double maximumNearestDistance = 0;

    public static synchronized void setNearestDistance(double nearestCarparkDistance) {
        maximumNearestDistance = nearestCarparkDistance;
    }

    public NearPrecomputationThread(TileDownloaderThread tileDownloader) {
        this.tileDownloader = tileDownloader;
        tileDownloader.registerTileUpdateListener(this);
        tileDownloader.registerTileDesirabilityChecker(this);
    }

    // todo E0 is a bad mnemonic, it's actually E1, or rather E6/TILE_SIZE
    private int lastTileMinLatE0 = Integer.MIN_VALUE;
    private int lastTileMinLonE0 = Integer.MIN_VALUE;
    private int lastTileMaxLatE0 = Integer.MAX_VALUE;
    private int lastTileMaxLonE0 = Integer.MAX_VALUE;
    private Location lastCoords = null;

    private final List<MapTile> updatedTiles = new ArrayList<MapTile>(1000);
    private int maxQueueSize = 0;

    public void run() {
        for (;;) {
            try {
                int queueSize = this.eventQueue.size();
                if (queueSize > this.maxQueueSize) {
                    this.maxQueueSize = queueSize;
//                    Log.i(TAG, "queue size maximum increased to " + this.maxQueueSize);
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue - just under a 100 is OK because of tile update events from newly-loaded supertiles
                }

                boolean forceUpdate = false;

                Event event = this.eventQueue.take();
                // in case the events are only tile updates, we want to act on the last coordinates
                Location coords = this.lastCoords;
                do {
                    switch (event.type) {
                    case NEW_COORDINATES:
                        coords = event.coords;
                        break;
                    case TILE_UPDATE:
                        this.updatedTiles.add(event.tile);
                        break;
                    case REFRESH:
                        forceUpdate = true;
                        break;
                    }
                    event = this.eventQueue.poll();
                } while (event != null);

                if (coords == null) {
                    continue;
                }

                forceUpdate |= this.lastCoords == null;
                if (!forceUpdate) {
                    for (MapTile tile : this.updatedTiles) {
                        int tileLatE0 = tile.late6min / ParkingsService.TILE_SIZE_E6;
                        int tileLonE0 = tile.lone6min / ParkingsService.TILE_SIZE_E6;
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

                // for now we simply combines the parks in the 3x3 tiles centered on the coordinates
                // there is a buffer zone in which the current-precomputed is still acceptable
                // it can be optimized to 1-to-4 (depending on where in its tile the point is)


                this.lastCoords = coords;

                // the tiles that are at least partially visible
                int late6 = (int) Math.floor(coords.getLatitude()*1e6);
                int lone6 = (int) Math.floor(coords.getLongitude()*1e6);

                int tileLatE0 = late6 / ParkingsService.TILE_SIZE_E6; if (late6 < 0) tileLatE0--;
                int tileLonE0 = lone6 / ParkingsService.TILE_SIZE_E6; if (lone6 < 0) tileLonE0--;

                // the stuff with % is about whether more than a half of the border tile is visible (so we don't really necessarily have space next to it)

                // first check that the new request isn't already satisfied
                if (forceUpdate ||
                        ((tileLatE0 == this.lastTileMinLatE0) && (late6 - tileLatE0*ParkingsService.TILE_SIZE_E6)*2 < ParkingsService.TILE_SIZE_E6) ||
                        (tileLatE0 < this.lastTileMinLatE0) ||
                        ((tileLonE0 == this.lastTileMinLonE0) && (lone6 - tileLonE0*ParkingsService.TILE_SIZE_E6)*2 < ParkingsService.TILE_SIZE_E6) ||
                        (tileLonE0 < this.lastTileMinLonE0) ||
                        ((tileLatE0 == this.lastTileMaxLatE0) && (late6 - tileLatE0*ParkingsService.TILE_SIZE_E6)*2 > ParkingsService.TILE_SIZE_E6) ||
                        (tileLatE0 > this.lastTileMaxLatE0) ||
                        ((tileLonE0 == this.lastTileMaxLonE0) && (lone6 - tileLonE0*ParkingsService.TILE_SIZE_E6)*2 > ParkingsService.TILE_SIZE_E6) ||
                        (tileLonE0 > this.lastTileMaxLonE0)) {
                    // need to recompute

                    int tileMinLatE0 = tileLatE0-1;
                    int tileMinLonE0 = tileLonE0-1;
                    int tileMaxLatE0 = tileLatE0+1;
                    int tileMaxLonE0 = tileLonE0+1;

                    this.lastTileMinLatE0 = tileMinLatE0;
                    this.lastTileMinLonE0 = tileMinLonE0;
                    this.lastTileMaxLatE0 = tileMaxLatE0;
                    this.lastTileMaxLonE0 = tileMaxLonE0;

                    this.currentCoveredCoordinatesE6 =
                            new MapRectangle(
                                    tileMinLatE0 * ParkingsService.TILE_SIZE_E6,
                                    tileMinLonE0 * ParkingsService.TILE_SIZE_E6,
                                    tileMaxLatE0 * ParkingsService.TILE_SIZE_E6,
                                    tileMaxLonE0 * ParkingsService.TILE_SIZE_E6);

                    List<Parking> retval = new ArrayList<Parking>(200);
//                    int count=0; int total=0;
                    // todo this should go from the center, not from the corner
                    for (int latE0 = tileMinLatE0; latE0 <= tileMaxLatE0; latE0++) {
                        for (int lonE0 = tileMinLonE0; lonE0 <= tileMaxLonE0; lonE0++) {
                            MapTile tile = this.tileDownloader.getTile(latE0*ParkingsService.TILE_SIZE_E6, lonE0*ParkingsService.TILE_SIZE_E6);
//                            total++;
                            if (tile != null) {
                                retval.addAll(tile.parkings.values());
//                                count++;
                            }
                        }
                    }

                    this.nearCurrentItems = retval;
//                    Log.d(TAG, "recomputed (with " + count + " out of " + total + " tiles, " + retval.size() + " parkings) for " + coords);
                } else {
                    // otherwise no need to do anything
//                        Log.i(TAG, "no recomputation for " + event);
                }

                // recompute the nearest one here
                Parking newNearest = findNearestCarPark(coords); // todo this may not be necessary if the coordinates haven't changed and the tiles haven't changed
                if (this.currentNearestCarpark != newNearest) {
                    this.currentNearestCarpark = newNearest;
                    // let listeners know about this change
                    synchronized(this) {
                        for (NearbyCarparkUpdateListener listener : this.updateListeners) {
                            listener.onNearbyCarparkUpdated();
                        }
                    }
                }
//                Log.d(TAG, "found new nearest " + newNearest + " for " + coords);
            } catch (InterruptedException e) {
//                Log.i(TAG, "thread interrupted, quitting");
                return;
            } catch (Exception e) {
//                Log.w(TAG, "thread almost died of exception", e);
            }
        }
    }

    public void onNewCoordinates(Location coords) {
        boolean added = this.eventQueue.offer(new Event(coords));
        if (!added) {
//            Log.e(TAG, "event queue full, cannot add new rectangle!");
        }
    }

    public void onTileUpdated(MapTile tile) {
        boolean added = this.eventQueue.offer(new Event(tile));
        if (!added) {
//            Log.e(TAG, "event queue full, cannot add new tile!");
        }
    }

    public void onAllTileRefresh() {
        boolean added = this.eventQueue.offer(new Event());
        if (!added) {
//            Log.e(TAG, "event queue full, cannot add new tile!");
        }
    }

    public Parking findNearestCarPark(Location location) {
        Parking currentCarpark = null;
        if (location != null) {
            float[] distresult = new float[1];

            final boolean onlyConfirmed = !ParkingsService.get(null).getShowUnconfirmedCarparks();

            Collection<Parking> parkings = this.nearCurrentItems;
            double distance = maximumNearestDistance;
            for (Parking parking : parkings) {
                if (onlyConfirmed && parking.unconfirmed) {
                    continue;
                }
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), parking.point.latitude, parking.point.longitude, distresult);
                if (distresult[0] < distance) {
                    distance = distresult[0];
                    currentCarpark = parking;
                }
            }
        }
        return currentCarpark;
    }

    private static class Event {
        enum Type { TILE_UPDATE, NEW_COORDINATES, REFRESH };
        final Type type;
        Location coords;
        MapTile tile;

        public Event(Location coords) {
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

    private final Set<NearbyCarparkUpdateListener> updateListeners = new HashSet<NearbyCarparkUpdateListener>();

    public synchronized void registerUpdateListener(NearbyCarparkUpdateListener listener) {
        this.updateListeners.add(listener);
    }
    public synchronized void unregisterUpdateListener(NearbyCarparkUpdateListener listener) {
        this.updateListeners.remove(listener);
    }
}
