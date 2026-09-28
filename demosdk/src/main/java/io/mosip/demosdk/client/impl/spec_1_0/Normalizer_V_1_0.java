package io.mosip.demosdk.client.impl.spec_1_0;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.demographics.spi.IDemoNormalizer;

/**
 * Reference implementation of the MOSIP {@link IDemoNormalizer} SPI (specification version 1.0).
 *
 * <p>
 * Normalizes names and addresses before they are matched by {@link Client_V_1_0}. The
 * normalization rules are regular-expression replacements read from the Spring
 * {@link Environment} using the key template {@value #IDA_BASIC_NORMALISER}:
 * </p>
 *
 * <pre>
 * ida.demo.&lt;type&gt;.normalization.regex.&lt;language&gt;[&lt;index&gt;]=&lt;regex&gt;&lt;sep&gt;&lt;replacement&gt;
 * </pre>
 *
 * <ul>
 * <li>{@code type} is {@code name}, {@code address} or {@code common}.</li>
 * <li>{@code language} is the language code or {@code any}.</li>
 * <li>{@code index} starts at {@code 0}; loading stops at the first missing index (at most
 * {@value #NORMALIZER_CONFIG_LIMIT} entries).</li>
 * <li>{@code sep} is read from {@value #IDA_NORMALISER_SEP} and defaults to
 * {@value #DEFAULT_SEP}. A value without the separator means "replace with empty string".</li>
 * </ul>
 *
 * <p>
 * For a given type and language, patterns are applied in this order: {@code <type>/<language>},
 * {@code <type>/any}, {@code common/<language>}, {@code common/any}. Patterns are compiled with
 * {@link Pattern#UNICODE_CHARACTER_CLASS}.
 * </p>
 */
@Component
public class Normalizer_V_1_0 implements IDemoNormalizer {

	/** Property key template: {@code type}, {@code language}, then the index placeholder. */
	public static final String IDA_BASIC_NORMALISER = "ida.demo.%s.normalization.regex.%s[%s]";

	/** Property key holding the separator between the regex and its replacement. */
	public static final String IDA_NORMALISER_SEP = "ida.norm.sep";

	/** Separator used when {@value #IDA_NORMALISER_SEP} is not configured. */
	private static final String DEFAULT_SEP = "=";

	/** Maximum number of indexed patterns read for one type/language combination. */
	private static final int NORMALIZER_CONFIG_LIMIT = 1000;

	/** Spring environment that supplies the normalization properties. */
	@Autowired
	private Environment environment;

	/**
	 * Normalizes a name.
	 *
	 * <p>
	 * First removes every title configured for {@code language} (for example {@code Mr},
	 * {@code Dr}), longest title first, both with and without a trailing dot and in original,
	 * lower and upper case. Then applies the {@code name} and {@code common} patterns for the
	 * language and for {@code any}. The result is trimmed.
	 * </p>
	 *
	 * @param nameInfo    name to normalize; must not be {@code null}
	 * @param language    language code used to select titles and patterns
	 * @param fetchTitles titles keyed by language code; the list for {@code language} is sorted
	 *                    in place by descending length
	 * @return the normalized, trimmed name
	 */
	@Override
	public String normalizeName(String nameInfo, String language, Map<String, List<String>> fetchTitles) {
		StringBuilder nameBuilder = new StringBuilder(nameInfo);
		List<String> titlesList = fetchTitles.get(language);
		if (null != titlesList) {
			Collections.sort(titlesList, Comparator.comparing(String::length).reversed());
			for (String title : titlesList) {
				String title1 = title + ".";
				removeAllCases(nameBuilder, title1);
				removeAllCases(nameBuilder, title);
			}
		}
		Map<Pattern, String> namePatterns = normalizeWithCommonAttributes("name", language);
		normalize(nameBuilder, namePatterns);
		return nameBuilder.toString().trim();
	}

	/**
	 * Loads the patterns for a type and language merged with the {@code any} and {@code common}
	 * patterns, in application order.
	 *
	 * @param type     normalization type ({@code name} or {@code address})
	 * @param language language code
	 * @return ordered map of compiled pattern to replacement string
	 */
	private Map<Pattern, String> normalizeWithCommonAttributes(String type, String language) {
		Map<Pattern, String> namePatterns = getNormalisersByTypeAndLang(type, language);
		namePatterns.putAll(getNormalisersByTypeAndLang(type, "any"));
		namePatterns.putAll(getNormalisersByTypeAndLang("common", language));
		namePatterns.putAll(getNormalisersByTypeAndLang("common", "any"));
		return namePatterns;
	}

