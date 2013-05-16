/*
  (c) Copyright 2002, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: EnhGraph.java,v 1.2 2009/10/06 13:04:43 ian_dickinson Exp $
*/

package com.hp.hpl.jena.enhanced;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.cache.*;

/**
   TODO: remove the polymorphic aspect of EnhGraphs.
<p>
    A specialisation of Polymorphic that models an extended graph - that is, one that 
    contains{@link EnhNode Enhanced nodes} or one that itself exposes additional 
    capabilities beyond the graph API.
 <p>   
    <span style="color:red">WARNING</span>. The polymorphic aspects of EnhGraph 
    are <span style="color:red">not supported</span> and are not expected to be
    supported in this way for the indefinite future.
    
    @author <a href="mailto:Jeremy.Carroll@hp.com">Jeremy Carroll</a> (original code)
    <br><a href="mailto:Chris.Dollin@hp.com">Chris Dollin</a> (original code)
    <br><a href="mailto:ian_dickinson@users.sourceforge.net">Ian Dickinson</a> 
    (refactoring and commentage)
*/

public class EnhGraph 
//    extends Polymorphic 
{
    // Instance variables
    /** The graph that this enhanced graph is wrapping */
    protected Graph graph;
    
    /** Counter that helps to ensure that caches are kept distinct */
    static private int cnt = 0;

    /** Cache of enhanced nodes that have been created */
    protected Cache enhNodes = CacheManager.createCache( CacheManager.ENHNODECACHE, "EnhGraph-" + cnt++, 1000 );
    
    /** The unique personality that is bound to this polymorphic instace */
    private Personality<RDFNode> personality;

//    @Override public boolean isValid()
//        { return true; }
    
    // Constructors
    /**
     * Construct an enhanced graph from the given underlying graph, and
     * a factory for generating enhanced nodes.
     * 
     * @param g The underlying plain graph, may be null to defer binding to a given 
     *      graph until later.
     * @param p The personality factory, that maps types to realisations
     */
    public EnhGraph( Graph g, Personality<RDFNode> p ) {
        super();
        graph = g;
        personality = p;
    }
   
    // External methods
    
    /**
     * Answer the normal graph that this enhanced graph is wrapping.
     * @return A graph
     */
    public Graph asGraph() {
        return graph;
    }
   
    /**
     * Hashcode for an enhnaced graph is delegated to the underlyin graph.
     * @return The hashcode as an int
     */
    @Override final public int hashCode() {
     	return graph.hashCode();
    }

     
    /**
     * An enhanced graph is equal to another graph g iff the underlying graphs
     * are equal.
     * This  is deemed to be a complete and correct interpretation of enhanced
     * graph equality, which is why this method has been marked final.
     * <p> Note that this equality test does not look for correspondance between
     * the structures in the two graphs.  To test whether another graph has the
     * same nodes and edges as this one, use {@link #isIsomorphicWith}.
     * </p>
     * @param o An object to test for equality with this node
     * @return True if o is equal to this node.
     * @see #isIsomorphicWith
     */
    @Override final public boolean equals(Object o) {
        return 
            this == o 
            || o instanceof EnhGraph && graph.equals(((EnhGraph) o).asGraph());
    }
    
    
    /**
     * Answer true if the given enhanced graph contains the same nodes and 
     * edges as this graph.  The default implementation delegates this to the
     * underlying graph objects.
     * 
     * @param eg A graph to test
     * @return True if eg is a graph with the same structure as this.
     */
    final public boolean isIsomorphicWith(EnhGraph eg){
        return graph.isIsomorphicWith(eg.graph);
    }

    /**
     * Answer an enhanced node that wraps the given node and conforms to the given
     * interface type.
     * 
     * @param n A node (assumed to be in this graph)
     * @param interf A type denoting the enhanced facet desired
     * @return An enhanced node
     */
    public <X extends RDFNode> X getNodeAs( Node n, Class<X> interf ) 
        {
         // We use a cache to avoid reconstructing the same Node too many times.
        EnhNode eh = (EnhNode) enhNodes.get( n );
        if (eh == null)
            {           
            // not in the cache, so build a new one
            X constructed = personality.newInstance( interf, n, this );
            enhNodes.put( n, constructed );        
            return constructed;
            }
        else
            return eh.viewAs( interf );
        }
    
    /**
     * Answer the cache controlle for this graph
     * @return A cache controller object
     */
    public CacheControl getNodeCacheControl() {
         return enhNodes;
    }
    
    /**
     * Set the cache controller object for this graph
     * @param cc The cache controller
     */
    public void setNodeCache(Cache cc) {
         enhNodes = cc;
    }
     
//     
//    /** 
//     * Answer an enhanced graph that presents <i>this</i> in a way which satisfies type
//     * t.  This is a stub method that has not yet been implemented.
//     @param t A type
//     @return A polymorphic instance, possibly but not necessarily this, that conforms to t.
//     */
//    @Override protected Polymorphic convertTo(Class t) {
//        throw new PersonalityConfigException
//            ( "Alternative perspectives on graphs has not been implemented yet" );
//    }
//    
//    /**
//        we can't convert to anything. 
//    */
//    @Override protected boolean canSupport( Class t )
//        { return false; }
        
    /**
     * Answer the personality object bound to this polymorphic instance
     * 
     * @return The personality object
     */
    protected Personality<RDFNode> getPersonality() {
        return personality;
    }
    
}

/*
    (c) Copyright 2002, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
