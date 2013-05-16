/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            01-Apr-2003
 * Filename           $RCSfile: ObjectPropertyImpl.java,v $
 * Revision           $Revision: 1.2 $
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



// Imports
///////////////
import java.util.*;

import com.hp.hpl.jena.enhanced.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;



/**
 * <p>
 * Implementation of the object property abstraction
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:ian_dickinson@users.sourceforge.net" >email</a>)
 * @version CVS $Id: ObjectPropertyImpl.java,v 1.2 2009/10/06 13:04:42 ian_dickinson Exp $
 */
public class ObjectPropertyImpl
    extends OntPropertyImpl
    implements ObjectProperty
{
    // Constants
    //////////////////////////////////

    // Static variables
    //////////////////////////////////

    /**
     * A factory for generating ObjectProperty facets from nodes in enhanced graphs.
     * Note: should not be invoked directly by user code: use
     * {@link com.hp.hpl.jena.rdf.model.RDFNode#as as()} instead.
     */
    @SuppressWarnings("hiding")
    public static Implementation factory = new Implementation() {
        @Override
        public EnhNode wrap( Node n, EnhGraph eg ) {
            if (canWrap( n, eg )) {
                return new ObjectPropertyImpl( n, eg );
            }
            else {
                throw new ConversionException( "Cannot convert node " + n + " to ObjectProperty");
            }
        }

        @Override
        public boolean canWrap( Node node, EnhGraph eg ) {
            // node will support being an ObjectProperty facet if it has rdf:type owl:ObjectProperty or equivalent
            Profile profile = (eg instanceof OntModel) ? ((OntModel) eg).getProfile() : null;
            return (profile != null)  &&  profile.isSupported( node, eg, ObjectProperty.class );
        }
    };


    // Instance variables
    //////////////////////////////////

    // Constructors
    //////////////////////////////////

    /**
     * <p>
     * Construct a functional property node represented by the given node in the given graph.
     * </p>
     *
     * @param n The node that represents the resource
     * @param g The enh graph that contains n
     */
    public ObjectPropertyImpl( Node n, EnhGraph g ) {
        super( n, g );
    }


    // External signature methods
    //////////////////////////////////

    /**
     * <p>Answer a property that is an inverse of this property, ensuring that it
     * presents the ObjectProperty facet.</p>
     * @return A property inverse to this property
     * @exception OntProfileException If the {@link Profile#INVERSE_OF()} property is not supported in the current language profile.
     */
    @Override
    public OntProperty getInverseOf() {
        return super.getInverseOf().asObjectProperty();
    }

    /**
     * <p>Answer an iterator over all of the properties that are declared to be inverse properties of
     * this property, ensuring that each presents the objectProperty facet.</p>
     * @return An iterator over the properties inverse to this property.
     * @exception OntProfileException If the {@link Profile#INVERSE_OF()} property is not supported in the current language profile.
     */
    @Override
    public ExtendedIterator<? extends OntProperty> listInverseOf() {
        List<OntProperty> objPs = new ArrayList<OntProperty>();
        for (Iterator<? extends OntProperty> i = super.listInverseOf(); i.hasNext(); ) {
            objPs.add( i.next().as( ObjectProperty.class ) );
        }
        return WrappedIterator.create( objPs.iterator() );
    }

    /**
     * <p>Answer the property that is the inverse of this property, ensuring that it presents
     * the object property facet.</p>
     * @return The property that is the inverse of this property, or null.
     */
    @Override
    public OntProperty getInverse() {
        OntProperty inv = super.getInverse();
        return (inv != null) ? inv.asObjectProperty() : null;
    }


    // Internal implementation methods
    //////////////////////////////////

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

