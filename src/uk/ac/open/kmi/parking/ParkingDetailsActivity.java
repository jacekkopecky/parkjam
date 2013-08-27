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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.open.kmi.parking.Parking.Availability;
import uk.ac.open.kmi.parking.ontology.Ontology;
import uk.ac.open.kmi.parking.service.CarparkAvailabilityUpdateListener;
import uk.ac.open.kmi.parking.service.CarparkDetailsUpdateListener;
import uk.ac.open.kmi.parking.service.ParkingsService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * the activity that shows the details of a car park
 * @author Jacek Kopecky
 *
 */
public class ParkingDetailsActivity extends Activity implements CarparkDetailsUpdateListener, CarparkAvailabilityUpdateListener {
    private static final int AUTO_LINK_MASK = Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS;

    @SuppressWarnings("unused")
    private static final String TAG = "parking details activity";

    // enumerate and sort properties that have presentation:order (double value)
    // for each of them, if the car park has it, find its presentation:label, show label, show value
    // later, give an option to change each value, and to add any value, shown by presentation:labels and sorted alphabetically (or by pres:order?)

    private Parking parking;
    private ParkingsService parkings;

    private TextView availabilityTextView;
    private ImageView reportButtonAvail;
    private ImageView reportButtonFull;
    private TextView carparkName;
    private ViewGroup detailsLayout;
    private ImageView editDetailsButton;

    private static Drawable editButtonDrawable;
    private static Drawable editButtonDoneDrawable;
    private static Drawable smallEditButtonDrawable;
    private static Drawable smallAddButtonDrawable;
    private static Drawable lineSeparator;
    private static Drawable lineSeparatorInvisible;

    private static int colorEntryBackground;
    private static int colorEntryText;
    private static int colorBubbleEntryText;
    private static int colorBubbleLinkText;
    private static int colorAddEntryText;
    private static int colorEntrySelected;
    private static int entryPaddingSide = -1;
    private static int entryPaddingTopBottom = -1;
    private static float detailsBubbleTextSize;
    private static float detailsTextSize;

    private static boolean initialized = false;

    private static SpannableString unverified;


    static synchronized void staticInit(Context context) {
        if (initialized) {
            return;
        }

        Resources res = context.getResources();

        entryPaddingSide = res.getDimensionPixelOffset(R.dimen.details_entry_side_padding);
        entryPaddingTopBottom = res.getDimensionPixelOffset(R.dimen.details_entry_topbottom_padding);

        editButtonDrawable = res.getDrawable(R.drawable.ic_menu_edit_purple);
        editButtonDoneDrawable = res.getDrawable(R.drawable.ic_menu_done_purple);
        smallEditButtonDrawable = res.getDrawable(R.drawable.ic_menu_edit_small);
        smallEditButtonDrawable.setBounds(0, 0, smallEditButtonDrawable.getIntrinsicWidth(), smallEditButtonDrawable.getIntrinsicHeight());
        smallAddButtonDrawable = res.getDrawable(R.drawable.ic_menu_plus_small);
        smallAddButtonDrawable.setBounds(0, 0, smallAddButtonDrawable.getIntrinsicWidth(), smallAddButtonDrawable.getIntrinsicHeight());

        unverified = new SpannableString(res.getText(R.string.details_unverified_property));
        unverified.setSpan(new ForegroundColorSpan(res.getColor(R.color.details_unverified_text)), 0, unverified.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        colorEntryBackground = res.getColor(R.color.details_entry_background);
        colorEntrySelected = res.getColor(R.color.details_entry_selected);
        colorEntryText = res.getColor(R.color.default_details_text);
        colorBubbleEntryText = res.getColor(R.color.bubble_text);
        colorBubbleLinkText = res.getColor(R.color.bubble_link);
        colorAddEntryText = res.getColor(R.color.details_add_entry_text);

        lineSeparator = res.getDrawable(R.drawable.details_line_separator);
        lineSeparator.setBounds(0, 0, res.getDisplayMetrics().widthPixels*2, LINE_SEPARATOR_HEIGHT); // *2 so that it covers the whole line
        lineSeparatorInvisible = res.getDrawable(R.drawable.details_line_separator_invisible);
        lineSeparatorInvisible.setBounds(0, 0, res.getDisplayMetrics().widthPixels*2, LINE_SEPARATOR_HEIGHT); // *2 so that it covers the whole line

        detailsTextSize = res.getDimension(R.dimen.details_text_size);
        detailsBubbleTextSize = res.getDimension(R.dimen.bubble_details_text_size);

        initialized = true;
    }

    private static final int LINE_SEPARATOR_HEIGHT = 1;

    private static PresentationOntology presentationOntology;

    private boolean showUnconfirmedProperties = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        staticInit(this);

        Intent intent = this.getIntent();
        Uri parkingID = intent.getData();

        this.parkings = ParkingsService.get(this);
        this.parking = Parking.getParking(parkingID);
        if (this.parking == null) {
            Toast.makeText(this, R.string.error_parking_not_found, Toast.LENGTH_LONG).show();
//            Log.e(TAG, "parking not found " + parkingID);
            this.finish();
            return;
        }

        preparePresentationOntology(this);

        setContentView(R.layout.parking_details);
        this.availabilityTextView = (TextView) findViewById(R.id.details_availability_text);
        this.reportButtonAvail = (ImageView) findViewById(R.id.details_button_avail);
        this.reportButtonFull = (ImageView) findViewById(R.id.details_button_full);
        this.carparkName = (TextView) findViewById(R.id.details_currpark);
        this.detailsLayout = (ViewGroup) findViewById(R.id.details_description);
        this.editDetailsButton = (ImageView) findViewById(R.id.details_button_edit_details);

        this.showUnconfirmedProperties = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true);