	/**
	 * Removes every occurrence of a title from the builder, matching it case-insensitively and
	 * deleting its original, lower-case and upper-case forms.
	 *
	 * @param nameBuilder builder holding the name; modified in place
	 * @param title1      title to remove
	 */
	private static void removeAllCases(StringBuilder nameBuilder, String title1) {
		while (nameBuilder.toString().toLowerCase().contains(title1.toLowerCase())) {
			int index = nameBuilder.indexOf(title1);
			if (index >= 0) {
				nameBuilder.replace(index, index + title1.length(), "");
			}

			index = nameBuilder.indexOf(title1.toLowerCase());
			if (index >= 0) {
				nameBuilder.replace(index, index + title1.length(), "");
			}

			index = nameBuilder.indexOf(title1.toUpperCase());
			if (index >= 0) {
				nameBuilder.replace(index, index + title1.length(), "");
			}
		}
	}

	/**
	 * Normalizes an address by applying the {@code address} and {@code common} patterns for the
	 * language and for {@code any}.
	 *
	 * @param address  address to normalize; must not be {@code null}
	 * @param language language code used to select patterns
	 * @return the normalized, trimmed address
	 */
	@Override
	public String normalizeAddress(String address, String language) {
		Map<Pattern, String> addressPattern = normalizeWithCommonAttributes("address", language);
		return normalize(address, addressPattern);
	}

	/**
	 * Applies the patterns to a string and trims the result.
	 *
	 * @param data              value to normalize
	 * @param normalizePatterns ordered map of pattern to replacement
	 * @return the normalized, trimmed value
	 */
	private String normalize(String data, Map<Pattern, String> normalizePatterns) {
		StringBuilder addressBuilder = new StringBuilder(data);
		normalize(addressBuilder, normalizePatterns);
		return addressBuilder.toString().trim();
	}

	/**
	 * Applies each pattern in order, replacing every non-empty match in place. Scanning resumes
	 * after the inserted replacement, so a replacement is never re-matched by the same pattern.
	 * A zero-length match stops processing of that pattern to avoid an infinite loop.
	 *
	 * @param stringBuilder builder to modify in place
	 * @param normPatterns  ordered map of pattern to replacement
	 */
	private void normalize(StringBuilder stringBuilder, Map<Pattern, String> normPatterns) {
		for (Map.Entry<Pattern, String> entry : normPatterns.entrySet()) {
			Matcher m = entry.getKey().matcher(stringBuilder);
			int findStart = 0;
			while (m.find(findStart)) {
				int start = m.start();
				int end = m.end();
				if (end - start == 0) {
					break;
				}

				String replacement = entry.getValue();
				stringBuilder.replace(m.start(), end, replacement);
				findStart = start + replacement.length();
			}
		}
	}

	/**
	 * Reads indexed patterns for one key template from the environment.
	 *
	 * <p>
	 * Iterates indices {@code 0..}{@value #NORMALIZER_CONFIG_LIMIT}{@code -1} and stops at the
	 * first missing property. Each value is split on the configured separator into a regex and
	 * a replacement; a missing replacement means the empty string.
	 * </p>
	 *
	 * @param normalizerKey property key with a single {@code %s} placeholder for the index
	 * @return ordered map of compiled pattern to replacement string
	 */
	private Map<Pattern, String> getBasicNormalisers(String normalizerKey) {
		Map<Pattern, String> basicPatternMap = new LinkedHashMap<>();
		for (int i = 0; i < NORMALIZER_CONFIG_LIMIT; i++) {
			String basicPattern = String.format(normalizerKey, i);
			String normaliseValue = environment.getProperty(basicPattern);
			if (null != normaliseValue) {
				String sep = environment.getProperty(IDA_NORMALISER_SEP, DEFAULT_SEP);
				if (normaliseValue.contains(sep)) {
					String[] patternReplacement = normaliseValue.split(sep);
					Pattern normPattern = Pattern.compile(patternReplacement[0], Pattern.UNICODE_CHARACTER_CLASS);
					String replacement;
					if (patternReplacement.length > 1) {
						replacement = patternReplacement[1];
					} else {
						replacement = "";
					}
					basicPatternMap.put(normPattern, replacement);
				} else {
					basicPatternMap.put(Pattern.compile(normaliseValue, Pattern.UNICODE_CHARACTER_CLASS), "");
				}
			} else {
				break;
			}
		}
		return basicPatternMap;
	}

	/**
	 * Reads the patterns configured for a type and language.
	 *
	 * @param type     normalization type ({@code name}, {@code address} or {@code common})
	 * @param language language code or {@code any}
	 * @return ordered map of compiled pattern to replacement string
	 */
	private Map<Pattern, String> getNormalisersByTypeAndLang(String type, String language) {
		return getBasicNormalisers(String.format(IDA_BASIC_NORMALISER, type, language, "%s"));
	}

}
