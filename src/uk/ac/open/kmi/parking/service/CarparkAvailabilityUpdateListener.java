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

import uk.ac.open.kmi.parking.Parking;

/**
 * listener for updates of car park availability
 * not guaranteed to be called on the GUI thread
 * @author Jacek Kopecky
 */
public interface CarparkAvailabilityUpdateListener {
    /**
     * the ParkingsService has new availability status for this Parking
     * not guaranteed to be called on the GUI thread
     * @param parking the parking whose availability has been updated
     */
    public void onCarparkAvailabilityUpdated(Parking parking);
}
