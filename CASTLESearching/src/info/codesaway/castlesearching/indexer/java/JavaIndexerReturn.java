package info.codesaway.castlesearching.indexer.java;

import org.eclipse.jdt.annotation.NonNullByDefault;

import info.codesaway.castlesearching.CommentType;

@NonNullByDefault
public class JavaIndexerReturn {
	private final CommentType commentType;
	private final String previousLineType;
	private final String previousLine;

	public JavaIndexerReturn(final CommentType commentType, final String previousLineType, final String previousLine) {
		this.commentType = commentType;
		this.previousLineType = previousLineType;
		this.previousLine = previousLine;
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
