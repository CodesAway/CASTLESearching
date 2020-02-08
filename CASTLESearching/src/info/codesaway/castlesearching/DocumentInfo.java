package info.codesaway.castlesearching;

public class DocumentInfo {
	// Don't need document number since map based on pathname
	// (can always add if needed)
	//	private final int documentNumber;
	private final long lastModified;
	private final long documentVersion;

	public DocumentInfo(final long lastModified, final long documentVersion) {
		//	public DocumentInfo(final int documentNumber, final long lastModified) {
		//		this.documentNumber = documentNumber;
		this.lastModified = lastModified;
		this.documentVersion = documentVersion;
	}

	//	public int getDocumentNumber() {
	//		return this.documentNumber;
	//	}

	public long getLastModified() {
		return this.lastModified;
	}

	public long getDocumentVersion() {
		return this.documentVersion;
	}
}
