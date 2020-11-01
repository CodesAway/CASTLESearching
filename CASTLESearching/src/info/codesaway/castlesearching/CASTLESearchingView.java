package info.codesaway.castlesearching;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.Operator;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import info.codesaway.castlesearching.handlers.PreviousSearchesHandler;
import info.codesaway.castlesearching.indexer.CASTLEIndexer;
import info.codesaway.castlesearching.indexer.java.CASTLEJavaIndexer;
import info.codesaway.castlesearching.jobs.CASTLEIndexJob;
import info.codesaway.castlesearching.jobs.CASTLESearchJob;
import info.codesaway.castlesearching.searcher.CASTLESearcher;
import info.codesaway.castlesearching.util.RegexUtilities;
import info.codesaway.util.SystemClipboard;
import info.codesaway.util.indexer.PathWithTerm;
import info.codesaway.util.regex.Matcher;

// Source: https://www.vogella.com/tutorials/EclipseJFaceTable/article.html
public class CASTLESearchingView extends ViewPart {
	// public class CASTLESearchingView {
	public static final String ID = CASTLESearchingView.class.getName();

	// TODO: See how to change the font size
	// https://stackoverflow.com/a/34953145/12610042

	// Make CASTLE Searching stand-alone java application
	// https://www.vogella.com/tutorials/SWT/article.html#optional-exercise-use-swt-in-a-standalone-java-application

	// TODO: figure out how to make key bindings in e4 plugin
	// https://www.vogella.com/tutorials/EclipseRCP/article.html#key-bindings
	// For now, use e3 settings which creates this for me

	// // https://www.vogella.com/tutorials/Eclipse4Services/article.html
	// @Inject
	// private EPartService partService;

	public static CASTLESearchingView INSTANCE;

	// private static StyleRange[] EMPTY_STYLE_RANGE = {};

	private static CASTLEIndexJob indexJob;
	private static CASTLESearchJob searchJob;

	// private Button incremental;

	/**
	 * Quickly change index settings (workspace, incremental, user defined)
	 */
	private Combo comboDropDown;

	private StyledText searchText;
	// Used to prevent duplicate searchesu
	// (such as if do incremental search, don't want regular search after)
	// private String lastSearch = "";
	private long lastSearchDoneTime;

	private Label statusLabel;
	private Label messageLabel;

	private TableViewer viewer;

	private long lastIndexTime = 0;
	// Initialize with true, so if index doesn't exist,
	// the first time we query, it will create the index
	// (since isIndexCreated is true and if the index didn't exist when call
	// setIndexCreated would be false)
	// (as a result, setIndexCreated will run the indexer)
	// (whereas if isIndexCreated were initialized to false, calling
	// setIndexCreated with false would do nothing, since the value didn't
	// change)
	private boolean isIndexCreated = true;

	private static int DEFAULT_PREVIOUS_SEARCHES_COUNT = 10;
	private final int previousSeachesLimit = DEFAULT_PREVIOUS_SEARCHES_COUNT;
	private final ArrayDeque<CASTLESearchResult> previousSearches = new ArrayDeque<>(DEFAULT_PREVIOUS_SEARCHES_COUNT);
	private static IHandlerActivation previousSearcHandlerActivation;

	public static final String INDEXING_STATUS = "Indexing...";
	public static final String ERROR_STATUS = "ERROR";

	// Delay to wait until search after stop tying
	// (allows user to type query and then when done typing, will search)
	private static int SEARCH_DELAY = 1000;

	public static int DEFAULT_SMALL_HIT_LIMIT = 10;
	private static int DEFAULT_INCREMENTAL_HIT_LIMIT = 100;
	private static int DEFAULT_LARGE_HIT_LIMIT = 1000;
	// Useful when want to get all results, such as to copy into report
	private static int DEFAULT_MEGA_HIT_LIMIT = 100_000;

	// If user hasn't typed anything into CASTLE Searching for a while, perform
	// an
	// incremental indexing when they start typing again
	// (this way, any changes can be indexed, in case modified files outside of
	// Eclipse)
	private static int INDEX_DELAY = 5 * 60 * 1000;

	private static final ThreadLocal<Matcher> SEARCH_TEXT_FORMATTER_MATCHERS = RegexUtilities
			.getThreadLocalMatcher("\\w++(?=:)");

	private static IResourceChangeListener RESOURCE_CHANGE_LISTENER = event -> {
		// https://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
		// if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
		// return;
		// }

		ArrayDeque<IResourceDelta> deltas = new ArrayDeque<>();
		ArrayDeque<PathWithTerm> paths = new ArrayDeque<>();
		ArrayDeque<Term> deletes = new ArrayDeque<>();
		deltas.add(event.getDelta());

		while (!deltas.isEmpty()) {
			IResourceDelta delta = deltas.remove();

			// System.out.println(
			// "Checking Delta: " + delta.getKind() + ": " +
			// delta.getFlags() + ": " +
			// delta.getFullPath());

			IResourceDelta[] children = delta.getAffectedChildren();

			// Is a file
			if (children.length == 0) {
				// The content was modified or it's a new file
				if ((delta.getFlags() & IResourceDelta.CONTENT) != 0
						|| (delta.getKind() & IResourceDelta.ADDED) != 0) {
					IResource resource1 = delta.getResource();
					// IJavaElement element = JavaCore.create(resource);
					// if (element instanceof ICompilationUnit) {
					// ICompilationUnit unit = (ICompilationUnit) element;
					// unit.getTypes();
					// }
					// JavaCore.createCompilationUnitFrom(null)

					Path path = resource1.getRawLocation().toFile().toPath();

					if (CASTLESearchingSettings.shouldIndexFile(path)) {
						IProject iProject = resource1.getProject();
						String project = iProject != null ? iProject.getName() : "";
						// System.out.println("Changed! " + path);
						paths.add(PathWithTerm.wrap(project, path));
					}
				} else if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
					// || (delta.getKind() & IResourceDelta.REMOVED_PHANTOM)
					// != 0) {
					// Handle removed files
					// (in this case, just need to delete the documents from
					// the index

					IResource resource2 = delta.getResource();
					@NonNull
					@SuppressWarnings("null")
					String pathname = resource2.getRawLocation().toFile().toPath().toString();

					deletes.add(PathWithTerm.getTerm(pathname));
				}
			} else {
				for (IResourceDelta child : children) {
					deltas.add(child);
				}
			}
		}

