/*
  (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: SimpleReifierFragmentsMap.java,v 1.1 2009/06/29 08:55:43 castagna Exp $
*/
package com.hp.hpl.jena.graph.impl;

import java.util.*;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;

/**
    SimpleReifierFragmentsMap - a map from nodes to the incompleteb(or 
    overcomplete) reification quadlets.
    
    @author kers
*/
public class SimpleReifierFragmentsMap implements ReifierFragmentsMap 
    {
    protected Map<Node, Fragments> forwardMap = CollectionFactory.createHashedMap();
    
    protected Fragments getFragments( Node tag )
        { return forwardMap.get( tag ); }
    
    protected void removeFragments( Node key )
        { forwardMap.remove( key ); }
    
    public void clear()
        { forwardMap.clear(); }
    
    /**
    update the map with (node -> fragment); return the fragment.
    */
    protected Fragments putFragments( Node key, Fragments value )
        {
        forwardMap.put( key, value );
        return value;
        }                    
    
    protected ExtendedIterator<Triple> allTriples( TripleMatch tm )
        {
        if (forwardMap.isEmpty())
            return NullIterator.instance();
        Triple t = tm.asTriple();
        Node subject = t.getSubject();
        if (subject.isConcrete())
            {
            Fragments x = forwardMap.get( subject );  
            return x == null
                ? NullIterator.<Triple>instance()
                : explodeFragments( t, subject, x )
                ; 
            }
        else
            {
            final Iterator<Map.Entry<Node, Fragments>> it = forwardMap.entrySet().iterator();   
            return new FragmentTripleIterator<Fragments>( t, it )
                {
                @Override public void fill( GraphAdd ga, Node n, Fragments fragmentsObject )
                    { fragmentsObject.includeInto( ga ); }
                };
            }
        }
    
    /**
     * @param t
     * @param subject
     * @param x
     * @return
     */
    protected ExtendedIterator<Triple> explodeFragments( Triple t, Node subject, Fragments x )
        {
        GraphAddList L = new GraphAddList( t );
        x.includeInto( L );
        return WrappedIterator.create( L.iterator() );
        }

    public ExtendedIterator<Triple> find( TripleMatch m )
        { return allTriples( m ); }
    
    public int size()
        { 
        int result = 0;
        Iterator<Map.Entry<Node, Fragments>> it = forwardMap.entrySet().iterator();   
        while (it.hasNext())
            {
            Map.Entry<Node, Fragments> e = it.next();
            result += e.getValue().size();
            }
        return result; 
        }
    
    /**
        given a triple t, see if it's a reification triple and if so return the internal selector;
        otherwise return null.
    */ 
    public ReifierFragmentHandler getFragmentHandler( Triple t )
        {
        Node p = t.getPredicate();
        ReifierFragmentHandler x = selectors.get( p );
        if (x == null || (p.equals( RDF.Nodes.type ) && !t.getObject().equals( RDF.Nodes.Statement ) ) ) return null;
        return x;
        }

    public void putAugmentedTriple( SimpleReifierFragmentHandler s, Node tag, Node object, Triple reified )
        {
        Fragments partial = new Fragments( tag, reified );
        partial.add( s, object );
        putFragments( tag, partial );
        }
    
    protected Triple reifyCompleteQuad( SimpleReifierFragmentHandler s, Triple fragment, Node tag, Node object )
        {       
        Fragments partial = getFragments( tag );
        if (partial == null) putFragments( tag, partial = new Fragments( tag ) );
        partial.add( s, object );
        if (partial.isComplete())
            {
            removeFragments( fragment.getSubject() );
            return partial.asTriple();
            }
        else
            return null;
        }

    protected Triple removeFragment( SimpleReifierFragmentHandler s, Node tag, Triple already, Triple fragment )
        {
        Fragments partial = getFragments( tag );
        Fragments fs = (already != null ? explode( tag, already )
            : partial == null ? putFragments( tag, new Fragments( tag ) )
            : (Fragments) partial);
        fs.remove( s, fragment.getObject() );
        if (fs.isComplete())
            {
            Triple result = fs.asTriple();
            removeFragments( tag );
            return result;
            }
        else
            {
            if (fs.isEmpty()) removeFragments( tag );
            return null;
            }
        }
    
    protected Fragments explode( Node s, Triple t )
        { return putFragments( s, new Fragments( s, t ) ); }

    public boolean hasFragments( Node tag )
        { return getFragments( tag ) != null; }

    protected static final Fragments.GetSlot TYPES_index = new Fragments.GetSlot() 
        { public Set<Node> get( Fragments f ) { return f.types; } };
    
    protected static final Fragments.GetSlot SUBJECTS_index = 
        new Fragments.GetSlot() { public Set<Node> get( Fragments f ) { return f.subjects; } };
    
    protected static final Fragments.GetSlot OBJECTS_index = 
        new Fragments.GetSlot() { public Set<Node> get( Fragments f ) { return f.objects; } };
    
    protected static final Fragments.GetSlot PREDICATES_index = 
        new Fragments.GetSlot() { public Set<Node> get( Fragments f ) { return f.predicates; } };
    
    protected final ReifierFragmentHandler TYPES = new SimpleReifierFragmentHandler( this, TYPES_index) 
        { @Override public boolean clashesWith( ReifierFragmentsMap map, Node n, Triple reified ) { return false; } };
    
    protected final ReifierFragmentHandler SUBJECTS = new SimpleReifierFragmentHandler( this, SUBJECTS_index) 
        { @Override public boolean clashesWith( ReifierFragmentsMap map, Node n, Triple reified ) { return !n.equals( reified.getSubject() ); } };
    
    protected final ReifierFragmentHandler PREDICATES = new SimpleReifierFragmentHandler( this, PREDICATES_index) 
        { @Override public boolean clashesWith( ReifierFragmentsMap map, Node n, Triple reified ) { return !n.equals( reified.getPredicate() ); } };
    
    protected final ReifierFragmentHandler OBJECTS = new SimpleReifierFragmentHandler( this, OBJECTS_index) 
        { @Override public boolean clashesWith( ReifierFragmentsMap map, Node n, Triple reified ) { return !n.equals( reified.getObject() ); } };

    public final Map<Node, ReifierFragmentHandler> selectors = makeSelectors();
          
    /**
        make the selector mapping.
    */
    protected Map<Node, ReifierFragmentHandler> makeSelectors()
        {
        Map<Node, ReifierFragmentHandler> result = CollectionFactory.createHashedMap();
        result.put( RDF.Nodes.subject, SUBJECTS );
        result.put( RDF.Nodes.predicate, PREDICATES );
        result.put( RDF.Nodes.object, OBJECTS );
        result.put( RDF.Nodes.type, TYPES );
        return result;
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