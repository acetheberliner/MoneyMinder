>*Progettazione e Sviluppo del Software – Relazione di progetto*  
>
>**Studente**: Tommaso Bagnolini – Matricola 0001071116 – <tommaso.bagnolini@studio.unibo.it><br>
>**Gruppo**: progetto svolto interamente in forma individuale

# MoneyMinder

## 1. Analisi

MoneyMinder è un’applicazione desktop multipiattaforma concepita per semplificare la gestione delle finanze personali.  
L’utente può registrare movimenti economici, analizzarli tramite report mensili / annuali, impostare dei budget, visualizzare grafici dinamici e convertire importi in più valute.  
L’app salva automaticamente i dati in locale, garantendone la persistenza fra sessioni.

---

### 1.1 Requisiti

#### Funzionalità minime (obbligatorie)

- **Registrazione movimenti** – entrate / uscite con data, importo, descrizione.  
- **Categorizzazione** – associazione a categoria predefinita o creata dall’utente.  
- **Consultazione cronologia** – lista completa, ordinata per data.  
- **Report mensile** – totale entrate, totale uscite, saldo, ripartizione per categoria.  
- **Salvataggio permanente** – i movimenti rimangono fra sessioni.

#### Funzionalità opzionali

- **Budget per categoria** – tetto mensile e alert al superamento.  
- **Import / Export** – esportazione Excel (import pianificato a versioni future).  
- **Statistiche grafiche** – pie-chart entrate / uscite; andamento saldo.  
- **Gestione multivaluta** – conversione automatica verso EUR.  
- **Report annuale** – confronto saldo fra mesi.

> Tutte le funzionalità sopra elencate sono implementate, eccetto l’import di dati esterni.

---

### 1.2 Analisi del dominio

MoneyMinder ruota attorno al concetto di **movimento finanziario**. Ogni movimento possiede:
- data
- importo (in EUR, o convertito)
- categoria
- descrizione
- tipologia (entrata / uscita)

Su tali informazioni si appoggiano budget, report mensili e report annuali.

```mermaid
classDiagram
    direction TB
    class Transaction {
        +LocalDate date
        +TxType type
        +Money amount
        +String category
        +String description
    }
    class Category { +String name }
    class Money { +BigDecimal value }
    class Budget { +String category +Money limit }
    class MonthlyReport {
        +YearMonth month
        +Money in
        +Money out
        +Money balance
    }
    class AnnualReport {
        +Year year
        +Money in
        +Money out
        +Money balance
    }

    Transaction --> Category
    Budget --> Category
    MonthlyReport --> "*" Category
    AnnualReport --> MonthlyReport
```

>Figura 1 – modello di dominio (solo entità, nessun dettaglio tecnico).

## 2. Design

### 2.1 Architettura

MoneyMinder segue il pattern MVC con tre layer principali:

```mermaid
flowchart LR
    %% VIEW
    V["JavaFX&nbsp;GUI"] -->|eventi| C(MainController)

    %% MODEL & SERVICE
    subgraph "Model&nbsp;&nbsp;/&nbsp;&nbsp;Service"
        TS(TransactionService)
        BS(BudgetService)
        CC(CurrencyConverter)
        DAO[(JsonTransactionDao)]
    end

    C --> TS
    C --> BS
    C --> CC
    TS --> DAO
```

>Figura 2 – interazioni architetturali essenziali

Ogni componente svolge non più di due ruoli:

| Componente             | Ruolo (≤ 3)                       | Dialoghi principali                    |
| ---------------------- | --------------------------------- | -------------------------------------- |
| **JavaFX GUI**         | boundary                          | binding tabelle / grafici ⇄ controller |
| **MainController**     | control                           | orchestration fra view e servizi       |
| **TransactionService** | domain-logic + persistence-facade | CRUD, aggregazioni, delega a DAO       |
| **BudgetService**      | policy                            | soglie per categoria, alert            |
| **CurrencyConverter**  | util                              | strategy-map tassi → EUR               |
| **JsonTransactionDao** | persistence                       | (de)serializzazione JSON su disco      |

---

### 2.2 Design dettagliato

>Ogni paragrafo segue lo schema **Problema** ▸ **Soluzione** ▸ **Pattern** ▸ **UML**.

#### 2.2.1 Filtri dinamici & grafici

- **Problema**: Aggiornare in tempo reale tabella e pie-chart al variare di filtri o nuovi movimenti.

- **Soluzione**: master (ObservableList) → FilteredList (predicate), SortedList → TableView.
Il controller rigenera i grafici ascoltando le variazioni della lista.

```mermaid
sequenceDiagram
    participant UI
    participant Controller
    participant List as master(ObservableList)
    UI->>Controller: modifica filtro
    Controller->>List: setPredicate(…)
    List-->>Controller: ListChangeEvent
    Controller->>UI: refresh charts & table
```

>Pattern Observer fra lista e controller; predicate-λ come Strategy.

---

#### 2.2.2 Budget alert

- **Problema**: Notificare l’utente appena la spesa mensile di una categoria eccede il limite.

- **Soluzione**: checkBudget(tx) somma le uscite del mese nella categoria e interroga BudgetService.

```mermaid
classDiagram
    class MainController
    class BudgetService
    MainController --> BudgetService
```

