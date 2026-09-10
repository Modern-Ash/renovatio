package org.shark.renovatio.generated.cobol;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * Generated controller that forwards REST calls to CICS transactions.
 */
@RestController
@RequestMapping("/api/cics")
public class Cics001CicsController {

    private final CicsTransactionGateway cicsService;

    public Cics001CicsController(CicsTransactionGateway cicsService) {
        this.cicsService = cicsService;
    }

    /** Target-side port implemented by the selected CICS adapter. */
    public interface CicsTransactionGateway {
        String invokeTransaction(String command, Map<String, Object> payload);
    }

    @PostMapping("/read")
    public ResponseEntity<String> read(@RequestBody Map<String, Object> payload) {
        String result = cicsService.invokeTransaction("READ", payload);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/return")
    public ResponseEntity<String> return_(@RequestBody Map<String, Object> payload) {
        String result = cicsService.invokeTransaction("RETURN", payload);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/receive")
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        String result = cicsService.invokeTransaction("RECEIVE", payload);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody Map<String, Object> payload) {
        String result = cicsService.invokeTransaction("SEND", payload);
        return ResponseEntity.ok(result);
    }
}
