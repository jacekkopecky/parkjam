/******************************************************************
 * File:        XSDGenericType.java
 * Created by:  Dave Reynolds
 * Created on:  13-Dec-2002
 * 
 * (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: XSDGenericType.java,v 1.1 2009/06/29 08:55:58 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.datatypes.xsd.impl;

//ANDROID: removed Xerces dependence
//import org.apache.xerces.impl.dv.XSSimpleType;

import com.hp.hpl.jena.datatypes.xsd.*;

//ANDROID: user defined datatypes are not supported in this release of Androjena, so 
// this class content is commented, and the class is just here as a reminder
/**
 * Datatype template that adapts any response back from Xerces type parsing
 * to an appropriate java representation. This is primarily used in creating
 * user defined types - the built in types have a fixed mapping.
 * <p>
 * This class is probably now redundant in that XSDDatatype can support
 * run time conversion of union results. Left in for now during restructuring and
 * in case any existing user code expects this type - very unlikely.
 * </p>
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:58 $
 */
public class XSDGenericType {//extends XSDDatatype {

//    /**
//     * Hidden constructor used when loading in external user defined XSD types
//     * @param xstype the XSSimpleType definition to be wrapped
//     * @param namespace the namespace for the type (used because the grammar loading doesn't seem to keep that)
//     */
//    public XSDGenericType(XSSimpleType xstype, String namespace) {
//        super(xstype, namespace);
//    }
//
//
////  No longer need to perform any special case processing of union types since we
////  now do runtime type coercion - is that right?
//
////    /**
////     * Parse a lexical form of this datatype to a value
////     * @throws DatatypeFormatException if the lexical form is not legal
////     */
////    public Object parse(String lexicalForm) throws DatatypeFormatException {
////        try {
////            ValidationContext context = new ValidationState();
////            ValidatedInfo resultInfo = new ValidatedInfo();
////            Object result = typeDeclaration.validate(lexicalForm, context, resultInfo);
////            return convertValidatedDataValue(resultInfo);
////        } catch (InvalidDatatypeValueException e) { 
////            throw new DatatypeFormatException(lexicalForm, this, "during parse -" + e);
////        } 
////    }
//    
// 
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