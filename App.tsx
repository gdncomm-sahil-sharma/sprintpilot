

import React, { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Screen, TeamMember, SprintSettings, JiraTask, Role, CapacitySummary, SprintDeployment, SprintData, MasterHoliday, TaskRisk, MeetingType, SprintMeeting } from './types';
import { generateSprintSummary, generateConfluencePage, generateTeamsMessage, generateOutlookInvite, generateRiskSummary, generatePerformanceInsights, generateMeetingInvite } from './services/geminiService';
import * as holidayService from './services/holidayService';
import { EditIcon, DeleteIcon, CalendarIcon, CopyIcon, CheckIcon, UsersIcon, CalendarDaysIcon, DocumentArrowUpIcon, ChartPieIcon, SparklesIcon, PaperAirplaneIcon, FlagIcon, PlusIcon, PlayIcon, ClockIcon, ArchiveBoxArrowDownIcon, GlobeAltIcon, BuildingLibraryIcon, JiraIcon, ShieldExclamationIcon, InformationCircleIcon, ChartBarIcon, EyeIcon } from './components/icons';

// --- Helper & Mock Data ---
const createNewSprint = (): SprintData => {
    const startDate = new Date();
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 13); // Default to roughly 10 working days
    return {
        id: `sprint-${Date.now()}`,
        settings: {
            startDate: startDate.toISOString().split('T')[0],
            duration: 10,
            endDate: endDate.toISOString().split('T')[0],
            publicHolidays: [],
            deployments: [],
            meetings: [],
        },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: [] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
            { id: '3', name: 'Charlie', role: Role.QA, dailyCapacity: 5, leaveDays: [] },
        ],
        tasks: [],
    };
};

const mockHistory: SprintData[] = [
    {
        id: 'sprint-hist-1',
        settings: { startDate: '2024-07-01', duration: 10, endDate: '2024-07-12', publicHolidays: [], deployments: [], meetings: [] },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: [] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
        ],
        tasks: [ { id: 't1', key: 'PROJ-98', summary: 'Launch page redesign', storyPoints: 24, category: 'FEATURE', assigneeId: '1' }, { id: 't2', key: 'PROJ-99', summary: 'API for launch page', storyPoints: 16, category: 'FEATURE', assigneeId: '2' } ]
    },
    {
        id: 'sprint-hist-2',
        settings: { startDate: '2024-07-15', duration: 10, endDate: '2024-07-26', publicHolidays: [], deployments: [{id: 'd1', name: 'Mid-sprint deploy', date: '2024-07-22'}], meetings: [] },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: [] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
            { id: '4', name: 'David', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
        ],
        tasks: [ 
            { id: 't3', key: 'PROJ-105', summary: 'Refactor database schema', storyPoints: 30, category: 'TECH_DEBT', assigneeId: '2' },
            { id: 't4', key: 'PROJ-106', summary: 'Build new caching layer', storyPoints: 25, category: 'TECH_DEBT', assigneeId: '4' },
            { id: 't5', key: 'PROJ-107', summary: 'UI adjustments for new data', storyPoints: 8, category: 'FEATURE', assigneeId: '1' } 
        ]
    },
    {
        id: 'sprint-hist-3',
        settings: { startDate: '2024-07-29', duration: 10, endDate: '2024-08-09', publicHolidays: [], deployments: [], meetings: [] },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: ['2024-08-01', '2024-08-02'] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
            { id: '3', name: 'Charlie', role: Role.QA, dailyCapacity: 5, leaveDays: [] },
        ],
        tasks: [ 
            { id: 't6', key: 'PROJ-110', summary: 'Critical login bug investigation', storyPoints: 20, category: 'PROD_ISSUE', assigneeId: '2' },
            { id: 't7', key: 'PROJ-111', summary: 'Regression testing for checkout flow', storyPoints: 25, category: 'PROD_ISSUE', assigneeId: '3' },
            { id: 't8', key: 'PROJ-112', summary: 'Display error messages gracefully', storyPoints: 10, category: 'FEATURE', assigneeId: '1' } 
        ]
    },
    {
        id: 'sprint-hist-4',
        settings: { startDate: '2024-08-12', duration: 10, endDate: '2024-08-23', publicHolidays: ['2024-08-15'], deployments: [], meetings: [] },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: [] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
            { id: '3', name: 'Charlie', role: Role.QA, dailyCapacity: 5, leaveDays: [] },
            { id: '4', name: 'David', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
        ],
        tasks: [ 
            { id: 't9', key: 'PROJ-120', summary: 'Implement new user dashboard', storyPoints: 40, category: 'FEATURE', assigneeId: '1' },
            { id: 't10', key: 'PROJ-121', summary: 'Backend APIs for dashboard', storyPoints: 32, category: 'FEATURE', assigneeId: '2' },
            { id: 't11', key: 'PROJ-122', summary: 'E2E tests for dashboard', storyPoints: 18, category: 'FEATURE', assigneeId: '3' } 
        ]
    },
    {
        id: 'sprint-hist-5',
        settings: { startDate: '2024-08-26', duration: 10, endDate: '2024-09-06', publicHolidays: [], deployments: [], meetings: [] },
        team: [
            { id: '1', name: 'Alice', role: Role.Frontend, dailyCapacity: 6, leaveDays: [] },
            { id: '2', name: 'Bob', role: Role.Backend, dailyCapacity: 7, leaveDays: [] },
        ],
        tasks: [ 
            { id: 't12', key: 'PROJ-130', summary: 'Performance optimization for mobile', storyPoints: 35, category: 'FEATURE', assigneeId: '1' },
            { id: 't13', key: 'PROJ-131', summary: 'Optimize database queries', storyPoints: 20, category: 'TECH_DEBT', assigneeId: '2' }
        ]
    }
];


const initialMasterHolidays: MasterHoliday[] = [
    { id: 'mh-1', name: 'New Year\'s Day', date: '2024-01-01' },
    { id: 'mh-2', name: 'Good Friday', date: '2024-03-29' },
    { id: 'mh-3', name: 'Labor Day', date: '2024-09-02' },
    { id: 'mh-4', name: 'Thanksgiving Day', date: '2024-11-28' },
    { id: 'mh-5', name: 'Christmas Day', date: '2024-12-25' },
];

const getWorkingDays = (start: Date, end: Date, holidays: string[]): number => {
    let count = 0;
    const curDate = new Date(start.getTime());
    const holidaySet = new Set(holidays);
    while (curDate <= end) {
        const dayOfWeek = curDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(curDate.toISOString().split('T')[0])) {
            count++;
        }
        curDate.setDate(curDate.getDate() + 1);
    }
    return count;
};

const addWorkingDays = (startDate: Date, days: number, holidays: string[]): Date => {
    let count = 0;
    const newDate = new Date(startDate.getTime());
    const holidaySet = new Set(holidays);
    
    // Find the first valid working day to start counting from
    while (true) {
        const dayOfWeek = newDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(newDate.toISOString().split('T')[0])) {
            break;
        }
        newDate.setDate(newDate.getDate() + 1);
    }

    while (count < days) {
        newDate.setDate(newDate.getDate() + 1);
        const dayOfWeek = newDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(newDate.toISOString().split('T')[0])) {
            count++;
        }
    }
    return newDate;
};


const calculateCapacitySummary = (sprintData: SprintData | null, masterHolidays: MasterHoliday[]): CapacitySummary[] => {
    if (!sprintData) return [];
    const { settings, team, tasks } = sprintData;
    const start = new Date(settings.startDate);
    const end = new Date(settings.endDate);
    const allHolidays = Array.from(new Set([...settings.publicHolidays, ...masterHolidays.map(h => h.date)]));
    const totalWorkingDays = getWorkingDays(start, end, allHolidays);
    return team.map(member => {
        const memberWorkingDays = totalWorkingDays - member.leaveDays.length;
        const totalCapacity = memberWorkingDays * member.dailyCapacity;
        const assignedHours = tasks
            .filter(task => task.assigneeId === member.id)
            .reduce((acc, task) => acc + task.storyPoints, 0);
        const remainingHours = totalCapacity - assignedHours;
        const utilization = totalCapacity > 0 ? (assignedHours / totalCapacity) * 100 : 0;
        let status: CapacitySummary['status'] = 'OK';
        if (utilization > 100) status = 'Overloaded';
        else if (utilization < 70) status = 'Underutilized';
        return {
            memberId: member.id,
            memberName: member.name,
            totalCapacity,
            assignedHours,
            remainingHours,
            status,
        };
    });
};

const calculateTaskRisk = (task: JiraTask): TaskRisk => {
    const today = new Date();
    const startDate = new Date(task.startDate!);
    const dueDate = new Date(task.dueDate!);

    if (today > dueDate && task.timeSpent! < task.storyPoints) {
        return { taskId: task.id, riskLevel: 'Off Track', reason: 'Task is past its due date but is not complete.' };
    }

    const totalTime = dueDate.getTime() - startDate.getTime();
    const elapsedTime = today.getTime() - startDate.getTime();

    if (totalTime <= 0) { // Due date is same or before start date
        return { taskId: task.id, riskLevel: 'On Track', reason: 'Progress is aligned with timeline.' };
    }

    const timeElapsedRatio = Math.max(0, elapsedTime / totalTime);
    const workCompletedRatio = (task.timeSpent! || 0) / task.storyPoints;

    if (timeElapsedRatio > 0.6 && workCompletedRatio < timeElapsedRatio * 0.75) {
        return { taskId: task.id, riskLevel: 'At Risk', reason: 'Progress is significantly lagging behind the timeline.' };
    }

    return { taskId: task.id, riskLevel: 'On Track', reason: 'Progress is aligned with the timeline.' };
};

// --- Sub-Components ---
const Header: React.FC<{
  currentScreen: Screen;
  setCurrentScreen: (screen: Screen) => void;
  sprintActive: boolean;
}> = ({ currentScreen, setCurrentScreen, sprintActive }) => (
  <header className="bg-white/70 backdrop-blur-lg sticky top-0 z-10 border-b border-gray-200">
    <div className="container mx-auto px-6 py-3 flex justify-between items-center">
      <div onClick={() => sprintActive && setCurrentScreen(Screen.Dashboard)} className={`flex items-center space-x-2 ${sprintActive ? 'cursor-pointer' : ''}`}>
        <span className="text-3xl">🚀</span>
        <h1 className="text-2xl font-bold text-primary-800">SprintPilot</h1>
      </div>
      {sprintActive && (
      <nav className="hidden md:flex items-center space-x-2 bg-gray-100 p-1 rounded-lg">
          {(Object.keys(Screen).filter(k => isNaN(Number(k))))
            .map(screenName => {
              const screenValue = Screen[screenName as keyof typeof Screen];
              const isActive = currentScreen === screenValue;
              return (
                <button 
                  key={screenName}
                  onClick={() => setCurrentScreen(screenValue)}
                  className={`px-4 py-1.5 rounded-md text-sm font-semibold transition-colors duration-200 ${
                    isActive ? "bg-white text-primary-600 shadow-sm" : "text-gray-600 hover:bg-white/60 hover:text-primary-600"
                  }`}
                >
                  {screenName.replace(/([A-Z])/g, ' $1').trim()}
                </button>
              )
          })}
      </nav>
      )}
    </div>
  </header>
);

