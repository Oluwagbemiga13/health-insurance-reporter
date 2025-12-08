package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;

import java.util.List;

public interface SpreadsheetReader {

    List<Client> readClients(String filePath);
}
