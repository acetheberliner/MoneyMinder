# MoneyMinder – Piano di lavoro

## Legenda
- [ ] task aperto
- [x] task completato
- ➡️ dipende da…

---

## Sprint 0 – Bootstrap ✓
- [x] Repo GitHub creata (`pss23-24-money-minder-bagnolini`)
- [x] Gradle wrapper 8.x / JDK 17
- [x] Workflow CI `gradle.yml`
- [x] Struttura cartelle + file base
- [x] Commit iniziale / tag `v0.0.0`

## Sprint 1 – Funzionalità MINIME
### Model & DAO
- [ ] Definire `Money` VO (BigDecimal + currency)  
- [ ] Completare `JsonTransactionDao`  
- [ ] JUnit test DAO

### Service
- [ ] Implementare `TransactionService`   ➡️ DAO  
- [ ] Metodo `monthlyReport(YearMonth)`  
- [ ] Test report calcoli

### CLI
- [ ] Comando `add`  
- [ ] Comando `list`  
- [ ] Comando `report`  
- [ ] Help/usage (`--help`)

### Persistence
- [ ] Path default `~/.money-minder.json`  
- [ ] Autoload + autosave su edit

### Deliverable
- [ ] Fat-jar `money-minder-all.jar`  
- [ ] Tag `v0.1-cli-json`

## Sprint 2 – Hardening
- [ ] Validazioni input (date future, importi negativi)  
- [ ] RoundingMode & scale fissi  
- [ ] Logging slf4j-simple

## Sprint 3 – Opzionali **(scegli ordine)**
- [ ] Budget per categoria (observer)  
- [ ] Import/Export CSV  
- [ ] Statistiche grafiche (JavaFX Pie/Bar)  
- [ ] SQLite backend (SqliteDao)  
- [ ] Report annuale  
- [ ] Multivaluta (ExchangeRateProvider)

## Documentazione
- [ ] Aggiornare README – build, usage, licenza  
- [ ] Relazione: Analisi (DONE), Design diagrammi, Testing, Note sviluppo  
- [ ] Mermaid UML dominio + architettura

## Consegna finale
- [ ] Fat-jar allegato in repo  
- [ ] Post sul Forum con URL repo  
- [ ] Tag `v1.0-release`