const Card: React.FC<{ children: React.ReactNode; className?: string; onClick?: () => void; }> = ({ children, className = '', onClick }) => (
    <div onClick={onClick} className={`bg-white rounded-xl shadow-sm border border-gray-200/80 p-6 ${className} ${onClick ? 'cursor-pointer' : ''}`}>
        {children}
    </div>
);

const Button: React.FC<{ onClick: (e: React.MouseEvent<HTMLButtonElement>) => void; children: React.ReactNode; variant?: 'primary' | 'secondary' | 'danger'; disabled?: boolean; }> = ({ onClick, children, variant = 'primary', disabled = false }) => {
    const baseClasses = "px-5 py-2.5 rounded-lg font-semibold text-sm transition-all duration-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 flex items-center justify-center space-x-2";
    const variantClasses = {
        primary: 'bg-primary-600 hover:bg-primary-700 text-white focus:ring-primary-500 disabled:bg-primary-300',
        secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-800 focus:ring-gray-400 disabled:bg-gray-100',
        danger: 'bg-danger hover:bg-red-600 text-white focus:ring-red-500 disabled:bg-red-300',
    };
    
    const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault();
        onClick(e);
    }

    return <button type="button" onClick={handleClick} disabled={disabled} className={`${baseClasses} ${variantClasses[variant]}`}>{children}</button>;
};

const WorkDistributionChart: React.FC<{ tasks: JiraTask[] }> = ({ tasks }) => {
    const categoryConfig = { FEATURE: { label: 'Feature', color: '#4f46e5' }, TECH_DEBT: { label: 'Tech Debt', color: '#f59e0b' }, PROD_ISSUE: { label: 'Prod Issue', color: '#ef4444' }, OTHER: { label: 'Other', color: '#6b7280' } };
    const stats = useMemo(() => {
        const categoryPoints: { [key: string]: number } = { FEATURE: 0, TECH_DEBT: 0, PROD_ISSUE: 0, OTHER: 0 };
        const totalPoints = tasks.reduce((acc, task) => acc + task.storyPoints, 0);
        tasks.forEach(task => {
            const category = (task.category || 'OTHER').toUpperCase();
            if (category in categoryPoints) categoryPoints[category] += task.storyPoints;
            else categoryPoints.OTHER += task.storyPoints;
        });
        if (totalPoints === 0) return [];
        return Object.entries(categoryPoints).map(([key, points]) => ({ ...categoryConfig[key as keyof typeof categoryConfig], points, percentage: (points / totalPoints) * 100, })).filter(item => item.points > 0);
    }, [tasks]);
    const radius = 40; const circumference = 2 * Math.PI * radius; let offset = 0;
    return (
        <Card className="h-full">
            <h3 className="text-2xl font-bold mb-4 border-b pb-3 text-gray-700">Work Distribution</h3>
            {tasks.length === 0 ? (<div className="flex items-center justify-center h-48 text-gray-500">No tasks imported.</div>) : (
                <div className="flex flex-col md:flex-row items-center justify-center pt-4 gap-6">
                    <div className="relative w-40 h-40">
                        <svg className="w-full h-full" viewBox="0 0 100 100">
                            <circle cx="50" cy="50" r={radius} fill="none" stroke="#e5e7eb" strokeWidth="15" />
                            {stats.map((stat, index) => {
                                const strokeDasharray = `${(stat.percentage / 100) * circumference} ${circumference}`;
                                const strokeDashoffset = -offset;
                                offset += (stat.percentage / 100) * circumference;
                                return (<circle key={index} cx="50" cy="50" r={radius} fill="none" stroke={stat.color} strokeWidth="15" strokeDasharray={strokeDasharray} strokeDashoffset={strokeDashoffset} transform="rotate(-90 50 50)" strokeLinecap="round" />);
                            })}
                        </svg>
                    </div>
                    <ul className="space-y-2">
                        {stats.map((stat, index) => (<li key={index} className="flex items-center text-sm"><span className="w-3 h-3 rounded-full mr-2" style={{ backgroundColor: stat.color }}></span><span className="text-gray-700 font-semibold w-24">{stat.label}</span><span className="text-gray-500">{stat.percentage.toFixed(1)}%</span></li>))}
                    </ul>
                </div>
            )}
        </Card>
    );
};

// --- Screen Components ---

const DashboardScreen: React.FC<{
    setCurrentScreen: (screen: Screen) => void;
    sprint: SprintData;
    capacity: CapacitySummary[];
    onCompleteSprint: () => void;
}> = ({ setCurrentScreen, sprint, capacity, onCompleteSprint }) => {
    const totalCapacity = capacity.reduce((acc, curr) => acc + curr.totalCapacity, 0);
    const totalAssigned = capacity.reduce((acc, curr) => acc + curr.assignedHours, 0);

    const ActionCard: React.FC<{ title: string; description: string; icon: React.ReactNode; onClick: () => void; }> = ({ title, description, icon, onClick }) => (
        <div onClick={onClick} className="bg-white p-6 rounded-xl border border-gray-200 hover:border-primary-400 hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1 cursor-pointer group">
            <div className="flex items-center space-x-4"><div className="bg-primary-50 text-primary-600 rounded-lg p-3 group-hover:bg-primary-100 transition-colors">{icon}</div><div><h3 className="text-lg font-bold text-gray-800 group-hover:text-primary-700">{title}</h3><p className="text-sm text-gray-500 mt-1">{description}</p></div></div>
        </div>
    );
    
    const AiSummaryCard: React.FC<{ sprint: SprintData | null, capacity: CapacitySummary[] }> = ({ sprint, capacity }) => {
        const [summary, setSummary] = useState('');
        const [isLoading, setIsLoading] = useState(false);
    
        const handleGenerate = async () => {
            if (!sprint) return;
            setIsLoading(true);
            setSummary('');
            const result = await generateSprintSummary(sprint.team, sprint.settings, sprint.tasks, capacity);
            setSummary(result);
            setIsLoading(false);
        };
    
        const renderSummaryContent = (text: string) => {
            return text.split('\n').filter(line => line.trim() !== '').map((line, index) => {
                const boldedLine = line.replace(/\*\*(.*?)\*\*/g, '<strong class="text-gray-900">$1</strong>');
                const content = boldedLine.replace(/^- /, '').trim();
                return (
                    <li key={index} className="flex items-start">
                        <span className="text-primary-500 mr-2 mt-1">&#8227;</span>
                        <span dangerouslySetInnerHTML={{ __html: content }} />
                    </li>
                );
            });
        };
    
        return (
            <Card>
                <h3 className="text-2xl font-bold mb-4 border-b pb-3 text-gray-700 flex items-center space-x-2">
                    <SparklesIcon className="w-6 h-6 text-primary-500" />
                    <span>AI Sprint Summary</span>
                </h3>
                {isLoading ? (
                    <div className="flex items-center justify-center h-32 text-gray-500">
                        <SparklesIcon className="w-5 h-5 animate-pulse mr-2" /> Generating insights...
                    </div>
                ) : summary ? (
                    <div className="text-gray-600 space-y-2 text-sm">
                        <ul className="space-y-2">{renderSummaryContent(summary)}</ul>
                    </div>
                ) : (
                    <div className="text-center py-4">
                        <p className="text-gray-500 mb-4">Get a quick overview of the sprint's focus, workload, and risks.</p>
                        <Button onClick={() => handleGenerate()} disabled={!sprint || isLoading}>
                           <SparklesIcon className="w-4 h-4" /> Generate Summary
                       </Button>
                    </div>
                )}
            </Card>
        );
    };

    return (
        <div className="space-y-10">
            <div className="flex flex-col md:flex-row justify-between md:items-start gap-4">
                <div className="text-left">
                    <h2 className="text-3xl md:text-4xl font-extrabold text-gray-800">Dashboard</h2>
                    <p className="text-gray-500 mt-2 text-lg">Your command center for the current sprint.</p>
                </div>
                <Button onClick={() => onCompleteSprint()} variant="secondary">
                    <ArchiveBoxArrowDownIcon /> Complete & Archive Sprint
                </Button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <ActionCard title="Sprint Setup" description="Dates, holidays & meetings." icon={<CalendarDaysIcon />} onClick={() => setCurrentScreen(Screen.SprintSetup)} />
                <ActionCard title="Task Planning" description="Import, assign & analyze." icon={<ChartPieIcon />} onClick={() => setCurrentScreen(Screen.TaskPlanning)} />
                <ActionCard title="Team Config" description="Manage team members." icon={<UsersIcon />} onClick={() => setCurrentScreen(Screen.TeamConfig)} />
                <ActionCard title="Holiday Master" description="Manage all holidays." icon={<BuildingLibraryIcon />} onClick={() => setCurrentScreen(Screen.HolidayMaster)} />
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
                <div className="lg:col-span-3 space-y-8">
                    <Card>
                        <h3 className="text-2xl font-bold mb-4 border-b pb-3 text-gray-700">Quick Stats</h3>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-4">
                            <div className="text-center p-4 bg-gray-100/70 rounded-lg"><p className="text-4xl font-bold text-primary-600">{sprint?.team.length || 0}</p><p className="text-gray-600 mt-1">Team Members</p></div>
                            <div className="text-center p-4 bg-gray-100/70 rounded-lg"><p className="text-lg font-bold text-primary-600">{sprint ? `${sprint.settings.startDate} → ${sprint.settings.endDate}` : 'Not Configured'}</p><p className="text-gray-600 mt-1">Sprint Dates</p></div>
                            <div className="text-center p-4 bg-gray-100/70 rounded-lg"><p className="text-2xl font-bold"><span className={totalAssigned > totalCapacity ? 'text-danger' : 'text-success'}>{totalAssigned}h</span> / {totalCapacity}h</p><p className="text-gray-600 mt-1">Assigned vs. Capacity</p></div>
                        </div>
                    </Card>
                    <AiSummaryCard sprint={sprint} capacity={capacity} />
                </div>
                <div className="lg:col-span-2">
                    <WorkDistributionChart tasks={sprint?.tasks || []} />
                </div>
            </div>
        </div>
    );
};

