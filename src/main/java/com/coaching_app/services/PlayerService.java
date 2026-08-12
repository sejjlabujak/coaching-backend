package com.coaching_app.services;

import com.coaching_app.dto.PlayerRequestDTO;
import com.coaching_app.dto.PlayerResponseDTO;
import com.coaching_app.enums.AgeGroup;
import com.coaching_app.models.Image;
import com.coaching_app.models.Player;
import com.coaching_app.models.User;
import com.coaching_app.repositories.ImageRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    public List<PlayerResponseDTO> getPlayersForUser(Long userId) {
        return playerRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public PlayerResponseDTO createPlayer(PlayerRequestDTO dto, User user) {
        Player player = new Player();
        player.setFirstName(dto.getFirstName());
        player.setLastName(dto.getLastName());
        player.setPosition(dto.getPosition());
        player.setJerseyNumber(dto.getJerseyNumber());
        player.setHeightCm(dto.getHeightCm());
        player.setWeightKg(dto.getWeightKg());
        player.setBirthDate(dto.getBirthDate());
        player.setBirthCity(dto.getBirthCity());
        player.setNationality(dto.getNationality());
        player.setAgeGroup(parseAgeGroup(dto.getAgeGroup()));
        player.setUser(user);

        return toDto(playerRepository.save(player));
    }

    public PlayerResponseDTO updatePlayer(Long id, Long userId, PlayerRequestDTO dto) {
        Player player = findOwnedPlayer(id, userId);

        if (dto.getFirstName() != null) player.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) player.setLastName(dto.getLastName());
        if (dto.getPosition() != null) player.setPosition(dto.getPosition());
        if (dto.getJerseyNumber() != null) player.setJerseyNumber(dto.getJerseyNumber());
        if (dto.getHeightCm() != null) player.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg() != null) player.setWeightKg(dto.getWeightKg());
        if (dto.getBirthDate() != null) player.setBirthDate(dto.getBirthDate());
        if (dto.getBirthCity() != null) player.setBirthCity(dto.getBirthCity());
        if (dto.getNationality() != null) player.setNationality(dto.getNationality());
        if (dto.getAgeGroup() != null) player.setAgeGroup(parseAgeGroup(dto.getAgeGroup()));

        return toDto(playerRepository.save(player));
    }

    public void deletePlayer(Long id, Long userId) {
        Player player = findOwnedPlayer(id, userId);
        playerRepository.delete(player);
    }

    public PlayerResponseDTO uploadImage(Long id, Long userId, MultipartFile file) {
        Player player = findOwnedPlayer(id, userId);

        String relativePath = fileStorageService.storeImage(file, "players");
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/" + relativePath)
                .toUriString();

        imageRepository.deleteByPlayerPlayerID(player.getPlayerID());
        player.getImages().clear();

        Image image = new Image();
        image.setUrl(url);
        image.setPlayer(player);
        imageRepository.save(image);

        return toDto(playerRepository.findById(player.getPlayerID()).orElseThrow());
    }

    private Player findOwnedPlayer(Long id, Long userId) {
        return playerRepository.findByPlayerIDAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + id));
    }

    private AgeGroup parseAgeGroup(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return AgeGroup.valueOf(raw.trim().toUpperCase());
    }

    public PlayerResponseDTO toDto(Player p) {
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
