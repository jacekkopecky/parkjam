/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: TransactionHandler.java,v 1.1 2009/06/29 08:55:45 castagna Exp $
*/

package com.hp.hpl.jena.graph;

import com.hp.hpl.jena.shared.*;

/**
    Preliminary interface for graphs supporting transactions.
    
 	@author kers
*/
public interface TransactionHandler
    {
    /**
        Does this handler support transactions at all?
        
        @return true iff begin/abort/commit are implemented and make sense.
    */
    boolean transactionsSupported();
    
    /**
        If transactions are supported, begin a new transaction. If tranactions are
        not supported, or they are but this tranaction is nested and nested transactions
        are not supported, throw an UnsupportedOperationException.
    */
    void begin();
    
    /**
        If transactions are supported and there is a tranaction in progress, abort
        it. If transactions are not supported, or there is no transaction in progress,
        throw an UnsupportedOperationException.
    */
    void abort();
    
    /**
        If transactions are supported and there is a tranaction in progress, commit
        it. If transactions are not supported, , or there is no transaction in progress,
        throw an UnsupportedOperationException.
   */
    void commit();
    
    /**
        If transactions are supported, execute the command c within a transaction
        and return its result. If not, throw an UnsupportedOperationException.
    */
    Object executeInTransaction( Command c );
    }


/*
    (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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