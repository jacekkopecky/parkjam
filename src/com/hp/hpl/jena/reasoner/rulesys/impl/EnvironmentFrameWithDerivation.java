/******************************************************************
 * File:        EnvironmentFrameWithDerivation.java
 * Created by:  Dave Reynolds
 * Created on:  18-Aug-2003
 * 
 * (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: EnvironmentFrameWithDerivation.java,v 1.1 2009/06/29 08:55:33 castagna Exp $
 *****************************************************************/
package com.hp.hpl.jena.reasoner.rulesys.impl;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.reasoner.TriplePattern;

import java.util.*;

/**
 * Extension of the normal AND-stack environment frame to support
 * incremental derivation logging.
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:33 $
 */
public class EnvironmentFrameWithDerivation extends EnvironmentFrame {

    /** The initial starting arguments for the call */
    Node[] argVars = new Node[RuleClauseCode.MAX_ARGUMENT_VARS];
        
    /** The set of instantiated subgoals processed so far */
    TriplePattern[] matches;
        
    /** 
     * Constructor 
     * @param clause the compiled code being interpreted by this env frame 
     */
    public EnvironmentFrameWithDerivation(RuleClauseCode clause) {
        super(clause);
        if (clause.getRule() != null) {
            matches = new TriplePattern[clause.getRule().bodyLength()];
        }
    }
    
    /** Instantiate and record a matched subgoal */
    public void noteMatch(TriplePattern pattern, int pc) {
        TriplePattern match = pattern;
        int term = clause.termIndex(pc);   
        if (term >= 0) {                                 
            matches[term] = match;
        }
    }

    /**
     * Return the final instantiated goal given the current binding state.
     */
    public Triple getResult() {
        return new Triple(
                    LPInterpreter.deref(argVars[0]),
                    LPInterpreter.deref(argVars[1]), 
                    LPInterpreter.derefPossFunctor(argVars[2]));
    }
    
    /**
     * Return a safe copy of the list of matched subgoals in this subderivation.
     */
    public List<Triple> getMatchList() {
        ArrayList<Triple> matchList = new ArrayList<Triple>();
        for (int i = 0; i < matches.length; i++) {
            matchList.add( LPInterpreter.deref(matches[i]));
        }
        return matchList;
    }
    
    /**
     * Create an initial derivation record for this frame, based on the given
     * argument registers.
     */
    public void initDerivationRecord(Node[] args) {
        System.arraycopy(args, 0, argVars, 0, RuleClauseCode.MAX_ARGUMENT_VARS);
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