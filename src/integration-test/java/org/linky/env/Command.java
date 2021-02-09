package org.linky.env;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Command {

    private static final String JAVA_BINARY = String.format("%s/bin/java", System.getProperty("java.home"));
    private static final String CLASSPATH = Path.of("build/libs/*").toAbsolutePath().toString();
    private static final String MAIN_CLASS = "org.linky.cli.Main";

    @NonNull
    private final Path workingDirectory;
    @NonNull
    private final Map<String, String> systemProperties;

    public Execution run(String[] args, long timeout) {
        List<String> command = command(systemProperties, args);
        try {
            Process process = new ProcessBuilder()
                    .directory(workingDirectory.toFile())
                    .command(command)
                    .start();
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                fail(commandFailureMessage("Command did not finish in time", command));
            }
            return Execution.of(process);
        } catch (InterruptedException | IOException e) {
            Thread.interrupted();
            fail(commandFailureMessage("Command execution failed with: " + e.getMessage(), command));
            throw new IllegalStateException("unreachable");
        }
    }

    private List<String> command(Map<String, String> systemProperties, String[] args) {
        List<String> command = new ArrayList<>();
        command.add(JAVA_BINARY);
        systemProperties.forEach((key, value) -> command.add(String.format("-D%s=%s", key, value)));
        command.add("-cp");
        command.add(CLASSPATH);
        command.add(MAIN_CLASS);
        command.addAll(Arrays.asList(args));
        return command;
    }

    private String commandFailureMessage(String message, List<String> command) {
        return String.format(
                "%s%n\tworking directory:%n\t\t%s%n\tCommand:%n\t\t%s",
                message, workingDirectory, command);
    }
}
