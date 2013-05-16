/*
 *  (c) Copyright 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
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
 * Property.java
 *
 * Created on 26 July 2000, 06:46
 */

package com.hp.hpl.jena.rdf.model;

/** An RDF Property.
 * @author bwm
 * @version Release='$Name:  $' Revision='$Revision: 1.1 $' Date='$Date: 2009/06/29 08:55:38 $'
 */
public interface Property extends Resource {
	
  public boolean isProperty();
    /** Returns the namespace associated with this property.
     * @return The namespace for this property.
     */
  public String getNameSpace();
    /** Returns the name of this property within its namespace.
     * @return The name of this property within its namespace.
     */
  public String getLocalName();
    /** Returns the ordinal value of a containment property.
     *
     * <p>RDF containers use properties of the form _1, _2, _3 etc to represent
     * the containment relationship between the container and the objects it
     * contains.  When invoked on such a containment property, this method
     * returns the integer part of the property name.  When invoked on other
     * properties, it returns 0.
     * @return The ordinal value of a containment property,
     * or 0 otherwise.
     *
     */
  public int    getOrdinal();
}


