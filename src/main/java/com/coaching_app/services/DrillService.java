package com.coaching_app.services;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.enums.AgeGroup;
import com.coaching_app.enums.IntensityLevel;
import com.coaching_app.enums.TrainingFocus;
import com.coaching_app.models.Drill;
import com.coaching_app.repositories.DrillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrillService {

    private final DrillRepository drillRepository;

    public List<DrillDTO> getDrills(String focus, String search) {
        String normalizedFocus = hasText(focus)
                ? focus.trim().toUpperCase().replace(" ", "_")
                : null;
        String normalizedSearch = normalize(search);

        return drillRepository.findFiltered(normalizedFocus, normalizedSearch)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public DrillDTO getDrillById(Long id) {
        Drill drill = findDrillById(id);
        return toDto(drill);
    }

    public void updateDrill(Long id, DrillDTO dto) {
        Drill drill = findDrillById(id);

        if (dto.getTitle() != null) {
            drill.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            drill.setDescription(dto.getDescription());
        }
        if (dto.getFocus() != null) {
            drill.setFocus(TrainingFocus.valueOf(dto.getFocus().trim().toUpperCase()));
        }
        if (dto.getIntensity() != null) {
            drill.setIntensity(IntensityLevel.valueOf(dto.getIntensity().trim().toUpperCase()));
        }
        if (dto.getAgeGroup() != null) {
            drill.setAgeGroup(AgeGroup.valueOf(dto.getAgeGroup().trim().toUpperCase()));
        }
        if (dto.getTag() != null) {
            drill.setTag(dto.getTag());
        }
        if (dto.getDuration() != null) {
            drill.setDuration(dto.getDuration());
        }
        if (dto.getLevel() != null) {
            drill.setLevel(dto.getLevel());
        }
        if (dto.getEquipment() != null) {
            drill.setEquipment(dto.getEquipment());
        }

        drillRepository.save(drill);
    }

    public void deleteDrill(Long id) {
        Drill drill = findDrillById(id);
        drill.setDeleted(true);
        drillRepository.save(drill);
    }

    public Long saveDrillFromOcr(OcrConfirmDTO dto) {
        Drill drill = new Drill();
        drill.setTitle(dto.getTitle());
        drill.setDescription(dto.getDescription());

        if (dto.getFocus() != null) {
            drill.setFocus(TrainingFocus.valueOf(dto.getFocus().trim().toUpperCase()));
        }
        if (dto.getIntensity() != null) {
            drill.setIntensity(IntensityLevel.valueOf(dto.getIntensity().trim().toUpperCase()));
        }
        if (dto.getAgeGroup() != null) {
            drill.setAgeGroup(AgeGroup.valueOf(dto.getAgeGroup().trim().toUpperCase()));
        }

        if (dto.getDuration() != null) {
            drill.setDuration(dto.getDuration());
        }
        drill.setLevel(dto.getLevel());
        drill.setEquipment(dto.getEquipment());

        Drill saved = drillRepository.save(drill);
        return saved.getId();
    }

    public int saveAllDrillsFromOcr(List<OcrConfirmDTO> drills) {
        for (OcrConfirmDTO dto : drills) {
            try {
                saveDrillFromOcr(dto);
            } catch (Exception e) {
                System.out.println("Skipping drill due to error: " + dto.getTitle() + " - " + e.getMessage());
            }
        }
        return drills.size();
    }

    public Long createDrill(DrillDTO dto) {
        Drill drill = new Drill();
        drill.setTitle(dto.getTitle());
        drill.setDescription(dto.getDescription());

        if (dto.getFocus() != null) {
            drill.setFocus(TrainingFocus.valueOf(dto.getFocus().trim().toUpperCase()));
        }
        if (dto.getIntensity() != null) {
            drill.setIntensity(IntensityLevel.valueOf(dto.getIntensity().trim().toUpperCase()));
        }
        if (dto.getAgeGroup() != null) {
            drill.setAgeGroup(AgeGroup.valueOf(dto.getAgeGroup().trim().toUpperCase()));
        }

        drill.setTag(dto.getTag());
        drill.setDuration(dto.getDuration());
        drill.setLevel(dto.getLevel());
        drill.setEquipment(dto.getEquipment());

        Drill saved = drillRepository.save(drill);
        return saved.getId();
    }


    private Drill findDrillById(Long id) {
        return drillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drill not found: " + id));
    }

    private DrillDTO toDto(Drill drill) {
        return new DrillDTO(
                drill.getId(),
                drill.getTitle(),
                drill.getDescription(),
                drill.getFocus() != null ? drill.getFocus().name() : null,
                drill.getIntensity() != null ? drill.getIntensity().name() : null,
                drill.getAgeGroup() != null ? drill.getAgeGroup().name() : null,
                drill.getTag(),
                drill.getDuration(),
                drill.getLevel(),
                drill.getEquipment()
        );
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
