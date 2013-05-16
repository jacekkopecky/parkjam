/*
 * (c) Copyright 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 */

/** A class to create and recreate UUIDs.
 * @author   Andy Seaborne
 * @version  $Id: JenaUUID.java,v 1.1 2009/06/29 08:55:39 castagna Exp $
 * http://www.opengroup.org/onlinepubs/009629399/apdxa.htm
 */

package com.hp.hpl.jena.shared.uuid;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TO DO
// + Comments and renaming.
// ? Move to/from string code here (string <=> pair of longs).
//   OK but unparse code makes explicit what goes where in the structures
//   parse/unparseV4 is the generic code.

// UUID and factory

public abstract class JenaUUID
{
    static final int HEX = 16 ;
    static final int Var_NCS = 0 ;
    static final int Var_Std = 2 ;      // Same as DCE
    static final int Var_DCE = 2 ;
    static final int Var_MS_GUID = 6 ; 
    static final int Var_Reserved = 7 ;
    
    abstract public int getVersion() ;  
    abstract public int getVariant() ;
    
    abstract public long getMostSignificantBits() ;
    abstract public long getLeastSignificantBits() ;
    
    protected int _getVersion(long mostSigBits, long leastSigBits)
    { 
//        int variant = (int)((UUID_V1_Gen.maskVariant & leastSigBits)>>>62) ;
//        int version = (int)((UUID_V1_Gen.maskVersion & mostSigBits)>>>12) ;
        int version = (int)Bits.unpack(mostSigBits, 12, 16) ;
        return version ;
    }    
    
    protected int _getVariant(long mostSigBits, long leastSigBits)
    { 
        int variant = (int)Bits.unpack(leastSigBits, 62, 64) ;
        return variant ;
    }    
    
    protected JenaUUID()
    {}
    
    /** Format as a string - no URI scheme **/
    public String asString() { return toString() ; }
    /** Format as a URI - that is uuid:ABCD */
    public String asURI()    { return "uuid:"+toString() ; }
    /** Format as a URN - that is urn:uuid:ABCD */
    public String asURN()    { return "urn:uuid:"+toString() ; }

	/** Return a {@link java.util.UUID} for this Jena-generated UUID */  
    public UUID asUUID()
    {
        return new UUID(getMostSignificantBits(), getLeastSignificantBits()) ;
    }

    // ----------------------------------------------------		
	// Factory
    
    static UUIDFactory factory = new UUID_V1_Gen() ;
    public static void setFactory(UUIDFactory factory) { JenaUUID.factory = factory ; } 
    public static UUIDFactory getFactory() { return factory ; } 
    
    /** Create a UUID */
    public static JenaUUID generate()   { return factory.generate() ; }
    public static void reset() { factory.reset() ; } 
    
    /** The nil UUID */
    public static JenaUUID nil()      { return UUID_nil.getNil() ; }
    public static String strNil()     { return UUID_nil.getNilString() ; }
    public boolean isNil()            { return this.equals(nil()) ; } // Or this == UUID_nil.nil because it's a singleton.

    /** Recreate a UUID from string */
    public static JenaUUID parse(String s)
    {
    	if ( s.equals(strNil()) )
            return nil() ;
    	
        // Canonical: this works in conjunction with .equals
        s = s.toLowerCase() ;

        if ( s.startsWith("urn:") )
            s = s.substring(4) ;
        if ( s.startsWith("uuid:") )
            s = s.substring(5) ;

        if ( s.length() != 36 )
            throw new FormatException("UUID string is not 36 chars long: it's "+s.length()+" ["+s+"]") ;

        if ( s.charAt(8)  != '-' || s.charAt(13) != '-' || s.charAt(18) != '-' || s.charAt(23) != '-' )
            throw new FormatException("String does not have dashes in the right places: "+s) ;

		//       00000000-0000-0000-0000-000000000000
		//       ^        ^    ^    ^    ^           
		// Byte: 0        4    6    8    10
		// Char: 0        9    14   19   24  including hyphens
		
        int x = (int)Bits.unpack(s, 19, 23) ;
        int variant = (x>>>14) ;
        int version = (int)Bits.unpack(s, 14, 15) ;
        
        if ( variant == Var_Std )
        {
            switch (version)
            {
                case UUID_V1.version: return  UUID_V1_Gen.parse$(s) ;
                case UUID_V4.version: return  UUID_V4_Gen.parse$(s) ;
            }
            LoggerFactory.getLogger(JenaUUID.class).warn(s+" : Unsupported version: "+version) ;
            throw new UnsupportedOperationException("String specifies unsupported UUID version: "+version) ;
        }
        
        Logger log = LoggerFactory.getLogger(JenaUUID.class) ;
        
        switch (variant)
        {
            case Var_NCS: // NCS
                log.warn(s+" : Oh look! An NCS UUID ID.  Call the museum.") ;
                break ;
            case Var_DCE: // DCE - should have been caught earlier.
                log.warn(s+" : Oh look! A DCE UUID ID - but we should have already handled this") ;
                break ;
            case Var_MS_GUID:
                log.warn(s+" : Microsoft UUID ID.") ;
                break ;
            case Var_Reserved:
                log.warn(s+" : Reserved variant") ;
                break ;
            default:
                log.warn(s+" : Unknown variant: "+variant) ;
                break ;
        }
		throw new UnsupportedOperationException("String specifies unsupported UUID variant: "+variant) ;
    }
    
	// ----------------------------------------------------
	// Worker functions

    static void toHex(StringBuffer sBuff, long value, int lenBytes)
    {
        // Insert in high-low order, by nibble
        for ( int i = 2*lenBytes-1 ; i >= 0 ; i-- )
        {
            int shift = 4*i ;
            int x = (int)(value>>>shift & 0xF) ;
            sBuff.append(Character.forDigit(x, 16)) ;
        }
    }
    
  	static public class FormatException extends RuntimeException
	{
		public FormatException()
		{
			super();
		}

		public FormatException(String msg)
		{
			super(msg);
		}
	}

}

/*
 *  (c) Copyright 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