const TeamConfigScreen: React.FC<{
    team: TeamMember[],
    onTeamUpdate: (team: TeamMember[]) => void
}> = ({ team, onTeamUpdate }) => {
    const [editingMember, setEditingMember] = useState<TeamMember | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const handleAddMember = () => { setEditingMember({ id: '', name: '', role: Role.Backend, dailyCapacity: 6, leaveDays: [] }); setIsModalOpen(true); };
    const handleEdit = (member: TeamMember) => { setEditingMember({ ...member }); setIsModalOpen(true); };
    const handleDelete = (id: string) => { if (window.confirm('Are you sure?')) onTeamUpdate(team.filter(m => m.id !== id)); };
    const handleSave = (member: TeamMember) => {
        if (member.id) onTeamUpdate(team.map(m => (m.id === member.id ? member : m)));
        else onTeamUpdate([...team, { ...member, id: Date.now().toString() }]);
        setIsModalOpen(false);
    };
    const MemberModal = () => {
        if (!isModalOpen || !editingMember) return null;
        const [memberData, setMemberData] = useState<TeamMember>(editingMember);
        const [leaveDate, setLeaveDate] = useState('');
        const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => { const { name, value } = e.target; setMemberData(prev => ({ ...prev, [name]: name === 'dailyCapacity' ? Number(value) : value })); };
        const handleAddLeave = () => { if (leaveDate && !memberData.leaveDays.includes(leaveDate)) { setMemberData(prev => ({...prev, leaveDays: [...prev.leaveDays, leaveDate].sort()})); setLeaveDate(''); } };
        const handleRemoveLeave = (date: string) => { setMemberData(prev => ({...prev, leaveDays: prev.leaveDays.filter(d => d !== date)})); };
        const FormField: React.FC<{label: string, children: React.ReactNode}> = ({label, children}) => (<div><label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>{children}</div>);
        const inputClasses = "block w-full border-gray-300 rounded-lg shadow-sm p-2.5 text-sm focus:ring-primary-500 focus:border-primary-500";
        return (
            <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
                <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg"><h3 className="text-2xl font-bold mb-6 text-gray-800">{memberData.id ? 'Edit' : 'Add'} Member</h3><div className="space-y-5"><FormField label="Name"><input type="text" name="name" value={memberData.name} onChange={handleChange} className={inputClasses} /></FormField><FormField label="Role"><select name="role" value={memberData.role} onChange={handleChange} className={inputClasses}>{Object.values(Role).map(role => <option key={role} value={role}>{role}</option>)}</select></FormField><FormField label="Daily Capacity (hrs/day)"><input type="number" name="dailyCapacity" value={memberData.dailyCapacity} onChange={handleChange} className={inputClasses} /></FormField><FormField label="Leave Days"><div className="flex items-center space-x-2"><input type="date" value={leaveDate} onChange={(e) => setLeaveDate(e.target.value)} className={inputClasses} /><Button onClick={() => handleAddLeave()}>Add</Button></div><div className="mt-3 flex flex-wrap gap-2">{memberData.leaveDays.map(date => (<span key={date} className="bg-primary-100 text-primary-800 text-xs font-medium px-2.5 py-1 rounded-full flex items-center">{date}<button type="button" onClick={() => handleRemoveLeave(date)} className="ml-2 text-red-500 hover:text-red-700 font-bold">&times;</button></span>))}</div></FormField></div><div className="mt-8 flex justify-end space-x-3"><Button onClick={() => setIsModalOpen(false)} variant="secondary">Cancel</Button><Button onClick={() => handleSave(memberData)}>Save</Button></div></div>
            </div>
        );
    };
    return (
        <Card>{isModalOpen && <MemberModal />}<div className="flex flex-col md:flex-row justify-between md:items-center mb-6 gap-4"><h2 className="text-3xl font-bold text-gray-800">Team Configuration</h2><Button onClick={() => handleAddMember()}>+ Add Member</Button></div><div className="overflow-x-auto"><table className="min-w-full bg-white text-sm"><thead className="bg-gray-50"><tr><th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Name</th><th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Role</th><th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Capacity (hrs/day)</th><th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Leave Days</th><th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Actions</th></tr></thead><tbody className="divide-y divide-gray-200">{team.map(member => (<tr key={member.id}><td className="py-3 px-4 font-medium text-gray-900">{member.name}</td><td className="py-3 px-4 text-gray-600">{member.role}</td><td className="py-3 px-4 text-gray-600">{member.dailyCapacity}</td><td className="py-3 px-4 text-gray-600">{member.leaveDays.length}</td><td className="py-3 px-4"><div className="flex space-x-3"><button type="button" onClick={() => handleEdit(member)} className="text-primary-600 hover:text-primary-800"><EditIcon /></button><button type="button" onClick={() => handleDelete(member.id)} className="text-danger hover:text-red-700"><DeleteIcon /></button></div></td></tr>))}</tbody></table></div></Card>
    );
};

