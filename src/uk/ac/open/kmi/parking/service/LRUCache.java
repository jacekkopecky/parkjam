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

import java.util.LinkedHashMap;

/**
 * this class implements an Least Recently Used cache of objects of type T
 * currently uses fixed size, todo see if needs to be more dynamic, maybe connected to java garbage collection
 * this queue is NOT synchronized in any way
 *
 * @param <T> type of object in the queue
 */
class LRUCache <T> {
    @SuppressWarnings("unused")
    private static final String TAG = "LRUCache";
    private final int size;
    private LinkedHashMap<T,T> objects;

    public LRUCache(int capacity) {
        this.size = capacity;
        this.objects = new LinkedHashMap<T,T>(this.size+1, .5f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Entry<T, T> eldest) {
                return size() > LRUCache.this.size;
            }
        };
    }

    /**
     * adds an object at the end of the queue
     * @param object the object to be added
     */
    public void add(T object) {
        this.objects.put(object, object);
//        printOutStats();
    }

    /**
     * checks whether an objects is in the cache through a template (which must evaluate as equal to the requested object), and makes the object the freshest one in the cache
     * @param template an object that must evaluate as equal to the desired object
     * @return the actual object that was in the cache, or null if it wasn't
     */
    public T get(T template) {
        return this.objects.get(template);
    }

    public boolean peek(T template) {
        return this.objects.containsKey(template);
    }

}
