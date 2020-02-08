package info.codesaway.castlesearching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Display;

import info.codesaway.castlesearching.indexer.CASTLEIndexer;
import info.codesaway.castlesearching.linetype.LineType;
import info.codesaway.castlesearching.linetype.PredicateLineType;
import info.codesaway.castlesearching.searcher.CASTLESearcher;
import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;
import info.codesaway.util.regex.PatternSyntaxException;
import info.codesaway.util.xpath.XPathDocument;
import info.codesaway.util.xpath.XPathElement;
import info.codesaway.util.xpath.XPathNode;
import info.codesaway.util.xpath.XPathNodeList;

public class CASTLESearchingSettings {
	// TODO: when export settings, save this to the configuration file
	// (that way when user can modify settings, would increment this)
	// TODO: change to 3 when done
	// TODO: also support each file type having it's own version
	// (this way if update settigs for Java files, don't need to reindex JSP for
	// example)
	public static long DOCUMENT_VERSION;

	public static final List<PatternInfo> JAVA_FILENAME_PATTERNS = new ArrayList<>();

	public static final List<LineType> JAVA_LINE_TYPES = new ArrayList<>();

	// TODO: make use of the hit limit
	public static final CASTLESearcher SEARCHER_WORKSPACE = new CASTLESearcher("Workspace", CASTLEIndexer.INDEX_PATH,
			10);
	// TODO: how to allow incremental over the custom searchers?
	// public static final CASTLESearcher SEARCHER_INCREMENTAL = new
	// CASTLESearcher("Incremental",
	// CASTLEIndexer.INDEX_PATH, 100);

	// TODO: move to settings file
	// TODO: have setting for default hitLimit
	// private static final String OPTION_SEARCH_CONTROL_REPORT = "Control
	// Report";

	// Use LinkedHashMap so can use the set of values to populate the dropdown
	// (keeping the same order as defined in the file)
	private static Map<String, CASTLESearcher> SEARCHERS = new LinkedHashMap<>();

	// TODO: make setting to list file types to index
	// TODO: Setting file lists indexers, so read these to determine which
	// extensions to read
	private static Pattern FILES_TO_INDEX_PATTERN = Pattern.compile("(?!)");
	private static ThreadLocal<Matcher> FILES_TO_INDEX_MATCHER = ThreadLocal
			.withInitial(() -> FILES_TO_INDEX_PATTERN.matcher(""));

	private static String[] SEARCHER_NAMES;

	private static Thread WATCH_SETTINGS_THREAD;

	// TODO: does it need to be concurrent?
	private static final Map<Path, SearcherManager> SEARCHER_MANAGERS = new ConcurrentHashMap<>();

	public static void setFilesToIndexPattern(final Pattern filesToIndexPattern) {
		FILES_TO_INDEX_PATTERN = filesToIndexPattern;

		FILES_TO_INDEX_MATCHER = ThreadLocal.withInitial(() -> FILES_TO_INDEX_PATTERN.matcher(""));
	}

	public static void init(final CASTLESearchingView view) {
		loadSettings(view);

		// Add file watcher to check if settings file is modified
		WatchDirectory watchDirectory = new WatchDirectory(Activator.STATE_LOCATION, path -> {
			// System.out.println("Event occurred on path: " + path);

			if (path.equals(Activator.SETTINGS_PATH)) {
				Display display = view.getDisplay();

				if (display != null) {
					display.syncExec(CASTLESearchingSettings::refreshSettings);
				}
			}
		});

		WATCH_SETTINGS_THREAD = new Thread(watchDirectory);
		WATCH_SETTINGS_THREAD.start();
	}

	/**
	 * <p>
	 * Note: Run from UI thread
	 * </p>
	 */
	public static void refreshSettings() {
		if (CASTLESearchingView.INSTANCE == null) {
			return;
		}

		loadSettings(CASTLESearchingView.INSTANCE);

		// Index after refresh settings
		// (since may have changed what want to index, such as adding new types)
		CASTLESearchingView.INSTANCE.index();
	}

