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

import java.util.Collection;

import uk.ac.open.kmi.parking.service.ParkingsService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MapView.LayoutParams;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * a map overlay that contains some DrawableOverlayItems
 * @author Jacek Kopecky
 *
 */
public class DrawableItemsOverlay extends Overlay {

    @SuppressWarnings("unused")
    private static final String TAG = "item overlay";

    private boolean noShadow;

    private ParkingsService parkingsService;

    private final BubbleOverlay bubbleOverlay;

    // these are pixels per meter on the equator
    // at zoom level 1, equator (ca 40000km) is 2^8 = 2<<7 pix (by definition in MapView)
    // at zoom level X, equator is 2<<(6+X) pix; 1m is 2<<(4+X)/1e7
    // we want to start showing car parks between zoom levels 15 and 16
    // we want to make them full-size between zoom levels 17 and 18

    // if the screen shows at least this many pix per meter (on the equator), show full-sized icons - this is just below zoom level 18
    private final static float mpxThresholdFull = (2<<22)/1e7f*.9f;
    // if the screen shows between full and tinyFirst pix per meter, transition between small and full-sized icons - this is just above zoom level 17
    private final static float mpxThresholdTinyFirst = (2<<21)/1e7f*1.1f;
    // if the screen shows less than tinyLast pix per meter, show tiny disappearing icons - this is just below zoom level 16
    private final static float mpxThresholdTinyLast = (2<<20)/1e7f*.9f;
    // if the screen shows less than this pix per meter, no icons are visible - this is just above zoom level 15
    private final static float mpxThresholdNone = (2<<19)/1e7f*1.1f;

    private static final float TINY_ICON_SIZE = .6f;

    private static final double PHYSICAL_PIN_DIAMETER_IN = .25f; // about 7.5mm diameter for where touch should register on a pin

    private float xdpi, ydpi;

    private View addingCarparkAnim;
    private Rect addingCarparkAnimBounds;

    private MainActivity activity;

    @SuppressWarnings("unused")
    private Context context; // for toasts
    private boolean isPinch = false;

    /**
     * sets whether the overlay should have a shadow
     * @param activity context
     * @param shadow whether the overlay should have shadows of the icons
     * @param bubble overlay for the bubbles created on a tap
     * @param ps the container for drawable items (todo should be an interface DrawableOverlayItemContainer or somesuch)
     * @param addCarparkAnim view whose background is the animation to show when highlighting adding car park in the middle of the map
     * @param addCarparkAnimBounds the bounds of the highlight view
     * @param xdpi the physical x dpi of the display
     * @param ydpi the physical y dpi of the display
     * @param ctxt context for toasts
     */
    public DrawableItemsOverlay(MainActivity activity, boolean shadow, BubbleOverlay bubble, ParkingsService ps, View addCarparkAnim, Rect addCarparkAnimBounds, float xdpi, float ydpi, Context ctxt) {
        this.activity = activity;
        this.noShadow = !shadow;
        this.bubbleOverlay = bubble;
        this.parkingsService = ps;
        this.addingCarparkAnim = addCarparkAnim;
        this.addingCarparkAnimBounds = addCarparkAnimBounds;
        this.context = ctxt;
        this.xdpi = xdpi;
        this.ydpi = ydpi;
    }

    /**
     * change whether the overlay should draw shadows of icons
     * @param shadow whether shadows should be drawn
     */
    public void setShadow(boolean shadow) {
        this.noShadow = !shadow;
    }

    private float currentSize = 1;
    private float oldMpx = 0;
    private Canvas bufferCanvas = null;
    private Bitmap bufferBitmap = null;
    private int bufferOffsetX = Integer.MAX_VALUE; // how far the left of the buffer canvas is to the LEFT of the map canvas
    private int bufferOffsetY = Integer.MAX_VALUE; // max value means forcing redraw
    private final Point tmpPt1 = new Point();
    private final Point tmpPt2 = new Point();
    private GeoPoint oldMapCenter = null;
    private static final Paint paint = new Paint();

    @Override
    public void draw(Canvas canvas, final MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }
//        long drawStartTime = System.currentTimeMillis();

        Projection proj = mapView.getProjection();

        // how many pixels are needed to display one meter at the equator at the current zoom level
        float mpx = proj.metersToEquatorPixels(100000f)/100000f;  // 100000 to get useful accuracy

