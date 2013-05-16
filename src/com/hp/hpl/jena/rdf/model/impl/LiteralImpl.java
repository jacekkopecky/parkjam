/*
 *  (c) Copyright 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
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
 *
 * LiteralImpl.java
 *
 * Created on 03 August 2000, 14:42
 */

package com.hp.hpl.jena.rdf.model.impl;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.shared.*;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.enhanced.*;

/** An implementation of Literal.
 *
 * @author  bwm and der
 * @version  Release='$Name:  $' Revision='$Revision: 1.1 $' Date='$Date: 2009/06/29 08:55:32 $'
 */
public class LiteralImpl extends EnhNode implements Literal {
  
    final static public Implementation factory = new Implementation() {
        @Override public boolean canWrap( Node n, EnhGraph eg )
            { return n.isLiteral(); }
            
        @Override public EnhNode wrap(Node n, EnhGraph eg) {
            if (!n.isLiteral()) throw new LiteralRequiredException( n );
            return new LiteralImpl(n,eg);
        }
    };          
          
    public LiteralImpl( Node n, ModelCom m) {
        super( n, m );
    }
    
    public LiteralImpl( Node n, EnhGraph m ) {
        super( n, m );
    }
    
    public Object visitWith( RDFVisitor rv )
        { return rv.visitLiteral( this ); }
        
    /**
        Literals are not in any particular model, and so inModel can return this.
        @param m a model to move the literal into
        @return this
    */
    public RDFNode inModel( Model m )
        { return this; }
    
    @Override public String toString() {
        return asNode().toString( PrefixMapping.Standard, false );
    }
    
    /**
     * Return the value of the literal. In the case of plain literals
     * this will return the literal string. In the case of typed literals
     * it will return a java object representing the value. In the case
     * of typed literals representing a java primitive then the appropriate
     * java wrapper class (Integer etc) will be returned.
     */
    public Object getValue() {
        return asNode().getLiteralValue();
    }
    
    /**
     * Return the datatype of the literal. This will be null in the
     * case of plain literals.
     */
    public RDFDatatype getDatatype() {
        return asNode().getLiteralDatatype();
    }
     
    /**
     * Return the uri of the datatype of the literal. This will be null in the
     * case of plain literals.
     */
    public String getDatatypeURI() {
        return asNode().getLiteralDatatypeURI();
    }
    
    /**
     * Return true if this is a "plain" (i.e. old style, not typed) literal.
     */
    public boolean isPlainLiteral() {
        return asNode().getLiteralDatatype() == null;
    }
    
    /**
     * Return the lexical form of the literal.
     */
    public String getLexicalForm() {
        return asNode().getLiteralLexicalForm();
    }

    public boolean getBoolean()  {
        Object value = asNode().getLiteralValue();
        if (isPlainLiteral()) {
            // old style plain literal - try parsing the string
            if (value.equals("true")) {
                return true;
            } else if (value.equals("false")) {
                return false;
            } else {
                throw new BadBooleanException( value.toString() );
            }
        } else {
            // typed literal
            if (value instanceof Boolean) {
                return ((Boolean)value).booleanValue();
            } else {
                throw new DatatypeFormatException(this.toString() + " is not a Boolean");
            }
        }
    }
    
    public byte getByte()  {
        if (isPlainLiteral()) {
            return Byte.parseByte(getLexicalForm());
        } else {
            return byteValue( asNumber( getValue() ) );
        }
    }

    
    public short getShort()  {
        if (isPlainLiteral()) {
            return Short.parseShort(getLexicalForm());
        } else {
            return shortValue( asNumber( getValue() ) );
        }
    }

    public int getInt()  {
        if (isPlainLiteral()) {
            return Integer.parseInt(getLexicalForm());
        } else {
            return intValue( asNumber( getValue() ) );
        }
    }

    public long getLong()  {
        if (isPlainLiteral()) {
            return Long.parseLong(getLexicalForm());
        } else {
            return asNumber(getValue()).longValue();
        }
    }

    public char getChar()  {
        if (isPlainLiteral()) {
            if (getString().length()==1) {
                return (getString().charAt(0));
            } else {
                throw new BadCharLiteralException( getString() );
            }
        } else {
            Object value = getValue();
            if (value instanceof Character) {
                return ((Character) value).charValue();
            } else {
                throw new DatatypeFormatException(value.toString() + " is not a Character");
            }
        }
    }
    
    public float getFloat()  {
        if (isPlainLiteral()) {
            return Float.parseFloat(getLexicalForm());
        } else {
            return asNumber(getValue()).floatValue();
        }
    }

    public double getDouble()  {
        if (isPlainLiteral()) {
            return Double.parseDouble(getLexicalForm());
        } else {
            return asNumber(getValue()).doubleValue();
        }
    }

    public String getString()  {
        return asNode().getLiteralLexicalForm();
    }
    
//    @Deprecated public Object getObject(ObjectF f)  {
//        if (isPlainLiteral()) {
//            try {
//                return f.createObject(getString());
//            } catch (Exception e) {
//                throw new JenaException(e);
//            }
//        } else {
//            return getValue();
//        }
//    }
    
    public String getLanguage() {
        return asNode().getLiteralLanguage();
    }
    
    public boolean isWellFormedXML() {
        return asNode().getLiteralIsXML();
    } 
   
    /**
     * Test that two literals are semantically equivalent.
     * In some cases this may be the sames as equals, in others
     * equals is stricter. For example, two xsd:int literals with
     * the same value but different language tag are semantically
     * equivalent but distinguished by the java equality function
     * in order to support round tripping.
     */
    public boolean sameValueAs(Literal other) {
        return asNode().sameValueAs(other.asNode());
    }
        
     // Internal helper method to convert a value to number
    private Number asNumber(Object value) {
        if (value instanceof Number) {
            return ((Number)value);
        } else {
            String message = "Error converting typed value to a number. \n";
            message += "Datatype is: " + getDatatypeURI();
            if ( getDatatypeURI() == null || ! getDatatypeURI().startsWith(XSDDatatype.XSD)) {
                message +=" which is not an xsd type.";
            }
            message += " \n";
            String type = 
            message += "Java representation type is " + (value == null ? "null" : value.getClass().toString());
            throw new DatatypeFormatException(message);
        }
    }    
    private byte byteValue( Number n )
        {
        return (byte) getIntegralValueInRange( Byte.MIN_VALUE, n, Byte.MAX_VALUE );
        }

    private short shortValue( Number n )
        {
        return (short) getIntegralValueInRange( Short.MIN_VALUE, n, Short.MAX_VALUE );
        }

    private int intValue( Number n )
        {        
        return (int) getIntegralValueInRange( Integer.MIN_VALUE, n, Integer.MAX_VALUE );
        }

    private long getIntegralValueInRange( long min, Number n, long max )
        {
        long result = n.longValue();
        if (min <= result && result <= max) return result;
        throw new IllegalArgumentException( "byte value required: " + result );
        }
        
}
