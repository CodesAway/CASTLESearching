package info.codesaway.castlesearching;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "info.codesaway.castlesearching"; //$NON-NLS-1$

	// The shared instance
	private static Activator INSTANCE;

	public static Path STATE_LOCATION;
	public static String SETTINGS_FILENAME = "settings.xml";

	public static final IWorkspace WORKSPACE = ResourcesPlugin.getWorkspace();
	public static final IWorkspaceRoot WORKSPACE_ROOT = WORKSPACE.getRoot();

	public static final String WORKSPACE_PATHNAME = WORKSPACE_ROOT.getLocation().toString();

	public static final Path WORKSPACE_PATH = Paths.get(WORKSPACE_PATHNAME);

	public static File SETTINGS_FILE;
	public static Path SETTINGS_PATH;

	// Managed resources (don't need to dispose on my own)
	// public static final Font BOLD_FONT =
	// JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

	// public static final Styler BOLD_STYLER = new
	// BoldStylerProvider(JFaceResources.getFontRegistry().defaultFont())
	// .getBoldStyler();
	private static Font FONT;
	private static Font BOLD_FONT;

	public static Styler BOLD_STYLER;

	private static final IPropertyChangeListener PROPERTY_CHANGE_LISTENER = e -> {
		if (e.getProperty().equals("org.eclipse.jdt.ui.editors.textfont")) {
			// this.viewer.getControl().setFont(JFaceResources.getTextFont());
			refreshStyles();
		}
	};

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		INSTANCE = this;

		// FontDescriptor descriptor = FontDescriptor.createFrom(BOLD_FONT);
		// // Setter creates new object, so have to assign back to variable
		// // https://stackoverflow.com/a/21675911/12610042
		// descriptor = descriptor.setStyle(SWT.BOLD | SWT.ITALIC);
		// BOLD_ITALIC_FONT = descriptor.createFont(Display.getCurrent());

		STATE_LOCATION = this.getStateLocation().toFile().toPath();

		SETTINGS_FILE = this.getStateLocation().append(SETTINGS_FILENAME).toFile();
		SETTINGS_PATH = SETTINGS_FILE.toPath();

		// Create empty files, so doesn't cause error when perform search
		IPath abbreviationsPath = this.getStateLocation().append("abbreviations.txt");
		File abbreviationsFile = abbreviationsPath.toFile();
		abbreviationsFile.createNewFile();

		IPath synonymsPath = this.getStateLocation().append("synonyms.txt");
		File synonymsFile = synonymsPath.toFile();
		synonymsFile.createNewFile();

		// NOTE: settings.xml is created and read when open CASTLESearchingView
		// (this allows closing and reopening the view to load new settings)
		// (will eventually add reload button, once i figure out how to add
		// items to the view's toolbar)

		JFaceResources.getFontRegistry().addListener(PROPERTY_CHANGE_LISTENER);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		INSTANCE = null;
		super.stop(context);

		CASTLESearchingView.cancelJobs();

		JFaceResources.getFontRegistry().removeListener(PROPERTY_CHANGE_LISTENER);
		disposeFonts();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return INSTANCE;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void disposeFonts() {
		if (FONT != null) {
			FONT.dispose();
		}

		if (BOLD_FONT != null) {
			BOLD_FONT.dispose();
		}
	}

	public static void refreshStyles() {
		// BOLD_STYLER = new
		// BoldStylerProvider(JFaceResources.getTextFont()).getBoldStyler();
		// int currentHeight = FONT != null ? FONT.getFontData()[0].getHeight()
		// : 0;
		// int newHeight =
		// JFaceResources.getTextFont().getFontData()[0].getHeight();

		// if (currentHeight == newHeight) {
		// // No change, so don't need to create new fonts / styler
		//
		// // Still need to refresh fonts in view, since view may not exist yet
		// CASTLESearchingView.refreshFonts(Activator.FONT);
		// return;
		// }

		// Use the default font, but get the font size from the text font
		// This way, as the text size changes, so does the size of the text in
		// the view
		FontDescriptor defaultFontDescriptor = JFaceResources.getDefaultFontDescriptor();
		defaultFontDescriptor = defaultFontDescriptor
				.setHeight(JFaceResources.getTextFont().getFontData()[0].getHeight());

		disposeFonts();

		FONT = defaultFontDescriptor.createFont(Display.getCurrent());
		BOLD_FONT = defaultFontDescriptor.createFont(Display.getCurrent());

		BOLD_STYLER = new StyledString.Styler() {
			@Override
			public void applyStyles(final TextStyle textStyle) {
				textStyle.font = BOLD_FONT;
			}
		};

		// new BoldStylerProvider(BOLD_FONT).getBoldStyler();

		CASTLESearchingView.refreshFonts(Activator.FONT);
	}
}
