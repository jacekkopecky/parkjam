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

import java.util.HashSet;
import java.util.Set;

/**
 * a central place for keeping information about whether the app is loading something and should show a spinner
 * @author Jacek Kopecky
 *
 */
public class LoadingStatus {
    @SuppressWarnings("unused")
    private static final String TAG = "loading status";

    private static Set<Listener> listeners = new HashSet<Listener>();
    private static int loadingCount = 0;

    /**
     * A listener for when loading starts and when it stops
     */
    public static interface Listener {
        /**
         * called when background loading starts
         * guaranteed to be called on the UI thread
         */
        public void onStartedLoading();

        /**
         * called when no more background loading is going on
         * guaranteed to be called on the UI thread
         */
        public void onStoppedLoading();

        /**
         * should call the runnable on the preferred thread (in Android, the UI thread Ñ this is handled by Activity)
         * @param r a runnable
         */
        public void runOnUiThread(Runnable r);
    }

    /**
     * register a listener for loading start/stop events
     * @param l the listener
     */
    public synchronized static void registerListener(Listener l) {
        if (listeners.contains(l)) {
//            Log.e(TAG, "listener registered twice at ", new Exception());
        }
        listeners.add(l);
        if (loadingCount > 0) {
            startLoadingOnUiThread(l);
        }
    }

    /**
     * unregister a listener for loading start/stop events
     * @param l the listener
     */
    public synchronized static void unregisterListener(Listener l) {
        if (loadingCount > 0) {
            stopLoadingOnUiThread(l);
        }
        if (!listeners.contains(l)) {
//            Log.e(TAG, "unknown listener unregistered at ", new Exception());
        }
        listeners.remove(l);
    }

    /**
     * this should be called when background loading is started
     */
    public static synchronized void startedLoading() {
//        Log.i(TAG, "starting loading, current count " + loadingCount);
        if (0 == loadingCount++) {
            if (delayedStopper != null) {
                delayedStopper.interrupt();
                delayedStopper = null;
//                Log.v(TAG, "cancelling stopping of loading");
                return;
            }

//            Log.i(TAG, "starting loading animation");
            for (Listener l : listeners) {
                startLoadingOnUiThread(l);
            }
        }
    }

    private static Thread delayedStopper = null;

    /**
     * this should be called when background loading is done or interrupted
     */
    public static synchronized void stoppedLoading() {
//        Log.i(TAG, "stopping loading, current count " + loadingCount, new Exception());

        if (0 == --loadingCount) {
//            Log.i(TAG, "stopping loading animation");
            if (delayedStopper == null) {
                delayedStopper = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                            // it's possible we miss the interrupt right after sleep ends but before we would do the interrupts - but then the delayedStopper will be null (guarded by synchronization)
                            synchronized (LoadingStatus.class) {
                                if (LoadingStatus.delayedStopper != null) {
                                    LoadingStatus.delayedStopper = null;
                                    for (final Listener l : listeners) {
                                        stopLoadingOnUiThread(l);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
//                            Log.v(TAG, "loading not stopped after all");
                        }
                    }
                };
                delayedStopper.start();
            }
        }
    }

    private static void startLoadingOnUiThread(final Listener l) {
        l.runOnUiThread(new Runnable() {
            public void run() {
                l.onStartedLoading();
            }
        });
    }

    private static void stopLoadingOnUiThread(final Listener l) {
        l.runOnUiThread(new Runnable() {
            public void run() {
                l.onStoppedLoading();
            }
        });
    }
}
