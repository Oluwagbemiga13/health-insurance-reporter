package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;

import java.io.FileNotFoundException;
import java.util.List;

public interface SpreadsheetReader {

    List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException;
}
