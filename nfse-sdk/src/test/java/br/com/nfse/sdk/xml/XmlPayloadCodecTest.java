package br.com.nfse.sdk.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlPayloadCodecTest {

    @Test
    void shouldRoundTripXmlWithGzipBase64() {
        String xml = "<DPS><infDPS Id=\"DPS1\">Servico</infDPS></DPS>";

        String payload = XmlPayloadCodec.gzipBase64(xml);

        assertNotEquals(xml, payload);
        assertEquals(xml, XmlPayloadCodec.ungzipBase64(payload));
    }

    @Test
    void shouldRejectInvalidPayload() {
        assertThrows(XmlPayloadException.class, () -> XmlPayloadCodec.ungzipBase64("payload-invalido"));
    }
}
