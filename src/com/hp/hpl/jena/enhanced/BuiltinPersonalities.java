/*
  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: BuiltinPersonalities.java,v 1.1 2009/06/29 08:55:56 castagna Exp $
*/

package com.hp.hpl.jena.enhanced;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.ontology.impl.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;

/**
    The personalities that are provided for the existing Jena classes. It is likely that this
    should be factored.

    @author jjc + kers
*/
public class BuiltinPersonalities {

    static final private Personality<RDFNode> graph = new Personality<RDFNode>();

    static final public Personality<RDFNode> model = graph.copy()
        .add( Resource.class, ResourceImpl.factory )
        .add( Property.class, PropertyImpl.factory )
        .add( Literal.class,LiteralImpl.factory )
        .add( Container.class, ResourceImpl.factory )
        .add( Alt.class, AltImpl.factory )
        .add( Bag.class, BagImpl.factory )
        .add( Seq.class, SeqImpl.factory )
        .add( ReifiedStatement.class, ReifiedStatementImpl.reifiedStatementFactory )
        .add( RDFList.class, RDFListImpl.factory )

        // ontology additions
        .add( OntResource.class, OntResourceImpl.factory )
        .add( Ontology.class, OntologyImpl.factory )
        .add( OntClass.class, OntClassImpl.factory )
        .add( EnumeratedClass.class, EnumeratedClassImpl.factory )
        .add( IntersectionClass.class, IntersectionClassImpl.factory )
        .add( UnionClass.class, UnionClassImpl.factory )
        .add( ComplementClass.class, ComplementClassImpl.factory )
        .add( DataRange.class, DataRangeImpl.factory )

        .add( Restriction.class, RestrictionImpl.factory )
        .add( HasValueRestriction.class, HasValueRestrictionImpl.factory )
        .add( AllValuesFromRestriction.class, AllValuesFromRestrictionImpl.factory )
        .add( SomeValuesFromRestriction.class, SomeValuesFromRestrictionImpl.factory )
        .add( CardinalityRestriction.class, CardinalityRestrictionImpl.factory )
        .add( MinCardinalityRestriction.class, MinCardinalityRestrictionImpl.factory )
        .add( MaxCardinalityRestriction.class, MaxCardinalityRestrictionImpl.factory )
        .add( QualifiedRestriction.class, QualifiedRestrictionImpl.factory )
        .add( MinCardinalityQRestriction.class, MinCardinalityQRestrictionImpl.factory )
        .add( MaxCardinalityQRestriction.class, MaxCardinalityQRestrictionImpl.factory )
        .add( CardinalityQRestriction.class, CardinalityQRestrictionImpl.factory )

        .add( OntProperty.class, OntPropertyImpl.factory )
        .add( ObjectProperty.class, ObjectPropertyImpl.factory )
        .add( DatatypeProperty.class, DatatypePropertyImpl.factory )
        .add( TransitiveProperty.class, TransitivePropertyImpl.factory )
        .add( SymmetricProperty.class, SymmetricPropertyImpl.factory )
        .add( FunctionalProperty.class, FunctionalPropertyImpl.factory )
        .add( InverseFunctionalProperty.class, InverseFunctionalPropertyImpl.factory )
        .add( AllDifferent.class, AllDifferentImpl.factory )
        .add( Individual.class, IndividualImpl.factory )
        .add( AnnotationProperty.class, AnnotationPropertyImpl.factory )

        // Last and least ?
        .add( RDFNode.class, ResourceImpl.rdfNodeFactory )
        ;


    /**
     * For debugging purposes, list the standard personalities on the given
     * output writer.
     *
     * @param writer A printwriter to list the personalities mapping to
     */
    static public void listPersonalities( PrintWriter writer ) {
        for (Iterator<Map.Entry<Class<? extends RDFNode>, Implementation>> i = model.getMap().entrySet().iterator();  i.hasNext(); ) 
            {
            Map.Entry<Class<? extends RDFNode>, Implementation> e = i.next();
            writer.println( "personality key " + e.getKey().getName() + " -> value " + e.getValue() );
            }
        writer.flush();
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
