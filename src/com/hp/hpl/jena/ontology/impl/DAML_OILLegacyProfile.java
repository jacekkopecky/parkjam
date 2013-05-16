/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            10 Feb 2003
 * Filename           $RCSfile: DAML_OILLegacyProfile.java,v $
 * Revision           $Revision: 1.2 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2009/10/06 13:04:42 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (see footer for full conditions)
 *****************************************************************************/

// Package
///////////////
package com.hp.hpl.jena.ontology.impl;


// Imports
///////////////
import com.hp.hpl.jena.enhanced.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;

import java.util.*;



/**
 * <p>
 * Vocabulary constants for DAML+OIL March 2001 version. This version of the vocabulary is
 * the one that was in use up to Jena 2.1.  It has been replaced by a new version that
 * uses RDFS namespace terms for subClass and subProperty, which is more normal practice
 * in DAML ontologies. This version is retained for backwards compatibility with Jena 2.1
 * programs.
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:ian_dickinson@users.sourceforge.net" >email</a>)
 * @version CVS $Id: DAML_OILLegacyProfile.java,v 1.2 2009/10/06 13:04:42 ian_dickinson Exp $
 */
public class DAML_OILLegacyProfile
    extends AbstractProfile
{
    // Constants
    //////////////////////////////////


    // Static variables
    //////////////////////////////////


    // Instance variables
    //////////////////////////////////

    /** Model to hold the vocabulary resources only */
    private Model m_vocabModel = ModelFactory.createDefaultModel();

    // class resources
    private Resource m_class                        = m_vocabModel.createResource( DAML_OIL.Class.getURI()                     );
    private Resource m_restriction                  = m_vocabModel.createResource( DAML_OIL.Restriction.getURI()               );
    private Resource m_thing                        = m_vocabModel.createResource( DAML_OIL.Thing.getURI()                     );
    private Resource m_nothing                      = m_vocabModel.createResource( DAML_OIL.Nothing.getURI()                   );
    private Resource m_property                     = m_vocabModel.createResource( DAML_OIL.Property.getURI()                  );
    private Resource m_objectProperty               = m_vocabModel.createResource( DAML_OIL.ObjectProperty.getURI()            );
    private Resource m_datatypeProperty             = m_vocabModel.createResource( DAML_OIL.DatatypeProperty.getURI()          );
    private Resource m_transitiveProperty           = m_vocabModel.createResource( DAML_OIL.TransitiveProperty.getURI()        );
    private Resource m_symmetricProperty            = null;
    private Resource m_functionalProperty           = m_vocabModel.createResource( DAML_OIL.UniqueProperty.getURI()            );
    private Resource m_inverseFunctionalProperty    = m_vocabModel.createResource( DAML_OIL.UnambiguousProperty.getURI()       );
    private Resource m_allDifferent                 = null;
    private Resource m_ontology                     = m_vocabModel.createResource( DAML_OIL.Ontology.getURI()                  );
    private Resource m_deprecatedClass              = null;
    private Resource m_deprecatedProperty           = null;
    private Resource m_annotationProperty           = null;
    private Resource m_ontologyProperty             = null;
    private Resource m_list                         = m_vocabModel.createResource( DAML_OIL.List.getURI()                      );
    private Resource m_nil                          = m_vocabModel.createResource( DAML_OIL.nil.getURI()                       );
    private Resource m_datarange                    = null;

    private Property m_equivalentProperty           = m_vocabModel.createProperty( DAML_OIL.samePropertyAs.getNameSpace(),          DAML_OIL.samePropertyAs.getLocalName() );
    private Property m_equivalentClass              = m_vocabModel.createProperty( DAML_OIL.sameClassAs.getNameSpace(),             DAML_OIL.sameClassAs.getLocalName() );
    private Property m_disjointWith                 = m_vocabModel.createProperty( DAML_OIL.disjointWith.getNameSpace(),            DAML_OIL.disjointWith.getLocalName() );
    private Property m_sameIndividualAs             = m_vocabModel.createProperty( DAML_OIL.sameIndividualAs.getNameSpace(),        DAML_OIL.sameIndividualAs.getLocalName() );
    private Property m_sameAs                       = m_vocabModel.createProperty( DAML_OIL.equivalentTo.getNameSpace(),            DAML_OIL.equivalentTo.getLocalName() );
    private Property m_differentFrom                = m_vocabModel.createProperty( DAML_OIL.differentIndividualFrom.getNameSpace(), DAML_OIL.differentIndividualFrom.getLocalName() );
    private Property m_distinctMembers              = null;
    private Property m_unionOf                      = m_vocabModel.createProperty( DAML_OIL.unionOf.getNameSpace(),                 DAML_OIL.unionOf.getLocalName() );
    private Property m_intersectionOf               = m_vocabModel.createProperty( DAML_OIL.intersectionOf.getNameSpace(),          DAML_OIL.intersectionOf.getLocalName() );
    private Property m_complementOf                 = m_vocabModel.createProperty( DAML_OIL.complementOf.getNameSpace(),            DAML_OIL.complementOf.getLocalName() );
    private Property m_oneOf                        = m_vocabModel.createProperty( DAML_OIL.oneOf.getNameSpace(),                   DAML_OIL.oneOf.getLocalName() );
    private Property m_onProperty                   = m_vocabModel.createProperty( DAML_OIL.onProperty.getNameSpace(),              DAML_OIL.onProperty.getLocalName() );
    private Property m_allValuesFrom                = m_vocabModel.createProperty( DAML_OIL.toClass.getNameSpace(),                 DAML_OIL.toClass.getLocalName() );
    private Property m_hasValue                     = m_vocabModel.createProperty( DAML_OIL.hasValue.getNameSpace(),                DAML_OIL.hasValue.getLocalName() );
    private Property m_someValuesFrom               = m_vocabModel.createProperty( DAML_OIL.hasClass.getNameSpace(),                DAML_OIL.hasClass.getLocalName() );
    private Property m_minCardinality               = m_vocabModel.createProperty( DAML_OIL.minCardinality.getNameSpace(),          DAML_OIL.minCardinality.getLocalName() );
    private Property m_maxCardinality               = m_vocabModel.createProperty( DAML_OIL.maxCardinality.getNameSpace(),          DAML_OIL.maxCardinality.getLocalName() );
    private Property m_cardinality                  = m_vocabModel.createProperty( DAML_OIL.cardinality.getNameSpace(),             DAML_OIL.cardinality.getLocalName() );
    private Property m_inverseOf                    = m_vocabModel.createProperty( DAML_OIL.inverseOf.getNameSpace(),               DAML_OIL.inverseOf.getLocalName() );
    private Property m_imports                      = m_vocabModel.createProperty( DAML_OIL.imports.getNameSpace(),                 DAML_OIL.imports.getLocalName() );
    private Property m_versionInfo                  = m_vocabModel.createProperty( DAML_OIL.versionInfo.getNameSpace(),             DAML_OIL.versionInfo.getLocalName() );
    private Property m_priorVersion                 = null;
    private Property m_backwardsCompatibleWith      = null;
    private Property m_incompatibleWith             = null;
    private Property m_subPropertyOf                = m_vocabModel.createProperty( DAML_OIL.subPropertyOf.getNameSpace(),           DAML_OIL.subPropertyOf.getLocalName() );
    private Property m_subClassOf                   = m_vocabModel.createProperty( DAML_OIL.subClassOf.getNameSpace(),              DAML_OIL.subClassOf.getLocalName() );
    private Property m_domain                       = m_vocabModel.createProperty( DAML_OIL.subClassOf.getNameSpace(),              DAML_OIL.domain.getLocalName() );
    private Property m_range                        = m_vocabModel.createProperty( DAML_OIL.subClassOf.getNameSpace(),              DAML_OIL.range.getLocalName() );
    private Property m_first                        = m_vocabModel.createProperty( DAML_OIL.first.getNameSpace(),                   DAML_OIL.first.getLocalName() );
    private Property m_rest                         = m_vocabModel.createProperty( DAML_OIL.rest.getNameSpace(),                    DAML_OIL.rest.getLocalName() );
    private Property m_minCardinalityQ              = m_vocabModel.createProperty( DAML_OIL.minCardinalityQ.getNameSpace(),         DAML_OIL.minCardinalityQ.getLocalName() );
    private Property m_maxCardinalityQ              = m_vocabModel.createProperty( DAML_OIL.maxCardinalityQ.getNameSpace(),         DAML_OIL.maxCardinalityQ.getLocalName() );
    private Property m_cardinalityQ                 = m_vocabModel.createProperty( DAML_OIL.cardinalityQ.getNameSpace(),            DAML_OIL.cardinalityQ.getLocalName() );
    private Property m_hasClassQ                    = m_vocabModel.createProperty( DAML_OIL.hasClassQ.getNameSpace(),               DAML_OIL.hasClassQ.getLocalName() );

    // Constructors
    //////////////////////////////////


    // External signature methods
    //////////////////////////////////

    /**
     * <p>
     * Answer the string that is the namespace prefix for this vocabulary
     * </p>
     *
     * @return The namespace prefix <code>http://www.daml.org/2001/03/daml+oil#</code>
     */
    public static String _NAMESPACE() {             return "http://www.daml.org/2001/03/daml+oil#"; }


    public String   NAMESPACE() {                   return DAML_OILProfile._NAMESPACE(); }

    public Resource CLASS() {                       return m_class; }
    public Resource RESTRICTION() {                 return m_restriction; }
    public Resource THING() {                       return m_thing; }
    public Resource NOTHING() {                     return m_nothing; }
    public Resource PROPERTY() {                    return m_property; }
    public Resource OBJECT_PROPERTY() {             return m_objectProperty; }
    public Resource DATATYPE_PROPERTY() {           return m_datatypeProperty; }
    public Resource TRANSITIVE_PROPERTY() {         return m_transitiveProperty; }
    public Resource SYMMETRIC_PROPERTY() {          return m_symmetricProperty; }
    public Resource FUNCTIONAL_PROPERTY() {         return m_functionalProperty; }
    public Resource INVERSE_FUNCTIONAL_PROPERTY() { return m_inverseFunctionalProperty; }
    public Resource ALL_DIFFERENT() {               return m_allDifferent; }
    public Resource ONTOLOGY() {                    return m_ontology; }
    public Resource DEPRECATED_CLASS() {            return m_deprecatedClass; }
    public Resource DEPRECATED_PROPERTY() {         return m_deprecatedProperty; }
    public Resource ANNOTATION_PROPERTY() {         return m_annotationProperty; }
    public Resource ONTOLOGY_PROPERTY() {           return m_ontologyProperty; }
    public Resource LIST() {                        return m_list; }
    public Resource NIL() {                         return m_nil; }
    public Resource DATARANGE() {                   return m_datarange; }

    public Property EQUIVALENT_PROPERTY() {         return m_equivalentProperty; }
    public Property EQUIVALENT_CLASS() {            return m_equivalentClass; }
    public Property DISJOINT_WITH() {               return m_disjointWith; }
    public Property SAME_INDIVIDUAL_AS() {          return m_sameIndividualAs; }
    public Property SAME_AS() {                     return m_sameAs; }
    public Property DIFFERENT_FROM() {              return m_differentFrom; }
    public Property DISTINCT_MEMBERS() {            return m_distinctMembers; }
    public Property UNION_OF() {                    return m_unionOf; }
    public Property INTERSECTION_OF() {             return m_intersectionOf; }
    public Property COMPLEMENT_OF() {               return m_complementOf; }
    public Property ONE_OF() {                      return m_oneOf; }
    public Property ON_PROPERTY() {                 return m_onProperty; }
    public Property ALL_VALUES_FROM() {             return m_allValuesFrom; }
    public Property HAS_VALUE() {                   return m_hasValue; }
    public Property SOME_VALUES_FROM() {            return m_someValuesFrom; }
    public Property MIN_CARDINALITY() {             return m_minCardinality; }
    public Property MAX_CARDINALITY() {             return m_maxCardinality; }
    public Property CARDINALITY() {                 return m_cardinality; }
    public Property INVERSE_OF() {                  return m_inverseOf; }
    public Property IMPORTS() {                     return m_imports; }
    public Property PRIOR_VERSION() {               return m_priorVersion; }
    public Property BACKWARD_COMPATIBLE_WITH() {    return m_backwardsCompatibleWith; }
    public Property INCOMPATIBLE_WITH() {           return m_incompatibleWith; }
    public Property SUB_CLASS_OF() {                return m_subClassOf; }
    public Property SUB_PROPERTY_OF() {             return m_subPropertyOf; }
    public Property DOMAIN() {                      return m_domain; }
    public Property RANGE() {                       return m_range; }
    public Property FIRST() {                       return m_first; }
    public Property REST() {                        return m_rest; }
    public Property MIN_CARDINALITY_Q() {           return m_minCardinalityQ; }
    public Property MAX_CARDINALITY_Q() {           return m_maxCardinalityQ; }
    public Property CARDINALITY_Q() {               return m_cardinalityQ; }
    public Property HAS_CLASS_Q() {                 return m_hasClassQ; }


    // Annotations
    public Property VERSION_INFO() {                return m_versionInfo; }
    public Property LABEL() {                       return RDFS.label; }
    public Property COMMENT() {                     return RDFS.comment; }
    public Property SEE_ALSO() {                    return RDFS.seeAlso; }
    public Property IS_DEFINED_BY() {               return RDFS.isDefinedBy; }

    @Override
    protected Resource[][] aliasTable() {
        return new Resource[][] {
            {DAML_OIL.subClassOf,                   RDFS.subClassOf},
            {DAML_OIL.Literal,                      RDFS.Literal},
            {DAML_OIL.Property,                     RDF.Property},
            {DAML_OIL.type,                         RDF.type},
            {DAML_OIL.value,                        RDF.value},
            {DAML_OIL.subPropertyOf,                RDFS.subPropertyOf},
            {DAML_OIL.domain,                       RDFS.domain},
            {DAML_OIL.range,                        RDFS.range},
            {DAML_OIL.label,                        RDFS.label},
            {DAML_OIL.comment,                      RDFS.comment},
            {DAML_OIL.seeAlso,                      RDFS.seeAlso},
            {DAML_OIL.isDefinedBy,                  RDFS.isDefinedBy},
        };
    }

    /** There are no first-class axioms in DAML */
    public Iterator<Resource> getAxiomTypes() {
        return Arrays.asList(
            new Resource[] {
            }
        ).iterator();
    }

    /** The annotation properties of DAML (currently none) */
    public Iterator<Resource> getAnnotationProperties() {
        return Arrays.asList(
            new Resource[] {
            }
        ).iterator();
    }

    public Iterator<Resource> getClassDescriptionTypes() {
        return Arrays.asList(
            new Resource[] {
                DAML_OIL.Class,
                DAML_OIL.Restriction
            }
        ).iterator();
    }



    /**
     * <p>
     * Answer true if the given graph supports a view of this node as the given
     * language element, according to the semantic constraints of the profile.
     * If strict checking on the ontology model is turned off, this check is
     * skipped.
     * </p>
     *
     * @param n A node to test
     * @param g The enhanced graph containing <code>n</code>, which is assumed to
     * be an {@link OntModel}.
     * @param type A class indicating the facet that we are testing against.
     * @return True if strict checking is off, or if <code>n</code> can be
     * viewed according to the facet resource <code>res</code>
     */
    public <T> boolean isSupported( Node n, EnhGraph g, Class<T> type ) {
        if (g instanceof OntModel) {
            OntModel m = (OntModel) g;

            if (type == null) {
                // if the facet resource is null, the facet is not in this profile so
                // we automatically return false;
                return false;
            }
            else if (!m.strictMode()) {
                // checking turned off
                return true;
            }
            else {
                // lookup the profile check for this resource
                SupportsCheck check = (SupportsCheck) s_supportsChecks.get( type );

                return (check == null)  || check.doCheck( n, g );
            }
        }
        else {
            return false;
        }
    }


    /**
     * <p>
     * Answer a descriptive string for this profile, for use in debugging and other output.
     * </p>
     * @return "DAML+OIL"
     */
    public String getLabel() {
        return "DAML+OIL";
    }



    // Internal implementation methods
    //////////////////////////////////


    //==============================================================================
    // Inner class definitions
    //==============================================================================

    /** Helper class for doing syntactic/semantic checks on a node */
    protected static class SupportsCheck
    {
        public boolean doCheck( Node n, EnhGraph g ) {
            return true;
        }
    }


    // Table of check data
    //////////////////////

    private static Object[][] s_supportsCheckTable = new Object[][] {
        // Resource (key),              check method
        {  OntClass.class,              new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Class.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), RDFS.Class.asNode() ) ||
                                                       // common cases we should support
                                                       n.equals( DAML_OIL.Thing.asNode() ) ||
                                                       n.equals( DAML_OIL.Nothing.asNode() ) ||
                                                       g.asGraph().contains( Node.ANY, DAML_OIL.domain.asNode(), n ) ||
                                                       g.asGraph().contains( Node.ANY, DAML_OIL.range.asNode(), n )
                                                       ;
                                            }
                                        }
        },
        {  DatatypeProperty.class,      new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.DatatypeProperty.asNode() );
                                            }
                                        }
        },
        {  ObjectProperty.class,        new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                               return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.ObjectProperty.asNode() ) ||
                                                      g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.TransitiveProperty.asNode() ) ||
                                                      g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.UnambiguousProperty.asNode() );
                                            }
                                        }
        },
        {  FunctionalProperty.class,    new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                // DAML's alias for functional property is uniqueProperty
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.UniqueProperty.asNode() );
                                            }
                                        }
        },
        {  InverseFunctionalProperty.class, new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                // DAML's alias for functional property is unambiguousProperty
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.UnambiguousProperty.asNode() );
                                            }
                                        }
        },
        {  RDFList.class,               new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return n.equals( DAML_OIL.nil.asNode() )  ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.List.asNode() );
                                            }
                                        }
        },
        {  Ontology.class,              new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return n.equals( RDF.nil.asNode() )  ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Ontology.asNode() );
                                            }
                                        }
        },
        {  OntProperty.class,           new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), RDF.Property.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Property.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.ObjectProperty.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.DatatypeProperty.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.TransitiveProperty.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.UnambiguousProperty.asNode() ) ||
                                                       g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.UniqueProperty.asNode() )
                                                       ;
                                            }
                                        }
        },
        {  Restriction.class,           new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() );
                                            }
                                        }
        },
        {  HasValueRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.hasValue ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  AllValuesFromRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.toClass ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  SomeValuesFromRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.hasClass ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  CardinalityRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.cardinality ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  MinCardinalityRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.minCardinality ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  MaxCardinalityRestriction.class,   new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       containsSome( g, n, DAML_OIL.maxCardinality ) &&
                                                       containsSome( g, n, DAML_OIL.onProperty );
                                            }
                                        }
        },
        {  TransitiveProperty.class,    new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.TransitiveProperty.asNode() ) &&
                                                       !g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.DatatypeProperty.asNode() );
                                            }
                                        }
        },
        {  QualifiedRestriction.class,  new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       g.asGraph().contains( n, DAML_OIL.hasClassQ.asNode(), Node.ANY );
                                            }
                                        }
        },
        {  CardinalityQRestriction.class,  new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       g.asGraph().contains( n, DAML_OIL.cardinalityQ.asNode(), Node.ANY );
                                            }
                                        }
        },
        {  MinCardinalityQRestriction.class,  new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       g.asGraph().contains( n, DAML_OIL.minCardinalityQ.asNode(), Node.ANY );
                                            }
                                        }
        },
        {  MaxCardinalityQRestriction.class,  new SupportsCheck() {
                                            @Override
                                            public boolean doCheck( Node n, EnhGraph g ) {
                                                return g.asGraph().contains( n, RDF.type.asNode(), DAML_OIL.Restriction.asNode() ) &&
                                                       g.asGraph().contains( n, DAML_OIL.maxCardinalityQ.asNode(), Node.ANY );
                                            }
                                        }
        },
    };

    /* just to avoid having to decorate all the calls above */

    public static boolean  containsSome( EnhGraph g, Node n, Property p ) {
        return AbstractProfile.containsSome( g, n, p );
    }

    // Static variables
    //////////////////////////////////

    /** Map from resource to syntactic/semantic checks that a node can be seen as the given facet */
    protected static HashMap<Object, Object> s_supportsChecks = new HashMap<Object, Object>();

    static {
        // initialise the map of supports checks from a table of static data
        for (int i = 0;  i < s_supportsCheckTable.length;  i++) {
            s_supportsChecks.put( s_supportsCheckTable[i][0], s_supportsCheckTable[i][1] );
        }
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

