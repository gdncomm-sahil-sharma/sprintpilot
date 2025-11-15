package com.sprintpilot.service;

import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.SprintEventDto;
import java.time.LocalDate;
import java.util.List;

public interface SprintService {
    
    SprintDto createSprint(SprintDto sprintDto);
    
    SprintDto updateSprint(String id, SprintDto sprintDto);
    
    SprintDto getSprintById(String id);
    
    SprintDto getSprintWithFullDetails(String id);
    
    List<SprintDto> getAllSprints();
    
    List<SprintDto> getActiveSprints();
    
    List<SprintDto> getCompletedSprints();
    
    SprintDto startSprint(String id);
    
    SprintDto completeSprint(String id);
    
    SprintDto archiveSprint(String id);
    
    void deleteSprint(String id);
    
    SprintDto addEvent(String sprintId, SprintEventDto event);
    
    SprintDto removeEvent(String sprintId, String eventId);
    
    SprintDto addTeamMember(String sprintId, String memberId);
    
    SprintDto removeTeamMember(String sprintId, String memberId);
    
    SprintDto calculateSprintDates(LocalDate startDate, Integer duration, List<String> holidays);
    
    SprintDto cloneSprint(String sourceSprintId);
}
