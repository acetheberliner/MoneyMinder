package app.ui;

import info.picocli.CommandLine;                    // per new CommandLine(...)
import info.picocli.CommandLine.Command;            // <─ annotazione
import info.picocli.CommandLine.Option;             // <─ annotazione
import info.picocli.CommandLine.Parameters;         // <─ annotazione
import info.picocli.CommandLine.Spec;               // <─ annotazione
import info.picocli.CommandLine.CommandSpec;        // <─ tipo usato da @Spec

import app.model.*;                                 // i tuoi model
import app.service.TransactionService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.Callable;

@Command(name = "money",
         mixinStandardHelpOptions = true,
         version = "0.1",
         description = "Gestione finanze personali")
public final class CliCommand implements Callable<Integer> {

    @Spec CommandSpec spec;
    private final TransactionService service;

    public CliCommand(TransactionService service) { this.service = service; }

    @Command(name = "add", description = "Aggiunge una transazione")
    void add(@Option(names = "-d", required = true) LocalDate date,
             @Option(names = "-t", required = true) TxType type,
             @Option(names = "-c", required = true) Category cat,
             @Option(names = "-a", required = true) String amount,
             @Parameters String descr) {

        service.add(new Transaction(date, descr, cat, Money.of(amount), type));
        spec.commandLine().getOut().println("✅ OK");
    }

    @Command(name = "list")
    void list() { /* … */ }

    @Command(name = "report")
    void report(@Parameters String month) { /* … */ }

    @Override public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }
}
