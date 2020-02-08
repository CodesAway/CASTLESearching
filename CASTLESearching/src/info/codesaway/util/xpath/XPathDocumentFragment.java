package info.codesaway.util.xpath;

import org.w3c.dom.DocumentFragment;

/**
 * The Class XPathDocumentFragment.
 */
public class XPathDocumentFragment extends XPathNode implements DocumentFragment {
	/**
	 * Instantiates a blank xpath document fragment.
	 *
	 */
	public XPathDocumentFragment() {
		// TODO: verify this works
		super(new XPathDocument().createDocumentFragment());
	}

	/**
	 * Instantiates a new xpath document fragment.
	 *
	 * @param documentFragment
	 *            the document fragment
	 */
	protected XPathDocumentFragment(final DocumentFragment documentFragment) {
		super(documentFragment);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DocumentFragment getNode() {
		return (DocumentFragment) super.getNode();
	}

	/**
	 * Returns <tt>XPathDocumentFragment</tt> instance representing the specified
	 * <tt>DocumentFragment</tt> value.
	 *
	 * @param documentFragment
	 *            <code>DocumentFragment</code> value
	 * @return <code>XPathDocumentFragment</code> instance representing <code>documentFragment</code>
	 */
	public static XPathDocumentFragment valueOf(final DocumentFragment documentFragment) {
		if (documentFragment == null) {
			return null;
		}

		if (documentFragment instanceof XPathDocumentFragment) {
			return (XPathDocumentFragment) documentFragment;
		} else {
			return new XPathDocumentFragment(documentFragment);
		}
	}
}
