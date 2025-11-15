import { GoogleGenAI, Type } from "@google/genai";
import { TeamMember, SprintSettings, JiraTask, CapacitySummary, TaskRisk, SprintData, SprintMeeting, MeetingType } from '../types';

if (!process.env.API_KEY) {
  console.warn("API_KEY environment variable not set. AI features will not work.");
}

const ai = new GoogleGenAI({ apiKey: process.env.API_KEY! });

const generateSprintDataPrompt = (
    team: TeamMember[],
    sprint: SprintSettings,
    tasks: JiraTask[],
    workload: CapacitySummary[]
): string => {
    const teamDetails = team.map(m => `- ${m.name} (${m.role})`).join('\n');
    const taskDetails = tasks.map(t => `- ${t.key}: ${t.summary} (${t.storyPoints}h) - Assigned: ${team.find(tm => tm.id === t.assigneeId)?.name || 'Unassigned'}`).join('\n');
    const workloadDetails = workload.map(w => `- ${w.memberName}: Capacity ${w.totalCapacity}h, Assigned ${w.assignedHours}h, Status: ${w.status}`).join('\n');

    return `
      **Sprint Details:**
      - Start Date: ${sprint.startDate}
      - End Date: ${sprint.endDate}
      - Duration: ${sprint.duration} working days

      **Team Members:**
      ${teamDetails}

      **Sprint Tasks:**
      ${taskDetails}

      **Workload Allocation:**
      ${workloadDetails}
    `;
};


export const generateSprintSummary = async (
    team: TeamMember[],
    sprint: SprintSettings,
    tasks: JiraTask[],
    workload: CapacitySummary[]
): Promise<string> => {
    if (!process.env.API_KEY) {
        return "AI Summary is unavailable. API key is not configured.";
    }

    try {
        const basePrompt = generateSprintDataPrompt(team, sprint, tasks, workload);
        const fullPrompt = `
          Analyze the following sprint data and generate a concise summary for an engineering manager.

          ${basePrompt}

          **Your Task:**
          Generate a summary as a readable, bulleted list. The summary should be easy to scan and highlight key information. Include the following points using markdown bullet points (e.g., "- Point 1"):
          - **Primary Focus:** Briefly describe the main goal or theme of the sprint based on the work items.
          - **Workload Balance:** Comment on the team's capacity vs. assigned work. Mention any members who are overloaded or underutilized.
          - **Potential Risks:** Identify any potential risks, such as a high workload on a specific team member or a large amount of unplanned work.
          - **Overall Assessment:** Provide a brief, overall assessment of the sprint plan.
        `;
        const response = await ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: fullPrompt,
        });

        return response.text;
    } catch (error) {
        console.error("Error generating sprint summary:", error);
        return "An error occurred while generating the AI summary. Please check the console for details.";
    }
};

export const generateRiskSummary = async (tasks: JiraTask[], risks: TaskRisk[]): Promise<string> => {
    if (!process.env.API_KEY) {
        throw new Error("API key is not configured.");
    }
    try {
        const taskDetails = tasks.map(task => {
            const risk = risks.find(r => r.taskId === task.id);
            return {
                key: task.key,
                summary: task.summary,
                storyPoints: task.storyPoints,
                riskLevel: risk?.riskLevel || 'Unknown',
                reason: risk?.reason || 'Not analyzed',
            };
        });

        const prompt = `
            You are an expert project manager. I have analyzed a sprint and have the following tasks with their calculated risk levels.
            Your task is to provide a high-level summary for an engineering manager.
            
            Analyze the data below, identify patterns, highlight the most critical issues, and provide a brief, actionable overall assessment.
            Focus on what a manager needs to know to get the sprint back on track.

            - Point out the most severe blockers ('Off Track' items).
            - Identify any potential bottlenecks or patterns in the 'At Risk' items (e.g., are multiple at-risk tasks assigned to one person or of a similar type?).
            - Conclude with a brief overall health assessment of the sprint.
            - Format your response using clear, concise markdown bullet points.

            Here is the risk data:
            ${JSON.stringify(taskDetails, null, 2)}
        `;
        
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash',
            contents: prompt,
        });

        return response.text;

    } catch (error) {
        console.error("Error generating risk summary:", error);
        throw new Error("An error occurred while generating the AI risk summary.");
    }
};


