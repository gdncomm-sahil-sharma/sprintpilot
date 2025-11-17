# Confluence Page Templates

This directory contains HTML templates for Confluence sprint documentation pages.

## Template Format

Templates use HTML format with placeholder variables in the format `{{variableName}}`. The templates are converted to Confluence Storage Format when creating/updating pages.

## Available Templates

### `sprint-planning-page.html`
Template for creating sprint planning pages when a sprint is started.

**Template Variables:**
- `{{sprintId}}` - Sprint ID
- `{{startDate}}` - Sprint start date
- `{{endDate}}` - Sprint end date
- `{{duration}}` - Sprint duration in working days
- `{{status}}` - Sprint status
- `{{freezeDate}}` - Sprint freeze date (optional)
- `{{teamSize}}` - Number of team members
- `{{teamMembers}}` - HTML formatted list of team members
- `{{totalCapacity}}` - Total team capacity
- `{{sprintCapacity}}` - Sprint capacity
- `{{plannedStoryPoints}}` - Planned story points
- `{{averageVelocity}}` - Average velocity
- `{{workingDays}}` - Number of working days
- `{{holidayCount}}` - Number of holidays in sprint
- `{{holidays}}` - List of holiday dates
- `{{plannedTasks}}` - HTML table of planned tasks
- `{{totalPlannedStoryPoints}}` - Total planned story points
- `{{featureStoryPoints}}` - Feature story points
- `{{featurePercentage}}` - Feature percentage
- `{{techDebtStoryPoints}}` - Tech debt story points
- `{{techDebtPercentage}}` - Tech debt percentage
- `{{prodIssueStoryPoints}}` - Production issue story points
- `{{prodIssuePercentage}}` - Production issue percentage
- `{{events}}` - HTML list of sprint events
- `{{createdAt}}` - Page creation timestamp
- `{{updatedAt}}` - Page update timestamp

### `sprint-summary-section.html`
Template for appending sprint completion summary to existing sprint pages.

**Template Variables:**
- `{{completionDate}}` - Sprint completion date
- `{{completionStatus}}` - Sprint completion status
- `{{completionRate}}` - Completion rate percentage
- `{{totalStoryPoints}}` - Total story points planned
- `{{completedStoryPoints}}` - Completed story points
- `{{incompleteStoryPoints}}` - Incomplete story points
- `{{completionPercentage}}` - Completion percentage
- `{{velocity}}` - Sprint velocity
- `{{teamSize}}` - Team size
- `{{averageVelocity}}` - Average velocity
- `{{averageVelocity3Sprints}}` - Average velocity (last 3 sprints)
- `{{velocityTrend}}` - Velocity trend (Increasing/Decreasing/Stable)
- `{{velocityVsPlan}}` - Velocity vs plan comparison
- `{{completedTasks}}` - HTML table of completed tasks
- `{{completedTasksCount}}` - Number of completed tasks
- `{{incompleteTasks}}` - HTML table of incomplete tasks
- `{{incompleteTasksCount}}` - Number of incomplete tasks
- `{{featurePlanned}}` - Feature story points planned
- `{{featureCompleted}}` - Feature story points completed
- `{{featureCompletionPercentage}}` - Feature completion percentage
- `{{techDebtPlanned}}` - Tech debt story points planned
- `{{techDebtCompleted}}` - Tech debt story points completed
- `{{techDebtCompletionPercentage}}` - Tech debt completion percentage
- `{{prodIssuePlanned}}` - Production issue story points planned
- `{{prodIssueCompleted}}` - Production issue story points completed
- `{{prodIssueCompletionPercentage}}` - Production issue completion percentage
- `{{activeTeamMembers}}` - Number of active team members
- `{{capacityUtilized}}` - Capacity utilization percentage
- `{{storyPointsPerMember}}` - Average story points per member
- `{{teamPerformance}}` - Team performance assessment
- `{{blockers}}` - HTML list of blockers encountered
- `{{achievements}}` - HTML list of key achievements
- `{{lessonsLearned}}` - HTML list of lessons learned
- `{{recommendations}}` - HTML list of recommendations
- `{{generatedAt}}` - Summary generation timestamp

## Page Naming Convention

Pages are created with the following naming format:
```
Sprint {sprintId} - {month} {year}
```

Example: `Sprint SPRINT-001 - January 2024`

This allows the system to search for and update existing pages without storing page IDs in the database.

## Usage

Templates are automatically used by `ConfluenceService` when:
1. Creating sprint pages (when sprint is started)
2. Updating sprint pages with completion summary (when sprint is completed)

## Adding New Templates

1. Create a new HTML file in this directory
2. Use `{{variableName}}` for placeholders
3. Use conditional blocks: `{{#if variable}}...{{/if}}`
4. Update `ConfluenceServiceImpl` to use the new template

## Template Features

- **HTML Format**: Easy to edit and maintain
- **Placeholder Replacement**: Automatic variable substitution
- **Conditional Blocks**: Support for `{{#if}}` blocks
- **HTML Escaping**: Automatic escaping of special characters
- **Confluence Conversion**: Automatic conversion to Confluence Storage Format

