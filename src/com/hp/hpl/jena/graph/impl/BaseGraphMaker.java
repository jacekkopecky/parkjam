/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: BaseGraphMaker.java,v 1.1 2009/06/29 08:55:43 castagna Exp $
*/

package com.hp.hpl.jena.graph.impl;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.shared.*;

/**
    This base class provides convenience functions for the three "usual" graph
    makers and a place to hold the reification style for the graphs it constructs.   
 
 	@author kers
*/
public abstract class BaseGraphMaker implements GraphMaker
    {
    /**
        Construct the base level of a graph maker.
     	@param style the reification style for all the graphs it makes
     */
    public BaseGraphMaker( ReificationStyle style )
        { this.style = style; }
        
    private int counter = 0;
    protected ReificationStyle style;
    
    /**
        Answer our reification style.
    */
    public ReificationStyle getReificationStyle()
        { return style; }
        
    /**
        Answer the default graph for this maker. If we haven't already made it, make it
        now.
     */
    public Graph getGraph()
        { 
        if (defaultGraph == null) { defaultGraph = createGraph(); }
        return defaultGraph;
        }
        
    private Graph defaultGraph;
    
    public Graph openGraph()
        { if (defaultGraph == null) throw new DoesNotExistException
            ( "no default graph in this GraphMaker [" + this.getClass() + "]" ); 
        return defaultGraph; }
    
    /**
        Make a fresh anonymous graph.
    */
    public Graph createGraph()
        { return createGraph( "anon_" + counter++ + "", false ); }
         
     /**
        A non-strict create.
      	@see com.hp.hpl.jena.graph.GraphMaker#createGraph(java.lang.String)
      */
    public Graph createGraph(String name)
        { return createGraph( name, false ); }
        
    /**
        A non-strict open.
     	@see com.hp.hpl.jena.graph.GraphMaker#openGraph(java.lang.String)
     */
    public Graph openGraph( String name )
        { return openGraph( name, false ); }

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