package org.shark.renovatio.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "serve", description = "Start the Renovatio MCP server (delegates to the existing server entry points).")
public final class ServeCommand implements Callable<Integer> {

    @Option(names = "--stdio", description = "Start in stdio mode (default).")
    boolean stdio;

    @Option(names = "--http", description = "Start in HTTP mode.")
    boolean http;

    @Parameters(description = "Passthrough arguments for the MCP server.")
    List<String> passthrough = List.of();

    @Override
    public Integer call() {
        String[] args = passthrough.toArray(new String[0]);
        try {
            if (http) {
                System.err.println("[renovatio] Starting MCP HTTP server...");
                Class<?> clazz = Class.forName("org.shark.renovatio.mcp.server.McpServerApplication");
                Method main = clazz.getMethod("main", String[].class);
                main.invoke(null, (Object) args);
            } else {
                System.err.println("[renovatio] Starting MCP stdio server...");
                Class<?> clazz = Class.forName("org.shark.renovatio.mcp.server.McpStdioServerApplication");
                Method main = clazz.getMethod("main", String[].class);
                main.invoke(null, (Object) args);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("error: failed to start MCP server: " + e.getMessage());
            return 1;
        }
    }
}
