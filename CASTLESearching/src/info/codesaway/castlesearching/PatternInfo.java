package info.codesaway.castlesearching;

import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

public class PatternInfo {
	private final Pattern pattern;
	private final String field;
	private final String value;

	private final ThreadLocal<Matcher> matchers;

	public PatternInfo(final Pattern pattern, final String field, final String value) {
		this.pattern = pattern;
		this.field = field;
		this.value = value;

		this.matchers = ThreadLocal.withInitial(() -> this.pattern.matcher(""));
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public Matcher getMatcher() {
		return this.matchers.get();
	}

	public String getField() {
		return this.field;
	}

	public String getValue() {
		return this.value;
	}
}
