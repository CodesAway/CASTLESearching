package info.codesaway.castlesearching.jobs;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;

import info.codesaway.castlesearching.CASTLESearch;
import info.codesaway.castlesearching.CASTLESearchResult;
import info.codesaway.castlesearching.CASTLESearchResultEntry;
import info.codesaway.castlesearching.CASTLESearching;
import info.codesaway.castlesearching.CASTLESearchingSettings;
import info.codesaway.castlesearching.CASTLESearchingView;
import info.codesaway.castlesearching.indexer.CASTLEIndexer;
import info.codesaway.util.indexer.LuceneStep;

public class CASTLESearchJob extends Job {

	private final CASTLESearchingView view;
	private CASTLESearch currentSearch;
	// TODO: implement so when get to end to search after and append these
	// results to the existing results
	private ScoreDoc searchAfter;

	private static final String[] CONTENT_AND_COMMENT_FIELDS = { "content", "comment" };

	public CASTLESearchJob(final CASTLESearchingView view) {
		super("CASTLE Searching");

		this.view = view;
		// Set priority as short since typically takes less than 1 second to run
		// (usually around 10ms)
		this.setPriority(Job.SHORT);
	}

	// private Label getStatusLabel() {
	// return this.view.getStatusLabel();
	// }
	//
	// private Label getMessageLabel() {
	// return this.view.getMessageLabel();
	// }
	//
	// private TableViewer getViewer() {
	// return this.view.getViewer();
	// }

	public void handleSearch(final CASTLESearch newSearch) {
		// System.out.println("Delay " + newSearch.getDelay() + ": " +
		// newSearch);

		// System.out.println("Searching for " + newSearch.getText());

		// If blank, don't search
		if (newSearch.getText().isEmpty()) {
			// System.out.println("Cancel since empty search");
			this.cancel();
			return;
		}
		// // // Don't need to perform search again if same text as last search
		// // // (if press enter, delay is 0, do search even if same search,
		// // // such as if new files were indexed)
		// // // Allows searching after index finishes building for first time
		// // if (newSearch.getDelay() > 0 &&
		// newSearch.getText().equals(this.view.getLastSearch())) {
		// // // Cancel current job
		// // // Handles case that user did a search, deleted the text, then
		// typed the same text
		// // // In this case, we don't want to search again
		// // // However, we also want to cancel the current search, since the
		// user was tying but we don't want to search for what they were tying
		// // // For example
		// // // 1) Typed "test" and did a search
		// // // 2) Cleared the text
		// // // 3) Typed "test" again
		// // // 4) Wouldn't want to search for "test", but also wouldn't want
		// to search for "tes", the last search likely done,
		// // // (this search is scheduled with a delay since the user released
		// a key, but we don't want to search for it)
		// // System.out.println("Cancel since same text");
		// // this.cancel();
		// // return;
		// // }

		// searchJob.cancel();

		boolean scheduleNewSearch;

		// Compare current search with new search to see if should cancel
		// current search
		if (newSearch.getDelay() == 0) {
			// Run new search with no delay (such as if pressed enter)
			// (always rerun search, since even if identical, may have indexed
			// additional files)
			// Could also have changed hitLimit (for example, pressed CTRL +
			// Enter instead of just ENTER)
			scheduleNewSearch = true;
		} else if (this.currentSearch == null) {
			// No current search, so run new search;
			scheduleNewSearch = true;
		} else if (newSearch.equals(this.currentSearch)) {
			// Running same search, ignore
			// (can ignore since if new search has no delay, earlier "else if"
			// would say to schedule the new search
			// System.out.println("Same search: " + newSearch);

			scheduleNewSearch = false;
			// System.out.println("Ignore search");

			// TODO: if search location changes or if settings change, should
			// search again (change option in dropdown)

		} else if (newSearch.getText().equals(this.currentSearch.getText())) {

			// Different search, but same text
			// System.out.println("Different search, but same text: " +
			// newSearch);
			// New search has a delay (otherwise earlier "else if" would
			// schedule the new search

			if (newSearch.getExtraQuery().isPresent() && this.currentSearch.getExtraQuery().isPresent()
					&& !newSearch.getExtraQuery().equals(this.currentSearch.getExtraQuery())) {
				// Uncommon scenario where user switches to a new tab while on
				// incremental search and pastes the same text as the prior
				// search
				// In this case, it's a delayed search (since entered wasn't
				// pressed)
				// It's the same text, but a different tab
				// So, incremental search may have different results since the
				// tab changed
				// System.out.println("Different tab for " + newSearch);
				scheduleNewSearch = true;
			} else {
				// System.out.println("Ignore search");
				scheduleNewSearch = false;
			}
		} else {
			// Searching is different
			// (cancel current job so new one can run)
			// System.out.println("New search: " + newSearch);

			scheduleNewSearch = true;
		}

		if (scheduleNewSearch) {
			this.cancel();
			this.schedule(newSearch);
		}
	}

