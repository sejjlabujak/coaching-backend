//package com.coaching_app.controllers;
//
//import com.coaching_app.services.FibaWidgetScraperService;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/test")
//public class TestController {
//
//    private final FibaWidgetScraperService scraperService;
//
//    public TestController(FibaWidgetScraperService scraperService) {
//        this.scraperService = scraperService;
//    }
//
//    @GetMapping("/scrape")
//    public String scrape() {
//
//        scraperService.scrapeGames();
//
//        return "Scraping finished";
//    }
//}

package com.coaching_app.controllers;

import com.coaching_app.services.FibaWidgetScraperService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {

    private final FibaWidgetScraperService scraperService;

    // Test single game
    @GetMapping("/game/{id}")
    public ResponseEntity<?> getGame(@PathVariable int id) {
        JsonNode game = scraperService.fetchGame(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game.toString());
    }

    // Scan range for playoff-related keywords
    @GetMapping("/scan")
    public ResponseEntity<?> scan(
            @RequestParam(required = false, defaultValue = "2847000") int startId,
            @RequestParam(required = false, defaultValue = "2849000") int endId) {

        List<String> keywords = List.of(
                "nameInternational\":\"Playoff",
                "nameInternational\":\"Play Off"
        );
        List<Integer> ids = scraperService.findGameIdsByKeywordsInRange(startId, endId, keywords);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("startId", startId);
        response.put("endId", endId);
        response.put("keywords", keywords);
        response.put("found", ids.size());
        response.put("ids", ids);

        return ResponseEntity.ok(response);
    }

}