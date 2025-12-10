package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface SpreadsheetWorker {

    List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException;

    void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) throws IOException;

}
