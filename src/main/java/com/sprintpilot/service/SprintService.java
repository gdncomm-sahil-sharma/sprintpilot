package com.sprintpilot.service;

import com.sprintpilot.dto.CompleteSprintResponse;
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
    
    List<SprintDto> getArchivedSprints();
    
    SprintDto archiveSprint(String id);
    
    SprintDto reactivateSprint(String id);
    
    boolean canCompleteSprint(String id);
    
    CompleteSprintResponse completeAndArchiveSprint(String id);
    
    void deleteSprint(String id);
    
    void deleteLatestArchivedSprint(String id);
    
    SprintDto addEvent(String sprintId, SprintEventDto event);
    
    SprintDto removeEvent(String sprintId, String eventId);
    
    SprintDto addTeamMember(String sprintId, String memberId);
    
    SprintDto removeTeamMember(String sprintId, String memberId);
    
    SprintDto calculateSprintDates(LocalDate startDate, Integer duration, List<String> holidays);
    
    SprintDto cloneSprint(String sourceSprintId);

    String getSprintName(String sprintId);
    
    // New methods for active sprint detection and templates
    SprintDto getCurrentActiveSprint();
    
    boolean hasActiveSprint();
    
    List<SprintDto> getSprintTemplates();
    
    SprintDto createSprintFromTemplate(String templateSprintId, LocalDate newStartDate);
}
