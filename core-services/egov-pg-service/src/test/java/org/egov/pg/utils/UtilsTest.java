package org.egov.pg.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void formatAmtAsRupeeAndPaise() {
        assertEquals("100.00", Utils.formatAmtAsRupee("100"));
        assertEquals("10000", Utils.formatAmtAsPaise("100"));
    }

    @Test
    public void convertPaiseToRupee() {
        assertEquals("100.50", Utils.convertPaiseToRupee("10050"));
    }

    @Test
    public void convertObjectToString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = Utils.convertObjectToString(mapper, Collections.singletonMap("k", "v"));
        assertEquals("{\"k\":\"v\"}", json);
    }
}