const SprintSetupScreen: React.FC<{
    settings: SprintSettings;
    onSettingsUpdate: (settings: SprintSettings) => void;
    masterHolidays: MasterHoliday[];
}> = ({ settings: initialSettings, onSettingsUpdate, masterHolidays }) => {
    const [settings, setSettings] = useState<SprintSettings>(initialSettings);
    const [newHoliday, setNewHoliday] = useState('');
    const [newDeployment, setNewDeployment] = useState({name: '', date: ''});
    
    useEffect(() => {
        const calculateEndDate = async () => {
            if(settings.startDate && settings.duration > 0) {
                try {
                    const start = new Date(settings.startDate);
                    const estimatedEnd = new Date(start);
                    estimatedEnd.setDate(start.getDate() + settings.duration * 2);
                    
                    // Fetch holidays from API for the date range
                    const holidayDates = await holidayService.getHolidayDatesForSprint(
                        settings.startDate,
                        estimatedEnd.toISOString().split('T')[0]
                    );
                    
                    const allHolidayDates = Array.from(new Set([...settings.publicHolidays, ...holidayDates]));
                    // addWorkingDays' second param is the number of working days to add, so duration-1 for the end date.
                    const end = addWorkingDays(start, settings.duration - 1, allHolidayDates);
                    setSettings(s => ({...s, endDate: end.toISOString().split('T')[0]}));
                } catch (error) {
                    console.error('Error calculating end date with holidays:', error);
                    // Fallback to simple calculation
                    const start = new Date(settings.startDate);
                    const allHolidayDates = Array.from(new Set([...settings.publicHolidays, ...masterHolidays.map(h => h.date)]));
                    const end = addWorkingDays(start, settings.duration - 1, allHolidayDates);
                    setSettings(s => ({...s, endDate: end.toISOString().split('T')[0]}));
                }
            }
        };
        calculateEndDate();
    }, [settings.startDate, settings.duration, settings.publicHolidays, masterHolidays]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => { const { name, value } = e.target; setSettings(prev => ({ ...prev, [name]: name === 'duration' ? Number(value) : value })); };
    
    const handleAddSprintHoliday = () => {
        if (newHoliday && !settings.publicHolidays.includes(newHoliday)) {
            setSettings(prev => ({...prev, publicHolidays: [...prev.publicHolidays, newHoliday].sort()}));
            setNewHoliday('');
        }
    };

    const handleRemoveSprintHoliday = (date: string) => { setSettings(prev => ({...prev, publicHolidays: prev.publicHolidays.filter(d => d !== date)})); };
    
    const handleAddDeployment = () => {
        if (newDeployment.name && newDeployment.date) {
            setSettings(prev => ({...prev, deployments: [...prev.deployments, {id: Date.now().toString(), ...newDeployment}]}));
            setNewDeployment({name: '', date: ''});
        }
    };

    const handleRemoveDeployment = (id: string) => { setSettings(prev => ({...prev, deployments: prev.deployments.filter(d => d.id !== id)})); };
    const handleSave = () => { onSettingsUpdate(settings); alert('Sprint settings saved!'); };

    const inputClasses = "block w-full border-gray-300 rounded-lg shadow-sm p-2.5 text-sm focus:ring-primary-500 focus:border-primary-500";
    const FormField: React.FC<{label: string, children: React.ReactNode}> = ({label, children}) => (<div><label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>{children}</div>);
    
    const TimelineVisualization: React.FC<{settings: SprintSettings}> = ({settings}) => {
        const events = useMemo(() => {
            let allEvents: {date: string, name: string, type: 'start' | 'end' | 'holiday' | 'deployment' | 'freeze' | 'meeting', isMaster: boolean, time?: string}[] = [];
            if (!settings.startDate || !settings.endDate) return [];
            
            const start = new Date(settings.startDate);
            const end = new Date(settings.endDate);

            allEvents.push({ date: settings.startDate, name: 'Sprint Start', type: 'start', isMaster: false });
            allEvents.push({ date: settings.endDate, name: 'Sprint End', type: 'end', isMaster: false });
            
            settings.publicHolidays.forEach(h => allEvents.push({date: h, name: 'Holiday (Sprint)', type: 'holiday', isMaster: false}));
            
            masterHolidays.forEach(h => {
                const hDate = new Date(h.date);
                if (hDate >= start && hDate <= end) {
                    allEvents.push({date: h.date, name: h.name, type: 'holiday', isMaster: true});
                }
            });

            settings.deployments.forEach(d => allEvents.push({date: d.date, name: d.name, type: 'deployment', isMaster: false}));
            if (settings.freezeDate) allEvents.push({date: settings.freezeDate, name: 'Code Freeze', type: 'freeze', isMaster: false});
            settings.meetings.forEach(m => allEvents.push({date: m.date, name: `Meeting: ${m.type}`, type: 'meeting', isMaster: false, time: m.time}));

            return allEvents.sort((a,b) => {
                const dateA = new Date(a.date).getTime();
                const dateB = new Date(b.date).getTime();
                if(dateA !== dateB) return dateA - dateB;
                return (a.time || '00:00').localeCompare(b.time || '00:00');
            });
        }, [settings, masterHolidays]);

        const eventIcons = {
            start: <PlayIcon className="w-4 h-4 text-green-500"/>,
            end: <FlagIcon className="w-4 h-4 text-red-500"/>,
            holiday: <CalendarIcon />,
            deployment: <PaperAirplaneIcon className="w-4 h-4 text-blue-500" />,
            freeze: <span className="text-xl">❄️</span>,
            meeting: <ClockIcon className="w-4 h-4 text-purple-500"/>,
        }

        return (
            <Card className="h-full">
                 <h3 className="text-xl font-bold mb-6 text-gray-700 border-b pb-3">Sprint Timeline</h3>
                 <div className="relative pl-6">
                     <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-gray-200"></div>
                     <div className="space-y-6">
                        {events.map((event, index) => (
                            <div key={index} className="relative flex items-start space-x-4">
                                <div className="absolute left-[-26px] top-0.5 bg-white h-5 w-5 rounded-full border-2 border-primary-500 flex items-center justify-center">
                                  <div className="h-2 w-2 bg-primary-500 rounded-full"></div>
                                </div>
                                <div>
                                    <p className="font-bold text-gray-800 flex items-center">{event.name} {event.isMaster && <GlobeAltIcon className="w-4 h-4 ml-2 text-gray-400" title="From Holiday Master"/>}</p>
                                    <p className="text-sm text-gray-500 flex items-center space-x-1.5">
                                        {eventIcons[event.type]}
                                        <span>{new Date(event.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })} {event.time && `at ${event.time}`}</span>
                                    </p>
                                </div>
                            </div>
                        ))}
                     </div>
                 </div>
            </Card>
        );
    };

    const HolidayManager = () => {
        const [apiHolidays, setApiHolidays] = useState<MasterHoliday[]>([]);
        const [isLoadingHolidays, setIsLoadingHolidays] = useState(false);

        useEffect(() => {
            const loadHolidays = async () => {
                if (!settings.startDate || !settings.endDate) return;
                
                setIsLoadingHolidays(true);
                try {
                    const holidays = await holidayService.getHolidaysByDateRange(settings.startDate, settings.endDate);
                    const masterHolidays: MasterHoliday[] = holidays.map(h => ({
                        id: h.id || '',
                        name: h.name,
                        date: h.holidayDate
                    }));
                    setApiHolidays(masterHolidays);
                } catch (error) {
                    console.error('Error loading holidays:', error);
                } finally {
                    setIsLoadingHolidays(false);
                }
            };
            loadHolidays();
        }, [settings.startDate, settings.endDate]);

        const sprintHolidaysInScope = useMemo(() => {
            const allHolidays = [...apiHolidays, ...masterHolidays];
            const start = new Date(settings.startDate);
            const end = new Date(settings.endDate);
            const holidaysInSprint = [];

            if (settings.startDate && settings.endDate) {
                for (const holiday of allHolidays) {
                    const hDate = new Date(holiday.date);
                    if (hDate >= start && hDate <= end) {
                        holidaysInSprint.push(holiday);
                    }
                }
            }
            return holidaysInSprint.sort((a,b) => new Date(a.date).getTime() - new Date(b.date).getTime());
        }, [settings.startDate, settings.endDate, apiHolidays, masterHolidays]);

        return (
            <Card>
                <h3 className="text-lg font-bold mb-2 text-gray-700">Sprint Holidays</h3>
                <p className="text-xs text-gray-500 mb-4">Add one-off holidays for this sprint. Master holidays are included automatically.</p>
                <div className="flex items-center space-x-2"><input type="date" value={newHoliday} onChange={e => setNewHoliday(e.target.value)} className={inputClasses}/><Button onClick={() => handleAddSprintHoliday()} variant="secondary"><PlusIcon className="w-4 h-4"/></Button></div>
                <div className="mt-4 space-y-2 max-h-40 overflow-y-auto pr-2">
                    {isLoadingHolidays && <p className="text-sm text-gray-500">Loading holidays...</p>}
                    {sprintHolidaysInScope.map(holiday => (
                         <div key={holiday.id} className="flex justify-between items-center bg-blue-50 p-2 rounded-md">
                            <div>
                                <p className="text-sm font-semibold text-blue-800 flex items-center">{holiday.name} <GlobeAltIcon className="w-4 h-4 ml-2 text-blue-400" title="From Holiday Master"/></p>
                                <p className="text-xs text-blue-600">{holiday.date}</p>
                            </div>
                         </div>
                    ))}
                    {settings.publicHolidays.map(date => (
                        <div key={date} className="flex justify-between items-center bg-gray-50 p-2 rounded-md">
                            <div>
                                <p className="text-sm font-semibold text-gray-800">Sprint-specific Holiday</p>
                                <p className="text-xs text-gray-500">{date}</p>
                            </div>
                            <button type="button" onClick={() => handleRemoveSprintHoliday(date)} className="text-danger hover:text-red-700 p-1 rounded-full"><DeleteIcon/></button>
                        </div>
                    ))}
                </div>
            </Card>
        );
    }
    
    const DeploymentManager = () => (
        <Card>
            <h3 className="text-lg font-bold mb-2 text-gray-700">Deployments</h3>
            <div className="space-y-2">
                <input type="text" placeholder="Deployment Name" value={newDeployment.name} onChange={e => setNewDeployment(p => ({...p, name: e.target.value}))} className={inputClasses} />
                <div className="flex items-center space-x-2">
                    <input type="date" value={newDeployment.date} onChange={e => setNewDeployment(p => ({...p, date: e.target.value}))} className={inputClasses}/>
                    <Button onClick={() => handleAddDeployment()} variant="secondary"><PlusIcon className="w-4 h-4"/></Button>
                </div>
            </div>
             <div className="mt-2 space-y-2 max-h-40 overflow-y-auto pr-2">{settings.deployments.map((item) => (
                <div key={item.id} className="flex justify-between items-center bg-gray-50 p-2 rounded-md">
                    <div>
                        <p className="text-sm font-semibold text-gray-800">{item.name}</p>
                        <p className="text-xs text-gray-500">{item.date}</p>
                    </div>
                    <button type="button" onClick={() => handleRemoveDeployment(item.id)} className="text-danger hover:text-red-700 p-1 rounded-full"><DeleteIcon/></button>
                </div>
            ))}</div>
        </Card>
    );

    const MeetingManager = () => {
        const [inviteModalContent, setInviteModalContent] = useState<{ subject: string; body: string } | null>(null);
        const [isGenerating, setIsGenerating] = useState<MeetingType | null>(null);
        const [copySuccess, setCopySuccess] = useState(false);
        const [activeMeetingType, setActiveMeetingType] = useState<MeetingType | null>(null);

        const handleMeetingChange = (type: MeetingType, field: keyof Omit<SprintMeeting, 'id' | 'type'>, value: string | number) => {
            const existingMeetingIndex = settings.meetings.findIndex(m => m.type === type);
            let newMeetings = [...settings.meetings];
            
            if (field === 'duration' && typeof value === 'string') {
                value = parseInt(value, 10) || 0;
            }

            if (existingMeetingIndex > -1) {
                const updatedMeeting = { ...newMeetings[existingMeetingIndex], [field]: value };
                newMeetings[existingMeetingIndex] = updatedMeeting;
            } else {
                const newMeeting: SprintMeeting = {
                    id: `meeting-${type}-${Date.now()}`,
                    type,
                    date: '', time: '', duration: 60,
                    ...{ [field]: value }
                };
                newMeetings.push(newMeeting);
            }
            setSettings(prev => ({...prev, meetings: newMeetings}));
        };

        const handleGenerateInvite = async (type: MeetingType) => {
            const meeting = settings.meetings.find(m => m.type === type);
            if (!meeting || !meeting.date || !meeting.time) {
                alert("Please set a date and time for the meeting first.");
                return;
            }
            setIsGenerating(type);
            setActiveMeetingType(type);
            const content = await generateMeetingInvite(meeting, settings);
            setInviteModalContent(content);
            setIsGenerating(null);
        };
        
        const handleCopy = () => {
            if(!inviteModalContent?.body) return;
            navigator.clipboard.writeText(inviteModalContent.body).then(() => {
                setCopySuccess(true);
                setTimeout(() => setCopySuccess(false), 2000);
            });
        };

        const handleOpenInOutlook = () => {
            if (!inviteModalContent || !activeMeetingType) return;
            const meeting = settings.meetings.find(m => m.type === activeMeetingType);
            if (!meeting) return;

            const { date, time, duration } = meeting;
            const { subject, body } = inviteModalContent;

            const startDateTime = new Date(`${date}T${time}`);
            const endDateTime = new Date(startDateTime.getTime() + duration * 60000);

            const outlookUrl = new URL('https://outlook.office.com/calendar/deeplink/compose');
            outlookUrl.searchParams.append('startdt', startDateTime.toISOString());
            outlookUrl.searchParams.append('enddt', endDateTime.toISOString());
            outlookUrl.searchParams.append('subject', subject);
            outlookUrl.searchParams.append('body', body);
            
            window.open(outlookUrl.toString(), '_blank');
        };

        const handleCloseModal = () => {
            setInviteModalContent(null);
            setActiveMeetingType(null);
        }

        const renderMeetingForm = (type: MeetingType, title: string) => {
            const meeting = settings.meetings.find(m => m.type === type);
            return (
                <div className="py-4">
                    <h4 className="font-semibold text-md text-gray-800">{title}</h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mt-2 items-end">
                        <FormField label="Date"><input type="date" value={meeting?.date || ''} onChange={e => handleMeetingChange(type, 'date', e.target.value)} className={inputClasses}/></FormField>
                        <FormField label="Time"><input type="time" value={meeting?.time || ''} onChange={e => handleMeetingChange(type, 'time', e.target.value)} className={inputClasses}/></FormField>
                        <FormField label="Duration (min)"><input type="number" value={meeting?.duration || 60} onChange={e => handleMeetingChange(type, 'duration', e.target.value)} className={inputClasses} step="15" min="15"/></FormField>
                        <Button onClick={() => handleGenerateInvite(type)} disabled={!meeting?.date || !meeting?.time || !!isGenerating}>
                             {isGenerating === type ? (<><SparklesIcon className="w-4 h-4 animate-pulse"/> Generating...</>) : (<><SparklesIcon className="w-4 h-4"/> Generate Invite</>)}
                        </Button>
                    </div>
                </div>
            );
        };

        return (
            <Card>
                {inviteModalContent && (
                    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
                        <div className="bg-white p-6 rounded-xl shadow-2xl w-full max-w-2xl">
                             <div className="flex justify-between items-start mb-4 pb-3 border-b">
                                <div>
                                    <h3 className="text-xl font-bold text-gray-800">Generated Outlook Invite</h3>
                                    <p className="text-sm text-gray-500 mt-1">{inviteModalContent.subject}</p>
                                </div>
                                <div className="flex space-x-2 flex-shrink-0">
                                    <Button onClick={handleCopy} variant="secondary">
                                        {copySuccess ? <CheckIcon/> : <CopyIcon/>}
                                        {copySuccess ? 'Copied!' : 'Copy Body'}
                                    </Button>
                                    <Button onClick={handleOpenInOutlook}>
                                        <PaperAirplaneIcon className="w-4 h-4" />
                                        Open in Outlook
                                    </Button>
                                </div>
                            </div>
                            <pre className="bg-gray-50 p-4 rounded-lg text-sm whitespace-pre-wrap font-sans text-gray-800 max-h-96 overflow-y-auto">{inviteModalContent.body}</pre>
                            <div className="mt-6 flex justify-end">
                                <Button onClick={handleCloseModal} variant="secondary">Close</Button>
                            </div>
                        </div>
                    </div>
                )}
                <h3 className="text-xl font-bold text-gray-700">Sprint Meetings</h3>
                <p className="text-sm text-gray-500 mt-1">Schedule key sprint ceremonies and generate AI-powered Outlook invites.</p>
                <div className="divide-y divide-gray-200">
                    {renderMeetingForm('Planning', 'Sprint Planning')}
                    {renderMeetingForm('Grooming', 'Backlog Grooming/Refinement')}
                    {renderMeetingForm('Retrospective', 'Sprint Retrospective')}
                </div>
            </Card>
        )
    }

    return (
      <div className="space-y-6"><div className="flex flex-col md:flex-row justify-between md:items-center gap-4"><h2 className="text-3xl font-bold text-gray-800">Sprint Setup</h2><Button onClick={() => handleSave()}>Save Sprint Settings</Button></div>
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
            <div className="lg:col-span-3 space-y-6">
                <Card>
                    <h3 className="text-xl font-bold mb-4 text-gray-700">Core Configuration</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <FormField label="Sprint Start Date"><input type="date" name="startDate" value={settings.startDate} onChange={handleChange} className={inputClasses}/></FormField>
                        <FormField label="Sprint Duration (working days)"><input type="number" name="duration" value={settings.duration} onChange={handleChange} className={inputClasses} min="1"/></FormField>
                        <FormField label="Sprint End Date (Calculated)"><input type="date" name="endDate" value={settings.endDate} readOnly className={`${inputClasses} bg-gray-100 cursor-not-allowed`}/></FormField>
                        <FormField label="Code Freeze Date (Optional)"><input type="date" name="freezeDate" value={settings.freezeDate || ''} onChange={handleChange} className={inputClasses}/></FormField>
                    </div>
                </Card>
                <MeetingManager/>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <HolidayManager />
                    <DeploymentManager />
                </div>
            </div>
            <div className="lg:col-span-2">
                <TimelineVisualization settings={settings} />
            </div>
        </div>
      </div>
    );
};