	private void schedule(final CASTLESearch search) {
		this.currentSearch = search;
		this.searchAfter = null;

		this.schedule(search.getDelay());
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		Display display = this.view.getDisplay();

		try {
			// System.out.println("Searching for " + this.searchText);

			// display.syncExec(() -> {
			// // Run in UI
			// // System.out.println("Set searching status");
			// // this.getStatusLabel().setText("Searching...");
			//
			// // TODO: handle fact that could change by another thread
			// this.view.setLastSearch(this.currentSearch.getText());
			// });

			// System.out.println("Run: " + this.currentSearch);
			CASTLESearchResult result = search(this.currentSearch, this.searchAfter);
			this.searchAfter = result.getLastDocument();

			if (monitor.isCanceled()) {
				// System.out.println("Canceled?!");
				return Status.CANCEL_STATUS;
			}

			// System.out.println("Done searching: index? " +
			// results.isIndexCreated());

			// TODO: if performed search while index wasn't created, once index
			// is created,
			// perform search again automatically
			// (this way, user can see search results once available)
			this.view.setIndexCreated(result.isIndexCreated());

			// var:test file:compare

			if (display != null) {
				display.syncExec(() -> {
					// Run in UI
					List<CASTLESearchResultEntry> resultsList = result.getResults();

					this.view.setResults(resultsList);
					this.view.addSearch(result);

					if (!resultsList.isEmpty()) {
						if (this.currentSearch.shouldSelectFirstResult()) {
							this.view.selectAndRevealTopResult();
						} else {
							// Show top of results (since if at end when ran
							// last query, would likely want
							// to see the top result when perform a search)
							// Don't want to select item, just want to reveal it
							this.view.revealTopResult();
						}
					}

					this.view.setLastSearchDoneTime(System.currentTimeMillis());

					if (this.view.getStatus().equals(CASTLESearchingView.ERROR_STATUS)) {
						if (CASTLESearchingView.isIndexing()) {
							this.view.setStatus(CASTLESearchingView.INDEXING_STATUS);
						} else {
							this.view.setStatus("");
						}
					}

					this.view.setMessage(result.getMessage());
				});
			}
		} catch (IOException e) {
			if (display != null) {
				display.syncExec(() -> {
					// Run in UI
					this.view.setStatus("ERROR");
					this.view.setMessage("Could not search. Please try again later.");
				});
				// Also catch IllegalArgumentException in case has error in
				// Regex query
			}
		} catch (QueryNodeException | IllegalArgumentException | ParseException e) {
			if (display != null) {
				display.syncExec(() -> {
					// Run in UI
					this.view.setStatus("ERROR");
					this.view.setMessage("Search query is not valid.");
				});
			}
		}

		return Status.OK_STATUS;
	}

