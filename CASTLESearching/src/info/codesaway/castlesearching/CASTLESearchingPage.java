package info.codesaway.castlesearching;

import javax.annotation.PostConstruct;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class CASTLESearchingPage extends DialogPage implements ISearchPage {
	@Nullable
	private String selected;
	private Text searchText;

	private ISearchPageContainer container;

	@Override
	public void createControl(final Composite parent) {
		// https://bassistance.de/2009/11/17/eclipse-dev-custom-search-page/
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		parent.setLayout(layout);

		new Label(parent, SWT.NONE).setText("Containing text:");
		this.searchText = new Text(parent, SWT.BORDER);
		this.searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Set search text to selected text
		// Introduced local variable to ensure isn't null when try to set value
		// (not sure if code is multi-threaded)
		String selected = this.selected;
		if (selected != null) {
			this.searchText.setText(selected);
			this.searchText.setSelection(0, selected.length());
		}

		this.setControl(parent);
	}

	@PostConstruct
	@Override
	public boolean performAction() {
		String searchText = this.searchText.getText();

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {

			// https://www.programcreek.com/java-api-examples/?api=org.eclipse.e4.core.contexts.IEclipseContext
			// IEclipseContext context =
			// EclipseContextFactory.getServiceContext(Activator.getContext());

			// Display.getCurrent().syncExec(() -> {
			// System.out.println("Context: " + context);
			// https://stackoverflow.com/a/34306890/12610042

			// EPartService partService =
			// Services.getInstance().getPartService();

			// System.out.println("Part service: " + partService);

			// CASTLESearchingView searchView = (CASTLESearchingView)
			// partService.showPart(CASTLESearchingView.ID, PartState.ACTIVATE);
			// searchView.search(searchText, 0);
			// });

			// CASTLESearchingView searchView = (CASTLESearchingView)
			// partService.showPart(CASTLESearchingView.ID, PartState.ACTIVATE);

			// org.eclipse.ui.internal.E4PartWrapper
			page.showView(CASTLESearchingView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

			// CASTLESearchingView searchView = (CASTLESearchingView)
			// page.showView(CASTLESearchingView.ID, null,
			// IWorkbenchPage.VIEW_ACTIVATE);
			if (CASTLESearchingView.INSTANCE != null) {
				// TODO: handle better (such as waiting or something)
				CASTLESearchingView.INSTANCE.search(searchText, 0);
			}
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void setContainer(final ISearchPageContainer container) {
		this.container = container;

		if (container.getSelection() instanceof TextSelection) {
			this.selected = ((TextSelection) container.getSelection()).getText();
		}
	}

	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		this.searchText.setFocus();
		this.container.setPerformActionEnabled(true);
	}
}
