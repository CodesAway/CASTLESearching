package info.codesaway.castlesearching.util;

import java.io.File;
import java.nio.file.Path;

import org.apache.lucene.index.Term;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class PathWithLastModified implements PathWithTerm {
	private final String project;
	private final Path path;
	private final File file;
	private final String pathname;

	private final long lastModified;
	private final Term term;

	@SuppressWarnings("null")
	public PathWithLastModified(final String project, final Path path) {
		this.project = project;
		this.path = path;
		this.file = path.toFile();
		this.pathname = path.toString();

		// Data used when indexing
		this.lastModified = this.file.lastModified();
		this.term = PathWithTerm.getTerm(this.pathname);
	}

	@Override
	public String getProject() {
		// TODO Auto-generated method stub
		return this.project;
	}

	@Override
	public Path getPath() {
		return this.path;
	}

	@Override
	public File getFile() {
		return this.file;
	}

	public String getPathname() {
		return this.pathname;
	}

	public long getLastModified() {
		return this.lastModified;
	}

	@Override
	public Term getTerm() {
		return this.term;
	}

	@Override
	public String toString() {
		return this.pathname;
	}
}
