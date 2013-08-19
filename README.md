ParkJam mobile app
=======

"The best way to find parking is to know beforehand where the spaces are."

Web site: http://parking.kmi.open.ac.uk 

Google Play: https://play.google.com/store/apps/details?id=uk.ac.open.kmi.parking


Copyright: 
 - 2012-2013 Jacek Kopecky (jacek@jacek.cz) 
 - 2012-2013 Knowledge Media Institute, The Open University, Milton Keynes, UK
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


ParkJam is built with Androjena for handling RDF data, see copyright
information below.

Androjena here was the 0.5 version (2010-12-04), tweaked to remove (hack out)
ARP, Jena IRI, ICU4J, and command-line tools, as none of that is useful to ParkJam.
Also rewrote UID so there's no GPL code around; with weaker uniqueness
guarantees.



-----------------------------------------------------------------------------------------------------------
Setting up in Eclipse:

These instructions should get you from cloning the ParkJam source code to
running your own ParkJam in your device.

Prerequisites:

- you have cloned the sources from github
- you have set up Eclipse to compile Android projects (API version 10, with Google APIs)
- you have appropriate development keys
    - you need an existing Google Maps API v1 key (new ones cannot be obtained 
      as of March 2013, but ParkJam will migrate to maps API v2 in due course)
    - possibly, you may use my debug key if you ask me for it
    - on the above point - you may need to change the top-level package for
      ParkJam if you want to test it with your API key
    - without a valid key, the map won't display map data, but it will
      display car parks and your location
- you have set up your device(s) and/or emulator(s) to run 
    - you need at least Android 2.3 with Google APIs
    - you have the correct settings for running apps you build yourself

Setting up:

1) File/Import "Existing Android Code Into Workspace"
 - select the directory/folder where you cloned the sources
 - Eclipse should show you uk.ac.open.kmi.parking.MainActivity 
 - confirm to Finish the import

2) in Project Properties / Android select build target Google APIs 2.3.3
 - if that's not already there

3) now you should be able to run this as an android application


If any steps don't work as described, or if anything is omitted, please do
let us know and we may update these instructions.




-----------------------------------------------------------------------------------------------------------

The following information is about Androjena:

Androjena is an Android port of Hewlett-Packard's Jena Semantic Framework (see http://jena.sourceforge.net).

Androjena is licensed under the Apache License, Version 2.0 (the "License");
A copy of the License is shipped with this distribution in licenses/apache_software_license_2.0.txt

Androjena is based on Hewlett-Packard's Jena library version 2.6.2
A copy of Jena's copyright notice and full license are shipped with this distribution in 
licenses/jena_2.6.2_license.txt

Androjena ships with or contains code from several libraries. A list follows with library names and 
location of respective licenses/copyright notices copies inside this distribution:

Jena IRI 0.8: licenses/jena_2.6.2_license.txt
Apache Xerces: licenses/apache_software_license_2.0.txt
Quality Open Software's SLF4J-android 1.6.1: licenses/slf4j_android_1.6.1_license.txt

Copyright 2010 Lorenzo Carrara (lorecarra@gmail.com)
