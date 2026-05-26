package com.coaching_app.services;

import com.coaching_app.dto.InjuryDTO;
import com.coaching_app.models.Injury;
import com.coaching_app.models.Player;
import com.coaching_app.repositories.InjuryRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InjuryService {

    private final InjuryRepository injuryRepository;
    private final PlayerRepository playerRepository;

    // ── Get all injuries for a player ─────────────────────────────────────────

    public List<InjuryDTO> getInjuriesForPlayer(Long playerId) {
        return injuryRepository.findByPlayerPlayerID(playerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Get only active injuries for a player ─────────────────────────────────

    public List<InjuryDTO> getActiveInjuriesForPlayer(Long playerId) {
        return injuryRepository.findByPlayerPlayerIDAndIsActiveTrue(playerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Get all active injuries across all players ────────────────────────────

    public List<InjuryDTO> getAllActiveInjuries() {
        return injuryRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Create a new injury ───────────────────────────────────────────────────

    public InjuryDTO createInjury(Long playerId, InjuryDTO dto) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));

        Injury injury = new Injury();
        injury.setDescription(dto.getDescription());
        injury.setStartDate(dto.getStartDate() != null
                ? LocalDate.parse(dto.getStartDate())
                : LocalDate.now());
        injury.setIsActive(true);
        injury.setPlayer(player);

        return toDto(injuryRepository.save(injury));
    }

    // ── Update description ────────────────────────────────────────────────────

    public InjuryDTO updateInjury(Long injuryId, InjuryDTO dto) {
        Injury injury = findById(injuryId);

        if (dto.getDescription() != null) {
            injury.setDescription(dto.getDescription());
        }

        // setIsActive handles endDate automatically via the model method
        injury.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        return toDto(injuryRepository.save(injury));
    }

    // ── Close injury (set inactive, record endDate) ───────────────────────────

    public InjuryDTO closeInjury(Long injuryId) {
        Injury injury = findById(injuryId);
        injury.setIsActive(false); // endDate set automatically in model
        return toDto(injuryRepository.save(injury));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteInjury(Long injuryId) {
        injuryRepository.delete(findById(injuryId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Injury findById(Long id) {
        return injuryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Injury not found: " + id));
    }

    private InjuryDTO toDto(Injury injury) {
        InjuryDTO dto = new InjuryDTO();
        dto.setId(injury.getId());
        dto.setDescription(injury.getDescription());
        dto.setStartDate(injury.getStartDate().toString());
        dto.setEndDate(injury.getEndDate() != null ? injury.getEndDate().toString() : null);
        dto.setIsActive(injury.isActive());
        dto.setPlayerId(injury.getPlayer().getPlayerID());
        dto.setPlayerName(injury.getPlayer().getFirstName() + " " + injury.getPlayer().getLastName());
        return dto;
    }
}