package com.coaching_app.services;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.dto.SessionDTO;
import com.coaching_app.dto.SessionDetailDTO;
import com.coaching_app.dto.TrainingDrillDTO;
import com.coaching_app.dto.UpdateSessionDTO;
import com.coaching_app.models.Session;
import com.coaching_app.models.TrainingDrill;
import com.coaching_app.models.User;
import com.coaching_app.repositories.DrillRepository;
import com.coaching_app.repositories.SessionRepository;
import com.coaching_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final DrillRepository drillRepository;
    private final UserRepository userRepository;

    public List<SessionDTO> getSessions(Integer month, Integer year, Long userId) {
        List<Session> sessions;

        if (month != null && year != null) {
            sessions = sessionRepository.findByMonthAndYear(userId, month, year);
        } else {
            sessions = sessionRepository.findAll()
                    .stream()
                    .filter(s -> !s.isDeleted() && s.getUser() != null && s.getUser().getId().equals(userId))
                    .toList();
        }

        return sessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionDetailDTO getSessionById(Long id) {
        Session session = sessionRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        return convertToSessionDetailDTO(session);
    }

    public Long reuseSession(Long sourceSessionId, String newDate, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
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
        newSession.setUser(user);

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

    public Long createSession(SessionDetailDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        LocalDate date = LocalDate.parse(dto.getDate());
        String time = dto.getTime() != null ? dto.getTime() : "00:00";
        int duration = dto.getDuration() != null ? dto.getDuration() : 0;

        if (duration > 0) {
            List<Session> same = sessionRepository.findActiveByUserAndDate(user.getId(), date);
            int newStart = toMinutes(time);
            int newEnd = newStart + duration;
            for (Session existing : same) {
                if (existing.getTime() == null || existing.getDuration() == null) continue;
                int exStart = toMinutes(existing.getTime());
                int exEnd = exStart + existing.getDuration();
                if (newStart < exEnd && exStart < newEnd) {
                    throw new IllegalStateException(
                        "Session overlaps with \"" + existing.getTitle() + "\" (" +
                        existing.getTime() + ", " + existing.getDuration() + " min)");
                }
            }
        }

        Session session = new Session();
        session.setTitle(dto.getTitle());
        session.setDate(date);
        session.setDuration(duration);
        session.setTime(time);
        session.setFocus(dto.getFocus());

        if (dto.getIntensity() != null) {
            session.setIntensity(com.coaching_app.enums.IntensityLevel.valueOf(dto.getIntensity().toUpperCase()));
        }
        if (dto.getAgeGroup() != null) {
            session.setAgeGroup(com.coaching_app.enums.AgeGroup.valueOf(dto.getAgeGroup().toUpperCase()));
        }

        session.setColor(null);
        session.setUser(user);

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

    public Long updateSession(Long id, UpdateSessionDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Session session = sessionRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));

        assertEditable(session);

        LocalDate date = dto.getDate() != null ? LocalDate.parse(dto.getDate()) : session.getDate();
        if (dto.getDate() != null && date.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot move a session to a past date");
        }
        String time = dto.getTime() != null ? dto.getTime() : (session.getTime() != null ? session.getTime() : "00:00");
        int duration = dto.getDuration() != null ? dto.getDuration() : (session.getDuration() != null ? session.getDuration() : 0);

        if (duration > 0) {
            List<Session> same = sessionRepository.findActiveByUserAndDate(user.getId(), date);
            int newStart = toMinutes(time);
            int newEnd = newStart + duration;
            for (Session existing : same) {
                if (existing.getId().equals(id)) continue; // skip self
                if (existing.getTime() == null || existing.getDuration() == null) continue;
                int exStart = toMinutes(existing.getTime());
                int exEnd = exStart + existing.getDuration();
                if (newStart < exEnd && exStart < newEnd) {
                    throw new IllegalStateException(
                        "Session overlaps with \"" + existing.getTitle() + "\" (" +
                        existing.getTime() + ", " + existing.getDuration() + " min)");
                }
            }
        }

        if (dto.getTitle() != null) session.setTitle(dto.getTitle());
        session.setDate(date);
        session.setTime(time);
        session.setDuration(duration);
        if (dto.getFocus() != null) session.setFocus(dto.getFocus());
        if (dto.getNote() != null) session.setNote(dto.getNote());
        if (dto.getIntensity() != null)
            session.setIntensity(com.coaching_app.enums.IntensityLevel.valueOf(dto.getIntensity().toUpperCase()));
        if (dto.getAgeGroup() != null)
            session.setAgeGroup(com.coaching_app.enums.AgeGroup.valueOf(dto.getAgeGroup().toUpperCase()));

        if (dto.getDrills() != null) {
            session.getDrills().clear();
            for (TrainingDrillDTO drillDTO : dto.getDrills()) {
                TrainingDrill drill = new TrainingDrill();
                drill.setName(drillDTO.getTitle());
                drill.setDuration(drillDTO.getDuration());
                drill.setOrderIndex(drillDTO.getOrderIndex());
                drill.setSession(session);
                session.getDrills().add(drill);
            }
        }

        return sessionRepository.save(session).getId();
    }

    public void updateNote(Long id, String note) {
        Session session = sessionRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        session.setNote(note);
        sessionRepository.save(session);
    }

    /** Soft delete — sets deletedAt, never removes from DB. Past sessions cannot be deleted. */
    public void deleteSession(Long id) {
        Session session = sessionRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        assertEditable(session);
        session.softDelete();
        sessionRepository.save(session);
    }

    private void assertEditable(Session session) {
        if (session.isPast()) {
            throw new IllegalStateException("Past sessions are read-only");
        }
    }
    private SessionDTO convertToSessionDTO(Session session) {
        SessionDTO dto = new SessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setDate(session.getDate().toString());
        dto.setTime(session.getTime());
        dto.setDuration(session.getDuration());
        dto.setReadOnly(session.isPast());
        return dto;
    }

    private SessionDetailDTO convertToSessionDetailDTO(Session session) {
        List<TrainingDrillDTO> drillDTOs = session.getDrills().stream()
                .map(this::convertToTrainingDrillDTO)
                .collect(Collectors.toList());

        SessionDetailDTO dto = new SessionDetailDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setDate(session.getDate().toString());
        dto.setTime(session.getTime());
        dto.setDuration(session.getDuration());
        dto.setIntensity(session.getIntensity() != null ? session.getIntensity().toString() : null);
        dto.setFocus(session.getFocus());
        dto.setAgeGroup(session.getAgeGroup() != null ? session.getAgeGroup().toString() : null);
        dto.setDrills(drillDTOs);
        dto.setNote(session.getNote());
        dto.setReadOnly(session.isPast());
        return dto;
    }

    private TrainingDrillDTO convertToTrainingDrillDTO(TrainingDrill drill) {
        return new TrainingDrillDTO(
                drill.getId(),
                drill.getName(),
                drill.getDuration(),
                drill.getOrderIndex()
        );
    }

    private int toMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
