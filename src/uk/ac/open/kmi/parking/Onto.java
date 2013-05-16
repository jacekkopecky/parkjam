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

import uk.ac.open.kmi.parking.ontology.Ontology;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * provides Androjena shortcuts for Ontology constants
 * @author Jacek Kopecky
 */
@SuppressWarnings("javadoc")
public class Onto {

    public static final Property GEOPOS_lat = p(Ontology.GEOPOS_lat);
    public static final Property GEOPOS_long = p(Ontology.GEOPOS_long);
    public static final Resource LGO_Parking = r(Ontology.LGO_Parking);
    public static final Resource PARKING_AvailabilityEstimate = r(Ontology.PARKING_AvailabilityEstimate);
    public static final Resource PARKING_AvailabilityObservation = r(Ontology.PARKING_AvailabilityObservation);
    public static final Resource PARKING_AvailabilitySubmission = r(Ontology.PARKING_AvailabilitySubmission);
    public static final Resource PARKING_UnverifiedInstance = r(Ontology.PARKING_UnverifiedInstance);
    public static final Property PARKING_availabilityResource = p(Ontology.PARKING_availabilityResource);
    public static final Property PARKING_updateResource = p(Ontology.PARKING_updateResource);
    public static final Property PARKING_binaryAvailability = p(Ontology.PARKING_binaryAvailability);
    public static final Property PARKING_binaryAvailabilityTimestamp = p(Ontology.PARKING_binaryAvailabilityTimestamp);
    public static final Property PARKING_hasUnverifiedProperties = p(Ontology.PARKING_hasUnverifiedProperties);
    public static final Property PARKING_geocodedTitle = p(Ontology.PARKING_geocodedTitle);
    public static final Property SSN_featureOfInterest = p(Ontology.SSN_featureOfInterest);
    public static final Property SSN_hasValue = p(Ontology.SSN_hasValue);
    public static final Property SSN_observationResult = p(Ontology.SSN_observationResult);
    public static final Property SSN_observationSamplingTime = p(Ontology.SSN_observationSamplingTime);
    public static final Property PRES_order = p(Ontology.PRESENTATION_ORDER);
    public static final Property PRES_label = p(Ontology.PRESENTATION_LABEL);
    public static final Property PRES_comment = p(Ontology.PRESENTATION_COMMENT);
    public static final Property PRES_PreferredProperty = p(Ontology.PRESENTATION_PREFERRED_PROPERTY);

    private static final Resource r(String uri) {
        return ResourceFactory.createResource(uri);
    }

    private static final Property p(String uri) {
        return ResourceFactory.createProperty(uri);
    }
}
