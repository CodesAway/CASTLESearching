package info.codesaway.castlesearching.menus;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;

import info.codesaway.castlesearching.CASTLESearchingView;

public class PreviousSearchesMenu extends ContributionItem {

	// https://stackoverflow.com/a/29412697
	// https://phisymmetry.wordpress.com/2010/01/03/eclipse-tips-how-to-create-menu-items-dynamically/
	private final IMenuListener menuListener = new IMenuListener() {
		@Override
		public void menuAboutToShow(final IMenuManager manager) {
			CASTLESearchingView.INSTANCE.fillPreviousSearches(manager);
		}
	};

	// Reference: https://insights.sigasi.com/tech/dynamic-menu-items-eclipse/
	public PreviousSearchesMenu() {
	}

	public PreviousSearchesMenu(final String id) {
		super(id);
	}

	//	private void fillMenu(final IMenuManager mgr) {
	//		CASTLESearchingView.INSTANCE.fillPreviousSearches();
	//		mgr.update();
	//	}

	//	private IContributionItem createContributionItem() {
	//		Action action = new Action() {
	//			@Override
	//			public void run() {
	//				System.out.println("Found me!");
	//			}
	//		};
	//		action.setText("Cool stuff!");
	//
	//		ActionContributionItem contributionItem = new ActionContributionItem(action);
	//		return contributionItem;
	//	}

	@Override
	public void fill(final Menu menu, final int index) {
		super.fill(menu, index);

		if (this.getParent() instanceof MenuManager) {
			MenuManager manager = (MenuManager) this.getParent();

			manager.setRemoveAllWhenShown(true);
			manager.addMenuListener(this.menuListener);
		}
		/*
		//Here you could get selection and decide what to do
		//You can also simply return if you do not want to show a menu
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				//what to do when menu is subsequently selected.
				if (e.getSource() instanceof MenuItem) {
					MenuItem item = (MenuItem) e.getSource();
					System.err.println("Clicked: " + item.getData("text"));
					item.getData(PreviousSearchesMenu.this.getId());
					//					item.dispose();
				}
			}
		};
		
		//create the menu item
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
		menuItem.setText("My menu item (" + new Date() + ")");
		menuItem.setData("text", menuItem.getText());
		menuItem.addSelectionListener(selectionAdapter);
		
		for (int i = 0; i < 10; i++) {
			// Always add item to top of list
			final MenuItem newItem = new MenuItem(menu, SWT.NONE, 0);
			newItem.setText("Item " + i);
			newItem.setData("text", newItem.getText());
		
			newItem.addSelectionListener(selectionAdapter);
		}
		*/
	}
}
