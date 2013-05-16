/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       ian_dickinson@users.sourceforge.net
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            05-Jun-2003
 * Filename           $RCSfile: ResourceUtils.java,v $
 * Revision           $Revision: 1.2 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2009/10/06 13:04:43 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (see footer for full conditions)
 *****************************************************************************/

// Package
///////////////
package com.hp.hpl.jena.util;



// Imports
///////////////
import java.util.*;

import com.hp.hpl.jena.rdf.model.*;



/**
 * <p>
 * General utility methods that operate on RDF resources, but which are not specific
 * to a given model.
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:ian_dickinson@users.sourceforge.net" >email</a>)
 * @version CVS $Id: ResourceUtils.java,v 1.2 2009/10/06 13:04:43 ian_dickinson Exp $
 */
public class ResourceUtils {

    /**
     * <p>
     * Answer the maximal lower elements of the given collection, given the partial
     * ordering <code>rel</code>. See {@link #maximalLowerElements( Iterator, Property, boolean )}
     * for details.
     * </p>
     *
     * @param resources A collection of resources
     * @param rel A property defining a partial-ordering on <code>resources</code>
     * @param inverse If true, we invert the given property (by reversing the order
     * of the arguments), which allows us to use eg subClassOf as a partial order
     * operator for both sub-class and super-class relationships
     * @return The collection that contains only those <code>resources</code> are not
     * greater than another resource under the partial order.
     */
    public static <T extends Resource> List<T> maximalLowerElements( Collection<T> resources, Property rel, boolean inverse ) {
        return maximalLowerElements( resources.iterator(), rel, inverse );
    }

    /**
     * <p>
     * Given a collection of resources, and a relation defining a partial order over
     * those resources, answer the sub-collection that contains only those elements
     * that appear in the maximal generator of the relation.  Specifically, a resource
     * <code>x</code> is excluded from the return value if there is another resource
     * <code>y</code> in the input collection such that <code>y&nbsp;Rel&nbsp;x</code> holds.
     * </p>
     *
     * @param resources An iterator over a collection of resources
     * @param rel A property defining a partial-ordering on <code>resources</code>
     * @param inverse If true, we invert the given property (by reversing the order
     * of the arguments), which allows us to use eg subClassOf as a partial order
     * operator for both sub-class and super-class relationships
     * @return The list that contains only those <code>resources</code> are not
     * greater than another resource under the partial order.
     */
    public static <T extends Resource> List<T> maximalLowerElements( Iterator<T> resources, Property rel, boolean inverse ) {
        List<T> in = new ArrayList<T>();
        List<T> out = new ArrayList<T>();
        List<T> drop = new ArrayList<T>();

        while (resources.hasNext()) {
            in.add( resources.next() );
        }

        while (! in.isEmpty()) {
            T r = in.remove( 0 );
            boolean rCovered = testResourceCovered( in, rel, inverse, r ) ||
                               testResourceCovered( out, rel, inverse, r ) ||
                               testResourceCovered( drop, rel, inverse, r );

            // if r is not covered by another resource, we can add it to the output
            (rCovered ? drop : out).add( r );
        }

        return out;
    }

    private static boolean testResourceCovered( List< ? extends Resource> l, Property rel, boolean inverse, Resource r ) {
        boolean rCovered = false;
        for (Iterator< ? extends Resource> i = l.iterator();  !rCovered && i.hasNext(); ) {
            Resource next = i.next();
            rCovered = inverse ? r.hasProperty( rel, next ) : next.hasProperty( rel, r );
        }
        return rCovered;
    }


    /**
     * <p>Remove from the given list l of {@link Resource Resources}, any Resource that is equivalent
     * to the reference resource <code>ref</code> under the relation <code>p</code>. Typically,
     * <code>p</code> will be <code>owl:subClassOf</code> or <code>owl:subPropertyOf</code>
     * or some similar predicate.  A resource R is defined to be equivalent to <code>ref</code>
     * iff <code>R&nbsp;p&nbsp;ref</code> is true <em>and</em> <code>ref&nbsp;p&nbsp;R</code> is true.
     * </p>
     * <p>The equivalent resources are removed from list <code>l</code>
     * </em>in place</em>, the return value is the list of <em>removed</em> resources.</p>
     * @param l A list of resources from which the resources equivalent to ref will be removed
     * @param p An equivalence predicate
     * @param ref A reference resource
     * @return A list of the resources removed from the parameter list l
     */
    public static <T extends Resource> List<T> removeEquiv( List<T> l, Property p, Resource ref ) {
        List<T> equiv = new ArrayList<T>();

        for (Iterator<T> i = l.iterator(); i.hasNext(); ) {
            T r = i.next();

            if (r.hasProperty( p, ref ) && ref.hasProperty( p, r )) {
                // resource r is equivalent to the reference resource
                equiv.add( r );
            }
        }

        l.removeAll( equiv );
        return equiv;
    }


