package com.coaching_app.services;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.enums.AgeGroup;
import com.coaching_app.enums.IntensityLevel;
import com.coaching_app.enums.TrainingFocus;
import com.coaching_app.models.Drill;
import com.coaching_app.repositories.DrillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrillService unit tests")
class DrillServiceTest {

    @Mock
    private DrillRepository drillRepository;

    @InjectMocks
    private DrillService drillService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Drill shootingDrill;
    private Drill defenseDrill;

    @BeforeEach
    void setUp() {
        shootingDrill = new Drill();
        shootingDrill.setId(1L);
        shootingDrill.setTitle("Three-Point Shooting");
        shootingDrill.setDescription("Perimeter shooting drill");
        shootingDrill.setFocus(TrainingFocus.SHOOTING);
        shootingDrill.setIntensity(IntensityLevel.HIGH);
        shootingDrill.setAgeGroup(AgeGroup.SENIOR);
        shootingDrill.setTag("shooting");
        shootingDrill.setDuration(20);
        shootingDrill.setLevel("Intermediate");
        shootingDrill.setEquipment("balls,cones");
        shootingDrill.setDeleted(false);

        defenseDrill = new Drill();
        defenseDrill.setId(2L);
        defenseDrill.setTitle("Defensive Slides");
        defenseDrill.setDescription("Lateral movement drill");
        defenseDrill.setFocus(TrainingFocus.DEFENSE);
        defenseDrill.setIntensity(IntensityLevel.MEDIUM);
        defenseDrill.setAgeGroup(AgeGroup.U18);
        defenseDrill.setDuration(15);
        defenseDrill.setLevel("Beginner");
        defenseDrill.setDeleted(false);
    }

    // =========================================================================
    // getDrills
    // =========================================================================

    @Test
    @DisplayName("getDrills – no filters returns all non-deleted drills")
    void getDrills_noFilters_returnsAll() {
        when(drillRepository.findFiltered(null, null))
                .thenReturn(List.of(shootingDrill, defenseDrill));

        List<DrillDTO> result = drillService.getDrills(null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DrillDTO::getTitle)
                .containsExactly("Three-Point Shooting", "Defensive Slides");
    }

    @Test
    @DisplayName("getDrills – focus filter is normalized to upper-case enum string")
    void getDrills_withFocus_normalizesAndFilters() {
        when(drillRepository.findFiltered("SHOOTING", null))
                .thenReturn(List.of(shootingDrill));

        List<DrillDTO> result = drillService.getDrills("shooting", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFocus()).isEqualTo("SHOOTING");
        verify(drillRepository).findFiltered("SHOOTING", null);
    }

    @Test
    @DisplayName("getDrills – focus with spaces is normalized (e.g. 'team building' → 'TEAM_BUILDING')")
    void getDrills_focusWithSpaces_replacesSpaceWithUnderscore() {
        when(drillRepository.findFiltered("TEAM_BUILDING", null))
                .thenReturn(List.of());

        drillService.getDrills("team building", null);

        verify(drillRepository).findFiltered("TEAM_BUILDING", null);
    }

    @Test
    @DisplayName("getDrills – blank focus string is treated as null")
    void getDrills_blankFocus_treatedAsNull() {
        when(drillRepository.findFiltered(null, null)).thenReturn(List.of());

        drillService.getDrills("   ", null);

        verify(drillRepository).findFiltered(null, null);
    }

    @Test
    @DisplayName("getDrills – search keyword is trimmed and passed through")
    void getDrills_withSearch_passesKeywordToRepository() {
        when(drillRepository.findFiltered(null, "layup"))
                .thenReturn(List.of(shootingDrill));

        List<DrillDTO> result = drillService.getDrills(null, "  layup  ");

        assertThat(result).hasSize(1);
        verify(drillRepository).findFiltered(null, "layup");
    }

    @Test
    @DisplayName("getDrills – empty list returned when repository finds nothing")
    void getDrills_noMatches_returnsEmptyList() {
        when(drillRepository.findFiltered(any(), any())).thenReturn(List.of());

        List<DrillDTO> result = drillService.getDrills("DEFENSE", "xyz");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getDrills – DTO fields are mapped correctly from entity")
    void getDrills_dtoMappingIsComplete() {
        when(drillRepository.findFiltered(null, null))
                .thenReturn(List.of(shootingDrill));

        DrillDTO dto = drillService.getDrills(null, null).get(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Three-Point Shooting");
        assertThat(dto.getDescription()).isEqualTo("Perimeter shooting drill");
        assertThat(dto.getFocus()).isEqualTo("SHOOTING");
        assertThat(dto.getIntensity()).isEqualTo("HIGH");
        assertThat(dto.getAgeGroup()).isEqualTo("SENIOR");
        assertThat(dto.getTag()).isEqualTo("shooting");
        assertThat(dto.getDuration()).isEqualTo(20);
        assertThat(dto.getLevel()).isEqualTo("Intermediate");
        assertThat(dto.getEquipment()).isEqualTo("balls,cones");
    }

    // =========================================================================
    // getDrillById
    // =========================================================================

    @Test
    @DisplayName("getDrillById – returns DTO for existing drill")
    void getDrillById_exists_returnsDto() {
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));

        DrillDTO dto = drillService.getDrillById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Three-Point Shooting");
    }

