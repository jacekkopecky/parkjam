/*
  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: Query.java,v 1.1 2009/06/29 08:55:45 castagna Exp $
*/

package com.hp.hpl.jena.graph.query;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.shared.*;

import java.util.*;

/**
	The class of graph queries, plus some machinery (which should move) for
    implementing them.

	@author hedgehog
*/

public class Query 
	{   
    /**
        A convenient synonym for Node.ANY, used in a match to match anything.
    */ 
    public static final Node ANY = Node.ANY;
    
    /**
        A query variable called "S".
    */
    public static final Node S = Node.createVariable( "S" );
    /**
        A query variable called "P".
    */
    public static final Node P = Node.createVariable( "P" );
    /**
        A query variable called "O".
    */
    public static final Node O = Node.createVariable( "O" );
    /**
        A query variable called "X".
    */
    public static final Node X = Node.createVariable( "X" );
    /**
        A query variable called "Y".
    */
    public static final Node Y = Node.createVariable( "Y" );
    /**
        A query variable called "Z".
    */
    public static final Node Z = Node.createVariable( "Z" );
        
    /**
        Initialiser for Query; makes an empty Query [no matches, no constraints]
    */
	public Query()
		{ }
        
    /**
        Initialiser for Query; makes a Query with its matches taken from 
        <code>pattern</code>.
        @param pattern a Graph whose triples are used as match elements
    */
    public Query( Graph pattern )
        { addMatches( pattern ); }

    /**
        Exception thrown when a query variable is discovered to be unbound.
    */
    public static class UnboundVariableException extends JenaException
        { public UnboundVariableException( Node n ) { super( n.toString() ); } }
                        
    /**
        Add an (S, P, O) match to the query's collection of match triples. Return
        this query for cascading.
        @param s the node to match the subject
        @param p the node to match the predicate
        @param o the node to match the object
        @return this Query, for cascading
    */
    public Query addMatch( Node s, Node p, Node o )
        { return addNamedMatch( NamedTripleBunches.anon, s, p, o ); }    
    
    /**
        Add a triple to the query's collection of match triples. Return this query
        for cascading.
        @param t an (S, P, O) triple to add to the collection of matches
        @return this Query, for cascading
    */
    public Query addMatch( Triple t )
        { 
        triplePattern.add( t );
        triples.add( NamedTripleBunches.anon, t );
        return this; 
        }
    
    private Query addNamedMatch( String name, Node s, Node p, Node o )
        { 
        triplePattern.add( Triple.create( s, p, o ) );
        triples.add( name, Triple.create( s, p, o ) ); 
        return this; 
        }
    
    /** 
         The named bunches of triples for graph matching 
    */
    private NamedTripleBunches triples = new NamedTripleBunches();
    
    private List<Triple> triplePattern = new ArrayList<Triple>();
    
    /**
        Answer a list of the triples that have been added to this query.
        (Note: ignores "named triples").
        
     	@return List
    */
    public List<Triple> getPattern()
        { return new ArrayList<Triple>( triplePattern ); }
    
    private ExpressionSet constraint = new ExpressionSet();
    
    public ExpressionSet getConstraints()
        { return constraint; }
        
    public Query addConstraint( Expression e )
        { 
        if (e.isApply() && e.getFun().equals( ExpressionFunctionURIs.AND ))
           for (int i = 0; i < e.argCount(); i += 1) addConstraint( e.getArg( i ) ); 
        else if (e.isApply() && e.argCount() == 2 && e.getFun().equals( ExpressionFunctionURIs.Q_StringMatch))
            constraint.add( Rewrite.rewriteStringMatch( e ) );
        else
            constraint.add( e );
        return this;    
        }
    
    /**
        Add all the (S, P, O) triples of <code>p</code> to this Query as matches.
    */
    private void addMatches( Graph p )
        {
        ClosableIterator<Triple> it = GraphUtil.findAll( p );
        while (it.hasNext()) addMatch( it.next() );
        }

    public ExtendedIterator<Domain> executeBindings( Graph g, Node [] results )
        { return executeBindings( args().put( NamedTripleBunches.anon, g ), results ); }
                
    public ExtendedIterator<Domain> executeBindings( Graph g, List<Stage> stages, Node [] results )
        { return executeBindings( stages, args().put( NamedTripleBunches.anon, g ), results ); }
    
    public ExtendedIterator<Domain> executeBindings( NamedGraphMap args, Node [] nodes )
        { return executeBindings( new ArrayList<Stage>(), args, nodes ); }
        
    /**
        the standard "default" implementation of executeBindings.
    */
    public ExtendedIterator<Domain> executeBindings( List<Stage> outStages, NamedGraphMap args, Node [] nodes )
        {
        SimpleQueryEngine e = new SimpleQueryEngine( triplePattern, sortMethod, constraint );
        ExtendedIterator<Domain> result = e.executeBindings( outStages, args, nodes );
        lastQueryEngine = e;
        return result;
        }
    
    private SimpleQueryEngine lastQueryEngine = null;
        
    /** mapping of graph name -> graph */
    private NamedGraphMap argMap = new NamedGraphMap();
            
    public NamedGraphMap args()
        { return argMap; }

    public TripleSorter getSorter()
        { return sortMethod; }
        
    public void setTripleSorter( TripleSorter ts )
        { sortMethod = ts == null ? TripleSorter.dontSort : ts; }
        
    private TripleSorter sortMethod = TripleSorter.dontSort;
    
    public int getVariableCount()
        { return lastQueryEngine.getVariableCount(); }
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
