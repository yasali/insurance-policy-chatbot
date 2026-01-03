# Insurance Policy Manager

A Spring Boot application for managing insurance policies, with an AI-powered policy assistant and automated claim extraction.

It holds insurance contracts signed between an insurance provider and a policyholder (end-users), using a stripped-down version of home insurance for simplicity.

## Features

- **Insurance & Policy Management** — Create insurances, add policies with timeline support, query historical policies by date
- **AI Policy Assistant** — Chatbot using RAG (Retrieval-Augmented Generation) to answer questions about policies with conversation context
- **Claim Data Extraction** — Parse unstructured claim text using LLM to extract structured data with confidence scoring

## Getting Started

### Prerequisites

- Java 21
- Ollama with `llama3.2` model for local LLM inference

### 1. Install and Configure Ollama

```bash
chmod +x setup_ollama.sh
./setup_ollama.sh
```

Or manually:
```bash
brew install ollama
ollama pull llama3.2
ollama serve
```

### 2. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

**Database Console:** `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./db-file`
- Username: `sa`
- Password: (blank)

### 3. Run Tests

```bash
# Unit tests
mvn test

# Comprehensive integration tests
chmod +x run-comprehensive-tests.sh
./run-comprehensive-tests.sh
```

## API Endpoints

### Insurance Management
- `POST /api/v1/insurances` — Create new insurance
- `GET /api/v1/insurances/{id}` — Get insurance by ID
- `POST /api/v1/insurances/{id}/policies` — Add new policy
- `GET /api/v1/insurances/{id}/policies?date={YYYY-MM-DD}` — Get policy on specific date
- `GET /api/v1/policies?personalNumber={number}&date={YYYY-MM-DD}` — Get policies by personal number

### AI Assistant
- `POST /api/v1/assistant/chat` — Send message to assistant
- `GET /api/v1/assistant/conversations/{id}` — Get conversation history

### Claims
- `POST /api/v1/claims` — Submit claim for extraction

## Data Model

A `Policy` requires:
- `personalNumber` — personal identity number of the policyholder
- `address` — street address (e.g., "Kungsgatan 16")
- `postalCode` — postal code (e.g., "11135")
- `startDate` — the day this policy begins

An `Insurance` holds multiple `Policy` entries, forming a timeline. When a policyholder updates their information with a new `startDate`, a new policy is created. The `personalNumber` remains constant within an insurance.

## Tech Stack

- Kotlin + Spring Boot 3
- H2 Database with Liquibase migrations
- Spring Data JPA
- Ollama (llama3.2) for LLM inference
