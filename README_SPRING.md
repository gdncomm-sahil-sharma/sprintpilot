# SprintPilot - Spring Boot 3.x Migration

## 🚀 Project Overview

SprintPilot is a comprehensive Sprint Management System migrated from React to Spring Boot 3.x with pixel-perfect UI replication using Thymeleaf. The application provides full sprint lifecycle management, AI-powered insights via Google Gemini, and extensive team collaboration features.

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 21
- **Frontend**: Thymeleaf, Bootstrap 5.3, Chart.js
- **Database**: PostgreSQL (with H2 for development)
- **AI Integration**: Spring AI with Google Gemini
- **Build Tool**: Maven 3.9+

## 📁 Project Structure

```
sprintpilot/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sprintpilot/
│   │   │           ├── config/
│   │   │           │   ├── AppConfig.java
│   │   │           │   ├── GeminiConfig.java ✅
│   │   │           │   └── WebConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AIController.java ✅
│   │   │           │   ├── PageController.java ✅
│   │   │           │   ├── SprintController.java ✅
│   │   │           │   ├── TaskController.java
│   │   │           │   ├── TeamController.java
│   │   │           │   └── HolidayController.java
│   │   │           ├── service/
│   │   │           │   ├── AIService.java ✅
│   │   │           │   ├── SprintService.java ✅
│   │   │           │   ├── TaskService.java
│   │   │           │   ├── TeamService.java
│   │   │           │   ├── HolidayService.java
│   │   │           │   └── CapacityService.java
│   │   │           ├── service/impl/
│   │   │           │   ├── GeminiAIService.java ✅
│   │   │           │   ├── MockAIService.java ✅
│   │   │           │   ├── SprintServiceImpl.java ✅
│   │   │           │   └── [Other service implementations]
│   │   │           ├── repository/
│   │   │           │   ├── SprintRepository.java ✅
│   │   │           │   ├── TaskRepository.java ✅
│   │   │           │   ├── TeamMemberRepository.java ✅
│   │   │           │   ├── HolidayRepository.java ✅
│   │   │           │   └── SprintEventRepository.java ✅
│   │   │           ├── entity/
│   │   │           │   ├── Sprint.java ✅
│   │   │           │   ├── Task.java ✅
│   │   │           │   ├── TeamMember.java ✅
│   │   │           │   ├── Holiday.java ✅
│   │   │           │   ├── SprintEvent.java ✅
│   │   │           │   ├── LeaveDay.java ✅
│   │   │           │   └── SprintHistory.java ✅
│   │   │           ├── dto/
│   │   │           │   ├── ApiResponse.java ✅
│   │   │           │   ├── SprintDto.java ✅
│   │   │           │   ├── TaskDto.java ✅
│   │   │           │   ├── TeamMemberDto.java ✅
│   │   │           │   ├── CapacitySummaryDto.java ✅
│   │   │           │   ├── TaskRiskDto.java ✅
│   │   │           │   └── HolidayDto.java ✅
│   │   │           ├── mapper/
│   │   │           │   └── [Entity to DTO mappers]
│   │   │           ├── util/
│   │   │           │   ├── DateUtils.java ✅
│   │   │           │   └── [Other utilities]
│   │   │           ├── exception/
│   │   │           │   └── [Custom exceptions]
│   │   │           └── SprintPilotApplication.java ✅
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   │   └── layout.html ✅
│   │       │   ├── dashboard/
│   │       │   │   └── index.html ✅
│   │       │   ├── sprint/
│   │       │   │   ├── new.html
│   │       │   │   ├── view.html
│   │       │   │   ├── edit.html
│   │       │   │   └── timeline.html
│   │       │   ├── team/
│   │       │   ├── tasks/
│   │       │   ├── analytics/
│   │       │   └── configuration/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── styles.css ✅
│   │       │   ├── js/
│   │       │   │   └── app.js ✅
│   │       │   └── icons/
│   │       ├── dummy-data/
│   │       │   └── [JSON mock data files]
│   │       ├── application.yaml ✅
│   │       └── schema.sql ✅
├── pom.xml ✅
└── README.md

✅ = Completed
```

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL (optional, H2 used by default)
- Google Cloud account with Gemini API access (optional)

### Installation

1. **Clone the repository**
```bash
cd /Users/sahilsharma/Downloads/sprintpilot
```

