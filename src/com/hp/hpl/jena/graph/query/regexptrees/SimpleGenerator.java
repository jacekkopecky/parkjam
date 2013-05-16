/*
    (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
    [See end of file]
    $Id: SimpleGenerator.java,v 1.1 2009/06/29 08:55:51 castagna Exp $
*/

package com.hp.hpl.jena.graph.query.regexptrees;

import java.util.List;

/**
     The base implementation of <code>RegexpTreeGenerator</code>
 	@author hedgehog
*/
public class SimpleGenerator implements RegexpTreeGenerator
    {
    public RegexpTree getAnySingle() { return RegexpTree.ANY; }
    public RegexpTree getStartOfLine() { return RegexpTree.SOL; }
    public RegexpTree getEndOfLine() { return RegexpTree.EOL; }
    public RegexpTree getNothing() { return RegexpTree.NON; }
    
    public RegexpTree getText( char ch ) { return Text.create( ch ); }
    
    public RegexpTree getZeroOrMore( RegexpTree d ) { return new ZeroOrMore( d ); }
    public RegexpTree getOneOrMore( RegexpTree d ) { return new OneOrMore( d ); }
    public RegexpTree getOptional( RegexpTree d ) { return new Optional( d ); }
    
    public RegexpTree getSequence( List<? extends RegexpTree> operands ) { return Sequence.create( operands ); }
    public RegexpTree getAlternatives( List<? extends RegexpTree> operands ) { return Alternatives.create( operands ); }
    
    public RegexpTree getBackReference( int n ) { return new BackReference( n ); }
    
    public RegexpTree getClass( String chars, boolean reject ) 
        { return reject ? (RegexpTree) new NoneOf( chars ) : new AnyOf( chars ); }
    
    public RegexpTree getParen( RegexpTree operand, int index ) 
        { return new Paren( operand, index ); }
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