        // todo on a bigger screen, they should grow to full size sooner
        // conversely, on a smaller screen, they should stay small longer

        // set up currentsize
        if (mpx < mpxThresholdNone) {
            this.currentSize = 0f;
            this.bubbleOverlay.removeItem();
            this.activity.setTooFarOut(true);
            mapView.post(new Runnable() { public void run() { updateHighlightedCarpark(null, false, mapView); }});
            return; // no need to show anything, we're zoomed too far out
        } else if (mpx > mpxThresholdFull) {
            this.currentSize = 1f;
        } else if (mpx > mpxThresholdTinyFirst) {
            // small to normal
            this.currentSize = TINY_ICON_SIZE + (1-TINY_ICON_SIZE)*(mpx - mpxThresholdTinyFirst)/(mpxThresholdFull-mpxThresholdTinyFirst);
        } else if (mpx > mpxThresholdTinyLast) {
            this.currentSize = TINY_ICON_SIZE;
        } else {
            this.currentSize = TINY_ICON_SIZE * (mpx-mpxThresholdNone)/(mpxThresholdTinyLast-mpxThresholdNone);
        }
        this.activity.setTooFarOut(false);

        boolean doForceRedraw = false;
        if (this.forceRedraw) {
            doForceRedraw = true;
            this.forceRedraw = false;
//            Log.d(TAG, "redraw because of new car parks");
        }

        if (this.bufferCanvas == null) {
            synchronized (this) {
                if (this.bufferCanvas == null) {
                    this.bufferBitmap = Bitmap.createBitmap(canvas.getWidth()*3, canvas.getHeight()*3, Config.ARGB_8888);
                    this.bufferCanvas = new Canvas(this.bufferBitmap);
                    doForceRedraw = true;
                    Log.d(TAG, "buffer allocated");
//                    Log.d(TAG, "redraw first");
                }
            }
        }

        GeoPoint newMapCenter = mapView.getMapCenter();

        if (this.oldMpx != mpx) {
            this.oldMpx = mpx;
            doForceRedraw = true;
//            Log.d(TAG, "redraw due to zooming");
        } else {
            // this.oldMapCenter cannot be null here because it's only null the first time draw() is called, and then this.oldMpx is 0 so the above is triggered
            proj.toPixels(newMapCenter, this.tmpPt1);
            proj.toPixels(this.oldMapCenter, this.tmpPt2);
            this.bufferOffsetX += this.tmpPt1.x - this.tmpPt2.x;
            this.bufferOffsetY += this.tmpPt1.y - this.tmpPt2.y;
        }

        this.oldMapCenter = newMapCenter;

        if (this.bufferOffsetX < 0 || (this.bufferCanvas.getWidth() - this.bufferOffsetX) < canvas.getWidth() ||
                this.bufferOffsetY < 0 || (this.bufferCanvas.getHeight() - this.bufferOffsetY) < canvas.getHeight()) {
            doForceRedraw = true;
//            Log.d(TAG, "redraw due to moving too far: " + this.bufferOffsetX + "x" + this.bufferOffsetY);
        }

        final Parking currentCarpark = this.parkingsService.getCurrentCarpark();
        mapView.post(new Runnable() { public void run() { updateHighlightedCarpark(currentCarpark, DrawableItemsOverlay.this.activity.addingMode, mapView); }});

        if (doForceRedraw) {
            this.bufferBitmap.eraseColor(Color.TRANSPARENT);
            this.bufferOffsetX = (this.bufferBitmap.getWidth() - mapView.getWidth()) / 2;
            this.bufferOffsetY = (this.bufferBitmap.getHeight() - mapView.getHeight()) / 2;
//          android.util.Log.d("parkme", "currentSize " + this.currentSize);

            Collection<DrawableOverlayItem> items = this.parkingsService.getSortedCurrentItems(mapView.getMapCenter(), mapView.getLongitudeSpan(), mapView.getLatitudeSpan());

            // todo group to avoid overlapping?
            Point point = this.tmpPt1;

            shadow = !this.noShadow;
            do {
                for (DrawableOverlayItem item : items) {
//                    if (currentCarpark == item && !shadow) {
//                        continue; // current car park displayed on top
//                        // shadow is still displayed normally
//                    }
                    proj.toPixels(item.point, point);
                    drawScaledDrawable(this.bufferCanvas, point, item.getDrawable(), shadow);
                }
                if (shadow) {
                    shadow = false;
                } else {
                    break;
                }
            } while (true);

//            long drawEndTime = System.currentTimeMillis();
//            Log.d(TAG, "drawing time " + (drawEndTime - drawStartTime));
        }

