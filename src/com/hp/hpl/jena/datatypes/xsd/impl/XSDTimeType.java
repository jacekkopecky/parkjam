/******************************************************************
 * File:        XSDTimeType.java
 * Created by:  Dave Reynolds
 * Created on:  04-Dec-2003
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 * $Id: XSDTimeType.java,v 1.1 2009/06/29 08:55:58 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.datatypes.xsd.impl;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

/**
 * Type processor for time, most of the machinery is in the base
 * XSDAbstractDateTimeType class.
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:58 $
 */
public class XSDTimeType extends XSDAbstractDateTimeType {

	/**
	 * Constructor
	 */
	public XSDTimeType(String typename) {
		super(typename);
	}

	// ANDROID: removed Xerces dependence, parsing and validation are done in
	// android.jena.replacements.xsd classes
	// /**
	// * Parse a validated date. This is invoked from
	// * XSDDatatype.convertValidatedDataValue rather then from a local
	// * parse method to make the implementation of XSDGenericType easier.
	// */
	// @Override
	// public Object parseValidated(String str) {
	// int len = str.length();
	// int[] date = new int[TOTAL_SIZE];
	// int[] timeZone = new int[2];
	//
	// // time
	// // initialize to default values
	// date[CY]=YEAR;
	// date[M]=MONTH;
	// date[D]=DAY;
	// getTime(str, 0, len, date, timeZone);
	//
	// if ( date[utc]!=0 && date[utc]!='Z' ) {
	// AbstractDateTime.normalize(date, timeZone);
	// }
	//
	// return new XSDDateTime(date, TIME_MASK);
	// }

	/**
	 * Convert a value of this datatype out to lexical form.
	 */
	@Override
	public String unparse(Object value) {
		if (value instanceof XSDDateTime) {
			return ((XSDDateTime) value).timeLexicalForm();
		} else {
			// Don't think we should ever get here
			return value.toString();
		}
	}

}

/*
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard
 * Development Company, LP All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
