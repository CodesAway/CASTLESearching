package info.codesaway.util.xpath;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

/**
 * The Class XPathElement.
 */
public class XPathElement extends XPathNode implements Element {
	/**
	 * Instantiates a new <code>XPathElement</code>.
	 *
	 * @param element
	 *            the element
	 */
	protected XPathElement(final Element element) {
		super(element);
	}

	// Temporarily removed, until I can determine how to set namespace automatically
	// public XPathElement(String tagName)
	// {
	// super(nonexistentDocument.createElement(tagName));
	// }
	//
	// public XPathElement(String tagName, String text)
	// {
	// this(tagName);
	// setTextContent(text);
	// }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathElement cloneNode(final boolean deep) {
		return this.asXPathElement((Element) this.node.cloneNode(deep));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Element getNode() {
		return (Element) super.getNode();
	}

	/**
	 * Returns <tt>XPathElement</tt> instance representing the specified
	 * <tt>Element</tt> value.
	 *
	 * @param element
	 *            <code>Element</code> value
	 * @return XPathElement instance representing element
	 */
	public static XPathElement valueOf(final Element element) {
		if (element == null) {
			return null;
		}

		if (element instanceof XPathElement) {
			return (XPathElement) element;
		} else {
			return new XPathElement(element);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAttribute(final String name) {
		return this.getNode().getAttribute(name);
	}

	/**
	 * Retrieves an attribute value by name.
	 *
	 * <p>For each parent element name, {@link #getChildElement(String)} is invoked on the current element.
	 * If there is no element found (a return value of <code>null</code>), this method returns the empty string (since
	 * the specified attribute doesn't exist).</p>
	 *
	 * <p>After all the parent element names are handled, the {@link #getAttribute(String)} method is called on the
	 * resulting element, and the attribute value is returned.</p>
	 *
	 * @param name
	 *            The name of the attribute to retrieve.
	 * @param parentElementNames
	 *            any parent elements
	 * @return The <code>Attr</code> value as a string, or the empty string
	 *         if that attribute does not have a specified or default value.
	 */
	public String getAttribute(final String name, final String... parentElementNames) {
		XPathElement element = this;

		for (String parentElementName : parentElementNames) {
			element = element.getChildElement(parentElementName);

			if (element == null) {
				return "";
			}
		}

		return element.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAttributeNS(final String namespaceURI, final String localName)
			throws DOMException {
		return this.getNode().getAttributeNS(namespaceURI, localName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr getAttributeNode(final String name) {
		return this.asXPathAttr(this.getNode().getAttributeNode(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr getAttributeNodeNS(final String namespaceURI, final String localName)
			throws DOMException {
		return this.asXPathAttr(this.getNode().getAttributeNodeNS(namespaceURI, localName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNodeList<XPathElement> getElementsByTagName(final String name) {
		return this.asXPathNodeList(this.getNode().getElementsByTagName(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNodeList<XPathElement> getElementsByTagNameNS(final String namespaceURI, final String localName)
			throws DOMException {
		return this.asXPathNodeList(this.getNode().getElementsByTagNameNS(namespaceURI, localName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TypeInfo getSchemaTypeInfo() {
		return this.getNode().getSchemaTypeInfo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTagName() {
		return this.getNode().getTagName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasAttribute(final String name) {
		return this.getNode().hasAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasAttributeNS(final String namespaceURI, final String localName)
			throws DOMException {
		return this.getNode().hasAttributeNS(namespaceURI, localName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttribute(final String name) throws DOMException {
		this.getNode().removeAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeNS(final String namespaceURI, final String localName)
			throws DOMException {
		this.getNode().removeAttributeNS(namespaceURI, localName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr removeAttributeNode(final Attr oldAttr) throws DOMException {
		return this.asXPathAttr(this.getNode().removeAttributeNode(getAttr(oldAttr)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAttribute(final String name, final String value) throws DOMException {
		// String namespaceURI = getNamespaceURI();
		//
		this.getNode().setAttribute(name, value);
		// setAttributeNS(lookupNamespaceURI(getPrefix(name)), name, value);
		// setAttributeNS(getNamespaceURI(getPrefix(name)), name, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAttributeNS(final String namespaceURI, final String qualifiedName,
			final String value) throws DOMException {
		this.getNode().setAttributeNS(namespaceURI, qualifiedName, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr setAttributeNode(final Attr newAttr) throws DOMException {
		return this.asXPathAttr(this.getNode().setAttributeNode(getAttr(newAttr)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr setAttributeNodeNS(final Attr newAttr) throws DOMException {
		return this.asXPathAttr(this.getNode().setAttributeNodeNS(getAttr(newAttr)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIdAttribute(final String name, final boolean isId) throws DOMException {
		this.getNode().setIdAttribute(name, isId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIdAttributeNS(final String namespaceURI, final String localName,
			final boolean isId) throws DOMException {
		this.getNode().setIdAttributeNS(namespaceURI, localName, isId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIdAttributeNode(final Attr idAttr, final boolean isId)
			throws DOMException {
		this.getNode().setIdAttributeNode(getAttr(idAttr), isId);
	}

	/**
	 * Escapes xml characters in <code>input</code>: &amp; &lt; &gt;.
	 * These characters are the only characters which are required to be escaped in an element node.
	 *
	 * <p>Note that when using the methods of this class, escaping is not necessary, and is done for you. This method
	 * is provided for cases when you need to escape XML, but are not storing the value in an
	 * <code>XPathElement</code>.</p>
	 *
	 * @param input
	 *            the input
	 * @return the string
	 */
	public static String escapeXML(final String input) {
		checkNull("input", input);

		StringBuilder output = new StringBuilder();
		int start = 0;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			String escapedString;

			if (c == '&') {
				escapedString = "&amp;";
			} else if (c == '<') {
				escapedString = "&lt;";
			} else if (c == '>') {
				escapedString = "&gt;";
			} else {
				continue;
			}

			output.append(input, start, i);
			start = i + 1;
			output.append(escapedString);
		}

		// No characters need to be escaped
		if (start == 0) {
			return input;
		}

		return output.append(input, start, input.length()).toString();
	}
}
