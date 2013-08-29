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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import uk.ac.open.kmi.parking.service.CarparkAvailabilityUpdateListener;
import uk.ac.open.kmi.parking.service.CarparkDetailsUpdateListener;
import uk.ac.open.kmi.parking.service.ParkingsService;
import uk.ac.open.kmi.parking.service.SortedCurrentItemsUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * main
 * @author Jacek Kopecky
 *
 */
public class MainActivity extends Activity implements
        LocationListener,
        SortedCurrentItemsUpdateListener,
//        NearbyCarparkUpdateListener,
        LoadingStatus.Listener,
        CarparkAvailabilityUpdateListener,
        CarparkDetailsUpdateListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener
 {
//    @SuppressWarnings("unused")
    private static final String TAG = "main activity";

    private GoogleMap map;
    private MyLocationTracker myLocTracker;
    private MarkerManager carparkManager;
    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private AnimationDrawable loadingAnimation;

    private ParkingsService parkingsService = null;

    private TextView statusTextView;
    private View statusContainer;
    private View addparkContainer;
    private ListView pinnedDrawer;
    private View pinnedDrawerContainer;
    private PinnedDrawerListAdapter pinnedDrawerAdapter;
    boolean showingPinned = false;
    boolean showPinnedWhenAnyFound = true;

    private Location currentLocation = null;

    private boolean animateToNextLocationFix = false;

    private Uri desiredCarpark = null;

//    boolean tooFarOut = false;
    boolean addingMode = false;

    private static final int DIALOG_TERMS = 0;
    private static final int DIALOG_SUBMITTING_CARPARK = 1;
    private static final int DIALOG_LOADING_CARPARK = 2;
    private static final int DIALOG_SEARCH = 3;

    private static final float INITIAL_ZOOM = 18f;

    private Dialog termsDialog = null;
    private ProgressDialog addparkDialog = null;
    private ProgressDialog loadingDialog = null;
    private Dialog searchDialog = null;

    private boolean showingLoadingDialog = false;

    private EditText searchDialogAddress = null;
    private List<Address> searchResults = null;

    private static final String PREFERENCE_SEEN_TERMS = "preference.main.seen-terms-and-conditions";

    // todo when t&c or privacy policy change, the app should be updated and prompt the user to accept the t&c&pp again, so it should keep track of the last-run version - it's ok to add this with the first version that needs it

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.i(TAG, "on create");

        ParkingDetailsActivity.staticInit(this);

        this.parkingsService = ParkingsService.get(this);
        this.parkingsService.setShowUnconfirmedCarparks(checkSettingsPref(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true));

        Intent intent = this.getIntent();
        this.desiredCarpark = intent.getData();
        intent.setData(null);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (checkSettingsPref(Preferences.PREFERENCE_FULLSCREEN, true)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        setContentView(R.layout.main);

        if (checkSettingsPref(Preferences.PREFERENCE_NOSLEEP, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            Log.d(TAG, "setting nosleep");
        }

        this.loadingAnimation = (AnimationDrawable) this.getResources().getDrawable(R.drawable.loading);
        getActionBar().setLogo(this.loadingAnimation);

        this.addparkContainer = findViewById(R.id.addpark_container);
        findViewById(R.id.addpark_cancel).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onBackPressed();
            }
        });
        findViewById(R.id.addpark_done).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onButtonPressAddpark();
            }
        });

        this.statusTextView = (TextView) findViewById(R.id.status_text);
        this.statusContainer = findViewById(R.id.status_container);

        this.pinnedDrawer = (ListView) findViewById(R.id.pinned_drawer);
        this.pinnedDrawerContainer = findViewById(R.id.pinned_drawer_container);
        this.pinnedDrawerAdapter = new PinnedDrawerListAdapter(this);

        this.pinnedDrawer.setAdapter(this.pinnedDrawerAdapter);

        if (savedInstanceState != null) {
            this.showingPinned = savedInstanceState.getBoolean(SAVED_SHOWING_PINNED_DRAWER, this.showingPinned);
            this.showPinnedWhenAnyFound = false;
        }

        this.map = ((MapFragment)getFragmentManager().findFragmentById(R.id.mapview)).getMap();
        this.map.moveCamera(CameraUpdateFactory.zoomTo(savedInstanceState == null ? INITIAL_ZOOM : savedInstanceState.getFloat(SAVED_MAP_ZOOM, INITIAL_ZOOM)));

        this.carparkManager = new MarkerManager(this, this.parkingsService, this.map);

        this.myLocTracker = new MyLocationTracker(this.parkingsService, this.map, this);

        this.animateToNextLocationFix = this.desiredCarpark == null;


