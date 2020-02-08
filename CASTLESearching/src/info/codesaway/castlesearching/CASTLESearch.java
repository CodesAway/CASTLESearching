package info.codesaway.castlesearching;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.Operator;
import org.apache.lucene.search.Query;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class CASTLESearch {
	private final String text;
	private final long delay;
	private final boolean shouldSelectFirstResult;
	private final int hitLimit;
	private final Optional<Query> extraQuery;
	private final Path indexPath;
	private final Operator defaultOperator;
	private final boolean shouldIncludeComments;

	public CASTLESearch(final String text, final long delay, final boolean shouldSelectFirstResult, final int hitLimit,
			final Optional<Query> extraQuery, final Path indexPath, final Operator defaultOperator,
			final boolean shouldIncludeComments) {
		this.text = text;
		this.delay = delay;
		this.shouldSelectFirstResult = shouldSelectFirstResult;
		this.hitLimit = hitLimit;
		this.extraQuery = extraQuery;
		this.indexPath = indexPath;
		this.defaultOperator = defaultOperator;
		this.shouldIncludeComments = shouldIncludeComments;
	}

	public String getText() {
		return this.text;
	}

	public long getDelay() {
		return this.delay;
	}

	public boolean hasDelay() {
		return this.getDelay() != 0;
	}

	public boolean shouldSelectFirstResult() {
		return this.shouldSelectFirstResult;
	}

	public int getHitLimit() {
		return this.hitLimit;
	}

	public Optional<Query> getExtraQuery() {
		return this.extraQuery;
	}

	public Path getIndexPath() {
		return this.indexPath;
	}

	public Operator getDefaultOperator() {
		return this.defaultOperator;
	}

	public org.apache.lucene.queryparser.classic.QueryParser.Operator getClassicDefaultOperator() {
		switch (this.getDefaultOperator()) {
		case AND:
			return QueryParser.Operator.AND;
		case OR:
			return QueryParser.Operator.OR;
		default:
			return QueryParser.Operator.OR;
		}
	}

	public boolean shouldIncludeComments() {
		return this.shouldIncludeComments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.extraQuery, this.hitLimit, this.text, this.indexPath, this.defaultOperator,
				this.shouldIncludeComments);
	}

	@Override
	public boolean equals(final @Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		CASTLESearch other = (CASTLESearch) obj;
		return Objects.equals(this.extraQuery, other.extraQuery) && this.hitLimit == other.hitLimit
				&& Objects.equals(this.text, other.text) && Objects.equals(this.indexPath, other.indexPath)
				&& this.defaultOperator == other.defaultOperator
				&& this.shouldIncludeComments == other.shouldIncludeComments;
	}

	@Override
	public String toString() {
		@NonNull
		@SuppressWarnings("null")
		String toString = String.format(
				"CASTLE Searching top %d hits for %s%s (%s; defaultOperator = %s; includeComments = %s)", this.hitLimit,
				this.text, this.extraQuery.isPresent() ? " with extra query " + this.extraQuery.get() : "",
				this.indexPath, this.defaultOperator, this.shouldIncludeComments);

		return toString;
	}
}
