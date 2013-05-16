/*
 * (c) Copyright 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * [See end of file]
 */

package com.hp.hpl.jena.n3;

//import org.apache.commons.logging.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDF ;
import com.hp.hpl.jena.vocabulary.RDFS ;
import com.hp.hpl.jena.util.iterator.*;

import java.util.* ;

/** An N3 pretty printer.
 *  Tries to make N3 data look readable - works better on regular data.
 *
 * @author		Andy Seaborne
 * @version 	$Id: N3JenaWriterPP.java,v 1.3 2009/07/01 17:23:13 andy_seaborne Exp $
 */



public class N3JenaWriterPP extends N3JenaWriterCommon
    /*implements RDFWriter*/
{
	// This N3 writer proceeds in 2 stages.  First, it analysises the model to be
	// written to extract information that is going to be specially formatted
	// (RDF lists, small anon nodes) and to calculate the prefixes that will be used.

    final protected boolean doObjectListsAsLists = getBooleanValue("objectLists", true) ;
    
	// Data structures used in controlling the formatting

    protected Set<Resource> rdfLists      	= null ; 		// Heads of lists
    protected Set<Resource> rdfListsAll   	= null ;		// Any resources in a daml lists
    protected Set<Resource> rdfListsDone  	= null ;		// RDF lists written
    protected Set<RDFNode>  oneRefObjects 	= null ;		// Bnodes referred to once as an object - can inline
    protected Set<Resource> oneRefDone   	= null ;		// Things done - so we can check for missed items

    // Do we do nested (one reference) nodes?
    protected boolean allowDeep = true ;
    
    static final protected String objectListSep = " , " ;
    
    // ----------------------------------------------------
    // Prepatation stage

	@Override
    protected void prepare(Model model)
	{
		prepareLists(model) ;
		prepareOneRefBNodes(model) ;
	}

	// Find well-formed RDF lists - does not find empty lists (this is intentional)
	// Works by finding all tails, and work backwards to the head.
    // RDF lists may, or may not, have a type element.
    // Should do this during preparation, not as objects found during the write
    // phase.   

	protected void prepareLists(Model model)
	{
		Set<Resource> thisListAll = new HashSet<Resource>();

		StmtIterator listTailsIter = model.listStatements(null, RDF.rest, RDF.nil);

		// For every tail of a list
		//tailLoop:
		for ( ; listTailsIter.hasNext() ; )
		{
			// The resource for the current element being considered.
			Resource listElement  = listTailsIter.nextStatement().getSubject() ;
            // The resource pointing to the link we have just looked at.
            Resource validListHead = null ;

			// Chase to head of list
			for ( ; ; )
			{
				boolean isOK = checkListElement(listElement) ;
				if ( ! isOK )
					break ;

				// At this point the element is exactly a DAML list element.
				if ( N3JenaWriter.DEBUG ) out.println("# RDF list all: "+formatResource(listElement)) ;
				validListHead = listElement ;
				thisListAll.add(listElement) ;

				// Find the previous node.
				StmtIterator sPrev = model.listStatements(null, RDF.rest, listElement) ;

				if ( ! sPrev.hasNext() )
					// No daml:rest link
					break ;

				// Valid pretty-able list.  Might be longer.
				listElement = sPrev.nextStatement().getSubject() ;
				if ( sPrev.hasNext() )
				{
					if ( N3JenaWriter.DEBUG ) out.println("# RDF shared tail from "+formatResource(listElement)) ;
					break ;
				}
			}
			// At head of a pretty-able list - add its elements and its head.
			if ( N3JenaWriter.DEBUG ) out.println("# DAML list head: "+formatResource(validListHead)) ;
			rdfListsAll.addAll(thisListAll) ;
			if ( validListHead != null )
				rdfLists.add(validListHead) ;
		}
		listTailsIter.close() ;
	}

	// Validate one list element.
	protected boolean checkListElement(Resource listElement) 
	{
		if (!listElement.hasProperty(RDF.rest)
			|| !listElement.hasProperty(RDF.first))
		{
			if (N3JenaWriter.DEBUG)
				out.println(
					"# RDF list element does not have required properties: "
						+ formatResource(listElement));
			return false;
		}

        // Must be exactly two properties (the ones we just tested for)
        // or three including the RDF.type RDF.List statement.
        int numProp = countProperties(listElement);

        if ( numProp == 2)
            // Must have exactly the properties we just tested for.
            return true ;


        if (numProp == 3)
        {
            if (listElement.hasProperty(RDF.type, RDF.List))
                return true;
            if (N3JenaWriter.DEBUG)
                out.println(
                    "# RDF list element: 3 properties but no rdf:type rdf:List"
                        + formatResource(listElement));
            return false;
        }

        if (N3JenaWriter.DEBUG)
            out.println(
                "# RDF list element does not right number of properties: "
                    + formatResource(listElement));
        return false;
	}

	// Find bnodes that are objects of only one statement (and hence can be inlined)
	// which are not RDF lists.
    // Could do this testing at write time (unlike lists)

	protected void prepareOneRefBNodes(Model model) 
	{

		NodeIterator objIter = model.listObjects() ;
		for ( ; objIter.hasNext() ; )
		{
			RDFNode n = objIter.nextNode() ;
            
            if ( testOneRefBNode(n) )
                oneRefObjects.add(n) ; 
            objIter.close() ;

            // N3JenaWriter.DEBUG
            if ( N3JenaWriter.DEBUG )
            {
                out.println("# RDF Lists      = "+rdfLists.size()) ;
                out.println("# RDF ListsAll   = "+rdfListsAll.size()) ;
                out.println("# oneRefObjects  = "+oneRefObjects.size()) ;
            }
        }
    }
    
	protected boolean testOneRefBNode(RDFNode n)
    {
		if ( ! ( n instanceof Resource ) )
			return false ;

		Resource obj = (Resource)n ;

		if ( ! obj.isAnon() )
            return false ;

        // In a list - done as list, not as embedded bNode.
		if ( rdfListsAll.contains(obj) )
			// RDF list (head or element)
            return false ;

		StmtIterator pointsToIter = obj.getModel().listStatements(null, null, obj) ;
		if ( ! pointsToIter.hasNext() )
			// Corrupt graph!
			throw new JenaException("N3: found object with no arcs!") ;

		Statement s = pointsToIter.nextStatement() ;
               
		if ( pointsToIter.hasNext() )
            return false ;

    	if ( N3JenaWriter.DEBUG )
			out.println("# OneRef: "+formatResource(obj)) ;
		return true ; 
	}
  
    // ----------------------------------------------------
    // Output stage 
    
    // Property order is:
    // 1 - rdf:type (as "a")
    // 2 - other rdf: rdfs: namespace items (sorted)
    // 3 - all other properties, sorted by URI (not qname)  
    

    
    @Override
    protected ClosableIterator<Property> preparePropertiesForSubject(Resource r)
    {
        Set<Property> seen = new HashSet<Property>() ;
        boolean hasTypes = false ;
        SortedMap<String, Property> tmp1 = new TreeMap<String, Property>() ;
        SortedMap<String, Property> tmp2 = new TreeMap<String, Property>() ;
        
        StmtIterator sIter = r.listProperties();
        for ( ; sIter.hasNext() ; )
        {
            Property p = sIter.nextStatement().getPredicate() ;
            if ( seen.contains(p) )
                continue ;
            seen.add(p) ;
            
            if ( p.equals(RDF.type) )
            {
                hasTypes = true ;
                continue ;
            }
            
            if ( p.getURI().startsWith(RDF.getURI()) ||  
                 p.getURI().startsWith(RDFS.getURI()) )
            {
                tmp1.put(p.getURI(), p) ;
                continue ;
            }
            
            tmp2.put(p.getURI(), p) ;        
        }
        sIter.close() ;
        
        ExtendedIterator<Property> eIter = null ;
        
        if ( hasTypes )
            eIter = new SingletonIterator<Property>(RDF.type) ;

        ExtendedIterator<Property> eIter2 = WrappedIterator.create(tmp1.values().iterator()) ;
            
        eIter = (eIter == null) ? eIter2 : eIter.andThen(eIter2) ;
                    
        eIter2 = WrappedIterator.create(tmp2.values().iterator()) ;
        
        eIter = (eIter == null) ? eIter2 : eIter.andThen(eIter2) ;
        return eIter ;
    }
    
    @Override
    protected boolean skipThisSubject(Resource subj)
    {
        return rdfListsAll.contains(subj)   ||
               oneRefObjects.contains(subj)  ;
    }

//    protected void writeModel(Model model)
//	{
//        super.writeModel(model) ;
//
//

    // Before ... 

    @Override
    protected void startWriting()
    {
        allocateDatastructures() ;
    }

    // Flush any unwritten objects.
    // 1 - OneRef objects
    //     Normally there are "one ref" objects left
    //     However loops of "one ref" are possible.
    // 2 - Lists

    @Override
    protected void finishWriting()
    {
 
        // Are there any unattached RDF lists?
        // e..g ([] [] []) . in the data.
        // We missed these earlier.
        for (Iterator<Resource> leftOverIter = rdfLists.iterator(); leftOverIter.hasNext();)
        {
            Resource r = leftOverIter.next();
            if (rdfListsDone.contains(r))
                continue;
            out.println();
            if (N3JenaWriter.DEBUG)
                out.println("# RDF List");

            if (!r.isAnon() || countArcsTo(r) > 0 )
            {
                // Name it.
                out.print(formatResource(r));
                out.print(" :- ");
            }
            writeList(r);
            out.println(" .");
        }

        // Finally, panic.
        // Dump anything that has not been output yet. 
        oneRefObjects.removeAll(oneRefDone);
        for (Iterator<RDFNode> leftOverIter = oneRefObjects.iterator(); leftOverIter.hasNext();)
        {
            out.println();
            if (N3JenaWriter.DEBUG)
                out.println("# One ref");
            // Don't allow further one ref objects to be inlined. 
            allowDeep = false;
            writeOneGraphNode((Resource) leftOverIter.next());
            allowDeep = true;
        }

        //out.println() ;
        //writeModelSimple(model,  bNodesMap, base) ;
        out.flush();
        clearDatastructures() ;
    }



	// Need to decide between one line or many.
    // Very hard to do a pretty thing here because the objects may be large or small or a mix.

    @Override
    protected void writeObjectList(Resource subject, Property property)
    {
//        if ( ! doObjectListsAsLists )
//        {
//            super.writeObjectList(resource, property) ;
//            return ;
//        }

        String propStr = formatProperty(property);
        
        // Find which objects are simple (i.e. not nested structures)             

        StmtIterator sIter = subject.listProperties(property);
        Set<RDFNode> simple = new HashSet<RDFNode>() ;
        Set<RDFNode> complex = new HashSet<RDFNode>() ;

        for (; sIter.hasNext();)
        {
            Statement stmt = sIter.nextStatement();
            RDFNode obj = stmt.getObject() ;
            if ( isSimpleObject(obj) )
                simple.add(obj) ;
            else
                complex.add(obj) ;
        }
        sIter.close() ;
        // DEBUG
        int simpleSize = simple.size() ;
        int complexSize = complex.size() ;
        
        // Write property/simple objects
        
        if ( simple.size() > 0 )
        {
            String padSp = null ;
            // Simple objects - allow property to be long and alignment to be lost
            if ((propStr.length()+minGap) <= widePropertyLen)
                padSp = pad(calcPropertyPadding(propStr)) ;
            
            if ( doObjectListsAsLists )
            {
                // Write all simple objects as one list. 
                out.print(propStr);
                out.incIndent(indentObject) ; 
            
                if ( padSp != null )
                    out.print(padSp) ;
                else
                    out.println() ;
            
                for (Iterator<RDFNode> iter = simple.iterator(); iter.hasNext();)
                {
                    RDFNode n = iter.next();
                    writeObject(n);
                    
                    // As an object list
                    if (iter.hasNext())
                        out.print(objectListSep);
                }
                
                out.decIndent(indentObject) ;
            }
            else
            {
                for (Iterator<RDFNode> iter = simple.iterator(); iter.hasNext();)
                {
                    // This is also the same as the complex case 
                    // except the width the property can go in is different.
                    out.print(propStr);
                    out.incIndent(indentObject) ; 
                    if ( padSp != null )
                        out.print(padSp) ;
                    else
                        out.println() ;
                    
                    RDFNode n = iter.next();
                    writeObject(n);
                    out.decIndent(indentObject) ;
                    
                    // As an object list
                    if (iter.hasNext())
                        out.println(" ;");
                   }
                
            }
        }        
        // Now do complex objects.
        // Write property each time for a complex object.
        // Do not allow over long properties but same line objects.

        if (complex.size() > 0)
        {
            // Finish the simple list if there was one
            if ( simple.size() > 0 )
                out.println(" ;");
            
            int padding = -1 ;
            String padSp = null ;
            
            // Can we fit teh start of teh complex object on this line?
            
            // DEBUG variable.
            int tmp = propStr.length() ;
            // Complex objects - do not allow property to be long and alignment to be lost
            if ((propStr.length()+minGap) <= propertyCol)
            {
                padding = calcPropertyPadding(propStr) ;
                padSp = pad(padding) ;
            }

            for (Iterator<RDFNode> iter = complex.iterator(); iter.hasNext();)
            {
                int thisIndent = indentObject ;
                //if ( i )
                out.incIndent(thisIndent);
                out.print(propStr);
                if ( padSp != null )
                    out.print(padSp) ;
                else
                    out.println() ;
            
                RDFNode n = iter.next();
                writeObject(n);
                out.decIndent(thisIndent);
                if ( iter.hasNext() )
                    out.println(" ;");
            }
        }
        return;
	}


    protected boolean isSimpleObject(RDFNode node)
    {
        if (node instanceof Literal)
            return true ;
        Resource rObj = (Resource) node;
        if ( allowDeep && oneRefObjects.contains(rObj) )
            return false ;
        return true ;
    }

    @Override
    protected void writeObject(RDFNode node)
	{
		if (node instanceof Literal)
		{
			writeLiteral((Literal) node);
			return;
		}

		Resource rObj = (Resource) node;
		if ( allowDeep && ! isSimpleObject(rObj))
		{
			oneRefDone.add(rObj);
	        ClosableIterator<Property> iter = preparePropertiesForSubject(rObj);
	        if (! iter.hasNext() )
	        {
	            // No properties.
	            out.print("[]");
	        }
	        else
	        {
    			out.print("[ ");
    			out.incIndent(2);
    			writePropertiesForSubject(rObj, iter);
                out.decIndent(2);
                out.println() ;
                // Line up []
    			out.print("]");
	        }
	        iter.close();
			return ;
		}

		if (rdfLists.contains(rObj))
			if (countArcsTo(rObj) <= 1)
			{
				writeList(rObj);
				return;
			}

		out.print(formatResource(rObj));
	}



	// Need to out.print in short (all on one line) and long forms (multiple lines)
	// That needs starts point depth tracking.
    protected void writeList(Resource resource)
	{
		out.print( "(");
		out.incIndent(2) ;
		boolean listFirst = true;
		for (Iterator<RDFNode> iter = rdfListIterator(resource); iter.hasNext();)
		{
			if (!listFirst)
				out.print( " ");
			listFirst = false;
			RDFNode n = iter.next();
			writeObject(n) ;
		}
		out.print( ")");
		out.decIndent(2) ;
		rdfListsDone.add(resource);

	}

	// Called before each writing run.
	protected void allocateDatastructures()
	{
		rdfLists 		= new HashSet<Resource>() ;
		rdfListsAll 	= new HashSet<Resource>() ;
		rdfListsDone 	= new HashSet<Resource>() ;
		oneRefObjects 	= new HashSet<RDFNode>() ;
		oneRefDone 		= new HashSet<Resource>() ;
	}

	// Especially release large intermediate memory objects
	protected void clearDatastructures()
	{
		rdfLists 		= null ;
		rdfListsAll 	= null ;
		rdfListsDone 	= null ;
		oneRefObjects 	= null ;
		oneRefDone 		= null ;
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