//        List<Overlay> overlays = this.mapView.getOverlays();

//        // set parking drawables
//        Drawable drawableFull = this.getResources().getDrawable(R.drawable.parking_full);
//        int width = drawableFull.getIntrinsicWidth();
//        int height = drawableFull.getIntrinsicHeight();
////        Rect drawableFullBounds = new Rect(-width/2, -height/2, width-width/2, height-height/2);
//        Rect drawableFullBounds = new Rect(-width/2, -height, width-width/2, 0);
//
//        Drawable drawableAvailable = this.getResources().getDrawable(R.drawable.parking_available);
//        width = drawableAvailable.getIntrinsicWidth();
//        height = drawableAvailable.getIntrinsicHeight();
////        Rect drawableAvailableBounds = new Rect(-width/2, -height/2, width-width/2, height-height/2);
//        Rect drawableAvailableBounds = new Rect(-width/2, -height, width-width/2, 0);
//
//        Drawable drawableUnknown = this.getResources().getDrawable(R.drawable.parking_busy);
//        width = drawableUnknown.getIntrinsicWidth();
//        height = drawableUnknown.getIntrinsicHeight();
////        Rect drawableUnknownBounds = new Rect(-width/2, -height/2, width-width/2, height-height/2);
//        Rect drawableUnknownBounds = new Rect(-width/2, -height, width-width/2, 0);
//
//        View viewHighlightA = new View(this);
//        viewHighlightA.setBackgroundResource(R.drawable.parking_available_highlight);
//        View viewHighlightF = new View(this);
//        viewHighlightF.setBackgroundResource(R.drawable.parking_full_highlight);
//        View viewHighlightU = new View(this);
//        viewHighlightU.setBackgroundResource(R.drawable.parking_busy_highlight);
//
        Parking.setDrawables(BitmapFactory.decodeResource(this.getResources(), R.drawable.parking_full), BitmapFactory.decodeResource(this.getResources(), R.drawable.parking_available));
