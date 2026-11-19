package org.egov.dx.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UtilitiesTest {

    @Test
    void testGetStringFromInputStream() throws Exception {
        String original = "line1\nline2\nline3";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(original.getBytes(StandardCharsets.UTF_8));

        String result = Utilities.getStringFromInputStream(inputStream);
        assertEquals("line1line2line3", result);
    }

    @Test
    void testConvertStringToDocumentAndGetXpathValue() throws Exception {
        String xml = "<EsignResp status=\"1\" txn=\"TXN123456\"><Name>Citizen</Name></EsignResp>";

        Document doc = Utilities.convertStringToDocument(xml);
        assertNotNull(doc);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String status = Utilities.GetXpathValue(xPath, "/EsignResp/@status", doc);
        String txn = Utilities.GetXpathValue(xPath, "/EsignResp/@txn", doc);
        String name = Utilities.GetXpathValue(xPath, "/EsignResp/Name/text()", doc);

        assertEquals("1", status);
        assertEquals("TXN123456", txn);
        assertEquals("Citizen", name);
    }

    @Test
    void testConvertStringToDocument_RejectsXXEDoctype() {
        String maliciousXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>\n" +
                "<foo>&xxe;</foo>";

        assertThrows(Exception.class, () -> Utilities.convertStringToDocument(maliciousXml));
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Utilities instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