    @Test
    @DisplayName("getDrillById – throws RuntimeException when drill not found")
    void getDrillById_notFound_throwsException() {
        when(drillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drillService.getDrillById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // createDrill
    // =========================================================================

    @Test
    @DisplayName("createDrill – saves drill and returns new id")
    void createDrill_validDto_savesAndReturnsId() {
        DrillDTO dto = new DrillDTO(null, "New Drill", "desc",
                "SHOOTING", "LOW", "U16", "tag", 10, "Beginner", "cones");

        Drill saved = new Drill();
        saved.setId(42L);
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        Long id = drillService.createDrill(dto);

        assertThat(id).isEqualTo(42L);
        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        Drill persisted = captor.getValue();
        assertThat(persisted.getTitle()).isEqualTo("New Drill");
        assertThat(persisted.getFocus()).isEqualTo(TrainingFocus.SHOOTING);
        assertThat(persisted.getIntensity()).isEqualTo(IntensityLevel.LOW);
        assertThat(persisted.getAgeGroup()).isEqualTo(AgeGroup.U16);
        assertThat(persisted.getDuration()).isEqualTo(10);
        assertThat(persisted.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("createDrill – null enum fields are skipped without throwing")
    void createDrill_nullEnumFields_savedWithoutEnums() {
        DrillDTO dto = new DrillDTO(null, "Minimal Drill", null,
                null, null, null, null, null, null, null);

        Drill saved = new Drill();
        saved.setId(5L);
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        Long id = drillService.createDrill(dto);

        assertThat(id).isEqualTo(5L);
        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        assertThat(captor.getValue().getFocus()).isNull();
        assertThat(captor.getValue().getIntensity()).isNull();
        assertThat(captor.getValue().getAgeGroup()).isNull();
    }

    // =========================================================================
    // updateDrill
    // =========================================================================

    @Test
    @DisplayName("updateDrill – updates all provided fields and saves")
    void updateDrill_allFields_updatesAndSaves() {
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));
        when(drillRepository.save(any())).thenReturn(shootingDrill);

        DrillDTO dto = new DrillDTO(null, "Updated Title", "New desc",
                "DEFENSE", "LOW", "U14", "newtag", 30, "Advanced", "hoops");

        drillService.updateDrill(1L, dto);

        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        Drill updated = captor.getValue();
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getFocus()).isEqualTo(TrainingFocus.DEFENSE);
        assertThat(updated.getIntensity()).isEqualTo(IntensityLevel.LOW);
        assertThat(updated.getAgeGroup()).isEqualTo(AgeGroup.U14);
        assertThat(updated.getTag()).isEqualTo("newtag");
        assertThat(updated.getDuration()).isEqualTo(30);
        assertThat(updated.getLevel()).isEqualTo("Advanced");
        assertThat(updated.getEquipment()).isEqualTo("hoops");
    }

    @Test
    @DisplayName("updateDrill – null fields in DTO leave existing values unchanged")
    void updateDrill_nullFields_keepsExistingValues() {
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));
        when(drillRepository.save(any())).thenReturn(shootingDrill);

        // Only update title; everything else null
        DrillDTO dto = new DrillDTO(null, "Only Title Changed", null,
                null, null, null, null, null, null, null);

