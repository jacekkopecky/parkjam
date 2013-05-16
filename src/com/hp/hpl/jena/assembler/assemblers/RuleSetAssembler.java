/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: RuleSetAssembler.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.assembler.assemblers;

import java.util.*;

import com.hp.hpl.jena.assembler.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

public class RuleSetAssembler extends AssemblerBase implements Assembler
    {
    @Override public Object open( Assembler a, Resource root, Mode irrelevant )
        { 
        checkType( root, JA.RuleSet );
        return createRuleSet( a, root ); 
        }

    public static RuleSet createRuleSet( Assembler a, Resource root )
        { return RuleSet.create( addRules( new ArrayList<Rule>(), a, root ) ); }

    public static List<Rule> addRules( List<Rule> result, Assembler a, Resource root )
        {
        addLiteralRules( root, result );
        addIndirectRules( a, root, result );
        addExternalRules( root, result );
        return result;
        }

    static private void addIndirectRules( Assembler a, Resource root, List<Rule> result )
        {
        StmtIterator it = root.listProperties( JA.rules );
        while (it.hasNext()) 
            {
            Resource r = getResource( it.nextStatement() );
            result.addAll( ((RuleSet) a.open( r )).getRules() );
            }
        }

    static private void addExternalRules( Resource root, List<Rule> result )
        {
        StmtIterator it = root.listProperties( JA.rulesFrom );
        while (it.hasNext())
            {
            Resource s = getResource( it.nextStatement() );
            result.addAll( Rule.rulesFromURL( s.getURI() ) );
            }
        }

    static private void addLiteralRules( Resource root, List<Rule> result )
        {
        StmtIterator it = root.listProperties( JA.rule );
        while (it.hasNext())
            {
            String s = getString( it.nextStatement() );
            result.addAll( Rule.parseRules( s ) );
            }
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