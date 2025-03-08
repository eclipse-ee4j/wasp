package org.glassfish.wasp.taglibs.standard.tag.common.fmt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import jakarta.servlet.jsp.JspException;

import org.glassfish.wasp.taglibs.standard.resources.Resources;
import org.glassfish.wasp.taglibs.standard.tag.common.core.Util;

class DateFormatSupport {

    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String DATETIME = "both";
    private static final String LOCAL_DATE = "localDate";
    private static final String LOCAL_DATETIME = "localDateTime";
    private static final String LOCAL_TIME = "localTime";
    private static final String OFFSET_TIME = "offsetTime";
    private static final String OFFSET_DATETIME = "offsetDateTime";
    private static final String ZONED_DATETIME = "zonedDateTime";
    private static final Map<String, TemporalQuery<Object>> JAVA_TIME_TYPES = Map.of(
            LOCAL_DATE, LocalDate::from,
            LOCAL_DATETIME, LocalDateTime::from,
            LOCAL_TIME, LocalTime::from,
            OFFSET_TIME, OffsetTime::from,
            OFFSET_DATETIME, OffsetDateTime::from,
            ZONED_DATETIME, ZonedDateTime::from);
    private static final Pattern ESCAPED_DATE_TIME_PATTERN = Pattern.compile("'[^']*+'");

    private final DateFormat dateFormat;
    private final DateTimeFormatter dateTimeFormatter;
    private final TemporalQuery<Object> fromJavaTime;

    private DateFormatSupport(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        dateTimeFormatter = null;
        fromJavaTime = null;
    }

    private DateFormatSupport(DateTimeFormatter dateTimeFormatter, TemporalQuery<Object> fromJavaTime) {
        dateFormat = null;
        this.dateTimeFormatter = dateTimeFormatter;
        this.fromJavaTime = fromJavaTime;
    }

    public static DateFormatSupport createFormatter(Locale locale, TimeZone timeZone, String type, String dateStyle, String timeStyle, String pattern, boolean forParsing) throws JspException {
        DateFormat dateFormat = null;
        DateTimeFormatter dateTimeFormatter = null;
        DateTimeFormatterBuilder dateTimeFormatterBuilder = null;

        if (type == null || DATE.equalsIgnoreCase(type)) {
            dateFormat = DateFormat.getDateInstance(getDateStyle(dateStyle, forParsing), locale);
        }
        else if (TIME.equalsIgnoreCase(type)) {
            dateFormat = DateFormat.getTimeInstance(getTimeStyle(timeStyle, forParsing), locale);
        }
        else if (DATETIME.equalsIgnoreCase(type)) {
            dateFormat = DateFormat.getDateTimeInstance(getDateStyle(dateStyle, forParsing), getTimeStyle(timeStyle, forParsing), locale);
        }
        else if (pattern != null) {
            if (LOCAL_DATE.equalsIgnoreCase(type)) {
                dateTimeFormatterBuilder = new DateTimeFormatterBuilder().appendLocalized(getFormatDateStyle(dateStyle, forParsing), null);
            }
            else if (LOCAL_DATETIME.equalsIgnoreCase(type)) {
                dateTimeFormatterBuilder = new DateTimeFormatterBuilder().appendLocalized(getFormatDateStyle(dateStyle, forParsing), getFormatTimeStyle(timeStyle, forParsing));
            }
            else if (LOCAL_TIME.equalsIgnoreCase(type)) {
                dateTimeFormatterBuilder = new DateTimeFormatterBuilder().appendLocalized(null, getFormatTimeStyle(timeStyle, forParsing));
            }
            else if (OFFSET_TIME.equalsIgnoreCase(type)) {
                dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_TIME.withLocale(locale);
            }
            else if (OFFSET_DATETIME.equalsIgnoreCase(type)) {
                dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withLocale(locale);
            }
            else if (ZONED_DATETIME.equalsIgnoreCase(type)) {
                dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale(locale);
            }
            else {
                throw new JspException(Resources.getMessage(forParsing ? "PARSE_DATE_INVALID_TYPE" : "FORMAT_DATE_INVALID_TYPE", type));
            }
        }

        if (pattern != null) {
            if (dateFormat instanceof SimpleDateFormat) {
                ((SimpleDateFormat) dateFormat).applyPattern(pattern);
            }
            else if (dateFormat != null) {
                dateFormat = new SimpleDateFormat(pattern, locale);
            }
            else {
                dateTimeFormatterBuilder = new DateTimeFormatterBuilder().appendPattern(pattern);
            }
        }

        if (dateFormat != null) {
            if (forParsing) {
                dateFormat.setLenient(false);
            }

            if (timeZone != null) {
                dateFormat.setTimeZone(timeZone);
            }

            return new DateFormatSupport(dateFormat);
        }
        else {
            if (dateTimeFormatterBuilder != null) {
                if (pattern == null || !ESCAPED_DATE_TIME_PATTERN.matcher(pattern).replaceAll("").contains("u")) {
                    dateTimeFormatterBuilder.parseDefaulting(ChronoField.ERA, 1);
                }

                dateTimeFormatter = dateTimeFormatterBuilder.toFormatter(locale).withChronology(IsoChronology.INSTANCE).withResolverStyle(ResolverStyle.STRICT);
            }

            return new DateFormatSupport(dateTimeFormatter, JAVA_TIME_TYPES.get(type));
        }
    }

    private static int getDateStyle(String dateStyle, boolean forParsing) throws JspException {
        return Util.getStyle(dateStyle, forParsing ? "PARSE_DATE_INVALID_DATE_STYLE" : "FORMAT_DATE_INVALID_DATE_STYLE");
    }

    private static int getTimeStyle(String timeStyle, boolean forParsing) throws JspException {
        return Util.getStyle(timeStyle, forParsing ? "PARSE_DATE_INVALID_TIME_STYLE" : "FORMAT_DATE_INVALID_TIME_STYLE");
    }

    private static FormatStyle getFormatDateStyle(String timeStyle, boolean forParsing) throws JspException {
        return Util.getFormatStyle(timeStyle, forParsing ? "PARSE_DATE_INVALID_DATE_STYLE" : "FORMAT_DATE_INVALID_DATE_STYLE");
    }

    private static FormatStyle getFormatTimeStyle(String timeStyle, boolean forParsing) throws JspException {
        return Util.getFormatStyle(timeStyle, forParsing ? "PARSE_DATE_INVALID_TIME_STYLE" : "FORMAT_DATE_INVALID_TIME_STYLE");
    }

    public Object parse(CharSequence text) throws ParseException {
        return dateFormat != null ? dateFormat.parse((String) text) : dateTimeFormatter.parse(text, fromJavaTime);
    }

    public String format(Object obj) {
        return dateFormat != null ? dateFormat.format(obj) : dateTimeFormatter.format((TemporalAccessor) obj);
    }
}
