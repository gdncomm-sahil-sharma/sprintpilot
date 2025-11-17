# Microsoft Teams Notification Templates

This directory contains JSON templates for Microsoft Teams notifications.

## Template Format

Templates use the Microsoft Teams MessageCard format with placeholder variables in the format `{{variableName}}`.

## Available Templates

- `holiday-created.json` - Notification when a holiday is created
- `holiday-updated.json` - Notification when a holiday is updated
- `sprint-created.json` - Notification when a sprint is created
- `sprint-completed.json` - Notification when a sprint is completed
- `task-assigned.json` - Notification when a task is assigned

## Adding New Templates

1. Create a new JSON file in this directory
2. Use the MessageCard format (see examples)
3. Use `{{variableName}}` for placeholders
4. The service will automatically replace placeholders with actual values

## Template Variables

Each template defines its own variables. Check the template file to see which variables are expected.

Example:
- `{{holidayName}}` - Will be replaced with the actual holiday name
- `{{sprintId}}` - Will be replaced with the sprint ID

## Usage

```java
@Autowired
private TeamsNotificationService teamsNotificationService;

// Send notification
Map<String, String> variables = new HashMap<>();
variables.put("holidayName", "New Year");
variables.put("holidayDate", "2024-01-01");
teamsNotificationService.sendNotification("holiday-created.json", variables);
```

