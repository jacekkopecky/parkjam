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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import uk.ac.open.kmi.parking.LoadingStatus;
import uk.ac.open.kmi.parking.Onto;
import uk.ac.open.kmi.parking.Parking;
import uk.ac.open.kmi.parking.Parking.Availability;
import uk.ac.open.kmi.parking.service.TileDownloaderThread.ParkingInformation;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

class DetailsAndAvailabilityThread implements Runnable {
//    @SuppressWarnings("unused")
    private static final String TAG = "details and availability";

    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(100); // todo make this number configurable?
    // todo this queue should probably be a deque and work like a LIFO stack - last queries probably have the highest priority (but Deque is Android API 9+)
    private final Set<Event> eventPresenceSet = Collections.synchronizedSet(new HashSet<Event>(100));

    private final ParkingsService parkingsService;

    public DetailsAndAvailabilityThread(ParkingsService ps) {
        this.parkingsService = ps;
    }

//    private int downloadCount = 0;
    private int maxQueueSize = 0;
    private long maxTimeToTake = 0;

    // todo we may need an LRUCache of Parkings with details so that recent car parks are guaranteed to stay around -- maybe we don't because the parkings will be held elsewhere

    public void run() {
        boolean loading = false;
        for (;;) {
            try {
                int queueSize = this.eventQueue.size();
                if (queueSize > this.maxQueueSize) {
                    this.maxQueueSize = queueSize;
//                    Log.i(TAG, "queue size maximum increased to " + this.maxQueueSize);
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue
                }

                Event event = this.eventQueue.take();
                this.eventPresenceSet.remove(event);

                long time = System.currentTimeMillis();
                long timeToTake = time - event.timeMillis;
                if (timeToTake > this.maxTimeToTake) {
                    this.maxTimeToTake = timeToTake;
//                    Log.i(TAG, "queue maximum time to take up an event increased to " + this.maxTimeToTake + "ms");
                    // todo check that this is sane over longer lives of the app, possibly lower the size of the queue
                }
//                Log.d(TAG, "parking update request " + event.p.id + " taken up " + timeToTake + "ms after enqueued");

                LoadingStatus.startedLoading();
                loading = true;
                switch (event.type) {
                case SUBMIT_AVAILABILITY:
                    handleSubmitAvailabilityRequest(event);
                    break;
                case UPDATE_DETAILS:
                    handleUpdateDetailsRequest(event);
                    break;
                case UPDATE_AVAILABILITY:
                    handleUpdateAvailabilityRequest(event);
                    break;
                case SUBMIT_PROPERTY:
                    handleSubmitPropertyRequest(event);
                    break;
                case SUBMIT_CARPARK:
                    handleSubmitCarparkRequest(event);
                    break;
                case LOAD_EXTRA:
                    handleLoadExtraCarparkRequest(event);
                    break;
                }
            } catch (InterruptedException e) {
//                Log.i(TAG, "thread interrupted, quitting");
                break;
            } catch (Exception e) {
                Log.w(TAG, "thread almost died of exception", e);
            } finally {
                if (loading) {
                    LoadingStatus.stoppedLoading();
                    loading = false;
                }
            }
        }
    }

