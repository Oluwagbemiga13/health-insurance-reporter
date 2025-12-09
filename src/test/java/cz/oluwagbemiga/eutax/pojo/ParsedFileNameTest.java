package cz.oluwagbemiga.eutax.pojo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ParsedFileNameTest {

    @Test
    void componentsExposeValues() {
        ParsedFileName parsed = new ParsedFileName("12345678", LocalDate.of(2024, 11, 1));

        assertEquals("12345678", parsed.ico());
        assertEquals(LocalDate.of(2024, 11, 1), parsed.date());
    }

    @Test
    void equalsAndHashCodeUseAllComponents() {
        ParsedFileName first = new ParsedFileName("12345678", LocalDate.of(2024, 11, 1));
        ParsedFileName same = new ParsedFileName("12345678", LocalDate.of(2024, 11, 1));
        ParsedFileName differentIco = new ParsedFileName("11111111", LocalDate.of(2024, 11, 1));
        ParsedFileName differentDate = new ParsedFileName("12345678", LocalDate.of(2024, 10, 1));

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, differentIco);
        assertNotEquals(first, differentDate);
    }

    @Test
    void toStringContainsComponentValues() {
        ParsedFileName parsed = new ParsedFileName("12345678", LocalDate.of(2024, 11, 1));

        String rendered = parsed.toString();

        assertTrue(rendered.contains("12345678"));
        assertTrue(rendered.contains("2024-11-01"));
    }
}

