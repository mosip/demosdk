package io.mosip.demosdk.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.codec.EncoderException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TextMatcherUtil#phoneticsMatch(String, String, String)}.
 */
class TextMatcherUtilTest {

	/** Identical strings give the maximum score. */
	@Test
	void identicalStringsReturn100() throws EncoderException {
		assertEquals(100, TextMatcherUtil.phoneticsMatch("John", "John", "english").intValue());
	}

	/** Phonetically similar spellings score high. */
	@Test
	void similarSoundingNamesScoreHigh() throws EncoderException {
		int score = TextMatcherUtil.phoneticsMatch("Smith", "Smyth", "english");
		assertTrue(score >= 80, "expected >= 80 but was " + score);
	}

	/** Every score is a multiple of 20 in the range 20..100. */
	@Test
	void scoreIsMultipleOf20InRange() throws EncoderException {
		int score = TextMatcherUtil.phoneticsMatch("John", "Doe", "english");
		assertEquals(0, score % 20);
		assertTrue(score >= 20 && score <= 100, "out of range: " + score);
	}

	/** A language unknown to BeiderMorse is rejected. */
	@Test
	void unknownLanguageThrows() {
		assertThrows(IllegalArgumentException.class, () -> TextMatcherUtil.phoneticsMatch("Anna", "Anna", "xx"));
	}

	/** {@code null} arguments are rejected. */
	@Test
	void nullInputsThrow() {
		assertThrows(IllegalArgumentException.class, () -> TextMatcherUtil.phoneticsMatch(null, null, null));
	}
}
