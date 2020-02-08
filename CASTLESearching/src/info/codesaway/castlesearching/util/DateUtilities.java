package info.codesaway.castlesearching.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

public class DateUtilities {
	/**
	 * Convert to Date
	 *
	 * @param pLocalDate
	 * @return
	 */
	public static Date asDate(final LocalDate pLocalDate) {
		return asDate(pLocalDate, ZoneId.systemDefault());
	}

	/**
	 * Convert to Date
	 *
	 * @param pLocalDate
	 * @param pZoneId
	 * @return
	 */
	public static Date asDate(final LocalDate pLocalDate, final ZoneId pZoneId) {
		return Date.from(pLocalDate.atStartOfDay(pZoneId).toInstant());
	}

	// TODO: Java's regex doesn't allow underscore or hyphen in group name
	// Uses RegExPlus regular expression library
	// (see stackoverflow on how it was imported)
	// https://stackoverflow.com/a/5788295
	public static final Pattern DATE_PATTERN = Pattern
			.compile("(?<localDate>\\d{1,2}/\\d{1,2}/\\d{4})|(?<isoDate>\\d{4}-\\d{2}-\\d{2})");
	public static final ThreadLocal<Matcher> DATE_MATCHER = ThreadLocal.withInitial(() -> DATE_PATTERN.matcher(""));

	// XXX: how to handle different locales (such as UK?)
	// (sounds like a good setting choice to use M/d/yyyy or d/M/yyyy)
	private static final DateTimeFormatter MDYYYY_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

	/**
	 *
	 *
	 * @param date
	 * @param isLocalDate indicates if in format M/d/yyyy (if not, expecting iso-date)
	 * @return
	 */
	@Nullable
	public static LocalDate parseLocalDate(final String date, final boolean isLocalDate) {
		if (date.isEmpty()) {
			return null;
		}

		// Parse date depending on format
		DateTimeFormatter formatter = isLocalDate
				// TODO: handle local date format (based on setting, need to add setting)
				? MDYYYY_DATE_FORMATTER
				: DateTimeFormatter.ISO_LOCAL_DATE;

		try {
			return formatter.parse(date, LocalDate::from);
		} catch (DateTimeParseException e) {
			// Handle invalid dates, such as 50/12/2002
			return null;
		}
	}
}
