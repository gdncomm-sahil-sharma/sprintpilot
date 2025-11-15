export enum Screen {
  Dashboard,
  SprintSetup,
  TaskPlanning,
  Export,
  History,
  TeamConfig,
  HolidayMaster,
}

export enum Role {
  Backend = 'Backend',
  Frontend = 'Frontend',
  QA = 'QA',
  DevOps = 'DevOps',
  Manager = 'Manager',
  Designer = 'Designer',
}

export interface TeamMember {
  id: string;
  name: string;
  role: Role;
  dailyCapacity: number; // hours per day
  leaveDays: string[]; // array of ISO date strings
}

export interface SprintDeployment {
  id:string;
  name: string;
  date: string; // ISO date string
}

export interface MasterHoliday {
    id: string;
    name: string;
    date: string; // ISO date string
}

export type MeetingType = 'Planning' | 'Grooming' | 'Retrospective';

export interface SprintMeeting {
  id: string;
  type: MeetingType;
  date: string; // ISO date string
  time: string; // HH:mm
  duration: number; // in minutes
}

export interface SprintSettings {
  startDate: string; // ISO date string
  duration: number; // in working days
  endDate: string; // ISO date string, calculated
  freezeDate?: string; // ISO date string
  publicHolidays: string[]; // array of ISO date strings for sprint-specific holidays
  deployments: SprintDeployment[];
  meetings: SprintMeeting[];
}

export interface JiraTask {
  id: string;
  key: string;
  summary: string;
  storyPoints: number; // Equivalent to hours for this app
  category: string; // Expected: FEATURE, TECH_DEBT, PROD_ISSUE
  assigneeId?: string;
  startDate?: string; // ISO date string
  dueDate?: string; // ISO date string
  timeSpent?: number; // hours
}

export interface TaskRisk {
  taskId: string;
  riskLevel: 'On Track' | 'At Risk' | 'Off Track';
  reason: string;
}

export interface CapacitySummary {
    memberId: string;
    memberName: string;
    totalCapacity: number;
    assignedHours: number;
    remainingHours: number;
    status: 'OK' | 'Overloaded' | 'Underutilized';
}

export interface SprintData {
  id: string;
  settings: SprintSettings;
  team: TeamMember[];
  tasks: JiraTask[];
}