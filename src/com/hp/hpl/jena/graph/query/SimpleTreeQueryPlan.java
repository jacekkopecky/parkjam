/*
  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: SimpleTreeQueryPlan.java,v 1.1 2009/06/29 08:55:45 castagna Exp $
*/

package com.hp.hpl.jena.graph.query;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.*;

import java.util.*;

/**
    Incomplete class. Do not use.
*/
public class SimpleTreeQueryPlan implements TreeQueryPlan
	{
	private Graph pattern;
	private Graph target;
	
	public SimpleTreeQueryPlan( Graph target, Graph pattern )
		{
		this.target = target;
		this.pattern = pattern;
		}
		
	public Graph executeTree() 
		{ 
		Graph result = Factory.createGraphMem();
		Set<Node> roots = getRoots( pattern );
		for (Iterator<Node> it = roots.iterator(); it.hasNext(); handleRoot( result, it.next(), new HashSet<Triple>() )) {}
		return result;
		}
		
	private Iterator<Triple> findFromTriple( Graph g, Triple t )
		{
		return g.find( asPattern( t.getSubject() ), asPattern( t.getPredicate() ), asPattern( t.getObject() ) );
		}
		
	private Node asPattern( Node x )
		{ return x.isBlank() ? null : x; }
		
	private void handleRoot( Graph result, Node root, Set<Triple> pending )
		{
		ClosableIterator<Triple> it = pattern.find( root, null, null );
		if (!it.hasNext())
			{
			absorb( result, pending );
			return;
			}
		while (it.hasNext())
			{
			Triple base = it.next();
			Iterator<Triple> that = findFromTriple( target, base ); 
			while (that.hasNext())
				{
				Triple x = that.next();
				pending.add( x );
				handleRoot( result, base.getObject(), pending );
				}
			}
		}
		
	private void absorb( Graph result, Set<Triple> triples )
		{
		for (Iterator<Triple> it = triples.iterator(); it.hasNext(); result.add( it.next())) {}
		triples.clear(); 
		}
		
	public static Set<Node> getRoots( Graph pattern )
		{
		Set<Node> roots = CollectionFactory.createHashedSet();
		ClosableIterator<Triple> sub = GraphUtil.findAll( pattern );
		while (sub.hasNext()) roots.add( sub.next().getSubject() );
		ClosableIterator<Triple> obj = GraphUtil.findAll( pattern );
		while (obj.hasNext()) roots.remove( obj.next().getObject() );
		return roots;
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
