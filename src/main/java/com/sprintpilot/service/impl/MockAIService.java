package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.AIService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@ConditionalOnProperty(name = "app.ai.mock-mode", havingValue = "true")
public class MockAIService implements AIService {
    
    @Override
    public String generateSprintSummary(List<TeamMemberDto> team, SprintDto sprint, 
                                       List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        return """
            **Sprint Summary (Mock Response)**
            
            - **Primary Focus:** This sprint focuses on delivering key features for the upcoming release, 
              with emphasis on user dashboard improvements and API optimization.
            
            - **Workload Balance:** Team capacity is well-balanced at 85% utilization. 
              Backend team is at 90% capacity, Frontend at 82%, and QA at 78%.
            
            - **Potential Risks:** 
              - Bob (Backend) is slightly overloaded at 95% capacity
              - Critical dependency on external API integration needs monitoring
              - Two tasks are at risk due to unclear requirements
            
            - **Overall Assessment:** The sprint is well-planned with achievable goals. 
              Consider redistributing 1-2 tasks from Bob to maintain sustainable pace.
            """;
    }
    
    @Override
    public String generateMeetingInvite(SprintEventDto meeting, SprintDto sprint) {
        String meetingType = meeting.eventSubtype() != null ? 
                           meeting.eventSubtype().toString() : "Sprint Meeting";
        
        return String.format("""
            Subject: %s - Sprint %s to %s
            
            Dear Team,
            
            You are invited to attend our %s session for the upcoming sprint.
            
            **Meeting Details:**
            - Date: %s
            - Time: %s
            - Duration: %d minutes
            - Location: Conference Room / Teams Link
            
            **Agenda:**
            - Review of previous sprint outcomes
            - Discussion of sprint goals and objectives
            - Task estimation and assignment
            - Risk identification and mitigation planning
            - Q&A and open discussion
            
            Please come prepared with your updates and any blockers you're facing.
            
            Looking forward to a productive session!
            
            Best regards,
            Sprint Management Team
            """, 
            meetingType, sprint.startDate(), sprint.endDate(),
            meetingType.toLowerCase(), meeting.eventDate(), 
            meeting.eventTime(), meeting.durationMinutes());
    }
    
