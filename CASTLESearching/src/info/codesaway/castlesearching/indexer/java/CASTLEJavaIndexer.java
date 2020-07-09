package info.codesaway.castlesearching.indexer.java;

import static info.codesaway.castlesearching.linetype.LineType.addType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

import info.codesaway.castlesearching.Activator;
import info.codesaway.castlesearching.CASTLESearchResultEntry;
import info.codesaway.castlesearching.CASTLESearchingSettings;
import info.codesaway.castlesearching.CommentType;
import info.codesaway.castlesearching.PatternInfo;
import info.codesaway.castlesearching.indexer.CASTLEIndexer;
import info.codesaway.castlesearching.linetype.LineType;
import info.codesaway.castlesearching.util.RegexUtilities;
import info.codesaway.castlesearching.util.Utilities;
import info.codesaway.util.regex.Matcher;

public class CASTLEJavaIndexer {
	// https://en.wikipedia.org/wiki/List_of_Java_keywords
	private static final String JAVA_KEYWORD_REGEX = "(?:\\b(?:" + "abstract|assert|" + "boolean|break|byte|"
			+ "case|catch|char|class|continue|" + "default|do|double|" + "else|enum|extends|"
			+ "final(?:ly)?+|float|for|" + "if|implements|import|instanceof|int|interface|" + "long|" + "native|new|"
			+ "package|private|protected|public|" + "return|" + "short|static|strictfp|super|switch|synchronized|"
			+ "this|throws?+|transient|try|" + "void|volatile|" + "while|"
			// Literal values
			+ "true|null|false)"
			// Added in Java 9
			// + "exports|module|requires|"
			// Not used but reserved
			// + "const|goto)"
			+ "\\b)";

	private static final ThreadLocal<Matcher> javaStringTextMatchers = RegexUtilities
			.getThreadLocalMatcher("^\\s*+\"(?:\\\\\"|(?!\").)++\"[ ,){};]*+$");

	private static final String JAVA_IDENTIFIER_REGEX = "[A-Za-z_]\\w*+";

	// TODO: handle comments on same line as other values
	// (same thing done in CheckForJavaComment)
	private static final ThreadLocal<Matcher> JAVA_CONTENT_MATCHERS = RegexUtilities.getThreadLocalMatcher(
			"(?<string>" + RegexUtilities.STRING_REGEX + ")|" + "(?<keyword>" + JAVA_KEYWORD_REGEX + ")|"
			// \\x28 is open parenthesis, use code so parenthesis matching works
			// as expected
					+ "(?<method>" + JAVA_IDENTIFIER_REGEX + "(?=\\x28))");

	private static final Set<String> JAVA_COMMENT_LINE_TYPES = new HashSet<>(
			Arrays.asList("In block comment (empty line)", "In Javadoc (empty line)", "In block comment", "In Javadoc",
					"End block comment", "End Javadoc"));

	// TODO: indicate special types of assignment (such as copying from another
	// object, same field name)
	private static final ThreadLocal<Matcher> declarationAndAssignmentMatchers = RegexUtilities
			.getThreadLocalMatcher("^(?:\\s*+(?:public|protected|private|static|final)\\s++)*+"
					// Intentionally left as non-possessive at end so can handle
					// assignments like 'variable = true;'
					+ "(?:\\s*+(?!(?:return|package|continue|throw)\\b)" + "(?<class>(?:" + JAVA_IDENTIFIER_REGEX
					+ "\\.)*+" + JAVA_IDENTIFIER_REGEX + "(?:<[^>]++>|\\[\\])?+))?" + "(?:\\s*+(?!(?:continue)\\b)"
					+ "(?<var>(?:" + JAVA_IDENTIFIER_REGEX + "\\.)*+" + JAVA_IDENTIFIER_REGEX + "))"
					+ "(?:(?<assign>\\s*+=(?!=)\\s*+(?:(?:(?<assignValue>null|true|false);)?+|))|;)");

	private static final ThreadLocal<Matcher> variableAssignmentMatchers = RegexUtilities
			// Don't require to start at beginning of line
			// This way, can handle where declare and assign on same line
			// TODO: Change to allow any valid Java name (also prevent seeing
			// numbers as variable names)
			// Don't treat true / false / null as variables
			.getThreadLocalMatcher("(?:" + JAVA_IDENTIFIER_REGEX + "\\.)*+(?<name>" + JAVA_IDENTIFIER_REGEX + ")"
					+ "\\s*+=\\s*+(?:" + JAVA_IDENTIFIER_REGEX
					+ "\\.)*+(?:(?<copy>\\k<name>)|(?!(?:true|false|null)\\b)" + JAVA_IDENTIFIER_REGEX + ");$");

