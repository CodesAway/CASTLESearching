package info.codesaway.castlesearching.util;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ILineTracker;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

public class JDTUtilities {
	private JDTUtilities() {
		throw new UnsupportedOperationException();
	}

	public static void addRange(final ISourceRange sourceRange, final String value,
			final RangeMap<Integer, String> rangeMap, final ILineTracker lineTracker) {
		try {
			int start = sourceRange.getOffset();
			int end = start + sourceRange.getLength();

			// Add 1 so line number aligns line numbers shown in Eclipse
			// (change 0-based to 1-based number)
			int startLine = lineTracker.getLineNumberOfOffset(start) + 1;
			int endLine = lineTracker.getLineNumberOfOffset(end) + 1;

			rangeMap.put(Range.closed(startLine, endLine), value);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
