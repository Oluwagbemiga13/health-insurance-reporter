package cz.oluwagbemiga.eutax.pojo;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum InsuranceCompany {
    CPZP(List.of("CPZP","ČPZP"), "K", 11),
    OZP(List.of("OZP"), "L", 12),
    RBP(List.of("RBP","RPB"), "M", 13),
    VOZP(List.of("VOZP","V0ZP"), "N", 14),
    VZP(List.of("VZP"), "O", 15),
    ZP_SKODA(List.of("ZP Škoda", "ZPŠ", "ZPS", "ZP Skoda"), "P", 16),
    ZPMV(List.of("ZPMV","ZMVP"), "Q", 17);

    private final List<String> displayNames;
    private final String columnLetter;
    private final int columnIndex;

    InsuranceCompany(List<String> displayNames, String columnLetter, int columnIndex) {
        this.displayNames = displayNames;
        this.columnLetter = columnLetter;
        this.columnIndex = columnIndex;
    }

    @Override
    public String toString() {
        return displayNames.get(0);
    }

    public static Optional<InsuranceCompany> fromColumnLetter(String letter) {
        if (letter == null) return Optional.empty();
        String normalized = letter.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(e -> e.columnLetter.equals(normalized))
                .findFirst();
    }

    public static Optional<InsuranceCompany> fromColumnIndex(int index) {
        return Arrays.stream(values())
                .filter(e -> e.columnIndex == index)
                .findFirst();
    }

    public static Optional<InsuranceCompany> fromDisplayName(String name) {
        if (name == null) return Optional.empty();
        String trimmed = name.trim();
        return Arrays.stream(values())
                .filter(e -> e.displayNames.stream().anyMatch(d -> d.equalsIgnoreCase(trimmed)))
                .findFirst();
    }
}