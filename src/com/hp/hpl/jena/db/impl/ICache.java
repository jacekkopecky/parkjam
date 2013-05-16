/*
 *  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 *
 */


//=======================================================================
// Package
package com.hp.hpl.jena.db.impl;

//=======================================================================
/**
* Interface signature for cache implementations. Instances of this are
* used to cache resources and literals loaded from a database.
* <p>The type of the stored objects is left generic in case other caches
* are needed but could specialize it to RDFNodes.
*
* @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
* @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:37 $
*/


public interface ICache<K, V> {

    /**
     * Add an entry to the cache
     * @param id the database ID to be used as an index
     * @param val the literal or resources to be stored
     */
    public void put(K id, V val);

    /**
     * Retreive an object from the cache
     * @param id the database ID of the object to be retrieved
     * @return the object or null if it is not in the cache
     */
    public V get(K id);

    /**
     * Set a threshold for the cache size in terms of the count of cache entries.
     * For literals a storage limit rather than a count might be more useful but
     * counts are easier, more general and sufficient for the current use.
     *
     * @param threshold the cache size limit, use 0 for no cache, -1 for
     * unlimited cache growth; any other number indicates the number of cache entries
     */
    public void setLimit(int threshold);

    /**
     * Return the current threshold limit for the cache size.
     */
    public int getLimit();
}

/*
 *  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