	private static final ThreadLocal<Matcher> methodDeclarationMatchers = RegexUtilities
			.getThreadLocalMatcher("^(?:\\s*+(?:public|protected|private|static|final)\\s++)*+"
					// Intentionally left as non-possessive at end so can handle
					// constructors like 'MyClass('
					+ "(?:\\s*+(?!(?:return|package|new)\\b)(?<returnType>(?:" + JAVA_IDENTIFIER_REGEX + "\\.)*+"
					+ JAVA_IDENTIFIER_REGEX + "(?:<" + JAVA_IDENTIFIER_REGEX + ">)?+))?"
					+ "\\s*+(?!(?:super|this|if|while)\\()(?<name>" + JAVA_IDENTIFIER_REGEX + ")\\(");

	private static final ThreadLocal<Matcher> throwExceptionMatchers = RegexUtilities
			.getThreadLocalMatcher("^throw new (?<name>" + JAVA_IDENTIFIER_REGEX + ")\\(");

	private static final ThreadLocal<Matcher> classDeclarationMatchers = RegexUtilities
			.getThreadLocalMatcher("^(?:\\s*+(?:public|protected|private|static|final)\\s++)*+"
					+ "(?<bodyType>class|interface|enum)" + "\\s++(?<name>" + JAVA_IDENTIFIER_REGEX + ")");

	private static final ThreadLocal<Matcher> invokeSpecificMethodMatchers = RegexUtilities
			// Handles if invoke after call something else (such as getter
			// followed by setter)
			.getThreadLocalMatcher("(?:" + JAVA_IDENTIFIER_REGEX
					+ "|\\(\\))\\.(?<method>(?:add|close|handle|log|put|save|set|start|stop|take|validate)\\w*+)\\(");

	@NonNullByDefault
	public static Map<String, String> getJavaFileRelatedFields(final Path path, final String filename) {
		Map<String, String> fields = new HashMap<>();

		for (PatternInfo info : CASTLESearchingSettings.JAVA_FILENAME_PATTERNS) {
			Matcher matcher = info.getMatcher().reset(filename);

			if (matcher.find()) {
				@NonNull
				@SuppressWarnings("null")
				String field = matcher.getReplacement(info.getField());

				@NonNull
				@SuppressWarnings("null")
				String value = matcher.getReplacement(info.getValue());

				fields.put(field, value);
			}
		}

		return fields;
	}

