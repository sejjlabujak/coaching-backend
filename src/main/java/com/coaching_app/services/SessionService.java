package com.coaching_app.services;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.dto.SessionDTO;
import com.coaching_app.dto.SessionDetailDTO;
import com.coaching_app.dto.TrainingDrillDTO;
import com.coaching_app.models.Session;
import com.coaching_app.models.TrainingDrill;
import com.coaching_app.repositories.DrillRepository;
import com.coaching_app.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final DrillRepository drillRepository;

    public List<SessionDTO> getSessions(Integer month, Integer year) {
        List<Session> sessions;

        if (month != null && year != null) {
            sessions = sessionRepository.findByMonthAndYear(month, year);
        } else {
            sessions = sessionRepository.findAll()
                    .stream()
                    .filter(s -> !s.isDeleted())
                    .toList();
        }

        return sessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
    }

    public SessionDetailDTO getSessionById(Long id) {
        return sessionRepository.findById(id)
                .map(this::convertToSessionDetailDTO)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }


    public Long reuseSession(Long sourceSessionId, String newDate) {
        Session sourceSession = sessionRepository.findById(sourceSessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sourceSessionId));

        LocalDate date = (newDate != null && !newDate.isBlank())
                ? LocalDate.parse(newDate)
                : LocalDate.now();

        Session newSession = new Session();
        newSession.setTitle(sourceSession.getTitle());
        newSession.setDate(date);
        newSession.setTime(sourceSession.getTime());
        newSession.setDuration(sourceSession.getDuration());
        newSession.setIntensity(sourceSession.getIntensity());
        newSession.setFocus(sourceSession.getFocus());
        newSession.setAgeGroup(sourceSession.getAgeGroup());
        newSession.setColor(sourceSession.getColor());

        Session savedSession = sessionRepository.save(newSession);

        sourceSession.getDrills().forEach(sourceDrill -> {
            TrainingDrill newDrill = new TrainingDrill();
            newDrill.setName(sourceDrill.getName());
            newDrill.setDuration(sourceDrill.getDuration());
            newDrill.setOrderIndex(sourceDrill.getOrderIndex());
            newDrill.setDrill(sourceDrill.getDrill());   // preserve Drill FK
            newDrill.setSession(savedSession);
            savedSession.getDrills().add(newDrill);
        });

        return sessionRepository.save(savedSession).getId();
    }

    public Long createSession(SessionDetailDTO dto) {
        Session session = new Session();
        session.setTitle(dto.getTitle());
        session.setDate(LocalDate.parse(dto.getDate()));
        session.setDuration(dto.getDuration());
        session.setTime(dto.getTime() != null ? dto.getTime() : "00:00");
        session.setFocus(dto.getFocus());

        if (dto.getIntensity() != null) {
            session.setIntensity(com.coaching_app.enums.IntensityLevel.valueOf(dto.getIntensity().toUpperCase()));
        }
        if (dto.getAgeGroup() != null) {
            session.setAgeGroup(com.coaching_app.enums.AgeGroup.valueOf(dto.getAgeGroup().toUpperCase()));
        }

        session.setColor(null);

        Session savedSession = sessionRepository.save(session);

        if (dto.getDrills() != null) {
            for (TrainingDrillDTO drillDTO : dto.getDrills()) {
                TrainingDrill drill = new TrainingDrill();
                drill.setName(drillDTO.getTitle());
                drill.setDuration(drillDTO.getDuration());
                drill.setOrderIndex(drillDTO.getOrderIndex());
                drill.setSession(savedSession);
                savedSession.getDrills().add(drill);
            }
        }

        return sessionRepository.save(savedSession).getId();
    }

    /** Soft delete — sets deletedAt, never removes from DB. */
    public void deleteSession(Long id) {
        Session session = sessionRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        session.softDelete();
        sessionRepository.save(session);
    }
    private SessionDTO convertToSessionDTO(Session session) {
        return new SessionDTO(
                session.getId(),
                session.getTitle(),
                session.getDate().toString(),
                session.getTime()
        );
    }

    private SessionDetailDTO convertToSessionDetailDTO(Session session) {
        List<TrainingDrillDTO> drillDTOs = session.getDrills().stream()
                .map(this::convertToTrainingDrillDTO)
                .collect(Collectors.toList());

        return new SessionDetailDTO(
                session.getId(),
                session.getTitle(),
                session.getDate().toString(),
                session.getTime(),
                session.getDuration(),
                session.getIntensity() != null ? session.getIntensity().toString() : null,
                session.getFocus(),
                session.getAgeGroup() != null ? session.getAgeGroup().toString() : null,
                drillDTOs
        );
    }

    private TrainingDrillDTO convertToTrainingDrillDTO(TrainingDrill drill) {
        return new TrainingDrillDTO(
                drill.getId(),
                drill.getName(),
                drill.getDuration(),
                drill.getOrderIndex()
        );
    }
}