>Pattern Observer (evento nuovo movimento) + Strategy (somma parametrica su YearMonth).

---

#### 2.2.3 Color-hash categorie custom

- **Problema**: Assegnare un colore stabile e leggibile a ogni categoria creata dall’utente.

- **Soluzione**: Funzione Color.hsb(hash%360, 0.55, 0.75) calcolata on-the-fly; cache in dynColor.

```java
return dynColor.computeIfAbsent(name, k -> {
    Color c = Color.hsb((k.hashCode() & 0xffff)%360, 0.55, 0.75);

    return String.format("#%02x%02x%02x",
        (int)(c.getRed()*255), 
        (int)(c.getGreen()*255),
        (int)(c.getBlue()*255));
});
```

>Pattern Strategy per la scelta del colore; nessuna persistenza necessaria.

---

#### 2.2.4 Export Excel

- **Problema**: Produrre un file .xlsx dei movimenti filtrati senza esporre POI alla GUI.

- **Soluzione**: onExportXlsx() funge da facade su Apache POI: workbook, formato valuta, autosize.

```mermaid
sequenceDiagram
    participant Controller
    participant POI as «Apache POI»
    Controller->>POI: create workbook / sheet
    Controller->>POI: write rows & styles
    Controller->>POI: wb.write(file)
```

## 3. Sviluppo

### 3.1 Testing automatizzato

Una suite JUnit 5 - eseguita con ```./gradlew test``` – copre tutta la logica di dominio (la UI sarà in futuro verificata con TestFX).

| Test class               | Casi verificati (10 passati)                                      |
| ------------------------ | ----------------------------------------------------------------- |
| `TransactionServiceTest` | CRUD, lista immutabile, report mensile (4 test)                   |
| `BudgetServiceTest`      | Persistenza JSON, rimozione soglia 0, immutabilità mappa (3 test) |
| `CurrencyConverterTest`  | Identità EUR, tassi USD/GBP, fallback valuta sconosciuta (3 test) |

---

```bash
> Task :test

BudgetServiceTest > putAndGet_shouldPersistBetweenInstances() PASSED

BudgetServiceTest > put_zeroOrNullShouldRemoveEntry() PASSED

BudgetServiceTest > all_shouldBeUnmodifiable() PASSED

CurrencyConverterTest > toEur_shouldReturnIdentityForEur() PASSED

CurrencyConverterTest > unknownCurrencyFallsBackToIdentity() PASSED

CurrencyConverterTest > usdAndGbpRatesAreApplied() PASSED

TransactionServiceTest > replace_shouldSubstituteElement() PASSED

TransactionServiceTest > add_shouldPersistAndReturnInList() PASSED

TransactionServiceTest > list_shouldBeUnmodifiable() PASSED

TransactionServiceTest > monthlyReport_shouldAggregateCorrectly() PASSED
```

>Branch-coverage del pacchetto app.service ≈ 80 %; sarà incrementata estendendo i test alla UI con TestFX.

---

### 3.2 Note di sviluppo

Elenco 5 feature avanzate che ho implementato:


## 4. Commenti finali

### 4.1 Autovalutazione

| Area           | Punti di forza                       | Da migliorare                  |
| -------------- | ------------------------------------ | ------------------------------ |
| Architettura   | pattern chiari, alta coesione        | servizi ulteriormente modulari |
| UI / UX        | pie-chart intuitivi, filtri reattivi | dark-mode, localizzazione      |
| Qualità codice | naming coerente, Javadoc essenziale  | test automatizzati GUI         |

---

### 4.2 Lavori futuri

- Import CSV / Excel per completare l’opzionale O-02.
- Distribuzione runtime-image con jlink (Java + JavaFX embedded).
- Backup cloud (sincronizzazione JSON crittografato).

### 4.3 Difficoltà riscontrate

- Packaging JavaFX: risolto con script di lancio --module-path.

---

## 5 Guida utente

>Prerequisito – Java 17 e JavaFX SDK 21.0.8.

1. Download – MoneyMinder.jar.
2. Avvio (Windows):

```bash
java --module-path "C:\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml -jar MoneyMinder.jar
```

3. Visualizza lo storico delle transazioni
4. Dialog "**Aggiungi**" – Inserisci:
    - Data transazione
    - Tipologia (entrata/uscita)
    - Categoria (standard/custom)
    - Importo
    - Valuta (EUR/USD/GBP)
    - Descrizione
e infine conferma con "**OK**"

5. Dialog "**Budget**" – Imposta limiti mensili e ricevi alert al loro superamento.

6. Esegui ricerce di transazioni filtrando per:
    - Mese
    - Descrizione
    - Categoria
    - Tipologia

7. Cliccando prima su una transazione e poi sul rispettivo *button*, effettua operazioni CRUD di:
    - Modifica
    - Cancellazione

8. Dialog "**Report**" – Visualizza un riepilogo mensile con suddivisione del totale per categorie.

9. Bottone "**Esporta**" – Salva i movimenti del mese corrente in formato Excel "*.xlsx*".

10. Dialog "**Andamento**" - Visualizza grafici di andamento del saldo, con focus:
    - Giornaliero
    - Mensile
    - Annuale
  