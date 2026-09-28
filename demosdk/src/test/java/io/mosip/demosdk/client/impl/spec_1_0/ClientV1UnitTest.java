package io.mosip.demosdk.client.impl.spec_1_0;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Map;

import org.apache.commons.codec.EncoderException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.mosip.demosdk.client.utils.TextMatcherUtil;

/**
 * Unit tests for {@link Client_V_1_0}: exact, partial and phonetic matching plus {@code init}.
 */
class ClientV1UnitTest {

	/** Empty flags map; flags are ignored by the implementation. */
	private static final Map<String, String> NO_FLAGS = Map.of();

	/** Instance under test (stateless). */
	private final Client_V_1_0 client = new Client_V_1_0();

	/** Identical values give a full exact match. */
	@Test
	void exactMatchSameStringsReturns100() {
		assertEquals(Client_V_1_0.EXACT_MATCH_VALUE, client.doExactMatch("John Doe", "John Doe", NO_FLAGS));
	}

	/** Exact match ignores token order. */
	@Test
	void exactMatchDifferentOrderReturns100() {
		assertEquals(Client_V_1_0.EXACT_MATCH_VALUE, client.doExactMatch("John Doe", "Doe John", NO_FLAGS));
	}

	/** Exact match ignores case and extra whitespace. */
	@Test
	void exactMatchIgnoresCaseAndWhitespace() {
		assertEquals(Client_V_1_0.EXACT_MATCH_VALUE, client.doExactMatch("  JOHN   doe ", "john DOE", NO_FLAGS));
	}

	/** Different token counts never match exactly. */
	@Test
	void exactMatchDifferentSizeReturns0() {
		assertEquals(0, client.doExactMatch("John Doe", "John", NO_FLAGS));
	}

	/** Same token count but a differing token gives no exact match. */
	@Test
	void exactMatchSameSizeButDifferentTokenReturns0() {
		assertEquals(0, client.doExactMatch("A B", "A C", NO_FLAGS));
	}

	/** Two empty values are treated as an exact match (both token lists are empty). */
	@Test
	void exactMatchEmptyStringsReturns100() {
		assertEquals(Client_V_1_0.EXACT_MATCH_VALUE, client.doExactMatch("", "", NO_FLAGS));
	}

	/** One of two entity tokens matched: 1 * 100 / (2 + 0) = 50. */
	@Test
	void partialMatchOneOfTwoReturns50() {
		assertEquals(50, client.doPartialMatch("John", "John Doe", NO_FLAGS));
	}

	/** All tokens matched: full score. */
	@Test
	void partialMatchAllTokensReturns100() {
		assertEquals(100, client.doPartialMatch("doe john", "John Doe", NO_FLAGS));
	}

	/** An initial alone is not a matched token: 0 * 100 / (1 + 0) = 0. */
	@Test
	void partialMatchInitialOnlyReturns0() {
		assertEquals(0, client.doPartialMatch("J", "John", NO_FLAGS));
	}

	/** An initial removes its penalty: 1 * 100 / (2 + 0) = 50. */
	@Test
	void partialMatchInitialPlusTokenReturns50() {
		assertEquals(50, client.doPartialMatch("J Doe", "John Doe", NO_FLAGS));
	}

	/** An initial with no matching entity token stays unmatched: 1 * 100 / (2 + 1) = 33. */
	@Test
	void partialMatchUnmatchedInitialPenalises() {
		assertEquals(33, client.doPartialMatch("X Doe", "John Doe", NO_FLAGS));
	}

	/** An unmatched multi-character token stays unmatched: 1 * 100 / (2 + 1) = 33. */
	@Test
	void partialMatchUnmatchedWordPenalises() {
		assertEquals(33, client.doPartialMatch("Jack Doe", "John Doe", NO_FLAGS));
	}

	/** A duplicated reference token only consumes one entity token: 1 * 100 / (1 + 1) = 50. */
	@Test
	void partialMatchDuplicateReferenceTokenCountsOnce() {
		assertEquals(50, client.doPartialMatch("doe doe", "Doe", NO_FLAGS));
	}

	/** Phonetic match returns the score computed by {@link TextMatcherUtil}. */
	@Test
	void phoneticsMatchDelegatesToTextMatcherUtil() {
		try (MockedStatic<TextMatcherUtil> util = mockStatic(TextMatcherUtil.class)) {
			util.when(() -> TextMatcherUtil.phoneticsMatch("abc", "abc", "en")).thenReturn(100);
			assertEquals(100, client.doPhoneticsMatch("abc", "abc", "en", NO_FLAGS));
		}
	}

	/** An {@link EncoderException} is logged and yields a score of 0. */
	@Test
	void phoneticsMatchEncoderExceptionReturns0() {
		try (MockedStatic<TextMatcherUtil> util = mockStatic(TextMatcherUtil.class)) {
			util.when(() -> TextMatcherUtil.phoneticsMatch("x", "y", "en")).thenThrow(new EncoderException("boom"));
			assertEquals(0, client.doPhoneticsMatch("x", "y", "en", NO_FLAGS));
		}
	}

	/** Real phonetic match of identical names gives the maximum score. */
	@Test
	void phoneticsMatchRealIdenticalNamesReturns100() {
		assertEquals(100, client.doPhoneticsMatch("John", "John", "english", NO_FLAGS));
	}

	/** {@code init} only logs and must not throw. */
	@Test
	void initDoesNotThrow() {
		assertDoesNotThrow(client::init);
	}
}
