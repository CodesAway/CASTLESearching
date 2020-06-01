package info.codesaway.castlesearching.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import info.codesaway.castlesearching.CASTLESearchingView;

public class SearchHandler extends AbstractHandler {
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// Toggle the command we just clicked and perform a search
		HandlerUtil.toggleCommandState(event.getCommand());
		CASTLESearchingView.INSTANCE.search();
		return null;
	}
}