2. **Configure environment variables** (Optional for AI features)
```bash
export GEMINI_API_KEY="your-gemini-api-key"
export GOOGLE_CLOUD_PROJECT_ID="your-project-id"
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

5. **Access the application**
```
http://localhost:8080
```

## 🔧 Configuration

### Database Configuration

By default, the application uses H2 in-memory database. To use PostgreSQL:

```yaml
# Set these environment variables or update application.yaml
DB_URL=jdbc:postgresql://localhost:5432/sprintpilot
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
```

### AI Configuration

The application supports two AI modes:

1. **Real Gemini Integration**: Set your API key
```bash
export GEMINI_API_KEY="your-api-key"
export AI_MOCK_MODE=false
```

2. **Mock Mode** (Default): No API key required
```bash
export AI_MOCK_MODE=true
```

## 📋 Features Implemented

### ✅ Core Features
- [x] Spring Boot 3.x project structure
- [x] Database schema and entities
- [x] DTO layer with validation
- [x] Repository interfaces with JPA
- [x] Service layer with mock implementations
- [x] REST API controllers
- [x] Spring AI Gemini integration
- [x] Mock AI service for development
- [x] Basic Thymeleaf templates
- [x] Static assets (CSS, JS)
- [x] Date utilities for working days calculation

### 🔨 Features In Progress

#### Additional Services Needed
1. **TeamService** - Team member CRUD operations
2. **TaskService** - Task management and assignment
3. **HolidayService** - Holiday management
4. **CapacityService** - Capacity calculations

#### Additional Controllers Needed
1. **TeamController** - Team management endpoints
2. **TaskController** - Task management endpoints  
3. **HolidayController** - Holiday configuration

#### Thymeleaf Templates Needed
1. **Sprint Management**
   - `/templates/sprint/new.html` - New sprint creation
   - `/templates/sprint/view.html` - Sprint details view
   - `/templates/sprint/edit.html` - Sprint editing
   - `/templates/sprint/timeline.html` - Sprint timeline visualization

2. **Team Management**
   - `/templates/team/index.html` - Team member list
   - `/templates/team/capacity.html` - Capacity management

3. **Task Management**
   - `/templates/tasks/index.html` - Task list
   - `/templates/tasks/import.html` - CSV/Jira import

4. **Analytics & History**
   - `/templates/analytics/index.html` - Performance analytics
   - `/templates/history/index.html` - Sprint history

5. **Configuration**
   - `/templates/configuration/holidays.html` - Holiday management
   - `/templates/configuration/settings.html` - App settings

## 🎨 UI Components Replicated

- Dashboard with sprint overview cards
- Work distribution donut chart
- Sprint velocity line chart
- Responsive navigation bar
- Bootstrap-based forms and tables
- AI response containers
- Risk indicators (On Track/At Risk/Off Track)
- Capacity status badges

## 🧪 Testing the Application

### API Endpoints

**Sprint Management:**
- `GET /api/sprints` - Get all sprints
- `GET /api/sprints/active` - Get active sprints
- `POST /api/sprints/create` - Create new sprint
- `PUT /api/sprints/{id}` - Update sprint
- `POST /api/sprints/{id}/start` - Start sprint
- `POST /api/sprints/{id}/complete` - Complete sprint

**AI Services:**
- `POST /api/ai/sprint-summary` - Generate sprint summary
- `POST /api/ai/meeting-invite` - Generate meeting invite
- `POST /api/ai/risk-summary` - Generate risk analysis
- `POST /api/ai/confluence-page` - Generate Confluence page
- `POST /api/ai/teams-message` - Generate Teams message

### Sample API Call

```javascript
// Generate Sprint Summary
fetch('/api/ai/sprint-summary', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
        team: [...],
        sprint: {...},
        tasks: [...],
        workload: [...]
    })
})
```

## 📝 Next Steps for Complete Migration

1. **Complete Service Implementations**
   - Implement TeamService, TaskService, HolidayService
   - Add transaction management where needed
   - Implement actual database operations (currently using mocks)

2. **Complete Thymeleaf Templates**
   - Create all remaining HTML templates
   - Add form validation
   - Implement modal dialogs for AI responses

3. **Add Missing Features**
   - CSV import functionality
   - Jira integration mock
   - Export to Confluence/Teams
   - Historical analytics charts

4. **Exception Handling**
   - Create custom exception classes
   - Add global exception handler
   - Implement proper error pages

5. **Testing**
   - Add unit tests for services
   - Add integration tests for controllers
   - Add UI tests with Selenium

6. **Production Readiness**
   - Add security configuration (Spring Security)
   - Implement user authentication
   - Add API documentation (Swagger/OpenAPI)
   - Configure logging properly
   - Add monitoring endpoints (Actuator)

## 🤝 Contributing

To continue the migration:

1. Pick a pending component from the structure above
2. Follow the established patterns:
   - DTOs use records with validation
   - Services have interface + implementation
   - Controllers return ApiResponse wrapper
   - Thymeleaf templates extend the layout fragment
3. Maintain UI consistency with the React version
4. Use mock data for features not yet connected to DB

## 📄 License

This project is a migration exercise from React to Spring Boot.

## 🆘 Support

For issues or questions about the migration:
1. Check the existing code patterns
2. Refer to Spring Boot 3.x documentation
3. Follow the SOLID principles established in the codebase

---

**Note:** This is a work-in-progress migration. The core structure is complete, but additional features need to be implemented to achieve full parity with the React version.
