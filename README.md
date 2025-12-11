# 📄 Health Insurance Reporter

**Desktopová aplikace pro kontrolu kompletnosti přehledů pro zdravotní pojišťovny.**
Aplikace umožňuje porovnat seznam klientů uvedený v Excel tabulce (.xlsx) nebo Google Sheets s PDF přehledy vygenerovanými účetními programy **MONEY** a **POHODA**.
Cílem je rychle zjistit, zda jsou všechny požadované přehledy vytvořené, nebo zda některé chybí.

---

## 💡 Hlavní funkce

### ✔️ Import seznamu klientů

Aplikace načte Excel tabulku (.xlsx) nebo Google Sheets obsahující seznam klientů (název a IČO).

### ✔️ Kontrola PDF přehledů

Po výběru složky s PDF přehledy aplikace provede kontrolu podle názvů souborů:

- ověří, zda pro všechny klienty existuje odpovídající PDF přehled
- identifikuje chybějící přehledy
- upozorní na případné přebývající PDF soubory, které klientům z tabulky neodpovídají

### ✔️ Výsledky

Aplikace zobrazí:

- **nalezené klienty** ✔️
- **chybějící přehledy** ❌
- **nepárové PDF soubory**

---

## 🚀 Spuštění aplikace (Launch4j – stand-alone)

Aplikace se distribuuje jako **samostatný .exe soubor**.
Díky použití *Launch4j* není nutné, aby měl uživatel nainstalovanou Javu – JRE 17 je součástí balíčku.

### Jak spustit:

1. Stáhněte ZIP s aplikací
2. Rozbalte jej
3. Spusťte: `health-insurance-reporter.exe`
4. Aplikace se otevře v grafickém rozhraní (Java Swing)

---

## 📁 Vstupní data

### Excel tabulka (.xlsx)

Aplikace pracuje s Excel tabulkou ve formátu **.xlsx** (Microsoft Excel 2007+). 

**Podporovaná struktura:**
- **Sloupec A**: Název klienta
- **Sloupec B**: IČO klienta
- **Sloupec G**: Stav vygenerování přehledu (TRUE/FALSE)
- Listy pojmenované podle českých názvů měsíců (Leden, Únor, Březen, atd.)

### Google Sheets

