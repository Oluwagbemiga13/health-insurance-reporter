package cz.oluwagbemiga.eutax.pojo;

public record SpreadsheetSource(SpreadsheetSourceType type, String identifier) {

    public SpreadsheetSource {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        identifier = identifier == null ? "" : identifier.trim();
    }

    public boolean isGoogle() {
        return type == SpreadsheetSourceType.GOOGLE;
    }

    public boolean isExcel() {
        return type == SpreadsheetSourceType.EXCEL;
    }

    public enum SpreadsheetSourceType {
        EXCEL,
        GOOGLE
    }
}
