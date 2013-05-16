/*
 *  (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 */

//=======================================================================
// Package
package com.hp.hpl.jena.db.impl;

//=======================================================================
// Imports
import java.sql.*;
import java.util.*;
import java.io.*;

import com.hp.hpl.jena.db.*;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.CollectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//=======================================================================
/**
* Stores a set of sql statements loaded from a resource file.
* Caches prepared versions of the statements for a given db connection.
* <p>
* The resource file is located on the classpath and has the format:
* <pre>
* # comment at start of line
* operationName1
* sql code line 1
* ...
* sql code last line
*
* operationName2
* ...
* </pre>
* where the blank lines delimit one sql block from the next.
* <p>The sql code is typically a single SQL statement but some operations,
* specifically database initialization and cleanup may require a variable number
* of statments. To cater for this terminate each statement in those groups with
* the string ";;". Note that a single ";" is not used because these compound
* statements are often stored procedure definitions which end to have ";" line
* terminators!
*
* @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>.  Updated by hkuno to support GraphRDB.
* @version $Revision: 1.1 $ on $Date: 2009/06/29 08:55:37 $
*/

public class SQLCache {

//=======================================================================
// Variables

    /** Set of sql statements indexed by operation name. */
    protected Properties m_sql;

    /** Cache of prepared versions of the statements. Each map entry is a list
     *  of copies of the prepared statement for multi-threaded apps. */
    protected Map<String, List<PreparedStatement>> m_preparedStatements = CollectionFactory.createHashedMap();

	/** Track which cached, prepared statements are in use and the corresponding
	 *  list to which the statement should be returned. */
	protected Map<PreparedStatement, List<PreparedStatement>> m_cachedStmtInUse = CollectionFactory.createHashedMap();

    /** the packaged jdbc connection to the database itself. */
    protected IDBConnection m_connection;

    /** Maximum number of pre-prepared statements to keep for each operator. */
    protected static final int MAX_PS_CACHE = 4;

    /** Set to true to enable cache of pre-prepared statements. */
    protected boolean CACHE_PREPARED_STATEMENTS = true;

    static protected Logger logger = LoggerFactory.getLogger( SQLCache.class );
//=======================================================================
// Public interface

    /**
     * Constructor. Creates a new cache sql statements for interfacing to
     * a specific database.
     * @param sqlFile the name of the file of sql statements to load, this is
     * loaded from the classpath.
     * @param defaultOps Properties table which provides the default
     * sql statements, any definitions of a given operation in the loaded file
     * will override the default.
     * @param connection the jdbc connection to the database itself
     * @param idType the sql string to use for id types (substitutes for $id in files)
     */
    public SQLCache(String sqlFile, Properties defaultOps, IDBConnection connection, String idType) throws IOException {
        m_sql = loadSQLFile(sqlFile, defaultOps, idType);
        m_connection = connection;
    }

    /**
     * Set to true to enable cache of pre-prepared statements.
     */
    public void setCachePreparedStatements(boolean state) {
        CACHE_PREPARED_STATEMENTS = state;
    }

    /**
     * Return true if cache of pre-prepared statements is enabled.
     */
    public boolean getCachePreparedStatements() {
        return CACHE_PREPARED_STATEMENTS;
    }

    /**
     * Flush the cache of all currently prepared statements.
     */
    public void flushPreparedStatementCache() throws RDFRDBException {
        try {
            Iterator<List<PreparedStatement>> it = m_preparedStatements.values().iterator();
            while (it.hasNext()) {
                Iterator<PreparedStatement> psit = it.next().iterator();
                while (psit.hasNext()) {
                    psit.next().close();
                }
            }
        } catch (SQLException e) {
            throw new RDFRDBException("Problem flushing PS cache", e);
        } finally {
            m_preparedStatements = CollectionFactory.createHashedMap();
			m_cachedStmtInUse = CollectionFactory.createHashedMap();
        }
    }
    /**
     * Return the associated jdbc connection.
     */
    public Connection getConnection() throws SQLException {
        return m_connection.getConnection();
    }

