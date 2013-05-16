/*
 *  (c)     Copyright 2000, 2001, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *   All rights reserved.
 * [See end of file]
 *  $Id: Abbreviated.java,v 1.1 2009/06/29 08:55:51 castagna Exp $
 */

package com.hp.hpl.jena.xmloutput.impl;

import java.io.PrintWriter;
import java.io.Writer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DAML_OIL;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.RDFSyntax;
//Writer;
//import java.io.PrintWriter;

/** Writes out RDF in the abbreviated syntax,  for human consumption 
   not only machine readable.
 * It is not normal to call the constructor directly, but to use
 * the method RDFWriterF.getWriter("RDF/XML-ABBREV").
 * Does not support the <code>NSPREFIXPROPBASE</code> system properties.
 * Use <code>setNsPrefix</code>.
 * For best results it is necessary to set the property 
   <code>"prettyTypes"</code>. See setProperty for information.
   @see com.hp.hpl.jena.rdf.model.RDFWriterF#getWriter(String)
 * @author jjc
 * @version  Release='$Name:  $' Revision='$Revision: 1.1 $' Date='$Date: 2009/06/29 08:55:51 $'
 */
public class Abbreviated extends BaseXMLWriter implements RDFErrorHandler {

	private Resource types[] =
		new Resource[] {
			DAML_OIL.Ontology,
			OWL.Ontology,
			DAML_OIL.Datatype,
			//OWL.DataRange, named or orphaned dataranges unusual.      
			RDFS.Datatype,
			DAML_OIL.Class,
			RDFS.Class,
			OWL.Class,
			DAML_OIL.Property,
			OWL.ObjectProperty,
			RDF.Property,
			DAML_OIL.ObjectProperty,
			OWL.DatatypeProperty,
			DAML_OIL.DatatypeProperty,
			OWL.TransitiveProperty,
			OWL.SymmetricProperty,
			OWL.FunctionalProperty,
			OWL.InverseFunctionalProperty,
			DAML_OIL.TransitiveProperty,
			DAML_OIL.UnambiguousProperty,
			DAML_OIL.UniqueProperty,
			};
            
	boolean sReification;
    
    
	boolean sIdAttr;
    boolean sDamlCollection;
    boolean sParseTypeCollectionPropertyElt;
    boolean sListExpand;
    boolean sParseTypeLiteralPropertyElt;
    boolean sParseTypeResourcePropertyElt;
    boolean sPropertyAttr;

    boolean sResourcePropertyElt;

	@Override
    protected void unblockAll() {
		sDamlCollection = false;
		sReification = false;
		sResourcePropertyElt = false;
		sParseTypeLiteralPropertyElt = false;
		sParseTypeResourcePropertyElt = false;
		sParseTypeCollectionPropertyElt = false;
		sIdAttr = false;
		sPropertyAttr = false;
        sListExpand = false;
	}
    
    {
        unblockAll();
        blockRule(RDFSyntax.propertyAttr);
    }
    
    @Override
    protected void blockRule(Resource r) {
        if (r.equals(RDFSyntax.sectionReification)) sReification=true;
       // else if (r.equals(RDFSyntax.resourcePropertyElt)) sResourcePropertyElt=true;
        else if (r.equals(RDFSyntax.sectionListExpand)) sListExpand=true;
        else if (r.equals(RDFSyntax.parseTypeLiteralPropertyElt)) sParseTypeLiteralPropertyElt=true;
        else if (r.equals(RDFSyntax.parseTypeResourcePropertyElt)) sParseTypeResourcePropertyElt=true;
        else if (r.equals(RDFSyntax.parseTypeCollectionPropertyElt)) sParseTypeCollectionPropertyElt=true;
        else if (r.equals(RDFSyntax.idAttr)) {
            sIdAttr=true;
            sReification = true;
        }
        else if (r.equals(RDFSyntax.propertyAttr)) sPropertyAttr=true;
        else if (r.equals(DAML_OIL.collection)) sDamlCollection=true;
        else {
            logger.warn("Cannot block rule <"+r.getURI()+">");
        }
    }
	@Override
    Resource[] setTypes(Resource[] propValue) {
		Resource[] rslt = types;
		types = propValue;
		return rslt;
	}

	@Override
    synchronized public void write(Model baseModel, Writer out, String base)
	    { 
		if (baseModel.getGraph().getCapabilities().findContractSafe() == false) 
            {
			logger.warn( "Workaround for bugs 803804 and 858163: using RDF/XML (not RDF/XML-ABBREV) writer  for unsafe graph " + baseModel.getGraph().getClass() );
			baseModel.write( out, "RDF/XML", base );
            } 
        else
            super.write( baseModel, out, base );
		}
		
	@Override
    protected void writeBody(
		Model model,
		PrintWriter pw,
		String base,
		boolean useXMLBase) {
		Unparser unp = new Unparser(this, base, model, pw);

		unp.setTopLevelTypes(types);
		//unp.useNameSpaceDecl(nameSpacePrefices);
		if (useXMLBase)
			unp.setXMLBase(base);
		unp.write();
	}

	// Implemenatation of RDFErrorHandler
	public void error(Exception e) {
		errorHandler.error(e);
	}

	public void warning(Exception e) {
		errorHandler.warning(e);
	}

	public void fatalError(Exception e) {
		errorHandler.fatalError(e);
	}



}
/*
	(c) Copyright 200, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
