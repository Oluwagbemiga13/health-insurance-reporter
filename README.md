# 📄 Health Insurance Reporter

**Desktopová aplikace pro kontrolu kompletnosti přehledů pro zdravotní pojišťovny.**
Aplikace umožňuje porovnat seznam klientů uvedený v Excel/LibreOffice tabulce s PDF přehledy vygenerovanými účetními programy **MONEY** a **POHODA**.
Cílem je rychle zjistit, zda jsou všechny požadované přehledy vytvořené, nebo zda některé chybí.

---

## 💡 Hlavní funkce

### ✔️ Import seznamu klientů

Aplikace načte Excel nebo LibreOffice tabulku obsahující seznam klientů (název a ičo).

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

Výstup bude možné v budoucnu exportovat (např. CSV).

---

## 🚀 Spuštění aplikace (Launch4j – stand-alone)

Aplikace se distribuuje jako **samostatný .exe soubor**.
Díky použití *Launch4j* není nutné, aby měl uživatel nainstalovanou Javu.

### Jak spustit:

1. Stáhněte ZIP s aplikací
2. Rozbalte jej
3. Spusťte: HealthInsuranceReporter.exe
4. Aplikace se otevře v grafickém rozhraní (Java Swing)

---

## 📁 Vstupní data

### Excel / LibreOffice tabulka

Aplikace pracuje s tabulkou, která obsahuje seznam klientů.
Přesná struktura bude upřesněna (viz sekce *Open Questions*).

### PDF přehledy

Aplikace provádí párování klientů podle názvů PDF souborů.
Potřebujeme znát přesnou jmennou konvenci (viz níže).

---

# ❓ Otevřené otázky ?

Níže jsou otázky, které je třeba vyjasnit, aby bylo možné dokončit implementaci aplikace:

---

### 1️⃣ Nastavení aplikace

**Má být součástí aplikace možnost nastavení výchozích adresářů?**Například:

- root adresář pro výběr PDF přehledů
- výchozí umístění Excel/LibreOffice tabulek
- ukládání těchto cest do konfiguračního souboru
- cokoliv dalšího?

### 2️⃣ Historie předchozích kontrol

**Má aplikace uchovávat seznam výsledků předchozích kontrol?**

Možné scénáře:

- zobrazit přehled posledních kontrol
- zobrazovat dříve chybějící nebo nalezené klienty

---

### 3️⃣ Struktura Excel / LibreOffice souboru

**Jak přesně vypadá tabulka, kterou aplikace bude zpracovávat?**

Potřebujeme znát:

- názvy sloupců
- který sloupec obsahuje jméno klienta
- zda je jméno unikátní
- zda existují další identifikátory (IČO, rodné číslo apod.)

---

### 4️⃣ Jmenná konvence PDF přehledů

**Jak jsou pojmenované PDF soubory z MONEY / POHODA?**

Např.:

- obsahují cokoliv jiného mimo názvu klienta a iča?
- obsahují celé jméno klienta?
- používají jednotný formát, nebo se může lišit?

Tato informace je zásadní pro implementaci spolehlivého párování.
