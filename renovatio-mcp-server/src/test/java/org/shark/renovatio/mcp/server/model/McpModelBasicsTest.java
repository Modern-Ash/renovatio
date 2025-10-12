package org.shark.renovatio.mcp.server.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpModelBasicsTest {

    private static Object read(Object target, String field) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void mcpRequest_defaults_and_toString() {
        McpRequest req = new McpRequest("42", "tools/list", Map.of("k", "v"));
        assertEquals("2.0", read(req, "jsonrpc"));
        assertEquals("42", read(req, "id"));
        assertEquals("tools/list", read(req, "method"));
        assertTrue(req.toString().contains("McpRequest{"));
    }

    @Test
    void mcpResponse_success_and_error_constructors() {
        McpResponse ok = new McpResponse("1", Map.of("x", 1));
        assertEquals("2.0", read(ok, "jsonrpc"));
        assertEquals("1", read(ok, "id"));
        assertEquals(1, ((Map<?, ?>) read(ok, "result")).get("x"));

        McpError err = new McpError(400, "bad", Map.of("why", "nope"));
        McpResponse ko = new McpResponse("2", err);
        assertSame(err, read(ko, "error"));
        assertEquals(400, (int) read(err, "code"));
        assertEquals("bad", read(err, "message"));
        assertEquals("nope", ((Map<?, ?>) read(err, "data")).get("why"));
    }

    @Test
    void mcpError_constructors_and_fields() {
        McpError e1 = new McpError();
        // set via reflection to avoid Lombok setters
        try {
            Field c = McpError.class.getDeclaredField("code"); c.setAccessible(true); c.set(e1, 500);
            Field m = McpError.class.getDeclaredField("message"); m.setAccessible(true); m.set(e1, "boom");
            Field d = McpError.class.getDeclaredField("data"); d.setAccessible(true); d.set(e1, List.of("d1"));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        assertEquals(500, (int) read(e1, "code"));
        assertEquals("boom", read(e1, "message"));
        assertEquals("d1", ((List<?>) read(e1, "data")).get(0));

        McpError e2 = new McpError(404, "nf");
        assertEquals(404, (int) read(e2, "code"));
        assertEquals("nf", read(e2, "message"));
    }

    @Test
    void mcpTool_constructors_and_metadata_copy() {
        McpTool t1 = new McpTool();
        assertNotNull(read(t1, "metadata"));
        assertTrue(((Map<?, ?>) read(t1, "metadata")).isEmpty());

        Map<String, Object> meta = Map.of("a", 1);
        McpTool t2 = new McpTool(
                "n", "d", Map.of("in", Map.of()), Map.of("out", Map.of()), List.of(), Map.of("ex", 1), meta
        );
        assertEquals("n", read(t2, "name"));
        assertEquals("d", read(t2, "description"));
        assertEquals(1, ((Map<?, ?>) read(t2, "metadata")).get("a"));

        // setMetadata should defensively copy/null-safe
        t2.setMetadata(null);
        assertNotNull(read(t2, "metadata"));
        assertTrue(((Map<?, ?>) read(t2, "metadata")).isEmpty());
    }

    @Test
    void mcpResource_constructor_fields() {
        McpResource r = new McpResource("uri:a", "A", "text/plain", "hello");
        assertEquals("uri:a", read(r, "uri"));
        assertEquals("A", read(r, "name"));
        assertEquals("text/plain", read(r, "mimeType"));
        assertEquals("hello", read(r, "text"));
    }

    @Test
    void mcpPrompt_outer_and_inner() {
        McpPrompt.Message msg = new McpPrompt.Message("user", "hi");
        assertEquals("user", read(msg, "role"));
        assertEquals("hi", read(msg, "content"));

        McpPrompt p = new McpPrompt("n", "d", List.of(msg));
        assertEquals("n", read(p, "name"));
        assertEquals("d", read(p, "description"));
        assertEquals(1, ((List<?>) read(p, "messages")).size());
    }

    @Test
    void textContent_defaults_and_overloads() {
        TextContent c1 = new TextContent("hello");
        assertEquals(TextContent.TEXT_TYPE, c1.type());
        assertEquals("hello", c1.text());

        TextContent c2 = new TextContent(null, null);
        assertEquals(TextContent.TEXT_TYPE, c2.type());
        assertEquals("", c2.text());

        TextContent c3 = new TextContent("md", "# Title");
        assertEquals("md", c3.type());
        assertEquals("# Title", c3.text());
    }

    @Test
    void toolCallResult_factories_and_jsonCreator_defaults() {
        ToolCallResult ok = ToolCallResult.ok("done", Map.of("n", 1));
        assertFalse(ok.isError());
        assertEquals("done", ok.content().get(0).text());
        assertEquals(1, ((Map<?, ?>) ok.structuredContent()).get("n"));

        ToolCallResult err = ToolCallResult.error("fail");
        assertTrue(err.isError());
        assertEquals("fail", err.content().get(0).text());
        assertNull(err.structuredContent());

        // JsonCreator path: null content should default to singleton empty TextContent
        ToolCallResult created = new ToolCallResult(null, null, false);
        assertNotNull(created.content());
        assertEquals(1, created.content().size());
        assertEquals("", created.content().get(0).text());
    }

    @Test
    void records_issue_change_and_metrics() {
        Issue issue = new Issue("A.java", 10, "WARN", "T", "msg", "r");
        assertEquals("A.java", issue.file());
        assertEquals(10, issue.line());
        assertEquals("WARN", issue.severity());
        assertEquals("T", issue.type());
        assertEquals("msg", issue.message());
        assertEquals("r", issue.recipe());

        Change ch = new Change("B.java", "diff");
        assertEquals("B.java", ch.file());
        assertEquals("diff", ch.diff());

        Metrics m = new Metrics(Map.of("x", 1));
        assertEquals(1, m.values().get("x"));
    }

    @Test
    void mcpCapabilities_defaults() {
        McpCapabilities caps = new McpCapabilities();
        assertNotNull(read(caps, "tools"));
        assertTrue((boolean) read(read(caps, "tools"), "listChanged"));
        assertNotNull(read(caps, "prompts"));
        assertTrue((boolean) read(read(caps, "prompts"), "listChanged"));
        assertNotNull(read(caps, "resources"));
        assertTrue((boolean) read(read(caps, "resources"), "listChanged"));
    }
}
