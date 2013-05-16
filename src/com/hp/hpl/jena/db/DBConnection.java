/*
 *  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 *
 */

package com.hp.hpl.jena.db;

import java.sql.*;

import com.hp.hpl.jena.db.impl.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.*;

/**
* Encapsulate the specification of a jdbc connection.
* This is mostly used to simplify the calling pattern for ModelRDB factory methods.
*
* @author csayers (based in part on the jena 1 implementation by der).
* @version $Revision: 1.1 $
*/

public class DBConnection implements IDBConnection { 

    /** The jdbc connection being wrapped up */
    protected Connection m_connection;

    /** The url for the connection, may be null if the connection was passed in pre-opened */
    protected String m_url;

    /** The user name for the connection, may be null if the connection was passed in pre-opened */
    protected String m_user;

    /** The password for the connection, may be null if the connection was passed in pre-opened */
    protected String m_password;

	/** The database type: "Oracle", "mySQL, etc...
	 *  This is new in Jena2 - for compatability with older code we allow this to
	 * be left unspecified at the loss of some jena2 functionality.
	 */
	protected String m_databaseType = null;
	
	/** Driver to connect to this database */
	protected IRDBDriver m_driver = null;
	    
	
    /**
     * Create a connection specification based on jdbc address and
     * appropriate authentication information.
     * @param url the jdbc url for the database, note that the format of this
     * is database dependent and that the appropriate jdbc driver will need to
     * be specified via the standard pattern
     * <pre>
     *     Class.forName("my.sql.driver");
     * </pre>
     * @param user the user name to log on with
     * @param password the password corresponding to this user
     * @param databaseType the type of database to which we are connecting.
     * 
	 * @since Jena 2.0
     */
    public DBConnection(String url, String user, String password, String databaseType) {
        m_url = url;
        m_user = user;
        m_password = password;
		setDatabaseType(databaseType);
    }

