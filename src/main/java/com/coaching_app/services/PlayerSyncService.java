package com.coaching_app.services;

import com.coaching_app.dto.PlayerDTO;
import com.coaching_app.models.Image;
import com.coaching_app.models.Player;
import com.coaching_app.models.User;
import com.coaching_app.repositories.ImageRepository;
import com.coaching_app.repositories.PlayerStatsRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerSyncService {

    private final PlayersScraperService scraperService;
    private final PlayerRepository playerRepository;
    private final ImageRepository imageRepository;
    private final PlayerStatsRepository individualStatRepository;


    public void syncPlayersFromGame(List<FibaPlayerRef> fibaPlayers) {
        syncPlayersFromGame(fibaPlayers, null);
    }

    //Syncs players from FibaParserService.importGame
    public void syncPlayersFromGame(List<FibaPlayerRef> fibaPlayers, User coachUser) {
        log.info("Starting player sync for {} FIBA players", fibaPlayers.size());

        String rosterUrl = (coachUser != null && coachUser.getTeam() != null)
                ? coachUser.getTeam().getRosterUrl()
                : null;
        List<PlayerDTO> roster = rosterUrl != null
                ? scraperService.scrapeRoster(rosterUrl)
                : scraperService.scrapeRoster();

        if (roster.isEmpty()) {
            log.warn("Eurobasket roster is empty — skipping player sync");
            return;
        }

        for (FibaPlayerRef fibaPlayer : fibaPlayers) {
            try {
                PlayerDTO match = findBestMatch(fibaPlayer, roster);
                if (match == null) {
                    log.debug("No Eurobasket match for FIBA player: {} {}",
                            fibaPlayer.firstName(), fibaPlayer.familyName());
                    continue;
                }

                scraperService.scrapeProfile(match);
                upsertPlayer(fibaPlayer, match, coachUser);

            } catch (Exception e) {
                log.warn("Error syncing player {} {}: {}",
                        fibaPlayer.firstName(), fibaPlayer.familyName(), e.getMessage());
            }
        }

        log.info("Player sync complete");
    }

    //Triggered when coaches create account
    @Transactional
    public void syncRosterForCoach(User coachUser) {
        if (coachUser == null || coachUser.getTeam() == null || coachUser.getTeam().getRosterUrl() == null) {
            log.warn("Cannot sync roster: coach or coach's team is not set");
            return;
        }

        List<PlayerDTO> roster = scraperService.scrapeRoster(coachUser.getTeam().getRosterUrl());
        List<FibaPlayerRef> refs = roster.stream()
                .filter(dto -> dto.getFullName() != null && dto.getFullName().contains(" "))
                .map(dto -> {
                    String[] parts = dto.getFullName().trim().split("\\s+", 2);
                    return new FibaPlayerRef(parts[0], parts[1], null);
                })
                .toList();

        syncPlayersFromGame(refs, coachUser);
    }

    // ── Matching ──────────────────────────────────────────────────────────────
    private PlayerDTO findBestMatch(FibaPlayerRef fiba, List<PlayerDTO> roster) {
        String fibaFirst  = normalize(fiba.firstName());
        String fibaFamily = normalize(fiba.familyName());
        String fibaFull   = fibaFirst + " " + fibaFamily;

        for (PlayerDTO dto : roster) {
            if (normalize(dto.getFullName()).equals(fibaFull)) return dto;
        }

        for (PlayerDTO dto : roster) {
            String ebNorm = normalize(dto.getFullName());
            if (ebNorm.contains(fibaFirst) && ebNorm.contains(fibaFamily)) return dto;
        }

        List<PlayerDTO> familyMatches = roster.stream()
                .filter(dto -> normalize(dto.getFullName()).contains(fibaFamily))
                .toList();
        if (familyMatches.size() == 1) return familyMatches.get(0);

        return null;
    }

    // ── Upsert ────────────────────────────────────────────────────────────────
    @Transactional
    public void upsertPlayer(FibaPlayerRef fiba, PlayerDTO dto, User coachUser) {
        Optional<Player> existing = Optional.empty();

        if (dto.getPlayerID() != null) {
            existing = playerRepository.findById(Long.valueOf(dto.getPlayerID()));
        }

        if (existing.isEmpty()) {
            existing = playerRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                    fiba.firstName(), fiba.familyName()
            );
        }

        Player player = existing.orElseGet(Player::new);

        player.setFirstName(fiba.firstName());
        player.setLastName(fiba.familyName());

        if (coachUser != null) {
            player.setUser(coachUser);
        }

        if (fiba.shirtNumber() != null && !fiba.shirtNumber().isBlank()) {
            try {
                player.setJerseyNumber(Integer.parseInt(fiba.shirtNumber().trim()));
            } catch (NumberFormatException ignored) {}
        }

        if (dto.getPosition()    != null && !dto.getPosition().isBlank())    player.setPosition(dto.getPosition());
        if (dto.getHeightCm()    != null && dto.getHeightCm() > 0)           player.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg()    != null && dto.getWeightKg() > 0)           player.setWeightKg(dto.getWeightKg());
        if (dto.getBirthDate()   != null && !dto.getBirthDate().isBlank())   player.setBirthDate(dto.getBirthDate());
        if (dto.getBirthCity()   != null && !dto.getBirthCity().isBlank())   player.setBirthCity(dto.getBirthCity());
        if (dto.getNationality() != null && !dto.getNationality().isBlank()) player.setNationality(dto.getNationality());

        Player saved = playerRepository.save(player);
        log.info("{} player: {} {} (height={}, birthDate={})",
                existing.isPresent() ? "Updated" : "Created",
                saved.getFirstName(), saved.getLastName(),
                saved.getHeightCm(), saved.getBirthDate());

        // ── Link IndividualPerformance rows to this player ─────────────────
        int linked = individualStatRepository.linkPlayerByName(
                fiba.firstName(), fiba.familyName(), saved
        );
        if (linked > 0) {
            log.debug("Linked {} IndividualPerformance rows to player {}", linked, saved.getPlayerID());
        }

        // ── Persist image if not already saved ────────────────────────────
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            boolean alreadyHasImage = imageRepository
                    .findByPlayerPlayerID(saved.getPlayerID())
                    .stream()
                    .anyMatch(img -> dto.getImageUrl().equals(img.getUrl()));

            if (!alreadyHasImage) {
                Image image = new Image();
                image.setUrl(dto.getImageUrl());
                image.setPlayer(saved);
                imageRepository.save(image);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
    public record FibaPlayerRef(
            String firstName,
            String familyName,
            String shirtNumber
    ) {}
}
