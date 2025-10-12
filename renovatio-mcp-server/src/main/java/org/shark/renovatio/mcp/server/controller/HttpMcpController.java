package org.shark.renovatio.mcp.server.controller;

import org.shark.renovatio.mcp.server.model.McpRequest;
import org.shark.renovatio.mcp.server.service.McpProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP MCP Controller - handles MCP JSON-RPC 2.0 requests via HTTP transport.
 * This enables HTTP-based MCP clients to communicate with the server.
 */
@RestController
@RequestMapping(HttpMcpController.Paths.BASE)
public class HttpMcpController {

    // Constants for paths
    static final class Paths {
        private Paths() {}
        static final String BASE = "/mcp";
        static final String HEALTH = "/health";
    }

    // Constants for JSON-RPC structure
    static final class JsonRpc {
        private JsonRpc() {}
        static final String VERSION = "2.0";
        static final String FIELD_JSONRPC = "jsonrpc";
        static final String FIELD_ID = "id";
        static final String FIELD_METHOD = "method";
        static final String FIELD_PARAMS = "params";
        static final String FIELD_ERROR = "error";
        static final String FIELD_RESULT = "result";
    }

    // Constants for error handling
    static final class Errors {
        private Errors() {}
        static final int INTERNAL_ERROR_CODE = -32603; // JSON-RPC Internal error
        static final String INTERNAL_ERROR_PREFIX = "Internal error: ";
    }

    // Constants for health endpoint payload
    static final class Health {
        private Health() {}
        static final String STATUS_KEY = "status";
        static final String STATUS_UP = "UP";
        static final String SERVER_KEY = "server";
        static final String SERVER_NAME = "Renovatio MCP Server";
        static final String TIMESTAMP_KEY = "timestamp";
    }


    @Autowired
    private McpProtocolService mcpProtocolService;

    /**
     * Handle MCP JSON-RPC 2.0 requests via HTTP POST
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object handleMcpRequest(@RequestBody Map<String, Object> body) {
        try {
            // Convert HTTP request to MCP request format
            McpRequest mcpRequest = new McpRequest();
            mcpRequest.setJsonrpc((String) body.getOrDefault(JsonRpc.FIELD_JSONRPC, JsonRpc.VERSION));

            // Handle id field - can be String, Integer, or null according to JSON-RPC 2.0
            Object idObject = body.get(JsonRpc.FIELD_ID);
            String id = null;
            if (idObject != null) {
                id = idObject.toString(); // Convert Integer, String, or other types to String
            }
            mcpRequest.setId(id);

            mcpRequest.setMethod((String) body.get(JsonRpc.FIELD_METHOD));
            mcpRequest.setParams(body.get(JsonRpc.FIELD_PARAMS));

            // Process the request through the MCP protocol service
            var response = mcpProtocolService.handleMcpRequest(mcpRequest);

            // Convert MCP response to HTTP JSON response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put(JsonRpc.FIELD_JSONRPC, response.getJsonrpc());

            // Return id in the same format as received (preserve original type if possible)
            if (response.getId() != null && idObject instanceof Integer) {
                try {
                    jsonResponse.put(JsonRpc.FIELD_ID, Integer.valueOf(response.getId()));
                } catch (NumberFormatException e) {
                    jsonResponse.put(JsonRpc.FIELD_ID, response.getId()); // Fallback to string
                }
            } else {
                jsonResponse.put(JsonRpc.FIELD_ID, response.getId());
            }

            if (response.getError() != null) {
                jsonResponse.put(JsonRpc.FIELD_ERROR, response.getError());
            } else {
                jsonResponse.put(JsonRpc.FIELD_RESULT, response.getResult());
            }

            return jsonResponse;

        } catch (Exception e) {
            // Return JSON-RPC 2.0 error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(JsonRpc.FIELD_JSONRPC, JsonRpc.VERSION);

            // Handle id in error response as well
            Object idObject = body.get(JsonRpc.FIELD_ID);
            if (idObject instanceof Integer) {
                errorResponse.put(JsonRpc.FIELD_ID, idObject);
            } else if (idObject != null) {
                errorResponse.put(JsonRpc.FIELD_ID, idObject.toString());
            } else {
                errorResponse.put(JsonRpc.FIELD_ID, null);
            }

            Map<String, Object> error = new HashMap<>();
            error.put("code", Errors.INTERNAL_ERROR_CODE);
            error.put("message", Errors.INTERNAL_ERROR_PREFIX + e.getMessage());
            errorResponse.put(JsonRpc.FIELD_ERROR, error);

            return errorResponse;
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping(Paths.HEALTH)
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put(Health.STATUS_KEY, Health.STATUS_UP);
        health.put(Health.SERVER_KEY, Health.SERVER_NAME);
        health.put(Health.TIMESTAMP_KEY, System.currentTimeMillis());
        return health;
    }
}
