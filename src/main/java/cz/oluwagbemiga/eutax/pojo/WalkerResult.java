package cz.oluwagbemiga.eutax.pojo;

import java.util.List;

public record WalkerResult(
        List<ParsedFileName> parsedFileNames,
        List<ErrorReport> errorReports
) {
}
