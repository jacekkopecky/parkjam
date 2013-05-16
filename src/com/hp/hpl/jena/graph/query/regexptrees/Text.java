/*
  (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: Text.java,v 1.1 2009/06/29 08:55:51 castagna Exp $
*/
package com.hp.hpl.jena.graph.query.regexptrees;

/**
    Text - represents literal text for the match, to be taken as it stands. May
    include material that was meta-quoted in the original patterm. There are
    two sub-classes, one for strings and one for single characters; the factory
    methods ensure that there are no single-character TextString instances.
    
    @author kers
*/

public abstract class Text extends RegexpTree
    {
    public static Text create( String s )
        { 
        return s.length() == 1
            ? (Text) new TextChar( s.charAt( 0 ) )
            : new TextString( s ); 
        }
    
    public static Text create( char ch )
        { return new TextChar( ch ); }
    
    static class TextString extends Text
        {
        protected String literal;
        
        TextString( String s ) 
            { literal = s; }
        
        @Override
        public String getString()
            { return literal; }
        
        @Override
        public String toString()
            { return "<text.s '" + literal + "'>"; }
        
        @Override
        public boolean equals( Object x )
            { return x instanceof TextString && literal.equals( ((TextString) x).literal ); }
        
        @Override
        public int hashCode()
            { return literal.hashCode(); }
        }
    
    static class TextChar extends Text
        {
        protected char ch;
        
        TextChar( char ch ) 
            { this.ch = ch; }
        
        @Override
        public String getString()
            { return "" + ch; }
        
        @Override
        public String toString()
            { return "<text.ch '" + ch + "'>"; }
        
        @Override
        public boolean equals( Object x )
            { return x instanceof TextChar && ch == ((TextChar) x).ch; }
        
        @Override
        public int hashCode()
            { return ch; }
        }
    
    @Override
    public abstract boolean equals( Object x );
    
    @Override
    public abstract int hashCode();
    
    public abstract String getString();
    
    }

/*
    (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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