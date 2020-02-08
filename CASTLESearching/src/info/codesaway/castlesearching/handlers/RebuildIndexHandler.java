package info.codesaway.castlesearching.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import info.codesaway.castlesearching.indexer.CASTLEIndexer;

public class RebuildIndexHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		CASTLEIndexer.rebuildEntireIndex();
		return null;
	}
}