//
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        if (dm.widthPixels / 3 > getResources().getDimension(R.dimen.pinned_drawer_width_max)) {
            android.view.ViewGroup.LayoutParams lp = this.pinnedDrawerContainer.getLayoutParams();
            lp.height = dm.heightPixels * 4 / 5;
            this.pinnedDrawerContainer.setLayoutParams(lp);
        } else if (dm.heightPixels / 3 > getResources().getDimension(R.dimen.pinned_drawer_height)) {
            android.view.ViewGroup.LayoutParams lp = this.pinnedDrawerContainer.getLayoutParams();
            lp.height = dm.heightPixels / 3;
            this.pinnedDrawerContainer.setLayoutParams(lp);
        }

        // Acquire a reference to the system Location Manager
        this.locationClient = new LocationClient(this, this, this);
        this.locationRequest = LocationRequest.create();
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.locationRequest.setInterval(5000);
        this.locationRequest.setFastestInterval(1000);

    }

    private void showTermsAndConditions() {
        boolean seenTerms = getPreferences(MODE_PRIVATE).getBoolean(PREFERENCE_SEEN_TERMS, false);
        if (!seenTerms) {
            showDialog(DIALOG_TERMS);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
//        Log.i(TAG, "onCreateDialog");

        switch (id) {
        case DIALOG_TERMS:
            return prepareTermsDialog();
        case DIALOG_SUBMITTING_CARPARK:
            return prepareAddparkProgressDialog();
        case DIALOG_LOADING_CARPARK:
            return prepareLoadingProgressDialog();
        case DIALOG_SEARCH:
            return prepareSearchDialog();
        default:
            return super.onCreateDialog(id);
        }
    }

    private Dialog prepareAddparkProgressDialog() {
        if (this.addparkDialog != null) {
            return this.addparkDialog;
        }

        this.addparkDialog = new ProgressDialog(this);
        this.addparkDialog.setMessage(getResources().getString(R.string.add_carpark_submitting));
        return this.addparkDialog;
    }

    private Dialog prepareLoadingProgressDialog() {
        if (this.loadingDialog != null) {
            return this.loadingDialog;
        }

        this.loadingDialog = new ProgressDialog(this);
        this.loadingDialog.setMessage(getResources().getString(R.string.loading_desired_carpark));
        return this.loadingDialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == DIALOG_LOADING_CARPARK) {
            this.showingLoadingDialog = true;
        }
    }

    private Dialog prepareTermsDialog() {
        if (this.termsDialog != null) {
            return this.termsDialog;
        }

        Dialog dialog = new Dialog(this);
        this.termsDialog = dialog;
        dialog.setContentView(R.layout.terms_dalog);
        dialog.setTitle(R.string.tc_dialog_name);
        WebView wv = (WebView)dialog.findViewById(R.id.terms_webview);
        wv.loadUrl("file:///android_asset/terms.html");

        Button b = (Button) dialog.findViewById(R.id.terms_button_accept);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onButtonPressAcceptTerms();
            }
        });
        b = (Button) dialog.findViewById(R.id.terms_button_decline);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onButtonPressDeclineTerms();
            }
        });
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (checkSettingsPref(Preferences.PREFERENCE_FULLSCREEN, true)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (checkSettingsPref(Preferences.PREFERENCE_NOSLEEP, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

//        this.parkingsOverlay.setShadow(checkSettingsPref(Preferences.PREFERENCE_SHADOW, true));
        this.parkingsService.setShowUnconfirmedCarparks(checkSettingsPref(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true));
    }

    @Override
    protected void onStart() {
//        Log.i(TAG, "onStart");
        super.onStart();
        showTermsAndConditions();
        this.locationClient.connect();
        ParkingDetailsActivity.preparePresentationOntologyInNewThread(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.desiredCarpark = intent.getData();
        intent.setData(null);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        this.animateToNextLocationFix = state.getBoolean(SAVED_ANIMATE_TO_NEXT_LOCATION, true);
        String bubble = state.getString(SAVED_BUBBLE_ITEM);
        if (bubble != null) {
            this.carparkManager.showBubble(Parking.getParking(Uri.parse(bubble)));
        }
        this.addingMode = state.getBoolean(SAVED_ADDING_MODE, false);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
//        this.map = ((MapFragment)getFragmentManager().findFragmentById(R.id.mapview)).getMap();

        LoadingStatus.registerListener(this);
        this.parkingsService.registerSortedCurrentItemsUpdateListener(this);
//        this.parkingsService.registerNearbyCurrentItemsUpdateListener(this);
        this.parkingsService.registerCarparkAvailabilityUpdateListener(this);
        this.parkingsService.registerCarparkDetailsUpdateListener(this);
        this.parkingsService.geocoder = new Geocoder(this);
        this.parkingsService.startService();

        // start downloading tiles at current map location
        this.parkingsService.getSortedCurrentItems(this.map.getCameraPosition().target);

        if (this.desiredCarpark != null) {
            showDialog(DIALOG_LOADING_CARPARK);
            this.parkingsService.loadExtraCarpark(this.desiredCarpark, new CarparkDetailsUpdateListener() {
                public void onCarparkInformationUpdated(final Parking parking) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (MainActivity.this.showingLoadingDialog) {
                                dismissDialog(DIALOG_LOADING_CARPARK);
                                MainActivity.this.showingLoadingDialog = false;
                            }
                            if (parking == null) {
                                Toast.makeText(MainActivity.this, R.string.error_parking_not_found, Toast.LENGTH_LONG).show();
                                return;
                            }
                            MainActivity.this.centerOnCarpark(parking, true);
                        }
                    });
                }
            });
            this.desiredCarpark = null;
        }

        if (this.currentLocation == null && this.locationClient.isConnected()) {
            this.currentLocation = this.locationClient.getLastLocation();
            Log.d(TAG, "animate1: " + this.animateToNextLocationFix);
            if (this.animateToNextLocationFix && this.currentLocation != null) {
                animateToCurrentLocation(true);
                this.animateToNextLocationFix = false;
            }
            this.parkingsService.onLocationChanged(this.currentLocation);
        }

        this.pinnedDrawerAdapter.update();
        updateUIState();
        this.carparkManager.update();
    }

    void centerOnCarpark(final Parking parking, boolean immediate) {
        if (immediate) {
            this.map.moveCamera(CameraUpdateFactory.newLatLng(parking.point));
            this.carparkManager.showBubble(parking);
        } else {
            if (!parking.equals(this.carparkManager.getBubbleItem())) {
                this.carparkManager.removeBubble();
            }
            this.map.animateCamera(CameraUpdateFactory.newLatLng(parking.point), new GoogleMap.CancelableCallback() {
                public void onCancel() { /* ignore */ }
                public void onFinish() {
                    MainActivity.this.carparkManager.showBubble(parking);
                }
            });
        }
//        updateUIState();
    }

    private boolean checkSettingsPref(String pref, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(pref, def);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        if (this.showingLoadingDialog) {
            dismissDialog(DIALOG_LOADING_CARPARK);
            this.showingLoadingDialog = false;
        }

        LoadingStatus.unregisterListener(this);

        this.parkingsService.geocoder = null;
        this.parkingsService.unregisterCarparkDetailsUpdateListener(this);
        this.parkingsService.unregisterCarparkAvailabilityUpdateListener(this);
        this.parkingsService.unregisterSortedCurrentItemsUpdateListener(this);
//        this.parkingsService.unregisterNearbyCurrentItemsUpdateListener(this);
        this.parkingsService.stopService(this);

        super.onPause();
    }

    @Override
    public void onStop() {
        if (this.locationClient.isConnected()) {
            this.locationClient.removeLocationUpdates(this);
        }
        this.locationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (this.carparkManager.removeBubble()) {
//            updateUIState();
        } else if (this.addingMode) {
            this.addingMode = false;
            updateUIState();
        } else {
            super.onBackPressed();
        }
    }

