# QUAD Services (Java)

QUAD Framework - Centralized Business Logic Services

## Overview

This is the Java Spring Boot backend for QUAD Framework. All business logic lives here, separate from the Next.js UI layer.

```
┌─────────────────────────────────────────────────────────────┐
│                      QUAD Platform                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Web App    │  │ VS Code     │  │  Mobile Apps        │ │
│  │  (Next.js)  │  │ Plugin      │  │  (iOS/Android)      │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
│         │                │ REST               │ REST       │
│         ▼                ▼                    ▼            │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              quad-services (Spring Boot)           ││
│  │  ┌───────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐  ││
│  │  │  AI   │ │  Memory  │ │ Assignment   │ │Integrations││
│  │  └───────┘ └──────────┘ └──────────────┘ └──────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PostgreSQL (JPA/Hibernate)                 ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 15+

## Quick Start

```bash
# Build
mvn clean package

# Run DEV
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run QA
mvn spring-boot:run -Dspring-boot.run.profiles=qa

# Run with Docker
docker build -t quad-services .
docker run -p 14101:14101 quad-services
```

## Project Structure

```
quad-services/
├── src/main/java/com/quad/services/
│   ├── QuadServicesApplication.java   # Main entry point
│   ├── ai/                            # AI providers & routing
│   │   ├── AIService.java             # Main AI orchestrator
│   │   ├── AITier.java                # Tier configuration
│   │   ├── TaskType.java              # Task classification
│   │   ├── AIMessage.java             # Message DTO
│   │   ├── AIResponse.java            # Response DTO
│   │   └── providers/
│   │       ├── AIProvider.java        # Provider interface
│   │       ├── ClaudeProvider.java    # Anthropic Claude
│   │       ├── OpenAIProvider.java    # OpenAI GPT
│   │       └── GeminiProvider.java    # Google Gemini
│   ├── memory/                        # Context retrieval
│   │   ├── MemoryService.java         # Main service
│   │   ├── ContextScope.java          # Scope definition
│   │   ├── ContextChunk.java          # Context chunk
│   │   ├── ContextResult.java         # Result with chunks
│   │   └── SessionType.java           # Session types
│   ├── assignment/                    # Ticket routing
│   │   ├── AssignmentService.java     # Main service
│   │   └── AssignmentResult.java      # Result DTO
│   ├── integrations/
│   │   ├── github/
│   │   │   └── GitHubService.java     # GitHub OAuth + API
│   │   └── calendar/
│   │       └── GoogleCalendarService.java  # Google Calendar
│   ├── core/                          # Agent runtime (TODO)
│   └── config/                        # Configuration (TODO)
├── src/main/resources/
│   └── application.yml                # Spring Boot config
├── pom.xml                            # Maven dependencies
└── README.md                          # This file
```

## Services Reference

### 1. AI Service

Routes AI requests to the appropriate provider based on tier and task type.

```java
@Autowired AIService aiService;

// Simple call with automatic routing
AIResponse response = aiService.call(orgId, messages, TaskType.CODE_REVIEW);

// Streaming
Flux<String> stream = aiService.stream(orgId, messages, TaskType.COMPLEX_CODING);

// Direct provider call (for BYOK)
AIResponse response = aiService.callDirect("claude", messages, "claude-3-opus", 4000);
```

**AI Tiers:**

| Tier | Cost/Month | Best For |
|------|------------|----------|
| `turbo` | ~$5 | Fast tasks (classification) |
| `balanced` | ~$15 | General development |
| `quality` | ~$30 | Complex coding |
| `byok` | $0 | User's own API key |

### 2. Memory Service

Hierarchical context retrieval for AI conversations.

```java
@Autowired MemoryService memoryService;

ContextResult context = memoryService.getInitialContext(
    ContextScope.builder()
        .orgId(orgId)
        .domainId(domainId)
        .userId(userId)
        .sessionType(SessionType.TICKET_ANALYSIS)
        .build(),
    List.of("bug", "login", "authentication"),
    4000  // maxTokens
);

String markdown = context.toMarkdown();  // Use in AI prompt
```

### 3. Assignment Service

Intelligent ticket routing based on skills.

```java
@Autowired AssignmentService assignmentService;

// Auto-assign ticket
AssignmentResult result = assignmentService.assignTicket(ticketId, domainId, orgId);

// Get suggestions
List<AssignmentResult> suggestions = assignmentService.getSuggestions(ticketId, domainId, orgId, 3);
```

### 4. GitHub Service

GitHub OAuth and repository operations.

```java
@Autowired GitHubService gitHubService;

// OAuth flow
String authUrl = gitHubService.getAuthUrl(orgId, userId);
gitHubService.handleCallback(code, state);

// Repository operations
List<Repository> repos = gitHubService.listRepositories(orgId);
PullRequest pr = gitHubService.createPullRequest(orgId, "owner/repo", prRequest);
```

### 5. Google Calendar Service

Calendar integration with Google Meet support.

```java
@Autowired GoogleCalendarService calendarService;

// OAuth flow
String authUrl = calendarService.getAuthUrl(orgId, userId, redirectUri);

// Create meeting with Meet link
Meeting meeting = calendarService.createMeeting(orgId, CreateMeetingRequest.builder()
    .title("Sprint Planning")
    .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
    .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
    .createMeetLink(true)
    .build());
```

## Configuration

### Environment Variables

```bash
# AI Providers
ANTHROPIC_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
GOOGLE_GEMINI_API_KEY=...
GROQ_API_KEY=gsk_...

# GitHub
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...

# Google
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# Database (override defaults)
DB_HOST=localhost
DB_NAME=quad_dev_db
DB_USER=quad_user
DB_PASSWORD=quad_dev_pass
```

### Profiles

| Profile | Port | DB Port | Database | Use Case |
|---------|------|---------|----------|----------|
| `dev` | 14101 | 14201 | quad_dev_db | Local development |
| `qa` | 15101 | 15201 | quad_qa_db | QA testing |
| `prod` | 8080 | GCP | (env vars) | Production (GCP Cloud) |

## API Endpoints (TODO)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ai/chat` | POST | AI chat with routing |
| `/api/ai/stream` | POST | Streaming AI response |
| `/api/memory/context` | POST | Get context for session |
| `/api/tickets/assign` | POST | Auto-assign ticket |
| `/api/integrations/github/auth` | GET | Start GitHub OAuth |
| `/api/integrations/calendar/auth` | GET | Start Google OAuth |

## Migration from TypeScript

This Java version replaces the TypeScript `quad-services` package. The TypeScript version is kept as reference in `/quad-services` folder.

| TypeScript | Java |
|------------|------|
| `interface` | `interface` or `record` |
| `type` alias | `class` or `enum` |
| `async/await` | `CompletableFuture` or WebFlux |
| `Promise<T>` | `CompletableFuture<T>` or `Mono<T>` |
| `export function` | `@Service` method |
| Prisma ORM | JPA/Hibernate |

## License

MIT - A2Vibe Creators
