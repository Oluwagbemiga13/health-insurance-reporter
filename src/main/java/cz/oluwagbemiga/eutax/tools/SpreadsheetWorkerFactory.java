package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource;
import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource.SpreadsheetSourceType;

public final class SpreadsheetWorkerFactory {

    private SpreadsheetWorkerFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static SpreadsheetWorker create(SpreadsheetSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Spreadsheet source must not be null");
        }
        SpreadsheetSourceType type = source.type();
        return type == SpreadsheetSourceType.GOOGLE ? new GoogleWorker() : new ExcelWorker();
    }
}

