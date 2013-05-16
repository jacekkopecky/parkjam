/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: FragmentTripleIterator.java,v 1.1 2009/06/29 08:55:43 castagna Exp $
*/

package com.hp.hpl.jena.graph.impl;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.*;

import java.util.*;

/**
     Iterator which delivers all the triples from a Fragment's map. Subclasses
     must override <code>fill(GraphAdd,Node,T)</code> to fill the GraphAdd
     object with the triples implied by the unrefied <code>T</code> object.
     In the codebase, T is instantiated as either a Triple or a Fragments.     
*/
public abstract class FragmentTripleIterator<T> extends NiceIterator<Triple>
    {
    private final GraphAddList pending;
    private final Iterator<Map.Entry<Node, T>> it;
    
    /**
        An iterator over all the reification triples buried in <code>it</code> that match
        <code>match</code>. The elements of the iterator are either Triples
        or Fragmentss.
    */
    public FragmentTripleIterator( Triple match, Iterator<Map.Entry<Node, T>> it )
        {
        super();
        this.it = it;
        this.pending = new GraphAddList( match );
        }

            
    /**
        Answer true iff there are any triples left, ie, there are some triples in the pending
        list once we've refilled.
        @return true iff there are more triples to come
    */
    @Override public boolean hasNext() 
        { 
        refill();
        return pending.size() > 0; 
        }
        
    /**
        Answer the next triple in the iteration, if one exists. The triples are stored in the
        pending list and removed from its end (which we hope is efficient).
        @return the next triple
        @throws NoSuchElementException if there isn't one
    */
    @Override public Triple next()
        {
        ensureHasNext();
        return pending.removeLast();
        }
        
    /**
        Add all the [implied, matching] triples from the Object into the GraphAdd
        entity (which will be our list). It would be nice if we could create an interface
        for the fragmentObject's type.
    */
    protected abstract void fill( GraphAdd ga, Node n, T fragmentsObject );
        
    /**
        Refill the pending list. Keep trying until either there are some elements
        to be found in it, or we've run out of elements in the original iterator.
    */
    private void refill()
        { while (pending.size() == 0 && it.hasNext()) refillFrom( pending, it.next() );  }

    /**
         Refill the buffer <code>pending</code> from the iterator element 
         <code>next</code>. The default behaviour is to assume that this object is 
         a Map.Entry; over-ride if it's something else.
    */
    protected void refillFrom( GraphAdd pending, Map.Entry<Node, T> x )
        { fill( pending, x.getKey(), x.getValue() ); }
}

/*
    (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/