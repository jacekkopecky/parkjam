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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import uk.ac.open.kmi.parking.LoadingStatus;
import uk.ac.open.kmi.parking.Onto;
import uk.ac.open.kmi.parking.Parking;
import uk.ac.open.kmi.parking.Parking.Availability;
import uk.ac.open.kmi.parking.service.RememberedCarparks.ParkingLite;
import android.net.Uri;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

class TileDownloaderThread implements Runnable {
//    @SuppressWarnings("unused")
    private static final String TAG = "tile downloader";

    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(1000); // todo make this number configurable?
    private final Set<Event> eventPresenceSet = Collections.synchronizedSet(new HashSet<Event>(1000));

    private static final int SUPERTILE_FACTOR = 10;
    private static final int SUPERTILE_SIZE = ParkingsService.TILE_SIZE * SUPERTILE_FACTOR;

    // ten supertiles in the cache - up to four around the current location, six in browsing the map
    private final LRUCache<MapTile> cache = new LRUCache<MapTile>(10*SUPERTILE_FACTOR*SUPERTILE_FACTOR);

//    private int downloadCount = 0;
    private int maxQueueSize = 0;
    private long maxTimeToTake = 0;

    private final RememberedCarparks rememberedCarparks;

    public TileDownloaderThread(RememberedCarparks rc) {
        this.rememberedCarparks = rc;
    }

    private boolean loading = false;

    volatile boolean loadedSomeCarparks = false;

