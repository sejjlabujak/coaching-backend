package com.coaching_app.controllers;

import com.coaching_app.services.FibaWidgetScraperService;
import com.coaching_app.services.GameScanService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final FibaWidgetScraperService scraperService;
    private final GameScanService gameScanService;

    @GetMapping("/game/{id}")
    public ResponseEntity<?> getGame(@PathVariable int id) {
        JsonNode game = scraperService.fetchGame(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game.toString());
    }

    @GetMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(
            @RequestParam(required = false, defaultValue = "2818000") int startId,
            @RequestParam(required = false, defaultValue = "2820000") int endId) throws Exception {
        return ResponseEntity.ok(gameScanService.scan(startId, endId));
    }
}
