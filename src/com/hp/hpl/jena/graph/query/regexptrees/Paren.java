/*
  (c) Copyright 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: Paren.java,v 1.1 2009/06/29 08:55:51 castagna Exp $
*/

package com.hp.hpl.jena.graph.query.regexptrees;


/**
     Class which represents parenthesised regular expressions. Any parenthesised
     expression may have a non-zero label, meaning that it may be referred back
     to by BakReference expressions.
     
     @author hedgehog
*/
public class Paren extends RegexpTree
    {
    protected RegexpTree operand;
    protected int index;

    public Paren( RegexpTree tree ) 
        { this( tree, 0 ); }
    
    public Paren( RegexpTree tree, int index ) 
        { this.operand = tree; this.index = index; }
    
    public RegexpTree getOperand()
        { return operand; }
    
    public int getIndex()
        { return index; }

    @Override
    public boolean equals( Object other )
        { return other instanceof Paren && operand.equals( ((Paren) other).operand ); }

    @Override
    public int hashCode()
        { return operand.hashCode(); }

    @Override
    public String toString()
        { return "(" + operand + ")"; }
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