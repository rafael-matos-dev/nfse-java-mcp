package br.com.nfse.sdk.xml.dps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DpsIdGeneratorTest {

    @Test
    void shouldGenerateDpsIdForCnpj() {
        String id = DpsIdGenerator.generate("19.441.457/0001-60", "3129806", "70000", 24);

        assertEquals("DPS312980621944145700016070000000000000000024", id);
        assertEquals(45, id.length());
    }

    @Test
    void shouldRejectInvalidDocument() {
        assertThrows(IllegalArgumentException.class, () -> DpsIdGenerator.generate("123", "3129806", "1", 1));
    }
}
