/*
  (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: StoreTripleIterator.java,v 1.1 2009/06/29 08:55:55 castagna Exp $
*/
package com.hp.hpl.jena.mem;

import java.util.Iterator;

import com.hp.hpl.jena.graph.*;

/**
     An iterator wrapper for NodeToTriplesMap iterators which ensures that
     a .remove on the base iterator is copied to the other two maps of this
     GraphMem. The current triple (the most recent result of .next) is
     tracked by the parent <code>TrackingTripleIterator</code> so that it
     can be removed from the other two maps, which are passed in when this 
     StoreTripleIterator is created.
     
    @author kers
*/
public class StoreTripleIterator extends TrackingTripleIterator
	{
    protected NodeToTriplesMapBase X;
    protected NodeToTriplesMapBase A;
    protected NodeToTriplesMapBase B;
    protected Graph toNotify;
    
    public StoreTripleIterator
        ( Graph toNotify, Iterator<Triple> it, 
          NodeToTriplesMapBase X, 
          NodeToTriplesMapBase A, 
          NodeToTriplesMapBase B )
    	{ 
        super( it ); 
        this.X = X;
        this.A = A; 
        this.B = B; 
        this.toNotify = toNotify;
        }

    @Override public void remove()
        {
        super.remove();
        X.removedOneViaIterator();
        A.remove( current );
        B.remove( current );
        toNotify.getEventManager().notifyDeleteTriple( toNotify, current );
        }
	}

/*
    (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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