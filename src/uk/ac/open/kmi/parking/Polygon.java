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

import com.google.android.gms.maps.model.LatLng;

/**
 * this class implements a geopoint defined as the average of a bunch of points - later it could be upgraded to be an actual polygon that a parking could draw
 * todo treat this as an actual polygon
 * @author Jacek Kopecky
 *
 */
public class Polygon {
    /**
     * creates a polygon from a list of coordinates - the list must consist of pairs of latitude/longitude (*1e6) pairs, for example new Polygon(1,2,3,4,5,6) is a triangle between the points 1,2; 3,4 and 5,6.
     * @param latlone6 a list of pairs of latitude and longitude in microdegrees
     */
//    public Polygon(int... latlone6) {
//        super(avg(0,latlone6), avg(1,latlone6));
//    }

    public LatLng avg(int... latlone6) {
        return new LatLng(avg(0,latlone6), avg(1,latlone6));
    }

    private static int avg(int offset, int[] latlone6) {
        if ((latlone6.length | 1) == 1) {
            throw new IllegalArgumentException("Polygon constructor must get an even number of agruments, each two subsequent params forming a latitude/longitude pair");
        }
        long sum = 0;
        for (int i=offset; i< latlone6.length; i+=2) {
            sum += latlone6[i];
        }
        return (int)(sum*2/latlone6.length); // *2 because the length is twice the number of points
    }
}
