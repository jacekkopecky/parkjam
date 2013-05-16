/*
 	(c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: AssemblerException.java,v 1.1 2009/06/29 08:55:41 castagna Exp $
*/

package com.hp.hpl.jena.assembler.exceptions;

import java.util.*;

import com.hp.hpl.jena.assembler.assemblers.AssemblerGroup;
import com.hp.hpl.jena.assembler.assemblers.AssemblerGroup.Frame;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
    Assembler Exception class: contains code shared by all the Assembler
    exceptions.
    
    @author kers
*/
public class AssemblerException extends JenaException
    {
    protected final Resource root;
    protected List<Frame> doing = new ArrayList<Frame>();
    
    public AssemblerException( Resource root, String string, Throwable t )
        { 
        super( string, t ); 
        this.root = root;
        }

    public AssemblerException( Resource root, String message )
        {
        super( message );
        this.root = root;
        }

    /**
        Answer the root object whose model-filling was aborted
    */
    public Resource getRoot()
        { return root; }
    
    /**
        XXX 
    */
    public AssemblerException pushDoing( AssemblerGroup.Frame frame )
        { doing.add( frame ); return this; }
    
    /**
         Answer a "nice" representation of <code>r</code>, suitable for appearance
         within an exception message.
    */
    protected static String nice( Resource r )
        {
        String rString = r.asNode().toString( r.getModel() );
        return r.isAnon() ? rString + getLabels( r ) : rString; 
        }
    
    private static String getLabels( Resource r )
        {
        Model m = r.getModel();
        String labels = "", prefix = "labels: ";
        for (StmtIterator it = r.listProperties( RDFS.label ); it.hasNext();)
            {
            String label = it.nextStatement().getObject().asNode().toString( m, true );
            labels += prefix + label;
            prefix = ", ";
            }
        return labels.equals( "" ) ? getIncomingProperty( r ) : " [" + labels + "]";
        }

    private static String getIncomingProperty( Resource r )
        {
        String incomings = "", prefix = "";
        StmtIterator it = r.getModel().listStatements( null, null, r );
        while (it.hasNext())
            {
            Statement s = it.nextStatement();
            incomings += prefix + nice( s.getPredicate() ) + " of " + nice( s.getSubject() );
            prefix = ", ";
            }
        return incomings.equals( "" ) ? "" : " [" + incomings + "]";
        }

    protected static String nice( RDFNode r )
        { return r.isLiteral() ? r.asNode().toString(): nice( (Resource) r ); }

    public List<Frame> getDoing()
        { return doing; }
    
    @Override
    public String toString()
        { 
        String parent = super.toString();
        String frame = frameStrings();
        return frame.equals(  ""  ) ? parent : parent + "\n  doing:\n" + frame; 
        }
    
    protected String frameStrings()
        {
        StringBuffer result = new StringBuffer();
        for (Iterator<Frame> it = doing.iterator(); it.hasNext();)
            result.append( "    " ).append( it.next().toString() ).append( "\n" );
        return result.toString();
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