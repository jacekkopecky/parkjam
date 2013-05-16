/******************************************************************
 * File:        RuleDerivation.java
 * Created by:  Dave Reynolds
 * Created on:  06-Apr-03
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: RuleDerivation.java,v 1.1 2009/06/29 08:55:38 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.reasoner.rulesys;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.util.PrintUtil;
import java.io.PrintWriter;
import java.util.*;

/**
 * Derivation records are used to determine how an inferred triple
 * was derived from a set of source triples and a reasoner. SubClasses
 * provide more specific information.
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:38 $
 */
public class RuleDerivation implements Derivation {
    
    /** The rule which asserted this triple */
    protected Rule rule;
    
    /** The triple which was asserted */
    protected Triple conclusion;
    
    /** The list of triple matches that fired the rule */
    protected List<Triple> matches;

    /** The InfGraph which produced this derivation */
    protected InfGraph infGraph;
    
    /**
     * Constructor
     * @param rule the rule which created this derivation
     * @param conclusion the triple which the rule created
     * @param matches a list of matching Triples corresponding to the rule body patterns
     * @param infGraph the parent infGraph which was controlling the derivation
     */
    public RuleDerivation(Rule rule, Triple conclusion, List<Triple> matches, InfGraph infGraph) {
        this.rule = rule;
        this.conclusion = conclusion;
        this.matches = matches;
        this.infGraph = infGraph;
    }
    
    /**
     * Return a short-form description of this derivation.
     */
    @Override
    public String toString() {
        if (rule == null) {
            return "DUMMY";
        } else {
            return "Rule " + rule.toShortString();
        }
    }
    
    /**
     * Print a deep traceback of this derivation back to axioms and 
     * source assertions.
     * @param out the stream to print the trace out to
     * @param bindings set to true to print intermediate variable bindings for
     * each stage in the derivation
     */
    public void printTrace(PrintWriter out, boolean bindings) {
       printTrace(out, bindings, 0, new HashSet<RuleDerivation>());
    }
    
     /**
     * Print a deep traceback of this derivation back to axioms and 
     * source assertions.
     * @param out the stream to print the trace out to
     * @param bindings set to true to print intermediate variable bindings for
     * each stage in the derivation
     * @param indent the number of indent spaces to use
     * @param seen a HashSet of derviations that have already been listed
     */
    protected void printTrace(PrintWriter out, boolean bindings, int indent, HashSet<RuleDerivation> seen) {
        PrintUtil.printIndent(out, indent);
        out.print(this.toString());
        if (bindings) {
            out.print(" concluded " + PrintUtil.print(conclusion));
        }
        out.println(" <-");
        int margin = indent + 4;
        for (int i = 0; i < matches.size(); i++) {
            Triple match = matches.get(i);
            Iterator<Derivation> derivations = infGraph.getDerivation(match);
            if (derivations == null || !derivations.hasNext()) {
                PrintUtil.printIndent(out, margin);
                if (match == null) {
                    // A primitive
                    ClauseEntry term = rule.getBodyElement(i);
                    if (term instanceof Functor) {
                        out.println(((Functor)term).getName() + "()");
                    } else {
                        out.println("call to built in");
                    }
                } else {
                    out.println("Fact " + PrintUtil.print(match));
                }
            } else {
                RuleDerivation derivation = (RuleDerivation)derivations.next();
                if (seen.contains(derivation)) {
                    PrintUtil.printIndent(out, margin);
                    out.println("Known " + PrintUtil.print(match) + " - already shown");
                } else {
                    seen.add(derivation);
                    derivation.printTrace(out, bindings, margin, seen);
                }
            }
        }
    }
    

    /**
     * @return the triple concluded by the derivation
     */
    public Triple getConclusion() {
        return conclusion;
    }

    /**
     * @return the set of triples which were used in firing this rule derivation
     */
    public List<Triple> getMatches() {
        return matches;
    }

    /**
     * @return the rule which fired to create this derivation
     */
    public Rule getRule() {
        return rule;
    }

    /**
     * Compare two derivations. This is a shallow comparison, two derivations 
     * are the same if they contain the same conclusion, rule and match list. 
     * They do not need to be derived from the same (or any) infGraph.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof RuleDerivation) {
            RuleDerivation otherD = (RuleDerivation)other;
            return conclusion.equals(otherD.getConclusion()) &&
                    matches.equals(otherD.getMatches()) &&
                    rule.equals(otherD.getRule());
        } else {
            return false;
        }
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
