/* UID not taken from GNU Classpath any more - rewritten to avoid having to license ParkJam under GPL; this file is licensed without restriction for any purpose whatsoever */


package it.polimi.dei.dbgroup.pedigree.androjena.java.rmi.server;

/**
 * simple UID, based on time and a long count assumed never to overflow.
 * this is a rewrite of the GNU Classpath UID class that Androjena used originally.
 * this UID is only guaranteed to be unique within a single JVM and a single classloader - that should be enough for ParkJam's uses.
 *
 * @author Jacek Kopecky (jacek@jacek.cz)
 */
public final class UID
{
    private static long lastTime = 0;
    private static long lastCount = 0;

    private long time, count;

     /**
     * creates a new unique ID with the guarantees described in the class's javadoc
     */
    public UID() {
         synchronized(UID.class) {
             this.time = System.currentTimeMillis();
             if (this.time > lastTime) {
                 lastTime = this.time;
                 this.count = 0;
             } else {
                 this.time = lastTime;
                 this.count = lastCount + 1;
             }
             lastCount = this.count;
         }
     }

     @Override
    public String toString() {
         return Long.toString(this.time) + ":" + Long.toString(this.count);
     }
}