	public static JavaIndexerReturn indexJavaLine(final JavaIndexerRequest request) {
		String trimmedLine = request.getLine().trim();
		Document document = request.getDocument();
		CommentType commentType = request.getCommentType();

		String previousLineType = request.getPreviousLineType();
		String previousLine = request.getPreviousLine();

		// Initially set content to be the trimmed line
		// (will then change if part of the line is a comment)
		String content = trimmedLine;
		String comment = "";

		// Handle block comments
		// TODO: handle if has actual code on same line
		// (shouldn't since hard to read and code formatter should prevent)

		if (commentType == CommentType.NONE && !trimmedLine.isEmpty()) {
			int commentIndex = trimmedLine.indexOf("//");
			// Note: if already found a comment, don't care about checking for a
			// block comment
			// (short-circuit)
			int blockCommentIndex = commentIndex > -1 ? -2 : trimmedLine.indexOf("/*");

			// Short circuit case of single line comment
			if (commentIndex == 0) {
				// Entire line is single line comment
				commentType = CommentType.SINGLE;

				content = "";
				comment = trimmedLine;

				trimmedLine = "";
			} else if (blockCommentIndex == 0 && trimmedLine.lastIndexOf('/') == 0) {
				// Has block comment start and that's the only slash
				// (so doesn't have block comment end on same line)
				// Short-circuit to handle common case, so don't need to use
				// regular expression in CheckForJavaComment.check
				content = "";
				comment = trimmedLine;

				// charAt(0) = '/'
				// charAt(1) = '*'

				boolean isJavadocComment = (trimmedLine.length() > 2 && trimmedLine.charAt(2) == '*');

				// Logic from CheckForJavaComment.check

				if (isJavadocComment) {
					commentType = CommentType.JAVADOC;
					trimmedLine = trimmedLine.substring("/**".length());
				} else {
					commentType = CommentType.BLOCK;
					trimmedLine = trimmedLine.substring("/*".length());
				}

				// Logic from below (since part of a block comment)
				// Line is part of a block comment
				previousLineType = "";
				previousLine = "";
			} else if (commentIndex > -1 || blockCommentIndex > -1) {
				CheckForJavaComment.Result result = CheckForJavaComment.check(trimmedLine);

				commentType = result.getCommentType();
				trimmedLine = result.getTrimmedLine();
				content = result.getContent();
				comment = result.getComment();

				if (Utilities.in(commentType, CommentType.BLOCK, CommentType.JAVADOC)) {
					// Line is part of a block comment
					previousLineType = "";
					previousLine = "";
				}
			}
		} else if (Utilities.in(commentType, CommentType.BLOCK, CommentType.JAVADOC)) {
			// Line is part of block comment
			content = "";
			comment = trimmedLine;
		}
		// Don't need to handle
		// BLOCK_START (since would be set by CheckForJavaComment.check, which
		// would correctly set the content / comment)
		// NONE (since entire line is content and there is no comment;
		// this is the default)
		// SINGLE (since entire line is comment and handled by above if
		// SINGLE_BLOCK (since would be set by CheckForJavaComment.check, which
		// would correctly set the content / comment)

		// Index comment (even if empty?)
		document.add(new TextField("content", content, Field.Store.YES));

		if (!comment.isEmpty()) {
			// Only index comment if not empty
			document.add(new TextField("comment", comment, Field.Store.YES));
		}

		switch (commentType) {
		case BLOCK_START:
			commentType = CommentType.BLOCK;
			// Intentional fall through
			// line has stuff and want to indicate it
			// Then, next line is part of block comment
		case NONE:
			if (trimmedLine.isEmpty()) {
				// Don't set previousLineType and previousLine
				// Handle as if blank line doesn't exist
				// TODO: give option to not index blank lines
				addType(document, "empty line");
			} else {
				previousLineType = addJavaLineInfo(document, trimmedLine, previousLine, previousLineType);
				// Note: any trailing comments have already been removed
				previousLine = trimmedLine;
			}
			break;
		case SINGLE:
			// Entire line was single line comment

			// Note: Don't change previousLineType and previousLine
			// (treat as if comment line didn't exist)
			// (this way can mark more line types)

			addType(document, CASTLEIndexer.COMMENT);

			commentType = CommentType.NONE;
			break;
		case SINGLE_BLOCK:
			document.add(new TextField("type", "In " + commentType, Field.Store.YES));
			commentType = CommentType.NONE;

			break;
		case BLOCK:
		case JAVADOC:
			if (trimmedLine.isEmpty() || trimmedLine.equals("*")) {
				// Indicate blank lines within block comments
				// (this way they show up later in search results)
				String type = "In " + commentType + " (empty line)";
				document.add(new TextField("type", type, Field.Store.YES));
			} else {
				// Check if line ends the block comment
				boolean hasBlockCommentEnd = trimmedLine.contains("*/");
				// int blockCommentEnd = trimmedLine.indexOf("*/");

				String type = hasBlockCommentEnd ? "End " + commentType : "In " + commentType;

				// TODO: need to handle setting content versus comment if block comment ends and has other text on same line
				// (this case also messes up the idea of putting the comment at the end of the line)
				// (since in this case, the line would contain a comment end, which must be the beginning)
				// For example,
				// /* block start
				// block end */ other text
				// In this case, "block end */" would be the comment part, but must be first
				// (or, could put at end, but would need /* or /** comment start to make comment appear correct)
				// (might prefer doing this, so that can always put comment at end)
				// (also, it's a less common case to have a block commend end followed by other text on the same line)

				document.add(new TextField("type", type, Field.Store.YES));

				if (hasBlockCommentEnd) {
					commentType = CommentType.NONE;
				}
			}
			break;
		}

		return new JavaIndexerReturn(commentType, previousLineType, previousLine);
	}

