/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: Node_Variable.java,v 1.1 2009/06/29 08:55:45 castagna Exp $
*/

package com.hp.hpl.jena.graph;

/**
    "variable" nodes; these are outside the RDF2003 specification, but are
    used internally for "placeholder" nodes where blank nodes would be
    wrong, most specifically in Query.
    @author kers
*/

public class Node_Variable extends Node_Fluid
    {
    /**
         Initialise this Node_Variable with a name object (which should be a
         VariableName object).
    */
    protected Node_Variable( Object name )
        { super( name ); }
    
    /**
        Initialise this Node_Variable from a string <code>name</code>,
        which becomes wrapped in a VariableName.
    */
    public Node_Variable( String name )
        { super( new VariableName( name ) ); }

    @Override
    public String getName()
        { return ((VariableName) label).name; }
    
    @Override
    public Object visitWith( NodeVisitor v )
        { return v.visitVariable( this, getName() ); }
        
    @Override
    public boolean isVariable()
        { return true; }
        
    @Override
    public String toString()
        { return label.toString(); }
    
    @Override
    public boolean equals( Object other )
        {
        if ( this == other ) return true ;
        return other instanceof Node_Variable && label.equals( ((Node_Variable) other).label );
        }
    
    public static Object variable( String name )
        { return new VariableName( name ); }
    
    public static class VariableName
        {
        private String name;
        
        public VariableName( String name ) 
            { this.name = name; }
        
        @Override
        public int hashCode()
            { return name.hashCode(); }
        
        @Override
        public boolean equals( Object other )
            {
            if ( this == other ) return true ;
            return other instanceof VariableName && name.equals( ((VariableName) other).name );
            }
        
        @Override
        public String toString()
            { return "?" + name; }
        }
    }

/*
    (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
