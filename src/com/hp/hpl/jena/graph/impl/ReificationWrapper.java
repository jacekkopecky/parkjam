/*
 	(c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved.
 	$Id: ReificationWrapper.java,v 1.1 2009/06/29 08:55:43 castagna Exp $
*/

package com.hp.hpl.jena.graph.impl;

import java.util.List;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;

public class ReificationWrapper implements Reifier
    {
    protected final ReificationStyle style;
    protected final ReificationWrapperGraph graph;
    protected final Graph base;
    
    public ReificationWrapper( Graph graph, ReificationStyle style )
        { 
        this.style = style; 
        this.graph = (ReificationWrapperGraph) graph; 
        this.base = this.graph.getBase(); 
        }
    
    public ExtendedIterator<Node> allNodes()
        { // TODO needs constraining for :subject :object etc
        return base.find( Node.ANY, RDF.Nodes.type, RDF.Nodes.Statement ).mapWith( Triple.getSubject );
        }
    
    public ExtendedIterator<Node> allNodes( Triple t )
        { throw new BrokenException( "this reifier operation" ); }
    
    public void close()
        { /* nothing to do */ }
    
    public ExtendedIterator<Triple>find( TripleMatch m )
        { return base.find( m ).filterKeep( isReificationTriple ); }
    
    protected static final Filter<Triple> isReificationTriple = new Filter<Triple>()
        {
        @Override public boolean accept( Triple o )
            { return isReificationTriple( o ); }  
        };
    
    public ExtendedIterator<Triple> findEither( TripleMatch m, boolean showHidden )
        { return showHidden == style.conceals() ? find( m ) : Triple.None; }
    
    public ExtendedIterator<Triple> findExposed( TripleMatch m )
        { return find( m ); }
    
    public Graph getParentGraph()
        { return graph; }
    
    public ReificationStyle getStyle()
        { return style; }
    
    public boolean handledAdd( Triple t )
        {
        base.add( t );
        return isReificationTriple( t );
        }
    
    public boolean handledRemove( Triple t )
        { throw new BrokenException( "this reifier operation" ); }
    
    public boolean hasTriple( Node n )
        { return getTriple( n ) != null; }
    
    public Node reifyAs( Node n, Triple t )
        {
        Triple already = getTriple( n );
        if (already == null)
            {
            checkQuadElementFree( n, RDF.Nodes.subject, t.getSubject() );
            checkQuadElementFree( n, RDF.Nodes.predicate, t.getPredicate() );
            checkQuadElementFree( n, RDF.Nodes.object, t.getObject() );
            SimpleReifier.graphAddQuad( graph, n, t );
            }
        else if (!t.equals( already ))
            throw new AlreadyReifiedException( n );
        return n;
        }
    
    private void checkQuadElementFree( Node n, Node predicate, Node object )
        {
        List<Node> L = base.find( n, predicate, Node.ANY ).mapWith( Triple.getObject ).toList();
        if (L.size() == 0) return;
        if (L.size() == 1 && L.get( 0 ).equals( object )) return;
        throw new CannotReifyException( n );
        }
    
    public void remove( Node n, Triple t )
        { // TODO fix to ensure only works on complete reifications
        base.delete(  Triple.create( n, RDF.Nodes.subject, t.getSubject() ) );
        base.delete(  Triple.create( n, RDF.Nodes.predicate, t.getPredicate() ) );
        base.delete(  Triple.create( n, RDF.Nodes.object, t.getObject() ) );
        base.delete(  Triple.create( n, RDF.Nodes.type, RDF.Nodes.Statement ) );
        }
    
    public void remove( Triple t )
        { throw new BrokenException( "this reifier operation" ); }
    
    public int size()
        { return style.conceals() ? 0: countQuadlets(); }
    
    int count( ExtendedIterator<?> find )
        { 
        int result = 0;
        while (find.hasNext()) { result += 1; find.next(); }
        return result;
        }
    
    int countConcealed()
        { return style.conceals() ? countQuadlets() : 0; }

    private int countQuadlets()
        {
        return 
            count(  base.find( Node.ANY, RDF.Nodes.subject, Node.ANY ) )
            + count( base.find( Node.ANY, RDF.Nodes.predicate, Node.ANY ) )
            + count( base.find( Node.ANY, RDF.Nodes.object, Node.ANY ) )
            + count( base.find( Node.ANY, RDF.Nodes.type, RDF.Nodes.Statement ) )
            ;
        }
    
    public boolean hasTriple( Triple t )
        { // CHECK: there's one match AND it matches the triple t.
        Node X = Query.X,  S = Query.S, P = Query.P, O = Query.O;
        Query q = quadsQuery( Query.X );
        List<Domain> bindings = base.queryHandler().prepareBindings( q, new Node[] {X, S, P, O} ).executeBindings().toList();
        return bindings.size() == 1 && t.equals( tripleFromRSPO( bindings.get( 0 ) ) );
        }
    
    private Triple tripleFromRSPO( Domain domain )
        { 
        return Triple.create
            ( domain.getElement(1), domain.getElement(2), domain.getElement(3) );
        }
    
    public Triple getTriple( Node n )
        {
        Node S = Query.S, P = Query.P, O = Query.O;
        Query q = quadsQuery( n );
        List<Domain> bindings = base.queryHandler().prepareBindings( q, new Node[] {S, P, O} ).executeBindings().toList();
        return bindings.size() == 1 ? tripleFromSPO( bindings.get(0) ) : null;
        }
    
    private static Query quadsQuery( Node subject )
        {
        return new Query()
            .addMatch( subject, RDF.Nodes.subject, Query.S )
            .addMatch( subject, RDF.Nodes.predicate, Query.P )
            .addMatch( subject, RDF.Nodes.object, Query.O )
            .addMatch( subject, RDF.Nodes.type, RDF.Nodes.Statement )
            ;
        }
    
    private Triple tripleFromSPO( Domain d )
        { return Triple.create( d.getElement(0), d.getElement(1), d.getElement(2) ); }
    
    private static boolean isReificationTriple( Triple t )
        {
        return 
            Reifier.Util.isReificationPredicate( t.getPredicate() ) 
            || Reifier.Util.isReificationType( t.getPredicate(), t.getObject() )
            ;
        }
    }

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
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