//    void setTooFarOut(boolean far) {
//        if (this.tooFarOut != far) {
//            this.tooFarOut = far;
//            this.updateUIState();
//        }
//    }
//
    /**
     * helper method that calls the details view
     * @param context the current activity
     * @param item the parking whose details should be viewed
     */
    public static void showDetailsForCarpark(Context context, MapItem item) {
        Intent intent = new Intent(context, ParkingDetailsActivity.class);
        intent.setData(item.id);
        context.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.menu_traffic).setChecked(this.map.isTrafficEnabled());
        menu.findItem(R.id.menu_satellite).setChecked(this.map.getMapType() != GoogleMap.MAP_TYPE_NORMAL);
//        menu.findItem(R.id.menu_add_car_park).setVisible(!this.tooFarOut && !this.addingMode);
        menu.findItem(R.id.menu_pinned).setTitle(this.showingPinned ? R.string.menu_pinned_hide : R.string.menu_pinned_show);
//        menu.findItem(R.id.menu_pinned).setVisible(!this.tooFarOut && !this.addingMode);
        menu.findItem(R.id.menu_pinned).setVisible(!this.addingMode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_traffic:
            boolean state = !item.isChecked();
            item.setChecked(state);
            this.map.setTrafficEnabled(state);
            return true;
        case R.id.menu_satellite:
            state = !item.isChecked();
            item.setChecked(state);
            this.map.setMapType(state?GoogleMap.MAP_TYPE_HYBRID:GoogleMap.MAP_TYPE_NORMAL);
            return true;
//        case R.id.menu_my_location:
//            animateToCurrentLocation(false);
//            return true;
        case R.id.menu_settings:
            Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
            startActivity(settingsActivity);
            return true;
        case R.id.menu_add_car_park:
            startAddingCarpark();
            return true;
        case R.id.menu_search:
            // todo maybe search should be a separate activity, with immediate list of results
            // go to activity where at the top is search text box with the last search query
            // on button press, show list of results
            // on pressing a result, go on to main activity again, show map there, maybe with an annotated pin with location?, with car parks
            // back button then should take you back to search results
            startAddressSearch();
            return true;
        case R.id.menu_pinned:
            this.showingPinned = !this.showingPinned;
            updateUIState();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startAddingCarpark() {
        // what should be done if we're already in the mode? probably NOP
        if (this.addingMode) {
            return; // already in the mode
        }

        if (!checkSettingsPref(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true)) {
            Toast.makeText(this, R.string.toast_pref_unconfirmedprops, Toast.LENGTH_LONG).show();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true).apply();
            this.parkingsService.setShowUnconfirmedCarparks(true);
        }


        // drop bubble
        this.carparkManager.removeBubble();

        this.addingMode = true;
        updateUIState();
    }

    private void onButtonPressAddpark() {
        showDialog(DIALOG_SUBMITTING_CARPARK);

        // todo setOnDismissListener: catch the dismiss and make the listener below do NOP
        // or alternatively make it do NOP if the user has in the meantime done something that would make the action wrong - similar for initial desired carpark
        // there can be a global "last significant action timestamp" (or counter, doesn't matter)
        //    significant actions: starting addpark again; selecting some other car park; maybe even creating a bubble; explicitly following nearest
        // the listener will remember its state when it was created
        // then on activation it can do NOP if the timestamp has changed

        this.addingMode = false;
        this.parkingsService.submitNewCarpark(this.map.getCameraPosition().target, new CarparkDetailsUpdateListener() {
            public void onCarparkInformationUpdated(final Parking parking) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.dismissDialog(DIALOG_SUBMITTING_CARPARK);
                        if (parking == null) {
                            Toast.makeText(MainActivity.this, R.string.error_submitting_data, Toast.LENGTH_LONG).show();
                            return;
                        }
//                      Log.i(TAG, "car park added and loaded");
                        //     upon submission, open the details view so user can immediately fill in properties
                        //     also automatically have the new car park as watched
                        MainActivity.this.centerOnCarpark(parking, false);
                        showDetailsForCarpark(MainActivity.this, parking);
                    }
                });
            }
        });
        updateUIState();
    }

    private void animateToCurrentLocation(boolean zoomOutIfNecessary) {
        if (this.currentLocation != null) {
            this.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(this.currentLocation.getLatitude(), this.currentLocation.getLongitude())));
            final float accuracy = this.currentLocation.getAccuracy();
            if (zoomOutIfNecessary & accuracy > 200f & Math.abs(this.map.getCameraPosition().zoom - INITIAL_ZOOM) < .1f) {
                this.map.animateCamera(CameraUpdateFactory.zoomOut());
                if (accuracy > 500f) {
                    this.map.animateCamera(CameraUpdateFactory.zoomOut());
                }
            }
        } else {
            Toast.makeText(this, R.string.toast_no_location, Toast.LENGTH_SHORT).show();
        }
    }

    // todo the context menu could also be on some stuff in the bubble or on the active carpark's icon (should it be highlighted?) or on pinned entries

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v instanceof PinnedDrawerListAdapter.PinnedCarparkView) {
            getMenuInflater().inflate(R.menu.currpark_context, menu);
            this.contextMenuCarpark = ((PinnedDrawerListAdapter.PinnedCarparkView)v).getCarpark();
            menu.setHeaderIcon(this.contextMenuCarpark.getEffectiveAvailability() == Parking.Availability.AVAILABLE ? R.drawable.parking_available : R.drawable.parking_full); // todo parking drawables here must be kept in sync with the ones used in onCreate; maybe Drawable.mutate() might help here?
            menu.setHeaderTitle(this.contextMenuCarpark.getTitle());
        }
    }

    private Parking contextMenuCarpark = null;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_currpark_zoom:
            centerOnCarpark(this.contextMenuCarpark, false);
            return true;
        case R.id.menu_currpark_mark_available:
            reportAvailability(this.contextMenuCarpark, true);
            return true;
        case R.id.menu_currpark_mark_full:
            reportAvailability(this.contextMenuCarpark, false);
            return true;
        case R.id.menu_currpark_show_details:
            showDetailsForCarpark(this, this.contextMenuCarpark);
            return true;
        case R.id.menu_currpark_unpin:
            pinCarpark(this.contextMenuCarpark, false);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    void pinCarpark(Parking p, boolean pin) {
        this.parkingsService.pinCarpark(p, pin, this);
        boolean drawerHasItems = this.pinnedDrawerAdapter.update();
        if (drawerHasItems != this.showingPinned) {
            this.showingPinned = drawerHasItems;
            updateUIState();
        }
//        this.bubbleOverlay.updatePinnedStatus(p);  todo
    }

    void reportAvailability(Parking park, boolean avail) {
        this.parkingsService.submitAvailability(park, avail);
        updateUIState();
    }

    public void onLocationChanged(Location location) {
        // todo filter out fixes that are worse than this one
        // todo only change current near carpark if the accuracy of the fix is good enough (e.g. within the limit distance to the near carpark)
        this.currentLocation = location;
        if (this.animateToNextLocationFix && this.currentLocation != null) {
            animateToCurrentLocation(false);
            this.animateToNextLocationFix = false;
        }
        this.parkingsService.onLocationChanged(location);
        this.myLocTracker.onLocationChanged(location);
//        Log.d(TAG, "onLocationChanged called");

        // todo also make sure that after some time without updates, current location is no longer valid
        // then currentLocation must be set to null
    }

    /**
     * this method takes care of the state of the UI so it corresponds to the current mode as per ParkingsService
     */
    private void updateUIState() {
//        Log.d(TAG, "updating UI state");
//        if (this.tooFarOut) {
//            showStatusText(R.string.currpark_too_far_out);
//            return;
//        }

        if (!this.addingMode && !this.parkingsService.loadedSomeCarparks()) {
            showStatusText(R.string.currpark_initial);
            return;
        }

        hideStatusText();

//        this.carparkManager.updateAvailability();
//        this.mapView.invalidate();

        if (this.addingMode) {
            hidePinnedDrawer();
            showAddparkViews();
        } else {
            hideAddparkViews();

            if (this.showPinnedWhenAnyFound && !this.parkingsService.listKnownPinnedCarparks().isEmpty()) {
                this.showingPinned = true;
                this.showPinnedWhenAnyFound = false;
            }

            if (this.showingPinned) {
                showPinnedDrawer();
                this.showPinnedWhenAnyFound = false;
            } else {
                hidePinnedDrawer();
            }
        }

        // todo pinned car parks should have an icon for centering, reporting, voice info
        // todo details view/activity/fragment may need an icon for pinning
    }

    private void showAddparkViews() {
        this.addparkContainer.setVisibility(View.VISIBLE);
    }

    private void hideAddparkViews() {
        this.addparkContainer.setVisibility(View.GONE);
    }

    private void showStatusText(int resource) {
        hideAddparkViews();
        hidePinnedDrawer();
        this.statusTextView.setText(resource);
        this.statusContainer.setVisibility(View.VISIBLE);
    }

    private void hideStatusText() {
        this.statusContainer.setVisibility(View.GONE);
    }

    private void hidePinnedDrawer() {
        if (this.pinnedDrawer.getVisibility() == View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, 0, -this.pinnedDrawerContainer.getHeight());
            anim.setDuration(getResources().getInteger(R.integer.pinned_drawer_animation_duration));
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) { /* nothing */ }
                public void onAnimationRepeat(Animation animation) { /* nothing */ }
                public void onAnimationEnd(Animation animation) { MainActivity.this.pinnedDrawer.setVisibility(View.GONE); }
            });
            this.pinnedDrawer.startAnimation(anim);
        } else {
            // make sure
            this.pinnedDrawer.setVisibility(View.GONE);
        }
        this.parkingsService.setPinnedUpdating(false);
    }

    private void showPinnedDrawer() {
        if (this.pinnedDrawer.getVisibility() != View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, -this.pinnedDrawerContainer.getHeight(), 0);
            anim.setDuration(getResources().getInteger(R.integer.pinned_drawer_animation_duration));
            this.pinnedDrawer.startAnimation(anim);
            this.pinnedDrawer.setVisibility(View.VISIBLE);
        }
        this.parkingsService.setPinnedUpdating(true);
    }

    private final Runnable updateUIStateTask = new Runnable(){
        public void run() {
            updateUIState();
            MainActivity.this.pinnedDrawerAdapter.update();
            MainActivity.this.carparkManager.update();
        }
    };

    public void onSortedCurrentItemsUpdated() {
        runOnUiThread(this.updateUIStateTask);
    }

