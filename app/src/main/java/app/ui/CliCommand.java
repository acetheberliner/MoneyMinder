package app.ui;

import app.dao.JsonTransactionDao;
import app.model.*;
import app.service.TransactionService;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;               // ✅ import esplicito
import java.util.concurrent.Callable;

@Command(
        name = "money",
        mixinStandardHelpOptions = true,
        version = "MoneyMinder CLI 1.0",
        description = "Gestione finanze personali da riga di comando",
        subcommands = {CliCommand.Add.class, CliCommand.ListCmd.class, CliCommand.Report.class}
)
public class CliCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    /* service unico, condiviso da tutti i sub-command */
    static final TransactionService SERVICE = new TransactionService(
            new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));

    @Override public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }

    /* ---------------------- add ---------------------- */
    @Command(name = "add", description = "Aggiunge una nuova transazione")
    static class Add implements Callable<Integer> {

        @Option(names = "-d", required = true) LocalDate date;
        @Option(names = "-t", required = true) TxType type;
        @Option(names = "-c", required = true) Category category;
        @Option(names = "-a", required = true) String amount;
        @Parameters(index = "0..*", description = "Descrizione") List<String> descr;

        @Override public Integer call() {
            SERVICE.add(new Transaction(
                    date,
                    String.join(" ", descr),
                    category,
                    Money.of(amount.replace(',', '.')),
                    type));
            System.out.println("✅ Transazione salvata.");
            return 0;
        }
    }

    /* ---------------------- list --------------------- */
    @Command(name = "list", description = "Mostra tutte le transazioni")
    static class ListCmd implements Callable<Integer> {
        @Override public Integer call() {
            SERVICE.list().forEach(System.out::println);
            return 0;
        }
    }

    /* --------------------- report -------------------- */
    @Command(name = "report", description = "Report mensile (yyyy-MM, default mese corrente)")
    static class Report implements Callable<Integer> {

        @Parameters(index = "0", arity = "0..1") String month;

        @Override public Integer call() {
            YearMonth ym = month == null ? YearMonth.now() : YearMonth.parse(month);
            var rep = SERVICE.monthlyReport(ym);

            System.out.printf("%n=== Report %s ===%n", ym);
            System.out.printf(" Entrate : %s%n Uscite  : %s%n Saldo   : %s%n",
                    rep.income(), rep.expense(), rep.balance());
            System.out.println(" Uscite per categoria:");
            rep.byCategory().forEach((k, v) ->
                    System.out.printf("   %-15s %s%n", k, v));
            return 0;
        }
    }
}
