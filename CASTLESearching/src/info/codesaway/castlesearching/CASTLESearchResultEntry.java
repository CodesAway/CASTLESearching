package info.codesaway.castlesearching;

public class CASTLESearchResultEntry {
	private final int resultIndex;
	private final String resultNumber;
	private final String file;
	private final String line;
	private final String content;
	private final String type;
	private final String path;
	private final String extension;

	/**
	 *
	 * @param resultIndex the result index (starts with 1)
	 * @param file
	 * @param line
	 * @param content
	 * @param type
	 * @param path
	 */
	public CASTLESearchResultEntry(final int resultIndex, final String file, final String line, final String content,
			final String type, final String path, final String extension) {
		this.resultIndex = resultIndex;
		this.resultNumber = String.valueOf(resultIndex);

		this.file = file;
		this.line = line;
		this.content = content;
		this.type = type;
		this.path = path;
		this.extension = extension;
	}

	public int getResultIndex() {
		return this.resultIndex;
	}

	public String getResultNumber() {
		return this.resultNumber;
	}

	public String getFile() {
		return this.file;
	}

	public String getLine() {
		return this.line;
	}

	public String getContent() {
		return this.content;
	}

	public String getType() {
		return this.type;
	}

	public String getPath() {
		return this.path;
	}

	@Override
	public String toString() {
		return this.getFile() + ":" + this.getLine();
	}

	public int getLineNumber() {
		try {
			return Integer.parseInt(this.getLine());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getExtension() {
		return this.extension;
	}
}
