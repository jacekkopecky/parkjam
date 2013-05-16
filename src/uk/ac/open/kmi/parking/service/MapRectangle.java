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

class MapRectangle {
    public int latmin, lonmin, latmax, lonmax;
    public MapRectangle(int lami, int lomi, int lama, int loma) {
        this.latmin = lami;
        this.lonmin = lomi;
        this.latmax = lama;
        this.lonmax = loma;
    }
    
    public MapRectangle(MapRectangle r) {
        this.latmin = r.latmin;
        this.lonmin = r.lonmin;
        this.latmax = r.latmax;
        this.lonmax = r.lonmax;
    }

    @Override
    public String toString() {
        return "" + this.latmin + " " + this.lonmin + " " + this.latmax + " " + this.lonmax;
    }
}