//    public void onNearbyCarparkUpdated() {
//        runOnUiThread(this.updateUIStateTask);  // todo this only affects the pinned area with nearest carpark(s) so maybe not updateUIState?
//    }
//
    public void onCarparkAvailabilityUpdated(final Parking parking) {
        runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.pinnedDrawerAdapter.updateIfContains(parking);
                MainActivity.this.carparkManager.updateAvailability(parking);
//                MainActivity.this.mapView.invalidate();
            }
        });
    }

    public void onStartedLoading() {
        this.loadingAnimation.start();
    }

    public void onStoppedLoading() {
        this.loadingAnimation.stop();
        this.loadingAnimation.selectDrawable(0);
    }

    /**
     * called when terms&conditions are accepted
     */
    private void onButtonPressAcceptTerms() {
//        Log.d(TAG, "pressed accept");
        if (this.currentLocation != null) {
            this.map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(this.currentLocation.getLatitude(), this.currentLocation.getLongitude())));
        }
        dismissDialog(DIALOG_TERMS);
        Editor e = getPreferences(MODE_PRIVATE).edit();
        e.putBoolean(PREFERENCE_SEEN_TERMS, true);
        e.commit();
    }

    /**
     * called when terms&conditions are declined
     */
    private void onButtonPressDeclineTerms() {
        Editor e = getPreferences(MODE_PRIVATE).edit();
        e.putBoolean(PREFERENCE_SEEN_TERMS, false);
        e.commit();
        finish();
    }

    public void onCarparkInformationUpdated(final Parking parking) {
        runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.carparkManager.updateDetails(parking);
                MainActivity.this.pinnedDrawerAdapter.updateIfContains(parking);
            }
        });
    }

    /**
     * this gets called when pressing the app icon
     * @param v ignored
     */
    public void openOptionsMenu(@SuppressWarnings("unused") View v) {
        openOptionsMenu();
    }

    private void startAddressSearch() {
        showDialog(DIALOG_SEARCH);
//        this.searchDialogAddress.requestFocusFromTouch();
    }

    private void performSearch() {
//        Log.d(TAG, "searching for " + this.searchDialogAddress.getText().toString());

        this.searchResults = null;
        try {
            if (this.parkingsService.geocoder != null) {
                this.searchResults = this.parkingsService.geocoder.getFromLocationName(this.searchDialogAddress.getText().toString(), 10);
            }
        } catch (IOException e) {
            Log.d(TAG, "geocoding problem ", e);
        }

        if (this.searchResults == null || this.searchResults.size() == 0) {
            new AlertDialog.Builder(this).
                setTitle(R.string.search_results_title).
                setMessage(R.string.search_not_found).
                setCancelable(true).
//                setPositiveButton(R.string.search_dialog_ok, null).
                setNegativeButton(R.string.search_dialog_cancel, null).
                show();
        } else {

            // drop possible places without location
            Iterator<Address> it = this.searchResults.iterator();
            while (it.hasNext()) {
                Address a = it.next();
                if (!a.hasLatitude() || !a.hasLongitude()) {
                    it.remove();
                }
            }

            CharSequence[] addrs = new CharSequence[this.searchResults.size()];
            int i = 0;
            for (Address address : this.searchResults) {
                addrs[i] = formatAddress(address);
                i++;
            }

            AlertDialog.Builder resultsDialog = new AlertDialog.Builder(this);
            resultsDialog.setTitle(R.string.search_results_title);
            resultsDialog.setItems(addrs, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Address selected = MainActivity.this.searchResults.get(which);
//                        Log.d(TAG, "selected address " + which + " which is " + selected);
                        MainActivity.this.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(selected.getLatitude(), selected.getLongitude())));
                    }
                });
            resultsDialog.setCancelable(true);
            if (addrs.length == 1) {
                resultsDialog.setPositiveButton(R.string.search_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Address selected = MainActivity.this.searchResults.get(0);
                        MainActivity.this.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(selected.getLatitude(), selected.getLongitude())));
                    }
                });
            }
            resultsDialog.setNegativeButton(R.string.search_dialog_cancel, null);
            resultsDialog.show();
        }
    }

    static String formatAddress(Address a) {
        // todo this may need more
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<=a.getMaxAddressLineIndex(); i++) {
            sb.append(a.getAddressLine(i));
            if (i < a.getMaxAddressLineIndex()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private Dialog prepareSearchDialog() {
        if (this.searchDialog != null) {
            return this.searchDialog;
        }

        this.searchDialogAddress = new EditText(this);

        this.searchDialog = new AlertDialog.Builder(this).
                setCancelable(true).
                setTitle(R.string.search_dialog_title).
                setView(this.searchDialogAddress).
                setPositiveButton(R.string.search_dialog_search, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.performSearch();
                    }
                }).
                setNegativeButton(R.string.search_dialog_cancel, null).
                create();

        this.searchDialogAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    MainActivity.this.searchDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        return this.searchDialog;
    }

    void openNavigationTo(MapItem p) {
        String saddr = ""; // start point address
        if (this.currentLocation != null) {
            saddr = "saddr=" + this.currentLocation.getLatitude() + "," + this.currentLocation.getLongitude() + "&";
        }
        String daddr = "daddr=" + (p.point.latitude) + "," + (p.point.longitude); // destination point address
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?" + saddr + daddr));
        startActivity(intent);
    }

    private static final String SAVED_ANIMATE_TO_NEXT_LOCATION = "uk.ac.open.kmi.parking.animate-to-next-location";
    private static final String SAVED_ADDING_MODE = "uk.ac.open.kmi.parking.adding-mode";
    private static final String SAVED_BUBBLE_ITEM = "uk.ac.open.kmi.parking.bubble-item";
    private static final String SAVED_MAP_ZOOM = "uk.ac.open.kmi.parking.zoom-level.float";
    private static final String SAVED_SHOWING_PINNED_DRAWER = "uk.ac.open.kmi.parking.show-pinned-drawer";

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean(SAVED_ANIMATE_TO_NEXT_LOCATION, this.animateToNextLocationFix);
        Parking bubble = (Parking)this.carparkManager.getBubbleItem();
        state.putString(SAVED_BUBBLE_ITEM, bubble == null ? null : bubble.id.toString());
        state.putBoolean(SAVED_ADDING_MODE, this.addingMode);
        state.putFloat(SAVED_MAP_ZOOM, this.map.getCameraPosition().zoom);
        state.putBoolean(SAVED_SHOWING_PINNED_DRAWER, this.showingPinned);
    }

    public void onConnectionFailed(ConnectionResult arg0) {
        // TODO Auto-generated method stub

    }

    public void onConnected(Bundle arg0) {
        // start periodic updates
        this.locationClient.requestLocationUpdates(this.locationRequest, this);

        if (this.currentLocation == null) {
            this.currentLocation = this.locationClient.getLastLocation();
//            Toast.makeText(this, "location: " + this.currentLocation, Toast.LENGTH_LONG).show();
            if (this.animateToNextLocationFix && this.currentLocation != null) {
                animateToCurrentLocation(true);
                this.animateToNextLocationFix = false;
            }
            this.parkingsService.onLocationChanged(this.currentLocation);
        }
    }

    Location getCurrentLocation() {
        return this.currentLocation;
    }

    public void onDisconnected() {
        // TODO Auto-generated method stub

    }
}
