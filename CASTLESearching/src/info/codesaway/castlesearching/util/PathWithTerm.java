package info.codesaway.castlesearching.util;

import java.io.File;
import java.nio.file.Path;

import org.apache.lucene.index.Term;
import org.eclipse.jdt.annotation.NonNullByDefault;

import info.codesaway.castlesearching.indexer.CASTLEIndexer;

@NonNullByDefault
public interface PathWithTerm {
	public Path getPath();

	public File getFile();

	public Term getTerm();

	public static PathWithTerm wrap(final Path path) {
		return new Wrapper(path);
	}

	public static Term getTerm(final String pathname) {
		return new Term(CASTLEIndexer.FULL_PATH_FIELD, pathname);
	}

	public static class Wrapper implements PathWithTerm {
		private final Path path;
		private final File file;
		private final Term term;

		@SuppressWarnings("null")
		private Wrapper(final Path path) {
			this.path = path;
			this.file = path.toFile();
			this.term = PathWithTerm.getTerm(path.toString());
		}

		@Override
		public File getFile() {
			return this.file;
		}

		@Override
		public Path getPath() {
			return this.path;
		}

		@Override
		public Term getTerm() {
			return this.term;
		}
	}
}
