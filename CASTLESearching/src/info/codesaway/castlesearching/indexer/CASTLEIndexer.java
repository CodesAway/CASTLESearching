package info.codesaway.castlesearching.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.DefaultLineTracker;

import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import info.codesaway.castlesearching.Activator;
import info.codesaway.castlesearching.CASTLESearching;
import info.codesaway.castlesearching.CASTLESearchingSettings;
import info.codesaway.castlesearching.CASTLESearchingView;
import info.codesaway.castlesearching.CommentType;
import info.codesaway.castlesearching.DocumentInfo;
import info.codesaway.castlesearching.LuceneStep;
import info.codesaway.castlesearching.indexer.java.CASTLEJavaIndexer;
import info.codesaway.castlesearching.indexer.java.JavaIndexerRequest;
import info.codesaway.castlesearching.indexer.java.JavaIndexerReturn;
import info.codesaway.castlesearching.jobs.CASTLEIndexJob;
import info.codesaway.castlesearching.util.DateUtilities;
import info.codesaway.castlesearching.util.JDTUtilities;
import info.codesaway.castlesearching.util.PathWithLastModified;
import info.codesaway.castlesearching.util.PathWithTerm;
import info.codesaway.util.regex.Matcher;

public class CASTLEIndexer {
	// private static final List<Path> DIRECTORIES =
	// Arrays.asList(Activator.WORKSPACE_PATH);
	private static SearcherManager SEARCHER_MANAGER;

	public static final Path INDEX_PATH = Activator.STATE_LOCATION.resolve("WorkspaceIndex");

	private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	/**
	 * Match metadocuments which store the document_version
	 */
	private static final Term METADOCUMENT_TERM = new Term("metadocument", "meta");

	@NonNull
	public static final String COMMENT = "comment";

	private static IndexWriter INDEX_WRITER;

	// TODO: make settings
	// Number of files to index at once before committing
	// (especially needed to handle initial indexing when all documents need to
	// be indexed)
	// (this way, once a group of files have been indexed, they can be queried,
	// while the rest of the files are still being indexed)
	// https://stackoverflow.com/questions/32269632/writing-to-lucene-index-one-document-at-a-time-slows-down-over-time
	private static int INDEX_GROUP_COUNT = 100;

	private static int CANCEL_CHECK_COUNT = 200;

	// Number of documents to read on first pass before commiting
	// (keep this number low to see quick results when perform full rebuid of
	// index)
	private static int INITIALLY_READ_COUNT = 10;

	private static final int SECONDS_PER_MINUTE = 60;
	private static final int SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;

	public static final String FULL_PATH_FIELD = "fullpath";
	public static final String PATH_FIELD = "path";
	public static final String PATHNAME_FIELD = "pathname";

	// public static FieldType STORED_INDEXED = getStoredIndexedFieldType();
	//
	// private static FieldType getStoredIndexedFieldType() {
	// FieldType fieldType = new FieldType();
	// fieldType.setStored(true);
	// // TODO: is this the right IndexOption??
	// // (using for Date field)
	// fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
	// fieldType.freeze();
	//
	// return fieldType;
	// }

	private static IndexWriter createWriter() throws IOException {
		FSDirectory dir = FSDirectory.open(INDEX_PATH);

		Analyzer analyzer = CASTLESearching.createAnalyzer(LuceneStep.INDEX);

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(dir, config);
		return writer;
	}

	private static IndexWriter getWriter() throws IOException {
		if (INDEX_WRITER != null) {
			return INDEX_WRITER;
		}

		return INDEX_WRITER = createWriter();
	}

