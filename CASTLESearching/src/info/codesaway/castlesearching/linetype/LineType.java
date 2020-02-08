package info.codesaway.castlesearching.linetype;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

public interface LineType {
	/**
	 * Test whether the specified trimmed line matches this line type
	 *
	 * @param trimmedLine
	 * @return <code>true</code> if the specified trimmed line matches this LineType
	 */
	boolean test(String trimmedLine);

	/**
	 * Adds the type to the document
	 *
	 * <p>May also add other fields to the document as desired</p>
	 *
	 * @param document the document to which to add the type
	 * @return the added type
	 */
	String addType(Document document);

	/**
	 * Add the specified line type to the document
	 *
	 * @param document
	 * @param lineType
	 * @return the passed line type
	 */
	static String addType(final Document document, final String lineType) {
		document.add(new TextField("type", lineType, Field.Store.YES));
		return lineType;
	}
}
