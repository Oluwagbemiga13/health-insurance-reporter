package cz.oluwagbemiga.eutax.pojo;

import lombok.Getter;

@Getter
public enum CzechMonth {
    LEDEN("Leden"),
    UNOR("Únor"),
    BREZEN("Březen"),
    DUBEN("Duben"),
    KVETEN("Květen"),
    CERVEN("Červen"),
    CERVENEC("Červenec"),
    SRPEN("Srpen"),
    ZARI("Září"),
    RIJEN("Říjen"),
    LISTOPAD("Listopad"),
    PROSINEC("Prosinec");

    private final String czechName;

    CzechMonth(String czechName) {
        this.czechName = czechName;
    }

    @Override
    public String toString() {
        return czechName;
    }
}

