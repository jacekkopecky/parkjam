/******************************************************************
 * File:        TransitiveReasonerFactory.java
 * Created by:  Dave Reynolds
 * Created on:  19-Jan-03
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: TransitiveReasonerFactory.java,v 1.1 2009/06/29 08:55:57 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.reasoner.transitiveReasoner;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.reasoner.*;

/**
 * Factory class for creating blank instances of the transitive reasoner.
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:57 $
 */
public class TransitiveReasonerFactory implements ReasonerFactory {
    
    /** Single global instance of this factory */
    private static ReasonerFactory theInstance = new TransitiveReasonerFactory();
    
    /** Cache of the capabilities description */
    protected Model capabilities;
    
    /** Static URI for this reasoner type */
    public static final String URI = "http://jena.hpl.hp.com/2003/TransitiveReasoner";
    
    /**
     * Return the single global instance of this factory
     */
    public static ReasonerFactory theInstance() {
        return theInstance;
    }
    
    /**
     * Constructor method that builds an instance of the associated Reasoner
     * @param configuration a set of arbitrary configuration information to be 
     * passed the reasoner encoded within an RDF graph. This reasoner has no
     * configuration parameters so this arg is always ignored.
     */
    public Reasoner create(Resource configuration) {
        return new TransitiveReasoner();
    }
   
    /**
     * Return a description of the capabilities of this reasoner encoded in
     * RDF. This method is normally called by the ReasonerRegistry which caches
     * the resulting information so dynamically creating here is not really an overhead.
     */
    public Model getCapabilities() {
        if (capabilities == null) {
            capabilities = ModelFactory.createDefaultModel();
            Resource base = capabilities.createResource(getURI());
            base.addProperty(ReasonerVocabulary.nameP, "Transitive Reasoner")
                .addProperty(ReasonerVocabulary.descriptionP, "Provides reflexive-transitive closure of subClassOf and subPropertyOf")
                .addProperty(ReasonerVocabulary.supportsP, RDFS.subClassOf)
                .addProperty(ReasonerVocabulary.supportsP, RDFS.subPropertyOf)
                .addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.directSubClassOf)
                .addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.directSubPropertyOf)
                .addProperty(ReasonerVocabulary.versionP, "0.1");
        }
        return capabilities;
    }
    
    /**
     * Return the URI labelling this type of reasoner
     */
    public String getURI() {
        return URI;
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