    /**
     * Set the associated jdbc connection.
     */
    public void setConnection(IDBConnection connection) {
        m_connection = connection;
    }

    /**
     * Return the raw SQL statement corresponding to the named operation.
     */
    public String getSQLStatement(String opname) throws SQLException {
        return getSQLStatement(opname, (String[]) null);
    }

    /**
     * Return the raw SQL statement corresponding to the named operation.
     * Substitute the ${a} attribute macro for the current attribute number.
     */
	public String getSQLStatement(String opname, String[] attr) throws SQLException {
		String cmd = m_sql.getProperty(opname);
		if (cmd == null) {
			if ( opname.startsWith("*") ) {
				cmd = genSQLStatement(opname);
				m_sql.setProperty(opname, cmd);
			} else {
				logger.error("Unable to find SQL for operation: " + opname);
				throw new SQLException("Unable to find SQL for operation: " + opname);
			}
		}
		int attrCnt = (attr == null) ? 0 : attr.length;
		if ( attrCnt > 0 ) cmd = substitute(cmd, "${a}", attr[0]);
		if ( attrCnt > 1 ) cmd = substitute(cmd, "${b}", attr[1]);
		if ( attrCnt > 2 ) cmd = substitute(cmd, "${c}", attr[2]);
		if ( attrCnt > 3 ) throw new JenaException("Too many arguments");

		return cmd;
	}

    
    
    public String getSQLStatement(String opname, String attr) throws SQLException {
		String[] param = {attr};
		return getSQLStatement(opname,param);
    }

	/**
	 * Return the raw SQL statement corresponding to the named operation.
	 * Attribute version - substitute the ${a} attribute macro for
	 * the current attribute number.
	 */
	public String getSQLStatement(String opname, String attrA, String attrB) throws SQLException {
		String[] param = {attrA,attrB};
		return getSQLStatement(opname,param);
	}

    /**
     * Return a set of raw SQL statements corresponding to the named operation.
     * This is used for compound operations where more than one SQL command is needed to
     * implement the operation (e.g. database formating and clean up). The
     * individual statements should be separated by double-semicolons at the end of the line.
     * <p>Needs refactoring to clarify what operations are and are not compound but for now
     * it is assumed the caller knows which is correct. Compound statements are not called
     * repeatedly so don't currently cache the parsed statement set.
     */
    public Collection<String> getSQLStatementGroup(String opname) throws SQLException {
        String statementSrc = m_sql.getProperty(opname);
        if (statementSrc == null) {
            throw new SQLException("Unable to find SQL for operation: " + opname);
        }
        int start = 0;
        int split = 0;
        List<String> statements = new LinkedList<String>();
        while (split != -1) {
            split = statementSrc.indexOf(";;\n", start);
            String statement = null;
            if (split == -1) {
                statement = statementSrc.substring(start);
            } else {
                statement = statementSrc.substring(start, split);
                start = split +2;
            }
            if (!statement.trim().equals(""))
                statements.add(statement);
        }
        return statements;
    }

    /**
     * Return a prepared SQL statement corresponding to the named operation.
     * The statement should either be closed after use or returned to the
     * prepared statement pool using {@link #returnPreparedSQLStatement returnPreparedSQLStatement}
     * 
     * <p>Only works for single statements, not compound statements.
     * @param con the jdbc connection to use for preparing statements
     * @param opname the name of the sql operation to locate
     * @return a prepared SQL statement appropriate for the JDBC connection
     * used when this SQLCache was constructed or null if there is no such
     * operation or no such connection
     * 
     * 
     */
    