        // todo is it possible that this activity starts, gets the details, then tile downloader replaces the parking again and erases the details? if so, the updates (availability, details (but those shouldn't come any more)) would be lost
        setViewContents();

        this.parkings.registerCarparkDetailsUpdateListener(this);
        this.parkings.registerCarparkAvailabilityUpdateListener(this);
        this.parkings.updateParkingDetails(this.parking);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.stay);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        boolean unconfirmed = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Preferences.PREFERENCE_SHOW_UNCONFIRMED_PROPERTIES, true);
        if (unconfirmed != this.showUnconfirmedProperties) {
            this.showUnconfirmedProperties = unconfirmed;
            setViewContents();
        }
    }

    @Override
    public void onPause() {
        this.parkings.geocoder = null;
        this.parkings.updateParkingAvailabilityDetailsView(null);
        this.parkings.unregisterCarparkDetailsUpdateListener(this);
        this.parkings.unregisterCarparkAvailabilityUpdateListener(this);
        this.parkings.stopService(this);
        super.onPause();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_to_right);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.parkings.startService();
        this.parkings.registerCarparkDetailsUpdateListener(this);
        this.parkings.registerCarparkAvailabilityUpdateListener(this);
        this.parkings.updateParkingAvailabilityDetailsView(this.parking);
        this.parkings.geocoder = new Geocoder(this);
    }

    // todo if I had SPARQL (needs ARQ, see ARQoid port to android), I could just merge the presentation ontology with the car park model and
    // do a select ?label ?value where { _:a a parking; ?prop ?value. ?prop priority ?prio; label ?label. } order by ?prio
    private static class PresentationOntology {
        private class Item implements Comparable<Item> {
            Property id;
            double order;
            String label;
            String comment;

            public int compareTo(Item another) {
                int retval = Double.compare(this.order, another.order);
                return retval != 0 ? retval : this.label.compareTo(another.label);
            }

            @Override
            public String toString() {
                return this.label;
            }
        }

        List<Item> items;
        Item nameItem;
        ArrayAdapter<PresentationOntology.Item> adapter;
        Item selectOne;

        public PresentationOntology(Model model, Context context) {
            this.items = new ArrayList<Item>((int)model.size()/2);

            List<Item> preferredPropsList = new ArrayList<Item>((int)model.size()/2);
            this.selectOne = new Item();
            this.selectOne.order = -1; // must come first
            this.selectOne.label = context.getResources().getString(R.string.details_edit_select_one);
            this.selectOne.comment = context.getResources().getString(R.string.details_edit_select_one_comment);
            preferredPropsList.add(this.selectOne);

            for (StmtIterator properties = model.listStatements(null, Onto.PRES_order, (RDFNode)null); properties.hasNext(); ) {
                Statement stmt = properties.next();
                Resource prop = stmt.getSubject();
                Item item = new Item();
                item.id = model.createProperty(prop.getURI());
                item.order = stmt.getDouble();

                // todo handle language tags appropriately, currently assuming there's only one (@en) - both for label and for comment
                item.label = model.getProperty(prop, Onto.PRES_label).getString();

                Statement commentStmt = model.getProperty(prop, Onto.PRES_comment);
                if (commentStmt != null) {
                    item.comment = commentStmt.getString();
                }

                this.items.add(item);
                if (item.id.equals(RDFS.label)) {
                    this.nameItem = item;
                }

                if (model.contains(prop,  RDF.type, Onto.PRES_PreferredProperty)) {
                    preferredPropsList.add(item);
                }
            }
            Collections.sort(this.items);
            Collections.sort(preferredPropsList);
            this.adapter = new ArrayAdapter<PresentationOntology.Item>(context, R.layout.details_edit_dialog_property_item, preferredPropsList);
        }
    }

    private final SpannableStringBuilder tmpText = new SpannableStringBuilder(); // to be used only on the UI thread

    private void setViewContents() {
        setAvailabilityButtonState();

        this.availabilityTextView.setText(getAvailabilityDescription(this, this.parking, true));

        // prepare the title
        this.tmpText.clearSpans();
        this.tmpText.clear();
        this.tmpText.append(this.parking.getTitle());
        if (this.parking.unconfirmed) {
//            Log.d(TAG, "unconfirmed carpark");
            this.tmpText.append("\n");
            this.tmpText.append(unverified);
        }

        this.carparkName.setText(this.tmpText);
        this.detailsLayout.removeAllViews();

        // todo presentation ontology has p:datatype which should be handled somehow, e.g. for int data that's actually boolean 0,1 and we'd like to tweak the dialog for data entry, or tweak data display
        boolean gotSomething = createDetailsEntries(this, this.detailsLayout, this.parking, this.detailsEntryClickListener, this.showUnconfirmedProperties, true);

        TextView addInfo = newTextViewWithDetailsStyle(DescriptionEntry.Type.ADD_INFO);
        addInfo.setText(R.string.details_edit_add);
        addInfo.setTextColor(colorAddEntryText);
        addInfo.setCompoundDrawables(null, null, smallAddButtonDrawable, lineSeparatorInvisible);
        addInfo.setClickable(true);
        this.detailsLayout.addView(addInfo);

        if (this.parkings.isRememberedAddedCarpark(this.parking)) {
            TextView notIndexed = newTextViewWithDetailsStyle(DescriptionEntry.Type.EXTRA);
            notIndexed.setText(R.string.details_not_indexed);
            notIndexed.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
            notIndexed.setTextColor(getResources().getColor(R.color.details_unverified_text));
            this.detailsLayout.addView(notIndexed);
        }

        // todo bug: warning about notIndexed doesn't get underlined/overlined when editing

        if (!gotSomething) {
            TextView noInfo = newTextViewWithDetailsStyle(DescriptionEntry.Type.NO_INFO);
            noInfo.setText(R.string.details_no_information);
            this.detailsLayout.addView(noInfo);
        }

//        findViewById(R.id.details_services).setVisibility(View.GONE);
//        findViewById(R.id.details_businesses).setVisibility(View.GONE);
//        findViewById(R.id.details_events).setVisibility(View.GONE);
//        findViewById(R.id.details_users).setVisibility(View.GONE);

        TextView carparkUri = (TextView) findViewById(R.id.details_uri_text);

        StringBuilder uriText = new StringBuilder();
        uriText.append("URI: ");
        uriText.append(this.parking.id);
        carparkUri.setText(uriText);

        updateChildrenForEditMode();
    }

    static boolean createDetailsEntries(Context ctxt, ViewGroup detailsLayout, Parking parking, OnClickListener clickListener, boolean showUnconfirmedProperties, boolean longVersion) {
        Model model = parking.details;
        Resource parkingRes = model != null ? model.getResource(parking.id.toString()) : null;

        boolean gotSomething = false;
        if (model != null) {
            for (PresentationOntology.Item property : presentationOntology.items) {
                if (property.id.equals(parking.titleProperty)) {
                    continue;
                    // todo skipping this property because it was used for getting the name - what if there are multiple values in this property?
                }
                for (StmtIterator stmts = model.listStatements(parkingRes, property.id, (RDFNode)null); stmts.hasNext(); ) {
                    Statement s = stmts.next();
                    if (addDetailsEntry(ctxt, property, s.getObject(), false, detailsLayout, clickListener, longVersion)) {
                        gotSomething = true;
                    }
                }
            }
            if (showUnconfirmedProperties) {
                if (parking.hasAnyTitle() && !parking.hasExplicitTitle() && presentationOntology.nameItem != null) {
                    // mention the geocoded name property as unconfirmed rdfs:label
                    if (addDetailsEntry(ctxt, presentationOntology.nameItem, parking.getTitle(), true, detailsLayout, clickListener, longVersion)) {
                        gotSomething = true;
                    }
                }
                for (PresentationOntology.Item property : presentationOntology.items) {
                    for (StmtIterator unverifieds = model.listStatements(parkingRes, Onto.PARKING_hasUnverifiedProperties, (RDFNode)null); unverifieds.hasNext(); ) {
                        RDFNode bag = unverifieds.next().getObject();
                        if (!(bag instanceof Resource)) {
                            continue;
                        }
                        for (StmtIterator stmts = model.listStatements((Resource)bag, property.id, (RDFNode)null); stmts.hasNext(); ) {
                            Statement s = stmts.next();
                            if (addDetailsEntry(ctxt, property, s.getObject(), true, detailsLayout, clickListener, longVersion)) {
                                gotSomething = true;
                            }
                        }
                    }
                }
            }
        }
        return gotSomething;
    }

    private static boolean addDetailsEntry(Context ctxt, PresentationOntology.Item property, Object obj, boolean isUnverified, ViewGroup parent, OnClickListener clickListener, boolean longVersion) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append(property.label);
        text.append(": "); // todo this may need localization
        text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        String value = null;
        if (obj instanceof Literal) {
            value = ((Literal)obj).getLexicalForm();
        } else {
            value = obj.toString();
        }
        value = value.trim();
        if ("".equals(value)) {
            return false;
        }
        text.append(value);
        if (isUnverified) {
            text.append(" ");
            text.append(unverified);
        }
        TextView entry = newTextViewWithDetailsStyle(ctxt, property.id, property.label, property.comment, value, text, clickListener, longVersion);
        parent.addView(entry);
        return true;
    }

    private static TextView newTextViewWithDetailsStyle(Context ctxt, Property property, String label, String comment, String value, CharSequence text, OnClickListener clickListener, boolean longVersion) {
        return newTextViewWithDetailsStyle(ctxt, DescriptionEntry.Type.NORMAL, property, label, comment, value, text, clickListener, longVersion);
    }

    private TextView newTextViewWithDetailsStyle(DescriptionEntry.Type type) {
        return newTextViewWithDetailsStyle(this, type, null, null, null, null, null, this.detailsEntryClickListener, true);
    }

    private static TextView newTextViewWithDetailsStyle(Context ctxt, DescriptionEntry.Type type, Property property, String label, String comment, String value, CharSequence text, OnClickListener clickListener, boolean longVersion) {
        TextView view = new DescriptionEntry(ctxt, type, property, label, comment, value, text);
        view.setTypeface(Typeface.SANS_SERIF);
        if (longVersion) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsTextSize);
            view.setTextColor(colorEntryText);
            view.setLinkTextColor(colorBubbleLinkText);
            view.setBackgroundColor(colorEntryBackground);
            view.setPadding(entryPaddingSide,  entryPaddingTopBottom, entryPaddingSide, 0);
            view.setCompoundDrawablePadding(entryPaddingTopBottom);
            view.setCompoundDrawables(null, null, null, lineSeparatorInvisible);
            if (clickListener != null) view.setOnClickListener(clickListener);
        } else {
            view.setTextColor(colorBubbleEntryText);
            view.setLinkTextColor(colorBubbleLinkText);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsBubbleTextSize);