    /**
     * submit new car park data
     */
    private void handleSubmitCarparkRequest(Event event) {
//        Log.d(TAG, "adding car park");
        LatLng point = event.addparkPoint;

        // prepare the RDF for the data
        Model model = ModelFactory.createDefaultModel();
        Resource parking = model.createResource();
        model.add(parking, RDF.type, Onto.LGO_Parking);
        model.add(parking, Onto.GEOPOS_lat, model.createTypedLiteral(String.valueOf(point.latitude), XSDDatatype.XSDdouble));
        model.add(parking, Onto.GEOPOS_long, model.createTypedLiteral(String.valueOf(point.longitude), XSDDatatype.XSDdouble));

        try {
            // submit this to the server
//            Log.d(TAG, "submitting");
            URL submitURI = new URL(Config.SERVER + "parks");
            HttpURLConnection conn = (HttpURLConnection) submitURI.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "text/turtle");
            OutputStream out = conn.getOutputStream();
            model.write(out, "TURTLE");
            out.close();

            // handle the response
            int responseCode = conn.getResponseCode();
//            Log.d(TAG, "submitted car park data, got response code " + responseCode);
            if (responseCode != 201) {
                StringBuilder sb = new StringBuilder("submitting carpark to " + submitURI + " not 201 Created (actual response code " + responseCode + ")");
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                if (scanner.hasNext()) {
                    sb.append(": \n");
                    sb.append(scanner.next());
                }
                in.close();
                Log.w(TAG, sb.toString());
                // todo toast that there was an error
            }

            // got 201 response, get Location and load the car park from there
            String location = conn.getHeaderField("Location");
            if (location == null || location.equals("")) {
                Log.e(TAG, "submitted car park but got empty location back!");
            } else {
                this.parkingsService.rememberedCarparks.rememberAddedCarpark(location, point, event.updateListener);
                this.parkingsService.triggerTileRefresh(point);
            }
            conn.disconnect();
        } catch (IOException e) {
            Log.w(TAG, "io exception while submitting data: ", e);
            // todo toast that there was an error
        }
    }

    private void handleSubmitAvailabilityRequest(Event event) {
        Parking p = event.p;
        if (p.availabilityResource == null) {
            // this is an external parking with no availability resource to submit it to
            Log.w(TAG, "cannot submit data to a parking " + p.id + " without availability resource");
            return;
        }

        // prepare the RDF for the observation
        Model model = ModelFactory.createDefaultModel();
        Resource observation = model.createResource();
        model.add(observation, RDF.type, Onto.PARKING_AvailabilityObservation);
        model.add(observation, Onto.SSN_featureOfInterest, model.createResource(p.id.toString()));
        Calendar timestamp = new GregorianCalendar();
        timestamp.setTimeInMillis(event.binaryAvailabilityTimestamp);
        model.add(observation, Onto.SSN_observationSamplingTime, model.createTypedLiteral(timestamp));

        Resource submission = model.createResource();
        model.add(observation, Onto.SSN_observationResult, submission);
        model.add(submission, RDF.type, Onto.PARKING_AvailabilitySubmission);

        Resource estimate = model.createResource();
        model.add(submission, Onto.SSN_hasValue, estimate);
        model.add(estimate, RDF.type, Onto.PARKING_AvailabilityEstimate);
        model.add(estimate, Onto.PARKING_binaryAvailability, model.createTypedLiteral(event.binaryAvailability));

        try {
            // submit this to the server
            URL updateURI = new URL(p.availabilityResource);
            HttpURLConnection conn = (HttpURLConnection) updateURI.openConnection();
            conn.setDoOutput(true);
//            conn.setChunkedStreamingMode(0);
            conn.setRequestProperty("Content-type", "text/turtle");
            OutputStream out = conn.getOutputStream();
            model.write(out, "TURTLE");
            out.close();

            // handle the response
            int responseCode = conn.getResponseCode();
//            Log.d(TAG, "submitted availability data, got response code " + responseCode);
            if (responseCode / 100 != 2) {
                StringBuilder sb = new StringBuilder("submitting availability to " + p.availabilityResource + " not OK (response code " + responseCode + ")");
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                if (scanner.hasNext()) {
                    sb.append(": \n");
                    sb.append(scanner.next());
                }
                in.close();
                Log.w(TAG, sb.toString());
                // todo toast that there was an error
            }

            // got 2xx response, ignoring the body - todo could update the freshness from http response, better than below?
            conn.disconnect();
        } catch (IOException e) {
            Log.w(TAG, "io exception while submitting data: ", e);
            // todo toast that there was an error
        }

        long time = System.currentTimeMillis();
        p.nextAvailUpdate = time + Config.DEFAULT_AVAIL_TTL; // todo wait again 5s for next availability refresh - but we should take my very recent explicit update as authoritative
//        Log.d(TAG, "parking submission for " + p.id + " finished " + (time - event.timeMillis) + "ms after request enqueued");

        synchronized(this) { // for listeners access
            for (CarparkAvailabilityUpdateListener listener : this.availabilityUpdateListeners) {
                listener.onCarparkAvailabilityUpdated(p);
            }
        }
    }

    private void handleSubmitPropertyRequest(Event event) {
        Parking p = event.p;
        if (p.updateResource == null) {
            // this should not happen because if the resource doesn't tell us an update resource, we'll just use its address as the update resource, right?
//            Log.w(TAG, "cannot submit data to a parking " + p.id + " without update resource");
            return;
        }

        // prepare the RDF for the data
        Model model = ModelFactory.createDefaultModel();
        Resource parking = model.createResource(p.id.toString());
        model.add(parking, event.property, event.propertyValue);

        try {
            // submit this to the server
            URL updateURI = new URL(p.updateResource);
            HttpURLConnection conn = (HttpURLConnection) updateURI.openConnection();
            conn.setDoOutput(true);
//            conn.setRequestMethod("PATCH"); // todo should be PATCH but that's not supported here
            conn.setRequestProperty("Content-type", "text/turtle");
            OutputStream out = conn.getOutputStream();
            model.write(out, "TURTLE");
            out.close();

            // handle the response
            int responseCode = conn.getResponseCode();
//            Log.d(TAG, "submitted availability data, got response code " + responseCode);
            if (responseCode / 100 != 2) {
                StringBuilder sb = new StringBuilder("submitting property to " + p.updateResource + " not OK (response code " + responseCode + ")");
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                if (scanner.hasNext()) {
                    sb.append(": \n");
                    sb.append(scanner.next());
                }
                in.close();
                Log.w(TAG, sb.toString());
                // todo toast that there was an error
            }

            // got 2xx response, parse the body like it was a details update
            readCarparkDetails(p, p.updateResource, new BufferedInputStream(conn.getInputStream()));
            conn.disconnect();
        } catch (IOException e) {
            Log.w(TAG, "io exception while submitting data: ", e);
            // todo toast that there was an error
        }
    }

    private void handleUpdateDetailsRequest(Event event) {
        long time = System.currentTimeMillis();
        Parking p = event.p;

        // check that the parking hasn't been updated recently in which case ignore with debug message
        if (time < p.nextDetailsUpdate) {
//            Log.d(TAG, "parking details for " + p.id + " still fresh, next update expected in " + (p.nextDetailsUpdate - time) + "ms");
            return;
        }

        String uri = p.id.toString();

//        Log.d(TAG, "reading uri " + uri);
        InputStream is = FileManager.get().open(uri);
        if (is != null) {
            readCarparkDetails(p, uri, is);
        } else {
            Log.e(TAG, "cannot read " + uri);
            p.nextDetailsUpdate = time + Config.DEFAULT_NETWORK_PROBLEM_DELAY; // if the update failed, don't try to update it immediately again
        }
    }

    private void handleLoadExtraCarparkRequest(Event event) {
        // check that the parking isn't known yet
        Parking known = Parking.getParking(event.extraCarpark);
        if (known != null) {
            if (event.updateListener != null) {
                event.updateListener.onCarparkInformationUpdated(known);
                event.updateListener = null;
            }
            return;
        }

        String uri = event.extraCarpark.toString();

//        Log.d(TAG, "reading extra carpark from uri " + uri);
        InputStream is = FileManager.get().open(uri);
        if (is != null) {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, uri, "TURTLE");

            ParkingInformation pinfo = ParkingInformation.parse(model.getResource(uri), model);
            if (pinfo != null) {
                // get the point, give it and the id and the model to tiledownloaderthread
//                Log.d(TAG, "triggering tile refresh");
                this.parkingsService.triggerTileRefresh(new LatLng(pinfo.lat, pinfo.lon), event.extraCarpark, model, event.updateListener);
            }
        } else {
            Log.e(TAG, "cannot read extra carpark from " + uri);
        }
    }

    // parse and handle data about a car park, calls listeners
    private void readCarparkDetails(Parking p, String uri, InputStream is) {
        long time;
        Model model = ModelFactory.createDefaultModel();
        model.read(is, uri, "TURTLE");
//            this.downloadCount++;

        time = System.currentTimeMillis();
//            Log.d(TAG, "parking details nr " + this.downloadCount + " read " + (time - event.timeMillis) + "ms after request enqueued");

        ParkingInformation pinfo = ParkingInformation.parse(model.getResource(p.id.toString()), model);
        if (pinfo == null) {
            p.nextAvailUpdate = time + Config.DEFAULT_NETWORK_PROBLEM_DELAY; // if the update fails, don't try to update again immediately
            return;
        }
        if (p.point.latitude != pinfo.lat ||
                p.point.longitude != pinfo.lon) {
            Log.e(TAG, "parking location changed for " + p.id);
            // todo crucial information changed: tile downloader should flush/reload all? ParkingsService would potentially need to update current parking
        }

        if (!p.getTitle().equals(pinfo.title) && pinfo.titleProperty != null) {
            p.setTitle(pinfo.title, pinfo.titleProperty);
//            Log.d(TAG, "set title of car park @" + System.identityHashCode(p));
        }

        if (!p.hasAnyTitle()) {
            geocodeCarparkTitle(p);
        }

        p.details = model;
        p.lastDetailsUpdate = time;
        p.nextDetailsUpdate = time+Config.DEFAULT_DETAILS_TTL; // todo this should be taken from HTTP caching info

        synchronized(this) { // for listeners access
            for (CarparkDetailsUpdateListener listener : this.detailsUpdateListeners) {
                listener.onCarparkInformationUpdated(p);
            }
        }
//            Log.d(TAG, "update listeners (" + this.updateListeners.size() + ") took " + (System.currentTimeMillis() - time) + "ms");
    }

    static void geocodeCarparkTitle(Parking p) {
        final Geocoder geocoder = ParkingsService.get(null).geocoder;
        if (geocoder == null) {
            return;
        }
        try {
            List<Address> addrs = geocoder.getFromLocation(p.point.latitude, p.point.longitude, 1);
            if (addrs != null && addrs.size() != 0) {
//                Log.d(TAG, "geocoded address " + addrs.get(0));
                String addr = addrs.get(0).getAddressLine(0);
                if (addr != null && !"".equals(addr)) {
                    p.setTitle(addr, Onto.PARKING_geocodedTitle);
                }
            } else {
//                Log.d(TAG, "no address found for car park " + p);
            }
        } catch (IOException e) {
//            Log.d(TAG, "exception while geocoding car park " + p + " : ", e);
        }
    }

    private void handleUpdateAvailabilityRequest(Event event) {
        Parking p = Parking.getParking(event.p.id);
        if (p == null) {
            Log.w(TAG, "parking forgotten before handleUpdateAvailabilityRequest");
            return;
        }
        String resourceToRead = p.availabilityResource;
        if (resourceToRead == null) {
            // this is an external parking with no explicit availability resource
            // try simply to read the resource again
            resourceToRead = p.id.toString();
        }

        // check that the parking hasn't been updated recently in which case ignore with debug message
        long time = System.currentTimeMillis();
        if (time < p.nextAvailUpdate) {
//            Log.d(TAG, "parking availability for " + p.id + " still fresh, next update expected in " + (p.nextAvailUpdate - time) + "ms");
            return;
        }

        // todo this should add the trusted UDSs
//        Log.d(TAG, "reading uri " + resourceToRead);
        InputStream is = FileManager.get().open(resourceToRead);
        if (is != null) {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, resourceToRead, "TURTLE");
//            this.downloadCount++;

            time = System.currentTimeMillis();
//            Log.d(TAG, "parking availability nr " + this.downloadCount + " read " + (time - event.timeMillis) + "ms after request enqueued");

            ResIterator parkings = model.listResourcesWithProperty(RDF.type, Onto.LGO_Parking);
            if (!parkings.hasNext()) {
                Log.w(TAG, "loading " + resourceToRead + " didn't return any parking");
                p.nextAvailUpdate = time + Config.DEFAULT_SERVER_PROBLEM_DELAY; // if the server returns garbage, don't try again for a while
                return;
            }
            Resource parking = parkings.next();
            if (parkings.hasNext()) {
                Log.w(TAG, "loading " + resourceToRead + " returned multiple parkings");
                p.nextAvailUpdate = time + Config.DEFAULT_SERVER_PROBLEM_DELAY; // if the server returns garbage, don't try again for a while
                return;
            }
            if (!p.id.toString().equals(parking.getURI())) {
                Log.w(TAG, "loading " + resourceToRead + " returned information about parking different from the expected " + p.id);
                p.nextAvailUpdate = time + Config.DEFAULT_SERVER_PROBLEM_DELAY; // if the server returns garbage, don't try again for a while
                return;
            }

            StmtIterator availabilities = model.listStatements(parking, Onto.PARKING_binaryAvailability, (RDFNode)null);
            Statement avail = null;
            if (availabilities.hasNext()) {
                avail = availabilities.next();
            } else {
                Log.w(TAG, "loading " + resourceToRead + " didn't return any binary availaiblity");
            }
//            if (availabilities.hasNext()) {
//                Log.w(TAG, "loading " + resourceToRead + " returned multiple binary availabilities");
//                p.nextAvailUpdate = time + Config.DEFAULT_SERVER_PROBLEM_DELAY; // if the server returns garbage, don't try again for a while
//                return;
//            }

            StmtIterator availabilityTimestamps = model.listStatements(parking, Onto.PARKING_binaryAvailabilityTimestamp, (RDFNode)null);
            Statement availTimestamp = null;
            if (availabilityTimestamps.hasNext()) {
                availTimestamp = availabilityTimestamps.next();
            } else {
                Log.w(TAG, "loading " + resourceToRead + " didn't return any binary availaiblity timestamp");
            }
//            if (availabilityTimestamps.hasNext()) {
//                Log.w(TAG, "loading " + resourceToRead + " returned multiple binary availability timestamps");
//                p.nextAvailUpdate = time + Config.DEFAULT_SERVER_PROBLEM_DELAY; // if the server returns garbage, don't try again for a while
//                return;
//            }

            Availability newAvailability;
            Long newAvailabilityTimestamp = null;
            if (avail != null) {
                newAvailability = avail.getBoolean() ? Availability.AVAILABLE : Availability.FULL;
            } else {
                newAvailability = Availability.UNKNOWN;
            }
            if (availTimestamp != null) {
                Object val = availTimestamp.getLiteral().getValue();
                if (val instanceof XSDDateTime) {
                    XSDDateTime dtval = (XSDDateTime) val;
                    Calendar cal = dtval.asCalendar();
                    cal.setTimeZone(Config.UTC);
                    newAvailabilityTimestamp = cal.getTimeInMillis();
                }
            }

            p.setAvailability(newAvailability, newAvailabilityTimestamp) ;

            p.lastAvailUpdate = time;
            p.nextAvailUpdate = time+Config.DEFAULT_AVAIL_TTL; // todo take this from caching metadata

            synchronized(this) { // for listeners access
                for (CarparkAvailabilityUpdateListener listener : this.availabilityUpdateListeners) {
                    listener.onCarparkAvailabilityUpdated(p);
                }
            }
        } else {
            Log.e(TAG, "jena cannot read " + resourceToRead);
            p.nextAvailUpdate = time + Config.DEFAULT_NETWORK_PROBLEM_DELAY; // if the update fails, don't try to update again immediately
        }
    }

    private static class Event {
        public enum Type { UPDATE_DETAILS, SUBMIT_AVAILABILITY, UPDATE_AVAILABILITY, SUBMIT_PROPERTY, SUBMIT_CARPARK, LOAD_EXTRA }
        Type type;
        public Parking p;
        public long timeMillis;
        public boolean binaryAvailability;
        public Property property;
        public String propertyValue;
        public long binaryAvailabilityTimestamp;
        public LatLng addparkPoint;
        public CarparkDetailsUpdateListener updateListener;
        public Uri extraCarpark;

        private Event() {
            this.timeMillis = System.currentTimeMillis();
        }

        public static Event createSubmitCarparkEvent(LatLng point, CarparkDetailsUpdateListener listener) {
            Event retval = new Event();
            retval.addparkPoint = point;
            retval.updateListener = listener;
            retval.type = Type.SUBMIT_CARPARK;
            return retval;
        }

        public static Event createDetailsUpdateEvent(Parking p) {
            Event retval = new Event();
            retval.p = p;
            retval.type = Type.UPDATE_DETAILS;
            return retval;
        }

        public static Event createAvailabilityUpdateEvent(Parking p) {
            Event retval = new Event();
            retval.p = p;
            retval.type = Type.UPDATE_AVAILABILITY;
            return retval;
        }

        public static Event createSubmitAvailabilityEvent(Parking p, boolean availability, long timestamp) {
            Event retval = new Event();
            retval.p = p;
            retval.binaryAvailability = availability;
            retval.binaryAvailabilityTimestamp = timestamp;
            retval.type = Type.SUBMIT_AVAILABILITY;
            return retval;
        }

        public static Event createSubmitPropertyEvent(Parking p, Property prop, String value) {
            Event retval = new Event();
            retval.p = p;
            retval.property = prop;
            retval.propertyValue = value;
            retval.type = Type.SUBMIT_PROPERTY;
            return retval;
        }

        public static Event createLoadExtraCarparkEvent(Uri id, CarparkDetailsUpdateListener listener) {
            Event retval = new Event();
            retval.extraCarpark = id;
            retval.updateListener = listener;
            retval.type = Type.LOAD_EXTRA;
            return retval;
        }

        @Override
        public int hashCode() {
            if (this.p != null) {
                return this.p.hashCode();
            } else {
                return super.hashCode();
            }
        }
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Event)) {
                return false;
            }
            Event e = (Event) o;
            return this.type == e.type && this.binaryAvailability == e.binaryAvailability && (this.p == null ? e.p == null : this.p.equals(e.p));
        }
    }

    public void updateParkingDetails(Parking p) {
        enqueueEvent(Event.createDetailsUpdateEvent(p));
    }

    public void submitCarpark(LatLng point, CarparkDetailsUpdateListener listener) {
        enqueueEvent(Event.createSubmitCarparkEvent(point, listener));
    }

    public void updateParkingAvailability(Parking p) {
        enqueueEvent(Event.createAvailabilityUpdateEvent(p));
    }

    public void submitAvailability(Parking p, boolean binaryAvailability, long timestamp) {
        enqueueEvent(Event.createSubmitAvailabilityEvent(p, binaryAvailability, timestamp));
    }

    public void submitProperty(Parking p, Property prop, String value) {
        enqueueEvent(Event.createSubmitPropertyEvent(p, prop, value));
    }

    public void loadExtraCarparkAndTriggerTileRefresh(Uri id, CarparkDetailsUpdateListener listener) {
        enqueueEvent(Event.createLoadExtraCarparkEvent(id, listener));
    }

    private void enqueueEvent(Event request) {
        if (!this.eventPresenceSet.contains(request)) {
            this.eventPresenceSet.add(request);
            // todo only enqueue if the thread is running, otherwise loudly ignore?
            while (!this.eventQueue.offer(request)) {
                Log.e(TAG, "event queue full, removing old entry!");
                Event old = this.eventQueue.poll();
                this.eventPresenceSet.remove(old);
            }
//            Log.v(TAG, "added event " + request.p);
        }
    }

    private final Set<CarparkDetailsUpdateListener> detailsUpdateListeners = new HashSet<CarparkDetailsUpdateListener>();

    public synchronized void registerDetailsUpdateListener(CarparkDetailsUpdateListener listener) {
        this.detailsUpdateListeners.add(listener);
    }
    public synchronized void unregisterDetailsUpdateListener(CarparkDetailsUpdateListener listener) {
        this.detailsUpdateListeners.remove(listener);
    }

    private final Set<CarparkAvailabilityUpdateListener> availabilityUpdateListeners = new HashSet<CarparkAvailabilityUpdateListener>();

    public synchronized void registerAvailabilityUpdateListener(CarparkAvailabilityUpdateListener listener) {
        this.availabilityUpdateListeners.add(listener);
    }
    public synchronized void unregisterAvailabilityUpdateListener(CarparkAvailabilityUpdateListener listener) {
        this.availabilityUpdateListeners.remove(listener);
    }

}
