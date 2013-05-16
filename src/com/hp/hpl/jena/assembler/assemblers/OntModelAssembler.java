/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: OntModelAssembler.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;

import java.util.*;

import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

public class OntModelAssembler extends InfModelAssembler implements Assembler
    {
    @Override public Model openEmptyModel( Assembler a, Resource root, Mode mode )
        {
        checkType( root, JA.OntModel );
        Model baseModel = getBase( a, root, mode );
        OntModelSpec oms = getOntModelSpec( a, root );
        OntModel om = ModelFactory.createOntologyModel( oms, baseModel );
        addSubModels( a, root, mode, om );
        return om;
        }

    private void addSubModels( Assembler a, Resource root, Mode mode, OntModel om )
        {
        List<Model> subModels = getSubModels( a, root, mode );
        for (Iterator<Model> it = subModels.iterator(); it.hasNext();)
            om.addSubModel( it.next() );
        }

    private List<Model> getSubModels( Assembler a, Resource root, Mode mode )
        {
        List<Model> result = new ArrayList<Model>();
        for (StmtIterator it = root.listProperties( JA.subModel ); it.hasNext();)
            result.add( a.openModel( it.nextStatement().getResource(), mode ) );
        return result;
        }

    private static final OntModelSpec defaultSpec = OntModelSpec.OWL_MEM_RDFS_INF;

    protected OntModelSpec getOntModelSpec( Assembler a, Resource root )
        {
        Resource r = getUniqueResource( root, JA.ontModelSpec );
        return r == null ? defaultSpec : (OntModelSpec) a.open( r );
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