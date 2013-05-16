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

/**
 * listener for when there's a change in the sorted list of items near the user's current UI view location
 * not guaranteed to be called on the GUI thread
 * @author Jacek Kopecky
 *
 */
public interface SortedCurrentItemsUpdateListener {

    /**
     * called on a change in the sorted list of items near the user's current UI view location
     */
    public void onSortedCurrentItemsUpdated();

}
