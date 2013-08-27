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

import uk.ac.open.kmi.parking.service.ParkingsService;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;

/**
 * a map overlay that contains some DrawableOverlayItems
 * @author Jacek Kopecky
 *
 */
class BubbleOverlay {

//    @SuppressWarnings("unused")
//    private final static String TAG = "bubble overlay";
//
//    private MainActivity activity;
//    private ParkingsService parkingsService;
//    private GoogleMap map;
//
//    private View bubbleView = null;
//    private Parking bubbleCarpark = null;
//    private ImageView bubbleButtonFull = null;
//    private ImageView bubbleButtonAvail = null;
//    private ImageView bubbleButtonPin = null;
//
//    private final float density;
//
//    private boolean showUnconfirmedProperties = true;
//
//    private boolean nextDetailsUpdateTriggersRepositioning = false;
//
//    /**
//     * default constructor, no shadow
//     * @param activity the activity to notify about the selected item (and to use as context)
//     * @param ps parking service
//     * @param map the map
//     * @param density the logical density (160dpi = 1, 120dpi = .75) of the screen
//     */
//    public BubbleOverlay(MainActivity activity, ParkingsService ps, GoogleMap map, float density) {
//        this.activity = activity;
//        this.parkingsService = ps;
//        this.map = map;
//        this.density = density;
//    }
//
//    /**
//     * set item to show in the bubble, set it to null to remove the bubble
//     * WARNING: this must be called from the UI thread that calls draw()
//     * @param item the item to show in the bubble, null if no bubble should be shown
//     */
    public void setItem(MapItem item) {
//        if (this.bubbleView != null) {
//            if (this.bubbleCarpark != null && this.bubbleCarpark.equals(item)) {
//                return;
//            }
//            // remove previous bubble
//            this.mapView.removeView(this.bubbleView);
//            this.bubbleView = null;
//            this.bubbleCarpark = null;
//        }
//
//        if (item == null || item instanceof Parking) {
//            this.bubbleCarpark = (Parking)item;
//            this.parkingsService.setCurrentExplicitCarpark(this.bubbleCarpark);
//        }
//
//        if (item != null) {
//            this.showUnconfirmedProperties = PreferenceManager.getDefaultSharedPreferences(this.activity).getBoolean(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true);
//            createBubble(this.activity, item);
//        }
    }
//
    public Parking getItem() {
//        return this.bubbleCarpark;
        return null;
    }
//
//    private void makeBubbleOnScreen() {
//        if (this.bubbleView == null) {
//            return;
//        }
//
//        Projection proj = this.mapView.getProjection();
//        int xoffset = 0, yoffset = 0;
//        int left = this.bubbleView.getLeft();
//        int right = this.bubbleView.getRight();
//        int top = this.bubbleView.getTop();
//        int bottom = this.bubbleView.getBottom();
//
////        Log.d(TAG, "bottom: " + bottom);
//
//        // comparing right edge and then left edge; in case the bubble is wider than the map (which it shouldn't be) align the left side primarily
//        if (right > this.mapView.getWidth()) {
//            xoffset = right - this.mapView.getWidth();
//        }
//        if (left < 0) {
//            xoffset = left;
//        }
//
//        // comparing bottom edge and then top edge; in case the bubble is taller than the map (which it shouldn't be) align the top side primarily
//        if (bottom > this.mapView.getHeight()) {
//            yoffset = bottom - this.mapView.getHeight();
//        }
//        if (top < 0) {
//            yoffset = top;
//        }
//
//        if (xoffset != 0 || yoffset != 0) {
//            // animate map to center that's offset from the actual center
//            this.mapView.getController().animateTo(proj.fromPixels(this.mapView.getWidth()/2 + xoffset, this.mapView.getHeight()/2 + yoffset));
//        }
//    }
//
//    /**
//     * removes the bubble, returns if it did have a bubble to remove
//     * @return true if there was a bubble, false otherwise
//     */
    public boolean removeItem() {
//        if (this.bubbleView != null) {
//            setItem(null);
//            return true;
//        }
        return false;
    }
//
//    /**
//     * if the bubble is shown, make sure it's on top of other children of the mapview
//     */
    public void bringToFront() {
//        if (this.bubbleView != null) {
//            this.bubbleView.bringToFront();
//        }
    }
//
    public void createBubble(final MainActivity context, final MapItem item) {
//        // todo recycle the inflated object, just clean it up?
//        this.bubbleView = context.getLayoutInflater().inflate(R.layout.bubble, null);
//        ((TableLayout)this.bubbleView.findViewById(R.id.bubble)).setColumnShrinkable(0, true);
//        ((TextView)this.bubbleView.findViewById(R.id.bubble_title)).setText(item.getTitle());
//
//        this.bubbleButtonAvail = (ImageView) this.bubbleView.findViewById(R.id.bubble_report_available);
//        this.bubbleButtonFull = (ImageView) this.bubbleView.findViewById(R.id.bubble_report_full);
//        this.bubbleButtonPin = (ImageView) this.bubbleView.findViewById(R.id.bubble_pin);
//
//        if (item instanceof Parking) {
//            final Parking p = (Parking)item;
//            updateAvailability();
//            updatePinnedStatus();
//            fillDetails((ViewGroup)this.bubbleView.findViewById(R.id.bubble_details));
//
//            // register click listeners on carpark buttons
//            this.bubbleButtonAvail.setOnClickListener(new OnClickListener() {
//                public void onClick(View v) {
//                    BubbleOverlay.this.activity.reportAvailability(p, true);
//                }
//            });
//            this.bubbleButtonFull.setOnClickListener(new OnClickListener() {
//                public void onClick(View v) {
//                    BubbleOverlay.this.activity.reportAvailability(p, false);
//                }
//            });
//            this.bubbleButtonPin.setOnClickListener(new OnClickListener() {
//                public void onClick(View v) {
//                    BubbleOverlay.this.activity.pinCarpark(p, !BubbleOverlay.this.parkingsService.isPinnedCarpark(p));
//                }
//            });
//        }
//
//        // register click listeners on more buttons
//        this.bubbleView.findViewById(R.id.bubble_top_box).setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                MainActivity.showDetailsForCarpark(context, item);
//            }
//        });
//        this.bubbleView.findViewById(R.id.bubble_directions).setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                BubbleOverlay.this.activity.openNavigationTo(item);
//            }
//        });
//
//        this.nextDetailsUpdateTriggersRepositioning = true;
//
//        // set up resizing and repositioning
//        this.bubbleView.post(new Runnable() {
//            public void run() {
//                adjustBubbleHeight();
//                makeBubbleOnScreen();
//            }
//        });
//        int yOffset = (int)(-16 * this.density / 1.5f);  // the bubble's beak starts 16px below top in the HDPI (1.5 density) nine-patch
//        MapView.LayoutParams layoutParams = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT, MapView.LayoutParams.WRAP_CONTENT, item.point, 0, yOffset, MapView.LayoutParams.TOP|MapView.LayoutParams.CENTER_HORIZONTAL);
//        this.bubbleView.setLayoutParams(layoutParams);
//        this.mapView.addView(this.bubbleView);
    }
//
//    private void adjustBubbleHeight() {
//        if (this.bubbleView == null) {
//            return;
//        }
//
//        View details = this.bubbleView.findViewById(R.id.bubble_details);
//        View detailsScrollView = this.bubbleView.findViewById(R.id.bubble_details_scrollview);
//        View detailsTopSeparator = this.bubbleView.findViewById(R.id.bubble_top_separator);
//        LayoutParams lpS = detailsScrollView.getLayoutParams();
//        final int maxDetailsHeight = this.mapView.getHeight()/3;
//        final int height = details.getHeight();
//        if (height == 0) {
//            detailsTopSeparator.setVisibility(View.GONE);
//        } else {
//            detailsTopSeparator.setVisibility(View.VISIBLE);
//        }
//        lpS.height = height < maxDetailsHeight ? height : maxDetailsHeight;
////        final int change = lpS.height - detailsScrollView.getHeight();
//        detailsScrollView.setLayoutParams(lpS);
//
////        if (height != 0) {
////            ScaleAnimation anim = new ScaleAnimation(1, 1, 0, change < 0 ? change : 0);
////        }
//    }
//
//    /**
//     * update availability info in the bubble if the car park matches the bubble
//     * @param carpark
//     */
    void updateAvailability(Parking carpark) {
//        if (this.bubbleCarpark == null || !this.bubbleCarpark.equals(carpark)) {
//            return;
//        }
//
//        updateAvailability();
    }
//
//    /**
//     * update availability info in the bubble no matter what
//     */
    void updateAvailability() {
//        if (this.bubbleCarpark == null) {
//            return;
//        }
//
//        this.bubbleCarpark = Parking.getParking(this.bubbleCarpark.id);
//        if (this.bubbleCarpark == null) {
//            setItem(null);
//            return;
//        }
//
//        ((TextView)this.bubbleView.findViewById(R.id.bubble_availability)).setText(ParkingDetailsActivity.getAvailabilityDescription(this.activity, this.bubbleCarpark, false));
//        // todo also the appropriate button might want to be highlighted
//        if (this.bubbleCarpark.isAvailabilityReportOutdated()) {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_32px);
//        } else if (this.bubbleCarpark.getEffectiveAvailability() == Parking.Availability.AVAILABLE) {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_halo_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_32px);
//        } else {
//            this.bubbleButtonAvail.setImageResource(R.drawable.green_circle_32px);
//            this.bubbleButtonFull.setImageResource(R.drawable.red_circle_halo_32px);
//        }
    }
//
    void updatePinnedStatus(Parking p) {
//        if (this.bubbleCarpark == null || (p != null && !this.bubbleCarpark.equals(p))) {
//            return;
//        }
//        updatePinnedStatus();
    }
//
//    private void updatePinnedStatus() {
//        if (this.parkingsService.isPinnedCarpark(this.bubbleCarpark)) {
//            this.bubbleButtonPin.setImageResource(R.drawable.btn_pin_active);
//        } else {
//            this.bubbleButtonPin.setImageResource(R.drawable.btn_pin_inactive);
//        }
//    }
//
//    private void fillDetails(ViewGroup layout) {
//        ParkingDetailsActivity.createDetailsEntries(this.activity, layout, this.bubbleCarpark, null, this.showUnconfirmedProperties, false);
//    }
//
//    /**
//     * update the details in the bubble, but only if the given car park matches the currently held car park
//     * @param carpark may be null in which case details are updated
//     */
    void updateDetails(Parking carpark) {
//        if (this.bubbleCarpark == null) {
//            return;
//        }
//        if (carpark == null || this.bubbleCarpark.equals(carpark)) {
//            // update Parking instance in case it's stale
//            this.bubbleCarpark = Parking.getParking(this.bubbleCarpark.id);
//            if (this.bubbleCarpark == null) {
//                setItem(null);
//                return;
//            }
//
//            // update title
//            ((TextView)this.bubbleView.findViewById(R.id.bubble_title)).setText(this.bubbleCarpark.getTitle());
//
//            // update details
//            ViewGroup layout = (ViewGroup)this.bubbleView.findViewById(R.id.bubble_details);
//            layout.removeAllViews();
//            fillDetails(layout);
//            if (this.bubbleView != null) {
//                this.bubbleView.post(new Runnable() {
//                    public void run() {
//                        adjustBubbleHeight();
//                        if (BubbleOverlay.this.nextDetailsUpdateTriggersRepositioning) {
//                            BubbleOverlay.this.nextDetailsUpdateTriggersRepositioning = false;
//                            if (BubbleOverlay.this.bubbleView != null) {
//                                BubbleOverlay.this.bubbleView.post(new Runnable() {
//                                    public void run() {
//                                        makeBubbleOnScreen();
//                                    }
//                                });
//                            }
//                        }
//                    }
//                });
//            }
////            Log.d(TAG, "updated details for car park \"" + this.bubbleCarpark.getTitle() + "\" @" + System.identityHashCode(this.bubbleCarpark));
//        }
    }
}
