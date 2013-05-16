/******************************************************************
 * File:        InfModel.java
 * Created by:  Dave Reynolds
 * Created on:  08-May-2003
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: InfModel.java,v 1.1 2009/06/29 08:55:38 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.rdf.model;

import com.hp.hpl.jena.reasoner.*;
import java.util.Iterator;

/**
 * An extension to the normal Model interface that supports access to any
 * underlying inference capability. 
 * <p>In Jena the primary use of inference is
 * to generate additional entailments from a set of RDF data. These entailments
 * just appear as additional RDF data in the inferred model and are accessed
 * through the normal API. For example, if an inference engine can determine
 * the class of a resource "foo" is "fooClass" then all Model API calls such
 * as listStatements and getProperty should act as if the triple:
 * <pre>
 * foo rdf:type fooClass .
 * </pre>
 * were in the data. </p>
 * 
 * <p>A few reasoner services cannot be made directly available in this way
 * and the InfGraph extension gives access to these - specifically access to 
 * validation/consistency checking, derivation traces and find-with-posits.</p>
 * 
 * <p>Note that this interface, and especially the interface onto ValidityReports
 * and Derivations are not yet stable.</p>
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:38 $
 */
public interface InfModel extends Model {

    /**
     * Return the raw RDF model being processed (i.e. the argument
     * to the Reasonder.bind call that created this InfModel).
     */
    public Model getRawModel();
    
    /**
     * Return the Reasoner which is being used to answer queries to this graph.
     */
    public Reasoner getReasoner();

    /**
     * Cause the inference model  to reconsult the underlying data to take
     * into account changes. Normally changes are made through the InfModel's add and
     * remove calls are will be handled appropriately. However, in some cases changes
     * are made "behind the InfModels's back" and this forces a full reconsult of
     * the changed data. 
     */
    public void rebind();
    
    /**
     * Perform any initial processing and caching. This call is optional. Most
     * engines either have negligable set up work or will perform an implicit
     * "prepare" if necessary. The call is provided for those occasions where
     * substantial preparation work is possible (e.g. running a forward chaining
     * rule system) and where an application might wish greater control over when
     * this prepration is done rather than just leaving to be done at first query time.
     */
    public void prepare();
    
    /**
     * Reset any internal caches. Some systems, such as the tabled backchainer, 
     * retain information after each query. A reset will wipe this information preventing
     * unbounded memory use at the expense of more expensive future queries. A reset
     * does not cause the raw data to be reconsulted and so is less expensive than a rebind.
     */
    public void reset();
    
    /**
     * Test the consistency of the underlying data. This normally tests
     * the validity of the bound instance data against the bound
     * schema data. 
     * <p>Logically inconsistent models will be indicated by a ValidityReport which
     * reports isValid() as false. Additional non.error problems, such as uninstantiatable classes,
     * may be reported as warnings.
     * @return a ValidityReport structure
     */
    public ValidityReport validate();    
    
    /** Find all the statements matching a pattern.
     * <p>Return an iterator over all the statements in a model
     *  that match a pattern.  The statements selected are those
     *  whose subject matches the <code>subject</code> argument,
     *  whose predicate matches the <code>predicate</code> argument
     *  and whose object matchesthe <code>object</code> argument.
     *  If an argument is <code>null</code> it matches anything.</p>
     * <p>
     * The s/p/o terms may refer to resources which are temporarily defined in the "posit" model.
     * This allows one, for example, to query what resources are of type CE where CE is a
     * class expression rather than a named class - put CE in the posit arg.</p>
     * 
     * @return an iterator over the subjects
     * @param subject   The subject sought
     * @param predicate The predicate sought
     * @param object    The value sought
     */ 
    public StmtIterator listStatements( Resource subject, Property predicate, RDFNode object, Model posit );
    
    /**
     * Switch on/off drivation logging. If this is switched on then every time an inference
     * is a made that fact is recorded and the resulting record can be access through a later
     * getDerivation call. This may consume a lot of space!
     */
    public void setDerivationLogging(boolean logOn);
   
    /**
     * Return the derivation of the given statement (which should be the result of
     * some previous list operation).
     * Not all reasoneers will support derivations.
     * @return an iterator over Derivation records or null if there is no derivation information
     * available for this triple.
     * @see com.hp.hpl.jena.reasoner.Derivation Derviation
     */
    public Iterator<Derivation> getDerivation(Statement statement);    
    
    /**
     * Returns a derivations model. The rule reasoners typically create a 
     * graph containing those triples added to the base graph due to rule firings.
     * In some applications it can useful to be able to access those deductions
     * directly, without seeing the raw data which triggered them. In particular,
     * this allows the forward rules to be used as if they were rewrite transformation
     * rules.
     * @return the deductions model, if relevant for this class of inference
     * engine or null if not.
     */
    public Model getDeductionsModel(); 

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