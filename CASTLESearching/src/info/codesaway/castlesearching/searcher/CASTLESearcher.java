package info.codesaway.castlesearching.searcher;

import java.nio.file.Path;

public class CASTLESearcher {
	private final String name;
	private final Path indexPath;
	private final int hitLimit;

	public CASTLESearcher(final String name, final Path indexPath, final int hitLimit) {
		this.name = name;
		this.indexPath = indexPath;
		this.hitLimit = hitLimit;
	}

	public String getName() {
		return this.name;
	}

	public Path getIndexPath() {
		return this.indexPath;
	}

	public int getHitLimit() {
		return this.hitLimit;
	}
}
