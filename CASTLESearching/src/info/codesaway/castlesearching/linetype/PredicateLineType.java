package info.codesaway.castlesearching.linetype;

import java.util.function.Predicate;

import org.apache.lucene.document.Document;

public class PredicateLineType implements LineType {
	private final Predicate<String> predicate;
	private final String type;

	public PredicateLineType(final Predicate<String> predicate, final String type) {
		this.predicate = predicate;
		this.type = type;
	}

	@Override
	public boolean test(final String trimmedLine) {
		return this.predicate.test(trimmedLine);
	}

	@Override
	public String addType(final Document document) {
		return LineType.addType(document, this.type);
	}

	public static PredicateLineType isEqual(final String text, final String type) {
		return new PredicateLineType(l -> l.equals(text), type);
	}

	public static PredicateLineType startsWith(final String text, final String type) {
		return new PredicateLineType(l -> l.startsWith(text), type);
	}
}
