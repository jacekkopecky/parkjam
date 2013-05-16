/*
 	(c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: FileManagerAssembler.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;

import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.*;

/**
    A FileManagerAssembler creates a FileManager object which may be
    initialised with a LocationMapper specified by the object of a ja:locationMapper
    property.
    
    @author kers
*/
public class FileManagerAssembler extends AssemblerBase
    {
    @Override
    public Object open( Assembler a, Resource root, Mode irrelevant )
        { 
        checkType( root, JA.FileManager );
        FileManager fm = new FileManager( getLocationMapper( a, root ) );
        FileManager.setStdLocators( fm );
        return fm; 
        }

    private LocationMapper getLocationMapper( Assembler a, Resource root )
        {
        Resource lm = getUniqueResource( root, JA.locationMapper );
        return lm == null ? new LocationMapper() : (LocationMapper) a.open( lm );
        }
    }


/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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