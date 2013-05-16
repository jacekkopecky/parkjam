/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: ModelFactoryBase.java,v 1.1 2009/06/29 08:55:38 castagna Exp $
*/

package com.hp.hpl.jena.rdf.model;

import com.hp.hpl.jena.JenaRuntime;
import com.hp.hpl.jena.shared.*;

/**
    Helper functions for ModelFactory - in here to keep from obtruding on the
    end-users.
    
 	@author kers
*/
public class ModelFactoryBase
    {
    public static String guessDBURL()
        { return gp( "db.url" ); }
    
    public static String guessDBUser()
        { return gp( "db.user", "test" ); }
    
    public static String guessDBPassword()
        { return gp( "db.password", "" ); }
    
    public static String guessDBType()
        { 
        String possible = gp( "db.type", null );
        if (possible == null) possible = extractType( guessDBURL() );    
        if (possible == null) throw new JenaException( "cannot guess database type" );
        return possible;
        }
        
    public static String guessDBDriver()
        { return gp( "db.driver", null ); }
    
    /** Return true if the database should support concurrent read during transactions */
    public static boolean guessDBConcurrent() {
        return gp("db.concurrent", "true").equalsIgnoreCase("true");
    }
    
    /**
        Guess the database type as the string between the first and second colons of the
        URL. This method is public so that it may be invoked from test packages.
    
        @param dbURL a string of the form nocolons:somename:somestuff
        @return somename
    */
    public static String extractType( String dbURL )
        {
        int a = dbURL.indexOf( ':' );
        int b = dbURL.indexOf( ':', a + 1 );
        return dbURL.substring( a + 1, b );
        }
    
    protected static String gp( String name )
        {
        String answer = gp( name, null );
        if (answer == null) throw new JenaException( "no binding for " + name );
        return answer;
        }
    
    protected static String gp( String name, String ifAbsent )
        { 
        String answer = JenaRuntime.getSystemProperty( "jena." + name ); 
        return answer == null ? ifAbsent : answer;
        }

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