/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: SimpleQueryEngine.java,v 1.1 2009/06/29 08:55:45 castagna Exp $
*/

package com.hp.hpl.jena.graph.query;

import java.util.*;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.*;

/**
	SimpleQueryEngine

	@author kers
*/
public class SimpleQueryEngine 
    {
    private ExpressionSet constraint;
    private NamedTripleBunches triples;
    private TripleSorter sortMethod;
    private int variableCount;
    
    /**
         @deprecated NamedTripleBunches are not supported. Use SimpleQueryEngine
             ( List, TripleSorter, ExpressionSet ) instead.
    */
	@Deprecated
    public SimpleQueryEngine( NamedTripleBunches triples, TripleSorter ts, ExpressionSet constraint )
        { this.constraint = constraint; 
        this.triples = triples; 
        this.sortMethod = ts; }
        
    public SimpleQueryEngine( List<Triple> pattern, TripleSorter sorter, ExpressionSet constraints )
        { this.constraint = constraints; 
        this.triples = asNamedTripleBunches( pattern ); 
        this.sortMethod = sorter; }

    private static NamedTripleBunches asNamedTripleBunches( List<Triple> pattern )
        {
        NamedTripleBunches result = new NamedTripleBunches();
        for (Iterator<Triple> elements = pattern.iterator(); elements.hasNext();)
            result.add( NamedTripleBunches.anon, elements.next() );
        return result;
        }

    int getVariableCount()
        { return variableCount; }
        
    public ExtendedIterator<Domain> executeBindings( List<Stage> outStages, NamedGraphMap args, Node [] nodes )
        {
        Mapping map = new Mapping( nodes );
        ArrayList<Stage> stages = new ArrayList<Stage>();        
        addStages( stages, args, map );
        if (constraint.isComplex()) stages.add( new ConstraintStage( map, constraint ) );
        outStages.addAll( stages );
        variableCount = map.size();
        return filter( connectStages( stages, variableCount ) );
        }
                                  
    private ExtendedIterator<Domain> filter( final Stage allStages )
        {
        // final Pipe complete = allStages.deliver( new BufferPipe() );
        return new NiceIterator<Domain>()
            {
            private Pipe complete;
            
            private void ensurePipe()
                { if (complete == null) complete = allStages.deliver( new BufferPipe() ); }
            
            @Override public void close() { allStages.close(); clearPipe(); }
            
            @Override public Domain next() { ensurePipe(); return complete.get(); }
            
            @Override public boolean hasNext() { ensurePipe(); return complete.hasNext(); }
            
            private void clearPipe()
                { 
                int count = 0; 
                while (hasNext()) { count += 1; next(); }
                }
            };
        }
        
    public static Cons cons( Triple pattern, Object cons )
        { return new Cons( pattern, (Cons) cons ); }
        
    public static class Cons
        {
        Triple head;
        Cons tail;
        Cons( Triple head, Cons tail ) { this.head = head; this.tail = tail; }
        static int size( Cons L ) { int n = 0; while (L != null) { n += 1; L = L.tail; } return n; }
        }
                
    private void addStages( ArrayList<Stage> stages, NamedGraphMap arguments, Mapping map )
        {
        Iterator<Map.Entry<String, Cons>> it2 = triples.entrySetIterator();
        while (it2.hasNext())
            {
            Map.Entry<String, Cons> e = it2.next();
            String name = e.getKey();
            Cons nodeTriples = e.getValue();
            Graph g = arguments.get( name );
            int nBlocks = Cons.size( nodeTriples ), i = nBlocks;
            Triple [] nodes = new Triple[nBlocks];
            while (nodeTriples != null)
                {
                nodes[--i] = nodeTriples.head;
                nodeTriples = nodeTriples.tail;
                }
            nodes = sortTriples( nodes );
            Stage next = g.queryHandler().patternStage( map, constraint, nodes );
            stages.add( next );
            }
        }

    private Triple [] sortTriples( Triple [] ts )
        { return sortMethod.sort( ts ); }
                
    private Stage connectStages( ArrayList<Stage> stages, int count )
        {
        Stage current = Stage.initial( count );
        for (int i = 0; i < stages.size(); i += 1)
            current = stages.get( i ).connectFrom( current );
        return current;
        }                                          
    }

/*
    (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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