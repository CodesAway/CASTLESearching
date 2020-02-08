package info.codesaway.castlesearching.indexer.java;

import org.apache.lucene.document.Document;
import org.eclipse.jdt.annotation.NonNullByDefault;

import info.codesaway.castlesearching.CommentType;

@NonNullByDefault
public class JavaIndexerRequest {
	private final String line;
	private final Document document;
	private final CommentType commentType;
	private final String previousLineType;
	private final String previousLine;

	public JavaIndexerRequest(final String line, final Document document, final CommentType commentType,
			final String previousLineType, final String previousLine) {
		this.line = line;
		this.document = document;
		this.commentType = commentType;
		this.previousLineType = previousLineType;
		this.previousLine = previousLine;
	}

	public String getLine() {
		return this.line;
	}

	public Document getDocument() {
		return this.document;
	}

	public CommentType getCommentType() {
		return this.commentType;
	}

	public String getPreviousLineType() {
		return this.previousLineType;
	}

	public String getPreviousLine() {
		return this.previousLine;
	}
}
