package com.coaching_app.controllers;

import com.coaching_app.dto.PlayerDTO;
import com.coaching_app.dto.PlayerResponseDTO;
import com.coaching_app.models.Player;
import com.coaching_app.models.User;
import com.coaching_app.repositories.PlayerRepository;
import com.coaching_app.services.PlayerSyncService;
import com.coaching_app.services.PlayersScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PlayersScraperService scraperService;
    private final PlayerSyncService playerSyncService;

    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> getAllPlayers() {
        User coachUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(
                playerRepository.findByUserId(coachUser.getId()).stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @GetMapping("/roster-preview")
    public ResponseEntity<List<PlayerDTO>> previewRoster() {
        return ResponseEntity.ok(scraperService.scrapeRoster());
    }

    @PostMapping("/sync-roster")
    public ResponseEntity<Map<String, Object>> syncRoster() {
        User coachUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        playerSyncService.syncRosterForCoach(coachUser);
        return ResponseEntity.ok(Map.of("status", "Done"));
    }

    private PlayerResponseDTO toDto(Player p) {
        List<PlayerResponseDTO.ImageDTO> images = p.getImages().stream()
                .map(img -> new PlayerResponseDTO.ImageDTO(img.getImageID(), img.getUrl()))
                .toList();

        List<PlayerResponseDTO.InjuryResponseDTO> injuries = p.getInjuries().stream()
                .map(inj -> new PlayerResponseDTO.InjuryResponseDTO(
                        inj.getId(),
                        inj.getDescription(),
                        inj.getStartDate() != null ? inj.getStartDate().toString() : null,
                        inj.getEndDate() != null ? inj.getEndDate().toString() : null,
                        inj.isActive()
                ))
                .toList();

        return new PlayerResponseDTO(
                p.getPlayerID(),
                p.getFirstName(),
                p.getLastName(),
                p.getPosition(),
                p.getJerseyNumber(),
                p.getHeightCm(),
                p.getWeightKg(),
                p.getBirthDate(),
                p.getBirthCity(),
                p.getNationality(),
                p.getAgeGroup() != null ? p.getAgeGroup().name() : null,
                images,
                injuries
        );
    }
}
