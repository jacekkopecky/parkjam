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

import java.util.List;

import uk.ac.open.kmi.parking.service.ParkingsService;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class PinnedDrawerListAdapter extends BaseAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = "pinned drawer adapter";

    private MainActivity activity;
    private List<Parking> currentPinnedCarparks = null;
    private ParkingsService parkingsService;

    private int drawerEmptyTextColor;
    private int drawerTextColor;
    private int drawerEmptyTextSize;
    private int drawerTextSize;
    private int drawerEntryPadding;

    public PinnedDrawerListAdapter(MainActivity ctxt) {
        this.activity = ctxt;
        this.parkingsService = ParkingsService.get(ctxt);
        this.currentPinnedCarparks = this.parkingsService.listKnownPinnedCarparks();

        this.drawerEmptyTextColor = ctxt.getResources().getColor(R.color.drawer_empty_text);
        this.drawerTextColor = ctxt.getResources().getColor(R.color.drawer_text);
        this.drawerEmptyTextSize = ctxt.getResources().getDimensionPixelSize(R.dimen.pinned_drawer_empty_text_size);
        this.drawerTextSize = ctxt.getResources().getDimensionPixelSize(R.dimen.pinned_drawer_text_size);
        this.drawerEntryPadding = ctxt.getResources().getDimensionPixelSize(R.dimen.pinned_drawer_entry_padding);
//        Log.d(TAG, "adapter created");
    }

    public boolean update() {
        this.currentPinnedCarparks = this.parkingsService.listKnownPinnedCarparks();
        notifyDataSetChanged();
        return !this.currentPinnedCarparks.isEmpty();
    }

    public void updateIfContains(Parking p) {
        if (this.currentPinnedCarparks.contains(p)) {
            update();
        }
    }

    public int getCount() {
        int count = this.currentPinnedCarparks.size();
        return count > 0 ? count : 1;
    }

    public Parking getItem(int position) {
        return this.currentPinnedCarparks.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return this.currentPinnedCarparks.isEmpty() ? 0 : 1;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (this.currentPinnedCarparks.isEmpty()) {
            // return placeholder that says here will be pinned carparks
            TextView retval;
            if (convertView != null && convertView instanceof TextView) {
                retval = (TextView) convertView;
            } else {
                retval = new TextView(this.activity);
                retval.setTextColor(this.drawerEmptyTextColor);
                retval.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.drawerEmptyTextSize);
                retval.setPadding(this.drawerEntryPadding, this.drawerEntryPadding, this.drawerEntryPadding, this.drawerEntryPadding);
                retval.setTypeface(Typeface.SANS_SERIF);
                retval.setText(R.string.drawer_empty);
            }
            return retval;
        } else {
            // todo better view of a car park -- need to add an indicator of availability (circle) and text about availability (tiny, same as in bubble)
            PinnedCarparkView retval;
            if (convertView != null && convertView instanceof PinnedCarparkView) {
                retval = (PinnedCarparkView) convertView;
            } else {
                retval = new PinnedCarparkView(this.activity);
                retval.setTextColor(this.drawerTextColor);
                retval.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.drawerTextSize);
                retval.setPadding(this.drawerEntryPadding, this.drawerEntryPadding, this.drawerEntryPadding, this.drawerEntryPadding);
                retval.setBackgroundResource(R.drawable.bubble_highlight_background);
                retval.setTypeface(Typeface.SANS_SERIF);
                this.activity.registerForContextMenu(retval);
                retval.setOnClickListener(this.pinnedItemOnClickListener);
            }
            retval.setCarpark(this.currentPinnedCarparks.get(position));
            return retval;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return !this.currentPinnedCarparks.isEmpty();
    }

    @Override
    public boolean isEnabled(int position) {
        return !this.currentPinnedCarparks.isEmpty();
    }

    private final OnClickListener pinnedItemOnClickListener = new OnClickListener() {
        public void onClick(View v) {
            PinnedDrawerListAdapter.this.activity.centerOnCarpark(((PinnedCarparkView)v).getCarpark(), false);
        }
    };

    public static class PinnedCarparkView extends TextView {

        private Parking p;

        public PinnedCarparkView(Context context) {
            super(context);
        }

        public void setCarpark(Parking p) {
            SpannableStringBuilder title = new SpannableStringBuilder(p.getTitle());
            title.append("\n");
            int spanStart = title.length();
            title.append(ParkingDetailsActivity.getAvailabilityDescription(this.getContext(), p, false));
            title.setSpan(new RelativeSizeSpan(.75f), spanStart, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            setText(title);
            this.p = p;

            setCompoundDrawablesWithIntrinsicBounds(0, 0, p.getEffectiveAvailability() == Parking.Availability.AVAILABLE ?  R.drawable.green_circle_32px : R.drawable.red_circle_32px, 0);
        }

        public Parking getCarpark() {
            return this.p;
        }



    }
}
