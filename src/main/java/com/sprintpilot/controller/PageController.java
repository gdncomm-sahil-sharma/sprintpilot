package com.sprintpilot.controller;

import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.service.SprintService;
import com.sprintpilot.service.TeamService;
import com.sprintpilot.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

@Controller
public class PageController {
    
    @Autowired
    private SprintService sprintService;
    
    @Autowired(required = false)
    private TeamService teamService;
    
    @Autowired(required = false)
    private HolidayService holidayService;
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activeSprints", sprintService.getActiveSprints());
        model.addAttribute("pageTitle", "SprintPilot - Dashboard");
        return "dashboard/index";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeSprints", sprintService.getActiveSprints());
        model.addAttribute("completedSprints", sprintService.getCompletedSprints());
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/index";
    }
    
    @GetMapping("/sprint/new")
    public String newSprint(Model model) {
        // Simple approach - always show sprint/new page
        // JavaScript will call API to get active sprint and populate form
        model.addAttribute("pageTitle", "Sprint Setup");
        return "sprint/new";
    }
    
    @GetMapping("/sprint/{id}/setup")
    public String setupSprint(@PathVariable String id, Model model) {
        try {
            SprintDto sprint = sprintService.getSprintWithFullDetails(id);
            List<SprintDto> templates = sprintService.getSprintTemplates();
            
            model.addAttribute("pageTitle", "Sprint Setup - " + sprint.id());
            model.addAttribute("hasActiveSprint", true);
            model.addAttribute("currentSprint", sprint);
            model.addAttribute("sprintTemplates", templates);
            model.addAttribute("hasTemplates", !templates.isEmpty());
            model.addAttribute("currentSprintId", id);
            
            return "sprint/new";
        } catch (Exception e) {
            // Sprint not found, redirect to new sprint page
            return "redirect:/sprint/new";
        }
    }
    
    @GetMapping("/sprint/{id}")
    public String viewSprint(@PathVariable String id, Model model) {
        model.addAttribute("sprint", sprintService.getSprintWithFullDetails(id));
        model.addAttribute("pageTitle", "Sprint Details");
        return "sprint/view";
    }
    
    @GetMapping("/sprint/{id}/edit")
    public String editSprint(@PathVariable String id, Model model) {
        model.addAttribute("sprint", sprintService.getSprintWithFullDetails(id));
        model.addAttribute("pageTitle", "Edit Sprint");
        return "sprint/edit";
    }
    
    @GetMapping("/sprint/{id}/timeline")
    public String sprintTimeline(@PathVariable String id, Model model) {
        model.addAttribute("sprint", sprintService.getSprintWithFullDetails(id));
        model.addAttribute("pageTitle", "Sprint Timeline");
        return "sprint/timeline";
    }
    
    @GetMapping("/team")
    public String teamManagement(Model model) {
        model.addAttribute("pageTitle", "Team Management");
        return "team/index";
    }
    
    @GetMapping("/tasks")
    public String taskManagement(Model model) {
        model.addAttribute("pageTitle", "Task Management");
        return "tasks/index";
    }
    
    @GetMapping("/tasks/import")
    public String importTasks(Model model) {
        model.addAttribute("pageTitle", "Import Tasks");
        return "tasks/import";
    }
    
    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("pageTitle", "Analytics");
        model.addAttribute("completedSprints", sprintService.getCompletedSprints());
        return "analytics/index";
    }
    
    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("completedSprints", sprintService.getCompletedSprints());
        model.addAttribute("pageTitle", "Sprint History");
        return "history/index";
    }
    
    @GetMapping("/holidays")
    public String holidayManagement(Model model) {
        model.addAttribute("pageTitle", "Holiday Management");
        return "configuration/holidays";
    }
    
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Settings");
        return "configuration/settings";
    }

    @GetMapping("/export")
    public String exportCenter(Model model) {
        model.addAttribute("pageTitle", "Export Center");
        return "export/index";
    }
    
    @GetMapping("/test")
    @ResponseBody
    public String testEndpoint() {
        return "PageController is working! Current time: " + java.time.LocalDateTime.now();
    }
}
