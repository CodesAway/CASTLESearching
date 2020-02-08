package info.codesaway.castlesearching;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * CASTLE Searching is an Eclipse plugin written by Amy Brennan-Luna which uses Apache Lucene to index data for faster searching
 *
 * <pre><code>
 *   A
 *   N
 *   A    S
 *   L    E
 * C Y    A T
 * O Z    R O
 * D E    C O
 * E R    H L
 * LOOKING@_@
 * EVERYTHING
 * </code></pre>
 *
 * @author Amy Brennan-Luna
 *
 */
// Reference: https://lucene.apache.org/core/8_3_1/index.html
public class CASTLESearching {
	/**
	 *
	 *
	 * @param step Lucene step
	 * @return
	 * @throws IOException
	 */
	public static Analyzer createAnalyzer(final LuceneStep step) throws IOException {
		// Read files from the state location, so can be modified by user
		CustomAnalyzer.Builder builder = CustomAnalyzer.builder(Activator.STATE_LOCATION).withTokenizer("standard");

		// Is this required for index analyzer?? flattenGraph
		// https://lucene.apache.org/solr/guide/6_6/filter-descriptions.html
		//				.addTokenFilter("flattenGraph)
		// https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-pattern-capture-tokenfilter.html

		//		if (step == LuceneStep.INDEX) {
		//Handle camel case names such as SQLException
		// (so can search for "sql" or "exception")
		// Note: include when query as well
		// (necessary to allow searching for camelCase names which may not be exact and where synonyms can help)

		// TODO: add settings to customize creating analzyer
		// (have options such as camelCase / PascalCase pattern)
		// (this way, user can use if want or specify their own pattern)

		String camelCaseRegex = "(?# Comment that is ignored when finding matches)"
				+ "(?# Capture group 1, entire match, so can use as individual token when querying)"
				+ "("
				+ "(?# Lower-case letters such as 'test')"
				+ "\\p{Ll}++"
				+ "(?# Upper-case letter followed by lower case letters such as 'Test')"
				+ "|\\p{Lu}\\p{Ll}++"
				+ "(?# Multiple capital letters such as in 'SQLException' and 'CONSTANT_VALUE')"
				+ "|(?:\\p{Lu}(?=\\p{Lu}|[^\\p{Lu}\\p{Ll}]|$))++"
				+ "(?# End of capture group 1)"
				+ ")";

		builder.addTokenFilter(PatternCaptureGroupFilterFactory.class, "camelCase pattern", camelCaseRegex

		// Number
				, "number pattern", "(\\d++)"
		// Underscore
		// TODO: see if needed and find example where it has a noticeable benefit
		//				, "underscore pattern", "([A-Za-z0-9]++)(?=_\\b)"
		);
		//		}

		// https://lucene.apache.org/solr/guide/6_6/filter-descriptions.html#FilterDescriptions-SynonymGraphFilter
		if (step != LuceneStep.INDEX) {
			// Only use SynonymGraphFilter on query
			// (per documentation)
			// An added benefit is that I don't need to reindex when adding synonyms
			// http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
			// TODO: add support for user specifying files (and allow editing on the preferences page)
			builder.addTokenFilter("synonymGraph", "synonyms", "abbreviations.txt");
			builder.addTokenFilter("synonymGraph", "synonyms", "synonyms.txt", "ignoreCase", "true");
		}

		builder.addTokenFilter("englishPossessive").addTokenFilter("lowercase");

		// Use to preserve original token, before stemming
		// (helps improve wildcard matching and fuzzy searching)
		// https://lucene.apache.org/solr/guide/6_6/language-analysis.html#LanguageAnalysis-KeywordRepeatFilterFactory
		builder.addTokenFilter("keywordRepeat");

		// Only remove stop words when index
		// Don't remove stop words
		// Keep them when query, since "this" is a stop word and is a keyword in Java (so may want to search with it)
		//		if (step == LuceneStep.INDEX) {
		//			builder.addTokenFilter("stop");
		//		}

		builder.addTokenFilter("porterstem")
				// Added since due to synonyms and stemmer, may lead to duplicate tokens in same position
				// (added to reduce index space and possibly improve performance)
				.addTokenFilter("removeDuplicates");

		Analyzer analyzer = builder.build();

		Map<String, Analyzer> analyzerMap = new HashMap<>();
		// Don't want to have stop words for "type" (otherwise filters out "if" and "for")
		// TODO: allow specifying different analyzers for different fields
		analyzerMap.put("type", new StandardAnalyzer(CharArraySet.EMPTY_SET));

		PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(analyzer, analyzerMap);
		return wrapper;
	}
}
