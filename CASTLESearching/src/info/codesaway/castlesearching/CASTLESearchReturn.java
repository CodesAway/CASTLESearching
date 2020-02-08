package info.codesaway.castlesearching;

import java.util.List;

import org.apache.lucene.search.ScoreDoc;
import org.eclipse.jdt.annotation.Nullable;

public class CASTLESearchReturn {

	private final List<CASTLESearchResultEntry> results;
	private final String message;
	private final boolean isIndexCreated;

	/**
	 * Last document returned in search (used to allow search after)
	 */
	@Nullable
	private final ScoreDoc lastDocument;

	public CASTLESearchReturn(final List<CASTLESearchResultEntry> results, final String message,
			final boolean isIndexCreated, @Nullable final ScoreDoc lastDocument) {
		this.results = results;
		this.message = message;
		this.isIndexCreated = isIndexCreated;
		this.lastDocument = lastDocument;
	}

	public List<CASTLESearchResultEntry> getResults() {
		return this.results;
	}

	public String getMessage() {
		return this.message;
	}

	public boolean isIndexCreated() {
		return this.isIndexCreated;
	}

	@Nullable
	public ScoreDoc getLastDocument() {
		return this.lastDocument;
	}
}
