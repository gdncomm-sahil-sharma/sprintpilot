package com.sprintpilot.service;

import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.entity.SprintEvent;
import java.util.List;

public interface SprintEventService {
    
    /**
     * Create a new sprint event (meeting, deployment, etc.)
     */
    SprintEventDto createEvent(SprintEventDto eventDto);
    
    /**
     * Update an existing sprint event
     */
    SprintEventDto updateEvent(String eventId, SprintEventDto eventDto);
    
    /**
     * Get event by ID
     */
    SprintEventDto getEventById(String eventId);
    
    /**
     * Get all events for a specific sprint
     */
    List<SprintEventDto> getEventsBySprintId(String sprintId);
    
    /**
     * Get events by type for a sprint (MEETING, DEPLOYMENT, HOLIDAY)
     */
    List<SprintEventDto> getEventsBySprintAndType(String sprintId, SprintEvent.EventType eventType);
    
    /**
     * Delete an event
     */
    void deleteEvent(String eventId);
    
    /**
     * Create or update meeting event (Sprint Planning, Grooming, Retrospective)
     */
    SprintEventDto createOrUpdateMeeting(String sprintId, SprintEvent.MeetingType meetingType, SprintEventDto eventDto);
    
    /**
     * Create deployment event
     */
    SprintEventDto createDeploymentEvent(String sprintId, SprintEventDto eventDto);
    
    /**
     * Create holiday event
     */
    SprintEventDto createHolidayEvent(String sprintId, SprintEventDto eventDto);
    
    /**
     * Get meeting by sprint and type
     */
    SprintEventDto getMeetingBySprintAndType(String sprintId, SprintEvent.MeetingType meetingType);
    
    /**
     * Delete all events for a sprint
     */
    void deleteEventsBySprintId(String sprintId);
    
    /**
     * Delete events by sprint and type
     */
    void deleteEventsBySprintAndType(String sprintId, SprintEvent.EventType eventType);
}
