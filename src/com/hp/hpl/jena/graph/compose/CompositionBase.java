/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            4 Mar 2003
 * Filename           $RCSfile: CompositionBase.java,v $
 * Revision           $Revision: 1.2 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2009/10/06 13:04:43 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (see footer for full conditions)
 *****************************************************************************/

// Package
///////////////
package com.hp.hpl.jena.graph.compose;


// Imports
///////////////
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.util.IteratorCollection;
import com.hp.hpl.jena.util.iterator.*;

import java.util.*;


/**
 * <p>
 * Base class for graphs that are composed of multiple sub-graphs.  This is to provide
 * a home for shared functionality that was previously in {@link Dyadic} before
 * refactoring.
 * </p>
 *
 * @author Ian Dickinson, moved kers' code from Dyadic to this class, added commentage
 * @author Chris Dollin (kers)
 * @version CVS $Id: CompositionBase.java,v 1.2 2009/10/06 13:04:43 ian_dickinson Exp $
 */
public abstract class CompositionBase extends GraphBase
{
    /**
     * <p>
     * Answer a {@link Filter} that will reject any element that is a member of iterator i.
     * As a side-effect, i will be closed. 
     * </p>
     * 
     * @param i A closable iterator
     * @return A Filter that will accept any object not a member of i.
     */
    public static <T> Filter<T> reject( final ClosableIterator<? extends T> i )
        {
        final Set< ? extends T> suppress = IteratorCollection.iteratorToSet( i );
        return new Filter<T>()
            { @Override public boolean accept( T o ) { return !suppress.contains( o ); } };
        }
        
    /**
     * <p>
     * Answer an iterator over the elements of iterator a that are not members of iterator b.
     * As a side-effect, iterator b will be closed.
     * </p>
     * 
     * @param a An iterator that will be filtered by rejecting the elements of b
     * @param b A closable iterator 
     * @return The iteration of elements in a but not in b.
     */
    public static <T> ClosableIterator<T> butNot( final ClosableIterator<T> a, final ClosableIterator<? extends T> b )
        {
        return new FilterIterator<T>( reject( b ), a );
        }
        
    /**
     * <p>
     * Answer an iterator that will record every element delived by <code>next()</code> in
     * the set <code>seen</code>. 
     * </p>
     * 
     * @param i A closable iterator
     * @param seen A set that will record each element of i in turn
     * @return An iterator that records the elements of i.
     */
    public static <T> ExtendedIterator<T> recording( final ClosableIterator<T> i, final Set<T> seen )
        {
        return new NiceIterator<T>()
            {
            @Override
            public void remove()
                { i.remove(); }
            
            @Override
            public boolean hasNext()
                { return i.hasNext(); }    
            
            @Override
            public T next()
                { T x = i.next(); 
                try { seen.add( x ); } catch (OutOfMemoryError e) { throw e; } return x; }  
                
            @Override
            public void close()
                { i.close(); }
            };
        }
        
    //static final Object absent = new Object();
    
    /**
     * <p>
     * Answer an iterator over the elements of iterator i that are not in the set <code>seen</code>. 
     * </p>
     * 
     * @param i An extended iterator
     * @param seen A set of objects
     * @return An iterator over the elements of i that are not in the set <code>seen</code>.
     */
    public static ExtendedIterator<Triple> rejecting( final ExtendedIterator<Triple> i, final Set<Triple> seen )
        {
        Filter<Triple> seenFilter = new Filter<Triple>()
            { @Override
            public boolean accept( Triple x ) { return seen.contains( x ); } };
        return i.filterDrop( seenFilter );
        }
        
    /**
         Answer an iterator over the elements of <code>i</code> that are not in
         the graph <code>seen</code>.
    */
    public static ExtendedIterator<Triple> rejecting( final ExtendedIterator<Triple> i, final Graph seen )
        {
        Filter<Triple> seenFilter = new Filter<Triple>()
            { @Override public boolean accept( Triple x ) { return seen.contains( x ); } };
        return i.filterDrop( seenFilter );
        }
  
    /**
     * <p>
     * Answer a {@link Filter} that will accept any object that is an element of 
     * iterator i.  As a side-effect, i will be evaluated and closed. 
     * </p>
     * 
     * @param i A closable iterator 
     * @return A Filter that will accept any object in iterator i.
     */
    public static <T> Filter<T> ifIn( final ClosableIterator<T> i )
        {
        final Set<T> allow = IteratorCollection.iteratorToSet( i );
        return new Filter<T>()
            { @Override public boolean accept( T x ) { return allow.contains( x ); } };
        }
        
    /**
     * <p>
     * Answer a {@link Filter} that will accept any triple that is an edge of 
     * graph g. 
     * </p>
     * 
     * @param g A graph 
     * @return A Filter that will accept any triple that is an edge in g.
     */
    public static Filter<Triple> ifIn( final Graph g )
        {
        return new Filter<Triple>()
            { @Override public boolean accept( Triple x ) { return g.contains( x ); } };
        }
        

    // Internal implementation methods
    //////////////////////////////////


    //==============================================================================
    // Inner class definitions
    //==============================================================================


}


/*
    (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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