	public synchronized PreparedStatement getPreparedSQLStatement(String opname, String [] attr) throws SQLException {
		/* TODO extended calling format or statement format to support different
		 * result sets and conconcurrency modes.
		 */
		PreparedStatement ps=null;
		if (m_connection == null || opname == null) return null;
		int attrCnt = (attr == null) ? 0 : attr.length;
		String aop = opname;
		if ( attrCnt > 0 ) aop = concatOpName(aop, attr[0]);
		if ( attrCnt > 1 ) aop = concatOpName(aop, attr[1]);
		if ( attrCnt > 2 ) aop = concatOpName(aop, attr[2]);
		if ( attrCnt > 3 ) throw new JenaException("Too many arguments");
        
		List<PreparedStatement> psl = m_preparedStatements.get(aop);
		// OVERRIDE: added proper PreparedStatement removal.
		if (psl!=null && !psl.isEmpty()) {
			ps = psl.remove(0);
			try{
    			ps.clearParameters();
    		}catch(SQLException e) {
    			ps.close();
    		}
		}
		if (ps == null) {
			String sql = getSQLStatement(opname, attr);
			if (sql == null) {
				throw new SQLException("No SQL defined for operation: " + opname);
			}
			if (psl == null && CACHE_PREPARED_STATEMENTS) {
				psl = new LinkedList<PreparedStatement>();
				m_preparedStatements.put(aop, psl);
			} 
			ps = doPrepareSQLStatement(sql);
		}
		if ( CACHE_PREPARED_STATEMENTS ) m_cachedStmtInUse.put(ps,psl);
		return ps;
	}
	
	/**
	 * Prepare a SQL statement for the given statement string.
	 *  
	 * <p>Only works for single statements, not compound statements.
	 * @param stmt the sql statement to prepare.
	 * @return a prepared SQL statement appropriate for the JDBC connection
	 * used when this SQLCache was constructed or null if there is no such
	 * connection.
	 */

	private synchronized PreparedStatement doPrepareSQLStatement(String sql) throws SQLException {
		if (m_connection == null) return null;
		return getConnection().prepareStatement(sql);
	}

	/**
	 * Return a prepared SQL statement for the given statement string.
	 * The statement should either be closed after use.
	 *  
	 * <p>Only works for single statements, not compound statements.
	 * @param stmt the sql statement to prepare.
	 * @return a prepared SQL statement appropriate for the JDBC connection
	 * used when this SQLCache was constructed or null if there is no such
	 * connection.
	 */

	public synchronized PreparedStatement prepareSQLStatement(String sql) throws SQLException {
		if (m_connection == null) return null;
		return doPrepareSQLStatement(sql);
	}

    public synchronized PreparedStatement getPreparedSQLStatement(String opname) throws SQLException {
    	return getPreparedSQLStatement(opname, (String[]) null);
    }

    /**
     * Variant on {@link #getPreparedSQLStatement getPreparedSQLStatement} which
     * accesses the attribute variant correspond to the given attribute suffix.
     */
    public synchronized PreparedStatement getPreparedSQLStatement(String opname, String attr) throws SQLException {
		String[] param = {attr};
		return getPreparedSQLStatement(opname,param);
    }

    /**
     * Variant on {@link #getPreparedSQLStatement getPreparedSQLStatement} which
     * access the attribute variant correspond to the given attribute suffix.
     */
    public synchronized PreparedStatement getPreparedSQLStatement(String opname, String attrA, String attrB) throws SQLException {
		String[] param = {attrA,attrB};
		return getPreparedSQLStatement(opname,param);
	}
    
    /**
     * Return a prepared statement to the statement pool for reuse by
     * another caller. Any close problems logged rather than raising exception
     * so that iterator close() operations can be silent so that they can meet
     * the ClosableIterator signature.
     */
    public synchronized void returnPreparedSQLStatement(PreparedStatement ps) {
        if (!CACHE_PREPARED_STATEMENTS) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.warn("Problem discarded prepared statement", e);
            }
            return;
        }
        List<PreparedStatement> psl = m_cachedStmtInUse.get(ps);
        if (psl != null) {
        	if (psl.size() >= MAX_PS_CACHE) {
           		try {
               		ps.close();
            	} catch (SQLException e) {
                	logger.warn("Problem discarded prepared statement", e);
            	}
        	} else {
            	psl.add(ps);
        	}
        	m_cachedStmtInUse.remove(ps);
        } else {
        	throw new JenaException("Attempt to return unused prepared statement");
        }
    }