    /**
     * <p>Answer a list of lists, which is a partition of the given
     * input list of resources.  The equivalence relation is the predicate p.
     * So, two resources <code>a</code> and <code>b</code>
     * will be in the same partition iff
     * <code>(a p b) && (b p a)</code>.</p>
     * @param <T>
     * @param l A list of resources
     * @param p An equivalence predicate
     * @return A list of lists which are the partitions of <code>l</code>
     * under <code>p</code>
     */
    public static <T extends Resource> List<List<T>> partition( List<T> l, Property p ) {
        // first copy the input so we can mess with it
        List<T> source = new ArrayList<T>();
        source.addAll( l );
        List<List<T>> parts = new ArrayList<List<T>>();

        while (!source.isEmpty()) {
            // each step through the loop we pick a random element, and
            // create a list of that element and all its equivalent values
            T seed = source.remove( 0 );
            List<T> part = removeEquiv( source, p, seed );
            part.add( seed );

            // add to the partition list
            parts.add( part );
        }

        return parts;
    }


    /**
     * <p>Answer a new resource that occupies the same position in the graph as the current
     * resource <code>old</code>, but that has the given URI.  In the process, the existing
     * statements referring to <code>old</code> are removed.  Since Jena does not allow the
     * identity of a resource to change, this is the closest approximation to a rename operation
     * that works.
     * </p>
     * <p><strong>Notes:</strong> This method does minimal checking, so renaming a resource
     * to its own URI is unpredictable.  Furthermore, it is a general and simple approach, and
     * in given applications it may be possible to do this operation more efficiently. Finally,
     * if <code>res</code> is a property, existing statements that use the property will not
     * be renamed, nor will occurrences of <code>res</code> in other models.
     * </p>
     * @param old An existing resource in a given model
     * @param uri A new URI for resource old, or null to rename old to a bNode
     * @return A new resource that occupies the same position in the graph as old, but which
     * has the new given URI.
     */
    public static Resource renameResource( Resource old, String uri ) {
        Model m = old.getModel();
        List<Statement> stmts = new ArrayList<Statement>();

        // list the statements that mention old as a subject
        for (Iterator<Statement> i = old.listProperties();  i.hasNext(); stmts.add( i.next() ) ) {}

        // list the statements that mention old an an object
        for (Iterator<Statement> i = m.listStatements( null, null, old );  i.hasNext();  stmts.add( i.next() ) ) {}

        // create a new resource to replace old
        Resource res = (uri == null) ? m.createResource() : m.createResource( uri );

        // now move the statements to refer to res instead of old
        for (Iterator<Statement> i = stmts.iterator(); i.hasNext(); ) {
            Statement s = i.next();

            s.remove();

            Resource subj = s.getSubject().equals( old ) ? res : s.getSubject();
            RDFNode obj = s.getObject().equals( old ) ? res : s.getObject();

            m.add( subj, s.getPredicate(), obj );
        }

        return res;
    }


    /**
     * <p>Answer a model that contains all of the resources reachable from a given
     * resource by any property, transitively.  The returned graph is the sub-graph
     * of the parent graph of root, whose root node is the given root. Cycles are
     * permitted in the sub-graph.</p>
     * @param root The root node of the sub-graph to extract
     * @return A model containing all reachable RDFNodes from root by any property.
     */
    public static Model reachableClosure( Resource root ) {
        Model m = ModelFactory.createDefaultModel();

        // set of resources we have passed through already (i.e. the occurs check)
        Set<Resource> seen = CollectionFactory.createHashedSet();

        // queue of resources we have not yet visited
        List<RDFNode> queue = new LinkedList<RDFNode>();
        queue.add( root );

        while (!queue.isEmpty()) {
            Resource r = (Resource) queue.remove( 0 );

            // check for multiple paths arriving at this queue node
            if (!seen.contains( r )) {
                seen.add( r );

                // add the statements to the output model, and queue any new resources
                for (StmtIterator i = r.listProperties(); i.hasNext(); ) {
                    Statement s = i.nextStatement();

                    // don't do the occurs check now in case of reflexive statements
                    m.add( s );

                    if (s.getObject() instanceof Resource) {
                        queue.add( s.getObject() );
                    }
                }
            }
        }

        return m;
    }


    // Internal implementation methods
    //////////////////////////////////

    //==============================================================================
    // Inner class definitions
    //==============================================================================

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
