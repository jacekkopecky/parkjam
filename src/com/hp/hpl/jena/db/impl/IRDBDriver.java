/*
 *  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *  [see end of file]
 */

package com.hp.hpl.jena.db.impl;

//=======================================================================

// Imports

import java.util.List;

import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.db.RDFRDBException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;


/**
* Generic database interface used for implementing RDF Stores.
* Different database table layouts and different SQL dialects should all
* be supportable via this generic interface. 
* 
* In earlier versions of Jena the Driver was exposed to some applications -
* that is no longer the case, and no application should need to use these
* functions directly.
* 
* Based in part on the Jena 1.0 implementation by der.
* 
* @author csayers
* @version $Revision: 1.1 $
*/

public interface IRDBDriver {
	
	/**
	 * Set the database connection
	 */
	public void setConnection( IDBConnection dbcon );
	
	/**
	 * Return the connection
	 */
	public IDBConnection getConnection();
	
	/**
	 * Return the specialized graph containing system properties.
	 * Formats the database and constucts a new one if doInit is true.
	 * @param doInit if true, format the database if needed.
	 * 
	 * @return SpecializedGraph holding properties of this database
	 * @since Jena 2.0
	 */
	public SpecializedGraph getSystemSpecializedGraph(boolean doInit);
	
	/**
	 * Construct and return a list of specialized graphs.
	 * @param graphName TODO
	 * @param requestedProperties TODO
	 * @return List of SpecializedGraphs to store a Graph
	 * 
	 * @since Jena 2.0
	 */
	public List<SpecializedGraph> createSpecializedGraphs(String graphName, Graph requestedProperties);
	
	/**
	 * Reconstruct and return a list of specialized graphs.
	 * @param graphProperties A set of customization properties for the  graph.
	 * @return List of SpecializedGraphs to store a Graph
	 * 
	 * @since Jena 2.0
	 */
	public List<SpecializedGraph> recreateSpecializedGraphs(DBPropGraph graphProperties);
	
	/**
	 * Remove the specialized graph, erasing all trace of a Graph.
	 * @param graphProperties The properties for the graph to be removed.
	 * 
	 * @since Jena 2.0
	 */
	public void removeSpecializedGraphs(DBPropGraph graphProperties, List<SpecializedGraph> specializedGraphs);
	
	/**
	 * Test if the database has previously been formatted (there's no other
	 * way to easily tell, since getSpecializedGraph will always return
	 * something).
	 * 
	 * @return boolean true if database is correctly formatted, false on any error.
	 */
	public boolean isDBFormatOK();
	
	/**
	 * Method setDatabaseProperties.
	 * 
	 * Sets the current properties for the database.
	 * 
	 * @param databaseProperties is a Graph containing a full set of database properties
	 */
	void setDatabaseProperties(Graph databaseProperties);
		
	/**
	 * Obtain a default set of model properties.
	 * 
	 * Return the default properties for a new model stored in this database
	 * @return DBPropGraph containg the default properties for a new model
	 */
	DBPropGraph getDefaultModelProperties();
	
	/**
	 * Return a string identifying underlying database type.
	 *
	 */
	String getDatabaseType();
	

    /**
     * Remove all RDF information from a database.
     * 
     * There should be no need for an application to call this function
     * directly - instead use DBConnection.cleanDB().
     * 
     */

    public void cleanDB();

    /**
     * Close the driver (does not close the DB connection)
     * @throws RDFDBException if there is an access problem
     */

    public void close() throws RDFRDBException;

    /**
     * Acquire the mutex lock in the database. This
     * is used to implement critical sections to prevent
     * concurrent Jena threads from inteferring with
     * each other when creating/destroying models,
     * formatting databases, etc.
     * 
     * There should be no need for an application to
     * call this method.
     */
    
    public void lockDB() throws RDFRDBException;
    
    /**
     * Release the mutex lock in the database.
     */
    
    public void unlockDB() throws RDFRDBException;
    
    /**
     * Return true if the database is locked, else false.
     */
    
    public boolean tryLockDB() throws RDFRDBException;
    
	/**
	 * Create a table for storing asserted or reified statements.
	 * 
	 * @param graphId the graph which the table is created.
	 * @param isReif true if table stores reified statements.
	 * @return the name of the new table 
	 * 
	 */
    
    /**
     *  Return true if the mutex is held.
     */
    
    public boolean DBisLocked() throws RDFRDBException;

	abstract String createTable( int graphId, boolean isReif);
	   
	/**
	 * Delete a table. 
	 * For internal use only.
	 * 
	 */
	abstract void deleteTable( String tableName );
	   

	/**
     * Aborts the open transaction, then turns autocommit on.
     */
	public void abort() throws  RDFRDBException;
        