    public void run() {
        this.loading = false;
        for (;;) {
            try {
                int queueSize = this.eventQueue.size();
                if (queueSize > this.maxQueueSize) {
                    this.maxQueueSize = queueSize;
//                    Log.i(TAG, "queue size maximum increased to " + this.maxQueueSize);
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue
                    // todo do the same thing wherever I have an event queue
                }

                if (this.eventQueue.isEmpty() && this.loading) {
                    LoadingStatus.stoppedLoading();
                    this.loading = false;
                }
                Event event = this.eventQueue.take();

                this.eventPresenceSet.remove(event);

                long time = System.currentTimeMillis();

                MapTile tile = event.tile;
                boolean justRefresh = false;
                synchronized(this) {
                    MapTile old = this.cache.get(tile);
                    if (old != null && old.nextUpdate > time) {
                        if (event.type == Type.REFRESH_REMEMBERED) {
                            justRefresh = true;
                            tile = old;
                        } else {
//                            Log.d(TAG, "tile already downloaded: " + tile);
                            continue;
                        }
                    }
                }

                long timeToTake = time - event.timeMillis;
                if (timeToTake > this.maxTimeToTake) {
                    this.maxTimeToTake = timeToTake;
//                    Log.i(TAG, "queue maximum time to take up an event increased to " + this.maxTimeToTake + "ms");
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue
                    // todo maybe do the same thing wherever I have an event queue with longer-running event handling
                }
//                Log.d(TAG, "tile request " + tile + " taken up " + timeToTake + "ms after enqueued");

                // check desirability if the refresh is not otherwise necessary
                if (event.type != Type.REFRESH_REMEMBERED) {
                    boolean undesirable = true;
                    synchronized(this) {
                        for (TileDesirabilityChecker checker : this.tileDesirabilityCheckers) {
                            if (checker.isTileDesirable(tile)) {
                                undesirable = false;
                                break; // tile desirable for at least one party, that'll do
                            }
                        }
                    }
                    if (undesirable) {
    //                    Log.d(TAG, "tile no longer desirable: " + tile);
                        continue;
                    }
                }

                if (!this.loading) {
                    LoadingStatus.startedLoading();
                    this.loading = true;
                }

                MapTile supertile = new MapTile();
                supertile.late6min = tile.late6min - (tile.late6min >= 0 ? tile.late6min % SUPERTILE_SIZE : (tile.late6min+1) % SUPERTILE_SIZE + SUPERTILE_SIZE-1);
                supertile.lone6min = tile.lone6min - (tile.lone6min >= 0 ? tile.lone6min % SUPERTILE_SIZE : (tile.lone6min+1) % SUPERTILE_SIZE + SUPERTILE_SIZE-1);

//                Log.d(TAG, "tile " + tile + " makes supertile " + supertile);

                MapTile[][] newtiles = new MapTile[SUPERTILE_FACTOR][SUPERTILE_FACTOR];
                if (justRefresh) {
                    // put in newtiles the one that we're refreshing in its rightful position
                    int i = (tile.late6min - supertile.late6min) / ParkingsService.TILE_SIZE;
                    int j = (tile.lone6min - supertile.lone6min) / ParkingsService.TILE_SIZE;
                    if (i < 0 || j < 0 || i >= SUPERTILE_FACTOR || j >= SUPERTILE_FACTOR) {
                        Log.e(TAG, "supertile doesn't contain the tile we're refreshing: tile " + tile + " supertile + " + supertile + " factor " + SUPERTILE_FACTOR);
                    } else {
                        newtiles[i][j] = tile;
                    }
                } else {
                    String uri = Config.SERVER + "availetc?late6min=" + supertile.late6min +
                            "&lone6min=" + supertile.lone6min +
                            "&late6max=" + (supertile.late6min+SUPERTILE_SIZE) +
                            "&lone6max=" + (supertile.lone6min+SUPERTILE_SIZE);
                    InputStream is = FileManager.get().open(uri);
                    if (is != null) {
                        Model model = ModelFactory.createDefaultModel();
                        model.read(is, uri, "TURTLE");
//                        this.downloadCount++;

                        time = System.currentTimeMillis();
//                        Log.d(TAG, "supertile " + this.downloadCount + " read " + (time - event.timeMillis) + "ms after request enqueued");

                        for (int i=0; i<SUPERTILE_FACTOR; i++) {
                            for (int j=0; j<SUPERTILE_FACTOR; j++) {
                                MapTile newtile = new MapTile();
                                newtile.lastUpdate = time;
                                newtile.nextUpdate = time + Config.DEFAULT_TILE_TTL; // todo this should come from HTTP caching info
                                newtile.late6min = supertile.late6min + i*ParkingsService.TILE_SIZE;
                                newtile.lone6min = supertile.lone6min + j*ParkingsService.TILE_SIZE;
                                newtile.parkings = new HashMap<String, Parking>(200);
                                newtiles[i][j] = newtile;
//                                Log.v(TAG, "added new tile " + newtile);
                            }
                        }

                        for (ResIterator parkings = model.listResourcesWithProperty(RDF.type, Onto.LGO_Parking); parkings.hasNext(); ) {
                            Resource parking = parkings.next();
                            ParkingInformation pinfo = ParkingInformation.parse(parking, model);
                            if (pinfo == null) {
                                continue;
                            }
                            Parking newparking = pinfo.createParking();
                            int i = (pinfo.late6-supertile.late6min)/ParkingsService.TILE_SIZE;
                            if (pinfo.late6<supertile.late6min) i--;
                            int j = (pinfo.lone6-supertile.lone6min)/ParkingsService.TILE_SIZE;
                            if (pinfo.lone6<supertile.lone6min) j--;
                            if (i<0 || i >=SUPERTILE_FACTOR || j<0 || j >= SUPERTILE_FACTOR) {
//                                Log.w(TAG, "parking from server is not in supertile: " + pinfo.id);
                                continue;
                            }
//                            Log.v(TAG, "added parking in tile " + i + "," + j);
                            newtiles[i][j].parkings.put(pinfo.id, newparking);
                        }
                        this.loadedSomeCarparks = true;

//                        Log.d(TAG, "parsing took " + (-time + (time=System.currentTimeMillis())) + "ms");
                    } else {
//                        Log.e(TAG, "jena cannot read " + uri);
                        synchronized(this) {
                            MapTile old = this.cache.get(tile);
                            if (old != null) {
                                old.nextUpdate = time+Config.DEFAULT_NETWORK_PROBLEM_DELAY; // if it couldn't be read, don't try to update immediately again -- todo this should be propagated to all the tiles in this tile's supertile
                            }
                        }
                    }
                }

                addRememberedCarparks(supertile, newtiles, !justRefresh);

                // the extra carpark that triggered this update should now be loaded
                if (event.extraCarpark != null) {
                    Parking p = Parking.getParking(event.extraCarpark);
                    if (p != null) {
                        p.details = event.extraData;
                    }
                    if (event.extraListener != null) {
                        event.extraListener.onCarparkInformationUpdated(p);
                        event.extraListener = null;
                    }
                }

                // parsing done, handle the tiles
                synchronized(this) { // for cache access and for listeners access
                    for (int i=0; i<SUPERTILE_FACTOR; i++) {
                        for (int j=0; j<SUPERTILE_FACTOR; j++) {
                            MapTile newtile = newtiles[i][j];
                            if (newtile != null) {
                                this.cache.add(newtile);
                            }
                        }
                    }
                    for (int i=0; i<SUPERTILE_FACTOR; i++) {
                        for (int j=0; j<SUPERTILE_FACTOR; j++) {
                            MapTile newtile = newtiles[i][j];
                            if (newtile != null) {
                                for (TileUpdateListener listener : this.tileUpdateListeners) {
                                    listener.onTileUpdated(newtile);
                                }
                            }
                        }
                    }
                }
//                Log.d(TAG, "caching and tile update listeners took " + (System.currentTimeMillis() - time) + "ms");
            } catch (InterruptedException e) {
//                Log.i(TAG, "thread interrupted, quitting");
                break;
            } catch (Exception e) {
//                Log.w(TAG, "thread almost died of exception", e);
            }
        }
        if (this.loading) {
            LoadingStatus.stoppedLoading();
            this.loading = false;
        }
    }

