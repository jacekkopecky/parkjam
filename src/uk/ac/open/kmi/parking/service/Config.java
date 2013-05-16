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

import java.util.TimeZone;

class Config {

    static final int EXTRA_SLEEP_TIME = 200;
    static final int DEFAULT_AVAIL_TTL = 5000;
    static final int DEFAULT_DETAILS_TTL = 30000;
    static final int DEFAULT_TILE_TTL = 60000;

    static final int DEFAULT_NETWORK_PROBLEM_DELAY = 60000;
    static final int DEFAULT_SERVER_PROBLEM_DELAY = 600000;
    
    static final String SERVER = "http://parking.kmi.open.ac.uk/data/";
//    static final String SERVER = "http://10.100.22.96:8080/data/"; 

    static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
}
