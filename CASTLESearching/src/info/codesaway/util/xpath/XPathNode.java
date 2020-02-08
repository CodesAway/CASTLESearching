package info.codesaway.util.xpath;

import static info.codesaway.util.regex.Pattern.compile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

// source: http://www.roseindia.net/tutorials/xPath/java-xpath.shtml
/**
 * The Class XPathNode.
 */
public class XPathNode implements Node {
	// TODO: replace deprecated classes
	// https://stackoverflow.com/a/8257906/12610042
	// import org.apache.xml.serialize.OutputFormat;
	// import org.apache.xml.serialize.XMLSerializer;

	/** The xpath. */
	// protected XPath xpath;

	// private static final XPathFactory xpathFactory =
	// XPathFactory.newInstance();

	protected XPathDocument rootDocument;

	/** The node. */
	protected final Node node;

	/** Line separator used when outputting the xml as text */
	private static final String lineSeparator = System.getProperty("line.separator");

	private static final int Indent = 4;
	private static final int LineWidth = 72;

	/**
	 * Instantiates a new xpath node.
	 *
	 * @param node
	 *            the node
	 */
	public XPathNode(final Node node) {
		this.node = node;
		// xpath = xpathFactory.newXPath();
	}

	/**
	 * Gets the node.
	 *
	 * @return the node
	 */
	public Node getNode() {
		return this.node;
	}

	/**
	 * Unwraps the specified node, getting the internal {@link Node}.
	 *
	 * <p>
	 * If the passed node is an {@link XPathNode}, this method's return is the
	 * return of the {@link #getNode()} method. Otherwise, the passed node is
	 * returned.
	 * </p>
	 *
	 * @param node
	 *            the node
	 * @return If the passed node is an {@link XPathNode}, this method's return
	 *         is the return of the {@link #getNode()} method. Otherwise, the
	 *         passed node is returned.
	 */
	public static Node getNode(Node node) {
		if (node instanceof XPathNode) {
			node = ((XPathNode) node).getNode();
		}

		return node;
	}

	/**
	 * Unwraps the specified attribute, getting the internal {@link Attr}.
	 *
	 * <p>
	 * If the passed node is an {@link XPathAttr}, this method's return is the
	 * return of the {@link #getNode()} method. Otherwise, the passed attribute
	 * is returned.
	 * </p>
	 *
	 * @param attribute
	 *            the attribute
	 * @return If the passed attribute is an {@link XPathAttr}, this method's
	 *         return is the return of the {@link #getNode()} method. Otherwise,
	 *         the passed attribute is returned.
	 */
	public static Attr getAttr(Attr attribute) {
		if (attribute instanceof XPathAttr) {
			attribute = ((XPathAttr) attribute).getNode();
		}

		return attribute;
	}

