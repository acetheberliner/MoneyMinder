package app;

import picocli.CommandLine;
import app.ui.CliCommand;

public final class App {
    public static void main(String[] args) {
        int exit = new CommandLine(new CliCommand()).execute(args);
        System.exit(exit);
    }
}