	public static void closeWriter() {
		if (INDEX_WRITER != null) {
			try {
				INDEX_WRITER.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static Stream<PathWithLastModified> walkDirectories() {
		// System.out.println("Walk directories: " + directories);

		//		List<Path> directories = new ArrayList<>();
		//		directories.add(Activator.WORKSPACE_PATH);

		Stream<PathWithLastModified> result = Stream.empty();

		// Add any external projects (not a child of the workspace path)
		for (IProject project : Activator.WORKSPACE_ROOT.getProjects()) {
			@NonNull
			@SuppressWarnings("null")
			String projectName = project.getName();
			Path projectPath = project.getLocation().toFile().toPath();

			Stream<PathWithLastModified> projectStream;
			try {
				projectStream = Files.walk(projectPath)
						.filter(CASTLESearchingSettings::shouldIndexFile)
						.map(p -> new PathWithLastModified(projectName, p));
			} catch (IOException e) {
				projectStream = Stream.empty();
			}

			result = Stream.concat(result, projectStream);

			// https://stackoverflow.com/a/15169614
			//			boolean isChild = projectPath.startsWith(Activator.WORKSPACE_PATH);

			//			if (!isChild) {
			//				System.out.println("External project: " + projectPath);
			//				directories.add(projectPath);
			//			}
		}

		return result;

		//		try (Stream<Path> stream = directories
		//				// Multi-threaded
		//				.parallelStream()
		//				.flatMap(t -> {
		//					try {
		//						return Files.walk(t);
		//					} catch (IOException e1) {
		//						// Return empty stream if have error
		//						return Stream.empty();
		//					}
		//				})) {
		//
		//			return stream.parallel()
		//					.filter(CASTLESearchingSettings::shouldIndexFile)
		//					.map();
		//		}
	}

	private static boolean shouldIndex(final PathWithLastModified path, final Map<String, DocumentInfo> documents) {

		DocumentInfo doc = documents.get(path.getPathname());

		if (doc == null) {
			// Newly added file, should index
			return true;
		}

		long lastModifiedValue = doc.getLastModified();
		long fileLastModified = path.getLastModified();

		long documentVersion = doc.getDocumentVersion();

		boolean isModified = fileLastModified != lastModifiedValue
				// TODO: change to have version per indexer (based on file
				// extension)
				|| CASTLESearchingSettings.DOCUMENT_VERSION != documentVersion;

		return isModified;
	}

	public static void rebuildEntireIndex() {
		if (CASTLESearchingView.INSTANCE == null) {
			//			System.out.println("View INSTANCE is null");
			return;
		}

		//		System.out.println("Cancel indexing");
		CASTLESearchingView.cancelIndexing();

		try {
			IndexWriter writer = getWriter();
			//			System.out.println("Created writer");
			// Set all metadocuments to have the same document version
			// Will then index, which should be a different value
			long documentVersion = (CASTLESearchingSettings.DOCUMENT_VERSION != Long.MIN_VALUE ? Long.MIN_VALUE
					: Long.MAX_VALUE);

			//			System.out.println("Update document version");
			writer.updateNumericDocValue(METADOCUMENT_TERM, "documentVersion", documentVersion);
			//			System.out.println("Done updating document version");

			commit(writer);
		} catch (IOException e) {
			CASTLESearchingView.INSTANCE.setStatus(CASTLESearchingView.ERROR_STATUS);
			CASTLESearchingView.INSTANCE.setMessage("Cannot rebuild index");
			return;
		}

		//		System.out.println("Start indexing");
		CASTLESearchingView.INSTANCE.index();
	}

	public static String incrementalRebuildIndex(final CASTLEIndexJob castleIndexJob, final IProgressMonitor monitor)
			throws IOException {
		IndexSearcher searcher = getSearcher();

		LocalDateTime startTime = LocalDateTime.now();

		Map<String, DocumentInfo> documents = new HashMap<>();
		List<Term> deleteDocuments = new ArrayList<>();

		if (searcher != null) {
			// Determine when last modified file
			// (to allow incremental reindexing)
			for (LeafReaderContext context : searcher.getIndexReader().leaves()) {
				LeafReader leafReader = context.reader();
				Bits liveDocs = leafReader.getLiveDocs();

				BinaryDocValues pathnameDocValues = DocValues.getBinary(leafReader, PATHNAME_FIELD);

				// Will be iterating over both groups of values
				// (uses pathnameDocValues as the main iteration)
				NumericDocValues lastModifiedDocValues = DocValues.getNumeric(leafReader, "fileLastModified");
				int lastModifiedDocId = lastModifiedDocValues.nextDoc();

				// Used to track which version of the code created the document
				// (this way as changes are made to how the documents are
				// indexed, can update the existing documents)
				NumericDocValues documentVersionDocValues = DocValues.getNumeric(leafReader, "documentVersion");
				int documentVersionDocId = documentVersionDocValues.nextDoc();

				while (pathnameDocValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
					// https://stackoverflow.com/a/15570353
					int docID = pathnameDocValues.docID();

					// Ignore deleted documents
					if (liveDocs != null && !liveDocs.get(docID)) {
						continue;
					}

					// int docKey = docID + context.docBase;

					// Find the last modified value corresponding to the
					// document
					// (if none, use 0, so will show as file has been modified)
					// (handles case if each document doesn't have the pathname
					// and fileLastModified doc values)
					// (should never occur, but handling just in case)
					while (lastModifiedDocId < docID) {
						lastModifiedDocId = lastModifiedDocValues.nextDoc();
					}

					while (documentVersionDocId < docID) {
						documentVersionDocId = documentVersionDocValues.nextDoc();
					}

					if (lastModifiedDocId == docID) {
						long lastModified = lastModifiedDocValues.longValue();
						lastModifiedDocId = lastModifiedDocValues.nextDoc();

						long documentVersion = documentVersionDocId == docID ? documentVersionDocValues.longValue() : 0;

						@NonNull
						@SuppressWarnings("null")
						String pathname = pathnameDocValues.binaryValue().utf8ToString();

						// Check if pathname still exists
						// (if not, delete the associated documents)
						if (new File(pathname).exists()) {
							documents.put(pathname, new DocumentInfo(lastModified, documentVersion));
						} else {
							// Path no longer exists
							deleteDocuments.add(PathWithTerm.getTerm(pathname));
						}
					}
				}
			}
		}

		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		// Determine files need to index and start with the latest modified
		// (since it more likely to want to search more recently modified files)

		@SuppressWarnings("null")
		Stream<PathWithLastModified> stream = walkDirectories()
				.filter(p -> searcher == null ? true : shouldIndex(p, documents))
				// Sort by last modified descending
				// (since want to index recently modified files first
				.sorted(Comparator.comparing(PathWithLastModified::getLastModified).reversed());

		int indexedFiles = index(stream, castleIndexJob, monitor, false, deleteDocuments.toArray(new Term[0]));

		LocalDateTime endTime = LocalDateTime.now();

		Duration duration = Duration.between(startTime, endTime);

		return createIndexDoneMessage(indexedFiles, duration);
	}

	public static <T extends PathWithTerm> int index(final Stream<T> paths, final CASTLEIndexJob castleIndexJob,
			final IProgressMonitor monitor, final boolean removeWhenDone, final Term[] deleteDocuments)
			throws IOException {
		// TODO: see how can use submonitor to indicate progress (harder since
		// parallel)
		int initiallyReadCount = INITIALLY_READ_COUNT;

		if (INDEX_GROUP_COUNT < initiallyReadCount) {
			initiallyReadCount = INDEX_GROUP_COUNT;
		}

		// Introduced to make lambda happy
		int initiallyReadCountFinal = initiallyReadCount;

		AtomicInteger filesModifiedCount = new AtomicInteger();

		//		try (IndexWriter writer = createWriter()) {
		IndexWriter writer = getWriter();

		if (deleteDocuments.length > 0) {
			writer.deleteDocuments(deleteDocuments);
			writer.commit();

			if (SEARCHER_MANAGER != null) {
				SEARCHER_MANAGER.maybeRefreshBlocking();
			}
		}

		// Delete documents which don't have the fullpath field (older
		// versions of index)
		// https://lucene.472066.n3.nabble.com/Check-if-a-field-exists-of-not-in-a-query-td642816.html
		// Test query: *:* AND -fullpath:[* TO *]
		// XXX: comment out before go public, since no one else will need to
		// do this
		// (keep code for reference)
		try {
			Query query = new StandardQueryParser().parse("*:* AND -" + FULL_PATH_FIELD + ":[* TO *]", "content");
			long count = writer.deleteDocuments(query);

			if (count > 0) {
				writer.commit();
				// Must merge deletes, otherwise cannot change path field
				// from StringField to TextField
				writer.forceMergeDeletes();

				if (SEARCHER_MANAGER != null) {
					SEARCHER_MANAGER.maybeRefreshBlocking();
				}
			}
		} catch (QueryNodeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Iterate in order (so index newly modified files first)
		paths.forEachOrdered(path -> {
			if (path == null) {
				return;
			}

			// Index files

			// First delete any documents, in case file was partially
			// indexed and interrupted
			// (want to start fresh and reindex file)
			try {
				if (!writer.isOpen()) {
					// Handle case such as user deleting the index directory
					// in the middle of indexing
					monitor.setCanceled(true);
					throw new OperationCanceledException();
				}

				writer.deleteDocuments(path.getTerm());

				addDocument(writer, path);

				int count = filesModifiedCount.addAndGet(1);

				if (count % CANCEL_CHECK_COUNT == 0 && monitor.isCanceled()) {
					throw new OperationCanceledException();
				}

				if (count % INDEX_GROUP_COUNT == 0) {
					if (!writer.isOpen()) {
						// Handle case such as user deleting the index
						// directory in the middle of indexing
						monitor.setCanceled(true);
						throw new OperationCanceledException();
					}

					// Commit changes in groups, so can start querying even
					// as rest of files index
					commit(writer);
				}

				if (count == initiallyReadCountFinal) {
					if (count < INDEX_GROUP_COUNT) {
						if (!writer.isOpen()) {
							// Handle case such as user deleting the index
							// directory in the middle of indexing
							monitor.setCanceled(true);
							throw new OperationCanceledException();
						}

						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}

						// Commit changes so can then search quickly
						commit(writer);
					}

					castleIndexJob.setIndexCreated(true);
				}

				if (removeWhenDone) {
					castleIndexJob.removePath(path);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		//		}

		int modifiedCount = filesModifiedCount.get();

		if (modifiedCount > 0) {
			commit(writer);
		}

		return modifiedCount;
	}

	@NonNullByDefault
	private static Map<String, String> getFileRelatedFields(final Path path, final String filename,
			final String extension) {

		switch (extension) {
		case "java":
			return CASTLEJavaIndexer.getJavaFileRelatedFields(path, filename);
		default:
			@SuppressWarnings("null")
			@NonNull
			Map<String, String> emptyMap = Collections.emptyMap();

			return emptyMap;

		}
	}

	/**
	 * Index file with each line being a separate document
	 *
	 * @param indexWriter
	 * @param path
	 */
	@NonNullByDefault
	private static void addDocument(final IndexWriter indexWriter, final PathWithTerm pathWithTerm)
			throws IOException {
		File file = pathWithTerm.getFile();
		boolean isFile = file.isFile();

		if (!isFile) {
			// Don't need to index directories
			return;
		}

		// Indicate which project in
		//		Path relative = Activator.WORKSPACE_PATH.relativize(path);

		//		String project = relative.getNameCount() > 0 ? relative.getName(0).toString() : "";

		String project = pathWithTerm.getProject();
		Path path = pathWithTerm.getPath();
		String pathString = path.toString();

		@NonNull
		@SuppressWarnings("null")
		String filename = path.getFileName().toString();

		long fileLastModified = file.lastModified();

		String extension = getExtension(filename);

		Map<String, String> fileRelatedFields = getFileRelatedFields(path, filename, extension);

		CommentType commentType = CommentType.NONE;
		String previousLine = "";
		String previousLineType = "";

		// https://www.vogella.com/tutorials/EclipseJDT/article.html
		// https://stackoverflow.com/a/11168301
		RangeMap<Integer, String> javaElements = TreeRangeMap.create();

		IFile[] files = Activator.WORKSPACE_ROOT.findFilesForLocationURI(file.toURI());

		if (files.length > 0) {
			ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(files[0]);
			//			System.out.println("Parse: " + file);

			try {
				// Note: offset 0 is line 0
				// (so add 1 to line number to match what would show on the screen)
				DefaultLineTracker lineTracker = new DefaultLineTracker();
				lineTracker.set(compilationUnit.getSource());

				IType[] types = compilationUnit.getTypes();
				for (IType type : types) {
					IField[] fields = type.getFields();
					for (IField field : fields) {
						String elementName = field.getElementName();
						ISourceRange sourceRange = field.getSourceRange();
						JDTUtilities.addRange(sourceRange, elementName, javaElements, lineTracker);
					}

					IMethod[] methods = type.getMethods();
					for (IMethod method : methods) {
						@NonNull
						@SuppressWarnings("null")
						String elementName = method.getElementName();
						//						System.out.println("Method name " + method.getElementName());
						//						System.out.println("Signature " + method.getSignature());
						//						System.out.println("Return Type " + method.getReturnType());

						ISourceRange sourceRange = method.getSourceRange();
						JDTUtilities.addRange(sourceRange, elementName, javaElements, lineTracker);
					}
				}
			} catch (JavaModelException e) {
				// Do nothing
				// (if there's an error, just don't index line info
			}
		}

		// Read input line by line
		// TODO: select a better charset
		try (BufferedReader reader = Files.newBufferedReader(path, ISO_8859_1)) {
			String line;
			int lineNumber = 0;

			while ((line = reader.readLine()) != null) {
				lineNumber++;

				Document document = new Document();

				document.add(new StringField(FULL_PATH_FIELD, pathString, Field.Store.YES));

				// Index path with normal parser, so can search path
				// (don't store, since value is already stored as part of
				// fullpath)
				document.add(new TextField(PATH_FIELD, pathString, Field.Store.NO));
				document.add(new TextField("file", filename, Field.Store.YES));

				@Nullable
				String element = javaElements.get(lineNumber);

				if (element != null) {
					document.add(new TextField("element", element, Field.Store.YES));
				}

				// Store line as int instead of as String
				document.add(new IntPoint("line", lineNumber));

				document.add(new StoredField("line", lineNumber));
				// document.add(new StringField("line",
				// String.valueOf(lineNumber), Field.Store.YES));

				// Add fields based on file itself
				for (Map.Entry<String, String> entry : fileRelatedFields.entrySet()) {
					@SuppressWarnings("null")
					String key = entry.getKey();
					@SuppressWarnings("null")
					String value = entry.getValue();

					document.add(new TextField(key, value, Field.Store.NO));
				}

				// Add project
				if (!project.isEmpty()) {
					document.add(new TextField("proj", project, Field.Store.YES));
				}

				// Add date
				Matcher dateMatcher = DateUtilities.DATE_MATCHER.get().reset(line);

				// TODO: should this be a while loop, so can have multiple dates
				// on same line?
				if (dateMatcher.find()) {
					String date = dateMatcher.group();

					boolean isLocalDate = dateMatcher.matched("localDate");

					LocalDate localDate = DateUtilities.parseLocalDate(date, isLocalDate);

					if (localDate != null) {
						// TODO: verify this doesn't break anything
						document.add(new TextField("date",
								DateTools.dateToString(DateUtilities.asDate(localDate), DateTools.Resolution.DAY),
								Field.Store.YES));
					}
				}

				if (!extension.isEmpty()) {
					document.add(new StringField("ext", extension, Field.Store.YES));
				}

				// Handle data specific to file extension
				if (extension.equals("java")) {
					// Don't index entire line as content
					// (instead, will index content and comment separately (so
					// can filter out comments in results if desired)
					JavaIndexerReturn javaIndexerReturn = CASTLEJavaIndexer.indexJavaLine(
							new JavaIndexerRequest(line, document, commentType, previousLineType, previousLine));

					commentType = javaIndexerReturn.getCommentType();
					previousLineType = javaIndexerReturn.getPreviousLineType();
					previousLine = javaIndexerReturn.getPreviousLine();
				} else {
					// For other files, treat entire line as content
					// TODO: add support for recognizing comments in other files
					// (such as XML, HTML, SQL, JSP)
					document.add(new TextField("content", line, Field.Store.YES));
				}

				if (!indexWriter.isOpen()) {
					return;
				}

				indexWriter.addDocument(document);
			}
		}

		// Store information about the file itself
		// (stores last modified so can do incremental reindexing, when files
		// are added or modified)
		// (done last in case indexing was interupted in middle of file)
		// (in this case, the document would not show as indexed in full and
		// would be reindexed)
		// TODO: should also delete documents when corresponding file is deleted
		Document metaDocument = new Document();

		// Indicate this is a meta document, so it can be ignored when searching
		metaDocument.add(new StringField("metadocument", "meta", Field.Store.NO));

		metaDocument.add(new StringField(FULL_PATH_FIELD, pathString, Field.Store.YES));

		// Put pathname as a docvalue, so can quickly retrieve
		// (used to determine which files were modified when incremental
		// indexing)
		metaDocument.add(new BinaryDocValuesField(PATHNAME_FIELD, new BytesRef(pathString)));

		// Store data to help know when to reindex
		metaDocument.add(new NumericDocValuesField("fileLastModified", fileLastModified));

		// Track the version of the code that was used to write the document
		// (this way, can incrementally update files as the logic changes)
		// (it will essentially be a full rebuild of the index)
		// TODO: allow different document version for each indexer
		// (index based on files listed which have indexers)
		// (store attribution for version on the indexer instead of on the
		// parent indexers XML field)
		metaDocument.add(new NumericDocValuesField("documentVersion", CASTLESearchingSettings.DOCUMENT_VERSION));

		if (!indexWriter.isOpen()) {
			return;
		}

		indexWriter.addDocument(metaDocument);
	}

	@NonNullByDefault
	private static String getExtension(final String filename) {
		int lastPeriod = filename.lastIndexOf('.');

		@NonNull
		@SuppressWarnings("null")
		String extension = lastPeriod > 0 ? filename.substring(lastPeriod + 1) : "";
		return extension;
	}

	private static void commit(final IndexWriter writer) throws IOException {
		writer.commit();

		// Reset the searcher, so will create a new one
		// (since want to refresh with new documents)
		if (SEARCHER_MANAGER != null) {
			SEARCHER_MANAGER.maybeRefresh();
		}
	}

	public static IndexSearcher getSearcher() throws IOException {
		if (SEARCHER_MANAGER == null) {
			SEARCHER_MANAGER = CASTLESearchingSettings.getSearcherManager(CASTLEIndexer.INDEX_PATH);
		}

		if (SEARCHER_MANAGER != null) {
			return SEARCHER_MANAGER.acquire();
		} else {
			return null;
		}
	}

	private static String createIndexDoneMessage(final int indexedFiles, final Duration duration) {
		String indexMessage = indexedFiles == 1 ? "1 file" : indexedFiles + " files";

		return String.format("Indexed %s. It took %s.", indexMessage, formatDuration(duration));
	}

	private static String formatDuration(final Duration duration) {
		// Based on Duration.toString();
		// (word play - notice there's no 'C' in duration)
		long time = duration.getSeconds();

		long hours = time / SECONDS_PER_HOUR;
		int minutes = (int) ((time % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
		int secs = (int) (time % SECONDS_PER_MINUTE);

		StringBuilder durationStringBuilder = new StringBuilder();

		if (hours != 0) {
			if (hours == 1) {
				durationStringBuilder.append("1 hour");
			} else {
				durationStringBuilder.append(hours + " hours");
			}
		}

		if (minutes != 0) {
			if (durationStringBuilder.length() > 0) {
				durationStringBuilder.append(' ');
			}

			if (minutes == 1) {
				durationStringBuilder.append("1 minute");
			} else {
				durationStringBuilder.append(minutes + " minutes");
			}
		}

		if (secs != 0) {
			if (durationStringBuilder.length() > 0) {
				durationStringBuilder.append(' ');
			}

			if (secs == 1) {
				durationStringBuilder.append("1 second");
			} else {
				durationStringBuilder.append(secs + " seconds");
			}
		}

		if (durationStringBuilder.length() == 0) {
			durationStringBuilder.append("0 seconds");
		}

		return durationStringBuilder.toString();
	}
}