    @Override
    public String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks) {
        return """
            **Risk Analysis Summary (Mock Response)**
            
            **Critical Issues (Off Track):**
            - PROJ-105: Database migration script failing - Immediate attention required
            - PROJ-108: Production API timeout issues - Blocking downstream tasks
            
            **At Risk Items:**
            - PROJ-110: UI components delayed due to design changes
            - PROJ-112: Integration testing blocked by environment issues
            - PROJ-115: Performance optimization needs additional resources
            
            **Pattern Analysis:**
            - Multiple backend tasks are at risk due to shared dependencies
            - QA tasks are blocked waiting for development completion
            - Technical debt items are being deprioritized
            
            **Recommendations:**
            1. Schedule emergency meeting for PROJ-105 resolution
            2. Allocate additional backend resources to unblock dependencies
            3. Consider moving non-critical features to next sprint
            4. Daily standup focus on at-risk items until resolved
            """;
    }
    
    @Override
    public String generateConfluencePage(List<TeamMemberDto> team, SprintDto sprint, 
                                        List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        return String.format("""
            h1. Sprint Plan: %s to %s
            
            h2. Sprint Goals
            * Deliver core features for Q4 release
            * Complete technical debt reduction initiative
            * Improve system performance by 20%%
            * Fix all P1 production issues
            
            h2. Team Capacity
            ||Team Member||Role||Capacity (hours)||Assigned (hours)||Status||
            |Alice|Frontend|60|51|OK|
            |Bob|Backend|70|66|OK|
            |Charlie|QA|50|40|Underutilized|
            |David|Backend|70|63|OK|
            
            h2. Sprint Metrics
            * Total Story Points: 125
            * Team Velocity (3-sprint avg): 110
            * Sprint Duration: %d working days
            * Total Capacity: 250 hours
            * Committed Work: 220 hours
            
            h2. Work Items
            
            h3. Features (60%%)
            * [PROJ-201] Implement new dashboard - 40h (Alice)
            * [PROJ-202] API v2 endpoints - 32h (Bob)
            * [PROJ-203] Mobile responsive design - 24h (Alice)
            
            h3. Technical Debt (25%%)
            * [PROJ-210] Refactor authentication module - 20h (David)
            * [PROJ-211] Database query optimization - 16h (Bob)
            
            h3. Bug Fixes (15%%)
            * [PROJ-220] Fix payment gateway timeout - 12h (David)
            * [PROJ-221] Resolve UI rendering issues - 8h (Alice)
            
            h2. Dependencies and Risks
            * External API integration pending vendor confirmation
            * New framework version requires team training
            * Production deployment window limited to weekends
            
            h2. Success Criteria
            * All committed stories completed and tested
            * Zero P1 bugs in production
            * Performance metrics meet target thresholds
            * Team satisfaction score > 4.0
            """, sprint.startDate(), sprint.endDate(), sprint.duration());
    }
    
    @Override
    public String generateTeamsMessage(List<TeamMemberDto> team, SprintDto sprint, 
                                      List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        return String.format("""
            **🚀 Sprint Kick-off Alert! 🚀**
            
            Hey Team! We're starting our new sprint today (%s to %s)!
            
            **📅 Sprint Duration:** %d working days
            
            **🎯 Key Focus Areas:**
            • 🆕 Launch the new user dashboard with real-time analytics
            • ⚡ Improve API response time by 30%%
            • 🐛 Squash those pesky production bugs
            • 🛠️ Pay down technical debt in the authentication module
            
            **💪 Team Strength:** %d amazing engineers ready to deliver!
            
            **📊 Sprint Stats:**
            • Total Story Points: 125
            • Features: 75 points
            • Tech Debt: 31 points
            • Bug Fixes: 19 points
            
            Remember: Daily standups at 9:30 AM sharp! 
            Let's make this sprint our best one yet! 
            
            Together we build, together we ship! 🎉
            
            #SprintGoals #TeamWork #LetsDoThis
            """, sprint.startDate(), sprint.endDate(), sprint.duration(), team.size());
    }
    
    @Override
    public String generateOutlookBody(List<TeamMemberDto> team, SprintDto sprint, 
                                     List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        return String.format("""
            Subject: Sprint Planning Meeting - %s to %s
            
            Dear Team,
            
            This is an invitation to our Sprint Planning meeting for the upcoming sprint cycle.
            
            **Sprint Overview:**
            - Start Date: %s
            - End Date: %s
            - Duration: %d working days
            - Team Size: %d members
            - Total Capacity: 250 hours
            
            **Meeting Agenda:**
            
            1. **Sprint Retrospective Review** (15 mins)
               - Review action items from last sprint
               - Discuss what went well and areas for improvement
            
            2. **Sprint Goals Presentation** (10 mins)
               - Product Owner to present sprint objectives
               - Clarify success criteria
            
            3. **Backlog Review and Estimation** (45 mins)
               - Review prioritized backlog items
               - Story point estimation using planning poker
               - Identify dependencies and risks
            
            4. **Task Assignment and Commitment** (30 mins)
               - Voluntary task assignment based on expertise
               - Capacity planning and workload balancing
               - Final commitment to sprint backlog
            
            5. **Risk Mitigation Planning** (15 mins)
               - Identify potential blockers
               - Define mitigation strategies
            
            6. **Q&A and Open Discussion** (15 mins)
               - Address any concerns or questions
               - Align on communication plan
            
            **Preparation Required:**
            - Review the product backlog in Jira
            - Update your capacity for the sprint period
            - Come prepared with questions about unclear requirements
            
            Please confirm your attendance. If you cannot attend, please notify me so we can arrange an alternative.
            
            Best regards,
            Scrum Master
            """, sprint.startDate(), sprint.endDate(), 
            sprint.startDate(), sprint.endDate(), sprint.duration(), team.size());
    }
    
    @Override
    public String analyzeHistoricalPerformance(List<SprintDto> sprints, 
                                              List<Map<String, Object>> velocityTrend,
                                              List<Map<String, Object>> workMixTrend,
                                              List<Map<String, Object>> roleUtilization) {
        return """
            **Historical Performance Analysis (Mock Response)**
            
            **📈 Overall Performance:**
            - Team velocity has increased by 18% over the last 5 sprints
            - Consistent delivery with 92% sprint completion rate
            - Velocity stabilizing around 115-125 story points per sprint
            - Sprint predictability improved from 75% to 88%
            
            **🎯 Strategic Focus:**
            - Healthy work mix: 60% features, 25% tech debt, 15% bugs
            - Technical debt reduction initiative showing positive results
            - Feature delivery aligned with product roadmap
            - Bug count decreasing sprint-over-sprint
            
            **👥 Team Health & Bottlenecks:**
            - Backend team consistently at 95% utilization (risk of burnout)
            - Frontend team at optimal 85% utilization
            - QA team underutilized at 70% (opportunity for cross-training)
            - DevOps capacity constraint affecting deployment frequency
            
            **💡 Recommendations:**
            1. **Immediate Actions:**
               - Add 1 backend engineer to distribute workload
               - Implement pair programming to share knowledge
               - Schedule technical debt sprints quarterly
            
            2. **Long-term Improvements:**
               - Invest in test automation to optimize QA capacity
               - Cross-train team members for better flexibility
               - Implement continuous deployment to reduce DevOps bottleneck
               - Consider 15% buffer for unplanned work
            
            **🎊 Achievements:**
            - Zero production incidents in last 2 sprints
            - Customer satisfaction score increased to 4.5/5
            - Team morale at all-time high (4.6/5)
            - Knowledge sharing sessions proving effective
            """;
    }
    
    @Override
    public Map<String, String> generateMeetingDetails(SprintEventDto meeting, SprintDto sprint) {
        Map<String, String> details = new HashMap<>();
        
        String meetingType = meeting.eventSubtype() != null ? 
                           meeting.eventSubtype().toString() : "Sprint Meeting";
        
        details.put("subject", String.format("%s - Sprint %s to %s", 
                   meetingType, sprint.startDate(), sprint.endDate()));
        
        details.put("body", generateMeetingInvite(meeting, sprint));
        
        return details;
    }
}