    synchronized void refreshAllListeners() {
        for (TileUpdateListener listener : this.tileUpdateListeners) {
            listener.onAllTileRefresh();
        }
    }

    /**
     * checks remembered carparks that fit in the given supertile and loads/adds/forgets them, as appropriate
     */
    private void addRememberedCarparks(MapTile supertile, MapTile[][] newtiles, boolean newlyLoaded) {
        Collection<ParkingLite> toAdd = this.rememberedCarparks.listAddedCarparks();
        List<ParkingLite> toForget = new ArrayList<ParkingLite>(toAdd.size());
        for (ParkingLite remembered : toAdd) {
            // check if the position is in the current supertile:
            if (remembered.point.getLatitudeE6()<supertile.late6min) continue;
            int i = (remembered.point.getLatitudeE6()-supertile.late6min)/ParkingsService.TILE_SIZE;
            if (remembered.point.getLongitudeE6()<supertile.lone6min) continue;
            int j = (remembered.point.getLongitudeE6()-supertile.lone6min)/ParkingsService.TILE_SIZE;
            if (i >=SUPERTILE_FACTOR || j >= SUPERTILE_FACTOR) {
                // this car park not in the current supertile
                continue;
            }

            if (newtiles[i][j] == null) {
                // this tile is missing but not interesting
                continue;
            }

            if (newtiles[i][j].parkings.containsKey(remembered.id)) {
                if (remembered.listener != null) {
                    remembered.listener.onCarparkInformationUpdated(newtiles[i][j].parkings.get(remembered.id));
                    remembered.listener = null;
                }
                if (newlyLoaded) {
                    toForget.add(remembered);
                }
            } else {
                // if we don't already know it (Parking.getParking), load it from its URI
                Parking newparking = Parking.getParking(remembered.uri);
                if (newparking == null) {
                    newparking = readCarpark(remembered.id);
                    if (newparking == null) {
                        Log.w(TAG, "cannot load carpark " + remembered.id);
                        if (remembered.listener != null) {
                            remembered.listener.onCarparkInformationUpdated(null);
                            remembered.listener = null;
                        }
                        continue;
                    }
                }
                // add it to the appropriate tile, tell the listener (if any) and forget the listener
                newtiles[i][j].parkings.put(remembered.id, newparking);
                if (remembered.listener != null) {
                    remembered.listener.onCarparkInformationUpdated(newparking);
                    remembered.listener = null;
                }
            }
        }
        this.rememberedCarparks.removeAllAddedCarparks(toForget);
    }

    private Parking readCarpark(String uri) {
//        Log.d(TAG, "loading new carpark from " + uri);
        // load the new car park

        try {
            InputStream is = FileManager.get().open(uri);
            if (is != null) {
                Model model = ModelFactory.createDefaultModel();
                model.read(is, uri, "TURTLE");

                ParkingInformation pinfo = ParkingInformation.parse(model.getResource(uri), model);
                if (pinfo == null) {
                    Log.e(TAG, "couldn't parse RDF parking information for " + uri + " - model statement count: " + model.size());
                    return null;
                }

                long time = System.currentTimeMillis();

                Parking newparking = pinfo.createParking();

                if (!newparking.hasAnyTitle()) {
                    DetailsAndAvailabilityThread.geocodeCarparkTitle(newparking);
                }

                newparking.details = model;
                newparking.lastDetailsUpdate = time;
                newparking.nextDetailsUpdate = time+Config.DEFAULT_DETAILS_TTL; // todo this should be taken from HTTP caching info
                // todo is initial availability TTL of 5s a good value? it should be the same as the smallest value the server would return from PAVAIL
                return newparking;
            } else {
                Log.e(TAG, "cannot read " + uri);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "reading carpark " + uri + " failed with exception ", e);
            return null;
        }
    }




    private final MapTile cacheTemplate = new MapTile();

