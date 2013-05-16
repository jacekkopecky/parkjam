/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: Content.java,v 1.1 2009/06/29 08:55:36 castagna Exp $
*/

package com.hp.hpl.jena.assembler;

import java.util.*;

import com.hp.hpl.jena.rdf.model.Model;

/**
    A Content object records content to be used to fill models. This Content
    class contains other Content objects. 
    @author kers
*/
public class Content
    {
    /**
        An empty Content object for your convenience.
    */
    public static final Content empty = new Content();
    
    /**
        The list of component Content objects. 
    */
    protected final List<Content> contents;
    
    /**
        Initialise a content object that includes the contents of each (Content) item
        in the list <code>contents</code>.
    */
    public Content( List<Content> contents )
        { this.contents = contents; }
    
    /**
        Initialise an empty Content object.
    */
    public Content()
        { this( new ArrayList<Content>() ); }

    /**
        Answer the model <code>m</code> after filling it with the contents
        described by this object.
    */
    public Model fill( Model m )
        {
        for (int i = 0; i < contents.size(); i += 1) contents.get(i).fill( m );
        return m; 
        }

    public boolean isEmpty()
        {
        for (int i = 0; i < contents.size(); i += 1) if (!contents.get( i ).isEmpty()) return false;
        return true;
        }
    }


/*
 * (c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
*/