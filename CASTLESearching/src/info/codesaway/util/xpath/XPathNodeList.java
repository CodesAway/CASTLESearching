package info.codesaway.util.xpath;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.NodeList;

/**
 * The Class XPathNodeList.
 *
 * @param <T>
 *            the node type
 */
public class XPathNodeList<T extends XPathNode> implements Iterable<T>, NodeList {
	/** The nodes. */
	private List<T> nodes;

	/**
	 * Instantiates a new xpath node list.
	 *
	 * @param nodes
	 *            the nodes
	 */
	@SafeVarargs
	XPathNodeList(final T... nodes) {
		this(asList(nodes));
	}

	/**
	 * Instantiates a new xpath node list.
	 *
	 * @param nodes
	 *            the nodes
	 */
	public XPathNodeList(final Collection<T> nodes) {
		this.nodes = new ArrayList<>(nodes);
	}

	/**
	 * Instantiates a new xpath node list.
	 *
	 * @param nodeList
	 *            the node list
	 */
	@SuppressWarnings("unchecked")
	private XPathNodeList(final NodeList nodeList) {
		// this.nodes = new XPathNode[nodeList.getLength()];
		this.nodes = new ArrayList<>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			// nodes[i] = XPathNode.valueOf(nodeList.item(i));
			this.nodes.add((T) XPathNode.valueOf(nodeList.item(i)));
		}
	}

	/**
	 * Returns <code>XPathNodeList</code> instance representing the specified
	 * <code>NodeList</code> value.
	 *
	 * @param <T>
	 *            the
	 * @param nodeList
	 *            <code>NodeList</code> value
	 * @return XPathNodeList instance representing nodeList
	 */
	@SuppressWarnings("unchecked")
	public static <T extends XPathNode> XPathNodeList<T> valueOf(final NodeList nodeList) {
		if (nodeList == null) {
			return null;
		} else if (nodeList instanceof XPathNodeList) {
			return (XPathNodeList<T>) nodeList;
		} else {
			return new XPathNodeList<>(nodeList);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			/**
			 * Index of element to be returned by subsequent call to next.
			 */
			int cursor = 0;

			@Override
			public boolean hasNext() {
				return this.cursor != XPathNodeList.this.size();
			}

			@Override
			public T next() {
				try {
					return XPathNodeList.this.get(this.cursor++);
				} catch (IndexOutOfBoundsException e) {
					throw new NoSuchElementException();
				}
			}

			/**
			 * Not supported
			 */
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * The number of nodes in the list. The range of valid child node indices
	 * is 0 to <code>length-1</code> inclusive.
	 *
	 * @return the length
	 * @see #size()
	 */
	@Override
	public int getLength() {
		return this.size();
	}

	/**
	 * The number of nodes in the list. The range of valid child node indices
	 * is 0 to <code>length-1</code> inclusive.
	 *
	 * @return the number of nodes in the list
	 */
	public int size() {
		return this.nodes.size();
	}

	/**
	 * Returns <tt>true</tt> if this list contains no elements.
	 *
	 * @return <tt>true</tt> if this list contains no elements
	 */
	public boolean isEmpty() {
		return this.size() == 0;
	}

	/**
	 * Returns the <code>index</code>th item in the collection. If
	 * <code>index</code> is greater than or equal to the number of nodes in
	 * the list, this returns <code>null</code>.
	 *
	 * @param index
	 *            Index into the collection.
	 * @return The node at the <code>index</code>th position in the
	 *         <code>NodeList</code>, or <code>null</code> if that is not a
	 *         valid
	 *         index.
	 * @see #get(int)
	 */
	@Override
	public T item(final int index) {
		return this.get(index);
	}

	/**
	 * Returns the <code>index</code>th item in the collection. If
	 * <code>index</code> is greater than or equal to the number of nodes in
	 * the list, this returns <code>null</code>.
	 *
	 * @param index
	 *            Index into the collection.
	 * @return The node at the <code>index</code>th position in the
	 *         <code>NodeList</code>, or <code>null</code> if that is not a
	 *         valid
	 *         index.
	 */
	public T get(final int index) {
		return this.nodes.get(index);
	}
}
