package com.sprintpilot.service.impl;

import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.SprintEvent;
import com.sprintpilot.repository.SprintEventRepository;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.service.SprintEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SprintEventServiceImpl implements SprintEventService {

    @Autowired
    private SprintEventRepository sprintEventRepository;
    
    @Autowired
    private SprintRepository sprintRepository;

    @Override
    @Transactional
    public SprintEventDto createEvent(SprintEventDto eventDto) {
        validateSprintExists(eventDto.sprintId());
        
        SprintEvent event = dtoToEntity(eventDto);
        SprintEvent savedEvent = sprintEventRepository.save(event);
        
        log.info("Created sprint event: {} for sprint: {}", savedEvent.getId(), eventDto.sprintId());
        return entityToDto(savedEvent);
    }

    @Override
    @Transactional
    public SprintEventDto updateEvent(String eventId, SprintEventDto eventDto) {
        SprintEvent existingEvent = sprintEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Sprint event not found with id: " + eventId));
        
        // Update fields
        existingEvent.setEventType(eventDto.eventType());
        existingEvent.setEventSubtype(eventDto.eventSubtype());
        existingEvent.setName(eventDto.name());
        existingEvent.setEventDate(eventDto.eventDate());
        existingEvent.setEventTime(eventDto.eventTime());
        existingEvent.setDurationMinutes(eventDto.durationMinutes());
        existingEvent.setDescription(eventDto.description());
        
        SprintEvent updatedEvent = sprintEventRepository.save(existingEvent);
        log.info("Updated sprint event: {}", eventId);
        
        return entityToDto(updatedEvent);
    }

    @Override
    public SprintEventDto getEventById(String eventId) {
        SprintEvent event = sprintEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Sprint event not found with id: " + eventId));
        return entityToDto(event);
    }

    @Override
    public List<SprintEventDto> getEventsBySprintId(String sprintId) {
        List<SprintEvent> events = sprintEventRepository.findBySprintIdOrderByEventDate(sprintId);
        return events.stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<SprintEventDto> getEventsBySprintAndType(String sprintId, SprintEvent.EventType eventType) {
        List<SprintEvent> events = sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(sprintId, eventType);
        return events.stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteEvent(String eventId) {
        if (!sprintEventRepository.existsById(eventId)) {
            throw new RuntimeException("Sprint event not found with id: " + eventId);
        }
        
        sprintEventRepository.deleteById(eventId);
        log.info("Deleted sprint event: {}", eventId);
    }

    @Override
    @Transactional
    public SprintEventDto createOrUpdateMeeting(String sprintId, SprintEvent.MeetingType meetingType, SprintEventDto eventDto) {
        validateSprintExists(sprintId);
        
        // Check if meeting already exists
        Optional<SprintEvent> existingMeeting = sprintEventRepository
            .findBySprintIdAndEventTypeAndEventSubtype(sprintId, SprintEvent.EventType.MEETING, meetingType);
        
        SprintEvent event;
        if (existingMeeting.isPresent()) {
            // Update existing meeting
            event = existingMeeting.get();
            event.setName(eventDto.name());
            event.setEventDate(eventDto.eventDate());
            event.setEventTime(eventDto.eventTime());
            event.setDurationMinutes(eventDto.durationMinutes());
            event.setDescription(eventDto.description());
            log.info("Updated existing {} meeting for sprint: {}", meetingType, sprintId);
        } else {
            // Create new meeting
            event = new SprintEvent();
            event.setId("event-" + UUID.randomUUID().toString());
            Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
            event.setSprint(sprint);
            event.setEventType(SprintEvent.EventType.MEETING);
            event.setEventSubtype(meetingType);
            event.setName(eventDto.name());
            event.setEventDate(eventDto.eventDate());
            event.setEventTime(eventDto.eventTime());
            event.setDurationMinutes(eventDto.durationMinutes());
            event.setDescription(eventDto.description());
            log.info("Created new {} meeting for sprint: {}", meetingType, sprintId);
        }
        
        SprintEvent savedEvent = sprintEventRepository.save(event);
        return entityToDto(savedEvent);
    }

    @Override
    @Transactional
    public SprintEventDto createDeploymentEvent(String sprintId, SprintEventDto eventDto) {
        validateSprintExists(sprintId);
        
        SprintEvent event = new SprintEvent();
        event.setId("event-" + UUID.randomUUID().toString());
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
        event.setSprint(sprint);
        event.setEventType(SprintEvent.EventType.DEPLOYMENT);
        event.setName(eventDto.name());
        event.setEventDate(eventDto.eventDate());
        event.setEventTime(eventDto.eventTime());
        event.setDescription(eventDto.description());
        
        SprintEvent savedEvent = sprintEventRepository.save(event);
        log.info("Created deployment event: {} for sprint: {}", savedEvent.getId(), sprintId);
        
        return entityToDto(savedEvent);
    }

    @Override
    @Transactional
    public SprintEventDto createHolidayEvent(String sprintId, SprintEventDto eventDto) {
        validateSprintExists(sprintId);
        
        SprintEvent event = new SprintEvent();
        event.setId("event-" + UUID.randomUUID().toString());
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
        event.setSprint(sprint);
        event.setEventType(SprintEvent.EventType.HOLIDAY);
        event.setName(eventDto.name());
        event.setEventDate(eventDto.eventDate());
        event.setEventTime(eventDto.eventTime());
        event.setDescription(eventDto.description());
        
        SprintEvent savedEvent = sprintEventRepository.save(event);
        log.info("Created holiday event: {} for sprint: {}", savedEvent.getId(), sprintId);
        
        return entityToDto(savedEvent);
    }

    @Override
    public SprintEventDto getMeetingBySprintAndType(String sprintId, SprintEvent.MeetingType meetingType) {
        Optional<SprintEvent> meeting = sprintEventRepository
            .findBySprintIdAndEventTypeAndEventSubtype(sprintId, SprintEvent.EventType.MEETING, meetingType);
        
        return meeting.map(this::entityToDto).orElse(null);
    }

    @Override
    @Transactional
    public void deleteEventsBySprintId(String sprintId) {
        List<SprintEvent> events = sprintEventRepository.findBySprintIdOrderByEventDate(sprintId);
        sprintEventRepository.deleteAll(events);
        log.info("Deleted {} events for sprint: {}", events.size(), sprintId);
    }

    @Override
    @Transactional
    public void deleteEventsBySprintAndType(String sprintId, SprintEvent.EventType eventType) {
        List<SprintEvent> events = sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(sprintId, eventType);
        sprintEventRepository.deleteAll(events);
        log.info("Deleted {} {} events for sprint: {}", events.size(), eventType, sprintId);
    }

    /**
     * Convert DTO to Entity
     */
    private SprintEvent dtoToEntity(SprintEventDto dto) {
        SprintEvent event = new SprintEvent();
        event.setId(dto.id() != null ? dto.id() : "event-" + UUID.randomUUID().toString());
        
        if (dto.sprintId() != null) {
            Sprint sprint = sprintRepository.findById(dto.sprintId())
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + dto.sprintId()));
            event.setSprint(sprint);
        }
        
        event.setEventType(dto.eventType());
        event.setEventSubtype(dto.eventSubtype());
        event.setName(dto.name());
        event.setEventDate(dto.eventDate());
        event.setEventTime(dto.eventTime());
        event.setDurationMinutes(dto.durationMinutes());
        event.setDescription(dto.description());
        
        return event;
    }

    /**
     * Convert Entity to DTO
     */
    private SprintEventDto entityToDto(SprintEvent entity) {
        return new SprintEventDto(
            entity.getId(),
            entity.getSprint() != null ? entity.getSprint().getId() : null,
            entity.getEventType(),
            entity.getEventSubtype(),
            entity.getName(),
            entity.getEventDate(),
            entity.getEventTime(),
            entity.getDurationMinutes(),
            entity.getDescription()
        );
    }

    /**
     * Validate that sprint exists
     */
    private void validateSprintExists(String sprintId) {
        if (sprintId == null || !sprintRepository.existsById(sprintId)) {
            throw new RuntimeException("Sprint not found with id: " + sprintId);
        }
    }
}
