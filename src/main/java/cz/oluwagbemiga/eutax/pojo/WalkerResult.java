package cz.oluwagbemiga.eutax.pojo;

import java.util.List;

public record WalkerResult(
        List<Report> reports,
        List<ErrorReport> errorReports
) {
}
