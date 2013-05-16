/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: RDBModelAssembler.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;

import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.db.*;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.*;

public class RDBModelAssembler extends NamedModelAssembler implements Assembler
    {
    @Override
    protected Model openEmptyModel( Assembler a, Resource root, Mode mode )
        { return openModel( a, root, Content.empty, mode ); }    
    
    @Override
    protected Model openModel( Assembler a, Resource root, Content initial, Mode mode )
        {
        checkType( root, JA.RDBModel );
        String name = getModelName( root );
        ReificationStyle style = getReificationStyle( root );
        ConnectionDescription c = getConnection( a, root );
        Model m = openModel( root, c, name, style, initial, mode );
//        if (!initial.isEmpty()) addContent( root, m, initial );
        return m;
        }

    protected ConnectionDescription getConnection( Assembler a, Resource root )
        {
        Resource C = getRequiredResource( root, JA.connection );
        return (ConnectionDescription) a.open( C );        
        }

    protected Model openModel( Resource root, ConnectionDescription c, String name, ReificationStyle style, Content initial, Mode mode )
        {
        IDBConnection ic = c.getConnection();
        return isDefaultName( name )
            ? ic.containsDefaultModel() ? ModelRDB.open( ic ) : ModelRDB.createModel( ic )
            : openByMode( root, initial, name, mode, style, ic );
        }

    private Model openByMode( Resource root, Content initial, String name, Mode mode, ReificationStyle style, IDBConnection ic )
        {
        if (ic.containsModel( name ))
            {
            if (mode.permitUseExisting( root, name )) return consModel( ic, name, style, false );
            throw new AlreadyExistsException( name );
            }
        else
            {
            if (mode.permitCreateNew( root, name )) return initial.fill( consModel( ic, name, style, true ) );
            throw new NotFoundException( name );
            }
        }
    
    private static final String nameForDefault = "DEFAULT";

    private boolean isDefaultName( String name )
        { return name.equals( nameForDefault ) || name.equals( "" ); }
    
    protected Model consModel( IDBConnection c, String name, ReificationStyle style, boolean fresh )
        { return new ModelRDB( consGraph( c, name, style, fresh ) ); }
    
    protected GraphRDB consGraph( IDBConnection c, String name, ReificationStyle style, boolean fresh )
        {        
        Graph p = c.getDefaultModelProperties().getGraph();
        int reificationStyle = GraphRDB.styleRDB( style );
        return new GraphRDB( c, name, (fresh ? p : null), reificationStyle, fresh );
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