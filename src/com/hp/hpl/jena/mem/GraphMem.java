/*
  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: GraphMem.java,v 1.1 2009/06/29 08:55:55 castagna Exp $
*/

package com.hp.hpl.jena.mem;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.graph.impl.TripleStore;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

/**
    A memory-backed graph with S/P/O indexes. 
    @author  bwm, kers
*/
public class GraphMem extends GraphMemBase implements Graph 
    {    
    public GraphTripleStore forTestingOnly_getStore() 
        { return (GraphTripleStore) store; }
    
    /**
        Initialises a GraphMem with the Minimal reification style. Use the
        factory if possible; this method is public to allow certain reflective
        tests.
    */
    public GraphMem() 
        { this( ReificationStyle.Minimal ); }
    
    /**
        Initialises a GraphMem with the given reification style. Use the
        factory if possible; this method is public to allow certain reflective
        tests.
    */
    public GraphMem( ReificationStyle style )
        { super( style ); }
    
    @Override protected TripleStore createTripleStore()
        { return new GraphTripleStore( this ); }

    @Override protected void destroy()
        { store.close(); }

    @Override public void performAdd( Triple t )
        { if (!getReifier().handledAdd( t )) store.add( t ); }

    @Override public void performDelete( Triple t )
        { if (!getReifier().handledRemove( t )) store.delete( t ); }

    @Override public int graphBaseSize()  
        { return store.size(); }
    
    @Override public QueryHandler queryHandler()
        {
        if (queryHandler == null) queryHandler = new GraphMemBaseQueryHandler( this );
        return queryHandler;
        }
        
    /**
         Answer an ExtendedIterator over all the triples in this graph that match the
         triple-pattern <code>m</code>. Delegated to the store.
     */
    @Override public ExtendedIterator<Triple> graphBaseFind( TripleMatch m ) 
        { return store.find( m.asTriple() ); }
    
    /**
         Answer true iff this graph contains <code>t</code>. If <code>t</code>
         happens to be concrete, then we hand responsibility over to the store.
         Otherwise we use the default implementation.
    */
    @Override public boolean graphBaseContains( Triple t )
        { return isSafeForEquality( t ) ? store.contains( t ) : super.graphBaseContains( t ); }
    
    /**
        Clear this GraphMem, ie remove all its triples (delegated to the store).
    */
    @Override public void clear()
        { 
        store.clear(); 
        ((SimpleReifier) getReifier()).clear();
        }
    }

/*
	 *  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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