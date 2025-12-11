package io.mosip.demosdk.client.utils;

import static org.junit.Assert.*;

import org.apache.commons.codec.EncoderException;
import org.junit.Test;

public class TextMatcherUtilTest {

    @Test
    public void testPhoneticsMatchIdenticalStringsReturnsPositiveMultipleOf20() throws EncoderException {
        Integer v = TextMatcherUtil.phoneticsMatch("John", "John", "english");
        assertNotNull(v);
        assertTrue(v > 0);
        assertEquals(0, v.intValue() % 20);
        assertTrue(v >= 20 && v <= 100);
    }

    @Test
    public void testPhoneticsMatchDifferentStringsReturnsValueInExpectedRange() throws EncoderException {
        Integer v1 = TextMatcherUtil.phoneticsMatch("John", "John", "english");
        Integer v2 = TextMatcherUtil.phoneticsMatch("John", "Doe", "english");
        assertNotNull(v1);
        assertNotNull(v2);
        assertEquals(0, v1.intValue() % 20);
        assertEquals(0, v2.intValue() % 20);
        assertTrue(v1 >= 20 && v1 <= 100);
        assertTrue(v2 >= 20 && v2 <= 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPhoneticsMatchNullInputsThrows() throws EncoderException {
        TextMatcherUtil.phoneticsMatch(null, null, null);
    }
}