        drillService.updateDrill(1L, dto);

        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        Drill updated = captor.getValue();
        assertThat(updated.getTitle()).isEqualTo("Only Title Changed");
        assertThat(updated.getFocus()).isEqualTo(TrainingFocus.SHOOTING);      // unchanged
        assertThat(updated.getIntensity()).isEqualTo(IntensityLevel.HIGH);     // unchanged
        assertThat(updated.getDuration()).isEqualTo(20);                        // unchanged
    }

    @Test
    @DisplayName("updateDrill – throws RuntimeException when drill not found")
    void updateDrill_notFound_throwsException() {
        when(drillRepository.findById(99L)).thenReturn(Optional.empty());

        DrillDTO dto = new DrillDTO(null, "Title", null,
                null, null, null, null, null, null, null);

        assertThatThrownBy(() -> drillService.updateDrill(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(drillRepository, never()).save(any());
    }

    // =========================================================================
    // deleteDrill  — tested with multiple scenarios to satisfy homework requirement
    // =========================================================================

    @Test
    @DisplayName("deleteDrill – sets deleted flag to true and saves")
    void deleteDrill_existingDrill_setsDeletedTrue() {
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));
        when(drillRepository.save(any())).thenReturn(shootingDrill);

        drillService.deleteDrill(1L);

        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteDrill – drill is NOT removed from the database (soft delete)")
    void deleteDrill_softDelete_doesNotCallDeleteById() {
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));
        when(drillRepository.save(any())).thenReturn(shootingDrill);

        drillService.deleteDrill(1L);

        verify(drillRepository, never()).deleteById(any());
        verify(drillRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteDrill – throws RuntimeException when drill id does not exist")
    void deleteDrill_notFound_throwsException() {
        when(drillRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drillService.deleteDrill(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");

        verify(drillRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteDrill – already-deleted drill can be deleted again without error")
    void deleteDrill_alreadyDeleted_setsDeletedTrueAgain() {
        shootingDrill.setDeleted(true);
        when(drillRepository.findById(1L)).thenReturn(Optional.of(shootingDrill));
        when(drillRepository.save(any())).thenReturn(shootingDrill);

        drillService.deleteDrill(1L);

        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    // =========================================================================
    // saveDrillFromOcr
    // =========================================================================

    @Test
    @DisplayName("saveDrillFromOcr – maps all OCR fields including duration and saves")
    void saveDrillFromOcr_fullDto_savesCorrectly() {
        OcrConfirmDTO dto = new OcrConfirmDTO();
        dto.setTitle("Fast Break Drill");
        dto.setDescription("Full court transition drill");
        dto.setFocus("CONDITIONING");
        dto.setIntensity("HIGH");
        dto.setAgeGroup("U18");
        dto.setLevel("Advanced");
        dto.setEquipment("balls");
        dto.setDuration(25);

        Drill saved = new Drill();
        saved.setId(10L);
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        Long id = drillService.saveDrillFromOcr(dto);

        assertThat(id).isEqualTo(10L);
        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        Drill persisted = captor.getValue();
        assertThat(persisted.getTitle()).isEqualTo("Fast Break Drill");
        assertThat(persisted.getFocus()).isEqualTo(TrainingFocus.CONDITIONING);
        assertThat(persisted.getIntensity()).isEqualTo(IntensityLevel.HIGH);
        assertThat(persisted.getAgeGroup()).isEqualTo(AgeGroup.U18);
        assertThat(persisted.getDuration()).isEqualTo(25);
        assertThat(persisted.getLevel()).isEqualTo("Advanced");
        assertThat(persisted.getEquipment()).isEqualTo("balls");
    }

    @Test
    @DisplayName("saveDrillFromOcr – null enum strings do not throw, saved as null")
    void saveDrillFromOcr_nullEnums_savedWithNulls() {
        OcrConfirmDTO dto = new OcrConfirmDTO();
        dto.setTitle("Minimal OCR Drill");
        dto.setFocus(null);
        dto.setIntensity(null);
        dto.setAgeGroup(null);

        Drill saved = new Drill();
        saved.setId(11L);
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        Long id = drillService.saveDrillFromOcr(dto);

        assertThat(id).isEqualTo(11L);
        ArgumentCaptor<Drill> captor = ArgumentCaptor.forClass(Drill.class);
        verify(drillRepository).save(captor.capture());
        assertThat(captor.getValue().getFocus()).isNull();
        assertThat(captor.getValue().getIntensity()).isNull();
        assertThat(captor.getValue().getAgeGroup()).isNull();
    }

    // =========================================================================
    // saveAllDrillsFromOcr
    // =========================================================================

    @Test
    @DisplayName("saveAllDrillsFromOcr – saves every drill in the list and returns count")
    void saveAllDrillsFromOcr_multipleItems_savesAllAndReturnsSize() {
        OcrConfirmDTO dto1 = new OcrConfirmDTO();
        dto1.setTitle("Drill One");

        OcrConfirmDTO dto2 = new OcrConfirmDTO();
        dto2.setTitle("Drill Two");

        Drill saved = new Drill();
        saved.setId(1L);
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        int count = drillService.saveAllDrillsFromOcr(List.of(dto1, dto2));

        assertThat(count).isEqualTo(2);
        verify(drillRepository, times(2)).save(any(Drill.class));
    }

    @Test
    @DisplayName("saveAllDrillsFromOcr – one failing drill does not abort the rest")
    void saveAllDrillsFromOcr_oneInvalidDrill_skipsAndContinues() {
        OcrConfirmDTO bad = new OcrConfirmDTO();
        bad.setTitle("Bad Drill");
        bad.setFocus("INVALID_ENUM_VALUE"); // will throw IllegalArgumentException

        OcrConfirmDTO good = new OcrConfirmDTO();
        good.setTitle("Good Drill");
        good.setFocus(null);

        Drill saved = new Drill();
        saved.setId(2L);
        // Only the good drill reaches save(); bad one throws before that
        when(drillRepository.save(any(Drill.class))).thenReturn(saved);

        int count = drillService.saveAllDrillsFromOcr(List.of(bad, good));

        assertThat(count).isEqualTo(2);            // returns full list size regardless
        verify(drillRepository, times(1)).save(any()); // only the good one saved
    }

    @Test
    @DisplayName("saveAllDrillsFromOcr – empty list returns 0 and makes no DB calls")
    void saveAllDrillsFromOcr_emptyList_returnsZero() {
        int count = drillService.saveAllDrillsFromOcr(List.of());

        assertThat(count).isEqualTo(0);
        verify(drillRepository, never()).save(any());
    }
}