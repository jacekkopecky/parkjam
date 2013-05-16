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

import java.util.List;

import uk.ac.open.kmi.parking.Parking;
import android.net.Uri;

/**
 * this object knows about the car park IDs for several special car parks and it makes sure their availability is updated frequently
 */
class CarparkAvailabilityRefreshTrigger implements CarparkAvailabilityUpdateListener {
    @SuppressWarnings("unused")
    private static final String TAG = "availability refresh trigger";

    private DetailsAndAvailabilityThread availabilityUpdater;
    private RememberedCarparks rememberedCarparks;

    public CarparkAvailabilityRefreshTrigger(DetailsAndAvailabilityThread thread, RememberedCarparks rememberedCarparks) {
        this.availabilityUpdater = thread;
        this.rememberedCarparks = rememberedCarparks;
    }

    private Uri parkingCurrpark = null;
    private boolean parkingCurrparkLoading = false;
    private Uri parkingInBubble = null;
    private boolean parkingInBubbleLoading = false;
    private Uri parkingDetailsView = null;
    private boolean parkingDetailsViewLoading = false;
    private long parkingsPinnedNextLoading = 0;
    private boolean parkingsPinnedUpdating = false;

    public synchronized void updateCurrpark(Uri p) {
        if (p != null && p.equals(this.parkingCurrpark)) {
            return;
        }
        this.parkingCurrpark = p;
        this.parkingCurrparkLoading = false;
        refreshNextWakeTime();
    }

    public synchronized void updateBubble(Uri p) {
        if (p != null && p.equals(this.parkingInBubble)) {
            return;
        }
        this.parkingInBubble = p;
        this.parkingInBubbleLoading = false;
        refreshNextWakeTime();
    }

    public synchronized void updateDetailsView(Uri p) {
        if (p != null && p.equals(this.parkingDetailsView)) {
            return;
        }
        this.parkingDetailsView = p;
        this.parkingDetailsViewLoading = false;
        refreshNextWakeTime();
    }

    private synchronized void refreshNextWakeTime() {
        if (this.waitingThread == null) {
            return;
        }

        long minUpdateTime = Long.MAX_VALUE;
        minUpdateTime = Long.MAX_VALUE;
        minUpdateTime = minTime(minUpdateTime, this.parkingCurrpark, this.parkingCurrparkLoading);
        minUpdateTime = minTime(minUpdateTime, this.parkingInBubble, this.parkingInBubbleLoading);
        minUpdateTime = minTime(minUpdateTime, this.parkingDetailsView, this.parkingDetailsViewLoading);
        if (this.parkingsPinnedUpdating) {
            if (this.parkingsPinnedNextLoading < System.currentTimeMillis()) {
                List<Parking> pinned = this.rememberedCarparks.listLastKnownPinnedCarparks();
                for (Parking p : pinned) {
                    minUpdateTime = minTime(minUpdateTime, p.id, false);
                }
            } else if (minUpdateTime > this.parkingsPinnedNextLoading) {
                minUpdateTime = this.parkingsPinnedNextLoading;
            }
        }
//        Log.v(TAG, "minupdatetime = " + minUpdateTime);

        this.waitingThread.nextWake = minUpdateTime;
        this.waitingThread.interrupt();
    }

    private long minTime(long time, Uri parking, boolean ignore) {
        if (parking == null || ignore) {
            return time;
        }
        Parking p = Parking.getParking(parking);
//        Log.v(TAG, "parking nextAvailUpdate " + (p != null ? p.nextAvailUpdate : null));
        return p != null && time > p.nextAvailUpdate ? p.nextAvailUpdate : time;
    }

    private WaitingThread waitingThread = null;

    public synchronized void startThread() {
        if (this.waitingThread != null) {
//            Log.w(TAG, "asked to start thread when it's already apparently started");
            return;
        }
        this.parkingInBubbleLoading = false;
        this.parkingCurrparkLoading = false;
        this.parkingDetailsViewLoading = false;
        this.availabilityUpdater.registerAvailabilityUpdateListener(this);
        this.waitingThread = new WaitingThread();
        this.waitingThread.start();
        refreshNextWakeTime();
    }

