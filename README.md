# 🚀 SprintPilot

> **AI-Powered Sprint Management System** - A comprehensive solution for agile teams to plan, track, and optimize their sprints with intelligent insights.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.0-38B2AC.svg)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Team Setup](#-team-setup)
- [Contributing](#-contributing)
- [License](#-license)

## 🎯 Overview

SprintPilot is a modern, full-stack sprint management system designed to help agile teams optimize their workflow through intelligent planning, real-time tracking, and AI-powered insights. Built with Spring Boot 3.x and featuring a pixel-perfect UI inspired by modern React applications.

### 🎪 **Perfect for:**
- **Agile Development Teams** planning and tracking sprints
- **Project Managers** needing comprehensive sprint oversight
- **Organizations** wanting data-driven sprint optimization
- **Teams** looking to integrate Jira with custom workflows

## ✨ Key Features

### 🏃‍♂️ **Sprint Management**
- **Complete Sprint Lifecycle**: Planning → Active → Completed → Archived
- **Interactive Timeline**: Visual sprint progress with key milestones
- **Capacity Planning**: Team workload distribution and optimization
- **Working Days Calculator**: Automatic exclusion of holidays and weekends
- **Freeze Date Management**: Lock sprint scope at defined dates

### 📊 **Task Import & Management**
- **CSV Import**: Drag-and-drop CSV upload with intelligent column mapping
- **Jira Integration**: Direct import from Jira with JQL query support
- **Task Categories**: Feature, Tech Debt, Production Issues, Other
- **Priority Management**: 5-level priority system (Lowest to Critical)
- **Assignment Tracking**: Multi-assignee support with capacity validation

### 🤖 **AI-Powered Insights**
- **Sprint Summaries**: Automated sprint overview generation
- **Risk Analysis**: Intelligent identification of potential blockers
- **Performance Predictions**: Historical data-driven forecasting
- **Meeting Invite Generation**: AI-crafted meeting descriptions
- **Confluence Integration**: Automated documentation generation

### 👥 **Team Management**
- **Member Profiles**: Detailed team member information and roles
- **Capacity Planning**: Daily capacity tracking with leave management
- **Role-Based Access**: Backend, Frontend, QA, DevOps, Manager roles
- **Leave Integration**: Automatic capacity adjustment for time off

### 📈 **Analytics & Reporting**
- **Velocity Tracking**: Sprint-over-sprint velocity analysis
- **Burndown Charts**: Real-time progress visualization
- **Work Distribution**: Category-wise effort breakdown
- **Team Performance**: Individual and team productivity metrics
- **Historical Analysis**: Long-term trend identification

### 🏖️ **Holiday & Configuration Management**
- **Global Holiday Calendar**: Company and public holiday management
- **Recurring Holidays**: Automatic yearly holiday scheduling
- **Regional Support**: Multi-region holiday configuration
- **Custom Settings**: Personalized system preferences

## 🛠️ Technology Stack

### **Backend**
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.3.5** - Enterprise-grade application framework
- **Spring Data JPA** - Simplified data access layer
- **Spring AI** - Gemini integration for intelligent features
- **PostgreSQL** - Robust relational database
- **Maven** - Dependency management and build automation
- **Bucket4j** - Rate limiting for API calls

### **Frontend**
- **Thymeleaf** - Server-side templating engine
- **Tailwind CSS** - Utility-first CSS framework
- **Chart.js** - Interactive data visualization
- **Vanilla JS** - Modern JavaScript for interactivity

### **Architecture**
- **Layered Architecture**: Controller → Service → Repository → Entity
- **RESTful APIs**: Clean, resource-based API design
- **DTO Pattern**: Optimized data transfer objects
- **Repository Pattern**: Abstracted data access layer

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Thymeleaf  │  │ Tailwind CSS│  │  Chart.js   │         │
│  │  Templates  │  │   Styling   │  │  Analytics  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Controller Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Sprint    │  │    Task     │  │     AI      │         │
│  │ Controller  │  │ Controller  │  │ Controller  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Sprint    │  │    Task     │  │   Gemini    │         │
│  │   Service   │  │   Import    │  │ AI Service  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Repository Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Sprint    │  │    Task     │  │   Holiday   │         │
│  │ Repository  │  │ Repository  │  │ Repository  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                               │
│                 PostgreSQL Database                         │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### **Prerequisites**
- Java 21+ installed
- PostgreSQL 15+ running
- Maven 3.8+ installed
- Git for version control

### **1. Clone the Repository**
```bash
git clone https://github.com/your-org/sprintpilot.git
cd sprintpilot
```

### **2. Database Setup**
```sql
-- Create database and user
CREATE DATABASE sprintpilot;
CREATE USER inventory WITH PASSWORD 'inventory';
GRANT ALL PRIVILEGES ON DATABASE sprintpilot TO inventory;
```

### **3. Configure Application**

**A. Get Google Gemini API Key:**
1. Visit: https://aistudio.google.com/app/apikey
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy your API key

**B. Set Environment Variable:**
```bash
export GEMINI_API_KEY="your-api-key-here"
```

**C. Configuration in `application.properties`:**
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sprint_pilot
spring.datasource.username=inventory
spring.datasource.password=inventory

# JPA Configuration
spring.jpa.hibernate.ddl-auto=create-drop

# Google Gemini AI Integration (Spring AI)
spring.ai.google.genai.api-key=${GEMINI_API_KEY:}
spring.ai.google.genai.chat.options.model=gemini-2.5-flash
spring.ai.google.genai.chat.options.temperature=0.7

# AI Service Configuration
app.ai.enabled=true
app.ai.mock-mode=false  # Set to true for development without API key
app.ai.rate-limit.requests-per-minute=10
```

### **4. Run the Application**
```bash
# Development mode
mvn spring-boot:run

# Production build
mvn clean package
java -jar target/sprintpilot-1.0.0-SNAPSHOT.jar
```

### **5. Access the Application**
Open your browser and navigate to: `http://localhost:8080`

**Swagger UI**: Access interactive API documentation at `http://localhost:8080/swagger-ui.html`

---

## 🤖 Google Gemini AI Setup

SprintPilot uses **Spring AI with Google Gemini** for AI-powered features.

### **Quick Setup:**

1. **Get API Key**: Visit https://aistudio.google.com/app/apikey
2. **Set Environment Variable**:
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   ```
3. **Run Application**: `mvn spring-boot:run`

### **Development Without API Key:**

Set mock mode in `application-dev.properties`:
```properties
app.ai.mock-mode=true
```

Then run with dev profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### **Features Powered by Gemini:**
- ✅ Sprint summaries with workload analysis
- ✅ Risk identification and recommendations
- ✅ Meeting invite generation
- ✅ Confluence page creation
- ✅ Teams message generation
- ✅ Historical performance insights

**Rate Limiting**: Built-in rate limiting (10 requests/minute) to stay within free tier quotas.

---

## 📡 API Documentation

SprintPilot includes comprehensive **OpenAPI/Swagger** documentation for all REST APIs. Once the application is running, you can:

- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

The Swagger UI provides:
- Complete API endpoint documentation
- Request/response schemas and examples
- Interactive testing interface ("Try it out" feature)
- Organized by tags: Team Members, Tasks, Sprints, AI Services
- Real-time validation and error responses

For detailed Swagger setup and customization, see [SWAGGER_SETUP.md](SWAGGER_SETUP.md)

### **Sample API Endpoints**

### **Task Import APIs**

#### Import from CSV
```http
POST /api/tasks/import/csv
Content-Type: application/json

{
  "sprintId": "current-sprint",
  "source": "CSV",
  "tasks": [
    {
      "taskKey": "PROJ-001",
      "summary": "Implement user authentication",
      "storyPoints": 8,
      "category": "FEATURE",
      "priority": "HIGH",
      "assignee": "developer@company.com"
    }
  ]
}
```

#### Test Jira Connection
```http
POST /api/tasks/import/jira/test
Content-Type: application/json

{
  "url": "https://company.atlassian.net",
  "username": "user@company.com",
  "token": "your-api-token",
  "projectKey": "PROJ"
}
```

#### Fetch Jira Issues
```http
POST /api/tasks/import/jira/fetch
Content-Type: application/json

{
  "url": "https://company.atlassian.net",
  "username": "user@company.com",
  "token": "your-api-token",
  "projectKey": "PROJ",
  "jqlQuery": "project = PROJ AND status != Done"
}
```

### **AI-Powered APIs**

#### Generate Sprint Summary
```http
POST /api/ai/sprint-summary
Content-Type: application/json

{
  "sprintId": "current-sprint"
}
```

#### Risk Analysis
```http
POST /api/ai/risk-summary
Content-Type: application/json

{
  "sprintId": "current-sprint"
}
```

## 🎨 UI Features

### **Modern Glass-Morphism Design**
- Backdrop blur effects with transparency
- Smooth transitions and hover states
- Professional color scheme (#4f46e5 primary)
- Responsive design for all screen sizes

### **Interactive Components**
- **Drag & Drop**: File upload areas
- **Modal Dialogs**: Contextual forms and confirmations
- **Real-time Charts**: Interactive data visualization
- **Progressive Enhancement**: Works without JavaScript

### **Navigation**
- **Pills Navigation**: Modern tab-style navigation
- **Active States**: Clear visual feedback
- **Mobile Friendly**: Collapsible menu for mobile devices

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### **Development Setup**
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### **Code Style**
- Follow Java coding conventions
- Use meaningful variable names
- Write comprehensive JavaDoc
- Include unit tests for new features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Boot Team** for the excellent framework
- **Tailwind CSS** for the utility-first CSS approach
- **Chart.js** for beautiful data visualizations
- **PostgreSQL** for robust data storage
- **Thymeleaf** for powerful templating

---

<div align="center">

**Built with ❤️ by the SprintPilot Team**

[🐛 Report Bug](https://github.com/your-org/sprintpilot/issues) • [✨ Request Feature](https://github.com/your-org/sprintpilot/issues) • [📖 Documentation](https://docs.sprintpilot.com)

</div>