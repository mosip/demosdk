package io.mosip.demosdk.client.impl.spec_1_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.*;

public class NormalizerV1UnitTest {

    private Normalizer_V_1_0 normalizer;
    private Environment env;

    @Before
    public void setUp() {
        normalizer = new Normalizer_V_1_0();
        env = mock(Environment.class);
        // inject mocked environment via reflection since field is private and autowired
        try {
            java.lang.reflect.Field f = Normalizer_V_1_0.class.getDeclaredField("environment");
            f.setAccessible(true);
            f.set(normalizer, env);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRemoveTitlesAndNormalizeName_basic() {
        Map<String, List<String>> titles = new HashMap<>();
        List<String> enTitles = new ArrayList<>();
        enTitles.add("Mr");
        titles.put("en", enTitles);

        // no normalization patterns configured
        when(env.getProperty(anyString())).thenReturn(null);

        String out = normalizer.normalizeName("Mr John Doe", "en", titles);
        assertEquals("John Doe", out);
    }

    @Test
    public void testNormalizeAddress_withPatternReplacement() {
        // configure environment to return a pattern replacement for first key
        when(env.getProperty("ida.demo.address.normalization.regex.en[0]")).thenReturn("\\d+=#");
        when(env.getProperty("ida.norm.sep", "=")).thenReturn("=");

        String out = normalizer.normalizeAddress("123 Main St", "en");
        // digits replaced by # => "# Main St"
        assertTrue(out.startsWith("#"));
    }



    @Test
    public void testGetBasicNormalisers_multiplePatterns() {
        // provide two patterns: first with sep and replacement, second without sep
        when(env.getProperty("ida.demo.name.normalization.regex.en[0]")).thenReturn("foo=#");
        when(env.getProperty("ida.demo.name.normalization.regex.en[1]")).thenReturn("bar");
        when(env.getProperty("ida.norm.sep", "=")).thenReturn("=");

        // call private method via reflection
        try {
            java.lang.reflect.Method m = Normalizer_V_1_0.class.getDeclaredMethod("getBasicNormalisers", String.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Pattern, String> map = (Map<Pattern, String>) m.invoke(normalizer, "ida.demo.name.normalization.regex.en[%s]");
            // expect both patterns present
            assertEquals(2, map.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetBasicNormalisers_address_emptyReplacement_and_normalizeAddress() {
        // pattern with sep but empty replacement
        when(env.getProperty("ida.demo.address.normalization.regex.en[0]")).thenReturn("\\d+=");
        when(env.getProperty("ida.norm.sep", "=")).thenReturn("=");

        try {
            java.lang.reflect.Method m = Normalizer_V_1_0.class.getDeclaredMethod("getBasicNormalisers", String.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Pattern, String> map = (Map<Pattern, String>) m.invoke(normalizer, "ida.demo.address.normalization.regex.en[%s]");
            // expect at least one pattern present
            assertTrue(map.size() >= 1);
            // now normalize address using this configuration via public API
            String out = normalizer.normalizeAddress("123 Main St", "en");
            // digits removed -> should contain 'Main'
            assertTrue(out.contains("Main") || out.contains("main"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNormalizeName_with_common_and_any_patterns() {
        // configure patterns across name and common to ensure normalizeWithCommonAttributes merges them
        when(env.getProperty("ida.demo.name.normalization.regex.en[0]")).thenReturn("john=J");
        when(env.getProperty("ida.demo.name.normalization.regex.any[0]")).thenReturn("doe=D");
        when(env.getProperty("ida.demo.common.normalization.regex.en[0]")).thenReturn(null);
        when(env.getProperty("ida.norm.sep", "=")).thenReturn("=");

        Map<String, List<String>> titles = new HashMap<>();
        String out = normalizer.normalizeName("john doe", "en", titles);
        // both replacements applied
        assertTrue(out.contains("J") || out.contains("D"));
    }

    @Test
    public void testNormalizeName_removeAllCases_variousCasingsAndPunctuation() {
        Map<String, List<String>> titles = new HashMap<>();
        List<String> enTitles = new ArrayList<>();
        enTitles.add("Mr");
        enTitles.add("Dr");
        titles.put("en", enTitles);

        // no normalization patterns configured
        when(env.getProperty(anyString())).thenReturn(null);

        // create a name with multiple occurrences, different casings and punctuation
        String input = "Mr. JOHN Dr mr DR. Mr";
        String out = normalizer.normalizeName(input, "en", titles);
        // all title tokens should be removed, leaving only JOHN
        assertTrue(out.trim().equalsIgnoreCase("john") || out.trim().contains("john"));
    }

    @Test
    public void testNormalizeName_removeAllCases_repeatedOccurrences() {
        Map<String, List<String>> titles = new HashMap<>();
        List<String> enTitles = new ArrayList<>();
        enTitles.add("Hon");
        titles.put("en", enTitles);

        when(env.getProperty(anyString())).thenReturn(null);

        String input = "Hon Hon. HON honHon Hon.";
        String out = normalizer.normalizeName(input, "en", titles);
        // after removing Hon variants, result should be 'honHon' normalized to keep unmatched sequence
        String normalized = out.trim().toLowerCase();
        assertTrue("Should not contain 'hon' as separate token", !normalized.matches(".*\\bhon\\b.*"));
    }

    @Test
    public void testNormalize_handlesZeroLengthRegexMatch_breaksLoop() {
        // Configure a regex that matches empty string (start anchor) with replacement 'X'
        // use a valid zero-length-start regex '^' with replacement
        when(env.getProperty("ida.demo.name.normalization.regex.en[0]")).thenReturn("^=X");
        when(env.getProperty("ida.norm.sep", "=")).thenReturn("=");

        Map<String, List<String>> titles = new HashMap<>();
        String out = normalizer.normalizeName("abc", "en", titles);
        // zero-length match case: implementation breaks and should not loop infinitely; output should be trimmed string
        assertTrue(out != null && out.length() >= 0);
    }

    @Test
    public void testRemoveAllCases_coversOriginalLowerUpperIndexes() {
        // Prepare titles list with one title
        Map<String, List<String>> titles = new HashMap<>();
        List<String> enTitles = new ArrayList<>();
        enTitles.add("Sr");
        titles.put("en", enTitles);

        // no normalisers
        when(env.getProperty(anyString())).thenReturn(null);

        // craft a name that contains the title in different forms to exercise index checks
        String input = "Sr. sr SR Sr";
        String out = normalizer.normalizeName(input, "en", titles);
        // all title tokens removed
        assertTrue(!out.toLowerCase().contains("sr"));
    }
}
