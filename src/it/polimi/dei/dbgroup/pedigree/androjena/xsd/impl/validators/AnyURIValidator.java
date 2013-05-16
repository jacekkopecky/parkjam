/*
 * Copyright 2010 Lorenzo Carrara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*	This code is mainly adapated from Xerces 2.6.0 and Jena 2.6.2 
 * Xerces copyright and license: 
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights reserved.
 * License http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Jena copyright and license:
 * Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Specific source classes:
 * 
 * Xerces:
 * org.apache.xerces.impl.dv.xs.AnyURIDV
 * 
 * Jena:
 * com.hp.hpl.jena.datatypes.xsd.XSDDatatype
 */

package it.polimi.dei.dbgroup.pedigree.androjena.xsd.impl.validators;

import it.polimi.dei.dbgroup.pedigree.androjena.xsd.XSDBuiltinTypeFormatException;
import it.polimi.dei.dbgroup.pedigree.androjena.xsd.impl.TypeValidator;

import org.apache.xerces.util.URI;


public class AnyURIValidator extends TypeValidator {

	private static final URI BASE_URI;
	static {
		URI uri = null;
		try {
			uri = new URI("abc://def.ghi.jkl");
		} catch (URI.MalformedURIException ex) {
		}
		BASE_URI = uri;
	}

	// before we return string we have to make sure it is correct URI as per
	// spec.
	// for some types (string and derived), they just return the string itself
	@Override
	public Object getActualValue(String content)
			throws XSDBuiltinTypeFormatException {
		// check 3.2.17.c0 must: URI (rfc 2396/2723)
		try {
			if (content.length() != 0) {
				// encode special characters using XLink 5.4 algorithm
				final String encoded = encode(content);
				// Support for relative URLs
				// According to Java 1.1: URLs may also be specified with a
				// String and the URL object that it is related to.
				new URI(BASE_URI, encoded);
			}
		} catch (URI.MalformedURIException ex) {
			throw new XSDBuiltinTypeFormatException(content,
					"invalid URI data", ex);
		}

		return content;
	}

	@Override
	public short getNormalizationType() {
		return NORMALIZE_TRIM;
	}

	// which ASCII characters need to be escaped
	private static boolean gNeedEscaping[] = new boolean[128];
	// the first hex character if a character needs to be escaped
	private static char gAfterEscaping1[] = new char[128];
	// the second hex character if a character needs to be escaped
	private static char gAfterEscaping2[] = new char[128];
	private static char[] gHexChs = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	// initialize the above 3 arrays
	static {
		for (int i = 0; i <= 0x1f; i++) {
			gNeedEscaping[i] = true;
			gAfterEscaping1[i] = gHexChs[i >> 4];
			gAfterEscaping2[i] = gHexChs[i & 0xf];
		}
		gNeedEscaping[0x7f] = true;
		gAfterEscaping1[0x7f] = '7';
		gAfterEscaping2[0x7f] = 'F';
		char[] escChs = { ' ', '<', '>', '"', '{', '}', '|', '\\', '^', '~',
				'`' };
		int len = escChs.length;
		char ch;
		for (int i = 0; i < len; i++) {
			ch = escChs[i];
			gNeedEscaping[ch] = true;
			gAfterEscaping1[ch] = gHexChs[ch >> 4];
			gAfterEscaping2[ch] = gHexChs[ch & 0xf];
		}
	}

	// To encode special characters in anyURI, by using %HH to represent
	// special ASCII characters: 0x00~0x1F, 0x7F, ' ', '<', '>', etc.
	// and non-ASCII characters (whose value >= 128).
	private static String encode(String anyURI) {
		int len = anyURI.length(), ch;
		StringBuffer buffer = new StringBuffer(len * 3);

		// for each character in the anyURI
		int i = 0;
		for (; i < len; i++) {
			ch = anyURI.charAt(i);
			// if it's not an ASCII character, break here, and use UTF-8
			// encoding
			if (ch >= 128)
				break;
			if (gNeedEscaping[ch]) {
				buffer.append('%');
				buffer.append(gAfterEscaping1[ch]);
				buffer.append(gAfterEscaping2[ch]);
			} else {
				buffer.append((char) ch);
			}
		}

		// we saw some non-ascii character
		if (i < len) {
			// get UTF-8 bytes for the remaining sub-string
			byte[] bytes = null;
			byte b;
			try {
				bytes = anyURI.substring(i).getBytes("UTF-8");
			} catch (java.io.UnsupportedEncodingException e) {
				// should never happen
				return anyURI;
			}
			len = bytes.length;

			// for each byte
			for (i = 0; i < len; i++) {
				b = bytes[i];
				// for non-ascii character: make it positive, then escape
				if (b < 0) {
					ch = b + 256;
					buffer.append('%');
					buffer.append(gHexChs[ch >> 4]);
					buffer.append(gHexChs[ch & 0xf]);
				} else if (gNeedEscaping[b]) {
					buffer.append('%');
					buffer.append(gAfterEscaping1[b]);
					buffer.append(gAfterEscaping2[b]);
				} else {
					buffer.append((char) b);
				}
			}
		}

		// If encoding happened, create a new string;
		// otherwise, return the orginal one.
		if (buffer.length() != len) {
			return buffer.toString();
		} else {
			return anyURI;
		}
	}
}
