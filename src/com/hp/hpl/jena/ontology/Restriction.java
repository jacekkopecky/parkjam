/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            10 Feb 2003
 * Filename           $RCSfile: Restriction.java,v $
 * Revision           $Revision: 1.2 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2009/10/06 13:04:34 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (see footer for full conditions)
 * ****************************************************************************/

// Package
///////////////
package com.hp.hpl.jena.ontology;



// Imports
///////////////
import com.hp.hpl.jena.rdf.model.*;


/**
 * <p>
 * Interface that encapsulates a class description formed by restricting one or
 * more properties to have constrained values and/or cardinalities.
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:ian_dickinson@users.sourceforge.net" >email</a>)
 * @version CVS $Id: Restriction.java,v 1.2 2009/10/06 13:04:34 ian_dickinson Exp $
 */
public interface Restriction
    extends OntClass
{
    // Constants
    //////////////////////////////////


    // External signature methods
    //////////////////////////////////

    // onProperty
    
    /**
     * <p>Assert that the property that this restriction applies to is the given property. Any existing 
     * statements for <code>onProperty</code> will be removed.</p>
     * @param prop The property that this restriction applies to
     * @exception OntProfileException If the {@link Profile#ON_PROPERTY()} property is not supported in the current language profile.   
     */ 
    public void setOnProperty( Property prop );

    /**
     * <p>Answer the property that this property restriction applies to. If there is
     * more than one such resource, an arbitrary selection is made (though well-defined property restrictions
     * should not have more than one <code>onProperty</code> statement.</p>
     * @return The property that this property restriction applies to
     * @exception OntProfileException If the {@link Profile#ON_PROPERTY()} property is not supported in the current language profile.   
     */ 
    public OntProperty getOnProperty();

    /**
     * <p>Answer true if this restriction is a property restriction on the given property.</p>
     * @param prop A property to test against
     * @return True if this restriction is a restriction on <code>prop</code>
     * @exception OntProfileException If the {@link Profile#ON_PROPERTY()} property is not supported in the current language profile.   
     */
    public boolean onProperty( Property prop );
    
    /**
     * <p>Remove the given property as the property that this restriction applies to.  If this statement
     * is not true of the current model, nothing happens.</p>
     * @param prop The property to be removed as a the property that this restriction applies to
     */
    public void removeOnProperty( Property prop );
    


    // facets
    
    /** 
     * <p>Answer a view of this restriction as an all values from  expression</p>
     * @return This class, but viewed as an AllValuesFromRestriction node
     * @exception ConversionException if the class cannot be converted to an all values from restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public AllValuesFromRestriction asAllValuesFromRestriction();
         
    /** 
     * <p>Answer a view of this restriction as a some values from  expression</p>
     * @return This class, but viewed as a SomeValuesFromRestriction node
     * @exception ConversionException if the class cannot be converted to an all values from restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public SomeValuesFromRestriction asSomeValuesFromRestriction();
         
    /** 
     * <p>Answer a view of this restriction as a has value expression</p>
     * @return This class, but viewed as a HasValueRestriction node
     * @exception ConversionException if the class cannot be converted to a has value restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public HasValueRestriction asHasValueRestriction();
         
    /** 
     * <p>Answer a view of this restriction as a cardinality restriction class expression</p>
     * @return This class, but viewed as a CardinalityRestriction node
     * @exception ConversionException if the class cannot be converted to a cardinality restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public CardinalityRestriction asCardinalityRestriction();

    /** 
     * <p>Answer a view of this restriction as a min cardinality restriction class expression</p>
     * @return This class, but viewed as a MinCardinalityRestriction node
     * @exception ConversionException if the class cannot be converted to a min cardinality restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public MinCardinalityRestriction asMinCardinalityRestriction();

    /** 
     * <p>Answer a view of this restriction as a max cardinality restriction class expression</p>
     * @return This class, but viewed as a MaxCardinalityRestriction node
     * @exception ConversionException if the class cannot be converted to a max cardinality restriction
     * given the lanuage profile and the current state of the underlying model.
     */
    public MaxCardinalityRestriction asMaxCardinalityRestriction();


    // type tests
    
    /** 
     * <p>Answer true if this restriction is an all values from restriction</p>
     * @return True if this is an allValuesFrom property restriction
     * @exception ProfileException if {@link Profile#ALL_VALUES_FROM()} is not supported in the current profile
     */
    public boolean isAllValuesFromRestriction();
         
    /** 
     * <p>Answer true if this restriction is a some values from restriction</p>
     * @return True if this is a someValuesFrom property restriction
     * @exception ProfileException if {@link Profile#SOME_VALUES_FROM()} is not supported in the current profile
     */
    public boolean isSomeValuesFromRestriction();
         
    /** 
     * <p>Answer true if this restriction is a has value restriction</p>
     * @return True if this is a hasValue property restriction
     * @exception ProfileException if {@link Profile#HAS_VALUE()} is not supported in the current profile
     */
    public boolean isHasValueRestriction();
         
    /** 
     * <p>Answer true if this restriction is a cardinality restriction (ie is a property restriction
     * constructed with an <code>owl:cardinality</code> operator, or similar). This is not a test for
     * a restriction that tests cardinalities in general.</p>
     * @return True if this is a cardinality property restriction
     * @exception ProfileException if {@link Profile#CARDINALITY()} is not supported in the current profile
     */
    public boolean isCardinalityRestriction();

    /** 
     * <p>Answer true if this restriction is a min cardinality restriction (ie is a property restriction
     * constructed with an <code>owl:minCardinality</code> operator, or similar). This is not a test for
     * a restriction that tests cardinalities in general.</p>
     * @return True if this is a minCardinality property restriction
     * @exception ProfileException if {@link Profile#MIN_CARDINALITY()} is not supported in the current profile
     */
    public boolean isMinCardinalityRestriction();

    /** 
     * <p>Answer true if this restriction is a max cardinality restriction (ie is a property restriction
     * constructed with an <code>owl:maxCardinality</code> operator, or similar). This is not a test for
     * a restriction that tests cardinalities in general.</p>
     * @return True if this is a maxCardinality property restriction
     * @exception ProfileException if {@link Profile#MAX_CARDINALITY()} is not supported in the current profile
     */
    public boolean isMaxCardinalityRestriction();


    // conversions
    
    /** 
     * <p>Convert this restriction to an all values from class expression.</p>
     * @param cls The class to which all values of the restricted property must belong, to be in the
     * extension of this restriction
     * @return This class, but converted to a AllValuesFromRestriction class expression
     * @exception ProfileException if {@link Profile#ALL_VALUES_FROM()} is not supported in the current profile
     */
    public AllValuesFromRestriction convertToAllValuesFromRestriction( Resource cls );
         
    /** 
     * <p>Convert this restriction to a some values from class expression</p>
     * @param cls The class to which at least one value of the restricted property must belong, to be in the
     * extension of this restriction
     * @return This class, but converted to a SomeValuesFromRestriction node
     * @exception ProfileException if {@link Profile#SOME_VALUES_FROM()} is not supported in the current profile
     */
    public SomeValuesFromRestriction convertToSomeValuesFromRestriction( Resource cls );
         
    /** 
     * <p>Convert this restriction to a has value class expression</p>
     * @param value The value which the restricted property must have, for resource to be
     * in the extension of this restriction. Can be a resource or a literal.
     * @return This class, but converted to a HasValueRestriction
     * @exception ProfileException if {@link Profile#HAS_VALUE()} is not supported in the current profile
     */
    public HasValueRestriction convertToHasValueRestriction( RDFNode value );
         
    /** 
     * <p>Convert this restriction to a cardinality restriction class expression</p>
     * @param cardinality The exact cardinality for the restricted property
     * @return This class, but converted to a CardinalityRestriction node
     * @exception ProfileException if {@link Profile#CARDINALITY()} is not supported in the current profile
     */
    public CardinalityRestriction convertToCardinalityRestriction( int cardinality );

    /** 
     * <p>Convert this restriction to a min cardinality restriction class expression</p>
     * @param cardinality The minimum cardinality for the restricted property
     * @return This class, but converted to a MinCardinalityRestriction node
     * @exception ProfileException if {@link Profile#MIN_CARDINALITY()} is not supported in the current profile
     */
    public MinCardinalityRestriction convertToMinCardinalityRestriction( int cardinality );

    /** 
     * <p>Convert this restriction to a max cardinality restriction class expression</p>
     * @param cardinality The maximum cardinality for the restricted property
     * @return This class, but converted to a MaxCardinalityRestriction node
     * @exception ProfileException if {@link Profile#MAX_CARDINALITY()} is not supported in the current profile
     */
    public MaxCardinalityRestriction convertToMaxCardinalityRestriction( int cardinality );

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

