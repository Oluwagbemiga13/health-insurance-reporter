package cz.oluwagbemiga.eutax.pojo;

import java.time.LocalDate;

public record ParsedFileName(String ico, LocalDate date, InsuranceCompany insuranceCompany, String filePath,
                             boolean invalidDirectory, String parentDirName) {
    // Backward-compatible constructor for existing tests and callers that don't provide dir info
    public ParsedFileName(String ico, LocalDate date, InsuranceCompany insuranceCompany, String filePath) {
        this(ico, date, insuranceCompany, filePath, false, "");
    }
}
