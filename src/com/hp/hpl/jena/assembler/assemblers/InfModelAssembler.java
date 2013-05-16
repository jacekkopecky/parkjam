/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: InfModelAssembler.java,v 1.1 2009/06/29 08:55:48 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;


import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.*;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;

public class InfModelAssembler extends ModelAssembler
    {
    @Override
    protected Model openEmptyModel( Assembler a, Resource root, Mode mode )
        {
        checkType( root, JA.InfModel );
        Model base = getBase( a, root, mode );
        Reasoner reasoner = getReasoner( a, root );
        InfModel result = ModelFactory.createInfModel( reasoner, base );
        return result;
        }

    protected Model getBase( Assembler a, Resource root, Mode mode )
        {
        Resource base = getUniqueResource( root, JA.baseModel );
        return base == null ? ModelFactory.createDefaultModel() : a.openModel( base, mode );
        }

    protected Reasoner getReasoner( Assembler a, Resource root )
        { return getReasonerFactory( a, root ).create( root ); }
    
    protected ReasonerFactory getReasonerFactory( Assembler a, Resource root )
        { 
        Resource factory = getUniqueResource( root, JA.reasoner );
        return factory == null
            ? GenericRuleReasonerFactory.theInstance()
            : (ReasonerFactory) a.open( factory )
            ;        
        }
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