//    /**
//     * Execute a named pre-prepared SQL query statement taking a set of arguments and return
//     * a set of results as an iterator (probably a subclass of ResultSetIterator. Returns null
//     * if they query is an update (as opposed to an empty iterator for a true query which happens
//     * to return no answers).
//     * <p>
//     * Not sure this is a good design. Reducing this to a general interface leads to lots of clunky
//     * wrapping and unwrapping of primitive types, coercions and lack of compile-time type checking.
//     * On the other hand letting the clients do this themselves with direct jdbc calls leaves us up
//     * to the mercy of the client to correctly use returnPreparedSQLStatement and on average seems
//     * to lead to more duplication of boiler plate code. Currently the client can chose either approach.
//     * <p>
//     * The calling arguments are passed in as an array.
//     */
//    public ResultSetIterator<Object> runSQLQuery(String opname, Object[] args) throws SQLException {
//        PreparedStatement ps = getPreparedSQLStatement(opname);
//        if (args != null) {
//            for (int i = 0; i < args.length; i++) {
//                ps.setObject(i+1, args[i]);
//            }
//        }
//        return executeSQL(ps, opname, new ResultSetIterator<Object>());
//    }
//
//	/**
//     * Variant on {@link #runSQLQuery} which
//     * access the attribute variant correspond to the given attribute suffix.
//     */
//    public ResultSetIterator<Object> runSQLQuery(String opname,String attr, Object[] args) throws SQLException {
//        String aop = concatOpName(opname, attr);
//        PreparedStatement ps = getPreparedSQLStatement(aop);
//        
//        if (args != null) {
//            for (int i = 0; i < args.length; i++) {
//                ps.setObject(i+1, args[i]);
//            }
//        }
//        return executeSQL(ps, aop, new ResultSetIterator<Object>());
//    }
//
//	/**
//     * Variant on {@link #runSQLQuery} which
//     * access the attribute variant correspond to the given attribute suffix.
//     */
//    public ResultSetIterator<Object> runSQLQuery(String opname,String attrA, String attrB, Object[] args) throws SQLException {
//        String aop = concatOpName(opname, attrA, attrB);
//        PreparedStatement ps = getPreparedSQLStatement(aop);
//        
//        if (args != null) {
//            for (int i = 0; i < args.length; i++) {
//                ps.setObject(i+1, args[i]);
//            }
//        }
//        return executeSQL(ps, aop, new ResultSetIterator<Object>());
//    }


    /**
     * Execute a named pre-prepared SQL update statement taking a set of arguments and returning
     * the update count.
     */
    public int runSQLUpdate(String opname, Object[] args) throws SQLException {
        PreparedStatement ps = getPreparedSQLStatement(opname);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        int result = ps.executeUpdate();
        returnPreparedSQLStatement(ps);
        return result;
    }

	/**
     * Variant on {@link #runSQLUpdate} which
     * access the attribute variant correspond to the given attribute suffix.
     */
    public int runSQLUpdate(String opname,String attrA, Object[] args) throws SQLException {
        String aop = concatOpName(opname, attrA);
        PreparedStatement ps = getPreparedSQLStatement(aop);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        int result = ps.executeUpdate();
        returnPreparedSQLStatement(ps);
        return result;
    }

	/**
     * Variant on {@link #runSQLUpdate} which
     * access the attribute variant correspond to the given attribute suffix.
     */
    public int runSQLUpdate(String opname,String attrA, String attrB, Object[] args) throws SQLException {
        String aop = concatOpName(opname, attrA, attrB);
        PreparedStatement ps = getPreparedSQLStatement(aop);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        int result = ps.executeUpdate();
        returnPreparedSQLStatement(ps);
        return result;
    }







    /**
     * Execute a named pre-prepared SQL query statement taking a set of arguments and return
     * a set of results as an iterator (probably a subclass of ResultSetIterator. Returns null
     * if they query is an update (as opposed to an empty iterator for a true query which happens
     * to return no answers).
     * <p>
     * Not sure this is a good design. Reducing this to a general interface leads to lots of clunky
     * wrapping and unwrapping of primitive types, coercions and lack of compile-time type checking.
     * On the other hand letting the clients do this themselves with direct jdbc calls leaves us up
     * to the mercy of the client to correctly use returnPreparedSQLStatement and on average seems
     * to lead to more duplication of boiler plate code. Currently the client can chose either approach.
     * <p>
     * @param opname the name of the SQL operation to perform
     * @param args the arguments to pass to the SQL operation as an array of Objects
     * @param iterator the iterator to use to return the results
     */
    public ResultSetIterator<?> runSQLQuery(String opname, Object[] args, ResultSetIterator<?> iterator) throws SQLException {
        PreparedStatement ps = getPreparedSQLStatement(opname);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        return executeSQL(ps, opname, iterator);
    }

	/**
     * Variant on {@link #runSQLQuery} which
     * access the attribute variant correspond to the given attribute suffix.
     */
    public ResultSetIterator<?> runSQLQuery(String opname, String attrA, Object[] args, ResultSetIterator<?> iterator) throws SQLException {
        String aop = concatOpName(opname,attrA);
        PreparedStatement ps = getPreparedSQLStatement(aop);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        
        return executeSQL(ps, aop, iterator);
    }

	/**
     * Variant on {@link #runSQLQuery} which
     * access the attribute variant correspond to the given attribute suffix.
     */
    public ResultSetIterator<?> runSQLQuery(String opname, String attrA, String attrB, Object[] args, ResultSetIterator<?> iterator) throws SQLException {
        String aop = concatOpName(opname,attrA, attrB);
        PreparedStatement ps = getPreparedSQLStatement(aop);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i+1, args[i]);
            }
        }
        
        return executeSQL(ps, aop, iterator);
    }
  

	/**
	 * Run a group of sql statements - normally used for db formating and clean up.
	 * All statements are executed even if one raises an error then the error is
	 * reported at the end.
	 * 
	 * Attribute version -- substitute the ${a} attribute macro
	 * for the current attribute 
	 */
	public void runSQLGroup(String opname, String [] attr) throws SQLException {
		String op = null;
		SQLException eignore = null;
		java.sql.Statement sql = getConnection().createStatement();
		Iterator<String> ops = getSQLStatementGroup(opname).iterator();
		
		try {
    		int attrCnt = attr == null ? 0 : attr.length;
    		if ( attrCnt > 6 )
    			throw new RDFRDBException("Too many parameters");
    		while (ops.hasNext()) {
    			op = ops.next();
    			if ( attrCnt > 0 ) op = substitute(op,"${a}",attr[0]);
    			if ( attrCnt > 1 ) op = substitute(op,"${b}",attr[1]);
    			if ( attrCnt > 2 ) op = substitute(op,"${c}",attr[2]);
    			if ( attrCnt > 3 ) op = substitute(op,"${d}",attr[3]);
    			if ( attrCnt > 4 ) op = substitute(op,"${e}",attr[4]);
    			if ( attrCnt > 5 ) op = substitute(op,"${f}",attr[5]);			
    			try {
    			    //System.out.println("SQL : "+op) ;
    				sql.execute(op);
    			} catch (SQLException e) {
    				// This is debugging legacy, exception is still reported at the end
    				//System.out.println("SQL failure: " + op + ": " + e); System.out.flush() ;
    				throw e ;
    			}
    		}
		} finally 
		{
		    try { sql.close(); } catch (SQLException e2) {}
		}
	}



    /**
     * Run a group of sql statements - normally used for db formating and clean up.
     * All statements are executed even if one raises an error then the error is
     * reported at the end.
     */
    public void runSQLGroup(String opname) throws SQLException {
    	runSQLGroup(opname,(String[])null);
   }

	/**
     * Run a group of sql statements - normally used for db formating and clean up.
     * All statements are executed even if one raises an error then the error is
     * reported at the end.
     * 
     * Attribute version -- substitute the ${a} attribute macro
     * for the current attribute 
     */
    public void runSQLGroup(String opname, String attr) throws SQLException {
    	String[] param = {attr};
    	runSQLGroup(opname,param);
    }

	/**
     * Run a group of sql statements - normally used for db formating and clean up.
     * All statements are executed even if one raises an error then the error is
     * reported at the end.
     * 
     * Attribute version -- substitute the ${a} attribute macro
     * for the current attribute 
     */
    public void runSQLGroup(String opname, String attrA, String attrB) throws SQLException {
		String[] param = {attrA,attrB};
		runSQLGroup(opname,param);
    }



    /**
     * Close all prepared statements
     */
    public void close() throws SQLException {
        Iterator<List<PreparedStatement>> it = m_preparedStatements.values().iterator();
        while (it.hasNext()) {
            List<PreparedStatement> psl = it.next();
            Iterator<PreparedStatement> itl = psl.iterator();
            while (itl.hasNext()) {
                PreparedStatement ps = itl.next();
                ps.close();
            }
            it.remove();
        }
		it = m_cachedStmtInUse.values().iterator();
		while (it.hasNext()) {
			it.remove();
		}
    }

    /**
     * Load in a defined set of sql statements - see class comment for format.
     * The loaded file is return as a Property table. This call is static
     * to support the loading of a default sql mapping.
     * @param sqlFile the name of the file of sql statements to load, this is
     * loaded from the classpath.
     * @param defaultOps a Properties table of default sql definitions.
     * @param idType the sql string to use for id types (substitutes for $id in files)
     */
    public static Properties loadSQLFile(String sqlFile, Properties defaultOps, String idType) throws IOException {
        Properties sqlTable = new Properties(defaultOps);
        BufferedReader src = openResourceFile(sqlFile);
        String line = null;
        while ((line = src.readLine()) != null) {
            if (line.startsWith("#")) {
                continue; // Comment line so skip it
            }
            String opName = line.trim();
            StringBuffer sql = new StringBuffer();
            while (true) {
                line = src.readLine();
                if (line == null || line.trim().equals("")) {
                        // Blank line terminates sql block
                    sqlTable.setProperty(opName, sql.toString());
                    break;
                } else if (line.startsWith("#")) {
                    continue;
                } else {
                    sql.append(substitute(line.trim(), "${id}", idType));
                    sql.append("\n");
                }
            }
            if (line == null) break;        // Check if read to end of file
        }
        return sqlTable;
    }


	/** Helper function calculate op name given substitutions */
	public static String concatOpName(String opName, String attr) {
		return (opName + attr);
	}
	
	/** Helper function calculate op name given substitutions */
	public static String concatOpName(String opName, String attrA, String attrB) {
		return (opName  + attrA  + attrB);
	}

    /** Helper function substitute all occurances of macro with subs */
    public static String substitute(String line, String macro, String subs) {
        int loc = line.indexOf(macro);
        if (loc != -1) {
            return line.substring(0, loc) + subs + substitute(line.substring(loc+macro.length()),macro, subs);
        } else {
            return line;
        }
    }
    

