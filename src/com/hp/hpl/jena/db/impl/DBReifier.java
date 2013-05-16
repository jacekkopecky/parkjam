/*
  (c) Copyright 2002, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: DBReifier.java,v 1.1 2009/06/29 08:55:37 castagna Exp $
*/

package com.hp.hpl.jena.db.impl;

import com.hp.hpl.jena.db.*;

/**
 *  Implementation of Reifier for graphs stored in a database.
 * 
 * @author csayers based in part on SimpleReifier by kers.
*/

import java.util.List;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.shared.*;

public class DBReifier implements Reifier
    {
    protected GraphRDB m_parent = null;
    protected Graph m_hiddenTriples = null;    
	protected List<SpecializedGraphReifier> m_reifiers = null;
	protected List<SpecializedGraphReifier> m_hidden_reifiers = null;

	// For now, we just deal with a single specializedGraphReifier,
	// but in the future we could replace this with a list of
	// those and operate much as the GraphRDB implementation
	// does with it's list of SpecializedGraphs.
	protected SpecializedGraphReifier m_reifier = null;
    
    protected ReificationStyle m_style;
    
	/** 
	 *  Construct a reifier for GraphRDB's.
	 *  
	 *  @param parent the Graph for which we will expose reified triples.
	 *  @param allReifiers a List of SpecializedGraphReifiers which reifiy triples in that graph.
	 *  @param hiddenReifiers the subset of allReifiers whose triples are hidden when querying the parent graph.
	 */
	public DBReifier(GraphRDB parent, ReificationStyle style, 
	                 List<SpecializedGraphReifier> allReifiers, 
	                 List<SpecializedGraphReifier> hiddenReifiers ) {
		m_parent = parent;
		m_reifiers = allReifiers;
		m_hidden_reifiers = hiddenReifiers;
        m_style = style;
		
		// For now, just take the first specializedGraphReifier
		if (m_reifiers.size() != 1)
			throw new BrokenException("Internal error - DBReifier requires exactly one SpecializedGraphReifier");
		m_reifier = m_reifiers.get(0);
	}
            
    /* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#getParentGraph()
	 */
	public Graph getParentGraph() { 
    	return m_parent; }
        
    public ReificationStyle getStyle()
        { return m_style; }

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#getHiddenTriples()
	 */
	private Graph getReificationTriples() {
		if( m_hiddenTriples == null) 
            m_hiddenTriples = new DBReifierGraph(m_parent, m_hidden_reifiers);
		return m_hiddenTriples;
	}
    
    public ExtendedIterator<Triple> find( TripleMatch m )
        { return getReificationTriples().find( m ); }
    
    public ExtendedIterator<Triple> findExposed( TripleMatch m )
        { return getReificationTriples().find( m ); }
    
    public ExtendedIterator<Triple> findEither( TripleMatch m, boolean showHidden )
        { return showHidden == m_style.conceals() ? getReificationTriples().find( m ) : Triple.None; }

    public int size() 
        { return m_style.conceals() ? 0 : getReificationTriples().size(); }

    /**
        Utility method useful for its short name: answer a new CompletionFlag
        initialised to false.
    */
    protected static SpecializedGraph.CompletionFlag newComplete()  
        { return new SpecializedGraph.CompletionFlag(); }
        
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#reifyAs(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Triple)
	 */
	public Node reifyAs( Node n, Triple t ) {
		m_reifier.add( n, t, newComplete() );
		return n;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#hasTriple(com.hp.hpl.jena.graph.Node)
	 */
	public boolean hasTriple(Node n) {
		return m_reifier.findReifiedTriple( n, newComplete() ) != null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#hasTriple(com.hp.hpl.jena.graph.Triple)
	 */
	public boolean hasTriple( Triple t ) {
		return m_reifier.findReifiedNodes(t, newComplete() ).hasNext();
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#allNodes()
	 */
	public ExtendedIterator<Node> allNodes() {
		return m_reifier.findReifiedNodes( null, newComplete() );
	}
    
    /**
        All the nodes reifying triple <code>t</code>, using the matching code
        from SimpleReifier.
    */
    public ExtendedIterator<Node> allNodes( Triple t )
        { return m_reifier.findReifiedNodes( t, newComplete() ); }
        
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#remove(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Triple)
	 */
	public void remove( Node n, Triple t ) {
		m_reifier.delete( n, t, newComplete() );
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#remove(com.hp.hpl.jena.graph.Triple)
	 */
	public void remove( Triple t ) {
		m_reifier.delete(null,t, newComplete() );
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#handledAdd(com.hp.hpl.jena.graph.Triple)
	 */
	public boolean handledAdd(Triple t) {
		SpecializedGraph.CompletionFlag complete = newComplete();
		m_reifier.add(t, complete);
		return complete.isDone();
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Reifier#handledRemove(com.hp.hpl.jena.graph.Triple)
	 */
	public boolean handledRemove(Triple t) {
		SpecializedGraph.CompletionFlag complete = newComplete();
		m_reifier.delete(t, complete);
		return complete.isDone();
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.GetTriple#getTriple(com.hp.hpl.jena.graph.Node)
	 */
	public Triple getTriple(Node n) {
		return m_reifier.findReifiedTriple(n, newComplete() );
	}
    
    public void close() {
        // TODO anything useful for a close operation
    }
        
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
