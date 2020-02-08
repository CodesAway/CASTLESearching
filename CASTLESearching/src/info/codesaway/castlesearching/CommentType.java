package info.codesaway.castlesearching;

public enum CommentType {
	NONE("NONE"), SINGLE("comment"), BLOCK("block comment"), JAVADOC("Javadoc"),

	/**
	 * Indicates a block comment which starts and ends on the same line
	 */
	SINGLE_BLOCK("block comment"),

	/**
	 * Indicates a block comment which started but didn't end and has other content on the same line
	 */
	BLOCK_START("block start");

	private final String type;

	private CommentType(final String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return this.getType();
	}
}
