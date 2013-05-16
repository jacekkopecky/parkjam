/*
 	(c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 	All rights reserved - see end of file.
 	$Id: JA.java,v 1.1 2009/06/29 08:55:36 castagna Exp $
*/

package com.hp.hpl.jena.assembler;

import com.hp.hpl.jena.rdf.model.*;

public class JA
    {
    public static final String uri = "http://jena.hpl.hp.com/2005/11/Assembler#";
    
    public static String getURI()
        { return uri; }

    protected static Model schema;
    
    protected static Resource resource( String localName )
        { return ResourceFactory.createResource( uri + localName ); }
    
    public static Property property( String localName )
        { return ResourceFactory.createProperty( uri + localName ); }
    
    public static final Resource MemoryModel = resource( "MemoryModel" );
    
    public static final Resource DefaultModel = resource( "DefaultModel" );

    public static final Resource InfModel = resource( "InfModel" );

    public static final Resource Object = resource( "Object" );

    public static final Property reasoner = property( "reasoner" );

    public static final Property reasonerURL = property( "reasonerURL" );
    
    public static final Property baseModel = property( "baseModel" );

    public static final Property literalContent = property( "literalContent" );
    
    public static final Property connection = property( "connection" );

    public static final Property rules = property( "rules" );

    public static final Resource Model = resource( "Model" );

    public static final Resource OntModel = resource( "OntModel" );

    public static final Resource NamedModel = resource( "NamedModel" );

    public static final Resource FileModel = resource( "FileModel" );

    public static final Resource RDBModel = resource( "RDBModel" );

    public static final Resource PrefixMapping = resource( "PrefixMapping" );

    public static final Resource ReasonerFactory = resource( "ReasonerFactory" );

    public static final Resource HasFileManager = resource( "HasFileManager" );

    public static final Resource Content = resource( "Content" );

    public static final Resource Connection = resource( "Connection" );

    public static final Resource Connectable = resource( "Connectable" );

    public static final Resource LiteralContent = resource( "LiteralContent" );

    public static final Resource OntModelSpec = resource( "OntModelSpec" );

    public static final Resource ModelSource = resource( "ModelSource" );

    public static final Resource RDBModelSource = resource( "RDBModelSource" );

    public static final Property content = property( "content" );

    public static final Resource ExternalContent = resource( "ExternalContent" );

    public static final Property externalContent = property( "externalContent" );

    public static final Property modelName = property( "modelName" );

    public static final Property ontModelSpec = property( "ontModelSpec" );

    public static final Resource This = resource( "this" );
    
    public static final Resource True = resource( "true" );
    
    public static final Resource False = resource( "false" );

    public static final Resource Expanded = resource( "Expanded" );

    public static final Property prefix = property( "prefix" );

    public static final Property namespace = property( "namespace" );

    public static final Property includes = property( "includes" );

    public static final Property directory = property( "directory" );

    public static final Property create = property( "create" );

    public static final Property strict = property( "strict" );

    public static final Property mapName = property( "mapName" );

    public static final Property documentManager = property( "documentManager" );

    public static final Property ontLanguage = property( "ontLanguage" );

    public static final Property importSource = property( "importSource" );

    public static final Property quotedContent = property( "quotedContent" );

    public static final Property contentEncoding = property( "contentEncoding" );

    public static final Property initialContent = property( "initialContent" );

    public static final Resource RuleSet = resource( "RuleSet" );

    public static final Property rule = property( "rule" );

    public static final Resource HasRules = resource( "HasRules" );

    public static final Property rulesFrom = property( "rulesFrom" );

    public static final Resource ContentItem = resource( "ContentItem" );

    public static final Property dbClass = property( "dbClass" );

    public static final Resource LocationMapper = resource( "LocationMapper" );

    public static final Property locationMapper = property( "locationMapper" );

    public static final Resource FileManager = resource( "FileManager" );

    public static final Resource DocumentManager = resource( "DocumentManager" );

    public static final Property fileManager = property( "fileManager" );

    public static final Property policyPath = property( "policyPath" );

    public static final Resource UnionModel = resource( "UnionModel" );

    public static final Property subModel = property( "subModel" );

    public static final Property rootModel = property( "rootModel" );

    public static final Property reificationMode = property( "reificationMode" );

    public static final Resource minimal = resource( "minimal" );

    public static final Resource convenient = resource( "convenient" );

    public static final Resource standard = resource( "standard" );

    public static final Resource ReificationMode = resource( "ReificationMode" );

    public static final Property fileEncoding = property( "fileEncoding" );

    public static final Property dbUser = property( "dbUser" );

    public static final Property dbUserProperty = property( "dbUserProperty" );

    public static final Property dbPassword = property( "dbPassword" );

    public static final Property dbPasswordProperty = property( "dbPasswordProperty" );

    public static final Property dbURL = property( "dbURL" );

    public static final Property dbURLProperty = property( "dbURLProperty" );

    public static final Property dbType = property( "dbType" );

    public static final Property dbTypeProperty = property( "dbTypeProperty" );

    public static final Property dbClassProperty = property( "dbClassProperty" );

    public static final Property assembler = property( "assembler" );
    
    public static final Property loadClass = property( "loadClass" );
    
    public static final Property imports = property( "imports" );

    public static final Property reasonerFactory = property( "reasonerFactory" );

    public static final Property reasonerClass = property( "reasonerClass" );
    
    public static final Property ja_schema = property( "schema" );

    public static final Property likeBuiltinSpec = property( "likeBuiltinSpec" );

    public static final Resource SinglePrefixMapping = resource( "SinglePrefixMapping");
    
    public static final Property prefixMapping = property( "prefixMapping" );

    public static Model getSchema()
        { // inline packagename to avoid clash with /our/ FileManager.
        if (schema == null) schema = complete( com.hp.hpl.jena.util.FileManager.get().loadModel( getSchemaPath() ) );
        return schema;
        }

    private static Model complete( Model m )
        {
        Model result = ModelFactory.createDefaultModel();
        result.add( ModelFactory.createRDFSModel( m ) );
        return result;
        }
    
    private static String getSchemaPath()
        { return "vocabularies/assembler.n3"; }
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