//=======================================================================
// Internal support

    /**
     * Accessor. Returns the Properties table which maps operation names to
     * the plain text sql statements. This is using internally in the constructor.
     */
    protected Properties getSQLTable() {
        return m_sql;
    }

    /**
     * Open a resource file for reading. The file is found on the classpath.
     */
    public static BufferedReader openResourceFile(String filename) throws IOException {
        InputStream is = SQLCache.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) 
            throw new IOException("Can't open resource " + filename);
        return new BufferedReader(new InputStreamReader(is, "US-ASCII"));
    }

    /**
     * Execute the given statement, return null if the statement appears to be
     * just an update or return an iterator for the result set if the statement appears
     * to be a query
     */
    protected <T> ResultSetIterator<T> executeSQL(PreparedStatement ps, String opname, ResultSetIterator<T> iterator) throws SQLException {
        if (ps.execute()) {
            ResultSet rs = ps.getResultSet();
            iterator.reset(rs, ps, this, opname);
            return iterator;
        } else {
            returnPreparedSQLStatement(ps);
            return null;
        }
    }
    
    
    /**
     * Return dynamically generated SQL for the specified operation.
     * @param opname the command to generate; must start with "*", the opname and then op params.
     * @return the generated command as a String.
     */
    
    protected String genSQLStatement ( String opname ) throws SQLException {
    	/* for testing. for now, we only generate one operation, findReif,
    	 * to find reified statements from a triple match pattern.
    	 */
    	String sql = "";
    	boolean badop = false;
    	if ( opname.startsWith("*") ) {
    		// a space separate the operation name from its parameters.
    		int delim = opname.indexOf(' ');
    		String op = opname.substring(1,delim);
    		String args = opname.substring(delim+1);
    		if ( op.equals("findReif") ) {
    			sql = genSQLStmtFindReif(op,args);
    		} else badop = true;
    	} else badop = true;
    	if ( badop ) {
			logger.error("Unable to generate SQL for operation: " + opname);
			throw new JenaException("Unable to generate SQL for operation: " + opname);
    	}   	
    	return sql;    	
    }
    
	/**
	 * Return generate SQL for finding reified statements from a triple pattern.
	 * @param op the command to generate. should be findReif.
	 * @param args a string describing which command to generate. 
	 * it has the form [N][PS|PP|PO|PT][O[C]] where N means to search
	 * for the statement URI; Px means to search for reified subjects, properties,
	 * objects or types; O means to search for reified objects; OC means the object
	 * value is rdf:Statement.
	 */
    
	protected String genSQLStmtFindReif ( String op, String args ) throws SQLException {
		/* for a reified triple pattern <S,P,O>, there are 8 cases.
		 * 1. <-,-,->	this means retrieve all reified triples. args="".
		 * 2. <S,-,->	retrieve all reified triples for this subject. args="N".
		 * 3. <S,-,O>	retrieve all reified triples for this subject and
		 * 				object value. args="NO" or "NOC".
		 * 4. <-,-,O>	retrieve all reified triples with this object value.
		 * 				args="O" or "OC"
		 * 5. <-,P,->	retrieve all reified triples with this property. args="Px".
		 * 				property must be either rdf:subject, rdf:predicate,
		 * 				rdf:object, rdf:type.
		 * 6. <-,P,O>	retrieve all reified triples with this property and object
		 * 				value. args="PxO" or "PxOC".
		 * 7. <S,P,->	retrieve all reified triples with this subject and property.
		 * 				args="NPx".
		 * 8. <S,P,O>	retrieve all reified triples with this subject, property and
		 * 				object value. args="NPxO" or "NPxOC".
		 */

		String stmtStr = getSQLStatement("selectReified");
		String qual = "";
		IRDBDriver driver = m_connection.getDriver();
		
		if ( args.equals("") ) {
			// case 1 <-,-,->  nothing to do.
		} else {
			int ix = 0;
			boolean hasSubj = false;
			boolean hasProp = false;
			boolean hasObj = false;
			boolean objIsStmt = false;
			char reifProp = ' ';
			int argLen = args.length();
			
			if ( args.charAt(ix) == 'N' ) {
				hasSubj = true;
				ix++;
			}
			hasProp = (ix < argLen) && (args.charAt(ix) == 'P');
			if ( hasProp && (ix < argLen) ) {
				ix++;
				reifProp = args.charAt(ix++);
			}
			hasObj = (ix < argLen) && (args.charAt(ix) == 'O');
			if ( hasObj ) {
				ix++;
				objIsStmt = (ix < argLen) && (args.charAt(ix) == 'C');
			}
			if ( !hasProp ) {
				if ( hasSubj ) {
					// cases 2 and 3
					qual += driver.genSQLReifQualStmt();
					if ( hasObj ) {
						// case 3 above
						qual += " AND " + driver.genSQLReifQualAnyObj(objIsStmt);
					}			
				} else {
					// case 4 above
					qual += driver.genSQLReifQualAnyObj(objIsStmt);
				}
			} else {
				// have a reified property
				if ( hasSubj ) qual += driver.genSQLReifQualStmt() + " AND ";
				qual += driver.genSQLReifQualObj(reifProp,hasObj);
			}
			stmtStr += " AND " + qual;		
		}
		return stmtStr;
	}
	
}

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
