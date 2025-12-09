package cz.oluwagbemiga.eutax.pojo;

import lombok.Getter;

@Getter
public enum CzechMonth {
    LEDEN("Leden", 1),
    UNOR("Únor", 2),
    BREZEN("Březen", 3),
    DUBEN("Duben", 4),
    KVETEN("Květen", 5),
    CERVEN("Červen", 6),
    CERVENEC("Červenec", 7),
    SRPEN("Srpen", 8),
    ZARI("Září", 9),
    RIJEN("Říjen", 10),
    LISTOPAD("Listopad", 11),
    PROSINEC("Prosinec", 12);

    private final String czechName;
    private final int monthNumber;

    CzechMonth(String czechName, int monthNumber) {
        this.czechName = czechName;
        this.monthNumber = monthNumber;
    }

    @Override
    public String toString() {
        return czechName;
    }
}
