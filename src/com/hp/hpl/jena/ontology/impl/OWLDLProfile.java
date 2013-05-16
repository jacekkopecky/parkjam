/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            10 Feb 2003
 * Filename           $RCSfile: OWLDLProfile.java,v $
 * Revision           $Revision: 1.3 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2009/10/06 13:04:42 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (see footer for full conditions)
 *****************************************************************************/

// Package
///////////////
package com.hp.hpl.jena.ontology.impl;
import com.hp.hpl.jena.rdf.model.*;


// Imports
///////////////
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.enhanced.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.ontology.*;

import java.util.*;



/**
 * <p>
 * Ontology language profile implementation for the DL variant of the OWL 2002/07 language.
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:ian_dickinson@users.sourceforge.net" >email</a>)
 * @version CVS $Id: OWLDLProfile.java,v 1.3 2009/10/06 13:04:42 ian_dickinson Exp $
 */
public class OWLDLProfile
    extends OWLProfile
{
    // Constants
    //////////////////////////////////


    // Static variables
    //////////////////////////////////


    // Instance variables
    //////////////////////////////////


    // Constructors
    //////////////////////////////////


    // External signature methods
    //////////////////////////////////


    /**
     * <p>
     * Answer a descriptive string for this profile, for use in debugging and other output.
     * </p>
     * @return "OWL DL"
     */
    @Override
    public String getLabel() {
        return "OWL DL";
    }


    // Internal implementation methods
    //////////////////////////////////

    protected static Object[][] s_supportsCheckData = new Object[][] {
            // Resource (key),              check method
            {  AllDifferent.class,          new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.AllDifferent.asNode() );
                }
            }
            },
            {  AnnotationProperty.class,    new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    for (Iterator<Resource> i = ((OntModel) g).getProfile().getAnnotationProperties();  i.hasNext(); ) {
                        if (i.next().asNode().equals( n )) {
                            // a built-in annotation property
                            return true;
                        }
                    }
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.AnnotationProperty.asNode() );
                }
            }
            },
            {  OntClass.class,              new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph eg ) {
                    Graph g = eg.asGraph();
                    return hasType( n, eg, new Resource[] {OWL.Class, OWL.Restriction, RDFS.Class, RDFS.Datatype} ) ||
                           // These are common cases that we should support
                           n.equals( OWL.Thing.asNode() ) ||
                           n.equals( OWL.Nothing.asNode() ) ||
                           g.contains( Node.ANY, RDFS.domain.asNode(), n ) ||
                           g.contains( Node.ANY, RDFS.range.asNode(), n ) ||
                           g.contains( n, OWL.intersectionOf.asNode(), Node.ANY ) ||
                           g.contains( n, OWL.unionOf.asNode(), Node.ANY ) ||
                           g.contains( n, OWL.complementOf.asNode(), Node.ANY )
                           ;
                }
            }
            },
            {  DatatypeProperty.class,      new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.DatatypeProperty.asNode() );
                }
            }
            },
            {  ObjectProperty.class,        new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return hasType( n, g, new Resource[] {OWL.ObjectProperty,OWL.TransitiveProperty,
                                                          OWL.SymmetricProperty, OWL.InverseFunctionalProperty} );
                }
            }
            },
            {  FunctionalProperty.class,    new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.FunctionalProperty.asNode() );
                }
            }
            },
            {  InverseFunctionalProperty.class, new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.InverseFunctionalProperty.asNode() ) &&
                    !g.asGraph().contains( n, RDF.type.asNode(), OWL.DatatypeProperty.asNode() );
                }
            }
            },
            {  RDFList.class,               new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return n.equals( RDF.nil.asNode() )  ||
                    g.asGraph().contains( n, RDF.type.asNode(), RDF.List.asNode() );
                }
            }
            },
            {  OntProperty.class,           new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return hasType( n, g, new Resource[] {RDF.Property, OWL.ObjectProperty, OWL.DatatypeProperty,
                                                          OWL.AnnotationProperty, OWL.TransitiveProperty,
                                                          OWL.SymmetricProperty, OWL.InverseFunctionalProperty,
                                                          OWL.FunctionalProperty} );
                }
            }
            },
            {  Ontology.class,              new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Ontology.asNode() );
                }
            }
            },
            {  Restriction.class,           new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() );
                }
            }
            },
            {  AllValuesFromRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g, n, OWL.allValuesFrom ) &&
                    containsSome( g, n, OWL.onProperty );
                }
            }
            },
            {  SomeValuesFromRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g,n, OWL.someValuesFrom ) &&
                    containsSome( g,n, OWL.onProperty );
                }
            }
            },
            {  HasValueRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g, n, OWL.hasValue ) &&
                    containsSome( g, n, OWL.onProperty );
                }
            }
            },
            {  CardinalityRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g, n, OWL.cardinality ) &&
                    containsSome( g, n, OWL.onProperty );
                }
            }
            },
            {  MinCardinalityRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g, n, OWL.minCardinality ) &&
                    containsSome( g, n, OWL.onProperty );
                }
            }
            },
            {  MaxCardinalityRestriction.class,   new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.Restriction.asNode() ) &&
                    containsSome( g, n, OWL.maxCardinality ) &&
                    containsSome( g, n, OWL.onProperty );
                }
            }
            },
            {  SymmetricProperty.class,     new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.SymmetricProperty.asNode() ) &&
                    !g.asGraph().contains( n, RDF.type.asNode(), OWL.DatatypeProperty.asNode() );
                }
            }
            },
            {  TransitiveProperty.class,    new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return g.asGraph().contains( n, RDF.type.asNode(), OWL.TransitiveProperty.asNode() ) &&
                    !g.asGraph().contains( n, RDF.type.asNode(), OWL.DatatypeProperty.asNode() );
                }
            }
            },
            {  Individual.class,    new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    if (n instanceof Node_URI || n instanceof Node_Blank) {
                        return !hasType( n, g, new Resource[] {RDFS.Class, RDF.Property, OWL.Class,
                                                               OWL.ObjectProperty, OWL.DatatypeProperty, OWL.TransitiveProperty,
                                                               OWL.FunctionalProperty, OWL.InverseFunctionalProperty} );
                    }
                    else {
                        return false;
                    }
                }
            }
            },
            {  DataRange.class,    new SupportsCheck() {
                @Override
                public boolean doCheck( Node n, EnhGraph g ) {
                    return n instanceof Node_Blank  &&
                           g.asGraph().contains( n, RDF.type.asNode(), OWL.DataRange.asNode() );
                }
            }
            }
            };


    // to allow concise reference in the code above.
    public static boolean containsSome( EnhGraph g, Node n, Property p ) {
        return AbstractProfile.containsSome( g, n, p );
    }

    /** Map from resource to syntactic/semantic checks that a node can be seen as the given facet */
    private static HashMap<Class<?>,SupportsCheck> s_supportsChecks = new HashMap<Class<?>, SupportsCheck>();

    static {
        // initialise the map of supports checks from a table of static data
        for (int i = 0;  i < s_supportsCheckData.length;  i++) {
            s_supportsChecks.put( (Class<?>) s_supportsCheckData[i][0], (SupportsCheck) s_supportsCheckData[i][1] );
        }
    }

    @Override
    protected Map<Class<?>,SupportsCheck> getCheckTable() {
        return s_supportsChecks;
    }

    //==============================================================================
    // Inner class definitions
    //==============================================================================


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

