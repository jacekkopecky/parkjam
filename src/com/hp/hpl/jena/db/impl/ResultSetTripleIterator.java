/*
 *  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 *
 */

//=======================================================================
// Package
package com.hp.hpl.jena.db.impl;

//=======================================================================
// Imports
import java.sql.*;

import com.hp.hpl.jena.db.RDFRDBException;
import com.hp.hpl.jena.graph.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//=======================================================================
/**
* Version of ResultSetIterator that extracts database rows as Triples.
*
* @author hkuno.  Based on ResultSetResource Iterator, by Dave Reynolds, HPLabs, Bristol <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
* @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:37 $
*/
public class ResultSetTripleIterator extends ResultSetIterator<Triple> {

    /** The rdf model in which to instantiate any resources */
    protected IDBID m_graphID;

    /** The database driver, used to access namespace and resource caches */
    protected IPSet m_pset;
    
    /** Holds the current row as a triple */
    protected Triple m_triple;
    
    /** True if iterating over reified statements */
    protected boolean m_isReif;
    
    /** Statement URI if iterating over reified statements */
    protected Node m_stmtURI;
    
    /** HasType flag if iterating over reified statements */
    protected boolean m_hasType;

    static private Logger logger = LoggerFactory.getLogger( ResultSetTripleIterator.class );
    
	// Constructor
	public ResultSetTripleIterator(IPSet p, IDBID graphID) {
		m_pset = p;
		setGraphID(graphID);
		m_isReif = false;
	}

	// Constructor for iterating over reified statements
	public ResultSetTripleIterator(IPSet p, boolean isReif, IDBID graphID) {
		m_pset = p;
		setGraphID(graphID);
		m_isReif = isReif;
	}
	
	/**
	 * Set m_graphID.
	 * @param gid is the id of the graph associated with this iterator.
	 */
	public void setGraphID(IDBID gid) {
		m_graphID = gid;
	}
	
	/**
	 * Reset an existing iterator to scan a new result set.
	 * @param resultSet the result set being iterated over
	 * @param sourceStatement The source Statement to be cleaned up when the iterator finishes - return it to cache or close it if no cache
	 * @param cache The originating SQLcache to return the statement to, can be null
	 * @param opname The name of the original operation that lead to this statement, can be null if SQLCache is null
	 */
	@Override
    public void reset(ResultSet resultSet, PreparedStatement sourceStatement, SQLCache cache, String opname) {
		super.reset(resultSet, sourceStatement, cache, opname);
		m_triple = null;
	}

    /**
     * Extract the current row into a triple. 
     * Requires the row to be of the form:
     *   subject URI (String)
     *   predicate URI (String)
     *   object URI (String)
     *   object value (String)
     *   Object literal id (Object)
     * 
     * The object of the triple can be either a URI, a simple literal (in 
     * which case it will just have an object value, or a complex literal 
     * (in which case both the object value and the object literal id 
     * columns may be populated.
     */
    @Override
    protected void extractRow() throws SQLException {
        int rx = 1;
        ResultSet rs = m_resultSet;
        String subj = rs.getString(1);
		String pred = rs.getString(2);
		String obj = rs.getString(3);

		if ( m_isReif ) {
			m_stmtURI = m_pset.driver().RDBStringToNode(rs.getString(4));
			m_hasType = rs.getString(5).equals("T");
		}
		
		Triple t = null;
		
		try {
        t = m_pset.extractTripleFromRowData(subj, pred, obj);
		} catch (RDFRDBException e) {
			logger.debug("Extracting triple from row encountered exception: ", e);
		}
		
		m_triple = t;
		
	}
	
		/**
		 * Return the current row, which should have already been extracted.
		 */
		@Override
        protected Triple getRow() {
			return m_triple;
		}
		
		/**
	 	* Return the current row, which should have already been extracted.
	 	*/
		protected Node getStmtURI() {
			return m_stmtURI;
		}
		
		/**
		* Return the current row, which should have already been extracted.
		*/
		protected boolean getHasType() {
			return m_hasType;
		}
		
		/**
	 	* Delete the current row, which should have already been extracted.
	 	* Should only be used (carefully and) internally by db layer.
	 	*/
		protected void deleteRow() {
			try {
				m_resultSet.deleteRow();
			} catch (SQLException e) {
				throw new RDFRDBException("Internal sql error", e);
			}
		}
		
	
		/** 
		 * Remove the current triple from the data store.
		 */
		@Override
        public void remove() {
			if (m_triple == null)
				  throw new IllegalStateException();
			m_pset.deleteTriple(m_triple, m_graphID);
		}

} // End class

/*
 *  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
 */

