package app;

import info.picocli.CommandLine;
import app.dao.JsonTransactionDao;
import app.service.TransactionService;
import app.ui.CliCommand;

import java.io.File;

public final class App {
    public static void main(String[] args) {
        var dao = new JsonTransactionDao(
                new File(System.getProperty("user.home"), ".money-minder.json"));
        new CommandLine(new CliCommand(new TransactionService(dao))).execute(args);
    }
}
