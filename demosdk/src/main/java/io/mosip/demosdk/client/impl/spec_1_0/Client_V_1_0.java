package io.mosip.demosdk.client.impl.spec_1_0;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.EncoderException;
import org.springframework.stereotype.Service;

import io.mosip.demosdk.client.utils.TextMatcherUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.demographics.spi.IDemoApi;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Reference implementation of the MOSIP {@link IDemoApi} demographic matching SPI
 * (specification version 1.0).
 *
 * <p>
 * ID-Authentication calls this bean to compare a demographic value supplied in an
 * authentication request (the <em>reference</em> value) against the value stored for the
 * identity (the <em>entity</em> value). Every match method returns an integer score in the
 * range {@code 0..100}, where {@value #EXACT_MATCH_VALUE} means a full match.
 * </p>
 *
 * <ul>
 * <li>{@link #doExactMatch} — whitespace-tokenised, case-insensitive, order-insensitive
 * equality of the two token lists.</li>
 * <li>{@link #doPartialMatch} — proportion of reference tokens found in the entity tokens,
 * with single-character reference tokens treated as initials.</li>
 * <li>{@link #doPhoneticsMatch} — BeiderMorse + Soundex similarity delegated to
 * {@link TextMatcherUtil#phoneticsMatch(String, String, String)}.</li>
 * </ul>
 *
 * <p>
 * The class is stateless and therefore thread-safe; a single instance may be shared.
 * </p>
 */
@Service
public class Client_V_1_0 implements IDemoApi {

	/** Regular expression used to split a demographic value into tokens (one or more whitespace characters). */
	private static final String SPLIT_REGEX = "\\s+";

	/** Score returned for a full match; also the upper bound of every match score. */
	public static final int EXACT_MATCH_VALUE = 100;

	/** MOSIP SLF4J-backed logger for this class. */
	private static final Logger mosipLogger = Logfactory.getSlf4jLogger(Client_V_1_0.class);

	/**
	 * Performs an exact, order-insensitive match.
	 *
	 * <p>
	 * Both values are lower-cased and split on whitespace. The score is
	 * {@value #EXACT_MATCH_VALUE} when both token lists have the same size and every entity
	 * token is present in the reference tokens; otherwise {@code 0}.
	 * </p>
	 *
	 * @param reqInfo    demographic value from the authentication request; must not be {@code null}
	 * @param entityInfo demographic value stored for the identity; must not be {@code null}
	 * @param flags      matching flags supplied by the caller (not used by this implementation)
	 * @return {@value #EXACT_MATCH_VALUE} on an exact match, otherwise {@code 0}
	 */
	@Override
	public int doExactMatch(String reqInfo, String entityInfo, Map<String, String> flags) {
		int matchvalue = 0;
		List<String> refInfoList = split(reqInfo);
		List<String> entityInfoList = split(entityInfo);

		if (refInfoList.size() == entityInfoList.size() && allMatch(refInfoList, entityInfoList)) {
			matchvalue = EXACT_MATCH_VALUE;
		}
		return matchvalue;
	}

	/**
	 * Performs a partial token match.
	 *
	 * <p>
	 * Both values are lower-cased and split on whitespace. Each reference token that is found
	 * in the entity tokens counts as matched and consumes that entity token. Afterwards, every
	 * unmatched single-character reference token (an initial) is removed from the unmatched set
	 * if some remaining entity token starts with that character. The score is:
	 * </p>
	 *
	 * <pre>
	 * matchedTokens * 100 / (entityTokenCount + unmatchedReferenceTokenCount)
	 * </pre>
	 *
	 * <p>
	 * Initials reduce the penalty in the denominator but are not counted as matched tokens.
	 * </p>
	 *
	 * @param reqInfo    demographic value from the authentication request; must not be {@code null}
	 * @param entityInfo demographic value stored for the identity; must contain at least one token
	 * @param flags      matching flags supplied by the caller (not used by this implementation)
	 * @return score in the range {@code 0..100}
	 */
	@Override
	public int doPartialMatch(String reqInfo, String entityInfo, Map<String, String> flags) {
		int matchvalue = 0;
		List<String> refInfoList = split(reqInfo);
		List<String> originalEntityInfoList = split(entityInfo);
		List<String> entityInfoList = Collections.synchronizedList(new ArrayList<>(originalEntityInfoList));
		List<String> matchedList = new ArrayList<>();
		List<String> unmatchedList = new ArrayList<>();
		refInfoList.forEach((String refInfo) -> {
			if (entityInfoList.contains(refInfo)) {
				matchedList.add(refInfo);
				entityInfoList.remove(refInfo);
			} else {
				unmatchedList.add(refInfo);
			}
		});
		new ArrayList<>(unmatchedList).stream().filter(str -> str.length() == 1).forEach((String s) -> {
			Optional<String> matchingWord = entityInfoList.stream().filter(str -> str.startsWith(s)).findAny();
			if (matchingWord.isPresent()) {
				entityInfoList.remove(matchingWord.get());
				unmatchedList.remove(s);
			}
		});
		matchvalue = matchedList.size() * EXACT_MATCH_VALUE / (originalEntityInfoList.size() + unmatchedList.size());
		return matchvalue;
	}

	/**
	 * Performs a phonetic match using BeiderMorse encoding followed by a Soundex difference.
	 *
	 * <p>
	 * Delegates to {@link TextMatcherUtil#phoneticsMatch(String, String, String)}. If the
	 * encoder fails, the error is logged and {@code 0} is returned. An unsupported
	 * {@code language} propagates the {@link IllegalArgumentException} from BeiderMorse.
	 * </p>
	 *
	 * @param reqInfo    demographic value from the authentication request
	 * @param entityInfo demographic value stored for the identity
	 * @param language   BeiderMorse language name (for example {@code "english"})
	 * @param flags      matching flags supplied by the caller (not used by this implementation)
	 * @return score in the range {@code 20..100}, or {@code 0} when encoding fails
	 */
	@Override
	public int doPhoneticsMatch(String reqInfo, String entityInfo, String language, Map<String, String> flags) {
		int value = 0;
		try {
			value = TextMatcherUtil.phoneticsMatch(reqInfo, entityInfo, language);
		} catch (EncoderException e) {
			mosipLogger.error("sessionId", "doPhoneticsMatch", "EncoderException", e.getMessage());
		}

		return value;
	}

	/**
	 * Lower-cases a value and splits it into non-empty whitespace-separated tokens.
	 *
	 * @param str value to tokenise; must not be {@code null}
	 * @return mutable list of lower-case tokens, empty when {@code str} is blank
	 */
	private static List<String> split(String str) {
		return Stream.of(str.toLowerCase().split(SPLIT_REGEX)).filter(s -> s.length() > 0).collect(Collectors.toList());
	}

	/**
	 * Checks whether every entity token is present in the reference tokens.
	 *
	 * @param refInfoList    reference (request) tokens
	 * @param entityInfoList entity (stored) tokens
	 * @return {@code true} when all entity tokens are contained in {@code refInfoList}
	 */
	private static boolean allMatch(List<String> refInfoList, List<String> entityInfoList) {
		return entityInfoList.parallelStream().allMatch(str -> refInfoList.contains(str));
	}

	/**
	 * Initialises the SDK. This implementation holds no state, so it only logs that the
	 * shared instance is being reused.
	 */
	@Override
	public void init() {
		mosipLogger.debug("DEMOGRAPHICS", "intiated", "GET SDK INSTANCE",
				"DEMO SDK instance reused ");
	}

}
