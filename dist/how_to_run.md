>MoneyMinder non include i moduli JavaFX nel JAR. Per questo occorre specificare *--module-path* e *--add-modules* all’avvio

---

### Esecuzione con doppio click (Windows, file .BAT)

- Fai doppio click su **MoneyMinder.bat**

### Esecuzione da terminale (Windows CMD o PowerShell)

- Vai nella cartella **dist** che contiene **MoneyMinder.jar** e lancia il comando:

```bash
java --module-path "C:\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml -jar MoneyMinder.jar
```

>Se JavaFX è situato in un’altra cartella, sostituire il percorso dopo *--module-path*

---

### Dove vengono salvati i dati
I file di dati sono salvati nella home dell’utente:

- **Movimenti**: ~/.money-minder.json
- **Budget**: ~/.money-minder-budgets.json

Su Windows: *C:\Users\<NOME_UTENTE>\.money-minder.json* e *C:\Users\<NOME_UTENTE>\.money-minder-budgets.json*
e anche in */dist/data*