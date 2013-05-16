/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: LazyIterator.java,v 1.1 2009/06/29 08:55:49 castagna Exp $
*/

package com.hp.hpl.jena.util.iterator;

/** An ExtendedIterator that is created lazily.
 * This is useful when constructing an iterator is expensive and 
 * you'd prefer to delay doing it until certain it's actually needed.
 * For example, if you have <code>iterator1.andThen(iterator2)</code>
 * you could implement iterator2 as a LazyIterator.  
 * The sequence to be defined is defined by the subclass's definition 
 * of create().  That is called exactly once on the first attempt 
 * to interact with the LazyIterator.  
 * @author jjc, modified to use ExtendedIterators by csayers
 * @version $Revision: 1.1 $
 */
abstract public class LazyIterator<T> implements ExtendedIterator<T> {

	private ExtendedIterator<T> it=null;

	/** An ExtendedIterator that is created lazily. 
	 * This constructor has very low overhead - the real work is 
	 * delayed until the first attempt to use the iterator.
	 */
	public LazyIterator() {
	}

	public boolean hasNext() {
		lazy();
		return it.hasNext();
	}

	public T next() {
		lazy();
		return it.next();
	}

	public void remove() {
		lazy();
		it.remove();
	}

	public ExtendedIterator<T> andThen(ClosableIterator<? extends T> other) {
		lazy();
		return it.andThen(other);
	}

	public ExtendedIterator<T> filterKeep(Filter<T> f) {
		lazy();
		return it.filterKeep(f);
	}

	public ExtendedIterator<T> filterDrop(Filter<T> f) {
		lazy();
		return it.filterDrop(f);
	}

	public <U> ExtendedIterator<U> mapWith(Map1<T,U> map1) {
		lazy();
		return it.mapWith(map1);
	}

	public void close() {
		lazy();
		it.close();
			
	}
	 
	private void lazy() {
		if (it == null)
			it = create();
	}

	/** The subclass must define this to return
	 * the ExtendedIterator to invoke. This method will be
	 * called at most once, on the first attempt to
	 * use the iterator.
	 * From then on, all calls to this will be passed
	 * through to the returned Iterator.
	 * @return The parent iterator defining the sequence.
	 */
	public abstract ExtendedIterator<T> create();

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
