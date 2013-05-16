/*
 	(c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: ConnectionAssembler.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;

import com.hp.hpl.jena.JenaRuntime;
import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.rdf.model.*;

/**
    A ConnectionAssembler assembles a ConnectionDescription object which
    contains a database URL, user name, user password, and database type.
    Some of the components may have been specified in advance when the 
    Assembler was constructed. The ConnectionAssembler will also load any
    classes specified by dbClass[Property] statements of the root.
    
    @author kers
*/
public class ConnectionAssembler extends AssemblerBase implements Assembler
    {
    public final String defaultURL;
    public final String defaultUser;
    public final String defaultPassword;
    public final String defaultType;
    
    protected static final Resource emptyRoot = ModelFactory.createDefaultModel().createResource();
    
    public ConnectionAssembler( Resource init )
        {
        defaultUser = get( init, "dbUser", null );
        defaultPassword = get( init, "dbPassword", null );
        defaultURL = get( init, "dbURL", null );
        defaultType = get( init, "dbType", null );
        }
    
    public ConnectionAssembler()
        { this( emptyRoot ); }

    @Override
    public Object open( Assembler a, Resource root, Mode irrelevant )
        {
        checkType( root, JA.Connection );
        String dbUser = getUser( root ), dbPassword = getPassword( root );
        String dbURL = getURL( root ), dbType = getType( root );
        loadClasses( root );
        return createConnection( root.getURI(), dbURL, dbType, dbUser, dbPassword );
        }    
    
    /**
        Load all the classes that are named by the object of dbClass statements
        of <code>root</code>. Load all the classes named by the contents of
        system properties which are the objects of dbClassProperty statements
        of <code>root</code>.
    */
    private void loadClasses( Resource root )
        {
        for (StmtIterator it = root.listProperties( JA.dbClassProperty ); it.hasNext();)
            {
            String propertyName = getString( it.nextStatement() );
            String className = JenaRuntime.getSystemProperty( propertyName );
            loadClass( root, className );
            }
        for (StmtIterator it = root.listProperties( JA.dbClass ); it.hasNext();)
            {
            String className = getString( it.nextStatement() );
            loadClass( root, className );
            }
        }

    protected ConnectionDescription createConnection
        ( String subject, String dbURL, String dbType, String dbUser, String dbPassword )
        { return ConnectionDescription.create( subject, dbURL, dbUser, dbPassword, dbType ); }
    
    public String getUser( Resource root )
        { return get( root, "dbUser", defaultUser ); }

    public String getPassword( Resource root )
        { return get( root, "dbPassword", defaultPassword ); }

    public String getURL( Resource root )
        { return get( root, "dbURL", defaultURL );  }

    public String getType( Resource root )
        { return get( root, "dbType", defaultType ); }    
    
    protected String get( Resource root, String label, String ifAbsent )
        {
        Property property = JA.property( label );
        RDFNode L = getUnique( root, property );
        return 
            L == null ? getIndirect( root, label, ifAbsent ) 
            : L.isLiteral() ? ((Literal) L).getLexicalForm()
            : ((Resource) L).getURI()
            ;
        }

    private String getIndirect( Resource root, String label, String ifAbsent )
        {
        Property property = JA.property( label + "Property" );
        Literal name = getUniqueLiteral( root, property );
        return name == null ? ifAbsent : JenaRuntime.getSystemProperty( name.getLexicalForm() ); 
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