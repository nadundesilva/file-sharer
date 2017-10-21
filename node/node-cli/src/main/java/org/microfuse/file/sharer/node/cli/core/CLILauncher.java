package org.microfuse.file.sharer.node.cli.core;

import com.google.common.io.Files;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.FileSharer;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.QueryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command Line Interface Launcher class.
 */
public class CLILauncher {
    private static final Logger logger = LoggerFactory.getLogger(CLILauncher.class);

    private static final FileSharer fileSharer = new FileSharer();

    private static final String EMPTY_COMMAND = "";
    private static final String HELP_COMMAND = "help";
    private static final String EXIT_COMMAND = "exit";
    private static final String QUERY_COMMAND = "query";
    private static final String QUERY_COMMAND_START = "start";
    private static final String QUERY_COMMAND_START_FILE = "start-file";
    private static final String QUERY_COMMAND_PRINT = "print";
    private static final String LOAD_COMMAND = "load";
    private static final String LOAD_COMMAND_RANDOM = "random";

    public static void main(String[] args) {
        Console console = System.console();
        if (console != null) {
            try (PrintWriter writer = console.writer()) {
                LogManager.getRootLogger().setLevel(Level.OFF);
                writer.println("File Sharer\n");

                fileSharer.start();
                while (true) {
                    writer.print("> ");
                    writer.flush();
                    String input = console.readLine();

                    String[] command = input.split(" ");
                    switch (command[0]) {
                        case EMPTY_COMMAND:
                            break;
                        case HELP_COMMAND:
                            if (command.length == 1) {
                                help(console);
                            } else {
                                printUnknownParametersError(console, HELP_COMMAND, command);
                            }
                            break;
                        case EXIT_COMMAND:
                            if (command.length == 1) {
                                System.exit(0);
                            } else {
                                printUnknownParametersError(console, EXIT_COMMAND, command);
                            }
                            break;
                        case QUERY_COMMAND:
                            if (command.length == 3) {
                                query(console, command[1], command[2]);
                            } else {
                                printUnknownParametersError(console, QUERY_COMMAND, command);
                            }
                            break;
                        case LOAD_COMMAND:
                            if (command.length == 2) {
                                load(console, command[1]);
                            } else {
                                printUnknownParametersError(console, LOAD_COMMAND, command);
                            }
                            break;
                        default:
                            writer.println("Unknown command " + command[0]);
                    }
                }
            }
        } else {
            logger.error("Cannot run the CLI since this is not executed in the console.");
        }
    }

    /**
     * Print help message on the console.
     *
     * @param console The console object
     */
    private static void help(Console console) {
        try (PrintWriter writer = console.writer()) {
            writer.println("The following commands can be used\n\n" +
                    HELP_COMMAND + "\t\t\t\t\tShow a list of commands that can be used\n" +
                    EXIT_COMMAND + "\t\t\t\t\tExit the file sharer\n" +
                    QUERY_COMMAND + " <" + QUERY_COMMAND_START + "|" + QUERY_COMMAND_START_FILE + "|" +
                    QUERY_COMMAND_PRINT + ">" + "\t\tRun a query\n" +
                    "\t" + QUERY_COMMAND_START + " <query-string>\t\tStart a query for the <query-string>.\n" +
                    "\t" + QUERY_COMMAND_START_FILE + " \t\t\tStart a query for the query strings from file.\n" +
                    "\t" + QUERY_COMMAND_PRINT + " <query-string>\t\tPrint the results for the query string.\n" +
                    LOAD_COMMAND + " " + LOAD_COMMAND_RANDOM + "\t\t\t\tLoad a list of resources from file");
        }
    }

    /**
     * Run a query or print the results of a query.
     *
     * @param console The console object
     * @param subCommand The sub command under query command
     * @param queryString The query string to be used in the query
     */
    private static void query(Console console, String subCommand, String queryString) {
        try (PrintWriter writer = console.writer()) {
            QueryManager queryManager = fileSharer.getServiceHolder().getQueryManager();
            switch (subCommand) {
                case QUERY_COMMAND_START:
                    queryManager.query(queryString);
                    break;
                case QUERY_COMMAND_START_FILE:
                    writer.print("Enter file name :");
                    writer.flush();
                    String filePath = "";
                    while (Objects.equals(filePath, "")) {
                        filePath = console.readLine();
                    }
                    File file = new File(filePath);


                    try {
                        List<String> fileLines = Files.readLines(file, Constants.DEFAULT_CHARSET);
                        for (String fileLine : fileLines) {
                            queryManager.query(fileLine);
                        }
                    } catch (IOException e) {
                        writer.println("Invalid file");
                    }
                    break;
                case QUERY_COMMAND_PRINT:
                    List<AggregatedResource> queryResults = queryManager.getQueryResults(queryString);
                    if (queryResults.size() > 0) {
                        writer.println("Resource Name\tNodes");
                        for (AggregatedResource aggregatedResource : queryResults) {
                            writer.print(aggregatedResource.getName() + "\t");

                            List<Node> nodes = new ArrayList<>(aggregatedResource.getAllNodes());
                            for (int i = 0; i < nodes.size(); i++) {
                                if (i != 0) {
                                    writer.print(", ");
                                }
                                writer.println(nodes.get(i));
                            }
                        }
                    } else {
                        writer.println("No matches found yet for query " + queryString);
                    }
                    break;
                default:
                    writer.println("Unknown argument " + subCommand + " to command " + QUERY_COMMAND);
            }
        }
    }

    /**
     * Load a list of resources from a file.
     *
     * @param console The console object
     * @param subCommand The sub command of the load command
     */
    private static void load(Console console, String subCommand) {
        try (PrintWriter writer = console.writer()) {
            switch (subCommand) {
                case LOAD_COMMAND_RANDOM:
                    writer.print("Enter file name :");
                    writer.flush();
                    String filePath = "";
                    while (Objects.equals(filePath, "")) {
                        filePath = console.readLine();
                    }
                    File file = new File(filePath);

                    try {
                        List<String> fileLines = Files.readLines(file, Constants.DEFAULT_CHARSET);
                        for (String fileLine : fileLines) {
                            fileSharer.getServiceHolder().getResourceIndex()
                                    .addResourceToIndex(new OwnedResource(fileLine));
                        }
                    } catch (IOException e) {
                        writer.println("Invalid file");
                    }
                    break;
                default:
                    writer.println("Unknown argument " + subCommand + " to command " + QUERY_COMMAND);
            }
        }
    }

    /**
     * Print error message indicating that the parameters are invalid.
     *
     * @param console The console object
     * @param command The command
     * @param commandParams The command parameters
     */
    private static void printUnknownParametersError(Console console, String command, String[] commandParams) {
        try (PrintWriter writer = console.writer()) {
            if (commandParams.length >= 1) {
                String[] parameters = new String[commandParams.length - 1];
                System.arraycopy(commandParams, 1, parameters, 0, parameters.length);
                writer.println("Unknown command parameters " + String.join(" ", parameters)
                        + " for command " + commandParams[0]);
            } else {
                writer.println("Unknown command parameters for command " + command);
            }
            writer.println("Use \"help\" to get the list of available commands.");
        }
    }
}