	@NonNullByDefault
	private static String addJavaLineInfo(final Document document, final String trimmedLine, final String previousLine,
			final String previousLineType) {
		// String trimmedLine = line.trim();

		// if (trimmedLine.isEmpty()) {
		// return addType(document, "empty line");
		// }

		if (!previousLineType.isEmpty()) {
			if (trimmedLine.startsWith("? ") || trimmedLine.startsWith(": ") || trimmedLine.startsWith("&& ")
					|| trimmedLine.startsWith("|| ") || trimmedLine.startsWith("+ ") || trimmedLine.startsWith(", ")
					|| trimmedLine.startsWith("\"") || trimmedLine.startsWith(".")) {
				return addType(document, previousLineType);
			}
		}

		for (LineType lineType : CASTLESearchingSettings.JAVA_LINE_TYPES) {
			if (lineType.test(trimmedLine)) {
				return lineType.addType(document);
			}
		}

		// TODO: convert to PredicateLineType if desired

		Matcher declarationAndAssignmentMatcher = declarationAndAssignmentMatchers.get();

		if (declarationAndAssignmentMatcher.reset(trimmedLine).find()) {
			// System.out.printf("Assignment on line %s%n", line);

			String clazz = declarationAndAssignmentMatcher.group("class");
			String var = declarationAndAssignmentMatcher.group("var");

			if (clazz != null) {
				document.add(new TextField("assign", clazz, Field.Store.YES));
			}

			document.add(new TextField("var", var, Field.Store.YES));

			String basicType;

			boolean matchedAssign = declarationAndAssignmentMatcher.matched("assign");
			String assignValue = declarationAndAssignmentMatcher.group("assignValue");

			if (!matchedAssign) {
				basicType = "declare";
			} else if (assignValue != null) {
				basicType = "assign " + assignValue;
			} else {
				basicType = "assign";
			}

			if (matchedAssign) {
				Matcher variableAssignmentMatcher = variableAssignmentMatchers.get();

				if (variableAssignmentMatcher.reset(trimmedLine).find()) {
					boolean isCopy = variableAssignmentMatcher.matched("copy");

					if (isCopy) {
						basicType = "copy " + basicType;
					} else {
						basicType = "variable " + basicType;
					}
				}
			}

			String type = basicType + (clazz != null ? " " + clazz : "") + (var != null ? " " + var : "");

			return addType(document, type);
		}

		if (!trimmedLine.endsWith(";")) {
			Matcher methodDeclarationMatcher = methodDeclarationMatchers.get();

			if (methodDeclarationMatcher.reset(trimmedLine).find()) {
				boolean isMethod = methodDeclarationMatcher.matched("returnType");
				String name = methodDeclarationMatcher.group("name");

				if (isMethod) {
					document.add(new TextField("method", name, Field.Store.YES));
				}

				return addType(document, (isMethod ? "Method " : "Constructor ") + name);
			}
		}

		Matcher stringTextMatcher = javaStringTextMatchers.get();

		if (stringTextMatcher.reset(trimmedLine).matches()) {
			return addType(document, "String text");
		}

		Matcher invokeSpecificMethodMatcher = invokeSpecificMethodMatchers.get();

		if (invokeSpecificMethodMatcher.reset(trimmedLine).find()) {
			@NonNull
			@SuppressWarnings("null")
			String method = invokeSpecificMethodMatcher.group("method");
			return addType(document, method);
		}

		if (RegexUtilities.justSymbolsAndSpacesMatchers.get().reset(trimmedLine).matches()) {
			return addType(document, "symbols");
		}

		Matcher throwExceptionMatcher = throwExceptionMatchers.get();

		if (throwExceptionMatcher.reset(trimmedLine).find()) {
			String name = throwExceptionMatcher.group("name");
			return addType(document, "throw " + name);
		}

		Matcher classDeclarationMatcher = classDeclarationMatchers.get();

		if (classDeclarationMatcher.reset(trimmedLine).find()) {
			String bodyType = classDeclarationMatcher.group("bodyType");
			String name = classDeclarationMatcher.group("name");

			return addType(document, bodyType + " " + name);
		}

		if (!previousLineType.isEmpty()
				&& (previousLine.endsWith(",") || previousLine.endsWith("(") || previousLine.endsWith("="))) {
			return addType(document, previousLineType);
		}

		return "";
	}

	public static StyledString getJavaStyledContent(final CASTLESearchResultEntry entry) {
		String content = entry.getContent();

		if (JAVA_COMMENT_LINE_TYPES.contains(entry.getType())) {
			// No styling in in comments
			return new StyledString(content);
		}

		StyledString result = new StyledString();

		Matcher matcher = JAVA_CONTENT_MATCHERS.get().reset(content);

		int lastPosition = 0;

		while (matcher.find()) {
			result.append(content.substring(lastPosition, matcher.start()));

			Styler styler;
			// if (matcher.matched("string")) {
			// styler = JAVA_STRING_STYLER;
			if (matcher.matched("keyword")) {
				styler = Activator.BOLD_STYLER;
			} else if (matcher.matched("string")) {
				styler = Activator.BOLD_STYLER;
			} else if (matcher.matched("method")) {
				styler = Activator.BOLD_STYLER;
			} else {
				styler = null;
			}

			// Can always use getReplacement if desired
			// matcher.getReplacement("");

			result.append(matcher.group(), styler);

			lastPosition = matcher.end();
		}

		// Append tail
		return result.append(content.substring(lastPosition));
	}
}