		// For the added paths, add them to the collection of files to index
		if (!paths.isEmpty() || !deletes.isEmpty()) {
			indexJob.cancel();
			indexJob.schedule(false, paths, deletes);
		}
	};

	// private static void refreshStylers() {
	// ITheme theme = THEME_MANAGER.getCurrentTheme();

	// TODO: doesn't get correct colors when on dark theme
	// ColorRegistry colorRegistry = theme.getColorRegistry();

	// TODO: used when testing to get various values

	// Color javaKeywordColor =
	// colorRegistry.get("org.eclipse.jdt.ui.java_keyword");

	// org.eclipse.jdt.ui.java_keyword
	// org.eclipse.jdt.ui.java_doc_default
	// org.eclipse.jdt.ui.java_multi_line_comment
	// org.eclipse.jdt.ui.methodHighlighting
	// }

	@Override
	@PostConstruct
	public void createPartControl(final Composite parent) {
		// TODO: see how can use information

		// org.eclipse.jdt.ui.java_string

		GridLayout layout = new GridLayout(2, false);
		// GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);

		// Hack to allow mnemonic of ALT+` (same key as ~)
		// (however, don't want to actually show text)
		// https://stackoverflow.com/a/28829551/12610042
		// this.incremental = new Button(parent, SWT.CHECK) {
		// @Override
		// public String getText() {
		// return "&`";
		// }
		//
		// @Override
		// protected void checkSubclass() {
		// // Do Nothing to avoid Subclassing Not Allowed error.
		// }
		// };

		// this.incremental.setToolTipText("Incremental Find");
		// this.incremental = new Button(parent, SWT.CHECK);
		// this.incremental.addSelectionListener(SelectionListener.widgetSelectedAdapter(e
		// -> {
		// // Change focus to search text,
		// // so can start typing something after change the search
		// this.setFocus();
		//
		// this.search();
		// }));

		// Instead of having constant label,
		// use dropdownn to allow quick changes between search types
		// (populated as part of initialization)
		this.comboDropDown = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

		// Reinitialize settings when create view
		// (so can close view and open again to refresh settings)
		// TODO: add command to refresh settings

		// refreshStylers();

		// TODO: save and restore current setting
		// If prior setting isn't valid (such as removed, default to WORKSPACE
		this.comboDropDown.setBackground(JFaceColors.getBannerBackground(Display.getCurrent()));
		this.comboDropDown.addModifyListener(event -> {
			// Change focus to search text,
			// so can start typing something after change the search
			this.setFocus();

			this.search();
		});

		this.createSearchText(parent);

		// Filler element, so allign status label with combo box
		// new Label(parent, SWT.NONE);

		this.statusLabel = new Label(parent, SWT.NONE);
		// Initialize the size to the same as searchLabel
		this.statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		// this.statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE,
		// false, false, 2, 1));
		// this.statusLabel.setLayoutData(new
		// GridData(GridData.HORIZONTAL_ALIGN_FILL));

		this.messageLabel = new Label(parent, SWT.NONE);
		this.messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		// this.messageLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
		// | GridData.HORIZONTAL_ALIGN_FILL));

		this.createViewer(parent);

		indexJob = new CASTLEIndexJob(this);
		searchJob = new CASTLESearchJob(this);

		CASTLESearchingSettings.init(this);

		// Add change listener here instead of Activator
		// (since don't need to watch for changes unless the CASTLE Searching
		// view exists)
		// (since until the view exists, I don't create and run the indexer)
		Activator.WORKSPACE.addResourceChangeListener(RESOURCE_CHANGE_LISTENER, IResourceChangeEvent.POST_CHANGE);

		// TODO: use information about the workspace when indexing
		// https://www.vogella.com/tutorials/EclipseJDT/article.html
		// DisplayProjectInformation.display(Activator.WORKSPACE_ROOT);

		INSTANCE = this;

		Activator.refreshStyles();

		this.clearPreviousSearches();

		// Index the current files when the view is created
		this.index();
	}

	private int getHitLimit(final KeyEvent e) {
		// If pressing SHIFT, get mega group (such as to export into report)
		if ((e.stateMask & SWT.SHIFT) != 0) {
			return DEFAULT_MEGA_HIT_LIMIT;
			// If pressing CTRL, use larger group
		} else if ((e.stateMask & SWT.CTRL) != 0) {
			return DEFAULT_LARGE_HIT_LIMIT;
		} else {
			return DEFAULT_SMALL_HIT_LIMIT;
		}
	}

	protected void handleKeyPressedInSearchText(final KeyEvent e) {
		// Start indexing if start typing and haven't indexed for a while
		if (System.currentTimeMillis() - this.lastIndexTime >= INDEX_DELAY) {
			// Perform an incremental index update
			this.index();
			CASTLESearchingSettings.maybeRefreshSearcherManagers();
		}

		switch (e.keyCode) {
		// enter pressed
		case SWT.CR:
		case SWT.KEYPAD_CR:
		case SWT.F5:
			int hitLimit = this.getHitLimit(e);

			if (this.isIncrementalSearch() && DEFAULT_INCREMENTAL_HIT_LIMIT > hitLimit) {
				hitLimit = DEFAULT_INCREMENTAL_HIT_LIMIT;
			}

			this.search(0, false, hitLimit);
			return;
		}

		// Control hotkeys
		if ((e.stateMask & SWT.CTRL) != 0) {
			// CTRL + J - incremental search
			// (boost matches found in currently active file)
			if (e.keyCode == 'j') {
				boolean incrementalSearch = true;

				this.search(0, false, DEFAULT_INCREMENTAL_HIT_LIMIT, this.getExtraQuery(incrementalSearch));
			}
			// Don't have CTRL hotkey, since have ALT hotkey and CTRL one
			// doesn't make sense (confusing having two hotkeys for same action)
			// else if (e.keyCode == '`') {
			// // CTRL + ` (same key as ~) to switch between regular and
			// incremental find
			// // Toggle selection
			// this.incremental.setSelection(!this.incremental.getSelection());
			// this.search();
			// }
		}
		// ALT Hotkeys
		else if ((e.stateMask & SWT.ALT) != 0) {
			if (e.keyCode >= '0' && e.keyCode <= '9') {
				// Convert from char to int
				// (index is 1 based)
				int index1Based = e.keyCode - '0';

				// Treat ALT + 0 as the 10th index
				if (index1Based == 0) {
					index1Based = 10;
				}

				Optional<String> optional = CASTLESearchingSettings.getSearcherName(index1Based);

				if (optional.isPresent()) {
					this.comboDropDown.setText(optional.get());
					this.search();
				}
			} else if (e.keyCode == '`') {
				// ALT+` (same key as ~)
				// to switch between regular and incremental find
				// Toggle selection
				// this.incremental.setSelection(!this.incremental.getSelection());

				// https://www.vogella.com/tutorials/EclipseCommandsAdvanced/article.html#calling-commands-directly-via-code
				IHandlerService handlerService = this.getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand("info.codesaway.castlesearching.commands.incrementalfind", null);
				} catch (Exception ex) {
					// throw new RuntimeException("command not found");
					// Give message
				}
				// TODO: verify this works
				// (Chrome OS reserves, so cannot test on Chromebook)
			} else if (e.keyCode == '=') {
				// ALT+=
				// (same key as +,
				// toggle Operator AND
				// which essentially makes each term required
				// , same as adding '+' in front for lucene)

				// https://www.vogella.com/tutorials/EclipseCommandsAdvanced/article.html#calling-commands-directly-via-code
				IHandlerService handlerService = this.getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand("info.codesaway.castlesearching.commands.operatorand", null);
				} catch (Exception ex) {
					// throw new RuntimeException("command not found");
					// Give message
				}
			} else if (e.keyCode == '/') {
				// https://www.vogella.com/tutorials/EclipseCommandsAdvanced/article.html#calling-commands-directly-via-code
				IHandlerService handlerService = this.getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand("info.codesaway.castlesearching.commands.includecomments", null);
				} catch (Exception ex) {
					// throw new RuntimeException("command not found");
					// Give message
				}
			}
		}

	}

	protected void handleKeyReleasedInSearchText(final KeyEvent e) {
		// Add delay so don't search if still typing
		this.search(SEARCH_DELAY);

		if (e.keyCode == SWT.ARROW_DOWN) {
			// @SuppressWarnings("unchecked")
			// List<CASTLESearchResultEntry> entries =
			// (List<CASTLESearchResultEntry>) this.viewer.getInput();
			//
			// if (entries.isEmpty()) {
			// return;
			// }

			// Will return null if the list is empty
			// (don't need actual list though, since just selecting first
			// element)
			Object element = this.viewer.getElementAt(0);

			if (element != null) {
				// https://stackoverflow.com/a/12799175
				this.viewer.setSelection(new StructuredSelection(element), true);
				this.viewer.getControl().setFocus();
			}
		}
	}

	protected void handleKeyPressedInViewer(final KeyEvent e) {
		if ((e.stateMask & SWT.ALT) != 0) {
			if (e.keyCode >= '0' && e.keyCode <= '9') {
				// Convert from char to int
				// (index is 1 based)
				int index1Based = e.keyCode - '0';

				// Treat ALT + 0 as the 10th index
				if (index1Based == 0) {
					index1Based = 10;
				}

				Optional<String> optional = CASTLESearchingSettings.getSearcherName(index1Based);

				if (optional.isPresent()) {
					this.comboDropDown.setText(optional.get());
					// Always search
					// (even if selecting same option as currently selected,
					// which means that the modify listener won't fire)
					this.search();
					// Set focus to search text, even if searching for the same
					// type
					// (allows consistent behavior)
					this.setFocus();
					// Don't select top result, since not consistent and not
					// helpful
					// (for example if doesn't have results, would be better to
					// be able to keep typing)
					// this.selectAndRevealTopResult();
				}
			}
		}
		// else if ((e.stateMask & SWT.CTRL) != 0) {
		// Don't have CTRL hotkey, since have ALT hotkey and CTRL one doesn't
		// make sense (confusing having two hotkeys for same action)
		// if (e.keyCode == '`') {
		// // CTRL + ` (same key as ~) to switch between regular and incremental
		// find
		// // Toggle selection
		// this.incremental.setSelection(!this.incremental.getSelection());
		// this.search();
		// // Don't select top result, since not consistent and not helpful
		// // (for example if doesn't have results, would be better to be able
		// to keep typing)
		// // this.selectAndRevealTopResult();
		// }
		// }
	}

	protected void handleKeyReleasedInViewer(final KeyEvent e) {
		// Press ALT + F1 to search if currently focused on viewer
		// ALT Hotkeys
		if ((e.stateMask & SWT.ALT) != 0) {
			if (e.keyCode == SWT.F1) {
				// Use force focus so can have same shortcut in Eclipse and in
				// plugin itself
				// (easy key combo to always pull put the search)
				this.searchText.forceFocus();
				this.searchText.selectAll();
			}
		} else if (e.keyCode == SWT.F5) {
			int hitLimit = this.getHitLimit(e);

			this.search(0, true, hitLimit);
		} else if (e.keyCode == SWT.DEL) {
			// Delete selection
			IStructuredSelection selection = (IStructuredSelection) this.viewer.getSelection();

			if (selection.isEmpty()) {
				return;
			}

			this.viewer.remove(selection.toArray());
		} else if (e.keyCode == 'c' && (e.stateMask & SWT.CTRL) != 0) {
			// CTRL + C, copy the text

			IStructuredSelection selection = (IStructuredSelection) this.viewer.getSelection();

			if (selection.isEmpty()) {
				return;
			}

			@SuppressWarnings("unchecked")
			List<CASTLESearchResultEntry> list = selection.toList();

			StringJoiner text = new StringJoiner(System.lineSeparator());

			// TODO: support copying text in the column order displayed on
			// screen

			// If SHIFT key is pressed (CTRL + SHIFT + C), copy headers
			if ((e.stateMask & SWT.SHIFT) != 0) {
				text.add(this.getRowHeaderExtract());
			}

			for (CASTLESearchResultEntry entry : list) {
				text.add(this.getRowDataExtract(entry));
			}

			SystemClipboard.copy(text.toString());
		}
	}

	private boolean isIncrementalSearch() {
		return this.getToggleState("info.codesaway.castlesearching.commands.incrementalfind");
		// return this.incremental.getSelection();
		// return
		// this.comboDropDown.getText().equals(CASTLESearchingSettings.SEARCHER_INCREMENTAL.getName());
	}

	@NonNullByDefault
	@SuppressWarnings({ "null" })
	public Optional<String> getActivePathname() {
		// https://stackoverflow.com/a/17901551/12610042
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();

		if (window == null) {
			return Optional.empty();
		}

		IWorkbenchPage page = window.getActivePage();

		if (page == null) {
			return Optional.empty();
		}

		IWorkbenchPart workbenchPart = page.getActivePart();

		if (workbenchPart == null) {
			return Optional.empty();
		}

		IEditorPart editorPart = workbenchPart.getSite().getPage().getActiveEditor();

		if (editorPart == null) {
			return Optional.empty();
		}

		IEditorInput editorInput = editorPart.getEditorInput();

		if (editorInput == null) {
			return Optional.empty();
		}

		@Nullable
		IFile iFile = editorInput.getAdapter(IFile.class);

		// Not dead code, getAdapter says NonNull,
		// but Javadoc says can return null
		if (iFile == null) {
			return Optional.empty();
		}

		// System.out.println("Location: " + iFile.getLocation().toFile());

		// Get absolute path
		IPath iPath = iFile.getLocation();

		if (iPath == null) {
			return Optional.empty();
		}

		return Optional.of(iPath.toFile().toString());

		// return Optional.of(iFile.toString());
	}

	@NonNullByDefault
	@SuppressWarnings("null")
	public Optional<Query> getExtraQuery(final boolean isIncrementalSearch) {
		if (isIncrementalSearch) {
			Optional<String> activePathname = this.getActivePathname();

			if (activePathname.isPresent()) {
				String value = activePathname.get();

				// Boost matches found in currently active file
				BoostQuery query = new BoostQuery(new TermQuery(PathWithTerm.getTerm(value)), 5);
				return Optional.of(query);
			}
		}

		return Optional.empty();
	}

	@NonNullByDefault
	private Operator getDefaultOperator() {
		boolean currentState = this.getToggleState("info.codesaway.castlesearching.commands.operatorand");

		return currentState ? Operator.AND : Operator.OR;
	}

	@NonNullByDefault
	private boolean shouldIncludeComments() {
		return this.getToggleState("info.codesaway.castlesearching.commands.includecomments");
	}

	private boolean getToggleState(final String commandId) {
		// https://stackoverflow.com/a/23742598/12610042
		// https://web.archive.org/web/20180311233946/http://www.robertwloch.net:80/2011/01/eclipse-tips-tricks-label-updating-command-handler/

		// https://www.eclipse.org/forums/index.php/t/156292/
		// http://blog.eclipse-tips.com/2009/03/commands-part-6-toggle-radio-menu.html
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ICommandService commandService = window.getService(ICommandService.class);
		if (commandService != null) {
			Command command = commandService.getCommand(commandId);
			// commandService.refreshElements(command.getId(), null);
			State state = command.getState("org.eclipse.ui.commands.toggleState");

			// Added to prevent NullPointerException in some cases
			if (state == null) {
				return false;
			}

			boolean currentState = (Boolean) state.getValue();

			return currentState;
		}

		// How to add command
		// https://stackoverflow.com/a/34450815/12610042

		return false;
	}

	public void search() {
		this.search(0);
	}

	public void search(final String text, final long delay) {
		if (System.currentTimeMillis() - this.lastIndexTime >= INDEX_DELAY) {
			// Perform an incremental index update
			this.index();
			CASTLESearchingSettings.maybeRefreshSearcherManagers();
		}

		// Sets the search text then performs a search
		// (used to call from CASTLESearchPage)
		this.searchText.setText(text);
		this.search(delay);
	}

	public void search(final long delay) {
		// TODO: read hitLimit from Searcher
		// Search but don't select first result (just reveal it)
		this.search(delay, false, this.isIncrementalSearch() ? DEFAULT_INCREMENTAL_HIT_LIMIT : DEFAULT_SMALL_HIT_LIMIT);
	}

	/**
	 *
	 *
	 * @param delay
	 *            the delay to which before starting the search (enter 0 to
	 *            search immediately)
	 */
	public void search(final long delay, final boolean shouldSelectFirstResult, final int hitLimit) {
		this.search(delay, shouldSelectFirstResult, hitLimit, this.getExtraQuery(this.isIncrementalSearch()));
	}

	/**
	 *
	 *
	 * @param delay
	 *            the delay to which before starting the search (enter 0 to
	 *            search immediately)
	 */
	@NonNullByDefault
	public void search(final long delay, final boolean shouldSelectFirstResult, final int hitLimit,
			final Optional<Query> extraQuery) {
		CASTLESearcher searcher = CASTLESearchingSettings.getSearcher(this.comboDropDown.getText());

		if (searcher == null) {
			return;
		}

		Path indexPath = searcher.getIndexPath();

		if (indexPath == null) {
			return;
		}

		String text = this.getText();
		Operator defaultOperator = this.getDefaultOperator();
		boolean shouldIncludeComments = this.shouldIncludeComments();

		CASTLESearch search = new CASTLESearch(text, delay, shouldSelectFirstResult, hitLimit, extraQuery, searcher,
				defaultOperator, shouldIncludeComments);

		// Determines whether to run the current search or the new search
		searchJob.handleSearch(search);
	}

	public static boolean isIndexing() {
		return indexJob.getState() == Job.RUNNING;
	}

	public static void cancelIndexing() {
		indexJob.cancel();
	}

	public void index() {
		this.lastIndexTime = System.currentTimeMillis();
		// Cancel existing index job
		// (this way, will pick up recently modified files if want to refresh
		// index)
		// TODO: also add progress indicator, since now going in order
		indexJob.cancel();
		indexJob.schedule(true, Collections.emptyList(), Collections.emptyList());
	}

	public void setIndexCreated(final boolean isIndexCreated) {
		if (isIndexCreated == this.isIndexCreated) {
			// Don't need to do anything
			return;
		}

		// Index state changed
		// 1) Currently, index doesn't exist
		// a) Need to index
		// b) When index exists, refresh search TODO:
		// 2) Index now exists and previously didn't
		// TODO: do I need to do anything in this case?

		this.isIndexCreated = isIndexCreated;

		if (!isIndexCreated && indexJob.getState() != Job.RUNNING) {
			this.index();
		} else if (isIndexCreated) {
			// Index now exists and previously didn't
			// Perform search on current query
			Display display = this.getViewer().getControl().getDisplay();

			display.syncExec(() -> {
				// In UI thread
				this.search();
			});
		}
	}

	// public String getLastSearch() {
	// return this.lastSearch;
	// }

	// public void setLastSearch(final String lastSearch) {
	// this.lastSearch = lastSearch;
	// }

	public long getLastSearchDoneTime() {
		return this.lastSearchDoneTime;
	}

	public void setLastSearchDoneTime(final long lastSearchDoneTime) {
		this.lastSearchDoneTime = lastSearchDoneTime;
	}

	private void createSearchText(final Composite parent) {
		this.searchText = new StyledText(parent, SWT.BORDER | SWT.SEARCH | SWT.SINGLE);
		this.searchText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		// this.searchText.setLayoutData(new
		// GridData(GridData.FILL_HORIZONTAL));
		// this.searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
		// GridData.HORIZONTAL_ALIGN_FILL));
		this.searchText.addModifyListener(event -> {
			// Format text
			this.styleSearchText();
		});

		this.searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				CASTLESearchingView.this.handleKeyPressedInSearchText(e);
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				CASTLESearchingView.this.handleKeyReleasedInSearchText(e);
			}
		});
	}

	private void createViewer(final Composite parent) {
		// Source:
		// https://www.vogella.com/tutorials/EclipseJFaceTable/article.html
		this.viewer = new TableViewer(parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		this.createColumns(parent, this.viewer);

		final Table table = this.viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		this.viewer.setContentProvider(new ArrayContentProvider());
		this.viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		// this.viewer.getControl().setLayoutData(new GridData(SWT.FILL,
		// SWT.FILL, true, true, 3, 1));
		// TODO: how to use the text font in the view too?
		// (so changes to it will change the view as well)
		// https://www.vogella.com/tutorials/EclipseEditors/article.html#adding-colors-and-fonts-preferences
		// this.viewer.getControl().setFont(JFaceResources.getTextFont());

		// TODO: make the selection available to other views
		// this.getSite().setSelectionProvider(this.viewer);

		// Layout the viewer
		// GridData gridData = new GridData();
		// gridData.verticalAlignment = GridData.FILL;
		// gridData.horizontalSpan = 3;
		// gridData.grabExcessHorizontalSpace = true;
		// gridData.grabExcessVerticalSpace = true;
		// gridData.horizontalAlignment = GridData.FILL;
		// this.viewer.getControl().setLayoutData(gridData);

		// Double click to open file
		// https://stackoverflow.com/a/6342124
		this.viewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) this.viewer.getSelection();
			if (selection.isEmpty()) {
				return;
			}

			@SuppressWarnings("unchecked")
			List<CASTLESearchResultEntry> list = selection.toList();

			LinkedHashMap<String, CASTLESearchResultEntry> openResults = list.stream()
					.collect(Collectors.toMap(
							// Group by path, since can only open each file once
							// (in this case, want to get the line number
							// corresponding to the more
							// important, earlier entry)
							CASTLESearchResultEntry::getPath,

							// For the value, use the CASTLESearchResultEntry
							// object itself
							Function.identity(),

							// Merge function, always use the old value if
							// multiple for same path
							// (this way, get the one with higher importance)
							(old, e) -> old,

							// Create as LinkedHashMap, so keep insertion order
							// (since will then iterate in reverse order when
							// opening the results)
							LinkedHashMap::new));

			List<CASTLESearchResultEntry> entries = new ArrayList<>(openResults.values());

			// TODO: make preference
			int maxOpenFiles = 10;

			if (entries.size() > maxOpenFiles) {
				entries = entries.subList(0, maxOpenFiles);
			}

			// Iterate in reverse order, so most import page has focus
			// (last page opened is the most important)
			for (int i = entries.size() - 1; i >= 0; i--) {
				CASTLESearchResultEntry entry = entries.get(i);

				this.openResult(entry);
			}
		});

		this.viewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				CASTLESearchingView.this.handleKeyPressedInViewer(e);
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				CASTLESearchingView.this.handleKeyReleasedInViewer(e);
			}
		});
	}

	/**
	 * <p>
	 * Note: must be accessed from the UI thread
	 * </p>
	 *
	 * @return
	 */
	public String getSearcherName() {
		return this.comboDropDown.getText();
	}

	/**
	 * <p>
	 * Note: must be accessed from the UI thread
	 * </p>
	 */
	public void setSearcherName(final String searcherName) {
		this.comboDropDown.setText(searcherName);
	}

	/**
	 * <p>
	 * Note: must be accessed from the UI thread
	 * </p>
	 */
	public void setSearcherNames(final String[] searcherNames) {
		this.comboDropDown.setItems(searcherNames);
	}

	@NonNullByDefault
	public String getText() {
		// Trim since leading / trailing whitespace doesn't matter
		@NonNull
		@SuppressWarnings("null")
		String text = this.searchText.getText().trim();
		return text;
	}

	private TableViewer getViewer() {
		return this.viewer;
	}

	// private Label getStatusLabel() {
	// return this.statusLabel;
	// }
	//
	// private Label getMessageLabel() {
	// return this.messageLabel;
	// }

	// https://www.baeldung.com/java-initialize-hashmap

	// This will create the columns for the table
	private void createColumns(final Composite parent, final TableViewer viewer) {
		// https://www.programcreek.com/java-api-examples/?code=gw4e/gw4e.project/gw4e.project-master/bundles/gw4e-eclipse-plugin/src/org/gw4e/eclipse/wizard/convert/page/TableHelper.java
		// Make last column width based on column width left

		// TODO: calculate width based on available space

		this.createTableViewerColumn("#", 50, CASTLESearchResultEntry::getResultNumber);
		this.createTableViewerColumn("File", 250, CASTLESearchResultEntry::getFile);
		this.createTableViewerColumn("Element", 250, CASTLESearchResultEntry::getElement);
		this.createTableViewerColumn("Line", 65, CASTLESearchResultEntry::getLine);
		// Put type before content, since can use type for quick understanding
		// of line
		this.createTableViewerColumn("Type", 250, CASTLESearchResultEntry::getType);
		this.createStyledTableViewerColumn("Content", 750, CASTLESearchingView::getStyledContent);
		// this.createTableViewerColumn("Content", 500,
		// CASTLESearchResultEntry::getContent);
		this.createTableViewerColumn("Path", 1000, CASTLESearchResultEntry::getPath);
	}

	private String getRowHeaderExtract() {
		StringJoiner result = new StringJoiner("\t");

		result.add("Result");
		result.add("File");
		result.add("Line");
		result.add("Type");
		result.add("Content");
		result.add("Path");

		return result.toString();
	}

	private String getRowDataExtract(final CASTLESearchResultEntry entry) {
		StringJoiner result = new StringJoiner("\t");

		result.add(entry.getResultNumber());
		result.add(entry.getFile());
		result.add(entry.getLine());
		result.add(entry.getType());
		result.add(entry.getContent().replace("\t", "    "));
		result.add(entry.getPath());

		return result.toString();
	}

	private TableViewerColumn createTableViewerColumn(final String title, final int width) {
		TableViewerColumn viewerColumn = new TableViewerColumn(this.viewer, SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(width);
		column.setResizable(true);
		column.setMoveable(true);

		return viewerColumn;
	}

	private TableViewerColumn createTableViewerColumn(final String title, final int width,
			final Function<CASTLESearchResultEntry, String> valueFunction) {
		TableViewerColumn viewerColumn = this.createTableViewerColumn(title, width);

		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return valueFunction.apply((CASTLESearchResultEntry) element);
			}
		});

		return viewerColumn;
	}

	private TableViewerColumn createStyledTableViewerColumn(final String title, final int width,
			final Function<CASTLESearchResultEntry, StyledString> valueFunction) {
		TableViewerColumn viewerColumn = this.createTableViewerColumn(title, width);

		IStyledLabelProvider labelProvider = new WorkbenchLabelProvider();

		viewerColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(labelProvider) {
			@Override
			protected StyledString getStyledText(final Object element) {
				return valueFunction.apply((CASTLESearchResultEntry) element);
			}
		});

		return viewerColumn;
	}

	@Override
	// Set to ensure focus is set correctly
	// (tried without and didn't focus correctly for some reason)
	// @Focus
	public void setFocus() {
		this.searchText.setFocus();
		// TODO: make setting whether to select all when set focus
		// TODO: when would this be annoying?
		this.searchText.selectAll();
	}

	public void openResult(final CASTLESearchResultEntry entry) {
		// TODO: handle opening workspace file if project is closed
		// Example search:
		// var:test file:compare
		// TODO: able to open file to line, but may still want to prompt user if
		// they
		// want to open the project

		// https://stackoverflow.com/a/51552923
		// https://wiki.eclipse.org/FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace%3F

		String pathname = entry.getPath();
		int lineNumber = entry.getLineNumber();

		// TODO: how to open file in editor?
		// IPath path = new org.eclipse.core.runtime.Path(pathname);

		File file = new File(pathname);
		URI uri = file.toURI();

		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
		// // IFile file =
		// ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		//
		// System.out.printf("%d files for %s%n", files.length, pathname);

		if (files.length > 0) {
			IFile projectFile = files[0];

			if (!projectFile.exists()) {
				// File doesn't exist, so cannot open file
				return;
			}

			// https://stackoverflow.com/a/45352838/12610042
			// IWorkbenchPage page =
			// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			//
			// try {
			// IEditorPart editorPart = IDE.openEditor(page, projectFile);
			//
			// if (editorPart instanceof ITextEditor) {
			// ITextEditor editor = (ITextEditor) editorPart;
			//
			// IDocumentProvider provider = editor.getDocumentProvider();
			// IDocument document =
			// provider.getDocument(editor.getEditorInput());
			//
			// System.out.println("Line: " + lineNumber);
			//
			// if (lineNumber != 0) {
			// try {
			// int lineStart = document.getLineOffset(lineNumber);
			//
			// System.out.println("Line start: " + lineStart);
			// editor.selectAndReveal(lineStart, 0);
			// } catch (BadLocationException x) {
			// // ignore
			// x.printStackTrace();
			// }
			// }
			//
			// page.activate(editor);
			// }
			// } catch (PartInitException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			// Original logic
			if (projectFile.getProject().isOpen()) {
				// This logic only works if the project is open
				// TODO: may want to ask user if they want to open the project
				IMarker marker;
				try {
					marker = projectFile.createMarker(IMarker.TEXT);
					if (lineNumber != 0) {
						marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
					}

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IDE.openEditor(page, marker);
					marker.delete();

					return;
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Open as external file
		// TODO: also done if project is closed (may want to prompt to open
		// project)
		// https://stackoverflow.com/a/25385435
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		try {
			IFileStore fileStore = EFS.getStore(uri);
			IEditorPart openEditor = IDE.openEditorOnFileStore(page, fileStore);

			// http://eclipsesnippets.blogspot.com/2008/06/programmatically-opening-editor.html
			if (openEditor instanceof ITextEditor) {
				ITextEditor textEditor = (ITextEditor) openEditor;
				IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

				// TODO: why do I need to subtract 1?
				// (it works and article said to do it, but not sure why it
				// works)
				int line = lineNumber - 1;

				int lineOffset = document.getLineOffset(line);
				// -1 to put cursor at end of selected line
				int columnOffset = document.getLineLength(line) - 1;

				textEditor.selectAndReveal(lineOffset, columnOffset);
			}
		} catch (CoreException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void cancelJobs() {
		if (INSTANCE != null) {
			INSTANCE.clearPreviousSearches();
		}

		Activator.WORKSPACE.removeResourceChangeListener(RESOURCE_CHANGE_LISTENER);

		CASTLESearchingSettings.stopWatchingSettings();

		if (indexJob != null) {
			indexJob.cancel();

			try {
				indexJob.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (searchJob != null) {
			searchJob.cancel();
			try {
				searchJob.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		CASTLESearchingSettings.closeSearcherManagers();

		CASTLEIndexer.closeWriter();
	}

	private static StyledString getStyledContent(final CASTLESearchResultEntry entry) {
		// TODO: how to prevent meta documents from being returned in results
		// TODO: replace tabs with spaces in content (since tabs don't seem to
		// show, at all, causing text to get squished together)

		if (entry.getExtension() == null) {
			// Added to prevent NullPointerException if there was no content
			// (such as if metadocument was incorrectly returned)
			if (entry.getContent() == null) {
				return new StyledString();
			}

			return new StyledString(entry.getContent());
		}

		if (entry.getExtension().equals("java")) {
			return CASTLEJavaIndexer.getJavaStyledContent(entry);
		}

		return new StyledString(entry.getContent());

		// result.append(entry.getContent(), StyledString.COUNTER_STYLER);
		// result.append(entry.getContent(), StyledString.DECORATIONS_STYLER);
		// result.append(entry.getContent(), StyledString.QUALIFIER_STYLER);
		// // result.append(entry.getContent(),
		// StyledString.createColorRegistryStyler(JFacePreferences.ERROR_COLOR,
		// null));
		// result.append(entry.getContent(),
		// StyledString.createColorRegistryStyler(
		// JFacePreferences.INFORMATION_FOREGROUND_COLOR,
		// JFacePreferences.INFORMATION_BACKGROUND_COLOR));
		//
		// //
		// https://www.javatips.net/api/org.eclipse.koneki.ldt-master/plugins/org.eclipse.koneki.ldt.ui/src/org/eclipse/koneki/ldt/ui/internal/buildpath/LuaExecutionEnvironmentLabelProvider.java
		// result.append(entry.getContent(), BOLD_STYLER);
		//
		// result.append(entry.getContent(), JAVA_STRING_STYLER);
	}

	public void selectAndRevealTopResult() {
		// Set focus, so if switch setting (like incremental or searcher) will
		// then will still select the first element
		this.getViewer().getControl().setFocus();

		// Top result (or null or there are no results)
		Object element = this.getViewer().getElementAt(0);

		this.getViewer().setSelection(new StructuredSelection(element), true);
	}

	public void revealTopResult() {
		// Top result (or null or there are no results)
		Object element = this.getViewer().getElementAt(0);

		this.getViewer().reveal(element);
	}

	private void styleSearchText() {
		Matcher matcher = SEARCH_TEXT_FORMATTER_MATCHERS.get().reset(this.searchText.getText());

		List<StyleRange> ranges = new ArrayList<>();

		while (matcher.find()) {
			int length = matcher.end() - matcher.start();
			ranges.add(new StyleRange(matcher.start(), length, null, null, SWT.BOLD));
		}

		this.searchText.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	public String getStatus() {
		if (this.statusLabel.isDisposed()) {
			return "UNKNOWN";
		}

		return this.statusLabel.getText();
	}

	public void setStatus(final String status) {
		if (!this.statusLabel.isDisposed()) {
			this.statusLabel.setText(status);
		}
	}

	public String getMessage() {
		if (this.messageLabel.isDisposed()) {
			return "UNKNOWN";
		}

		return this.messageLabel.getText();
	}

	public void setMessage(final String message) {
		if (!this.messageLabel.isDisposed()) {
			this.messageLabel.setText(message);
		}
	}

	public void setResults(final List<CASTLESearchResultEntry> results) {
		this.viewer.setInput(results);
	}

	@Nullable
	public Display getDisplay() {
		if (this.searchText.isDisposed()) {
			return null;
		}

		return this.searchText.getDisplay();
	}

	public static void refreshFonts(final Font font) {
		Display display = INSTANCE == null ? null : INSTANCE.getDisplay();

		if (display != null) {
			display.syncExec(() -> {
				// int currentHeight =
				// INSTANCE.viewer.getControl().getFont().getFontData()[0].getHeight();
				// int newHeight = font.getFontData()[0].getHeight();

				// if (currentHeight == newHeight) {
				// return;
				// }

				// TODO: may need no resize objects as font size increases
				// INSTANCE.incremental.setFont(font);
				INSTANCE.comboDropDown.setFont(font);

				// Work around Eclipse bug by creating new instance when resize
				/*
				 * java.lang.IllegalArgumentException: Argument not valid at
				 * org.eclipse.swt.SWT.error(SWT.java:4701) at
				 * org.eclipse.swt.SWT.error(SWT.java:4635) at
				 * org.eclipse.swt.SWT.error(SWT.java:4606) at
				 * org.eclipse.swt.graphics.TextLayout.setFont(TextLayout.java:
				 * 2991)
				 */
				String searchText = INSTANCE.searchText.getText();
				INSTANCE.searchText.dispose();
				INSTANCE.createSearchText(INSTANCE.statusLabel.getParent());
				// Move the new control to the correct location, after the combo
				INSTANCE.searchText.moveBelow(INSTANCE.comboDropDown);
				INSTANCE.searchText.setFont(font);
				INSTANCE.searchText.setText(searchText);
				INSTANCE.styleSearchText();

				INSTANCE.statusLabel.setFont(font);
				INSTANCE.messageLabel.setFont(font);

				INSTANCE.viewer.getControl().setFont(font);
				INSTANCE.viewer.refresh();

				// Recalculate display based on changed font size
				// https://www.eclipse.org/articles/Article-Understanding-Layouts/Understanding-Layouts.htm
				INSTANCE.messageLabel.getParent().layout();
			});
		}
	}

	/**
	 * <p><b>NOTE:</b>Must call from UI thread or else won't do anything</p>
	 * @param isEnabled
	 */
	private void setPreviousSearchesEnabled(final boolean isEnabled) {
		// TODO: https://www.eclipse.org/forums/index.php/t/449683/

		String commandId = "info.codesaway.castlesearching.commands.previousSearches";

		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		//		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);

		if (isEnabled && previousSearcHandlerActivation == null) {
			//			Command command = commandService.getCommand(commandId);
			previousSearcHandlerActivation = handlerService.activateHandler(commandId, new PreviousSearchesHandler());
		} else if (!isEnabled && previousSearcHandlerActivation != null) {
			handlerService.deactivateHandler(previousSearcHandlerActivation);
			previousSearcHandlerActivation = null;
		}

		// Reference: http://blog.sdruskat.net/control-visibility-of-menu-contributions-on-text-selection-with-propertytesters/
		// https://stackoverflow.com/a/21836699
		/*
		 *    <!--<extension
		 point="org.eclipse.core.expressions.propertyTesters">
		<propertyTester
		    class="info.codesaway.castlesearching.propertytesters.PreviousSearchesTester"
		    id="info.codesaway.castlesearching.propertytesters.PreviousSearchesTester"
		    namespace="info.codesaway.castlesearching.propertytesters"
		    properties="nonEmpty"
		    type="java.lang.Object">
		</propertyTester>
		</extension>
		-->
		<!--<extension
		 point="org.eclipse.ui.handlers">
		<handler
		    class="info.codesaway.castlesearching.handlers.PreviousSearchesHandler"
		    commandId="info.codesaway.castlesearching.commands.previousSearches">
		 <enabledWhen>
		       <test
		       		property="info.codesaway.castlesearching.propertytesters.nonEmpty">
		       </test>
		 </enabledWhen>
		</handler>
		</extension>
		-->
		 */
	}

	private void clearPreviousSearches() {
		this.previousSearches.clear();
		this.setPreviousSearchesEnabled(false);
	}

	public void addSearch(final CASTLESearchResult result) {
		// Remove if it's the same search
		this.previousSearches.removeIf(i -> i.getSearch().equals(result.getSearch()));

		while (this.previousSearches.size() >= this.previousSeachesLimit) {
			// Remove the last search
			this.previousSearches.removeLast();
		}

		// Push the new search onto the stack
		this.previousSearches.push(result);
		this.setPreviousSearchesEnabled(true);
	}

	public void fillPreviousSearches(final IMenuManager manager) {
		if (this.previousSearches.isEmpty()) {
			//			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			return;
		}

		for (CASTLESearchResult result : this.previousSearches) {
			Action action = new Action() {
				@Override
				public void run() {
					CASTLESearchingView.this.restoreSearchResult(result);
				}
			};
			CASTLESearch search = result.getSearch();
			String text = String.format("'%s' - %s (in %s)", search.getText(), result.getMessage(),
					search.getSearcher().getName());
			action.setText(text);

			manager.add(action);
		}

		manager.add(new Separator());

		Action action = new Action() {
			@Override
			public void run() {
				CASTLESearchingView.this.clearPreviousSearches();
			}
		};
		String text = "Clear History";
		action.setText(text);

		manager.add(action);

		manager.update();
	}

	protected void restoreSearchResult(final CASTLESearchResult result) {
		CASTLESearch search = result.getSearch();

		// TODO: seems setting these values cause the search to run
		this.searchText.setText(search.getText());
		this.setSearcherName(search.getSearcher().getName());
		this.setMessage(result.getMessage());
		this.setResults(result.getResults());
		//		System.out.println("Result size: " + result.getResults().size());

		// TODO: toggle / restore settings based on CASTLESearch
		// TODO: if delete result entry, make sure actually removing from CASTLESearchResult
		// (if seems that the restore brings it back, which suggests it's being deleted from a copy, not from the actual search results)
	}
}
