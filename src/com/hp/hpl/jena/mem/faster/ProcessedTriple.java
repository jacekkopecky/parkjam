/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: ProcessedTriple.java,v 1.1 2009/06/29 08:55:55 castagna Exp $
*/

package com.hp.hpl.jena.mem.faster;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;

/**
    A ProcessedTriple is three QueryNodes; it knows how to deliver an
    optimised Matcher which will use only the necessary QueryNode.match
    methods.
    
    @author kers
*/
public class ProcessedTriple extends QueryTriple
    {    
    public ProcessedTriple( QueryNode S, QueryNode P, QueryNode O ) 
        { super( S, P, O ); }

    static final QueryNodeFactory factory = new QueryNodeFactoryBase()
        {
        @Override
        public QueryTriple createTriple( QueryNode S, QueryNode P, QueryNode O )
            { return new ProcessedTriple( S, P, O ); }
        
        @Override
        public QueryTriple [] createArray( int size )
            { return new ProcessedTriple[size]; }
        };

    @Override
    public Applyer createApplyer( Graph g )
        { return ((GraphMemFaster) g).createApplyer( this ); }

    public boolean hasNoVariables()
        { return S.isFrozen() && P.isFrozen() && O.isFrozen(); }
    }

/*
 * (c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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