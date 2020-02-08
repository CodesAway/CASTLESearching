package info.codesaway.util.xpath;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.TypeInfo;

/**
 * The Class XPathAttr.
 */
public class XPathAttr extends XPathNode implements Attr {
	/**
	 * Instantiates a new x path attr.
	 *
	 * @param attribute
	 *            the attribute
	 */
	protected XPathAttr(final Attr attribute) {
		super(attribute);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Attr getNode() {
		return (Attr) super.getNode();
	}

	/**
	 * Returns <tt>XPathAttr</tt> instance representing the specified
	 * <tt>Attr</tt> value.
	 *
	 * @param attribute
	 *            <code>Attr</code> value
	 * @return <code>XPathAttr</code> instance representing <code>attribute</code>
	 */
	public static XPathAttr valueOf(final Attr attribute) {
		if (attribute == null) {
			return null;
		}

		if (attribute instanceof XPathAttr) {
			return (XPathAttr) attribute;
		} else {
			return new XPathAttr(attribute);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.getNode().getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getSpecified() {
		return this.getNode().getSpecified();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return this.getNode().getValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValue(final String value) throws DOMException {
		this.getNode().setValue(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathElement getOwnerElement() {
		return this.asXPathElement(this.getNode().getOwnerElement());
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
	public boolean isId() {
		return this.getNode().isId();
	}
}