export const generateConfluencePage = async (
    team: TeamMember[],
    sprint: SprintSettings,
    tasks: JiraTask[],
    workload: CapacitySummary[]
): Promise<string> => {
     if (!process.env.API_KEY) return "AI is unavailable. API key is not configured.";
     try {
        const basePrompt = generateSprintDataPrompt(team, sprint, tasks, workload);
        const fullPrompt = `
        Based on the sprint data below, generate a Confluence page content using Confluence wiki markup.

        ${basePrompt}

        **Your Task:**
        Create a well-structured Confluence page with the following sections:
        1. A main title (h1) for the sprint plan including the dates.
        2. A "Sprint Goals" section (h2) with 2-3 plausible, high-level goals based on the task list.
        3. A "Team Capacity" section (h2) summarizing the workload.
        4. A "Work Items" section (h2) listing all tasks with their key, summary, hours, and assignee using bullet points (*).
        `;
        const response = await ai.models.generateContent({ model: 'gemini-2.5-flash', contents: fullPrompt });
        return response.text;
    } catch (error) {
        console.error("Error generating Confluence page:", error);
        return "An error occurred while generating the Confluence page content.";
    }
};

export const generateTeamsMessage = async (
    team: TeamMember[],
    sprint: SprintSettings,
    tasks: JiraTask[],
    workload: CapacitySummary[]
): Promise<string> => {
     if (!process.env.API_KEY) return "AI is unavailable. API key is not configured.";
     try {
        const basePrompt = generateSprintDataPrompt(team, sprint, tasks, workload);
        const fullPrompt = `
        Based on the sprint data below, generate a Microsoft Teams announcement message.

        ${basePrompt}

        **Your Task:**
        Create a concise and engaging Teams announcement for a sprint kick-off using Teams markdown (e.g., **bold**).
        - Start with a cheerful kick-off message.
        - State the sprint duration clearly.
        - List 2-3 key focus areas or goals for the sprint in bullet points.
        - End with a motivating closing sentence.
        `;
        const response = await ai.models.generateContent({ model: 'gemini-2.5-flash', contents: fullPrompt });
        return response.text;
    } catch (error) {
        console.error("Error generating Teams message:", error);
        return "An error occurred while generating the Teams message.";
    }
};

export const generateOutlookInvite = async (
    team: TeamMember[],
    sprint: SprintSettings,
    tasks: JiraTask[],
    workload: CapacitySummary[]
): Promise<string> => {
     if (!process.env.API_KEY) return "AI is unavailable. API key is not configured.";
     try {
        const basePrompt = generateSprintDataPrompt(team, sprint, tasks, workload);
        const fullPrompt = `
        Based on the sprint data below, generate the body text for an Outlook meeting invite for the sprint planning session.

        ${basePrompt}

        **Your Task:**
        Generate text suitable for an Outlook invite.
        - Start with a clear subject line like "Subject: Sprint Planning: [Start Date] - [End Date]".
        - Write a brief introductory sentence.
        - Include a simple agenda with items like reviewing the previous sprint, discussing goals, and assigning tasks.
        - End with a professional closing.
        `;
        const response = await ai.models.generateContent({ model: 'gemini-2.5-flash', contents: fullPrompt });
        return response.text;
    } catch (error) {
        console.error("Error generating Outlook invite:", error);
        return "An error occurred while generating the Outlook invite text.";
    }
};

