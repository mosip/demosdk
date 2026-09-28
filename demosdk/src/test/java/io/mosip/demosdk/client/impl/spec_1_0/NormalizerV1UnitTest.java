package io.mosip.demosdk.client.impl.spec_1_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * Unit tests for {@link Normalizer_V_1_0}. The {@link Environment} is mocked and injected into
 * the private {@code environment} field by {@link InjectMocks}.
 */
@ExtendWith(MockitoExtension.class)
class NormalizerV1UnitTest {

	/** Key of the first English name pattern. */
	private static final String NAME_EN_0 = "ida.demo.name.normalization.regex.en[0]";

	/** Key of the second English name pattern. */
	private static final String NAME_EN_1 = "ida.demo.name.normalization.regex.en[1]";

	/** Key of the first English address pattern. */
	private static final String ADDRESS_EN_0 = "ida.demo.address.normalization.regex.en[0]";

	/** Mocked Spring environment supplying normalization properties. */
	@Mock
	private Environment env;

	/** Instance under test. */
	@InjectMocks
	private Normalizer_V_1_0 normalizer;

	/** Default stubbing: no patterns configured and the default {@code =} separator. */
	@BeforeEach
	void setUp() {
		lenient().when(env.getProperty(anyString())).thenReturn(null);
		lenient().when(env.getProperty(eq(Normalizer_V_1_0.IDA_NORMALISER_SEP), anyString())).thenReturn("=");
	}

	/**
	 * Builds a mutable titles map for one language.
	 *
	 * @param language language code
	 * @param titles   titles for that language
	 * @return map of language to a mutable titles list
	 */
	private static Map<String, List<String>> titles(String language, String... titles) {
		Map<String, List<String>> map = new HashMap<>();
		map.put(language, new ArrayList<>(List.of(titles)));
		return map;
	}

	/** A configured title is stripped from the name. */
	@Test
	void normalizeNameRemovesTitle() {
		assertEquals("John Doe", normalizer.normalizeName("Mr John Doe", "en", titles("en", "Mr")));
	}

	/** Titles are removed with and without a trailing dot and in any case. */
	@Test
	void normalizeNameRemovesTitlesInAllCasesAndWithDot() {
		String out = normalizer.normalizeName("Mr. JOHN Dr mr DR. MR", "en", titles("en", "Mr", "Dr"));
		assertEquals("JOHN", out.replaceAll("\\s+", " ").trim());
	}

	/** Longer titles are removed before shorter ones that are their prefix. */
	@Test
	void normalizeNameRemovesLongestTitleFirst() {
		assertEquals("Smith", normalizer.normalizeName("Mrs Smith", "en", titles("en", "Mr", "Mrs")));
	}

	/** Titles of a different language are not applied. */
	@Test
	void normalizeNameIgnoresTitlesOfOtherLanguage() {
		assertEquals("Mr John", normalizer.normalizeName("Mr John", "en", titles("fr", "Mr")));
	}

	/** Language-specific, {@code any} and {@code common} patterns are all applied. */
	@Test
	void normalizeNameAppliesLanguageAnyAndCommonPatterns() {
		lenient().when(env.getProperty(NAME_EN_0)).thenReturn("john=J");
		lenient().when(env.getProperty("ida.demo.name.normalization.regex.any[0]")).thenReturn("doe=D");
		lenient().when(env.getProperty("ida.demo.common.normalization.regex.en[0]")).thenReturn("-= ");
		lenient().when(env.getProperty("ida.demo.common.normalization.regex.any[0]")).thenReturn("\\s+= ");

		assertEquals("J D X", normalizer.normalizeName("john-doe   X", "en", new HashMap<>()));
	}

	/** Multiple indexed patterns are read until the first missing index. */
	@Test
	void normalizeNameReadsIndexedPatternsUntilGap() {
		lenient().when(env.getProperty(NAME_EN_0)).thenReturn("a=1");
		lenient().when(env.getProperty(NAME_EN_1)).thenReturn("b=2");
		lenient().when(env.getProperty("ida.demo.name.normalization.regex.en[3]")).thenReturn("c=3");

		assertEquals("12c", normalizer.normalizeName("abc", "en", new HashMap<>()));
	}

	/** Reading stops at the 1000-entry limit even when every index is configured. */
	@Test
	void readsAtMostNormalizerConfigLimitEntries() {
		lenient().when(env.getProperty(startsWith("ida.demo.name.normalization.regex.en["))).thenReturn("q=");

		assertEquals("abc", normalizer.normalizeName("abc", "en", new HashMap<>()));
		verify(env).getProperty("ida.demo.name.normalization.regex.en[999]");
		verify(env, never()).getProperty("ida.demo.name.normalization.regex.en[1000]");
	}

	/** A pattern without a separator deletes its matches. */
	@Test
	void patternWithoutSeparatorRemovesMatches() {
		lenient().when(env.getProperty(NAME_EN_0)).thenReturn("[.,]");

		assertEquals("John Doe", normalizer.normalizeName("John., Doe", "en", new HashMap<>()));
	}

	/** A pattern with a separator but empty replacement deletes its matches. */
	@Test
	void patternWithEmptyReplacementRemovesMatches() {
		lenient().when(env.getProperty(ADDRESS_EN_0)).thenReturn("\\d+=");

		assertEquals("Main St", normalizer.normalizeAddress("123 Main St", "en"));
	}

	/** Address patterns replace every match. */
	@Test
	void normalizeAddressReplacesAllMatches() {
		lenient().when(env.getProperty(ADDRESS_EN_0)).thenReturn("\\d+=#");

		assertEquals("# Main St, #", normalizer.normalizeAddress("123 Main St, 45", "en"));
	}

	/** A custom separator configured via {@code ida.norm.sep} is honoured. */
	@Test
	void customSeparatorIsHonoured() {
		lenient().when(env.getProperty(eq(Normalizer_V_1_0.IDA_NORMALISER_SEP), anyString())).thenReturn("::");
		lenient().when(env.getProperty(ADDRESS_EN_0)).thenReturn("street::St");

		assertEquals("Main St", normalizer.normalizeAddress("Main street", "en"));
	}

	/** A replacement containing the pattern is not re-matched (no infinite loop). */
	@Test
	void replacementIsNotRematched() {
		lenient().when(env.getProperty(ADDRESS_EN_0)).thenReturn("a=aa");

		assertEquals("aabaa", normalizer.normalizeAddress("aba", "en"));
	}

	/** A zero-length match stops processing that pattern instead of looping. */
	@Test
	void zeroLengthMatchStopsPattern() {
		lenient().when(env.getProperty(NAME_EN_0)).thenReturn("^=X");

		assertEquals("abc", normalizer.normalizeName("abc", "en", new HashMap<>()));
	}

	/** Unicode character classes are enabled for patterns. */
	@Test
	void unicodeCharacterClassIsEnabled() {
		lenient().when(env.getProperty(NAME_EN_0)).thenReturn("\\W+= ");

		assertEquals("José Müller", normalizer.normalizeName("José--Müller", "en", new HashMap<>()));
	}

	/** Result is trimmed. */
	@Test
	void resultIsTrimmed() {
		assertEquals("Main St", normalizer.normalizeAddress("  Main St  ", "en"));
	}
}