	/**
	 * Turns autocommit off, then opens a new transaction.	 *
	 */
	public void begin() throws  RDFRDBException;
        
	/**
	 * Commits the open transaction, then turns autocommit on.
	 */
	public void commit() throws  RDFRDBException;

	/**
	 * Returns true if the underlying database supports transactions.
	 */
	public boolean transactionsSupported();    

	/**
	 * Allocate an identifier for a new graph.
	 * @param graphName The name of a new graph.
	 * @return the identifier of the new graph.
	 */
	 public int graphIdAlloc ( String graphName );
	
	/**
	 * Deallocate an identifier for a new graph.
	 * @param graphId The graph identifier.
	 */
	 public void graphIdDealloc ( int graphId );

	/**
 	* Return an auto-generated identifier for a table row.
 	* @param tableName The name of the table for the insert.
 	* @return the auto-generated identifier..
 	*/
	 public int getInsertID ( String tableName );

    
	/**
	* Convert a node to a string to be stored in a statement table.
	* @param Node The node to convert to a string. Must be a concrete node.
	* @param addIfLong If the node is a long object and is not in the database, add it.
	* @return the string.
	*/

	public String nodeToRDBString ( Node node, boolean addIfLong );
	
	/**
	* Convert an RDB string to the node that it encodes. Return null if failure.
	* @param RDBstring The string to convert to a node.
	* @return The node.
	*/
	
	public Node RDBStringToNode ( String RDBString );
	
	/**
	 * Generate an SQL string for a reified statement to match on the stmt URI. 
	 * @return qualifier string
	 */	
	
	public String genSQLReifQualStmt ();
	
	/**
	 * Generate an SQL string for a reified statement to match on any subject,
	 * predicate or object column.
	 * @param objIsStmt If true, the object value is rdf:Statement so also match
	 * on the hasType column. 
	 * @return qualifier string
	 */	

	public String genSQLReifQualAnyObj( boolean objIsStmt);
	
	/**
	 * Generate an SQL string for a reified statement to match on a property column.
	 * @param reifProp The property column to match, one of S,P,O,T for subject,
	 * predicate, object or type, respectively.
	 * @param hasObj If true, the object value is known so do equality match. Otherwise,
	 * just check for non-null value.
	 * @return qualifier string
	 */	
	
	public String genSQLReifQualObj ( char reifProp, boolean hasObj );
	
	/**
	 * Generate an SQL string to match a table column value to a constant.
	 * If the literal does not occur in the database, a match condition
	 * is generated that will always return false.
	 * There's a known bug in this method. the literal is converted to
	 * a string BEFORE the query is run. consequently, there's a race
	 * condition. if the (long) literal is not in the database
	 * when the query is compiled but is added prior to running the
	 * query, then the query will (incorrectly) return no results.
	 * for now, we'll ignore this case and document it as a bug.
	 * @param alias The table alias for this match.
	 * @param col The column to match, one of S,P,O,N,T for subject,
	 * predicate, object, statement or type, respectively.
	 * @param lit The literal value to match.
	 * @return SQL string.
	 */	
	
	public String genSQLQualConst ( int alias, char col, Node lit );
	
	/**
	 * Similar to genSQLQualConst except that it generates SQL strings
	 * for the reification statement table.
	 *
	 */
	public String genSQLReifQualConst ( int alias, char pred, Node lit );

	
	/**
	 * Generate an SQL string to match a table column value to a parameter.
	 * @param alias The table alias for this match.
	 * @param col The column to match, one of S,P,O,N,T for subject,
	 * predicate, object, statement or type, respectively.
	 * @return SQL string.
	 */	

	public String genSQLQualParam( int alias, char col );
	
	/**
	 * Generate an SQL string to match a graph id. 
	 * @param alias The table alias for this match.
	 * @param graphId The identifer of the graph to match.
	 * @return SQL string.
	 */	

	public String genSQLQualGraphId( int alias, int graphId );
	
	public String genSQLStringMatch( int alias, char col,
		String fun, String stringToMatch );
	
	public String genSQLStringMatchLHS( boolean ignCase, String var );
	public String genSQLStringMatchLHS_IC( String var );
	
	public String genSQLStringMatchOp( boolean ignCase, String fun );

	public String stringMatchAllChar();
	public String stringMatchEscapeChar();

	public String genSQLStringMatchRHS( boolean ignCase, boolean pfxMatch,
									String strToMatch );
	public String genSQLStringMatchRHS_IC( String strToMatch );

	public String genSQLStringMatchOp( String fun );

	public String genSQLStringMatchOp_IC( String fun );
	
	public boolean stringMatchNeedsEscape ( String strToMatch );

