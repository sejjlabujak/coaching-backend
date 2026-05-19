package com.coaching_app.services;

import com.coaching_app.dto.PlayerDTO;
import com.coaching_app.models.Image;
import com.coaching_app.models.Player;
import com.coaching_app.repositories.ImageRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full sync flow:
 *  1. Scrape the current Eurobasket roster (once per import session).
 *  2. For each FIBA player name, find the best roster match.
 *  3. If matched, scrape the player's profile page for extra details.
 *  4. Save a new Player or update an existing one with all available fields.
 *
 * Name matching strategy
 * ──────────────────────
 * FIBA stores names as firstName + familyName separately.
 * Eurobasket stores "Firstname Lastname" in one string.
 * We normalise both to lowercase ASCII (strips diacritics) and try:
 *   a) Exact full-name match
 *   b) Both first + family name tokens present in the Eurobasket name
 *   c) Family name only — only when unique on the roster (safe fallback)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerSyncService {

    private final PlayersScraperService scraperService;
    private final PlayerRepository playerRepository;
    private final ImageRepository imageRepository;
    /**
     * Call this once per game import with the FIBA players who actually played.
     * Scrapes the live roster, matches each player, enriches via profile page,
     * then upserts the Player entity.
     */
    public void syncPlayersFromGame(List<FibaPlayerRef> fibaPlayers) {
        log.info("Starting player sync for {} FIBA players", fibaPlayers.size());

        List<PlayerDTO> roster = scraperService.scrapeRoster();
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

                // Enrich with birth date, city, weight from the profile page
                scraperService.scrapeProfile(match);

                upsertPlayer(fibaPlayer, match);

            } catch (Exception e) {
                log.warn("Error syncing player {} {}: {}",
                        fibaPlayer.firstName(), fibaPlayer.familyName(), e.getMessage());
            }
        }

        log.info("Player sync complete");
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    private PlayerDTO findBestMatch(FibaPlayerRef fiba,
                                              List<PlayerDTO> roster) {
        String fibaFirst  = normalize(fiba.firstName());
        String fibaFamily = normalize(fiba.familyName());
        String fibaFull   = fibaFirst + " " + fibaFamily;

        // a) Exact full-name match
        for (PlayerDTO dto : roster) {
            if (normalize(dto.getFullName()).equals(fibaFull)) {
                log.debug("Exact match: {}", dto.getFullName());
                return dto;
            }
        }

        // b) Both tokens present anywhere in the Eurobasket name
        for (PlayerDTO dto : roster) {
            String ebNorm = normalize(dto.getFullName());
            if (ebNorm.contains(fibaFirst) && ebNorm.contains(fibaFamily)) {
                log.debug("Token match: {} <-> {}", fibaFull, dto.getFullName());
                return dto;
            }
        }

        // c) Family name only — safe only when unique on the roster
        List<PlayerDTO> familyMatches = roster.stream()
                .filter(dto -> normalize(dto.getFullName()).contains(fibaFamily))
                .toList();
        if (familyMatches.size() == 1) {
            log.debug("Family-name fallback match: {} <-> {}",
                    fibaFamily, familyMatches.get(0).getFullName());
            return familyMatches.get(0);
        }

        return null;
    }

    // ── Upsert ────────────────────────────────────────────────────────────────
    @Transactional
    public void upsertPlayer(FibaPlayerRef fiba, PlayerDTO dto) {
        // Find existing player by firstName + lastName (case-insensitive, ASCII-normalised)
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

        // ── Core identity fields ───────────────────────────────────────────
        player.setFirstName(fiba.firstName());
        player.setLastName(fiba.familyName());

        // Jersey number from FIBA shirt number string
        if (fiba.shirtNumber() != null && !fiba.shirtNumber().isBlank()) {
            try {
                player.setJerseyNumber(Integer.parseInt(fiba.shirtNumber().trim()));
            } catch (NumberFormatException ignored) {
                log.debug("Could not parse shirt number: {}", fiba.shirtNumber());
            }
        }

        // ── Eurobasket profile fields — only overwrite if we have a value ──

        if (dto.getPosition() != null && !dto.getPosition().isBlank()) {
            player.setPosition(dto.getPosition());
        }

        if (dto.getHeightCm() != null && dto.getHeightCm() > 0) {
            player.setHeightCm(dto.getHeightCm());
        }

        if (dto.getWeightKg() != null && dto.getWeightKg() > 0) {
            player.setWeightKg(dto.getWeightKg());
        }

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            player.setBirthDate(dto.getBirthDate());
        }

        if (dto.getBirthCity() != null && !dto.getBirthCity().isBlank()) {
            player.setBirthCity(dto.getBirthCity());
        }

        if (dto.getNationality() != null && !dto.getNationality().isBlank()) {
            player.setNationality(dto.getNationality());
        }

        Player saved = playerRepository.save(player);
        log.info("{} player: {} {} (eurobasketId={}, height={}, birthDate={})",
                existing.isPresent() ? "Updated" : "Created",
                saved.getFirstName(), saved.getLastName(),
                saved.getPlayerID(), saved.getHeightCm(), saved.getBirthDate());

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
                log.debug("Saved image for player {}: {}", saved.getPlayerID(), dto.getImageUrl());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Lowercase + strip diacritics so "Omerbasić" matches "Omerbasic".
     */
    private String normalize(String input) {
        if (input == null) return "";
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    /**
     * Lightweight projection of a FIBA player — passed in from FibaParserService.
     */
    public record FibaPlayerRef(
            String firstName,
            String familyName,
            String shirtNumber
    ) {}
}