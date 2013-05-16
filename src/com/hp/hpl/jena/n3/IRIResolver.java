/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.n3;


//import com.hp.hpl.jena.iri.IRI;
//import com.hp.hpl.jena.iri.IRIException;
//import com.hp.hpl.jena.iri.IRIFactory;
import java.net.URI;

import com.hp.hpl.jena.util.FileUtils;

/** A simple class to access IRI resolution 
 * 
 * @author Andy Seaborne, Jeremy Carroll
 * */

public class IRIResolver {
	/**
	 * The current working directory, as a string.
	 */
	static private String globalBase = "http://localhost/LocalHostBase/" ;
	
	// Try to set the global base from the current directory.  
	// Security (e.g. Tomcat) may prevent this in which case we
	// use a common default set above.
	static {
	    try { globalBase = FileUtils.toURL("."); }
	    catch (Throwable th) {  }
	}
	    
	/**
	 * The current working directory, as an IRI.
	 */
	static final URI cwd;

//	/**
//	 * An IRIFactory appropriately configuired.
//	 */
//	static final IRIFactory factory = new IRIFactory(IRIFactory
//			.jenaImplementation());
//	static {
//		factory.setSameSchemeRelativeReferences("file");
//	}

	static {
		
//		IRI cwdx;
//		try {
//			cwdx = factory.construct(globalBase);
//		} catch (IRIException e) {
//			System.err.println("Unexpected IRIException in initializer: "
//					+ e.getMessage());
//			cwdx = factory.create("file:///");
//		}
//		cwd = cwdx;
	    cwd = URI.create("file:///");
	}


	
	/**
	 * Turn a filename into a well-formed file: URL relative to the working
	 * directory.
	 * 
	 * @param filename
	 * @return String The filename as an absolute URL
	 */
	static public String resolveFileURL(String filename) {
		URI r = cwd.resolve(filename);
		if (!r.getScheme().equalsIgnoreCase("file")) {
			return resolveFileURL("./" + filename);
		}
		return r.toString();
	}

	/**
	 * Create resolve a URI against a base. If baseStr is a relative file IRI
	 * then it is first resolved against the current working directory.
	 * 
	 * @param relStr
	 * @param baseStr
	 *            Can be null if relStr is absolute
	 * @return String An absolute URI
	 * @throws JenaURIException
	 *             If result would not be legal, absolute IRI
	 */
	static public String resolve(String relStr, String baseStr)
			throws JenaURIException {
		return exceptions(resolveIRI(relStr, baseStr)).toString();
	}

	/*
	 * No exception thrown by this method.
	 */
	static private URI resolveIRI(String relStr, String baseStr) {
		URI i = URI.create(relStr);
		if (i.isAbsolute())
			// removes excess . segments
			return cwd.resolve(i);

		URI base = URI.create(baseStr);

		if ("file".equalsIgnoreCase(base.getScheme()))
			return cwd.resolve(base).resolve(i);
		return base.resolve(i);
	}

	final private URI base;

	/**
	 * Construct an IRIResolver with base as the 
	 * current working directory.
	 *
	 */
	public IRIResolver() {
		this(null);
	}

	/**
	 * Construct an IRIResolver with base determined
	 * by the argument URI. If this is relative,
	 * it is relative against the current working directory.
	 * @param baseS
	 * 
	 * @throws JenaURIException
	 *             If resulting base would not be legal, absolute IRI
	 */
	public IRIResolver(String baseS) {
		if (baseS == null)
			baseS = chooseBaseURI();
		// IRI aaa = RelURI.factory.construct(baseS);
		base = exceptions(cwd.resolve(baseS));
	}

	/**
	 * The base of this IRIResolver.
	 * @return
	 */
	public String getBaseIRI() {
		return base.toString();
	}

	/**
	 * Resolve the relative URI against the base of
	 * this IRIResolver.
	 * @param relURI
	 * @return the resolved IRI
	 * @throws JenaURIException
	 *             If resulting URI would not be legal, absolute IRI
	
	 */
	public String resolve(String relURI) {
		return exceptions(base.resolve(relURI)).toString();
	}

	
	/**
	 * Throw any exceptions resulting from IRI.
	 * @param iri
	 * @return iri
	 */
	static private URI exceptions(URI iri) {
//		if (showExceptions && iri.hasViolation(false)) {
//			try {
//				cwd.construct(iri);
//			} catch (IRIException e) {
//				throw new JenaURIException(e);
//			}
//		}
		return iri;
	}
	
	private static boolean showExceptions = true;

	/**
	    To allow Eyeball to bypass IRI checking (because it's doing its own)
	    @author kers
	*/
	public static void suppressExceptions()
	    { showExceptions = false; }
	
	/**
	 * Resolve the relative URI str against the current
	 * working directory.
	 * @param str
	 * @return
	 */
	public static String resolveGlobal(String str) {
		return exceptions(cwd.resolve(str)).toString();
	}

	/**
	 * Choose a base URI based on the current directory
	 * 
	 * @return String Absolute URI
	 */

	static public String chooseBaseURI() {
		return chooseBaseURI(null);
	}

	/**
	 * Choose a baseURI based on a suggestion
	 * 
	 * @return String URI (if relative, relative to current working directory).
	 */

	static public String chooseBaseURI(String baseURI) {
		if (baseURI == null)
			baseURI = "file:.";
		return resolveGlobal(baseURI);
	}

}

/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
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