        canvas.drawBitmap(this.bufferBitmap, -this.bufferOffsetX, -this.bufferOffsetY, paint);
    }

    synchronized void onStop() {
        if (this.bufferBitmap != null) {
            this.bufferBitmap.recycle();
            this.bufferCanvas = null;
            this.bufferBitmap = null;
            Log.d(TAG, "buffer freed");
            System.gc();
        }
    }

    private boolean forceRedraw = false;

    void forceRedraw() {
        this.forceRedraw = true;
    }

    private void updateHighlightedCarpark(Parking currentCarpark, boolean addingCarpark, MapView mapView) {
        if (this.highlightedCarpark == currentCarpark &&
                (this.highlightedCarpark == null || this.highlightedCarpark.getEffectiveAvailability() == this.highlightedAvailability) &&
                this.highlightedSize == this.currentSize &&
                this.highlightedAdding == addingCarpark) {
            return;
        }

        if (this.highlightedCarpark != currentCarpark || this.highlightedAdding != addingCarpark || (this.highlightedCarpark != null && this.highlightedCarpark.getEffectiveAvailability() != this.highlightedAvailability)) {
            this.highlightedSize = this.currentSize;
            this.highlightedCarpark = currentCarpark;
            this.highlightedAdding = addingCarpark;

            // clear the old one
            if (this.highlightView != null) {
                ((AnimationDrawable)this.highlightView.getBackground()).stop();
//                Log.d(TAG, "removing view " + this.highlightView + " in view " + this);
                mapView.removeView(this.highlightView);
                this.highlightView = null;
            }

            // assuming currentCarpark is null when adding car park
            if (currentCarpark != null) {
                // create image view and put it in mapview
                this.highlightedAvailability = currentCarpark.getEffectiveAvailability();
                this.highlightView = currentCarpark.getHighlight();
                this.highlightBdb = currentCarpark.getDrawable().getBounds();

                MapView.LayoutParams layoutParams = new MapView.LayoutParams(
                        (int) ((this.highlightBdb.right-this.highlightBdb.left) * this.currentSize),
                        (int) ((this.highlightBdb.bottom-this.highlightBdb.top) * this.currentSize),
                        currentCarpark.point,
                        (int) (this.highlightBdb.left * this.currentSize),
                        (int) (this.highlightBdb.top * this.currentSize),
                        MapView.LayoutParams.TOP_LEFT);
                this.highlightView.setLayoutParams(layoutParams);
//                Log.d(TAG, "adding view " + this.highlightView + " in view " + this);
                mapView.addView(this.highlightView, 0);

                AnimationDrawable anim = (AnimationDrawable)this.highlightView.getBackground();
                anim.start();

//                StringBuffer sb = new StringBuffer("animation durations: ");
//                for (int i=0; i<anim.getNumberOfFrames(); i++) {
//                    sb.append(anim.getDuration(i));
//                    sb.append(", ");
//                }
//                Log.d(TAG, sb.toString());
            } else if (addingCarpark) {
                this.highlightView = this.addingCarparkAnim;
                this.highlightBdb = this.addingCarparkAnimBounds;

                int mapWidth = mapView.getWidth();
                int mapHeight = mapView.getHeight();

                MapView.LayoutParams layoutParams = new MapView.LayoutParams(
                        this.highlightBdb.right-this.highlightBdb.left,
                        this.highlightBdb.bottom-this.highlightBdb.top,
                        mapWidth / 2 + this.highlightBdb.left,
                        mapHeight / 2 + this.highlightBdb.top,
                        MapView.LayoutParams.TOP_LEFT);
                this.highlightView.setLayoutParams(layoutParams);
                mapView.addView(this.highlightView);
                // there should be no bubbles
//                this.bubbleOverlay.bringToFront();

                AnimationDrawable anim = (AnimationDrawable)this.highlightView.getBackground();
                anim.start();
            }
        }

        // adapt the size of the hightlighted pin if it's for an existing car park (don't make added car park smaller)
        if (this.highlightedSize != this.currentSize && this.highlightedCarpark != null) {
            MapView.LayoutParams lp = (LayoutParams) this.highlightView.getLayoutParams();
            lp.width = (int) ((this.highlightBdb.right-this.highlightBdb.left) * this.currentSize);
            lp.height = (int) ((this.highlightBdb.bottom-this.highlightBdb.top) * this.currentSize);
            lp.x = (int) (this.highlightBdb.left * this.currentSize);
            lp.y = (int) (this.highlightBdb.top * this.currentSize);
            this.highlightView.setLayoutParams(lp);
            this.highlightedSize = this.currentSize;
        }
    }

    private Parking highlightedCarpark = null;
    private View highlightView = null;
    private Rect highlightBdb = null;
    private Parking.Availability highlightedAvailability = null;
    private float highlightedSize = 1;
    private boolean highlightedAdding = false;

    private static final Rect tmpRect = new Rect();

    private void drawScaledDrawable(Canvas canvas, Point point, Drawable drawable, boolean shadow) {
        drawable.copyBounds(tmpRect);
        drawable.setBounds((int)(tmpRect.left * this.currentSize), (int)(tmpRect.top * this.currentSize), (int)(tmpRect.right * this.currentSize), (int)(tmpRect.bottom * this.currentSize));
        drawAt(canvas, drawable, point.x + this.bufferOffsetX, point.y + this.bufferOffsetY, shadow);
        drawable.setBounds(tmpRect);
    }

    @Override
    public boolean onTap(GeoPoint point, MapView mapView) {
        // onTap should not be fired on release of pinch zoom
        if (this.isPinch) {
            return false;
        }
        if (this.currentSize == 0f) {
            return false;
        }

        // when adding a car park, tap on a map will move there
        if (this.activity.addingMode) {
            mapView.getController().animateTo(point);
            return true;
        }

        DrawableOverlayItem tapped = null;
        Projection proj = mapView.getProjection();
        Point pix = proj.toPixels(point, null);

        // todo the current car park should be checked first because it's in the foreground

        Point ipix = new Point();
        Collection<DrawableOverlayItem> items = this.parkingsService.getSortedCurrentItems();

        double nearestDistance = PHYSICAL_PIN_DIAMETER_IN;

        for (DrawableOverlayItem item : items) {
            proj.toPixels(item.point, ipix);
            int dx = pix.x - ipix.x;
            int dy = pix.y - ipix.y;

            Drawable idr = item.getDrawable();
            idr.copyBounds(tmpRect);
            tmpRect.left *= this.currentSize;
            tmpRect.right *= this.currentSize;
            tmpRect.top *= this.currentSize;
            tmpRect.bottom *= this.currentSize;

            if ((dx > tmpRect.left) && (dx < tmpRect.right) && (dy > tmpRect.top) && (dy < tmpRect.bottom) ) {
                tapped = item;
                nearestDistance = 0;
            } else if (nearestDistance != 0) {
                // tolerance of ca .25in around the center of the tmpRect for the tap:
                double dist = Math.hypot((dx-(tmpRect.left + tmpRect.right)/2)/this.xdpi, (dy-(tmpRect.top + tmpRect.bottom)/2)/this.ydpi);
                if (dist < nearestDistance) {
                    nearestDistance = dist;
                    // set the tapped item to the current because it's the nearest so far
                    tapped = item;
                }
            }
        }

        if (tapped != null) {
            this.bubbleOverlay.setItem(tapped);
            mapView.invalidate();
            this.firstTapTime = 0;
            return true;
        } else {
            if (this.bubbleOverlay.removeItem()) {
                this.firstTapTime = 0;
                return true;
            } else {
                return super.onTap(point, mapView);
            }
        }
    }

    private long firstTapTime = 0;

    @Override
    public boolean onTouchEvent(MotionEvent e, MapView mapView) {
        int fingers = e.getPointerCount();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            this.isPinch = false; // Touch DOWN, don't know if it's a pinch yet
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.firstTapTime < 300l) {
                // double tap
                mapView.getController().zoomInFixing((int)e.getX(), (int)e.getY());
                this.firstTapTime = 0;
                return true;
            } else {
                this.firstTapTime = currentTime;
            }
        }
        if (e.getAction() == MotionEvent.ACTION_MOVE && fingers == 2) {
            this.isPinch = true; // Two fingers, def a pinch
        }
        return super.onTouchEvent(e, mapView);
    }
}
