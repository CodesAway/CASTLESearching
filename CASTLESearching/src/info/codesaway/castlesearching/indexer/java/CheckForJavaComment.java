package info.codesaway.castlesearching.indexer.java;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

import info.codesaway.castlesearching.CommentType;
import info.codesaway.castlesearching.util.RegexUtilities;
import info.codesaway.util.regex.Matcher;

@NonNullByDefault
public class CheckForJavaComment {

	/**
	 * Regular expression used to help check for comments
	 * <ul>
	 * <li>Single line comment</li>
	 * <li>Block comment (possibly ending on same line, or possibly left open
	 * </li>
	 * </ul>
	 */
	// NOTE: group 0 will contain the comment
	// (including comment start / end if any)
	// (except if matches STRING_REGEX, which is skipped by the if blocks below)
	// (the purpose the matching a String literal is, for example,
	// to ensure we don't treat "//" within a String as starting a line comment)
	private static final ThreadLocal<Matcher> javaCommentMatchers = RegexUtilities.getThreadLocalMatcher("(?:"
			+ "//(?<comment_text>.*)" + "|" + RegexUtilities.STRING_REGEX
			+ "|(?<block_comment_start>/\\*(?<javadoc>\\*(?!/))?+)" + "(?<block_comment>(?:\\*(?!/)|[^*\\r\\n])*+)"
			// Optional block comment end
			+ "(?<block_comment_end>\\*/)?+" + ")");

	public static Result check(final String trimmedLine) {
		// Use regex to determine if block comment started on the specified line
		// Also handle checking for single line comment and removing any
		// trailing comment

		@SuppressWarnings("null")
		Matcher javaCommentMatcher = javaCommentMatchers.get().reset(trimmedLine);

		// Rest of line, ignoring the comment
		// (will be null if there is no comment)
		StringBuffer restOfLineBuffer = null;

		// Part of line which is a comment
		// (will be null if there is no comment)
		StringBuilder commentBuilder = null;

		CommentType commentType = CommentType.NONE;

		// System.out.println("CheckForJavaComment: " + trimmedLine);

		// TODO: need to track content versus comment

		while (javaCommentMatcher.find()) {
			// System.out.println(javaCommentMatcher);

			if (javaCommentMatcher.matched("comment_text")) {
				// Note: already handled before call this (handle basic case, so
				// don't need to do it here)
				// Line has single line comment
				// if (javaCommentMatcher.start() == 0) {
				// // Entire line was single-line comment
				// return new Result(CommentType.SINGLE, "");
				// } else {
				if (restOfLineBuffer == null) {
					restOfLineBuffer = new StringBuffer();
				}

				// Remove comment
				javaCommentMatcher.appendReplacement(restOfLineBuffer, "");

				// Append comment to comment builder
				if (commentBuilder == null) {
					commentBuilder = new StringBuilder();
				}

				commentBuilder.append(javaCommentMatcher.group());

				break;
				// }
			} else if (javaCommentMatcher.matched("block_comment_start")) {
				boolean hasCommentEnd = javaCommentMatcher.matched("block_comment_end");
				boolean isJavadocComment = javaCommentMatcher.matched("javadoc");

				if (javaCommentMatcher.start() == 0 && javaCommentMatcher.end() == trimmedLine.length()) {
					// Entire line is block comment
					CommentType resultCommentTypeommentType;

					@NonNull
					@SuppressWarnings("null")
					String comment = javaCommentMatcher.group("block_comment");

					if (hasCommentEnd) {
						resultCommentTypeommentType = CommentType.SINGLE_BLOCK;
					} else if (isJavadocComment) {
						resultCommentTypeommentType = CommentType.JAVADOC;
					} else {
						resultCommentTypeommentType = CommentType.BLOCK;
					}

					return new Result(resultCommentTypeommentType, comment, "", trimmedLine);
					// return new Result(resultCommentTypeommentType, "");
				}

				if (restOfLineBuffer == null) {
					restOfLineBuffer = new StringBuffer();
				}

				// Remove comment
				javaCommentMatcher.appendReplacement(restOfLineBuffer, "");

				// Append comment to comment builder
				if (commentBuilder == null) {
					commentBuilder = new StringBuilder();
				}

				commentBuilder.append(javaCommentMatcher.group());

				if (!hasCommentEnd) {
					// Rest of line is part of block comment
					commentType = CommentType.BLOCK_START;
					break;
				}

				// Comment started / stopped on the same line
				// (just remove comment)
				// (see if rest of line is something)
			}
		}

		if (restOfLineBuffer == null) {
			// There were no comments in line
			return new Result(CommentType.NONE, trimmedLine, trimmedLine, "");
		}

		String resultTrimmedLine = javaCommentMatcher.appendTail(restOfLineBuffer).toString().trim();

		if (resultTrimmedLine.isEmpty()) {
			// Entire line was comment
			// Such as /* abc */ /* cde */
			// Such as /* abc */ // cde
			if (commentType == CommentType.NONE) {
				commentType = CommentType.SINGLE_BLOCK;
			} else if (commentType == CommentType.BLOCK_START) {
				// Entire line was in multiple block comments
				// (last one was left open)
				commentType = CommentType.BLOCK;
			}

			return new Result(commentType, "", "", trimmedLine);
		} else {
			// Some of the line was a comment
			// Other part of line wasn't a comment
			// Will check rest of line to see if it matches something

			@NonNull
			@SuppressWarnings("null")
			String comment = commentBuilder == null ? "" : commentBuilder.toString();

			return new Result(commentType, resultTrimmedLine, resultTrimmedLine, comment);
		}
	}

	public static class Result {
		private final CommentType commentType;
		private final String trimmedLine;
		private final String content;
		private final String comment;

		/**
		 *
		 * @param commentType
		 * @param trimmedLine
		 * @param content
		 * @param comment
		 *            the comment (including the comment start / end, if any,
		 *            such as "// comment" or "/*")
		 */
		public Result(final CommentType commentType, final String trimmedLine, final String content,
				final String comment) {
			this.commentType = commentType;
			this.trimmedLine = trimmedLine;
			this.content = content;
			this.comment = comment;

			// System.out.println("CheckForJavaComment.Result:" +
			// this.getCommentType().name() + ":" + trimmedLine);
		}

		public CommentType getCommentType() {
			return this.commentType;
		}

		public String getTrimmedLine() {
			return this.trimmedLine;
		}

		public String getContent() {
			return this.content;
		}

		public String getComment() {
			return this.comment;
		}
	}
}
