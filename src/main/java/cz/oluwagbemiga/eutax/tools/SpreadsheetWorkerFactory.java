package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource;
import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource.SpreadsheetSourceType;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory class for creating {@link SpreadsheetWorker} instances based on the spreadsheet source type.
 * <p>
 * This factory encapsulates the logic for determining which implementation of {@link SpreadsheetWorker}
 * to use based on whether the source is a local Excel file or a Google Sheets document.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SpreadsheetSource source = new SpreadsheetSource("path/to/file.xlsx", SpreadsheetSourceType.EXCEL);
 * SpreadsheetWorker worker = SpreadsheetWorkerFactory.create(source);
 * List<Client> clients = worker.readClients(source.path(), CzechMonth.JANUARY);
 * }</pre>
 *
 * @see SpreadsheetWorker
 * @see ExcelWorker
 * @see GoogleWorker
 * @see SpreadsheetSource
 */
@Slf4j
public final class SpreadsheetWorkerFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException always thrown to prevent instantiation
     */
    private SpreadsheetWorkerFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new {@link SpreadsheetWorker} instance appropriate for the given source type.
     * <p>
     * Returns a {@link GoogleWorker} for {@link SpreadsheetSourceType#GOOGLE} sources,
     * and an {@link ExcelWorker} for all other source types.
     * </p>
     *
     * @param source the spreadsheet source configuration containing the type information
     * @return a new {@link SpreadsheetWorker} instance matching the source type
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public static SpreadsheetWorker create(SpreadsheetSource source) {
        if (source == null) {
            log.error("Cannot create SpreadsheetWorker: source is null");
            throw new IllegalArgumentException("Spreadsheet source must not be null");
        }
        SpreadsheetSourceType type = source.type();
        log.debug("Creating SpreadsheetWorker for source type: {}", type);
        SpreadsheetWorker worker = type == SpreadsheetSourceType.GOOGLE ? new GoogleWorker() : new ExcelWorker();
        log.debug("Created {} for source type: {}", worker.getClass().getSimpleName(), type);
        return worker;
    }
}

