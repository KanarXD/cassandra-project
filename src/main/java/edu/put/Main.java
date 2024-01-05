package edu.put;

import edu.put.commands.InitCommand;
import edu.put.commands.RunCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(subcommands = {InitCommand.class, RunCommand.class})
public class Main implements Runnable {
    public static void main(String[] args) {
        var app = new CommandLine(new Main());
        var code = app.execute(args);
        System.exit(code);
    }

    @Override
    public void run() {
        log.info("Use `init` or `run` command to execute appropriate action.");
    }
}