export const generateMeetingInvite = async (
    meeting: SprintMeeting,
    sprint: SprintSettings,
): Promise<{ subject: string, body: string }> => {
    if (!process.env.API_KEY) {
        return { subject: "Meeting Invite Generation Failed", body: "AI is unavailable. API key is not configured." };
    }
    
    const getMeetingDetails = (type: MeetingType) => {
        switch(type) {
            case 'Planning':
                return {
                    title: `Sprint Planning: ${sprint.startDate} to ${sprint.endDate}`,
                    purpose: "To plan the upcoming sprint, discuss goals, and finalize the backlog.",
                    agenda: "- Review sprint goals\n- Discuss team capacity\n- Select and estimate tasks\n- Finalize sprint backlog"
                };
            case 'Grooming':
                return {
                    title: "Backlog Grooming / Refinement",
                    purpose: "To review and prepare upcoming user stories for future sprints.",
                    agenda: "- Review top priority items in the backlog\n- Clarify requirements and acceptance criteria\n- Add estimations\n- Identify dependencies"
                };
            case 'Retrospective':
                 return {
                    title: `Sprint Retrospective: ${sprint.startDate} to ${sprint.endDate}`,
                    purpose: "To reflect on the past sprint and identify areas for improvement.",
                    agenda: "- What went well?\n- What could be improved?\n- Action items for the next sprint"
                };
        }
    }
    const details = getMeetingDetails(meeting.type);

    try {
        const prompt = `
            Generate a JSON object for a professional and concise Outlook meeting invite with "subject" and "body" keys.

            **Meeting Details:**
            - Type: ${meeting.type}
            - Date: ${meeting.date}
            - Time: ${meeting.time}
            - Duration: ${meeting.duration} minutes
            - Sprint Dates: ${sprint.startDate} to ${sprint.endDate}
            - Suggested Title: ${details.title}
            - Purpose: ${details.purpose}
            - Suggested Agenda:\\n${details.agenda}

            **Your Task:**
            Create a JSON object with two keys: "subject" and "body".
            1. The "subject" value should be the suggested meeting title.
            2. The "body" value should contain a brief, friendly introductory sentence stating the meeting's purpose, followed by a simple agenda (using newline characters for line breaks), and a professional closing line.
        `;
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash',
            contents: prompt,
            config: {
                responseMimeType: "application/json",
                responseSchema: {
                    type: Type.OBJECT,
                    properties: {
                        subject: { type: Type.STRING, description: 'The meeting subject line.' },
                        body: { type: Type.STRING, description: 'The body of the meeting invite.' }
                    },
                    required: ['subject', 'body']
                }
            }
        });

        const jsonText = response.text;
        return JSON.parse(jsonText);

    } catch (error) {
        console.error(`Error generating ${meeting.type} invite:`, error);
        return {
            subject: details.title,
            body: `Hi Team,\n\nThis is an invitation for our upcoming ${meeting.type} session.\n\n${details.purpose}\n\nAgenda:\n${details.agenda}\n\nLooking forward to a productive session.`
        };
    }
};

export const generatePerformanceInsights = async (
    sprints: SprintData[],
    velocityTrend: { sprintId: string; endDate: string; totalHours: number }[],
    workMixTrend: { sprintId: string; endDate: string; mix: { [key: string]: number } }[],
    roleUtilization: { role: string; utilization: number }[]
): Promise<string> => {
    if (!process.env.API_KEY) return "AI is unavailable. API key is not configured.";

    try {
        const velocityText = velocityTrend.map(v => `- Sprint ending ${v.endDate}: ${v.totalHours.toFixed(0)} hours`).join('\n');
        const workMixText = workMixTrend.map(wm => {
            const mixDetails = Object.entries(wm.mix).map(([cat, perc]) => `${cat}: ${perc.toFixed(0)}%`).join(', ');
            return `- Sprint ending ${wm.endDate}: ${mixDetails}`;
        }).join('\n');
        const utilizationText = roleUtilization.map(r => `- ${r.role}: ${r.utilization.toFixed(0)}%`).join('\n');

        const prompt = `
            You are an experienced Agile Coach and Engineering Manager. Analyze the following historical sprint data for my team.

            **Data Overview:**
            - Number of sprints analyzed: ${sprints.length}
            - Time period: ${sprints[0]?.settings.startDate || 'N/A'} to ${sprints[sprints.length - 1]?.settings.endDate || 'N/A'}

            **Team Velocity Trend (Total Hours Delivered Per Sprint):**
            ${velocityText}

            **Work Mix Trend (% of work by category per sprint):**
            ${workMixText}

            **Average Role Utilization (across all selected sprints):**
            ${utilizationText}

            **Your Task:**
            Provide actionable insights based on these trends for an engineering manager. Use markdown bullet points.
            - **Overall Performance:** Summarize the team's velocity and delivery consistency. Is it improving, declining, or erratic?
            - **Strategic Focus:** Comment on the work mix. Is there a healthy balance between new features, tech debt, and production issues? Are there any worrying trends (e.g., zero tech debt work for multiple sprints)?
            - **Team Health & Bottlenecks:** Identify any roles that are consistently overloaded (>100%) or underutilized (<70%). What are the risks associated with this (e.g., burnout, boredom, skill gaps)?
            - **Recommendations:** Suggest 1-2 concrete, actionable steps the manager can take to address any identified issues and help the team improve in the next sprint.
        `;

        const response = await ai.models.generateContent({ model: 'gemini-2.5-flash', contents: prompt });
        return response.text;

    } catch (error) {
        console.error("Error generating performance insights:", error);
        return "An error occurred while generating the AI performance insights.";
    }
};