package cz.oluwagbemiga.eutax.pojo;

import java.util.List;

public record Client(String name,
                     String ico,
                     boolean reportGenerated,
                     List<InsuranceCompany> insuranceCompanies) {
}