	/**
	 * <p>
	 * Note: Run from UI thread
	 * </p>
	 *
	 * @param view
	 */
	public static void loadSettings(final CASTLESearchingView view) {
		// Current comboText value
		// (will retain value if still valid after reload settings)

		// Must access from correct thread
		String searcherName = view.getSearcherName();

		File settingsFile = Activator.SETTINGS_FILE;

		if (settingsFile == null) {
			// Shouldn't occur, but don't want to get NullPointerException if
			// try to load settings before plugin is started
			return;
		}

		XPathDocument settings;
		try {
			if (settingsFile.createNewFile()) {
				// File doesn't exist, use default settings
				settings = new XPathDocument(DEFAULT_SETTINGS_XML);
				settings.saveXML(settingsFile, 4, 0);
			} else {
				settings = new XPathDocument(settingsFile);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (IllegalArgumentException e1) {
			// Could not parse file
			view.setStatus(CASTLESearchingView.ERROR_STATUS);
			view.setMessage("Could not load settings file");
			return;
		}

		XPathElement rootElement = settings.getRootElement();

		XPathElement indexersElement = rootElement.getChildElement("indexers");

		// Creates the regex to use to match the list of extensions
		// (if no indexers are specified with extensions, use the empty string)
		StringJoiner extensionsRegexJoiner = new StringJoiner("|", "\\.(?:", ")$").setEmptyValue("");
		// Pattern.compile("(?i)\\.(?:java)$");

		String extensionsRegex;

		if (indexersElement != null) {
			String versionString = indexersElement.getAttribute("version");

			try {
				long version = Long.parseLong(versionString);
				DOCUMENT_VERSION = version;
			} catch (NumberFormatException e) {
				// If version is not valid, use hashcode as version
				// (since goal of version is to see if version changes and to
				// reindex)
				DOCUMENT_VERSION = Long.MAX_VALUE - versionString.hashCode();
			}

			XPathNodeList<XPathElement> indexerElementss = indexersElement.getChildElements("indexer");

			for (XPathElement indexerElement : indexerElementss) {
				// TODO: also support pattern matching
				// (so can specify certain txt files be indexed one way with
				// option that remaining txt files be indexed a different way or
				// not indexed at all)
				String extension = indexerElement.getAttribute("ext");

				if (!extension.isEmpty()) {
					// Make literal pattern, to handle any special characters in
					// extension
					extensionsRegexJoiner.add(Pattern.literal(extension));
				}
			}

			extensionsRegex = extensionsRegexJoiner.toString();
		} else {
			extensionsRegex = "";
		}

		Pattern filesToIndexPattern;

		if (extensionsRegex.isEmpty()) {
			// Regex that always fails (don't match any files)
			filesToIndexPattern = Pattern.compile("(?!)");
		} else {
			// TODO: give option to be case-insensitive
			filesToIndexPattern = Pattern.compile(extensionsRegex);
		}

		setFilesToIndexPattern(filesToIndexPattern);

		// TODO: allow specifying each file extension
		// TODO: also allow specifying an indexer based on a pattern (such as if
		// want to handle certain txt files differently than others)
		XPathElement javaIndexer = rootElement.xpathElement("indexers/indexer[@ext='java']");

		// TODO: expand to allow any indexer to specify custom settings
		if (javaIndexer != null) {
			XPathNodeList<XPathNode> filenamePatterns = javaIndexer.xpathList("filename-patterns/filename-pattern");

			for (XPathNode node : filenamePatterns) {
				XPathElement patternNode = node.getChildElement("pattern");
				XPathElement fieldNode = node.getChildElement("field");
				XPathElement valueNode = node.getChildElement("value");

				if (patternNode != null && fieldNode != null && valueNode != null) {
					try {
						Pattern pattern = Pattern.compile(patternNode.getTextContent());
						String field = fieldNode.getTextContent();
						String value = valueNode.getTextContent();

						JAVA_FILENAME_PATTERNS.add(new PatternInfo(pattern, field, value));
					} catch (PatternSyntaxException e) {
						// TODO: error when compiling pattern
						// TODO: figure out how to log
					}
				}
			}

			XPathElement linetypesElement = javaIndexer.getChildElement("linetypes");

			XPathNodeList<XPathElement> linetypes = linetypesElement.getChildElements("linetype");

			for (XPathElement element : linetypes) {
				@SuppressWarnings("null")
				@NonNull
				String type = element.getAttribute("type");

				@SuppressWarnings("null")
				@NonNull
				String text = element.getTextContent();

				String condition = element.getAttribute("condition");

				switch (condition) {
				case "startsWith":
					JAVA_LINE_TYPES.add(PredicateLineType.startsWith(text, type));
					break;
				case "isEqual":
					JAVA_LINE_TYPES.add(PredicateLineType.isEqual(text, type));
					break;
				}
			}
		}

		SEARCHERS = new LinkedHashMap<>();
		SEARCHERS.put(SEARCHER_WORKSPACE.getName(), SEARCHER_WORKSPACE);
		// SEARCHERS.put(SEARCHER_INCREMENTAL.getName(), SEARCHER_INCREMENTAL);

		// Read searchers from XML
		XPathElement searchersElement = rootElement.getChildElement("searchers");

		if (searchersElement != null) {
			XPathNodeList<XPathElement> searchers = searchersElement.getChildElements("searcher");

			for (XPathElement searcher : searchers) {
				String name = searcher.getAttribute("name");
				String indexPathString = searcher.getAttribute("index-path");
				String hitLimitString = searcher.getAttribute("hit-limit");

				if (!name.isEmpty() && !indexPathString.isEmpty()) {
					int hitLimit;

					if (hitLimitString.isEmpty()) {
						hitLimit = CASTLESearchingView.DEFAULT_SMALL_HIT_LIMIT;
					} else {
						try {
							hitLimit = Integer.parseInt(hitLimitString);
						} catch (NumberFormatException e) {
							hitLimit = CASTLESearchingView.DEFAULT_SMALL_HIT_LIMIT;
						}
					}

					CASTLESearcher castleSearcher = new CASTLESearcher(name, Paths.get(indexPathString), hitLimit);
					SEARCHERS.put(castleSearcher.getName(), castleSearcher);
				}
			}
		}

		// TODO: move to settings file
		// INDEX_PATHS.put(OPTION_SEARCH_CONTROL_REPORT,
		// Paths.get("G:\\LogKeepIndexer"));
		SEARCHER_NAMES = SEARCHERS.keySet().toArray(new String[0]);
		view.setSearcherNames(SEARCHER_NAMES);

		// TODO: Read last value from settings
		maybeRefreshSearcherManagers();
		Activator.refreshStyles();

		if (getSearcher(searcherName) != null) {
			// Retain combo text since still valid
			view.setSearcherName(searcherName);
		} else {
			// TODO: isn't setting correctly when refresh font on initial view
			// load
			// (or more likely it's setting then being cleared)

			// TODO: should load from settings file? (need to add setting)
			view.setSearcherName(SEARCHER_WORKSPACE.getName());
		}
	}

	public static void maybeRefreshSearcherManagers() {
		// As part of initialization refresh managers
		// (allows quick refreshing; also, this way refreshes if closed then
		// reoppend later)
		for (SearcherManager searcherManager : SEARCHER_MANAGERS.values()) {
			try {
				searcherManager.maybeRefresh();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static Optional<String> getSearcherName(final int index1Based) {
		// Subtract 1 to make 0 based index so can read array
		int index = index1Based - 1;

		if (index < 0 || index >= SEARCHER_NAMES.length) {
			// Invalid index
			return Optional.empty();
		}

		return Optional.of(SEARCHER_NAMES[index]);
	}

	public static CASTLESearcher getSearcher(final String searcherName) {
		return SEARCHERS.get(searcherName);
	}

	/**
	 *
	 * @param indexPath
	 * @return the SearcherManager or <code>null</code> if one is not yet
	 *         available (such as if the index doesn't exist yet)
	 * @throws IOException
	 */
	public static SearcherManager getSearcherManager(final Path indexPath) throws IOException {
		SearcherManager searcherManager = SEARCHER_MANAGERS.get(indexPath);

		if (searcherManager == null) {
			searcherManager = createSearcherManager(indexPath);
		}

		return searcherManager;
	}

	/**
	 * Create SearcherManager used to search the index
	 *
	 * @return the searcher or <code>null</code> if the index does not exist
	 * @throws IOException
	 */
	private static SearcherManager createSearcherManager(final Path indexPath) throws IOException {
		// TODO: see about implement NRT (Near Real Time)
		// http://blog.mikemccandless.com/2011/11/near-real-time-readers-with-lucenes.html

		// http://blog.mikemccandless.com/2011/09/lucenes-searchermanager-simplifies.html

		Directory dir = FSDirectory.open(indexPath);

		// If index doesn't already exist, cannot search it
		if (!DirectoryReader.indexExists(dir)) {
			return null;
		}

		SearcherManager searcherManager = new SearcherManager(dir, null);
		SEARCHER_MANAGERS.put(indexPath, searcherManager);
		return searcherManager;
	}

	public static void closeSearcherManagers() {
		for (SearcherManager searcherManager : SEARCHER_MANAGERS.values()) {
			try {
				searcherManager.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void stopWatchingSettings() {
		if (WATCH_SETTINGS_THREAD == null) {
			return;
		}

		WATCH_SETTINGS_THREAD.interrupt();
		try {
			WATCH_SETTINGS_THREAD.join();
		} catch (InterruptedException e) {
		}
	}

	public static boolean shouldIndexFile(final Path path) {
		String pathString = path.toString();

		// TODO: support ignoring directories

		if (!FILES_TO_INDEX_MATCHER.get().reset(pathString).find()) {
			return false;
		}

		return true;
	}

	private static final String DEFAULT_SETTINGS_XML = "<settings xmlns='http://codesaway.info/CASTLESearching/'\r\n"
			+ "	xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\r\n"
			+ "	xsi:schemaLocation='http://codesaway.info/CASTLESearching/CASTLESearching.xsd'>\r\n"
			+ "	<indexers version='0'>\r\n" + "		<indexer ext='java'>\r\n" + "			<filename-patterns>\r\n"
			+ "				<filename-pattern>\r\n" + "             	<pattern>(?!)</pattern>\r\n"
			+ "             	<field>$0</field>\r\n" + "             	<value>$0</value>\r\n"
			+ "				</filename-pattern>\r\n" + "			</filename-patterns>\r\n" + "		\r\n"
			+ "			<linetypes>\r\n"
			+ "				<linetype type='open brace {' condition='isEqual'>{</linetype>\r\n"
			+ "				<linetype type='close brace {' condition='isEqual'>}</linetype>\r\n"
			+ "				<!-- Else if -->\r\n"
			+ "				<linetype type='else if' condition='startsWith'>} else if (</linetype>\r\n"
			+ "				<linetype type='else if' condition='startsWith'>else if (</linetype>\r\n"
			+ "				<!-- Else line -->\r\n"
			+ "				<linetype type='else line' condition='isEqual'>} else {</linetype>\r\n"
			+ "				<linetype type='else line' condition='isEqual'>else {</linetype>\r\n" + "			\r\n"
			+ "				<linetype type='if declaration' condition='startsWith'>if (</linetype>\r\n"
			+ "				<linetype type='for loop declaration' condition='startsWith'>for (</linetype>\r\n"
			+ "				<linetype type='while loop declaration' condition='startsWith'>while (</linetype>\r\n"
			+ "			\r\n" + "				<linetype type='return void' condition='isEqual'>return;</linetype>\r\n"
			+ "				<linetype type='return null' condition='isEqual'>return null;</linetype>\r\n"
			+ "				<linetype type='return' condition='startsWith'>return </linetype>\r\n" + "			\r\n"
			+ "				<linetype type='continue' condition='isEqual'>continue;</linetype>\r\n"
			+ "				<linetype type='break' condition='isEqual'>break;</linetype>	\r\n" + "\r\n"
			+ "				<linetype type='package' condition='startsWith'>package </linetype>\r\n"
			+ "				<linetype type='import static' condition='startsWith'>import static </linetype>\r\n"
			+ "				<linetype type='import' condition='startsWith'>import </linetype>\r\n" + "\r\n"
			+ "				<linetype type='Override' condition='isEqual'>@Override</linetype>\r\n"
			+ "				<linetype type='SuppressWarnings' condition='startsWith'>@SuppressWarnings</linetype>\r\n"
			+ "\r\n"
			+ "				<linetype type='System.out' condition='startsWith'>System.out.print</linetype>\r\n"
			+ "				<linetype type='System.err' condition='startsWith'>System.err.print</linetype>\r\n"
			+ "				<linetype type='System.exit' condition='startsWith'>System.exit()</linetype>\r\n" + "\r\n"
			+ "				<linetype type='throws' condition='startsWith'>throws </linetype>			\r\n"
			+ "			</linetypes>\r\n" + "		</indexer>\r\n" + "	</indexers>\r\n" + "</settings>";
}
