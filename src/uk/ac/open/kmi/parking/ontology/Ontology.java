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

package uk.ac.open.kmi.parking.ontology;

/**
 * this class, shared between the server and client of ParkJam, contains all the public URIs used in the app
 * @author Jacek Kopecky
 *
 */
@SuppressWarnings("javadoc")
public class Ontology {

    public static final String OMGEO_NS = "http://www.ontotext.com/owlim/geo#";
    public static final String VIRTRDF_NS = "http://www.openlinksw.com/schemas/virtrdf#";
    public static final String GEORSS_NS = "http://www.georss.org/georss/";
    public static final String GEOPOS_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

    public static final String GEOPOS_lat = GEOPOS_NS + "lat";
    public static final String GEOPOS_long = GEOPOS_NS + "long";

    public static final String LGO_NS = "http://linkedgeodata.org/ontology/";
    public static final String LGP_NS = "http://linkedgeodata.org/property/";
//    public static final String LGD_NS = "http://linkedgeodata.org/triplify/";

    public static final String LGO_Parking = LGO_NS + "Parking";

    public static final String DC_NS = "http://purl.org/dc/terms/";

    public static final String SSN_NS = "http://purl.oclc.org/NET/ssnx/ssn#";

    public static final String SSN_featureOfInterest = (SSN_NS + "featureOfInterest");
    public static final String SSN_observationSamplingTime = (SSN_NS + "observationSamplingTime");
    public static final String SSN_observationResult = (SSN_NS + "observationResult");
    public static final String SSN_hasValue = (SSN_NS + "hasValue");

    public static final String PROV_NS = "http://www.w3.org/ns/prov#";
    public static final String PROV_hadOriginalSource = (PROV_NS + "hadOriginalSource");

    // this is the NS for car park properties introduced in ParkJam - separate from the internal namespace PARKING_NS
    public static final String CARPARK_NS = "http://parking.kmi.open.ac.uk/ontologies/carpark#"; // todo put something at carpark ontology URI
    // contains carCountingZone and online_payment

    public static final String PARKING_NS = "http://parking.kmi.open.ac.uk/ontologies/parking#"; // todo put something at parking ontology URI
    public static final String PARKING_NS_UPPERCASE = PARKING_NS.toUpperCase();

    public static final String PARKING_UnverifiedInstance = (PARKING_NS + "UnverifiedInstance");
    public static final String PARKING_AvailabilityObservation = (PARKING_NS + "AvailabilityObservation");
    public static final String PARKING_AvailabilitySubmission = (PARKING_NS + "AvailabilitySubmission");
    public static final String PARKING_AvailabilityEstimate = (PARKING_NS + "AvailabilityEstimate");
    public static final String PARKING_binaryAvailability = (PARKING_NS + "binaryAvailability");
    public static final String PARKING_binaryAvailabilityTimestamp = (PARKING_NS + "binaryAvailabilityTimestamp"); // todo put this in the ontology
    public static final String PARKING_availabilityResource = (PARKING_NS + "availabilityResource");
    public static final String PARKING_updateResource = (PARKING_NS + "updateResource");
    public static final String PARKING_hasUnverifiedProperties = (PARKING_NS + "hasUnverifiedProperties"); // todo put this in the ontology
    public static final String PARKING_hasStatement = (PARKING_NS + "hasStatement"); // todo put this in the ontology
    public static final String PARKING_submissionTimestamp = (PARKING_NS + "submissionTimestamp");
    public static final String PARKING_submittedBy = (PARKING_NS + "submittedBy"); // todo put this in the ontology
    public static final String PARKING_anonymousSubmitter = (PARKING_NS + "anonymousSubmitter"); // todo put this in the ontology
    public static final String PARKING_geocodedTitle = (PARKING_NS + "geocodedTitle"); // todo put this in the ontology?
    public static final String PARKING_closed = (PARKING_NS + "closed"); // todo put this in the ontology
    public static final String PARKING_carCountingAvailability = (PARKING_NS + "carCountingAvailability"); // todo put this in the ontology
    public static final String PARKING_closedTimestamp = (PARKING_NS + "closedTimestamp"); // todo put this in the ontology
    public static final String PARKING_carCountingAvailabilityTimestamp = (PARKING_NS + "carCountingAvailabilityTimestamp"); // todo put this in the ontology

    public static final String PARKING_DATA_NS = "http://parking.kmi.open.ac.uk/data/parks/";

    public static final String PRESENTATION_ONTOLOGY = "http://parking.kmi.open.ac.uk/ontologies/presentation";
    public static final String PRESENTATION_NS = PRESENTATION_ONTOLOGY + "#";

    public static final String PRESENTATION_ORDER = PRESENTATION_NS + "order";
    public static final String PRESENTATION_LABEL = PRESENTATION_NS + "label";
    public static final String PRESENTATION_COMMENT = PRESENTATION_NS + "comment";
    public static final String PRESENTATION_PREFERRED_PROPERTY = PRESENTATION_NS + "PreferredProperty";
}
