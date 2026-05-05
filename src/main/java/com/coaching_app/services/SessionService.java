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

    /**
     * Get all sessions for a given month/year
     * If month/year are null, returns all sessions
     */
    public List<SessionDTO> getSessions(Integer month, Integer year) {
        List<Session> sessions;

        if (month != null && year != null) {
            sessions = sessionRepository.findByMonthAndYear(month, year);
        } else {
            sessions = sessionRepository.findAll();
        }

        return sessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get full session details by ID
     */
    public SessionDetailDTO getSessionById(Long id) {
        return sessionRepository.findById(id)
                .map(this::convertToSessionDetailDTO)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }

    /**
     * Clone/Reuse a session: creates a new session with same drills
     * For now, creates a copy with date = today
     * Frontend can later modify the date in the Training Builder
     */
    public Long reuseSession(Long sourceSessionId) {
        Session sourceSession = sessionRepository.findById(sourceSessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sourceSessionId));

        // Create new session with same properties
        Session newSession = new Session();
        newSession.setTitle(sourceSession.getTitle());
        newSession.setDate(LocalDate.now()); // Default to today; frontend adjusts
        newSession.setTime(sourceSession.getTime());
        newSession.setDuration(sourceSession.getDuration());
        newSession.setIntensity(sourceSession.getIntensity());
        newSession.setFocus(sourceSession.getFocus());
        newSession.setAgeGroup(sourceSession.getAgeGroup());
        newSession.setColor(sourceSession.getColor());

        // Save the new session first
        Session savedSession = sessionRepository.save(newSession);

        // Clone all training drills
        sourceSession.getDrills().forEach(sourceDrill -> {
            TrainingDrill newDrill = new TrainingDrill();
            newDrill.setName(sourceDrill.getName());
            newDrill.setDuration(sourceDrill.getDuration());
            newDrill.setOrderIndex(sourceDrill.getOrderIndex());
            newDrill.setSession(savedSession);

            savedSession.getDrills().add(newDrill);
        });

        // Save the session again with drills
        Session result = sessionRepository.save(savedSession);
        return result.getId();
    }

    /**
     * Convert Session entity to SessionDTO (for calendar list)
     */
    private SessionDTO convertToSessionDTO(Session session) {
        return new SessionDTO(
                session.getId(),
                session.getTitle(),
                session.getDate().toString(),  // "2026-04-05"
                session.getTime()
        );
    }

    /**
     * Convert Session entity to SessionDetailDTO (for modal/detail view)
     */
    private SessionDetailDTO convertToSessionDetailDTO(Session session) {
        List<TrainingDrillDTO> drillDTOs = session.getDrills().stream()
                .map(this::convertToTrainingDrillDTO)
                .collect(Collectors.toList());

        return new SessionDetailDTO(
                session.getId(),
                session.getTitle(),
                session.getDate().toString(),
                session.getDuration(),
                session.getIntensity() != null ? session.getIntensity().toString() : null,
                session.getFocus(),
                session.getAgeGroup() != null ? session.getAgeGroup().toString() : null,
                drillDTOs
        );
    }

    /**
     * Convert TrainingDrill entity to TrainingDrillDTO
     */
    private TrainingDrillDTO convertToTrainingDrillDTO(TrainingDrill drill) {
        return new TrainingDrillDTO(
                drill.getId(),
                drill.getName(),
                drill.getDuration(),
                drill.getOrderIndex()
        );
    }

    public Long createSession(SessionDetailDTO dto) {
        Session session = new Session();
        session.setTitle(dto.getTitle());
        session.setDate(LocalDate.parse(dto.getDate()));
        session.setDuration(dto.getDuration());
        session.setTime(null); // add later if your frontend sends time
        session.setFocus(dto.getFocus());

        if (dto.getIntensity() != null) {
            session.setIntensity(com.coaching_app.enums.IntensityLevel.valueOf(dto.getIntensity().toUpperCase()));
        }

        if (dto.getAgeGroup() != null) {
            session.setAgeGroup(com.coaching_app.enums.AgeGroup.valueOf(dto.getAgeGroup().toUpperCase()));
        }

        // Optional: leave color null for now unless your DTO includes it
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

        Session result = sessionRepository.save(savedSession);
        return result.getId();
    }

}
