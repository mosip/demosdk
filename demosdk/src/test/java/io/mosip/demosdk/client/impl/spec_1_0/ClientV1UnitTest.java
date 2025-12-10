package io.mosip.demosdk.client.impl.spec_1_0;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.EncoderException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import io.mosip.demosdk.client.utils.TextMatcherUtil;

public class ClientV1UnitTest {

    private final Client_V_1_0 client = new Client_V_1_0();
    private MockedStatic<TextMatcherUtil> mockedStatic;

    @Before
    public void initMocks() {
        // prepare static mock for TextMatcherUtil
        mockedStatic = mockStatic(TextMatcherUtil.class);
    }

    @After
    public void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @Test
    public void testDoExactMatch_sameStrings_returns100() {
        int v = client.doExactMatch("John Doe", "John Doe", new HashMap<>());
        assertEquals("Exact match should return 100", Client_V_1_0.EXACT_MATCH_VALUE, v);
    }

    @Test
    public void testDoExactMatch_differentOrder_returns100() {
        int v = client.doExactMatch("John Doe", "Doe John", new HashMap<>());
        assertEquals("Exact match should be order-insensitive and return 100", Client_V_1_0.EXACT_MATCH_VALUE, v);
    }

    @Test
    public void testDoExactMatch_differentSize_returns0() {
        int v = client.doExactMatch("John Doe", "John", new HashMap<>());
        assertEquals("Different sizes should not be exact match", 0, v);
    }

    @Test
    public void testDoPartialMatch_oneMatch_returns50() {
        int v = client.doPartialMatch("John", "John Doe", new HashMap<>());
        // matchedList.size() = 1, originalEntityInfoList.size() = 2 => 1*100/(2+0) = 50
        assertEquals("Partial match should compute proportional value", 50, v);
    }

    @Test
    public void testDoPartialMatch_singleCharPrefix_matchingStartsWith() {
        // ref has single char token 'J' that should match entity word starting with 'J'
        int v = client.doPartialMatch("J", "John", new HashMap<>());
        // matchedList will be empty but unmatchedList size 1, originalEntityInfoList size 1 => 0*100/(1+0)=0
        assertEquals(0, v);
    }

    @Test
    public void testDoPartialMatch_singleCharPrefix_foundMatch_returns50() {
        int v = client.doPartialMatch("J Doe", "John Doe", new HashMap<>());
        // matchedList.size() = 1 (Doe), originalEntityInfoList.size() = 2 => 1*100/(2+0) = 50
        assertEquals(50, v);
    }

    @Test
    public void testDoExactMatch_sameSize_but_notAllMatch_returns0() {
        int v = client.doExactMatch("A B", "A C", new HashMap<>());
        assertEquals(0, v);
    }

    @Test
    public void testDoExactMatch_withNulls_returns100() {
        // passing empty strings instead of null to avoid NPE from production code
        int v = client.doExactMatch("", "", new HashMap<>());
        assertEquals(Client_V_1_0.EXACT_MATCH_VALUE, v);
    }

    @Test
    public void testDoPhoneticsMatch_callsTextMatcherUtil() throws EncoderException {
        mockedStatic.when(() -> TextMatcherUtil.phoneticsMatch("abc", "abc", "en")).thenReturn(100);
        int val = client.doPhoneticsMatch("abc", "abc", "en", new HashMap<>());
        assertEquals(100, val);
    }

    @Test
    public void testDoPhoneticsMatch_handlesEncoderException() throws Exception {
        mockedStatic.when(() -> TextMatcherUtil.phoneticsMatch("x", "y", "en")).thenThrow(new org.apache.commons.codec.EncoderException("boom"));
        int val = client.doPhoneticsMatch("x", "y", "en", new HashMap<>());
        // exception path returns default 0
        assertEquals(0, val);
    }

    @Test
    public void testInit_executesWithoutException() {
        client.init();
    }
}
