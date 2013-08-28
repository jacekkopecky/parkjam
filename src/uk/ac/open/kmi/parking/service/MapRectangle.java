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

import com.google.android.gms.maps.model.LatLng;

class MapRectangle {
    public int latminE6, lonminE6, latmaxE6, lonmaxE6;
    public MapRectangle(int lami, int lomi, int lama, int loma) {
        this.latminE6 = lami;
        this.lonminE6 = lomi;
        this.latmaxE6 = lama;
        this.lonmaxE6 = loma;
    }

    public MapRectangle(MapRectangle r) {
        this.latminE6 = r.latminE6;
        this.lonminE6 = r.lonminE6;
        this.latmaxE6 = r.latmaxE6;
        this.lonmaxE6 = r.lonmaxE6;
    }

    public LatLng getCenter() {
        return new LatLng((this.latmaxE6+this.latminE6)/2e6d, (this.lonmaxE6+this.lonminE6)/2e6d);
    }

    @Override
    public String toString() {
        return "" + this.latminE6 + " " + this.lonminE6 + " " + this.latmaxE6 + " " + this.lonmaxE6;
    }
}
