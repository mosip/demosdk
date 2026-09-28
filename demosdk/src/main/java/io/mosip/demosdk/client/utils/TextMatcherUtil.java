package io.mosip.demosdk.client.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.bm.Languages;
import org.apache.commons.codec.language.bm.NameType;
import org.apache.commons.codec.language.bm.PhoneticEngine;
import org.apache.commons.codec.language.bm.RuleType;

/**
 * Phonetic text similarity helper used by
 * {@link io.mosip.demosdk.client.impl.spec_1_0.Client_V_1_0#doPhoneticsMatch}.
 *
 * <p>
 * Both strings are encoded with the Apache Commons Codec BeiderMorse {@link PhoneticEngine}
 * ({@link NameType#GENERIC}, {@link RuleType#EXACT}, concatenated alternatives) for the given
 * language. The two encodings are then compared with {@link Soundex#difference(String, String)},
 * which returns {@code 0..4}, and scaled to a score of {@code 20..100}.
 * </p>
 *
 * @author Nagarjuna
 */
public class TextMatcherUtil {

	/** Utility class: no instances. */
	private TextMatcherUtil() {
	}

	/**
	 * Computes a phonetic similarity score between two strings.
	 *
	 * <p>
	 * Score formula: {@code (soundexDifference + 1) * 20}, so {@code 20} means least similar
	 * and {@code 100} means most similar.
	 * </p>
	 *
	 * @param inputString  value from the authentication request
	 * @param storedString value stored for the identity
	 * @param language     BeiderMorse language name (for example {@code "english"})
	 * @return score in the range {@code 20..100}
	 * @throws EncoderException         if Soundex cannot encode one of the BeiderMorse outputs
	 * @throws IllegalArgumentException if any argument is {@code null} or {@code language} has
	 *                                  no BeiderMorse rules
	 */
	public static Integer phoneticsMatch(String inputString, String storedString, String language)
			throws EncoderException {
		PhoneticEngine phoneticEngine = new PhoneticEngine(NameType.GENERIC, RuleType.EXACT, true);

		Soundex soundex = new Soundex();

		Set<String> languageSet = new HashSet<>();
		languageSet.add(language);

		String encodedInputString = phoneticEngine.encode(inputString, Languages.LanguageSet.from(languageSet));

		String encodedStoredString = phoneticEngine.encode(storedString, Languages.LanguageSet.from(languageSet));

		return (soundex.difference(encodedInputString, encodedStoredString) + 1) * 20;
	}
}