    /**
     * Create a connection specification that just wraps up an existing database
     * connection.
     * @param connection the open jdbc connection to use
     * @param databaseType the type of database to which we are connecting.
	 * 
	 * @since Jena 2.0
     */
    public DBConnection(Connection connection, String databaseType) {
        m_connection = connection;
		setDatabaseType(databaseType);
    }
        

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getConnection()
	 */
	public Connection getConnection() throws SQLException {
		if (m_connection == null) {
			if (m_url != null) {
				m_connection =
					DriverManager.getConnection(m_url, m_user, m_password);
				m_connection.setAutoCommit(true);
			}
		}
		return m_connection;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#close()
	 */
	public void close() throws SQLException {
		if( m_driver != null ) {
			m_driver.close();
			m_driver = null;
		}
		if (m_connection != null) {
			m_connection.close();
			m_connection = null;
		}
	}

    /* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#cleanDB()
	 */
	public void cleanDB() throws SQLException {
		if (m_driver == null)
			m_driver = getDriver();
    	m_driver.cleanDB();
    }

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#isFormatOK()
	 */
	public boolean isFormatOK() {
// Removed exception trap, an exception might be a connection
// failure on a well formated database - der 24/7/04        
//		try {
			if( m_driver == null )
				m_driver = getDriver();
			return m_driver.isDBFormatOK();
//		} catch (Exception e) {
//			return false;
//		}
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#setDatabaseProperties(com.hp.hpl.jena.rdf.model.Model)
	 */
	public void setDatabaseProperties(Model dbProperties) throws RDFRDBException {
		if (m_driver == null)
			m_driver = getDriver();
		m_driver.setDatabaseProperties( dbProperties.getGraph());
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getDatabaseProperties()
	 */
	public Model getDatabaseProperties() throws RDFRDBException {
		if (m_driver == null)
			m_driver = getDriver();
		Model resultModel = ModelFactory.createDefaultModel();
		copySpecializedGraphToModel( m_driver.getSystemSpecializedGraph(true),
			                         resultModel, Triple.ANY );
		return resultModel;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getDefaultModelProperties()
	 */
	public Model getDefaultModelProperties() throws RDFRDBException {
		if (m_driver == null)
			m_driver = getDriver();
		DBPropGraph defaultProps = m_driver.getDefaultModelProperties();
		Model resultModel = ModelFactory.createDefaultModel();
		copySpecializedGraphToModel( m_driver.getSystemSpecializedGraph(true),
			                         resultModel,
			                         Triple.createMatch(defaultProps.getNode(), null, null));
		return resultModel;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getAllModelNames()
	 */
	public ExtendedIterator<String> getAllModelNames() throws RDFRDBException {
		if (m_driver == null)
			m_driver = getDriver();
		SpecializedGraph sg = m_driver.getSystemSpecializedGraph(false);
		ExtendedIterator<String> it;
		if ( sg == null )
			it = NullIterator.instance() ;
		else {
			DBPropDatabase dbprops = new DBPropDatabase(sg);
			it = dbprops.getAllGraphNames();
		}
		return it;
	}
	
	 /* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#containsModel(java.lang.String)
	 */
	public boolean containsModel(String name) throws RDFRDBException {
		boolean res = false;
		if (m_driver == null)
			m_driver = getDriver();
		SpecializedGraph sg = m_driver.getSystemSpecializedGraph(false);
		if ( sg != null ) {
			DBPropGraph g = DBPropGraph.findPropGraphByName(sg,name);
			res = g == null ? false : g.isDBPropGraphOk(name);
		}
		return res;		
	 }

	 /* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#containsDefaultModel()
	 */
	public boolean containsDefaultModel() throws RDFRDBException {
		return containsModel(GraphRDB.DEFAULT);
	 }

	/** 
	 * Copy the contents of a specialized graph to a new Model.
	 * 
	 * This has package scope - for internal use only.
	 * 
	 * @since Jena 2.0
	 */
	static void copySpecializedGraphToModel( SpecializedGraph fromGraph, Model toModel, TripleMatch filter) throws RDFRDBException {
		Graph toGraph = toModel.getGraph();
		SpecializedGraph.CompletionFlag complete = new SpecializedGraph.CompletionFlag();
		ExtendedIterator<Triple> it = fromGraph.find( filter, complete);
		while(it.hasNext())
			toGraph.add(it.next()); 
		it.close();
	}
		
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#setDatabaseType(java.lang.String)
	 */
	public void setDatabaseType( String databaseType )
	{
	    if ( databaseType == null )
	    {
	        m_databaseType = databaseType;
	        return ;
	    }
	    
	    if (databaseType.equalsIgnoreCase("mysql"))
	        m_databaseType = "MySQL";
	    else if ( databaseType.equalsIgnoreCase("hsql") )
	        m_databaseType = "HSQLDB" ;
	    else if ( databaseType.equalsIgnoreCase("hsqldb") )
	        m_databaseType = "HSQLDB" ;
	    else
	        m_databaseType = databaseType;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getDatabaseType()
	 */
	public String getDatabaseType() { return m_databaseType; }
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#getDriver()
	 */
	public IRDBDriver getDriver() throws RDFRDBException {
		try {
			if (m_connection == null)
				getConnection();

			if (m_driver == null) {
				// need to look for a suitable driver
				if (m_databaseType == null) {
					// without knowing the database type there's not much we can do.
					throw new RDFRDBException("Error - attempt to call DBConnection.getDriver before setting the database type");
				}
				m_driver = (IRDBDriver) (Class.forName("com.hp.hpl.jena.db.impl.Driver_" + m_databaseType).newInstance());
				m_driver.setConnection( this );
			} 
		} catch (Exception e) {
            // e.printStackTrace( System.err );
			throw new RDFRDBException("Failure to instantiate DB Driver:"+ m_databaseType+ " "+ e.toString(), e);
		}

		return m_driver;
	}

    /* (non-Javadoc)
	 * @see com.hp.hpl.jena.db.IDBConnection#setDriver(com.hp.hpl.jena.db.impl.IRDBDriver)
	 */
	public void setDriver(IRDBDriver driver) {
    	m_driver = driver;
    }
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