const HolidayMasterScreen: React.FC<{
    holidays: MasterHoliday[];
    onUpdate: (holidays: MasterHoliday[]) => void;
}> = ({ holidays, onUpdate }) => {
    const [newHoliday, setNewHoliday] = useState({ name: '', date: '', type: 'PUBLIC' as 'PUBLIC' | 'COMPANY', recurring: false });
    const [isLoading, setIsLoading] = useState(false);
    const inputClasses = "block w-full border-gray-300 rounded-lg shadow-sm p-2.5 text-sm focus:ring-primary-500 focus:border-primary-500";

    useEffect(() => {
        loadHolidays();
    }, []);

    const loadHolidays = async () => {
        setIsLoading(true);
        try {
            const apiHolidays = await holidayService.getAllHolidays();
            const masterHolidays: MasterHoliday[] = apiHolidays.map(h => ({
                id: h.id || '',
                name: h.name,
                date: h.holidayDate
            }));
            onUpdate(masterHolidays);
        } catch (error) {
            console.error('Error loading holidays:', error);
            alert('Failed to load holidays');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddHoliday = async () => {
        if (!newHoliday.name || !newHoliday.date) {
            alert('Please fill in all required fields');
            return;
        }

        setIsLoading(true);
        try {
            const created = await holidayService.createHoliday({
                name: newHoliday.name,
                holidayDate: newHoliday.date,
                holidayType: newHoliday.type,
                recurring: newHoliday.recurring
            });

            if (created) {
                await loadHolidays();
                setNewHoliday({ name: '', date: '', type: 'PUBLIC', recurring: false });
            } else {
                alert('Failed to create holiday');
            }
        } catch (error) {
            console.error('Error creating holiday:', error);
            alert('Error creating holiday');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteHoliday = async (id: string) => {
        if (!window.confirm('Are you sure you want to delete this holiday from the master list?')) {
            return;
        }

        setIsLoading(true);
        try {
            const success = await holidayService.deleteHoliday(id);
            if (success) {
                await loadHolidays();
            } else {
                alert('Failed to delete holiday');
            }
        } catch (error) {
            console.error('Error deleting holiday:', error);
            alert('Error deleting holiday');
        } finally {
            setIsLoading(false);
        }
    };
    
    return (
        <div className="space-y-6">
            <h2 className="text-3xl font-bold text-gray-800">Holiday Master List</h2>
            <p className="text-gray-600">Manage company-wide or regional holidays here. They will be automatically applied to relevant sprints.</p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                <div className="md:col-span-1">
                    <Card>
                        <h3 className="text-xl font-bold text-gray-700 mb-4">Add New Holiday</h3>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Holiday Name</label>
                                <input type="text" value={newHoliday.name} onChange={e => setNewHoliday(p => ({...p, name: e.target.value}))} className={inputClasses} placeholder="e.g., New Year's Day" />
                            </div>
                             <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
                                <input type="date" value={newHoliday.date} onChange={e => setNewHoliday(p => ({...p, date: e.target.value}))} className={inputClasses} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                                <select value={newHoliday.type} onChange={e => setNewHoliday(p => ({...p, type: e.target.value as 'PUBLIC' | 'COMPANY'}))} className={inputClasses}>
                                    <option value="PUBLIC">Public Holiday</option>
                                    <option value="COMPANY">Company Event</option>
                                </select>
                            </div>
                            <div>
                                <label className="flex items-center space-x-2">
                                    <input type="checkbox" checked={newHoliday.recurring} onChange={e => setNewHoliday(p => ({...p, recurring: e.target.checked}))} className="rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
                                    <span className="text-sm font-medium text-gray-700">Recurring Holiday</span>
                                </label>
                            </div>
                            <Button onClick={() => handleAddHoliday()} disabled={isLoading}><PlusIcon className="w-4 h-4"/> {isLoading ? 'Adding...' : 'Add Holiday'}</Button>
                        </div>
                    </Card>
                </div>
                <div className="md:col-span-2">
                    <Card>
                        <h3 className="text-xl font-bold text-gray-700 mb-4">Master Holiday List (2024)</h3>
                        <div className="space-y-2 max-h-96 overflow-y-auto pr-2">
                        {isLoading && holidays.length === 0 && <p className="text-gray-500 text-center py-4">Loading holidays...</p>}
                        {!isLoading && holidays.length === 0 && <p className="text-gray-500 text-center py-4">No master holidays defined.</p>}
                        {holidays.map(holiday => (
                            <div key={holiday.id} className="flex justify-between items-center bg-gray-50 p-3 rounded-md">
                                <div>
                                    <p className="font-semibold text-gray-800">{holiday.name}</p>
                                    <p className="text-sm text-gray-500">{holiday.date}</p>
                                </div>
                                <Button onClick={() => handleDeleteHoliday(holiday.id)} variant="danger" disabled={isLoading}><DeleteIcon/></Button>
                            </div>
                        ))}
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    )
}

const TaskPlanningScreen: React.FC<{
    sprintData: SprintData;
    capacity: CapacitySummary[];
    onTasksUpdate: (tasks: JiraTask[]) => void;
}> = ({ sprintData: initialSprintData, capacity, onTasksUpdate }) => {
    const [sprintData, setSprintData] = useState(initialSprintData);
    const [taskRisks, setTaskRisks] = useState<TaskRisk[]>([]);
    const [isAnalysisComplete, setIsAnalysisComplete] = useState(false);
    const [riskSummary, setRiskSummary] = useState('');
    const [isSummaryLoading, setIsSummaryLoading] = useState(false);
    
    // --- Import Logic ---
    const [error, setError] = useState('');
    const [isJiraModalOpen, setIsJiraModalOpen] = useState(false);
    
    const handleJiraTasksFetched = (tasks: JiraTask[]) => {
        onTasksUpdate(tasks);
        alert(`${tasks.length} tasks imported successfully!`);
        setError('');
    };

    useEffect(() => {
        setSprintData(initialSprintData);
    }, [initialSprintData]);
    
    const handleAssigneeChange = (taskId: string, assigneeId: string) => {
        const updatedTasks = sprintData.tasks.map(task => 
            task.id === taskId ? { ...task, assigneeId } : task
        );
        onTasksUpdate(updatedTasks);
    };
    
    const handleAnalyzeRisk = () => {
        if (sprintData.tasks.length === 0) {
            alert("Please import tasks before analyzing risk.");
            return;
        }

        const today = new Date();
        const enrichedTasks = sprintData.tasks.map((task, index) => {
            const startDate = new Date(sprintData.settings.startDate);
            startDate.setDate(startDate.getDate() + (index % 3)); // Stagger start dates
            const dueDate = new Date(startDate);
            dueDate.setDate(dueDate.getDate() + 2 + (index % 5)); // Varying due dates
            let timeSpent = 0;
            const timeDiff = today.getTime() - startDate.getTime();
            const dueDiff = dueDate.getTime() - startDate.getTime();
            if (timeDiff > 0 && dueDiff > 0) {
                const progressRatio = timeDiff / dueDiff;
                timeSpent = Math.min(task.storyPoints, task.storyPoints * progressRatio * (0.8 + Math.random() * 0.4));
            }
            if (index === 1 && sprintData.tasks.length > 1) { // Create a clear "at risk" scenario
                dueDate.setDate(today.getDate() + 1); // Due tomorrow
                timeSpent = task.storyPoints * 0.1; // Only 10% done
            }
            if (index === 2 && sprintData.tasks.length > 2) { // Create a clear "off track" scenario
                dueDate.setDate(today.getDate() - 2); // Was due 2 days ago
                timeSpent = task.storyPoints * 0.6; // Not finished
            }

            return { ...task, startDate: startDate.toISOString().split('T')[0], dueDate: dueDate.toISOString().split('T')[0], timeSpent: parseFloat(timeSpent.toFixed(1)), };
        });
        
        const risks = enrichedTasks.map(calculateTaskRisk);
        onTasksUpdate(enrichedTasks); // Update tasks globally with simulated data
        setTaskRisks(risks);
        setIsAnalysisComplete(true);
        setRiskSummary('');
    };

    const handleGenerateRiskSummary = async () => {
        setIsSummaryLoading(true);
        try {
            const summary = await generateRiskSummary(sprintData.tasks, taskRisks);
            setRiskSummary(summary);
        } catch (error) {
            alert("Failed to generate AI summary. See console for details.");
            console.error(error);
        } finally {
            setIsSummaryLoading(false);
        }
    };
    
    const renderSummaryContent = (text: string) => {
        return text.split('\n').filter(line => line.trim() !== '').map((line, index) => {
            const boldedLine = line.replace(/\*\*(.*?)\*\*/g, '<strong class="text-gray-900">$1</strong>');
            const content = boldedLine.replace(/^- /, '').trim();
            return (
                <li key={index} className="flex items-start">
                    <span className="text-primary-500 mr-2 mt-1">&#8227;</span>
                    <span dangerouslySetInnerHTML={{ __html: content }} />
                </li>
            );
        });
    };
    
    const JiraConnectModal: React.FC<{ onFetch: (tasks: JiraTask[]) => void; onClose: () => void; }> = ({ onFetch, onClose }) => {
        const [jql, setJql] = useState('project = "SP" AND sprint in openSprints() ORDER BY rank ASC');
        const [domain, setDomain] = useState('sprint-pilot.atlassian.net');
        const [isLoading, setIsLoading] = useState(false);
    
        const handleFetch = () => {
            setIsLoading(true);
            setTimeout(() => {
                const mockTasks: JiraTask[] = [
                    { id: 'task-jira-1', key: 'SP-101', summary: 'Develop new login flow with two-factor authentication', storyPoints: 8, category: 'FEATURE' },
                    { id: 'task-jira-2', key: 'SP-102', summary: 'Fix caching invalidation issue on user profile page', storyPoints: 5, category: 'PROD_ISSUE' },
                    { id: 'task-jira-3', key: 'SP-103', summary: 'Refactor authentication service to use new security library', storyPoints: 13, category: 'TECH_DEBT' },
                    { id: 'task-jira-4', key: 'SP-104', summary: 'Update documentation for all public API v2 endpoints', storyPoints: 3, category: 'FEATURE' },
                    { id: 'task-jira-5', key: 'SP-105', summary: 'Investigate performance degradation on main dashboard load', storyPoints: 8, category: 'PROD_ISSUE' },
                ];
                onFetch(mockTasks);
                setIsLoading(false);
                onClose();
            }, 2000);
        };
    
        const inputClasses = "block w-full border-gray-300 rounded-lg shadow-sm p-2.5 text-sm focus:ring-primary-500 focus:border-primary-500 font-mono";
        const FormField: React.FC<{label: string, children: React.ReactNode}> = ({label, children}) => (<div><label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>{children}</div>);
    
        return (
            <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
                <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-2xl">
                    <h3 className="text-2xl font-bold mb-2 text-gray-800 flex items-center"><JiraIcon className="w-6 h-6 mr-2 text-blue-600" /> Connect to Jira</h3>
                    <p className="text-sm text-gray-500 mb-6">Enter your Jira instance details and a JQL query to fetch tasks. This is a mock API call for demonstration.</p>
                    <div className="space-y-5">
                        <FormField label="Jira Domain">
                            <input type="text" value={domain} onChange={(e) => setDomain(e.target.value)} className={inputClasses} placeholder="your-company.atlassian.net" />
                        </FormField>
                        <FormField label="JQL Query">
                            <textarea value={jql} onChange={(e) => setJql(e.target.value)} rows={4} className={inputClasses}></textarea>
                        </FormField>
                    </div>
                    <div className="mt-8 flex justify-end space-x-3">
                        <Button onClick={() => onClose()} variant="secondary" disabled={isLoading}>Cancel</Button>
                        <Button onClick={() => handleFetch()} disabled={isLoading}>
                            {isLoading ? ( <> <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> Fetching... </> ) : 'Fetch Tasks'}
                        </Button>
                    </div>
                </div>
            </div>
        );
    };

    // FIX: The Importer component was incomplete and had a malformed className attribute.
    // This has been corrected to provide full functionality for Jira import.
    const Importer = () => (
        <Card className="p-0 overflow-hidden">
            <div className="p-6">
                <div>
                    <h3 className="text-lg font-bold text-gray-800">Import from Jira</h3>
                    <p className="text-sm text-gray-500 mt-1 mb-4">Connect to your Jira instance to import tasks directly. (This is a mocked action)</p>
                    <Button onClick={() => setIsJiraModalOpen(true)}>Connect to Jira</Button>
                </div>
                {error && <p className="text-danger mt-4 text-sm font-semibold">{error}</p>}
            </div>
        </Card>
    );

    // FIX: The TaskPlanningScreen component was missing a return statement, causing it to return 'void' instead of a ReactNode.
    // This has been fixed by adding the main JSX structure for the screen.
    return (
        <div className="space-y-8">
            {isJiraModalOpen && <JiraConnectModal onFetch={handleJiraTasksFetched} onClose={() => setIsJiraModalOpen(false)} />}
            <div className="flex flex-col md:flex-row justify-between md:items-start gap-4">
                <div>
                    <h2 className="text-3xl font-bold text-gray-800">Task Planning & Analysis</h2>
                    <p className="text-gray-500 mt-1">Import tasks, assign them to team members, and analyze potential risks.</p>
                </div>
                {sprintData.tasks.length > 0 && (
                    <Button onClick={handleAnalyzeRisk} disabled={isAnalysisComplete}>
                        <ShieldExclamationIcon className="w-4 h-4" /> 
                        {isAnalysisComplete ? 'Analysis Complete' : 'Analyze Task Risks'}
                    </Button>
                )}
            </div>

            {sprintData.tasks.length === 0 ? (
                <Importer />
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
                    <div className="lg:col-span-3">
                        <Card>
                            <h3 className="text-xl font-bold mb-4 text-gray-700 border-b pb-3">Sprint Backlog ({sprintData.tasks.length} tasks)</h3>
                            <div className="space-y-3 max-h-[600px] overflow-y-auto pr-2">
                                {sprintData.tasks.map(task => {
                                    const risk = taskRisks.find(r => r.taskId === task.id);
                                    const riskStyles = {
                                        'On Track': 'border-l-4 border-green-500',
                                        'At Risk': 'border-l-4 border-yellow-500 bg-yellow-50/50',
                                        'Off Track': 'border-l-4 border-red-500 bg-red-50/50'
                                    };
                                    const riskIcon = {
                                        'On Track': <CheckIcon className="w-5 h-5 text-green-500" />,
                                        'At Risk': <InformationCircleIcon className="w-5 h-5 text-yellow-500" />,
                                        'Off Track': <ShieldExclamationIcon className="w-5 h-5 text-red-500" />
                                    };

                                    return (
                                    <div key={task.id} className={`p-4 rounded-lg bg-white shadow-sm border border-gray-200 ${risk ? riskStyles[risk.riskLevel] : ''}`}>
                                        <div className="flex justify-between items-start">
                                            <div>
                                                <p className="font-bold text-gray-800">{task.key}: {task.summary}</p>
                                                <p className="text-sm text-gray-500">{task.category} &bull; {task.storyPoints} hours</p>
                                            </div>
                                            {risk && (
                                                <div className="flex items-center space-x-2 text-sm font-semibold" title={risk.reason}>
                                                    {riskIcon[risk.riskLevel]}
                                                    <span>{risk.riskLevel}</span>
                                                </div>
                                            )}
                                        </div>
                                        <div className="mt-3 flex items-center justify-between">
                                            <div className="w-1/2">
                                                <select
                                                    value={task.assigneeId || ''}
                                                    onChange={(e) => handleAssigneeChange(task.id, e.target.value)}
                                                    className="block w-full border-gray-300 rounded-md shadow-sm p-2 text-sm focus:ring-primary-500 focus:border-primary-500"
                                                >
                                                    <option value="">Unassigned</option>
                                                    {sprintData.team.map(member => (
                                                        <option key={member.id} value={member.id}>{member.name}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            {isAnalysisComplete && task.startDate && (
                                                <div className="text-xs text-gray-500 text-right">
                                                    <p>Start: {task.startDate} | Due: {task.dueDate}</p>
                                                    <p>Spent: {task.timeSpent}h</p>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                    )
                                })}
                            </div>
                        </Card>
                    </div>
                    <div className="lg:col-span-2">
                        <Card>
                            <h3 className="text-xl font-bold mb-4 text-gray-700 border-b pb-3 flex items-center space-x-2"><SparklesIcon className="w-5 h-5 text-primary-500"/> AI Risk Summary</h3>
                            {!isAnalysisComplete ? (
                                <div className="text-center py-10 text-gray-500">
                                    <ShieldExclamationIcon className="w-12 h-12 mx-auto text-gray-300 mb-2"/>
                                    <p>Run risk analysis to generate an AI summary.</p>
                                </div>
                            ) : isSummaryLoading ? (
                                 <div className="flex items-center justify-center h-32 text-gray-500">
                                    <SparklesIcon className="w-5 h-5 animate-pulse mr-2" /> Generating summary...
                                </div>
                            ) : riskSummary ? (
                                <div className="text-gray-600 space-y-2 text-sm">
                                    <ul className="space-y-2">{renderSummaryContent(riskSummary)}</ul>
                                </div>
                            ) : (
                                <div className="text-center py-4">
                                    <p className="text-gray-500 mb-4">Get a high-level summary of potential risks and bottlenecks.</p>
                                    <Button onClick={handleGenerateRiskSummary} disabled={isSummaryLoading}>
                                       <SparklesIcon className="w-4 h-4" /> Generate Risk Summary
                                   </Button>
                                </div>
                            )}
                        </Card>
                    </div>
                </div>
            )}
        </div>
    );
};

const ExportScreen: React.FC<{
    sprint: SprintData;
    capacity: CapacitySummary[];
}> = ({ sprint, capacity }) => {
    const [exportType, setExportType] = useState<'confluence' | 'teams' | 'outlook'>('confluence');
    const [generatedContent, setGeneratedContent] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [copySuccess, setCopySuccess] = useState(false);

    const handleGenerate = async () => {
        if (!sprint) {
            alert("No sprint data available to generate content.");
            return;
        }
        setIsLoading(true);
        setGeneratedContent('');
        let content = '';
        const { team, settings, tasks } = sprint;
        switch (exportType) {
            case 'confluence':
                content = await generateConfluencePage(team, settings, tasks, capacity);
                break;
            case 'teams':
                content = await generateTeamsMessage(team, settings, tasks, capacity);
                break;
            case 'outlook':
                content = await generateOutlookInvite(team, settings, tasks, capacity);
                break;
        }
        setGeneratedContent(content);
        setIsLoading(false);
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(generatedContent).then(() => {
            setCopySuccess(true);
            setTimeout(() => setCopySuccess(false), 2000);
        });
    };

    const exportOptions = [
        { id: 'confluence', name: 'Confluence Page' },
        { id: 'teams', name: 'MS Teams Message' },
        { id: 'outlook', name: 'Outlook Invite Body' },
    ];
    
    return (
        <div className="space-y-6">
            <h2 className="text-3xl font-bold text-gray-800">Export Center</h2>
            <p className="text-gray-600">Generate formatted content for other platforms based on your sprint plan.</p>
             <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-1">
                    <Card>
                        <h3 className="text-xl font-bold text-gray-700 mb-4">Select Export Type</h3>
                        <div className="space-y-2">
                            {exportOptions.map(opt => (
                                <button
                                    key={opt.id}
                                    onClick={() => setExportType(opt.id as any)}
                                    className={`w-full text-left p-3 rounded-lg border-2 transition-colors ${
                                        exportType === opt.id
                                            ? 'bg-primary-50 border-primary-500 text-primary-800 font-semibold'
                                            : 'bg-white border-gray-200 hover:bg-gray-50'
                                    }`}
                                >
                                    {opt.name}
                                </button>
                            ))}
                        </div>
                        <Button onClick={handleGenerate} disabled={isLoading} >
                             {isLoading ? ( <> <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> Generating... </> ) : <><SparklesIcon className="w-4 h-4"/> Generate Content</>}
                        </Button>
                    </Card>
                </div>
                <div className="lg:col-span-2">
                    <Card>
                        <div className="flex justify-between items-center mb-4 pb-3 border-b">
                            <h3 className="text-xl font-bold text-gray-700">Generated Content</h3>
                            {generatedContent && (
                                <Button onClick={handleCopy} variant="secondary">
                                    {copySuccess ? <CheckIcon/> : <CopyIcon/>}
                                    {copySuccess ? 'Copied!' : 'Copy'}
                                </Button>
                            )}
                        </div>
                         {isLoading ? (
                            <div className="flex items-center justify-center h-48 text-gray-500">
                                <SparklesIcon className="w-5 h-5 animate-pulse mr-2" /> AI is crafting your content...
                            </div>
                         ) : generatedContent ? (
                            <pre className="bg-gray-50 p-4 rounded-lg text-sm whitespace-pre-wrap font-sans text-gray-800 max-h-96 overflow-y-auto">{generatedContent}</pre>
                         ) : (
                            <div className="text-center py-10 text-gray-500">
                                <DocumentArrowUpIcon className="w-12 h-12 mx-auto text-gray-300 mb-2" />
                                <p>Select an export type and click "Generate Content".</p>
                            </div>
                         )}
                    </Card>
                </div>
            </div>
        </div>
    );
};

const HistoryScreen: React.FC<{ history: SprintData[]; masterHolidays: MasterHoliday[] }> = ({ history, masterHolidays }) => {
    const [viewingSprint, setViewingSprint] = useState<SprintData | null>(null);
    const [selectedSprintIds, setSelectedSprintIds] = useState<Set<string>>(new Set());
    const [isInsightsModalOpen, setIsInsightsModalOpen] = useState(false);
    const [insights, setInsights] = useState('');
    const [isInsightsLoading, setIsInsightsLoading] = useState(false);

    const sortedHistory = useMemo(() => 
        history.slice()
               .sort((a, b) => new Date(b.settings.endDate).getTime() - new Date(a.settings.endDate).getTime()),
    [history]);

    const handleSelectSprint = (sprintId: string) => {
        const newSelection = new Set(selectedSprintIds);
        if (newSelection.has(sprintId)) {
            newSelection.delete(sprintId);
        } else {
            newSelection.add(sprintId);
        }
        setSelectedSprintIds(newSelection);
    };

    const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.checked) {
            setSelectedSprintIds(new Set(sortedHistory.map(s => s.id)));
        } else {
            setSelectedSprintIds(new Set());
        }
    };
    
    const renderSummaryContent = (text: string) => {
        return text.split('\n').filter(line => line.trim() !== '').map((line, index) => {
            const boldedLine = line.replace(/\*\*(.*?)\*\*/g, '<strong class="text-gray-900">$1</strong>');
            const content = boldedLine.replace(/^- /, '').trim();
            return <li key={index} className="flex items-start"><span className="text-primary-500 mr-2 mt-1">&#8227;</span><span dangerouslySetInnerHTML={{ __html: content }} /></li>;
        });
    };

    const handleGenerateInsights = async () => {
        setIsInsightsModalOpen(true);
        setIsInsightsLoading(true);
        setInsights('');

        const selectedSprints = history.filter(s => selectedSprintIds.has(s.id));

        const velocityTrend = selectedSprints.map(sprint => ({
            sprintId: sprint.id,
            endDate: sprint.settings.endDate,
            totalHours: sprint.tasks.reduce((acc, task) => acc + task.storyPoints, 0),
        }));

        const workMixTrend = selectedSprints.map(sprint => {
            const totalHours = sprint.tasks.reduce((acc, task) => acc + task.storyPoints, 0);
            const mixByCategory: { [key: string]: number } = { FEATURE: 0, TECH_DEBT: 0, PROD_ISSUE: 0 };
            sprint.tasks.forEach(task => {
                const category = task.category || 'OTHER';
                mixByCategory[category] = (mixByCategory[category] || 0) + task.storyPoints;
            });
            const mixPercentage = totalHours > 0 ? {
                FEATURE: (mixByCategory.FEATURE / totalHours) * 100,
                TECH_DEBT: (mixByCategory.TECH_DEBT / totalHours) * 100,
                PROD_ISSUE: (mixByCategory.PROD_ISSUE / totalHours) * 100,
            } : { FEATURE: 0, TECH_DEBT: 0, PROD_ISSUE: 0 };
            return { sprintId: sprint.id, endDate: sprint.settings.endDate, mix: mixPercentage };
        });

        const roleData: { [key in Role]?: { totalCapacity: number, assignedHours: number } } = {};
        selectedSprints.forEach(sprint => {
            const sprintCapacity = calculateCapacitySummary(sprint, masterHolidays);
            sprint.team.forEach(member => {
                if (!roleData[member.role]) {
                    roleData[member.role] = { totalCapacity: 0, assignedHours: 0 };
                }
                const memberCapacity = sprintCapacity.find(sc => sc.memberId === member.id);
                if (memberCapacity) {
                    roleData[member.role]!.totalCapacity += memberCapacity.totalCapacity;
                    roleData[member.role]!.assignedHours += memberCapacity.assignedHours;
                }
            });
        });

        const roleUtilization = Object.entries(roleData).map(([role, data]) => ({
            role,
            utilization: data.totalCapacity > 0 ? (data.assignedHours / data.totalCapacity) * 100 : 0,
        }));

        const insightsResult = await generatePerformanceInsights(selectedSprints, velocityTrend, workMixTrend, roleUtilization);
        setInsights(insightsResult);
        setIsInsightsLoading(false);
    };


    const SprintDetailModal: React.FC<{
        sprint: SprintData;
        onClose: () => void;
    }> = ({ sprint, onClose }) => {
        const capacity = useMemo(() => calculateCapacitySummary(sprint, masterHolidays), [sprint, masterHolidays]);
        const [summary, setSummary] = useState('');
        const [isLoading, setIsLoading] = useState(false);
        
        const handleGenerateSummary = async () => {
            setIsLoading(true);
            const result = await generateSprintSummary(sprint.team, sprint.settings, sprint.tasks, capacity);
            setSummary(result);
            setIsLoading(false);
        };

        const totalHours = sprint.tasks.reduce((acc, task) => acc + task.storyPoints, 0);

        return (
            <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4 animate-fade-in-fast">
                <div className="bg-white p-6 rounded-xl shadow-2xl w-full max-w-6xl max-h-[90vh] flex flex-col">
                    <div className="border-b pb-4 mb-4">
                        <h2 className="text-2xl font-bold text-gray-800">Sprint Review</h2>
                        <p className="text-gray-500">{sprint.settings.startDate} to {sprint.settings.endDate}</p>
                    </div>

                    <div className="flex-grow overflow-y-auto pr-4 -mr-4 grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Left Column */}
                        <div className="lg:col-span-2 space-y-6">
                            <Card>
                                <h3 className="text-xl font-bold text-gray-700 mb-4">Sprint Backlog ({sprint.tasks.length} tasks)</h3>
                                <div className="max-h-80 overflow-y-auto pr-2 space-y-2">
                                    {sprint.tasks.map(task => (
                                        <div key={task.id} className="p-3 bg-gray-50 rounded-md text-sm">
                                            <p className="font-semibold text-gray-800">{task.key}: {task.summary}</p>
                                            <p className="text-xs text-gray-500">{task.category} &bull; {task.storyPoints}h &bull; Assignee: {sprint.team.find(m => m.id === task.assigneeId)?.name || 'Unassigned'}</p>
                                        </div>
                                    ))}
                                </div>
                            </Card>
                             <Card>
                                <h3 className="text-xl font-bold text-gray-700 mb-4">AI Summary</h3>
                                {isLoading ? (
                                    <div className="flex items-center justify-center h-24 text-gray-500"><SparklesIcon className="w-5 h-5 animate-pulse mr-2" /> Generating summary...</div>
                                ) : summary ? (
                                    <ul className="text-sm text-gray-600 space-y-1">{renderSummaryContent(summary)}</ul>
                                ) : (
                                    <div className="text-center py-2"><p className="text-gray-500 text-sm mb-3">Get a quick overview of this sprint.</p><Button onClick={handleGenerateSummary}><SparklesIcon className="w-4 h-4"/> Generate Summary</Button></div>
                                )}
                            </Card>
                        </div>
                        {/* Right Column */}
                        <div className="space-y-6">
                             <Card>
                                <h3 className="text-xl font-bold text-gray-700 mb-4">Quick Stats</h3>
                                <div className="space-y-3">
                                    <div className="flex justify-between items-baseline"><span className="text-gray-600">Total Hours Delivered:</span><span className="font-bold text-2xl text-primary-600">{totalHours}h</span></div>
                                    <div className="flex justify-between items-baseline"><span className="text-gray-600">Tasks Completed:</span><span className="font-bold text-2xl text-primary-600">{sprint.tasks.length}</span></div>
                                    <div className="flex justify-between items-baseline"><span className="text-gray-600">Team Size:</span><span className="font-bold text-2xl text-primary-600">{sprint.team.length}</span></div>
                                </div>
                            </Card>
                            <Card>
                                <h3 className="text-xl font-bold text-gray-700 mb-4">Team Snapshot</h3>
                                <div className="max-h-60 overflow-y-auto pr-2 space-y-2">
                                    {capacity.map(member => (
                                        <div key={member.memberId} className="p-2 bg-gray-50 rounded-md text-sm">
                                            <div className="flex justify-between font-semibold"><span>{member.memberName}</span><span className={member.status === 'Overloaded' ? 'text-danger' : ''}>{member.assignedHours}h / {member.totalCapacity}h</span></div>
                                        </div>
                                    ))}
                                </div>
                            </Card>
                        </div>
                    </div>

                    <div className="border-t pt-4 mt-4 flex justify-end space-x-3">
                        <Button onClick={onClose} variant="secondary">Close</Button>
                    </div>
                </div>
            </div>
        );
    };
    
    const InsightsModal = () => (
         <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4 animate-fade-in-fast">
            <div className="bg-white p-6 rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col">
                <h2 className="text-2xl font-bold text-gray-800 border-b pb-4 mb-4 flex items-center"><ChartBarIcon className="w-6 h-6 mr-2 text-primary-500" /> AI Performance Insights</h2>
                <div className="flex-grow overflow-y-auto pr-2">
                    {isInsightsLoading ? (
                        <div className="flex items-center justify-center h-48 text-gray-500"><SparklesIcon className="w-6 h-6 animate-pulse mr-2" /> Analyzing trends and generating insights...</div>
                    ) : insights ? (
                        <div className="text-sm text-gray-700 space-y-2 prose prose-sm max-w-none">
                            <ul className="space-y-2">{renderSummaryContent(insights)}</ul>
                        </div>
                    ) : <p>No insights generated yet.</p>}
                </div>
                <div className="border-t pt-4 mt-4 flex justify-end">
                    <Button onClick={() => setIsInsightsModalOpen(false)} variant="secondary">Close</Button>
                </div>
            </div>
        </div>
    );

    return (
        <div className="space-y-6">
            {viewingSprint && <SprintDetailModal sprint={viewingSprint} onClose={() => setViewingSprint(null)} />}
            {isInsightsModalOpen && <InsightsModal />}
            <h2 className="text-3xl font-bold text-gray-800">Sprint History</h2>
            <p className="text-gray-600">Review past sprints to identify trends and inform future planning. Select sprints to generate AI-powered performance insights.</p>
            <Card className="p-0 overflow-hidden">
                {selectedSprintIds.size > 0 && (
                     <div className="bg-primary-50 p-3 border-b border-primary-200 flex justify-between items-center animate-fade-in-fast">
                        <p className="text-sm font-semibold text-primary-800">{selectedSprintIds.size} sprint(s) selected.</p>
                        <Button onClick={handleGenerateInsights} disabled={isInsightsLoading}>
                            <ChartBarIcon className="w-4 h-4"/>
                            Generate Insights
                        </Button>
                    </div>
                )}
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white text-sm">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="py-3 px-4 text-center">
                                    <input 
                                        type="checkbox" 
                                        className="rounded border-gray-300 text-primary-600 shadow-sm focus:border-primary-300 focus:ring focus:ring-offset-0 focus:ring-primary-200 focus:ring-opacity-50"
                                        onChange={handleSelectAll}
                                        checked={sortedHistory.length > 0 && selectedSprintIds.size === sortedHistory.length}
                                        aria-label="Select all sprints"
                                    />
                                </th>
                                <th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Sprint Dates</th>
                                <th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Team Size</th>
                                <th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Tasks Delivered</th>
                                <th className="text-left py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Total Hours</th>
                                <th className="text-center py-3 px-4 font-semibold text-gray-600 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {history.length === 0 && (
                                <tr><td colSpan={6} className="text-center py-10 text-gray-500">No sprints have been archived yet.</td></tr>
                            )}
                            {sortedHistory.map(sprint => (
                                <tr key={sprint.id} className={selectedSprintIds.has(sprint.id) ? 'bg-primary-50' : ''}>
                                    <td className="py-3 px-4 text-center">
                                        <input 
                                            type="checkbox"
                                            className="rounded border-gray-300 text-primary-600 shadow-sm focus:border-primary-300 focus:ring focus:ring-offset-0 focus:ring-primary-200 focus:ring-opacity-50"
                                            checked={selectedSprintIds.has(sprint.id)}
                                            onChange={() => handleSelectSprint(sprint.id)}
                                            aria-label={`Select sprint from ${sprint.settings.startDate}`}
                                        />
                                    </td>
                                    <td className="py-3 px-4 text-gray-700 font-semibold">{sprint.settings.startDate} to {sprint.settings.endDate}</td>
                                    <td className="py-3 px-4 text-gray-700">{sprint.team.length} members</td>
                                    <td className="py-3 px-4 text-gray-700">{sprint.tasks.length} items</td>
                                    <td className="py-3 px-4 text-gray-700 font-bold">{sprint.tasks.reduce((sum, task) => sum + task.storyPoints, 0)}h</td>
                                    <td className="py-3 px-4 text-center">
                                        <Button onClick={() => setViewingSprint(sprint) } variant="secondary">
                                            <EyeIcon /> View Details
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </Card>
        </div>
    );
};

const StartNewSprintScreen: React.FC<{
    onStartBlank: () => void;
    onStartFromTemplate: () => void;
}> = ({ onStartBlank, onStartFromTemplate }) => {
    return (
        <div className="text-center py-16">
            <span className="text-6xl">🚀</span>
            <h2 className="text-4xl font-extrabold text-gray-800 mt-4">Start Your Next Sprint</h2>
            <p className="text-lg text-gray-500 mt-2 max-w-2xl mx-auto">Your previous sprint has been archived. Choose how you'd like to begin planning for the next one.</p>
            <div className="mt-8 flex justify-center space-x-4">
                <Button onClick={onStartBlank}>
                    <PlusIcon className="w-5 h-5"/> Start a Blank Sprint
                </Button>
                <Button onClick={onStartFromTemplate} variant="secondary">
                    <CopyIcon /> Use a Past Sprint as a Template
                </Button>
            </div>
        </div>
    );
};

const TemplateSelectionModal: React.FC<{
    history: SprintData[];
    onSelect: (sprint: SprintData) => void;
    onClose: () => void;
}> = ({ history, onSelect, onClose }) => {
    return (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
            <div className="bg-white p-6 rounded-xl shadow-2xl w-full max-w-2xl">
                <h3 className="text-2xl font-bold mb-4 text-gray-800">Select a Template</h3>
                <p className="text-sm text-gray-500 mb-6">Choose a past sprint to use as a starting point. The team and task structure will be copied over.</p>
                <div className="max-h-96 overflow-y-auto space-y-2 pr-2">
                    {history.slice().sort((a, b) => new Date(b.settings.endDate).getTime() - new Date(a.settings.endDate).getTime()).map(sprint => (
                        <div key={sprint.id} onClick={() => onSelect(sprint)} className="p-4 border rounded-lg hover:bg-primary-50 hover:border-primary-300 cursor-pointer transition-colors">
                             <p className="font-semibold text-gray-900">{sprint.settings.startDate} to {sprint.settings.endDate}</p>
                             <p className="text-xs text-gray-500">{sprint.team.length} members &bull; {sprint.tasks.length} tasks</p>
                        </div>
                    ))}
                </div>
                <div className="mt-6 flex justify-end">
                    <Button onClick={onClose} variant="secondary">Cancel</Button>
                </div>
            </div>
        </div>
    );
};

const App = () => {
    const [currentScreen, setCurrentScreen] = useState<Screen>(Screen.Dashboard);
    const [sprintData, setSprintData] = useState<SprintData | null>(null);
    const [masterHolidays, setMasterHolidays] = useState<MasterHoliday[]>(initialMasterHolidays);
    const [sprintHistory, setSprintHistory] = useState<SprintData[]>(mockHistory);
    const [isTemplateModalOpen, setIsTemplateModalOpen] = useState(false);

    useEffect(() => {
        // Load from localStorage or start a new sprint on initial load
        const savedData = localStorage.getItem('sprintData');
        if (savedData) {
            setSprintData(JSON.parse(savedData));
        }
        const savedHolidays = localStorage.getItem('masterHolidays');
        if (savedHolidays) setMasterHolidays(JSON.parse(savedHolidays));
         const savedHistory = localStorage.getItem('sprintHistory');
        if (savedHistory) setSprintHistory(JSON.parse(savedHistory));
    }, []);

    const updateAndSaveSprint = useCallback((data: SprintData | null) => {
        setSprintData(data);
        if (data) {
            localStorage.setItem('sprintData', JSON.stringify(data));
        } else {
            localStorage.removeItem('sprintData');
        }
    }, []);

    const handleTeamUpdate = (team: TeamMember[]) => {
        if (sprintData) updateAndSaveSprint({ ...sprintData, team });
    };

    const handleSettingsUpdate = (settings: SprintSettings) => {
        if (sprintData) updateAndSaveSprint({ ...sprintData, settings });
    };

    const handleTasksUpdate = (tasks: JiraTask[]) => {
        if (sprintData) updateAndSaveSprint({ ...sprintData, tasks });
    };

    const handleHolidaysUpdate = (holidays: MasterHoliday[]) => {
        setMasterHolidays(holidays);
        localStorage.setItem('masterHolidays', JSON.stringify(holidays));
    };
    
    const handleCompleteSprint = () => {
        if (!sprintData || !window.confirm("Are you sure you want to complete and archive this sprint? This will start a new, empty sprint.")) return;
        
        const newHistory = [...sprintHistory, sprintData];
        setSprintHistory(newHistory);
        localStorage.setItem('sprintHistory', JSON.stringify(newHistory));
        
        updateAndSaveSprint(null); // Set current sprint to null
        
        alert("Sprint archived! You can now start a new sprint.");
        setCurrentScreen(Screen.Dashboard);
    };

    const handleStartBlankSprint = () => {
        const newSprint = createNewSprint();
        updateAndSaveSprint(newSprint);
        setCurrentScreen(Screen.Dashboard);
    };
    
    const handleLoadFromTemplate = (template: SprintData) => {
        const newSprint = createNewSprint(); // Gets new ID and default dates
        
        // Intelligently copy data
        newSprint.team = JSON.parse(JSON.stringify(template.team));
        newSprint.tasks = JSON.parse(JSON.stringify(template.tasks)).map((task: JiraTask) => ({
            ...task,
            id: `task-${Date.now()}-${Math.random()}`, // new task id
            assigneeId: undefined, // unassign tasks
            startDate: undefined,
            dueDate: undefined,
            timeSpent: undefined
        }));

        updateAndSaveSprint(newSprint);
        setIsTemplateModalOpen(false);
        setCurrentScreen(Screen.Dashboard);
        alert("Created a new sprint from the selected template!");
    };

    const capacitySummary = useMemo(() => calculateCapacitySummary(sprintData, masterHolidays), [sprintData, masterHolidays]);

    const renderScreen = () => {
        if (!sprintData) {
            return <StartNewSprintScreen onStartBlank={handleStartBlankSprint} onStartFromTemplate={() => setIsTemplateModalOpen(true)} />;
        }
        switch (currentScreen) {
            case Screen.Dashboard:
                return <DashboardScreen setCurrentScreen={setCurrentScreen} sprint={sprintData} capacity={capacitySummary} onCompleteSprint={handleCompleteSprint} />;
            case Screen.TeamConfig:
                return <TeamConfigScreen team={sprintData.team} onTeamUpdate={handleTeamUpdate} />;
            case Screen.SprintSetup:
                return <SprintSetupScreen settings={sprintData.settings} onSettingsUpdate={handleSettingsUpdate} masterHolidays={masterHolidays} />;
            case Screen.HolidayMaster:
                return <HolidayMasterScreen holidays={masterHolidays} onUpdate={handleHolidaysUpdate} />;
            case Screen.TaskPlanning:
                return <TaskPlanningScreen sprintData={sprintData} capacity={capacitySummary} onTasksUpdate={handleTasksUpdate} />;
            case Screen.Export:
                return <ExportScreen sprint={sprintData} capacity={capacitySummary} />;
            case Screen.History:
                 return <HistoryScreen history={sprintHistory} masterHolidays={masterHolidays}/>;
            default:
                return <DashboardScreen setCurrentScreen={setCurrentScreen} sprint={sprintData} capacity={capacitySummary} onCompleteSprint={handleCompleteSprint} />;
        }
    };

    return (
        <div className="bg-gray-50 min-h-screen font-sans">
            <Header currentScreen={currentScreen} setCurrentScreen={setCurrentScreen} sprintActive={!!sprintData} />
            <main className="container mx-auto px-6 py-10">
                {renderScreen()}
                {isTemplateModalOpen && (
                    <TemplateSelectionModal 
                        history={sprintHistory}
                        onSelect={handleLoadFromTemplate}
                        onClose={() => setIsTemplateModalOpen(false)} 
                    />
                )}
            </main>
            <footer className="text-center py-6 text-sm text-gray-400">
                <p>&copy; {new Date().getFullYear()} SprintPilot. All rights reserved.</p>
            </footer>
        </div>
    );
};

// FIX: Add default export for the App component to be used in index.tsx
export default App;