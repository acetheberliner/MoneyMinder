# MoneyMinder – Piano di lavoro

### Deliverable
- [ ] Fat-jar `money-minder-all.jar`  
- [ ] Tag `v0.1-cli-json`

## Sprint 3 – Opzionali **(scegli ordine)**
- [ ] Budget per categoria (observer)  
- [ ] Import/Export CSV  
- [ ] Statistiche grafiche (JavaFX Pie/Bar)  
- [ ] SQLite backend (SqliteDao)  
- [ ] Report annuale  
- [ ] Multivaluta (ExchangeRateProvider)

-------------------------------------

- Categorie personalizzate
    - Aggiungi pulsante “+ Categoria” nel dialogo (o menu).
    - Mantieni elenco in un JSON a parte (categories.json) e ricaricalo all’avvio.

- Report mensile completo
    - Campo DatePicker (“mese”) sopra i grafici → al cambio mese ricalcoli.
    - Label saldo (entrate - uscite) sotto i grafici.

- Aggiornamento grafici in tempo reale
    - Collega un ListChangeListener alla ObservableList<Transaction> del table (vedi sezione D).

- UI moderna (TabPane) – vedi sezione C.

- Optional che vuoi tenere (budget, CSV, multivaluta…)
    - Scelta minima: Budget per categoria (mostra progress bar + alert rosso).
    - CSV import/export puoi rinviare se manca tempo.