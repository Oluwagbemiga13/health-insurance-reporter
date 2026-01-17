package cz.oluwagbemiga.eutax.pojo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InsuranceCompanyTest {

    @Test
    void testFromDisplayName_VOZP() {
        Optional<InsuranceCompany> result = InsuranceCompany.fromDisplayName("VOZP");
        assertTrue(result.isPresent());
        assertEquals(InsuranceCompany.VOZP, result.get());
    }

    @Test
    void testFromDisplayName_V0ZP() {
        // V0ZP with zero should be recognized as VOZP
        Optional<InsuranceCompany> result = InsuranceCompany.fromDisplayName("V0ZP");
        assertTrue(result.isPresent());
        assertEquals(InsuranceCompany.VOZP, result.get());
    }

    @Test
    void testFromDisplayName_VoZP() {
        // VoZP should be recognized as VOZP
        Optional<InsuranceCompany> result = InsuranceCompany.fromDisplayName("VoZP");
        assertTrue(result.isPresent());
        assertEquals(InsuranceCompany.VOZP, result.get());
    }

    @Test
    void testFromDisplayName_CaseInsensitive() {
        Optional<InsuranceCompany> result = InsuranceCompany.fromDisplayName("vozp");
        assertTrue(result.isPresent());
        assertEquals(InsuranceCompany.VOZP, result.get());
    }

    @Test
    void testFromDisplayName_WithWhitespace() {
        Optional<InsuranceCompany> result = InsuranceCompany.fromDisplayName("  VOZP  ");
        assertTrue(result.isPresent());
        assertEquals(InsuranceCompany.VOZP, result.get());
    }
}