Aplikace podporuje i přímé načítání dat z Google Sheets. Viz sekce [Nastavení Google Sheets](#-nastavení-google-sheets).

### PDF přehledy

Aplikace provádí párování klientů podle IČO v názvech PDF souborů.

**Podporované formáty názvů:**
- `PPPZ-02604477-2025-11_VZP_07.12.2025.pdf` (formát MONEY)
- `10751416_VZP_2025_11.pdf` (formát POHODA)

---

## 🔧 Technické informace

### Použité technologie

| Technologie | Verze | Popis |
|-------------|-------|-------|
| **Java** | 17 (Eclipse Temurin) | Programovací jazyk |
| **Maven** | 3.x | Build nástroj |
| **Apache POI** | 5.5.1 | Práce s Excel soubory |
| **Google Sheets API** | v4 | Integrace s Google Sheets |
| **Launch4j** | 2.4.1 | Vytvoření .exe souboru |
| **Logback** | 1.5.21 | Logování |
| **Lombok** | 1.18.28 | Redukce boilerplate kódu |

### Architektura

```
cz.oluwagbemiga.eutax
├── ApplicationLauncher.java      # Vstupní bod aplikace
├── pojo/                         # Datové objekty (Client, ParsedFileName, atd.)
├── security/                     # Správa klíčů a přihlašovacích údajů
│   ├── KeystoreCreator.java      # Vytváření PKCS12 keystore
│   └── SecretsRepository.java    # Načítání credentials z keystore
├── tools/                        # Pomocné třídy
│   ├── ExcelWorker.java          # Práce s Excel soubory
│   ├── GoogleWorker.java         # Práce s Google Sheets
│   ├── SpreadsheetWorker.java    # Rozhraní pro práci s tabulkami
│   ├── MatchEvaluator.java       # Párování klientů s PDF
│   └── IcoFromFiles.java         # Extrakce IČO z názvů souborů
└── ui/                           # Uživatelské rozhraní (Java Swing)
    ├── StartWindow.java          # Hlavní okno
    ├── LoginWindow.java          # Přihlašovací okno
    └── ResultsWindow.java        # Zobrazení výsledků
```

### Build

```bash
# Kompilace a vytvoření .exe
mvn clean package

# Výsledné soubory:
# - target/health-insurance-reporter-1.0-SNAPSHOT.jar
# - target/health-insurance-reporter.exe
```

---

## 🔑 Nastavení Google Sheets

Pro použití Google Sheets je nutné vytvořit Google Service Account a správně nastavit oprávnění.

### Krok 1: Vytvoření Google Cloud projektu

1. Přejděte na [Google Cloud Console](https://console.cloud.google.com/)
2. Vytvořte nový projekt nebo vyberte existující
3. V levém menu vyberte **APIs & Services** → **Enable APIs and Services**
4. Vyhledejte **Google Sheets API** a povolte jej

### Krok 2: Vytvoření Service Account

1. V [Google Cloud Console](https://console.cloud.google.com/) přejděte na **IAM & Admin** → **Service Accounts**
2. Klikněte na **Create Service Account**
3. Vyplňte:
   - **Service account name**: např. `health-insurance-reporter`
   - **Service account ID**: vyplní se automaticky
4. Klikněte na **Create and Continue**
5. Přeskočte sekci **Grant this service account access** (není potřeba)
6. Klikněte na **Done**

### Krok 3: Vygenerování JSON klíče

1. V seznamu Service Accounts klikněte na vytvořený účet
2. Přejděte na záložku **Keys**
3. Klikněte na **Add Key** → **Create new key**
4. Vyberte formát **JSON**
5. Klikněte na **Create**
6. Soubor se automaticky stáhne – **uložte jej na bezpečné místo!**

### Krok 4: Sdílení Google Sheets tabulky

**Důležité:** Service Account musí mít přístup k tabulce, kterou chcete používat.

1. Otevřete JSON soubor a najděte hodnotu `client_email`, např.:
   ```
   "client_email": "health-insurance-reporter@your-project.iam.gserviceaccount.com"
   ```
2. Otevřete vaši Google Sheets tabulku
3. Klikněte na tlačítko **Sdílet** (Share)
4. Vložte e-mailovou adresu Service Account
5. Nastavte oprávnění:
    - **Editor**: aplikace musí aktualizovat sloupec G (stav vygenerování)
6. Klikněte na **Odeslat** (Send)

### Krok 5: Import klíče do aplikace

1. Spusťte aplikaci Health Insurance Reporter
2. Při prvním spuštění (nebo přes menu) se zobrazí průvodce vytvoření keystore
3. Vyberte stažený JSON soubor
4. Nastavte heslo pro zabezpečení klíče
5. Aplikace vytvoří soubor `secrets.p12` vedle .exe souboru

> ⚠️ **Bezpečnostní upozornění:** JSON klíč má plný přístup k Service Account. Po importu do aplikace jej můžete smazat nebo bezpečně archivovat.

### Struktura Google Sheets tabulky

Tabulka musí mít stejnou strukturu jako Excel soubor:

| Sloupec | Obsah |
|---------|-------|
| A | Název klienta |
| B | IČO klienta |
| G | Stav vygenerování (TRUE/FALSE) |

Listy musí být pojmenované podle českých názvů měsíců: `Leden`, `Únor`, `Březen`, `Duben`, `Květen`, `Červen`, `Červenec`, `Srpen`, `Září`, `Říjen`, `Listopad`, `Prosinec`.

---

## 📝 Licence

Copyright © 2025 Daniel Rakovsky