    /**
     * returns a map tile with car parks
     * @param latE6 must be in multiples of TILE_SIZE
     * @param lonE6 must be in multiples of TILE_SIZE
     * @return the tile, if downloaded
     */
    public synchronized MapTile getTile(int latE6, int lonE6) {
        this.cacheTemplate.late6min = latE6;
        this.cacheTemplate.lone6min = lonE6;
        long time = System.currentTimeMillis();
        MapTile retval = this.cache.get(this.cacheTemplate);
//        if (retval != null) {
//            Log.d(TAG, "tile's update in " + (retval.nextUpdate-time) + "ms");
//        }
        if (retval == null || time > retval.nextUpdate) {
            Event request = new Event(new MapTile(this.cacheTemplate), System.currentTimeMillis());
            if (!this.eventPresenceSet.contains(request)) {
                this.eventPresenceSet.add(request);
                // todo only enqueue if the thread is running, otherwise loudly ignore?
                while (!this.eventQueue.offer(request)) {
//                    Log.e(TAG, "event queue full, removing old entry!");
                    Event old = this.eventQueue.poll();
                    this.eventPresenceSet.remove(old);
                }
//                Log.v(TAG, "added event " + request.tile + " from ", new Exception());
            }
        }
        return retval;
    }

    /**
     * triggers a refresh of the tile that contains the given point; this doesn't necessarily reload the tile itself (unless it's expired) but it re-checks remembered carparks
     * @param point a point somewhere whose map tile should be refreshed
     * @param extraId the URI ID of the extra car park whose data we've already loaded and which we should give to extraListener (may be null)
     * @param extraData data already loaded for the extraId carpark
     * @param extraListener who should be notified when the extraId carpark is actually loaded
     */
    public synchronized void refreshTile(GeoPoint point, Uri extraId, Model extraData, CarparkDetailsUpdateListener extraListener) {
        // check the tile and load all remembered but unloaded car parks; forget any remembered car parks loaded in normal tile; and then tell the listener and drop the listener from remembered carparks
        int tileLatE0 = point.getLatitudeE6() / ParkingsService.TILE_SIZE; if (point.getLatitudeE6() < 0) tileLatE0--;
        int tileLonE0 = point.getLongitudeE6() / ParkingsService.TILE_SIZE; if (point.getLongitudeE6() < 0) tileLonE0--;

        this.cacheTemplate.late6min = tileLatE0 * ParkingsService.TILE_SIZE;
        this.cacheTemplate.lone6min = tileLonE0 * ParkingsService.TILE_SIZE;
        long time = System.currentTimeMillis();
        MapTile retval = this.cache.get(this.cacheTemplate);
        Event request = new Event(new MapTile(this.cacheTemplate), time);
        if (retval != null && time <= retval.nextUpdate) {
            request.type = Type.REFRESH_REMEMBERED;
        }

        if (extraId != null) {
//            Log.d(TAG, "new refresh with an extra carpark " + extraId);
            request.extraCarpark = extraId;
            request.extraData = extraData;
            request.extraListener = extraListener;
            request.type = Type.REFRESH_REMEMBERED;
        }

        if (!this.eventPresenceSet.contains(request)) {
            this.eventPresenceSet.add(request);
            // todo only enqueue if the thread is running, otherwise loudly ignore?
            while (!this.eventQueue.offer(request)) {
                Event old = this.eventQueue.poll();
                this.eventPresenceSet.remove(old);
            }
        }
    }

    private final Set<TileUpdateListener> tileUpdateListeners = new HashSet<TileUpdateListener>();
    private final Set<TileDesirabilityChecker> tileDesirabilityCheckers = new HashSet<TileDesirabilityChecker>();

    public synchronized void registerTileUpdateListener(TileUpdateListener listener) {
        this.tileUpdateListeners.add(listener);
    }

    public synchronized void registerTileDesirabilityChecker(TileDesirabilityChecker checker) {
        this.tileDesirabilityCheckers.add(checker);
    }

    private static enum Type { LOAD_TILE, REFRESH_REMEMBERED }

    private static class Event {
        public Type type;
        public MapTile tile;
        public long timeMillis;

        public Uri extraCarpark = null;
        public Model extraData = null;
        public CarparkDetailsUpdateListener extraListener = null;

