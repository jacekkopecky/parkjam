/******************************************************************
 * File:        UniqueExtendedIterator.java
 * Created by:  Dave Reynolds
 * Created on:  28-Jan-2003
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: UniqueExtendedIterator.java,v 1.2 2009/08/08 11:25:31 andy_seaborne Exp $
 *****************************************************************/
package com.hp.hpl.jena.util.iterator;

import java.util.*;

/**
 * A variant on the closable/extended iterator that filters out
 * duplicate values. There is one complication that the value
 * which filtering is done on might not be the actual value
 * to be returned by the iterator. 
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.2 $ on $Date: 2009/08/08 11:25:31 $
 */
public class UniqueExtendedIterator<T> extends WrappedIterator<T> {

    /** The set of objects already seen */
    protected HashSet<T> seen = new HashSet<T>();
    
    /** One level lookahead */
    protected T next = null;
    
    /**
     * Constructor. Note the use of {@link #create} as reliable means of
     * creating a unique iterator without double-wrapping iterators that 
     * are already unique iterators.
     */
    public UniqueExtendedIterator(Iterator<T> underlying) {
        super(underlying, true);
    }
    
    /**
     * Factory method for generating an iterator that is guaranteed
     * only to return one instance of every result from the wrapped
     * iterator <code>it</code>.
     * @param it An iterator to wrap
     * @return A iterator that returns the elements of the wrapped
     * iterator exactly once.  If <code>it</code> is already a unique
     * extended iteator, it is not further wrapped.
     */
    public static <T> ExtendedIterator<T> create( Iterator<T> it ) {
        return (it instanceof UniqueExtendedIterator<?>) ? 
                    ((UniqueExtendedIterator<T>) it) : new UniqueExtendedIterator<T>( it );
    }
    
    /**
     * Fetch the next object to be returned, only if not already seen.
     * Subclasses which need to filter on different objects than the
     * return values should override this method.
     * @return the object to be returned or null if the object has been filtered.
     */
    protected T nextIfNew() {
        T value = super.next();
        return seen.add( value ) ? value : null;
    }
    
    /**
     * @see Iterator#hasNext()
     */
    @Override public boolean hasNext() {
        while (next == null && super.hasNext()) next = nextIfNew();
        return next != null;
    }

    /**
     * @see Iterator#next()
     */
    @Override public T next() {
        ensureHasNext();
        T result = next;
        next = null;
        return result;
    }
}

/*
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
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
 *
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