    public synchronized void stopThread() {
        if (this.waitingThread == null) {
//            Log.w(TAG, "asked to stop thread when it's already stopped");
            return;
        }
        this.waitingThread.shouldDie = true;
        this.waitingThread.interrupt();
        this.waitingThread = null;
        this.availabilityUpdater.unregisterAvailabilityUpdateListener(this);
    }

    private class WaitingThread extends Thread {
        volatile boolean shouldDie = false;
        volatile long nextWake = Long.MAX_VALUE;

        @Override
        public void run() {
            for (;;) {
                try {
                    long timeToSleep = Long.MAX_VALUE;
                    synchronized (CarparkAvailabilityRefreshTrigger.this) {
                        long time = System.currentTimeMillis();
                        if (time >= this.nextWake) {
                            CarparkAvailabilityRefreshTrigger.this.triggerRefresh(time);
                        } else {
                            timeToSleep = this.nextWake - time + Config.EXTRA_SLEEP_TIME; // sleeping 200ms longer than needed to try and make sure we don't wake up prematurely (timing not guaranteed by sleep)
                        }
                    }
//                    Log.v(TAG, "sleeping for " + timeToSleep + "ms");
                    Thread.sleep(timeToSleep);
//                    Log.v(TAG, "sleeper awoke");
                } catch (InterruptedException e) {
                    if (this.shouldDie) {
                        return;
                    }
                }
            }

        }
    }

    void triggerRefresh(long time) {
        if (this.parkingCurrpark != null && !this.parkingCurrparkLoading) {
            Parking p = Parking.getParking(this.parkingCurrpark);
            if (p != null && p.nextAvailUpdate <= time) {
                this.availabilityUpdater.updateParkingAvailability(p);
                this.parkingCurrparkLoading = true;
//                Log.d(TAG, "updating current carpark's availability for " + p.id);
            }
        }
        if (this.parkingInBubble != null && !this.parkingInBubbleLoading) {
            Parking p = Parking.getParking(this.parkingInBubble);
            if (p != null && p.nextAvailUpdate <= time) {
                this.availabilityUpdater.updateParkingAvailability(p);
                this.parkingInBubbleLoading = true;
//                Log.d(TAG, "updating bubble carpark's availability for " + p.id);
            }
        }
        if (this.parkingDetailsView != null && !this.parkingDetailsViewLoading) {
            Parking p = Parking.getParking(this.parkingDetailsView);
            if (p != null && p.nextAvailUpdate <= time) {
                this.availabilityUpdater.updateParkingAvailability(p);
                this.parkingDetailsViewLoading = true;
//                Log.d(TAG, "updating details view carpark's availability for " + p.id);
            }
        }
        if (this.parkingsPinnedUpdating && time >= this.parkingsPinnedNextLoading) {
            List<Parking> pinned = this.rememberedCarparks.listLastKnownPinnedCarparks();
            for (Parking p : pinned) {
                if (p.nextAvailUpdate <= time) {
                    this.availabilityUpdater.updateParkingAvailability(p);
                }
            }
            this.parkingsPinnedNextLoading = time + 10000; // todo constant somewhere?
        }

        refreshNextWakeTime();
    }

    public synchronized void onCarparkAvailabilityUpdated(Parking parking) {
//        Log.d(TAG, "carpark availability updated for " + parking.id);
        boolean refresh = false;
        if (parking.id.equals(this.parkingCurrpark)) {
            this.parkingCurrparkLoading = false;
            refresh = true;
        }
        if (parking.id.equals(this.parkingInBubble)) {
            this.parkingInBubbleLoading = false;
            refresh = true;
        }
        if (parking.id.equals(this.parkingDetailsView)) {
            this.parkingDetailsViewLoading = false;
            refresh = true;
        }
        if (refresh) {
            this.refreshNextWakeTime();
        }
    }

    public void setPinnedUpdating(boolean enable) {
        this.parkingsPinnedUpdating = enable;
        this.refreshNextWakeTime();
    }
}