        public Event(MapTile tile, long time) {
            this.tile = tile;
            this.timeMillis = time;
            this.type = Type.LOAD_TILE;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Event)) {
                return false;
            }
            Event e = (Event) o;
            return this.tile.equals(e.tile) && (this.extraCarpark == null ? e.extraCarpark == null : this.extraCarpark.equals(e.extraCarpark));
        }

        @Override
        public int hashCode() {
            return this.tile.hashCode();
        }
    }

    public static class ParkingInformation {
        private double lat = Double.NaN;
        private double lon = Double.NaN;

        public int late6 = 0;
        public int lone6 = 0;
        public Availability availability = Availability.UNKNOWN;
        public String title = null;
        public String id = null;
        public String availabilityResource = null;
        public String updateResource = null;
        public Property titleProperty = null;
        public Long timestamp = null;
        public boolean unconfirmed;

        public static ParkingInformation parse(Resource parking, Model model) {
            // todo error logging/handling
            if (parking == null) {
                return null;
            }
            ParkingInformation p = new ParkingInformation();
            p.id = parking.getURI();
            for (StmtIterator stmts = model.listStatements(parking, null, (RDFNode)null); stmts.hasNext(); ) {
                Statement s = stmts.next();
                if (s.getPredicate().equals(Onto.GEOPOS_lat)) {
                    p.lat = s.getDouble();
                } else if (s.getPredicate().equals(Onto.GEOPOS_long)) {
                    p.lon = s.getDouble();
                } else if (s.getPredicate().equals(RDFS.label)) {
                    p.title = s.getString();
                    p.titleProperty = RDFS.label;
                    // todo also take into account any properties with order 1.0d in presentation ontology - but they aren't returned by availetc are they?
                } else if (s.getPredicate().equals(Onto.PARKING_binaryAvailability)) {
                    p.availability = s.getBoolean() ? Availability.AVAILABLE : Availability.FULL ;
                    if (p.timestamp == null) {
                        p.timestamp = Long.MIN_VALUE;
                    }
                } else if (s.getPredicate().equals(Onto.PARKING_binaryAvailabilityTimestamp)) {
                    if (s.getObject().isLiteral() && XSD.dateTime.hasURI(s.getLiteral().getDatatypeURI())) {
                        Object val = s.getLiteral().getValue();
                        if (val instanceof XSDDateTime) {
                            XSDDateTime dtval = (XSDDateTime) val;
                            Calendar cal = dtval.asCalendar();
                            cal.setTimeZone(Config.UTC);
                            p.timestamp = cal.getTimeInMillis();
                        }
                    }
                } else if (s.getPredicate().equals(Onto.PARKING_availabilityResource)) {
                    p.availabilityResource = s.getResource().getURI();
                } else if (s.getPredicate().equals(Onto.PARKING_updateResource)) {
                    p.updateResource = s.getResource().getURI();
                } else if (s.getPredicate().equals(RDF.type) && s.getObject().equals(Onto.PARKING_UnverifiedInstance)) {
                    p.unconfirmed = true;
                }
            }

            if (p.title == null) {
                // try to find an unconfirmed title
                for (NodeIterator it = model.listObjectsOfProperty(parking, Onto.PARKING_hasUnverifiedProperties); it.hasNext(); ) {
                    RDFNode node = it.next();
                    if (node.isResource()) {
                        NodeIterator titles = model.listObjectsOfProperty((Resource)node, RDFS.label);
                        // todo above assuming only RDFS.label for an unconfirmed name property, which is likely OK because only that can be submitted by ParkJam
                        if (titles.hasNext()) {
                            String potentialTitle = titles.next().toString();
                            // ignore submitted empty titles
                            if (potentialTitle.trim().length() != 0) {
                                p.title = potentialTitle;
                                p.titleProperty = RDFS.label;
                                break;
                            }
                        }
                    }
                }
            }

            if (p.title == null) {
                p.title = ParkingsService.get(null).formatUnknownCarparkTitle(p.id.substring(p.id.lastIndexOf('/')+1));
            }

            // todo this shouldn't be here - the server should give us this URI, and the same for updateResource below
            if (p.availabilityResource == null) {
                p.availabilityResource = Config.SERVER + "parks/" + p.id.substring(p.id.lastIndexOf('/')+1) + "/avail";
            }

            if (p.updateResource == null) {
                p.updateResource = p.id;
            }

            if (p.lat==Double.NaN || p.lon==Double.NaN) {
//                Log.w(TAG, "parking " + p.id + " doesn't have both geo properties (" + p.lat + ", " + p.lon + ")");
                return null;
            }
            p.late6 = (int) Math.floor(p.lat*1e6);
            p.lone6 = (int) Math.floor(p.lon*1e6);
            return p;
        }

        public Parking createParking() {
            // todo is initial availability TTL of 5s a good value? it should be the same as the smallest value the server would return from PAVAIL
            return new Parking(new GeoPoint(this.late6, this.lone6),
                    this.title,
                    Uri.parse(this.id),
                    this.availabilityResource,
                    this.updateResource,
                    this.availability,
                    this.timestamp,
                    Config.DEFAULT_AVAIL_TTL,
                    this.titleProperty,
                    this.unconfirmed);
        }
    }
}