//            view.setBackgroundColor(colorBubbleEntryBackground);
        }
        view.setClickable(false);
        view.setAutoLinkMask(AUTO_LINK_MASK);
        view.setLinksClickable(true);
        view.setText(text);
        return view;
    }

    private final OnClickListener detailsEntryClickListener = new OnClickListener() {
        public void onClick(View v) {
            ParkingDetailsActivity.this.clickEditDetailsEntry((DescriptionEntry) v);
        }
    };

    private static class DescriptionEntry extends TextView {
        enum Type { NORMAL, NO_INFO, ADD_INFO, EXTRA };
        Type type;
        Property property;
        String value;
        String propertyLabel;
        String propertyComment;
        CharSequence text;

        public DescriptionEntry(Context ctxt, Type type, Property property, String label, String comment, String value, CharSequence text) {
            super(ctxt);
            this.type = type;
            this.property = property;
            this.propertyLabel = label;
            this.propertyComment = comment;
            this.value = value;
            this.text = text;
        }

        void resetText() {
            if (this.text == null) {
                return;
            }
            this.setText(this.text);
        }
    }

    public void onCarparkInformationUpdated(Parking p) {
        if (this.parking == p) {
            runOnUiThread(new Runnable() {
                public void run() {
                    ParkingDetailsActivity.this.setViewContents();
                }
            });
        }
    }

    public void onCarparkAvailabilityUpdated(final Parking p) {
        if (this.parking == p || this.parking.id.equals(p.id)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    ParkingDetailsActivity.this.parking = p;
                    ParkingDetailsActivity.this.availabilityTextView.setText(ParkingDetailsActivity.getAvailabilityDescription(ParkingDetailsActivity.this, ParkingDetailsActivity.this.parking, true));
                }
            });
        }
    }

    /**
     * formats description of availability
     * @param context for getting resources
     * @param parking the car park whose availability should be described
     * @param longVersion which version to produce, original (long, true) or shortened for bubble (false)
     * @return a string describing the availability
     */
    public static String getAvailabilityDescription(Context context, Parking parking, boolean longVersion) {
        String format;
        String availability = null, reportedAvailability = null, ago = null, pressButton = "";

        Resources res = context.getResources();

        if (longVersion) {
            if (parking.isAvailabilityReportOutdated()) {
                format = res.getString(R.string.availability_oldreport);
            } else if (parking.getReportedAvailabilityTimestamp() == null) {
                format = res.getString(R.string.availability_noreport);
            } else if (parking.getEffectiveAvailability() == Availability.UNKNOWN) {
                format = res.getString(R.string.availability_unknown);
            } else {
                format = res.getString(R.string.availability_normal);
            }
        } else {
            if (parking.isAvailabilityReportOutdated()) {
                format = res.getString(R.string.availability_short_oldreport);
            } else if (parking.getReportedAvailabilityTimestamp() == null) {
                format = res.getString(R.string.availability_short_noreport);
            } else if (parking.getEffectiveAvailability() == Availability.UNKNOWN) {
                format = res.getString(R.string.availability_unknown);
            } else {
                format = res.getString(R.string.availability_short_normal);
            }
        }

        availability = res.getString(parking.getEffectiveAvailability() == Availability.AVAILABLE ? R.string.availability_Available : R.string.availability_Full); // uppercase
        reportedAvailability = res.getString(parking.getReportedAvailability() == Availability.AVAILABLE ? R.string.availability_available : R.string.availability_full); // lowercase

        if (parking.getReportedAvailabilityTimestamp() != null) {
            int timeDeltaMinutes = (int)((System.currentTimeMillis() - parking.getReportedAvailabilityTimestamp()) / 60000l);
            if (timeDeltaMinutes <= 0) { // it's now (or in the future?)
                ago = res.getString(R.string.availability_age_now);
            } else if (timeDeltaMinutes < 60) {
                ago = res.getQuantityString(R.plurals.availability_age_minutes, timeDeltaMinutes, timeDeltaMinutes);
            } else {
                int timeDeltaHours = timeDeltaMinutes / 60;
                ago = res.getQuantityString(R.plurals.availability_age_hours, timeDeltaHours, timeDeltaHours);
            }
        }

        if (longVersion) {
            pressButton = res.getString(parking.getEffectiveAvailability() == Availability.AVAILABLE ? R.string.availability_report_as_full : R.string.availability_report_as_available);
        }

        return String.format(format, availability, reportedAvailability, ago, pressButton);
    }

    private static Thread presentationOntologyLoader = null;

    static synchronized void preparePresentationOntologyInNewThread(final Context ctxt) {
        if (presentationOntology == null && presentationOntologyLoader == null) {
            presentationOntologyLoader = new Thread(new Runnable() {
                public void run() {
                    preparePresentationOntology(ctxt);
                    presentationOntologyLoader = null; // so the thread can be reclaimed
                }
            });
            presentationOntologyLoader.start();
        }
    }

    private static synchronized void preparePresentationOntology(Context ctxt) {
        // todo every week, download and store in private cache the presentation-ontology file
        // when opening the file, first check that it exists in private cache, if not use the raw one in res/raw/
        if (presentationOntology != null) {
            return;
        }
        Model model = ModelFactory.createDefaultModel();
        model.read(ctxt.getResources().openRawResource(R.raw.presentation_ontology), Ontology.PRESENTATION_ONTOLOGY, "TURTLE");
        presentationOntology = new PresentationOntology(model, ctxt);
    }

    /**
     * called by the buttons to set "full" of "available"
     * @param view the button that was pressed
     */
    public void onButtonPressReport(View view) {
        if (this.parking != null) {
            boolean availability = view.equals(this.reportButtonAvail);
            this.parkings.submitAvailability(this.parking, availability);
            this.availabilityTextView.setText(getAvailabilityDescription(this, this.parking, true));
            setAvailabilityButtonState();
        }
    }

    private void setAvailabilityButtonState() {
        // todo set halo under current button
//            if (availability == Availability.AVAILABLE) {
//                this.currparkButton.setBackgroundResource(R.drawable.indy1spaces);
//            } else {
//                this.currparkButton.setBackgroundResource(R.drawable.indy1full);
//            }
        if (this.parking.isAvailabilityReportOutdated()) {
            this.reportButtonAvail.setImageResource(R.drawable.green_circle_48px);
            this.reportButtonFull.setImageResource(R.drawable.red_circle_48px);
        } else if (this.parking.getEffectiveAvailability() == Parking.Availability.AVAILABLE) {
            this.reportButtonAvail.setImageResource(R.drawable.green_circle_halo_48px);
            this.reportButtonFull.setImageResource(R.drawable.red_circle_48px);
        } else {
            this.reportButtonAvail.setImageResource(R.drawable.green_circle_48px);
            this.reportButtonFull.setImageResource(R.drawable.red_circle_halo_48px);
        }
    }

    private boolean editingMode = false;

    /**
     * called when the "edit" button on the "description" heading is pressed
     * @param v ignored
     */
    public void clickEditDetails(@SuppressWarnings("unused") View v) {
//        Log.d(TAG, "click edit details");
        this.editingMode = ! this.editingMode;

        updateChildrenForEditMode();
    }

    private void updateChildrenForEditMode() {

        this.editDetailsButton.setImageDrawable(this.editingMode ? editButtonDoneDrawable : editButtonDrawable);

        int childCount = this.detailsLayout.getChildCount();
        for (int i=0; i<childCount; i++) {
            View child = this.detailsLayout.getChildAt(i);
            if (child instanceof DescriptionEntry) {
                DescriptionEntry entry = (DescriptionEntry) child;
                switch (entry.type) {
                case NORMAL:
                    if (this.editingMode) {
                        entry.setCompoundDrawables(null, null, smallEditButtonDrawable, lineSeparator);
                        entry.setAutoLinkMask(0);
                        entry.resetText();
                        entry.setClickable(true);
                    } else {
                        entry.setClickable(false);
                        entry.setAutoLinkMask(AUTO_LINK_MASK);
                        entry.resetText();
                        entry.setCompoundDrawables(null, null, null, lineSeparatorInvisible);
                    }
                    break;
                case NO_INFO:
                    entry.setVisibility(this.editingMode ? View.GONE : View.VISIBLE);
                    break;
                case ADD_INFO:
                    entry.setVisibility(this.editingMode ? View.VISIBLE : View.GONE);
                    break;
                case EXTRA:
                    // no change
                }
            }
        }
    }

    /**
     * called when the "edit" button next to the car park's name is pressed
     * @param v ignored
     */
    public void clickEditTitle(@SuppressWarnings("unused") View v) {
//        Log.d(TAG, "click edit title");
        final EditText input = new EditText(this);
        if (this.parking.hasAnyTitle()) {
            // put in the current title
            input.setText(this.parking.getTitle());
        }
        new AlertDialog.Builder(this)
            .setTitle(R.string.details_edit_name)
            .setView(input)
            .setPositiveButton(R.string.details_edit_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    ParkingDetailsActivity.this.submitProperty(RDFS.label, value.toString(), null);
                }
            }).setNegativeButton(R.string.details_edit_cancel, null).show();
    }

    private void submitProperty(Property prop, String value, @SuppressWarnings("unused") String datatype) {
        // todo datatypes
//        Log.i(TAG, "submitting property " + prop + " with value \"" + value + "\" and datatype " + datatype);
        this.parkings.submitProperty(this.parking, prop, value);
        Toast.makeText(this, R.string.details_edit_submitted, Toast.LENGTH_SHORT).show();
    }

    private AlertDialog editDialog;

    private void clickEditDetailsEntry(final DescriptionEntry entry) {
        if (!this.editingMode) {
            return;
        }

        entry.setBackgroundColor(colorEntrySelected);
        entry.postDelayed(new Runnable() {
            public void run() {
                entry.setBackgroundColor(colorEntryBackground);
            }
        }, 200); // todo should be a declared constant somewhere

//        Log.i(TAG, "should edit \"" + entry.getText() + "\"");
        View dialogContent = getLayoutInflater().inflate(entry.type == DescriptionEntry.Type.NORMAL ? R.layout.details_edit_dialog : R.layout.details_edit_dialog_add, null);
        final EditText input = (EditText)dialogContent.findViewById(R.id.details_edit_dialog_edit);
        final TextView propcomment = (TextView)dialogContent.findViewById(R.id.details_edit_dialog_property_description);
        Spinner spinner = null;

        if (entry.type == DescriptionEntry.Type.NORMAL) {
            TextView propline = (TextView)dialogContent.findViewById(R.id.details_edit_dialog_heading);
            propline.setText(entry.propertyLabel);
            if (entry.propertyComment != null) {
                propcomment.setText(entry.propertyComment);
            } else {
                propcomment.setVisibility(View.GONE);
            }
            input.setText(entry.value);
        } else {
            spinner = (Spinner)dialogContent.findViewById(R.id.details_edit_dialog_property);
            spinner.setAdapter(presentationOntology.adapter);
        }

        this.editDialog = new AlertDialog.Builder(this)
            .setView(dialogContent)
            .setPositiveButton(R.string.details_edit_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface parent, int whichButton) {
                    Editable value = input.getText();
                    ParkingDetailsActivity.this.submitProperty(entry.property, value.toString(), null);
                }})
            .setNegativeButton(R.string.details_edit_cancel, null)
            .create();

        if (spinner != null) {
            spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int itemNr, long id) {
                    PresentationOntology.Item item = presentationOntology.adapter.getItem(itemNr);
                    ParkingDetailsActivity.this.editDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(item != presentationOntology.selectOne);
                    entry.property = item.id;
                    if (item.comment == null) {
                        propcomment.setVisibility(View.GONE);
                    } else {
                        propcomment.setText(item.comment);
                        propcomment.setVisibility(View.VISIBLE);
                    }
                }
                public void onNothingSelected(AdapterView<?> arg0) { /* do nothing */ }
            });
        }

        this.editDialog.show();
    }

    @Override
    protected void onStop() {
        if (this.editDialog != null) {
            this.editDialog.dismiss();
            this.editDialog = null;
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo in details menu, add pin/unpin for the displayed carpark, or add this as a graphic in the layout?
        getMenuInflater().inflate(R.menu.details_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
            startActivity(settingsActivity);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
