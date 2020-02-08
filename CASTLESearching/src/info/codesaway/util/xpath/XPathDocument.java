package info.codesaway.util.xpath;

import static info.codesaway.util.regex.Pattern.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

/**
 * The Class XPathDocument.
 */
public class XPathDocument extends XPathNode implements Document {
	/** The dom factory. */
	private static DocumentBuilderFactory domFactory;

	/** The builder. */
	private static DocumentBuilder builder;

	/** The xpath. */
	protected XPath xpath;

	/** The Constant xpathFactory. */
	private static final XPathFactory xpathFactory = XPathFactory.newInstance();

	static {
		try {
			domFactory = DocumentBuilderFactory.newInstance();

			// Source:
			// http://stackoverflow.com/questions/582352/how-can-i-ignore-dtd-validation-but-keep-the-doctype-when-writing-an-xml-file
			domFactory.setAttribute("http://xml.org/sax/features/namespaces", true);
			domFactory.setAttribute("http://xml.org/sax/features/validation", false);
			domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			domFactory.setNamespaceAware(true);

			builder = domFactory.newDocumentBuilder();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** A nonexistent document, used to create elements */
	// static final XPathDocument nonexistentDocument = new XPathDocument("<root></root>");
	// static final Document nonexistentDocument = newDocument();

	/**
	 * Instantiates a blank xpath document.
	 */
	public XPathDocument() {
		super(newDocument());
		this.rootDocument = this;

		this.setXPath(xpathFactory.newXPath());
	}

	/**
	 * Creates a blank DOM document.
	 *
	 * @return the document
	 */
	private static Document newDocument() {
		synchronized (builder) {
			return builder.newDocument();
		}
	}

	/**
	 * Instantiates a new xpath document.
	 *
	 * @param systemId
	 *            the pathname or xml text
	 * @throws NullPointerException
	 *             If <code>systemId</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur (for example, if <code>systemId</code> refers to a pathname which does not
	 *             exist) or if any parse errors occur
	 */
	public XPathDocument(final String systemId) {
		this(getInputSource(checkNull("systemId", systemId)));

		// Work around, required to allow adding an element in this document to another document
		this.normalize();
	}

	/**
	 * Regex to parse out header from XML file
	 */
	//	private static final Pattern xmlPartsPattern = compile("^(?s)(?<declaration>(?><\\?.*?\\?>\\r?\\n|))(?<body>.*)$");

	/**
	 * Gets the input source.
	 *
	 * @param systemId
	 *            the system id
	 * @return the input source
	 */
	//	private static InputSource getInputSource(String systemId)
	//	{
	//		if (systemId.trim().startsWith("<")) {
	//			// String is an xml string
	//			return new InputSource(new StringReader(systemId));
	//		} else {
	//			File file = new File(systemId);
	//			InputStream inputStream;
	//			try {
	//				inputStream = new FileInputStream(file);
	//			} catch (FileNotFoundException e) {
	//				throw new IllegalArgumentException(e);
	//			}
	//
	//			// Work around - read file contents and load from the String
	//			// (allows using new lines and tabs in attributes, which otherwise would need to be escaped)
	////			ByteArrayOutputStream contents = new ByteArrayOutputStream((int) file.length());
	////			copyContents(inputStream, contents);
	//
	////			String contentsString = contents.toString();
	//
	//			// Remove UTF-8 BOM
	////			if (contentsString.startsWith("\u00ef\u00bb\u00bf"))
	////				contentsString = contentsString.substring(3);
	////			else if (contentsString.startsWith("\uFEFF"))
	////				contentsString = contentsString.substring(1);
	//
	////			Matcher matcher = xmlPartsPattern.matcher(contentsString);
	//
	////			if (!matcher.find())
	////				throw new AssertionError("XML is not in a recognized format:\n" + contentsString);
	//
	////			contentsString = matcher.group("declaration") + encodeCharacters(matcher.group("body"));
	//
	////			 System.out.println(contentsString);
	////			InputSource inputSource = new InputSource(new StringReader(contentsString));
	//			InputSource inputSource = new InputSource();
	//
	//			// inputSource.setSystemId(systemId);
	//
	//			// Work around since using \\localhost doesn't work without it,
	//			// (even though the file can be read via it's input stream)
	//			inputSource.setByteStream(inputStream);
	//			inputSource.setSystemId(file.toURI().toString());
	//
	//			return inputSource;
	//		}
	//	}

	/**
	 * Gets the input source.
	 *
	 * @param systemId
	 *            the system id
	 * @return the input source
	 */
	private static InputSource getInputSource(final String systemId) {
		if (systemId.trim().startsWith("<")) {
			// String is an xml string
			return new InputSource(new StringReader(systemId));
		} else {
			InputSource inputSource = new InputSource(systemId);

			try {
				File file = new File(systemId);

				// Work around since using \\localhost doesn't work without it,
				// (even though the file can be read via it's input stream)
				inputSource.setByteStream(new FileInputStream(file));
				inputSource.setSystemId(file.toURI().toString());
			} catch (FileNotFoundException e) {
				// Silently ignored; use the default byte stream
			}

			return inputSource;
		}
	}

	/** Copied from FileUtility to allow reading the file content */
	/**
	 * Buffer size for a buffer.
	 */
	// private static final int BUFFER_SIZE = 8192;
	//	private static final int BUFFER_SIZE = 65536;

	/** Cache of temporary buffers (used by various methods). */
	//	private static final List<byte[]> bufferCache = new Vector<byte[]>();

	/**
	 * Copy the contents.
	 *
	 * @param in
	 *            the input (closed when finished copying)
	 * @param out
	 *            the output
	 * @throws IllegalArgumentException
	 *             if an I/O error occurs.
	 */
	//	private static void copyContents(InputStream in, OutputStream out)
	//	{
	//		checkNull("in", in);
	//		checkNull("out", out);
	//
	//		byte[] buffer = null;
	//		try {
	//			try {
	//				int len;
	//
	//				buffer = getBuffer();
	//
	//				while ((len = in.read(buffer)) > 0) {
	//					out.write(buffer, 0, len);
	//				}
	//			} finally {
	//				try {
	//					in.close();
	//				} finally {
	//					out.close();
	//				}
	//			}
	//		} catch (IOException e) {
	//			throw new IllegalArgumentException(e);
	//		} finally {
	//			if (buffer != null)
	//				bufferCache.add(buffer);
	//		}
	//	}

	/**
	 * Gets a byte array buffer, used when reading file contents.
	 *
	 * @return the buffer
	 */
	//	private static byte[] getBuffer()
	//	{
	//		synchronized (bufferCache) {
	//			return !bufferCache.isEmpty() ? bufferCache.remove(0) : new byte[BUFFER_SIZE];
	//		}
	//	}

	/**
	 * Instantiates a new xpath document.
	 *
	 * @param pathname
	 *            the pathname
	 * @throws NullPointerException
	 *             if <code>pathname</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur (for example, if the pathname does not exist) or if any parse errors
	 *             occur
	 */
	public XPathDocument(final File pathname) {
		this(new InputSource(checkNull("pathname", pathname).toURI().toASCIIString()));
	}

	/**
	 * Instantiates a new xpath document.
	 *
	 * @param inputStream
	 *            the input stream
	 * @throws NullPointerException
	 *             If <code>inputStream</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur or if any parse errors occur
	 */
	public XPathDocument(final InputStream inputStream) {
		this(new InputSource(checkNull("inputStream", inputStream)));
	}

	/**
	 * Instantiates a new xpath document.
	 *
	 * @param inputStream
	 *            the input stream
	 * @param systemId
	 *            the system id
	 * @throws NullPointerException
	 *             If <code>inputStream</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur or if any parse errors occur
	 */
	public XPathDocument(final InputStream inputStream, final String systemId) {
		this(getInputSource(inputStream, systemId));
	}

	/**
	 * Gets the input source.
	 *
	 * @param inputStream
	 *            the input stream
	 * @param systemId
	 *            the system id
	 * @return the input source
	 * @throws NullPointerException
	 *             If <code>inputStream</code> is <code>null</code>
	 */
	private static InputSource getInputSource(final InputStream inputStream, final String systemId) {
		checkNull("inputStream", inputStream);

		InputSource inputSource = new InputSource(inputStream);
		inputSource.setSystemId(systemId);

		return inputSource;
	}

	/**
	 * Pattern to match a namespace attribute.
	 *
	 * <p>The attribute name is captured to the name group, <b>xmlns</b></p>
	 */
	static final Pattern namespaceAttributeRegex = compile("(?J)^xmlns(?::(?<xmlns>[\\w-]++)|(?<xmlns>))$");

	/**
	 * Instantiates a new xpath node.
	 *
	 * @param inputSource
	 *            the input source
	 * @throws NullPointerException
	 *             If <code>inputSource</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur (for example, if the input source does not exist) or if any parse errors
	 *             occur
	 */
	public XPathDocument(final InputSource inputSource) {
		super(parse(inputSource));

		this.rootDocument = this;
		this.setXPath(xpathFactory.newXPath());

		try {
			// Check the root element's attributes for "xmlns" elements
			// - for namespace (if any)

			//			Node firstChild = node.getFirstChild();
			Node rootElement = this.getNode().getDocumentElement();
			NamedNodeMap attributes = null;

			final HashMap<String, String> namespaces = new HashMap<>();

			int attributesLength = rootElement == null ||
					(attributes = rootElement.getAttributes()) == null
							? 0
							: attributes.getLength();

			for (int i = 0; i < attributesLength; i++) {
				// Not actually null, since initialized if length != 0
				assert attributes != null;

				Node attribute = attributes.item(i);
				String attributeName = attribute.getNodeName();
				Matcher matcher = namespaceAttributeRegex.matcher(attributeName);

				if (matcher.matches()) {
					namespaces.put(matcher.group("xmlns"), attribute.getNodeValue());
				}
			}

			if (namespaces.size() != 0) {
				NamespaceContext namespaceContext = new NamespaceContext() {
					// private HashMap<String, String> namespaceURIs = namespaces;

					@Override
					public String getNamespaceURI(final String prefix) {
						return namespaces.get(prefix);
					}

					// Dummy implementation - not used!
					@Override
					public Iterator<?> getPrefixes(final String val) {
						return null;
					}

					// Dummy implemenation - not used!
					@Override
					public String getPrefix(final String uri) {
						return null;
					}
				};

				this.setNamespaceContext(namespaceContext);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// normalizeDocument();
	}

	/**
	 * Instantiates a new xpath document.
	 *
	 * @param document
	 *            the document
	 */
	protected XPathDocument(final Document document) {
		super(document);
		this.rootDocument = this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected XPath getXPath() {
		if (this.xpath == null) {
			this.xpath = xpathFactory.newXPath();
		}

		return this.xpath;
	}

	/**
	 * Sets the xpath.
	 *
	 * @param xpath
	 *            the new xpath
	 */
	public void setXPath(final XPath xpath) {
		this.xpath = xpath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Document getNode() {
		return (Document) super.getNode();
	}

	/**
	 * Returns <tt>XPathDocument</tt> instance representing the specified
	 * <tt>Document</tt> value.
	 *
	 * @param document
	 *            <code>Document</code> value
	 * @return XPathDocument instance representing document
	 */
	public static XPathDocument valueOf(final Document document) {
		if (document == null) {
			return null;
		}

		if (document instanceof XPathDocument) {
			return (XPathDocument) document;
		} else {
			return new XPathDocument(document);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathDocument cloneNode(final boolean deep) {
		XPathDocument document = this.asXPathDocument((Document) this.node.cloneNode(deep));
		// document.setXPath(xpathFactory.newXPath());
		document.setXPath(this.getXPath());

		return document;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode adoptNode(Node source) throws DOMException {
		source = getNode(source);
		Document currentDocument = this.getNode();

		// If the node is already in this document, don't want the side effect of removing it from its parent
		if (currentDocument == source.getOwnerDocument()) {
			return this.asXPathNode(source);
		}

		return this.asXPathNode(currentDocument.adoptNode(source));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr createAttribute(final String name) throws DOMException {
		return this.asXPathAttr(this.getNode().createAttribute(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathAttr createAttributeNS(final String namespaceURI, final String qualifiedName)
			throws DOMException {
		return this.asXPathAttr(this.getNode().createAttributeNS(namespaceURI, qualifiedName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CDATASection createCDATASection(final String data) throws DOMException {
		return this.getNode().createCDATASection(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comment createComment(final String data) {
		return this.getNode().createComment(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathDocumentFragment createDocumentFragment() {
		return this.asXPathDocumentFragment(this.getNode().createDocumentFragment());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityReference createEntityReference(final String name)
			throws DOMException {
		return this.getNode().createEntityReference(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProcessingInstruction createProcessingInstruction(final String target,
			final String data) throws DOMException {
		return this.getNode().createProcessingInstruction(target, data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Text createTextNode(final String data) {
		return this.getNode().createTextNode(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DocumentType getDoctype() {
		return this.getNode().getDoctype();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathElement getDocumentElement() {
		// return XPathElement.valueOf(getNode().getDocumentElement());
		return this.asXPathElement(this.getNode().getDocumentElement());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDocumentURI() {
		return this.getNode().getDocumentURI();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DOMConfiguration getDomConfig() {
		return this.getNode().getDomConfig();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathElement getElementById(final String elementId) {
		// return XPathElement.valueOf(getNode().getElementById(elementId));
		return this.asXPathElement(this.getNode().getElementById(elementId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNodeList<XPathElement> getElementsByTagName(final String tagname) {
		return this.asXPathNodeList(this.getNode().getElementsByTagName(tagname));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNodeList<XPathElement> getElementsByTagNameNS(final String namespaceURI, final String localName) {
		// return XPathNodeList.valueOf(getNode().getElementsByTagNameNS(namespaceURI, localName));
		return this.asXPathNodeList(this.getNode().getElementsByTagNameNS(namespaceURI, localName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DOMImplementation getImplementation() {
		return this.getNode().getImplementation();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInputEncoding() {
		return this.getNode().getInputEncoding();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getStrictErrorChecking() {
		return this.getNode().getStrictErrorChecking();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getXmlEncoding() {
		return this.getNode().getXmlEncoding();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getXmlStandalone() {
		return this.getNode().getXmlStandalone();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getXmlVersion() {
		return this.getNode().getXmlVersion();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode importNode(final Node importedNode, final boolean deep) throws DOMException {
		// return XPathNode.valueOf(getNode().importNode(getNode(importedNode), deep));
		return this.asXPathNode(this.getNode().importNode(getNode(importedNode), deep));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void normalizeDocument() {
		this.getNode().normalizeDocument();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XPathNode renameNode(final Node n, final String namespaceURI, final String qualifiedName)
			throws DOMException {
		// return XPathNode.valueOf(getNode().renameNode(getNode(n), namespaceURI, qualifiedName));
		return this.asXPathNode(this.getNode().renameNode(getNode(n), namespaceURI, qualifiedName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDocumentURI(final String documentURI) {
		this.getNode().setDocumentURI(documentURI);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStrictErrorChecking(final boolean strictErrorChecking) {
		this.getNode().setStrictErrorChecking(strictErrorChecking);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setXmlStandalone(final boolean xmlStandalone) throws DOMException {
		this.getNode().setXmlStandalone(xmlStandalone);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setXmlVersion(final String xmlVersion) throws DOMException {
		this.getNode().setXmlVersion(xmlVersion);
	}

	/**
	 * Parse the content of the given input source as an XML document
	 * and return a new <code>XPathDocument</code> object.
	 * An <code>IllegalArgumentException</code> is thrown if the
	 * <code>InputSource</code> is <code>null</code>.
	 *
	 * @param inputSource
	 *            InputSource containing the content to be parsed.
	 *
	 * @return A new DOM Document object.
	 *
	 * @throws NullPointerException
	 *             If <code>inputSource</code> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if any I/O errors occur (for example, if the input source does not exist) or if any parse errors
	 *             occur
	 */
	private static Document parse(final InputSource inputSource) {
		checkNull("inputSource", inputSource);

		try {
			synchronized (builder) {
				return builder.parse(inputSource);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