	public String addEscape ( String strToMatch );
	
	public String genSQLStringMatchEscape();
	
	public String genSQLLikeKW();

	public String genSQLEscapeKW();

	
	
	
	/**
	 * Generate an SQL string to joing two table columns.
	 * @param lhsAlias The left side table alias for the join.
	 * @param lhsCol The left side column to join, one of
	 * S,P,O,N,T.
	 * @param rhsAlias The right side table alias to join.
	 * @param rhsCol The right side column to join.
	 * @return SQL string.
	 */	

	public String genSQLJoin( int lhsAlias, char lhsCol,
		int rhsAlias, char rhsCol );
		
	/**
	 * Generate an SQL string for a result list of a select stmt.
	 * @param binding Array of Var containing the result list bindings.
	 * @return SQL string (not prefixed by "Select").
	 */	
	
	public String genSQLResList( int resIndex[], VarDesc[] binding );

	/**
	 * Generate an SQL string for a from list of a select stmt.
	 * @param aliasCnt The number of table aliases in the from list.
	 * @param table The name of the table to be queried.
	 * @return SQL string (not prefixed by "From").
	 */	
	
	public String genSQLFromList( int aliasCnt, String table );

		
	/**
	 * Generate an SQL Select statement given the result list,
	 * the from list and the where clause;
	 * @param res The result list as a string.
	 * @param from The from list as a string.
	 * @param where The where qualifier as a string.
	 * @return SQL statement.
	 */	

	public String genSQLSelectStmt( String res, String from, String where );
	
	public class GenSQLAnd {
		 private boolean init;
		 GenSQLAnd () { init = false; }
		 String gen( String qual ) {
			if ( qual == "" ) return "";
			if ( init == false ) {
				init = true;
				return qual;
			} else
				return " AND " + qual;
		 }  	 
	}
	
	/**
	 * Get the maximum possible value of LongObjectLength
	 * @return int
	 */
	public int getLongObjectLengthMax();

	/**
	 * Get the value of LongObjectLength
	 * @return int
	 */
	public int getLongObjectLength();

	/**
	* Set the value of LongObjectLength. Throws an exception if
	* the database has been initialized.
	* @param int
	*/
	public void setLongObjectLength(int len);

	/**
	 * Get the maximum possible value of IndexKeyLength
	 * @return int
	 */
	public int getIndexKeyLengthMax();

	/**
	 * Get the value of IndexKeyLength
	 * @return int
	 */
	public int getIndexKeyLength();

	/**
	* Set the value of IndexKeyLength. Throws an exception if
	* the database has been initialized.
	* @param int
	*/
	public void setIndexKeyLength(int len);

	/**
	* Get the value of IsTransactionDb
	* @return bool
	*/
	public boolean getIsTransactionDb();

	/**
	* Set the value of IsTransactionDb. Throws an exception if
	* the database has been initialized.
	* @param bool
	*/
	public void setIsTransactionDb(boolean bool);

	/**
	* Get the value of DoCompressURI
	* @return bool
	*/
	public boolean getDoCompressURI();

	/**
	* Set the value of DoCompressURI. Throws an exception if
	* the database has been initialized.
	* @param bool
	*/
	public void setDoCompressURI(boolean bool);

	/**
		* Get the value of CompressURILength
		* @return int
		*/
	public int getCompressURILength();

	/**
	* Set the value of CompressURILength. Throws an exception if
	* the database has been initialized.
	* @param int
	*/
	public void setCompressURILength(int len);

	/**
	* Get the value of DoDuplicateCheck
	* @return bool
	*/
	public boolean getDoDuplicateCheck();

	/**
	* Set the value of DoDuplicateCheck.
	* @param bool
	*/
	public void setDoDuplicateCheck(boolean bool);

	/**
	* Get the value of TableNamePrefix
	* @return String
	*/
	public String getTableNamePrefix();

	/**
	* Set the value of TableNamePrefix.
	* @param String
	*/
	public void setTableNamePrefix(String prefix);
	
	/**
	* Get the value of StoreWithModel
	* @return String
	*/
	public String getStoreWithModel();

	/**
	* Set the value of StoreWithModel.
	* @param String
	*/
	public void setStoreWithModel( String modelName );
	
	/**
	* Get the value of CompressCacheSize
	* @return int
	*/
	public int getCompressCacheSize();

	/**
	* Set the value of CompressCacheSize.
	* @param int
	*/
	public void setCompressCacheSize(int count);
	
	/**
	 * Return the number of system tables.
	 */

	public int getSystemTableCount();
	
	/**
	 * Return the name of a system table
	 */

	public String getSystemTableName ( int i );

}

/*
 *  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
	
 
