package CalenderApp.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> root() {
        return Map.of(
                "status", "ok",
                "message", "Backend is running. Use the UI at http://localhost:5173 and the API under /api.",
                "apiBase", "/api"
        );
    }

    @GetMapping(path = "/api/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ping() {
        return Map.of("status", "ok");
    }
}
