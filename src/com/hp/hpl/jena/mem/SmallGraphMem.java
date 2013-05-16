/*
  (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: SmallGraphMem.java,v 1.1 2009/06/29 08:55:55 castagna Exp $
*/
package com.hp.hpl.jena.mem;

import java.util.Set;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
     A SmallGraphMem is a memory-based Graph suitable only for Small models
     (a few triples, perhaps a few tens of triples), because it does no indexing,
     but it stores onlya single flat set of triples and so is memory-cheap.
     
    @author kers
*/

public class SmallGraphMem extends GraphMemBase
    {
    protected Set<Triple> triples = CollectionFactory.createHashedSet();
    
    public SmallGraphMem()
        { this( ReificationStyle.Minimal ); }
    
    public SmallGraphMem( ReificationStyle style )
        { super( style ); }
    
    /**
        SmallGraphMem's don't use TripleStore's at present. 
    */
    @Override protected TripleStore createTripleStore()
        { return null; }
    
    @Override public void performAdd( Triple t )
        { if (!getReifier().handledAdd( t )) triples.add( t ); }
    
    @Override public void performDelete( Triple t )
        { if (!getReifier().handledRemove( t )) triples.remove( t ); }
    
    @Override public int graphBaseSize()  
        { return triples.size(); }

    /**
        Answer true iff t matches some triple in the graph. If t is concrete, we
        can use a simple membership test; otherwise we resort to the generic
        method using find.
    */
    @Override public boolean graphBaseContains( Triple t ) 
        { return isSafeForEquality( t ) ? triples.contains( t ) : containsByFind( t ); }

    @Override protected void destroy()
        { triples = null; }
    
    @Override public void clear()
        { 
        triples.clear(); 
        ((SimpleReifier) getReifier()).clear();
        }
    
    @Override public BulkUpdateHandler getBulkUpdateHandler()
        {
        if (bulkHandler == null) bulkHandler = new GraphMemBulkUpdateHandler( this );
        return bulkHandler;
        }
    
    @Override public ExtendedIterator <Triple>graphBaseFind( TripleMatch m ) 
        {
        return 
            SimpleEventManager.notifyingRemove( this, triples.iterator() ) 
            .filterKeep ( new TripleMatchFilter( m.asTriple() ) );
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