	/**
	 * Returns an <tt>XPathNode</tt> instance representing the specified
	 * <tt>Node</tt> value.
	 *
	 * @param node
	 *            the node
	 * @return an <tt>XPathNode</tt> instance representing <tt>node</tt>.
	 */
	public static XPathNode valueOf(final Node node) {
		if (node == null) {
			return null;
		}

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			return XPathElement.valueOf((Element) node);
		}

		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return XPathDocument.valueOf((Document) node);
		}

		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			return XPathAttr.valueOf((Attr) node);
		}

		if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
			return XPathDocumentFragment.valueOf((DocumentFragment) node);
		}

		if (node instanceof XPathNode) {
			return (XPathNode) node;
		} else {
			return new XPathNode(node);
		}
	}

	/**
	 * Transfers settings from this node to the passed node.
	 *
	 * @param <T>
	 *            the xpath node type
	 * @param node
	 *            the node
	 * @return the passed node, with the settings from this node transfered to
	 *         it
	 */
	@Nullable
	protected <T extends XPathNode> T transferSettingsTo(final T node) {
		if (node == null) {
			return null;
		}

		// node.xpath = this.xpath;

		// if (node.getNodeType() != Node.DOCUMENT_NODE)

		node.rootDocument = node instanceof XPathDocument ? (XPathDocument) node : this.rootDocument;

		// NamespaceContext nsContext = getNamespaceContext();
		// if (nsContext != null)
		// setNamespaceContext(nsContext);

		return node;
	}

	/**
	 * Creates an XPathNode using the specified node, inheriting properties from
	 * this XPathNode.
	 *
	 * @param node
	 *            the node
	 * @return the xpath node
	 */
	public XPathNode asXPathNode(final Node node) {
		return this.transferSettingsTo(valueOf(node));
	}

	/**
	 * Creates an XPathNodeList, converting each <code>Node</code> in
	 * <code>nodeList</code> to an <code>XPathNode</code> where each inherits
	 * properties from this XPathNode.
	 *
	 * @param <T>
	 *            the node type
	 * @param nodeList
	 *            the node list
	 * @return the xpath node list
	 */
	@SuppressWarnings("unchecked")
	public <T extends XPathNode> XPathNodeList<T> asXPathNodeList(final NodeList nodeList) {
		if (nodeList == null) {
			return null;
		} else if (nodeList instanceof XPathNodeList) {
			return (XPathNodeList<T>) nodeList;
		}

		// XPathNode[] nodes = new XPathNode[nodeList.getLength()];
		List<T> nodes = new ArrayList<>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			// nodes[i] = asXPathNode(nodeList.item(i));
			nodes.add((T) this.asXPathNode(nodeList.item(i)));
		}

		return new XPathNodeList<>(nodes);
	}

	/**
	 * Creates an XPathDocument using the specified document, inheriting
	 * properties from this XPathNode.
	 *
	 * @param document
	 *            the document
	 * @return the xpath document
	 */
	public XPathDocument asXPathDocument(final Document document) {
		return this.transferSettingsTo(XPathDocument.valueOf(document));
	}

	/**
	 * Creates an XPathElement using the specified element, inheriting
	 * properties from this XPathNode.
	 *
	 * @param element
	 *            the element
	 * @return the xpath element
	 */
	public XPathElement asXPathElement(final Element element) {
		return this.transferSettingsTo(XPathElement.valueOf(element));
	}

	/**
	 * Creates an XPathAttr using the specified attribute, inheriting properties
	 * from this XPathNode.
	 *
	 * @param attribute
	 *            the attribute
	 * @return the xpath attr
	 */
	public XPathAttr asXPathAttr(final Attr attribute) {
		return this.transferSettingsTo(XPathAttr.valueOf(attribute));
	}

	/**
	 * Creates an XPathDocumentFragment using the specified document fragment,
	 * inheriting properties from this XPathNode.
	 *
	 * @param documentFragment
	 *            the document fragment
	 * @return the xpath document fragment
	 */
	public XPathDocumentFragment asXPathDocumentFragment(final DocumentFragment documentFragment) {
		return this.transferSettingsTo(XPathDocumentFragment.valueOf(documentFragment));
	}

	/**
	 * Gets the xpath object used to run xpath queries. If it does not exist, it
	 * will be created first.
	 *
	 * @return the xpath object used to run xpath queries
	 */
	protected XPath getXPath() {
		return this.getRootDocument().getXPath();
		// return xpath;
	}

	/**
	 * Fix default namespace. Java requires the namespace to be mentioned even
	 * if the namespace is blank for
	 *
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the string
	 */
	private static String fixDefaultNamespace(final String xpathExpression) {
		// if (xpathExpression.startsWith("row[@"))
		// {
		// System.out.println("Check expr: " + xpathExpression);
		// System.out.println(getXML());
		//
		// // throw new IllegalArgumentException();
		// }
		//
		// Node useNode = (getNodeType() == DOCUMENT_NODE ? node.getFirstChild()
		// : node);
		// NamedNodeMap attributes = null;
		//
		// int attributesLength = useNode == null ||
		// (attributes = useNode.getAttributes()) == null
		// ? 0 : attributes
		// .getLength();
		//
		// // No namespaces
		// if (attributes == null || attributesLength == 0)
		// return xpathExpression;
		//
		// System.out.println("Attributes for: " + xpathExpression);
		//
		// Matcher attributeMatcher = namespaceAttributeRegEx.matcher("");
		// boolean hasNamespace = false;
		//
		// for (int i = 0; i < attributes.getLength(); i++) {
		// attributeMatcher.reset(attributes.item(i).getNodeName());
		//
		// if (attributeMatcher.matches() && !attributeMatcher.isEmpty("xmlns"))
		// {
		// hasNamespace = true;
		// break;
		// }
		// }
		//
		// System.out.println("hasNamespace: " + hasNamespace);
		//
		// if (!hasNamespace)
		// return xpathExpression;
		//
		// System.out.println("End of attributes\n");

		StringBuffer sb = new StringBuffer();

		Pattern pattern = compile("self::(?&elementName)|" + "(?<attribute>@)?(?:(?<namespace>(?&elementName)?):)?"
				+ "(?:" + "(?('attribute')(?<attributeName>(?&elementName))|(*FAIL))|"
				+ "(?<function>(?&elementName)\\x28)|" + "(?J:(?<literalQuote>')|(?<literalQuote>\"))"
				+ "(?<literal>(?(literalQuote[1])[^']*+|[^\"]*+))\\k<literalQuote>|"
				+ "(?<elementName>[A-Za-z_.-][\\w.-]*+)" + ")");

		Matcher matcher = pattern.matcher(xpathExpression);

		while (matcher.find()) {
			if (matcher.matched("elementName") && !matcher.matched("namespace")) {
				matcher.appendReplacement(sb, ":${0}");
				// else
				// matcher.appendReplacement(sb, "${0}");
			}
		}

		return matcher.appendTail(sb).toString();
	}

	// public XPathDocument getDocument()
	// {
	// // TODO: determine whether document can be changed
	// // if (document != null)
	// // return document;
	//
	// return asXPathDocument((node instanceof Document) ? (Document) node :
	// getOwnerDocument());
	// // document.updateNamespaceContext(getNamespaceContext());
	// // return document;
	// }

	/**
	 * Gets the 1-based occurrence of this node's name for the current branch
	 * (that is, nodes that share this node's parent).
	 *
	 * <p>
	 * As an example, if this node's xpath is ".../element[5]", this method
	 * would return 5, since this node is the 5th occurrence of the "element"
	 * node, for this branch.
	 * </p>
	 *
	 * <p>
	 * If this node is a document or an attribute node, 1 is returned. Note that
	 * only one occurrence can occur for a document or an attribute node.
	 * </p>
	 *
	 * <p>
	 * <b>Usage example</b>: The return value for this method can be used when
	 * iterating over a node list contain elements which share the same name and
	 * parent.
	 * </p>
	 *
	 * <p>
	 * <b>Example</b>:
	 * </p>
	 * 
	 * <pre>
	 * <code> for (XPathNode filename : destination.xpathList("files/name"))
	 * {
	 * &nbsp; &nbsp; <span style="color: green;">// Outputs each filename, for example:</span>
	 * &nbsp; &nbsp; <span style="color: green;">// "1: /root/.../files/name[1]"</span>
	 * &nbsp; &nbsp; System.out.println(filename.getNodeIndex() + ": " + filename.xpath());
	 * }</code>
	 * </pre>
	 *
	 * @return the node index
	 */
	// public int getNodeIndex()
	// {
	// Removed due to HORRIBLE performance
	// if (node.getNodeType() == DOCUMENT_NODE)
	// return 1;
	//
	// Node currentNode = node;
	// String name = currentNode.getNodeName();
	//
	// if (currentNode.getNodeType() == ATTRIBUTE_NODE) {
	// return 1;
	// } else {
	// int occurrence = 1;
	// Node siblingNode = currentNode;
	//
	// while ((siblingNode = siblingNode.getPreviousSibling()) != null) {
	// if (siblingNode.getNodeType() != ELEMENT_NODE)
	// continue;
	//
	// // System.out.println(siblingNode.getNodeName());
	// if (name.equals(siblingNode.getNodeName()))
	// occurrence++;
	// }
	//
	// return occurrence;
	// }
	// }

	/**
	 * Returns the xpath for this XPathNode.
	 *
	 * <p>
	 * For each part, if there are multiple occurrences of the element, the
	 * index for the element will be inside square brackets. If there is only
	 * one occurrence of the element, the index will be omitted. This structure
	 * makes the returned xpath unique for the node - each returned xpath
	 * specifies exactly one node, and each node has only one xpath result.
	 *
	 * <p>
	 * In addition to the uniqueness, the returned xpath gives information about
	 * whether each part is uniquely named. For each part, if an index is
	 * specified (even if it is 1), that element name exists multiple times in
	 * the same "branch". If no index is specified, the part's name is unique
	 * for the "branch".
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> return signifies that the XPathNode is a Document
	 * Node.
	 * </p>
	 *
	 * @return the xpath for this node.
	 */
	public String xpath() {
		if (this.node.getNodeType() == DOCUMENT_NODE) {
			return null;
		}

		Node currentNode = this.node;
		StringBuilder nodeXPath = new StringBuilder();

		do {
			String name = currentNode.getNodeName();

			// System.out.println("Current: " + name);
			//
			// switch (currentNode.getNodeType()) {
			// case ELEMENT_NODE:
			// System.out.println("Type: element");
			// break;
			// case ATTRIBUTE_NODE:
			// System.out.println("Type: attribute");
			// break;
			// default:
			// break;
			// }

			String part;

			if (currentNode.getNodeType() == ATTRIBUTE_NODE) {
				Attr attr = (Attr) currentNode;
				currentNode = attr.getOwnerElement();
				part = "/@" + name;
			} else {
				int occurrence = 1;
				Node siblingNode = currentNode;

				while ((siblingNode = siblingNode.getPreviousSibling()) != null) {
					if (siblingNode.getNodeType() != ELEMENT_NODE) {
						continue;
					}

					// System.out.println(siblingNode.getNodeName());
					if (name.equals(siblingNode.getNodeName())) {
						occurrence++;
					}
				}

				int count = occurrence;
				siblingNode = currentNode;

				while ((siblingNode = siblingNode.getNextSibling()) != null) {
					if (siblingNode.getNodeType() != ELEMENT_NODE) {
						continue;
					}

					if (name.equals(siblingNode.getNodeName())) {
						count++;
					}
				}

				currentNode = currentNode.getParentNode();

				part = "/" + name + (count != 1 ? "[" + occurrence + "]" : "");
			}

			nodeXPath.insert(0, part);
			// System.out.println("---");

		} while (currentNode != null && currentNode.getNodeType() != DOCUMENT_NODE);

		if (currentNode == null && !isEmpty(nodeXPath) && nodeXPath.charAt(0) == '/') {
			// the node is relative - not attached to a root document
			// return as a relative path, not absolute (exclude leading '/')
			return nodeXPath.substring(1);
		}

		return nodeXPath.toString();
	}

	/**
	 * Runs an xpath query, returning an <code>XPathNode</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>XPathNode</code>
	 */
	public XPathNode xpath(final String xpathExpression) {
		return this.asXPathNode((Node) this.xpath(xpathExpression, XPathConstants.NODE));
	}

	/**
	 * Runs an xpath query, returning an <code>XPathElement</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>XPathElement</code>
	 */
	public XPathElement xpathElement(final String xpathExpression) {
		return (XPathElement) this.xpath(xpathExpression);
	}

	/**
	 * Runs an xpath query
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @param returnType
	 *            the return type
	 * @return the returned object
	 */
	public Object xpath(String xpathExpression, final QName returnType) {
		// System.out.println("Before xpath: " + xpathExpression);
		// if (!xpathExpression.equals("row[@r='5']"))

		// XXX: does not handle case of namespace changes (or gets declared) at
		// sub element (not at root)

		xpathExpression = this.fixupXPath(xpathExpression);

		// System.out.println("After xpath: " + xpathExpression);
		// else
		// xpathExpression = "self::node()/:row[@r='5']";

		// System.out.println(xpathExpression);
		try {
			// XPathExpression expression = getXPath().compile(xpathExpression);
			// return expression.evaluate(this.node, returnType);

			return this.getXPath().evaluate(xpathExpression, this.node, returnType);

			// return xpath.evaluate(xpathExpression, this.node, returnType);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Runs an xpath query, returning an <code>XPathNodeList</code>
	 *
	 * @param <T>
	 *            the node type for the returned list
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>XPathNodeList</code>
	 */
	public <T extends XPathNode> XPathNodeList<T> xpathList(final String xpathExpression) {
		return this.asXPathNodeList((NodeList) this.xpath(xpathExpression, XPathConstants.NODESET));
	}

	/**
	 * Runs an xpath query, returning a <code>Double</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>Double</code>
	 */
	public Double xpathDouble(final String xpathExpression) {
		String text = this.xpathString(xpathExpression);
		return text == null ? null : Double.parseDouble(text);
		// return (Double) xpath(xpathExpression, XPathConstants.NUMBER);
	}

	/**
	 * Runs an xpath query, returning an <code>Integer</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>Integer</code>
	 */
	public Integer xpathInt(final String xpathExpression) {
		String text = this.xpathString(xpathExpression);
		return text == null ? null : Integer.parseInt(text);
	}

	/**
	 * Runs an xpath query, returning a <code>Long</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>Long</code>
	 */
	public Long xpathLong(final String xpathExpression) {
		String text = this.xpathString(xpathExpression);
		return text == null ? null : Long.parseLong(text);
	}

	/**
	 * Runs an xpath query, returning a <code>String</code>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the returned <code>String</code>
	 */
	public String xpathString(final String xpathExpression) {
		return nodeString(this.xpath(xpathExpression));
		// return node == null ? null : node.getTextContent();
		// return (String) xpath(xpathExpression, XPathConstants.STRING);
	}

	/**
	 * Ensures the result of the xpath query is not <code>null</code> or the
	 * empty string.
	 *
	 * <p>
	 * If the result is <code>null</code>, a <code>NullPointerException</code>
	 * is thrown, whose message mentions <code>xpathExpression</code> and the
	 * xpath for this node.
	 * </p>
	 *
	 * <p>
	 * If the result is the empty string (length 0), an
	 * <code>IllegalArgumentException</code> is thrown, whose message mentions
	 * <code>xpathExpression</code> and the xpath for this node.
	 * </p>
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the result of the xpath query (calling the
	 *         {@link #xpathString(String)} method)
	 * @throws NullPointerException
	 *             If the result is <code>null</code>
	 * @throws IllegalArgumentException
	 *             If the result is the empty string (length 0)
	 */
	public String checkXPathString(final String xpathExpression) {
		String xpath = this.xpath();

		return checkEmpty(xpath != null ? xpath : "" + (!xpathExpression.startsWith("/") ? "/" : "") + xpathExpression,
				this.xpathString(xpathExpression));
	}

	/**
	 * Ensures a parameter is not <code>null</code>.
	 *
	 * <p>
	 * If <code>parameter</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown, whose message mentions the
	 * parameter name.
	 * </p>
	 *
	 * @param <T>
	 *            the parameter type
	 * @param parameterName
	 *            the parameter name
	 * @param parameter
	 *            the parameter
	 * @return the passed <code>parameter</code>
	 * @throws NullPointerException
	 *             If <code>parameter</code> is <code>null</code>
	 */
	static <T> T checkNull(final String parameterName, final T parameter) {
		if (parameter == null) {
			throw new NullPointerException(isEmpty(parameterName) ? null : "\"" + parameterName + "\" cannot be null");
		}

		return parameter;
	}

	/**
	 * Ensures a parameter is not <code>null</code> or the empty string.
	 *
	 * <p>
	 * If <code>parameter</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown, whose message mentions the
	 * parameter name.
	 * </p>
	 *
	 * <p>
	 * If <code>parameter</code> is the empty string (length 0), an
	 * <code>IllegalArgumentException</code> is thrown, whose message mentions
	 * the parameter name.
	 * </p>
	 *
	 * @param <S>
	 *            the <code>CharSequence</code> type
	 * @param parameterName
	 *            the parameter name
	 * @param parameter
	 *            the parameter
	 * @return the passed <code>parameter</code>
	 * @throws NullPointerException
	 *             If <code>parameter</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             If <code>parameter</code> is the empty string (length 0)
	 */
	static <S extends CharSequence> S checkEmpty(final String parameterName, final S parameter) {
		checkNull(parameterName, parameter);

		if (parameter.length() == 0) {
			throw new IllegalArgumentException("\"" + (isEmpty(parameterName) ? "charSequence" : parameterName)
					+ "\" cannot be the empty string (length 0)");
		}

		return parameter;
	}

	/**
	 * Checks if is <code>null</code> or the empty string.
	 *
	 * @param charSequence
	 *            the char sequence
	 * @return <code>true</code>, if is <code>null</code> or the empty string
	 */
	static boolean isEmpty(final CharSequence charSequence) {
		return charSequence == null || charSequence.length() == 0;
	}

	/**
	 * Returns the string value for the node, (via the
	 * {@link Node#getTextContent()} method).
	 *
	 * @param node
	 *            the node
	 *
	 * @return the string value for the node, or <code>null</code> if
	 *         <code>node</code> is <code>null</code>
	 */
	public static String nodeString(final Node node) {
		return node == null ? null : node.getTextContent();
	}

	/**
	 * Checks if a node which matches the specified expression exists.
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return <code>true</code> if and only if a node which matches the
	 *         expression exists
	 */
	public Boolean xpathBoolean(final String xpathExpression) {
		return (Boolean) this.xpath(xpathExpression, XPathConstants.BOOLEAN);
	}

	/**
	 * Indicates whether the value for the evaluated xpath expression is "true"
	 * or "false".
	 *
	 * <p>
	 * List of "false" values (all others are considered true) -
	 * case-insensitive:
	 * </p>
	 *
	 * <ul>
	 * <li><code>null</code></li>
	 * <li>"" (the empty string)</li>
	 * <li>"0"</li>
	 * <li>"false" (or "f")</li>
	 * <li>"no" (or "n")</li>
	 * </ul>
	 *
	 * <p>
	 * This method is a convenience method for {@link #trueFalse(String)
	 * trueFalse}(xpathString(xpathExpression))
	 * </p>
	 *
	 * @param xpathExpression
	 *            The XPath expression.
	 * @return <code>false</code> if the value is listed in the above list of
	 *         "false" values; otherwise, <code>true</code>.
	 */
	public boolean xpathTrueFalse(final String xpathExpression) {
		return trueFalse(this.xpathString(xpathExpression));
	}

	/**
	 * Indicates whether the value for the evaluated xpath expression is "true"
	 * or "false".
	 *
	 * <p>
	 * If the result of the xpath query is <code>null</code> or the empty
	 * string, <code>defaultValue</code> is returned.
	 * </p>
	 *
	 * <p>
	 * Otherwise, these are the list of "false" values (all others are
	 * considered true) - case-insensitive:
	 * </p>
	 *
	 * <ul>
	 * <li>"0"</li>
	 * <li>"false" (or "f")</li>
	 * <li>"no" (or "n")</li>
	 * </ul>
	 *
	 * @param xpathExpression
	 *            The XPath expression.
	 * @param defaultValue
	 *            the default value (used if the result of the xpath query is
	 *            <code>null</code> or the empty string
	 * @return If the result of the xpath query is <code>null</code> or the
	 *         empty string, <code>defaultValue</code> is returned.
	 *
	 *         <p>
	 *         In the other cases, if the value is listed in the above list of
	 *         "false" values, <code>false</code> is returned; otherwise,
	 *         <code>true</code>.
	 *         </p>
	 */
	public boolean xpathTrueFalse(final String xpathExpression, final boolean defaultValue) {
		String result = this.xpathString(xpathExpression);
		return isEmpty(result) ? defaultValue : trueFalse(result);
	}

	/**
	 * Indicates whether the value is "true" or "false".
	 *
	 * <p>
	 * List of "false" values (all others are considered true) -
	 * case-insensitive:
	 * </p>
	 *
	 * <ul>
	 * <li><code>null</code></li>
	 * <li>"" (the empty string)</li>
	 * <li>"0"</li>
	 * <li>"false" (or "f")</li>
	 * <li>"no" (or "n")</li>
	 * </ul>
	 *
	 * @param value
	 *            the value
	 * @return <code>false</code> if the value is listed in the above list of
	 *         "false" values; otherwise, <code>true</code>.
	 */
	public static boolean trueFalse(final String value) {
		// if value is the empty string, 0, false, f, no, or n
		// (Case-insensitive)
		// return false, else return true

		boolean valueIsFalse = value == null || value.length() == 0 || value.equals("0")
				|| value.equalsIgnoreCase("false") || value.equalsIgnoreCase("f") || value.equalsIgnoreCase("no")
				|| value.equalsIgnoreCase("n");

		return !valueIsFalse;
	}

	/**
	 * Fixes up an xpath expression, applying work-arounds related to Java DOM.
	 *
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the string
	 */
	private String fixupXPath(String xpathExpression) {
		if (this.getNamespaceURI("") != null) {
			xpathExpression = fixDefaultNamespace(xpathExpression);
		}

		// System.out.println(xpathExpression);

		if (xpathExpression.startsWith("//")) {
			// Fix for Java bug (suppose to search from current node, not root)
			xpathExpression = "." + xpathExpression;
		} else if (xpathExpression.startsWith(":")) {
			// Fix for Java bug - error if accessing item in default namespace
			// (when namespaces are used in XML)
			xpathExpression = "./" + xpathExpression;
		}

		// System.out.println(xpathExpression);

		return xpathExpression;
	}

	/**
	 * Adds the element.
	 *
	 * @param tagName
	 *            the tag name
	 * @return the XPathElement
	 */
	public XPathElement addElement(final String tagName) {
		XPathElement element = this.createElement(tagName);

		this.appendChild(element);

		return element;
	}

	/**
	 * Adds the element.
	 *
	 * @param tagName
	 *            the tag name
	 * @param text
	 *            the text
	 * @return the XPathElement
	 */
	public XPathElement addElement(final String tagName, final String text) {
		XPathElement element = this.addElement(tagName);
		element.setTextContent(text);
		return element;
	}

	/**
	 * Gets the line separator used when outputting the xml as text.
	 *
	 * @return the line separator used when outputting the xml as text.
	 */
	private String getLineSeparator() {
		// TODO: determine way to get line separator from file, or to specify
		// one
		return lineSeparator;
	}

	/**
	 * Gets the XML.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @return the XML
	 */
	public String getXML() {
		return this.getXML(Indent, LineWidth);
	}

	/**
	 * Gets the XML.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @return the XML
	 */
	public String getXML(final int indent) {
		return this.getXML(indent, LineWidth);
	}

	/**
	 * Gets the XML.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @return the XML
	 */
	public String getXML(final int indent, final int lineWidth) {
		Writer out = new StringWriter();
		return this.writeXML(out, indent, lineWidth).toString();
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param outputStream
	 *            the output stream
	 * @return the passed output stream
	 */
	public OutputStream writeXML(final OutputStream outputStream) {
		return this.writeXML(outputStream, Indent, LineWidth);
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param outputStream
	 *            the output stream
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @return the passed output stream
	 */
	public OutputStream writeXML(final OutputStream outputStream, final int indent) {
		return this.writeXML(outputStream, indent, LineWidth);
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param outputStream
	 *            the output stream
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @return the passed output stream
	 */
	public OutputStream writeXML(final OutputStream outputStream, final int indent, final int lineWidth) {
		this.writeXML(new OutputStreamWriter(outputStream), indent, lineWidth);
		return outputStream;
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param writer
	 *            the writer
	 * @return the passed writer
	 */
	public Writer writeXML(final Writer writer) {
		return this.writeXML(writer, Indent, LineWidth);
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param writer
	 *            the writer
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @return the passed writer
	 */
	public Writer writeXML(final Writer writer, final int indent) {
		return this.writeXML(writer, indent, LineWidth);
	}

	/**
	 * Writes the XML.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param writer
	 *            the writer
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @return the passed writer
	 */
	public Writer writeXML(final Writer writer, final int indent, final int lineWidth) {
		short nodeType = this.node.getNodeType();

		try {
			if (nodeType == ATTRIBUTE_NODE) {
				writer.write(this.node.getNodeName() + "=\"" + this.node.getNodeValue() + "\"");
				// XXX: this won't work is the node has a double quote
				// XXX: do I need to add a trailing space?
			} else if (nodeType == CDATA_SECTION_NODE || nodeType == COMMENT_NODE
					|| nodeType == PROCESSING_INSTRUCTION_NODE || nodeType == TEXT_NODE) {
				// XXX: need to verify this is correct
				writer.write(this.node.getNodeValue());
			} else if (nodeType == DOCUMENT_NODE || nodeType == DOCUMENT_FRAGMENT_NODE || nodeType == ELEMENT_NODE) {
				OutputFormat format;

				if (indent > 0) {
					format = new OutputFormat(this.getRootDocument());
					format.setIndenting(true);
					format.setIndent(indent);
					format.setLineWidth(lineWidth);
					format.setLineSeparator(this.getLineSeparator());
				} else if (indent == 0) {
					try {
						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						transformerFactory.setAttribute("indent-number", indent);

						Transformer transformer = transformerFactory.newTransformer();

						transformer.setOutputProperty(OutputKeys.INDENT, "yes");

						// Don't include XML declaration for elements, allows
						// writing document via traversal
						if (nodeType == ELEMENT_NODE || nodeType == DOCUMENT_FRAGMENT_NODE) {
							transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
						}

						// transformer.setOutputProperty(OutputKeys.ENCODING,
						// "UTF-8");

						// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
						// String.valueOf(indent));

						// Source:
						// http://techxplorer.com/2010/05/20/indenting-xml-output-in-java/
						// set some options on the transformer
						// transformer.setOutputProperty(OutputKeys.ENCODING,
						// "utf-8");
						// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
						// "no");
						// transformer.setOutputProperty(OutputKeys.INDENT,
						// "yes");
						// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
						// "2");

						// initialize StreamResult with File object to save to
						// file
						StreamResult result = new StreamResult(writer);
						DOMSource source = new DOMSource(this.node);
						transformer.transform(source, result);
						// String resultString = result.getWriter().toString();
						return writer;
					} catch (TransformerException e) {
						throw new IllegalArgumentException(e);
					}
				} else {
					format = new OutputFormat(this.getRootDocument());
					format.setIndenting(false);
					format.setLineSeparator(this.getLineSeparator());
				}

				// Don't include XML declaration for elements, allows writing
				// document via traversal
				if (nodeType == ELEMENT_NODE || nodeType == DOCUMENT_FRAGMENT_NODE) {
					format.setOmitXMLDeclaration(true);
				}

				// XXX: if comments appear of top level of document fragment,
				// they are omitted (for some weird reason)

				// TODO: doesn't seem to respect newline in comments or elements
				// String tempLineSeparator = format.getLineSeparator();
				// for (int i = 0; i < tempLineSeparator.length(); i++)
				// {
				// System.out.println((int) tempLineSeparator.charAt(i));
				// }

				// Writer out = new StringWriter();
				XMLSerializer serializer = new XMLSerializer(writer, format);

				switch (nodeType) {
				case DOCUMENT_NODE:
					serializer.serialize((Document) this.node);
					break;
				case DOCUMENT_FRAGMENT_NODE:
					serializer.serialize((DocumentFragment) this.node);
					break;
				case ELEMENT_NODE:
					serializer.serialize((Element) this.node);
					break;
				default:
					throw new AssertionError("Unexpected value in switch statement: " + nodeType);
				}

				// return out.toString();
			} else {
				String nodeTypeString;

				switch (nodeType) {
				case DOCUMENT_TYPE_NODE:
					nodeTypeString = "DOCUMENT_TYPE_NODE";
					break;
				case ENTITY_NODE:
					nodeTypeString = "ENTITY_NODE";
					break;
				case ENTITY_REFERENCE_NODE:
					nodeTypeString = "ENTITY_REFERENCE_NODE";
					break;
				case NOTATION_NODE:
					nodeTypeString = "NOTATION_NODE";
					break;
				default:
					nodeTypeString = String.valueOf(nodeType) + " (see org.w3c.dom.Node class for Node type)";
				}

				throw new IllegalStateException("Not sure how to output XML for node type: " + nodeTypeString);
			}

			return writer;
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		// try {
		// TransformerFactory transformerFactory =
		// TransformerFactory.newInstance();
		//
		// if (indent >= 0)
		// transformerFactory.setAttribute("indent-number", indent);
		//
		// Transformer transformer = transformerFactory.newTransformer();
		//
		// if (indent >= 0)
		// transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// // transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		//
		// //
		// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
		// // String.valueOf(indent));
		//
		// // Source:
		// http://techxplorer.com/2010/05/20/indenting-xml-output-in-java/
		// // set some options on the transformer
		// // transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
		// // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
		// "no");
		// // transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// //
		// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
		// "2");
		//
		// // initialize StreamResult with File object to save to file
		// StreamResult result = new StreamResult(new StringWriter());
		// DOMSource source = new DOMSource(node);
		// transformer.transform(source, result);
		//
		// String resultString = result.getWriter().toString();
		//
		// // if (true)
		// // return resultString;
		//
		// // Source:
		// http://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java
		// try {
		// final Document document = parseXmlFile(resultString);
		//
		// // OutputFormat format = new OutputFormat(document);
		//
		// OutputFormat format = new OutputFormat(getRootDocument());
		// format.setIndenting(true);
		// // format.setLineWidth(65);
		// format.setLineWidth(0);
		// format.setIndent(indent);
		// Writer out = new StringWriter();
		// XMLSerializer serializer = new XMLSerializer(out, format);
		// serializer.serialize(document);
		//
		// return out.toString();
		// } catch (Exception e) {
		// return resultString;
		// }
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// return null;
		// }
	}

	// Source:
	// http://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java
	// private Document parseXmlFile(String in)
	// {
	// try {
	// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	// DocumentBuilder db = dbf.newDocumentBuilder();
	// InputSource is = new InputSource(new StringReader(in));
	// return db.parse(is);
	// } catch (ParserConfigurationException e) {
	// throw new RuntimeException(e);
	// } catch (SAXException e) {
	// throw new RuntimeException(e);
	// } catch (IOException e) {
	// throw new RuntimeException(e);
	// }
	// }

	/**
	 * Gets the pathname.
	 *
	 * @return the pathname
	 * @throws IllegalStateException
	 *             If this XML was not loaded from a file
	 */
	public String getPathname() {
		// XXX: this returns the incorrect value on our external server when
		// using local host or computer name
		// - Java 1.6v14 (32-bit)
		String uriString = this.getBaseURI();

		if (uriString != null && uriString.startsWith("file:")) {
			URI uri = URI.create(uriString);

			// Remote server (when run on Windows, at least)
			String authority = uri.getAuthority();
			String path = uri.getPath();

			// System.out.println("Get pathname:");
			// System.out.println("Authority: " + authority);
			// System.out.println("Path: " + path);

			// TODO: need to verify this works on non-Windows operating systems

			String pathname = authority != null ? "\\\\" + authority + path : path;
			return new File(pathname).getPath();

			// return uriString.substring("file:".length());
		}

		throw new IllegalStateException("XML was not loaded from a file");
	}

	/**
	 * Saves the xml to the same place it was loaded.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML() {
		this.saveXML(Indent, LineWidth);
	}

	/**
	 * Saves the xml to the same place it was loaded.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final int indent) {
		this.saveXML(indent, LineWidth);
	}

	/**
	 * Saves the xml to the same place it was loaded.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final int indent, final int lineWidth) {
		String uriString = this.getBaseURI();

		if (uriString != null && uriString.startsWith("file:")) {
			uriString = this.getPathname();

			// System.out.println("URI String: " + getBaseURI());
			// System.out.println("Pathname: " + getPathname());
		}

		this.saveXML(uriString, indent, lineWidth);

		// saveXML(new File(URI.create(getBaseURI())));
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final String pathname) {
		this.saveXML(pathname, Indent, LineWidth);
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final String pathname, final int indent) {
		this.saveXML(pathname, indent, LineWidth);
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final String pathname, final int indent, final int lineWidth) {
		this.saveXML(pathname == null ? null : new File(pathname), indent, lineWidth);
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses an indent of 4 spaces with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final File pathname) {
		this.saveXML(pathname, Indent, LineWidth);
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses the specified indent with a line width of 72, as specified
	 * by {@link OutputFormat.Defaults}
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final File pathname, final int indent) {
		this.saveXML(pathname, indent, LineWidth);
	}

	/**
	 * Saves the xml to the specified pathname.
	 *
	 * <p>
	 * The XML uses the specified indent and line width.
	 * </p>
	 *
	 * @param pathname
	 *            the pathname
	 * @param indent
	 *            the indent (number of spaces). If 0, the output will be
	 *            left-aligned (no spaces). If negative, no indenting will be
	 *            done, not even placing each child element on their own line.
	 *            </p>
	 * @param lineWidth
	 *            the line width (if 0 then no line wrapping will occur)
	 * @throws IllegalArgumentException
	 *             If an I/O error occurs
	 */
	public void saveXML(final File pathname, final int indent, final int lineWidth) {
		checkNull("pathname", pathname);

		// Ensure destination directory exists
		File parentFile = pathname.getParentFile();

		if (parentFile != null) {
			parentFile.mkdirs();
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(pathname));

			try {
				out.write(this.getRootDocument().getXML(indent, lineWidth));
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Gets the root document.
	 *
	 * @return the root document
	 */
	public XPathDocument getRootDocument() {
		if (this.rootDocument != null) {
			return this.rootDocument;
		}

		XPathDocument document;

		// TODO: need to handle the case of a DocumentType
		if (this.node instanceof Document || this.node instanceof DocumentType) {
			// return asXPathDocument((Document) node);
			document = (XPathDocument) this;
		}

		document = this.asXPathDocument(this.node.getOwnerDocument());

		return this.rootDocument = document;
	}

	/**
	 * Returns the top level element.
	 *
	 * @return the top level element
	 */
	public XPathElement getRootElement() {
		return this.asXPathElement(this.getRootDocument().getDocumentElement());
	}

	/**
	 * Creates a new element, inheriting properties from this XPathNode.
	 *
	 * @param tagName
	 *            the tag name
	 * @return the created element
	 * @throws DOMException
	 *             the DOM exception
	 */
	public XPathElement createElement(final String tagName) throws DOMException {
		// return asXPathElement(getNode().createElement(tagName));
		// XPathElement element =
		// XPathElement.valueOf(getNode().createElement(tagName));
		// element.updateNamespaceContext(getNamespaceContext());
		// return element;
		String namespaceURI = this.getNamespaceURI(getPrefix(tagName));

		return namespaceURI != null ? this.createElementNS(namespaceURI, tagName)
				: this.asXPathElement(this.getRootDocument().getNode().createElement(tagName));
	}

	/**
	 * Gets the namespace prefix for the tag name
	 *
	 * @param tagName
	 *            the tag name
	 * @return the prefix (or the empty string if no namespace is specified)
	 */
	static String getPrefix(final String tagName) {
		int colonIndex = tagName.indexOf(':');
		return colonIndex == -1 ? "" : tagName.substring(0, colonIndex);
	}

	/**
	 * Creates a new element, inheriting properties from this XPathNode.
	 *
	 * @param tagName
	 *            the tag name
	 * @param text
	 *            the text to set on the newly created element
	 * @return the created element
	 * @throws DOMException
	 *             the DOM exception
	 */
	public XPathElement createElement(final String tagName, final String text) throws DOMException {
		XPathElement element = this.createElement(tagName);
		element.setTextContent(text);
		return element;
		// return createElementNS(getNamespaceURI(tagName), tagName);
	}

	/**
	 * Creates a new element, inheriting properties from this XPathNode.
	 *
	 * @param namespaceURI
	 *            the namespace uri
	 * @param qualifiedName
	 *            the qualified name
	 * @return the created element
	 * @throws DOMException
	 *             the DOM exception
	 */
	public XPathElement createElementNS(final String namespaceURI, final String qualifiedName) throws DOMException {
		// return getNode().createElementNS(namespaceURI, qualifiedName);
		// return asXPathElement(getNode().createElementNS(namespaceURI,
		// qualifiedName));
		// XPathElement element =
		// XPathElement.valueOf(getRootDocument().getNode().createElementNS(namespaceURI,
		// qualifiedName));
		return this.asXPathElement(this.getRootDocument().getNode().createElementNS(namespaceURI, qualifiedName));
		// element.updateNamespaceContext(getNamespaceContext());
		// return element;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.node.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode appendChild(Node newChild) throws DOMException {
		newChild = getNode(newChild);

		// Ensures that node can be added (even if from another document)
		this.getRootDocument().adoptNode(newChild);
		// newChild = getNode(getRootDocument().importNode(newChild, true));

		return this.asXPathNode(this.node.appendChild(newChild));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode cloneNode(final boolean deep) {
		return this.asXPathNode(this.node.cloneNode(deep));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public short compareDocumentPosition(final Node other) throws DOMException {
		return this.node.compareDocumentPosition(other);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedNodeMap getAttributes() {
		return this.node.getAttributes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBaseURI() {
		return getNode(this.getRootDocument()).getBaseURI();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNodeList<XPathNode> getChildNodes() {
		return this.asXPathNodeList(this.node.getChildNodes());
	}

	/**
	 * Gets the first child element with the specified element name.
	 *
	 * @param elementName
	 *            the element name
	 * @return the first child element with the specified element name (or
	 *         <code>null</code> if none exists)
	 */
	public XPathElement getChildElement(final String elementName) {
		return this.asXPathElement(getChildElement(this.node, elementName));
	}

	/**
	 * Gets the first child element with the specified element name.
	 *
	 * @param currentNode
	 *            the current node
	 * @param elementName
	 *            the element name (use "*" to get the first element, regardless
	 *            of its name)
	 * @return the first child element with the specified element name (or
	 *         <code>null</code> if none exists)
	 */
	private static Element getChildElement(final Node currentNode, final String elementName) {
		NodeList childNodes = currentNode.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);

			if (childNode instanceof Element) {
				if (elementName.equals("*") || childNode.getNodeName().equals(elementName)) {
					return (Element) childNode;
				}
			}
		}

		// No child element exists with the name
		return null;
	}

	/**
	 * Gets the first child element with the specified element name.
	 *
	 * <p>
	 * The current node is checked for the first child element name for a
	 * matching element. If found, this child element is then checked for a
	 * child element with the next child element name - this is done for each
	 * child element name.
	 * </p>
	 *
	 * <p>
	 * If at any point in the traversal, no child element exists,
	 * <code>null</code> will be returned, indicating that no child element
	 * exists
	 * </p>
	 *
	 * @param childElementNames
	 *            the child element names
	 * @return the first child element with the specified element name (or
	 *         <code>null</code> if none exists)
	 */
	public XPathElement getChildElement(final String... childElementNames) {
		if (childElementNames == null) {
			throw new NullPointerException("childElementNames cannot be null");
		}

		if (childElementNames.length == 0) {
			throw new IllegalArgumentException("At least one child element name must be specified");
		}

		Node currentNode = this.node;

		for (String elementName : childElementNames) {
			currentNode = getChildElement(currentNode, elementName);

			if (currentNode == null) {
				return null;
			}
		}

		// Will always be element, because
		return this.asXPathElement((Element) currentNode);
	}

	/**
	 * Gets the child elements with the specified element name.
	 *
	 * @param elementName
	 *            the element name (use "*" to get all child elements,
	 *            regardless of their name)
	 * @return the child elements (or an empty list if there are none)
	 */
	public XPathNodeList<XPathElement> getChildElements(final String elementName) {
		NodeList childNodes = this.node.getChildNodes();
		List<XPathElement> childElements = new ArrayList<>();

		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);

			if (childNode instanceof Element) {
				Element childElement = (Element) childNode;

				if (elementName.equals("*") || childElement.getNodeName().equals(elementName)) {
					childElements.add(this.asXPathElement(childElement));
				}
			}
		}

		return new XPathNodeList<>(childElements);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getFeature(final String feature, final String version) {
		return this.node.getFeature(feature, version);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode getFirstChild() {
		return this.asXPathNode(this.node.getFirstChild());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode getLastChild() {
		return this.asXPathNode(this.node.getLastChild());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLocalName() {
		return this.node.getLocalName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNamespaceURI() {
		return this.node.getNamespaceURI();
	}

	/**
	 * Gets the namespace uri with the given namespace prefix.
	 *
	 * @param prefix
	 *            the prefix
	 * @return the namespace uri
	 */
	public String getNamespaceURI(final String prefix) {
		NamespaceContext namespaceContext = this.getNamespaceContext();

		if (namespaceContext != null) {
			return namespaceContext.getNamespaceURI(prefix);
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode getNextSibling() {
		return this.asXPathNode(this.node.getNextSibling());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNodeName() {
		return this.node.getNodeName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public short getNodeType() {
		return this.node.getNodeType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNodeValue() throws DOMException {
		return this.node.getNodeValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathDocument getOwnerDocument() {
		// return asXPathDocument(node.getOwnerDocument());
		return this.getRootDocument();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode getParentNode() {
		return this.asXPathNode(this.node.getParentNode());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPrefix() {
		return this.node.getPrefix();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode getPreviousSibling() {
		return this.asXPathNode(this.node.getPreviousSibling());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTextContent() throws DOMException {
		return this.node.getTextContent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getUserData(final String key) {
		return this.node.getUserData(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasAttributes() {
		return this.node.hasAttributes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasChildNodes() {
		return this.node.hasChildNodes();
	}

	// private static int count = 0;

	/**
	 * Inserts this node before the specified reference node
	 *
	 * @param refNode
	 *            the reference node
	 * @return the node being inserted
	 * @throws NullPointerException
	 *             If <code>refNode</code> is <code>null</code>
	 */
	public XPathNode insertBefore(final Node refNode) {
		Node parent = refNode.getParentNode();

		if (parent == null) {
			parent = refNode;
		}

		return this.asXPathNode(parent.insertBefore(this.node, refNode));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode insertBefore(Node newChild, Node refChild) throws DOMException {
		// System.out.println(XPathNode.valueOf(newChild).xpath());
		// System.out.println(XPathNode.valueOf(refChild).xpath());

		newChild = getNode(newChild);
		refChild = getNode(refChild);

		this.getRootDocument().adoptNode(newChild);

		if (newChild == refChild) {
			// Do nothing, since the node is inserting before itself
			return this.asXPathNode(newChild);
		}

		return this.asXPathNode(this.node.insertBefore(newChild, refChild));
		// transferSettingsTo(newNode);
		// System.out.println("Get prefix: " + newNode.getPrefix());
		// System.out.println("Determine namespace: " +
		// getNamespaceURI(getPrefix(newNode.getNodeName())));

		// System.out.println("Insert before: " + newNode.xpath());
		// return newNode;
	}

	/**
	 * Inserts this node after the specified reference node
	 *
	 * @param refNode
	 *            the reference node
	 * @return the node being inserted
	 * @throws NullPointerException
	 *             If <code>refNode</code> is <code>null</code>
	 */
	public XPathNode insertAfter(final Node refNode) {
		Node parent = refNode.getParentNode();

		if (parent == null) {
			parent = refNode;
		}

		return this.asXPathNode(parent.insertBefore(this.node, refNode.getNextSibling()));
	}

	/**
	 * Inserts a new child before the specified reference child.
	 *
	 * <p>
	 * If <code>refChild</code> is <code>null</code>, the new child will be
	 * added as the new first child node.
	 * </p>
	 *
	 * @param newChild
	 *            the child node to add
	 * @param refChild
	 *            the reference child
	 * @return the node being inserted
	 */
	public XPathNode insertAfter(final Node newChild, final Node refChild) {
		return this.insertBefore(newChild, refChild == null ? this.getFirstChild() : refChild.getNextSibling());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDefaultNamespace(final String namespaceURI) {
		return this.node.isDefaultNamespace(namespaceURI);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEqualNode(final Node arg) {
		return this.node.isEqualNode(arg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSameNode(final Node other) {
		return this.node.isSameNode(getNode(other));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupported(final String feature, final String version) {
		return this.node.isSupported(feature, version);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String lookupNamespaceURI(final String prefix) {
		return this.node.lookupNamespaceURI(prefix);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String lookupPrefix(final String namespaceURI) {
		return this.node.lookupPrefix(namespaceURI);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void normalize() {
		this.node.normalize();
	}

	/**
	 * Removes this node from the DOM tree
	 *
	 * @return the removed child
	 */
	public XPathNode remove() {
		if (this.node.getNodeType() == Node.ATTRIBUTE_NODE) {
			Attr attr = (Attr) this.node;

			return this.asXPathAttr(attr.getOwnerElement().removeAttributeNode(attr));
		} else {
			return this.getParentNode().removeChild(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode removeChild(final Node oldChild) throws DOMException {
		return this.asXPathNode(this.node.removeChild(getNode(oldChild)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode replaceChild(final Node newChild, final Node oldChild) throws DOMException {
		// Ensures that node can be replaced (even if from another document)
		this.getRootDocument().adoptNode(newChild);
		// newChild = getRootDocument().importNode(newChild, true);

		return this.asXPathNode(this.node.replaceChild(getNode(newChild), getNode(oldChild)));
	}

	/**
	 * Replaces this node with the new node
	 *
	 * @param newNode
	 *            the new node
	 * @return the replaced node
	 */
	public XPathNode replaceWith(final Node newNode) {
		return this.getParentNode().replaceChild(newNode, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNodeValue(final String nodeValue) throws DOMException {
		this.node.setNodeValue(nodeValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPrefix(final String prefix) throws DOMException {
		this.node.setPrefix(prefix);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTextContent(final String textContent) throws DOMException {
		this.node.setTextContent(textContent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
		return this.node.setUserData(key, data, handler);
	}

	/**
	 * Gets the namespace context.
	 *
	 * @return the namespace context
	 */
	public NamespaceContext getNamespaceContext() {
		return this.getXPath().getNamespaceContext();
	}

	/**
	 * Sets the namespace context.
	 *
	 * @param nsContext
	 *            the new namespace context
	 */
	public void setNamespaceContext(final NamespaceContext nsContext) {
		this.getXPath().setNamespaceContext(nsContext);
	}

	// public XPathNode setNodeName(String name)
	// {
	// return renameNode(name);
	// }

	// public XPathNode setNamespaceURI(String namespaceURI)
	// {
	// return asXPathNode(getRootDocument().renameNode(node, namespaceURI,
	// node.getNodeName()));
	// }

	/**
	 * Renames the node, the name space is left unchanged
	 *
	 * @param name
	 *            the name
	 * @return this xpath node
	 */
	public XPathNode renameNode(final String name) {
		// TODO: enable using namespaces
		return this.asXPathNode(this.getRootDocument().renameNode(this.node, this.node.getNamespaceURI(), name));
	}

	// private Comparator<XPathNode> sortNodes = new Comparator<XPathNode>() {
	//
	// public int compare(XPathNode o1, XPathNode o2)
	// {
	// int compare = o1.getNodeName().compareTo(o2.getNodeName());
	//
	// if (compare != 0)
	// return compare;
	//
	// return o1.getXML().compareTo(o2.getXML());
	// }
	// };
	//
	// public XPathNode sort()
	// {
	// System.out.println(getXML());
	// System.out.println();
	//
	// XPathNodeList childNodes = getChildNodes();
	//
	// // TODO: need to sort attributes
	// if (childNodes.size() == 1
	// && childNodes.get(0).getNodeType() == Node.TEXT_NODE) {
	// // e.g. <element>value</element>
	// return this.cloneNode(true);
	// }
	//
	// List<XPathNode> sortedNodes = new ArrayList<XPathNode>();
	//
	// for (XPathNode child : childNodes) {
	// if (child.getNodeType() == Node.TEXT_NODE &&
	// child.getTextContent().trim().length() == 0) {
	// // Ignore white space between node elements
	// // (occurs when each element appears on its own line
	// // - standard XML format)
	// continue;
	// }
	//
	// child.sort();
	// int index = Collections.binarySearch(sortedNodes, child, sortNodes);
	//
	// if (index >= 0)
	// sortedNodes.add(index, child);
	// else
	// sortedNodes.add(-index - 1, child);
	// }
	//
	// XPathNode sortedNode = new XPathNode();
	//
	// for (XPathNode child : sortedNodes)
	// {
	// sortedNode.appendChild(child);
	// }
	// // for (int i = 0; i < childNodes.size(); i++) {
	// // XPathNode oldChild = childNodes.get(i);
	// //
	// // if (i >= sortedNodes.size())
	// // removeChild(oldChild);
	// //
	// // XPathNode newChild = sortedNodes.get(i);
	// //
	// // if (!newChild.isSameNode(oldChild))
	// // replaceChild(newChild, oldChild);
	// // }
	//
	// System.out.println("Sorted:\n" + sortedNode.getXML());
	// return sortedNode;
	// }

	/**
	 * Escapes xml characters in <code>input</code>: &amp; &lt; &gt; &quot; '.
	 * These characters are the only characters which are required to be escaped
	 * in any XML node.
	 *
	 * <p>
	 * Note that when using the methods of this class, escaping is not
	 * necessary, and is done for you. This method is provided for cases when
	 * you need to escape XML, but are not storing the value in an
	 * <code>XPathNode</code>.
	 * </p>
	 *
	 * @param input
	 *            the input
	 * @return the string
	 */
	public static String escapeXML(final String input) {
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
			} else if (c == '"') {
				escapedString = "&quot;";
			} else if (c == '\'') {
				escapedString = "&apos;";
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

	/**
	 * Encode characters.
	 *
	 * <ul>
	 * <li>'\r' as &amp;#x000d;</li>
	 * <li>'\n' as &amp;#x000a;</li>
	 * <li>'\t' as &amp;#x0009;</li>
	 * </ul>
	 *
	 * @param input
	 *            the input
	 * @return the encoded output
	 */
	// public static String encodeCharacters(String input)
	// {
	// /*
	// * TODO: Fails for the case of line separator in element start tag, and
	// close is on next line
	// *
	// * <root attr="text"
	// * />
	// *
	// * Have an escape character not within an element or attribute causes a
	// parse exception
	// *
	// * Will have to parse out attribute and only escape within attributes
	// * Use a regular expression to parse the input - shouldn't be too hard
	// */
	// StringBuilder output = new StringBuilder();
	// int start = 0;
	//
	// for (int i = 0; i < input.length(); i++) {
	// char c = input.charAt(i);
	// String encodedString;
	//
	// if (c == '\r') {
	// // Store internally as just '\n' if \r\n appears in the input
	// // (otherwise, an extra \r appears in the output for elements)
	// // This also means that the attribute will only have a '\n' encoded
	// encodedString = i < input.length() - 1 && input.charAt(i + 1) == '\n' ?
	// "" : "&#x000d;";
	// } else if (c == '\n')
	// encodedString = "&#x000a;";
	// else if (c == '\t')
	// encodedString = "&#x0009;";
	// else
	// continue;
	//
	// output.append(input, start, i);
	// start = i + 1;
	// output.append(encodedString);
	// }
	//
	// // No characters need to be escaped
	// if (start == 0)
	// return input;
	//
	// return output.append(input, start, input.length()).toString();
	// }

	/** Regex to match &amp;#0013; and &amp#0010; line encodings. */
	// private static final Pattern workaroundEncodingPattern = compile("&#" +
	// "(?:"
	// + "(?<r>x(?:000)?d|13)|"
	// + "(?<n>x(?:000)?a|10)|"
	// + "(?<t>x(?:000)?9|9)"
	// + ");");

	/**
	 * Decode characters.
	 *
	 * <ul>
	 * <li>&amp;#x000d; as '\r'</li>
	 * <li>&amp;#x000a; as '\n'</li>
	 * <li>&amp;#x0009; as '\t'</li>
	 * </ul>
	 *
	 * @param input
	 *            the input
	 * @return the decoded output
	 */
	// String decodeCharacters(String input)
	// {
	// Matcher matcher = workaroundEncodingPattern.matcher(input);
	// StringBuffer decodedString = new StringBuffer();
	//
	// while (matcher.find()) {
	// if (matcher.matched("r"))
	// matcher.appendReplacement(decodedString, "\r");
	// else if (matcher.matched("n")) {
	// int length = decodedString.length();
	// // Use xpath line separator if the previous character is not a '\r'
	// // Otherwise, use '\n'
	// // (this check prevents '\r\n' being replaced as '\r\r\n' on a Windows
	// computer)
	// String useLineSeparator = length != 0 && decodedString.charAt(length - 1)
	// != '\r'
	// ? getLineSeparator()
	// : "\n";
	//
	// matcher.appendReplacement(decodedString, useLineSeparator);
	// } else if (matcher.matched("t"))
	// matcher.appendReplacement(decodedString, "\t");
	// }
	//
	// return matcher.appendTail(decodedString).toString();
	// }
}