	public static CASTLESearchResult search(final CASTLESearch search, @Nullable final ScoreDoc searchAfter)
			throws IOException, QueryNodeException, ParseException {

		// TODO: add menu option to do full reindex

		// https://www.baeldung.com/lucene
		// https://howtodoinjava.com/lucene/lucene-index-search-examples/

		// TODO: make more generic to handle multiple SearchManagers
		SearcherManager searcherManager = CASTLESearchingSettings
				.getSearcherManager(search.getSearcher().getIndexPath());

		if (searcherManager == null) {
			String message = "Cannot query until index is initialized. Your query will run shortly.";
			return new CASTLESearchResult(search, Collections.emptyList(), message, false, null);
		}

		IndexSearcher searcher = searcherManager.acquire();

		List<CASTLESearchResultEntry> results = new ArrayList<>();
		String message;
		ScoreDoc lastDocument = null;
		try {
			// TODO: Handle abbreviation versus normal (such as esht for
			// escheat)
			// Reference: https://www.baeldung.com/lucene-analyzers
			Analyzer analyzer = CASTLESearching.createAnalyzer(LuceneStep.QUERY);

			// StandardQueryTreeBuilder builder = new
			// StandardQueryTreeBuilder();
			// builder.set

			StandardQueryParser standardQueryParser = new StandardQueryParser(analyzer);
			standardQueryParser.setDefaultOperator(search.getDefaultOperator());

			// TODO: how to support this using Classic parser, which doesn't support this

			// Allow searching for numeric range on "line" field
			// https://github.com/apache/lucene-solr/blob/master/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestPointQueryParser.java
			Map<String, PointsConfig> pointsConfigMap = new HashMap<>();
			pointsConfigMap.put("line", new PointsConfig(NumberFormat.getIntegerInstance(Locale.ROOT), Integer.class));

			standardQueryParser.setPointsConfigMap(pointsConfigMap);

			Query query;
			if (search.shouldIncludeComments()) {
				MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(CONTENT_AND_COMMENT_FIELDS,
						analyzer);
				multiFieldQueryParser.setDefaultOperator(search.getClassicDefaultOperator());

				query = multiFieldQueryParser.parse(search.getText());
			} else {
				query = standardQueryParser.parse(search.getText(), "content");
			}

			// Set commented out lines as lower priority
			// (whereas Javadoc keeps the same priority)
			String commentedOutQueryText = "*:*" + " AND NOT comment";
			Query commentedOutQuery = standardQueryParser.parse(commentedOutQueryText, "type");

			// Mark as lower priority for search
			// (acts as a "not in")
			// https://stackoverflow.com/a/12109781
			String lowPriorityQueryText = "*:*" + " AND NOT import" + " AND NOT empty" + " AND NOT \"close brace\""
					+ " AND NOT symbols" + " AND NOT \"else line\"" + " AND NOT \"return null\""
					+ " AND NOT \"return void\"";
			Query lowPriorityQuery = standardQueryParser.parse(lowPriorityQueryText, "type");

			BooleanQuery.Builder builder = new BooleanQuery.Builder()
					.add(new BoostQuery(query, 1f), BooleanClause.Occur.MUST)
					.add(new BoostQuery(commentedOutQuery, 0.5f), BooleanClause.Occur.SHOULD)
					.add(new BoostQuery(lowPriorityQuery, 0.75f), BooleanClause.Occur.SHOULD);

			if (search.getExtraQuery().isPresent()) {
				builder.add(search.getExtraQuery().get(), BooleanClause.Occur.SHOULD);
			}

			BooleanQuery booleanQuery = builder.build();

			TopDocs hits;
			if (searchAfter != null) {
				hits = searcher.searchAfter(searchAfter, booleanQuery, search.getHitLimit());
			} else {
				hits = searcher.search(booleanQuery, search.getHitLimit());
			}

			long count = hits.totalHits.value;
			boolean isRelationEqualTo = hits.totalHits.relation == Relation.EQUAL_TO;

			String totalResultsMessage;

			if (isRelationEqualTo) {
				if (count == 0) {
					totalResultsMessage = "There are no results";
				} else if (count == 1) {
					totalResultsMessage = "Total Results: 1 hit";
				} else if (count <= search.getHitLimit()) {
					totalResultsMessage = "Total Results: " + count + " hits";
				} else {
					totalResultsMessage = String.format("Showing Results 1-%d of %d hits", search.getHitLimit(), count);
				}
			} else {
				totalResultsMessage = String.format("Showing Results 1-%d of over %d hits", search.getHitLimit(),
						count);
			}

			// 1/4/2020 - Don't need to show how long it took (it's fast, we get
			// it)
			// The time portion just distracts from the results
			message = totalResultsMessage;

			// message = String.format("%s (it took %s %s)%n",
			// totalResultsMessage,
			// seconds, secondsString);

			/** Highlighter Code Start ****/
			/*
			 * //
			 * https://howtodoinjava.com/lucene/lucene-search-highlight-example/
			 *
			 * //Uses HTML &lt;B&gt;&lt;/B&gt; tag to highlight the searched
			 * terms Formatter formatter = new SimpleHTMLFormatter();
			 *
			 * //It scores text fragments by the number of unique query terms
			 * found //Basically the matching score in layman terms QueryScorer
			 * scorer = new QueryScorer(query);
			 *
			 * //used to markup highlighted terms found in the best sections of
			 * a text Highlighter highlighter = new Highlighter(formatter,
			 * scorer);
			 *
			 * //It breaks text up into same-size texts but does not split up
			 * spans Fragmenter fragmenter = new SimpleSpanFragmenter(scorer,
			 * 10);
			 *
			 * //breaks text up into same-size fragments with no concerns over
			 * spotting sentence boundaries. //Fragmenter fragmenter = new
			 * SimpleFragmenter(10);
			 *
			 * //set fragmenter to highlighter
			 * highlighter.setTextFragmenter(fragmenter);
			 *
			 * //Iterate over found results for (ScoreDoc scoreDoc :
			 * hits.scoreDocs) { int docid = scoreDoc.doc; Document doc =
			 * searcher.doc(docid); String title =
			 * doc.get(PathWithTerm.FULL_PATH_FIELD);
			 *
			 * //Printing - to which document result belongs System.out.println(
			 * "Path " + " : " + title);
			 *
			 * //Get stored text from found document String text =
			 * doc.get("content");
			 *
			 * //Create token stream // TokenStream stream =
			 * TokenSources.getAnyTokenStream(searcher.getIndexReader(), docid,
			 * "content", analyzer); TokenStream stream =
			 * analyzer.tokenStream("content", text);
			 *
			 * //Get highlighted text fragments String[] frags; try { frags =
			 * highlighter.getBestFragments(stream, text, 10); } catch
			 * (InvalidTokenOffsetsException e) { e.printStackTrace(); frags =
			 * new String[0]; } for (String frag : frags) {
			 * System.out.println("=======================");
			 * System.out.println(frag); } }
			 */

			int resultIndex = 0;

			if (hits.scoreDocs.length > 0) {
				lastDocument = hits.scoreDocs[hits.scoreDocs.length - 1];
			}

			for (ScoreDoc sd : hits.scoreDocs) {
				Document d = searcher.doc(sd.doc);
				String path = d.get(CASTLEIndexer.FULL_PATH_FIELD);
				String file = d.get("file");
				String element = d.get("element");
				String line = d.get("line");
				String type = d.get("type");
				// String date = d.get("date");
				String content = d.get("content");
				String extension = d.get("ext");

				if (content != null) {
					content = content.trim();
				} else {
					content = "";
				}

				if (element == null) {
					element = "";
				}

				if (type == null) {
					type = "";
				}

				String comment = d.get("comment");

				if (comment != null && !comment.isEmpty()) {
					// Show comment text after the content
					// (so can focus attention on the content first)
					content = content + " " + comment;
				}

				resultIndex++;

				results.add(
						new CASTLESearchResultEntry(resultIndex, file, element, line, content, type, path, extension));

				// if (isTesting && type != null) {
				// System.out.println("Type: " + type);
				// }

				// System.out.printf("%sFound match in (%s:%s)%s - %s%n", type
				// != null ? "(" + type + ") " : "", file, line,
				// date != null ? " for " + date : "",
				// path);
			}
		} finally {
			searcherManager.release(searcher);
			searcher = null;
		}

		return new CASTLESearchResult(search, results, message, true, lastDocument);
	}
}
