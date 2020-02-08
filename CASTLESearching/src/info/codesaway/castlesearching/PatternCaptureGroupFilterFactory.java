package info.codesaway.castlesearching;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.pattern.PatternCaptureGroupTokenFilter;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import info.codesaway.util.regex.Pattern;

/**
 * Factory for {@link PatternCaptureGroupTokenFilter}.
 *
 * @see PatternCaptureGroupTokenFilter
 */
public class PatternCaptureGroupFilterFactory extends TokenFilterFactory {

	private final java.util.regex.Pattern[] patterns;
	private final boolean preserveOriginal;

	public PatternCaptureGroupFilterFactory(final Map<String, String> args) {
		super(args);

		this.preserveOriginal = args.containsKey("preserve_original")
				? Boolean.parseBoolean(args.get("preserve_original"))
				: true;

		this.patterns = args.entrySet()
				.stream()
				// Any key which ends with "pattern" will become a pattern
				// (allows specifying multiple patters and using the key name to document code)
				// (for example "email pattern")
				.filter(e -> e.getKey().endsWith("pattern"))
				.map(Map.Entry::getValue)
				.map(Pattern::compile)
				// Map back to Java pattern (matches same stuff)
				.map(Pattern::getInternalPattern)
				.toArray(java.util.regex.Pattern[]::new);

		if (this.patterns.length == 0) {
			throw new IllegalArgumentException("Must specify a pattern");
		}
	}

	@Override
	public PatternCaptureGroupTokenFilter create(final TokenStream input) {
		return new PatternCaptureGroupTokenFilter(input, this.preserveOriginal, this.patterns);
	}
}
