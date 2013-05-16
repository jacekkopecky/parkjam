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

import android.view.animation.Interpolator;

/**
 * an interpolator that's smooth on both edges and starts backwards first
 * @author Jacek Kopecky
 *
 */
public class SmoothAnticipateInterpolator implements Interpolator {
    
    private final double t;
    private final double m;
    
    /**
     * constructor
     * @param t how much of the original distance should it go back
     */
    public SmoothAnticipateInterpolator(double t) {
        this.t = t;
        this.m = Math.sqrt(t*t+t)-t;
    }
    
    /**
     * constructor with overshoot of 0.3
     */
    public SmoothAnticipateInterpolator() {
        this(.3);
    }

    @SuppressWarnings("unqualified-field-access")
    public float getInterpolation(float x) {
        if (x < m) {
            return (float) (t/2*(Math.cos(x/m*Math.PI)-1));
        } else {
            return (float) (1.0-(t+1)/2*(1-Math.cos((1-x)/(1-m)*Math.PI)));
        }
    }

}
