# AI-Powered Ticket Management System (Backend)

## Project Overview
A Spring Boot REST API for creating and managing support tickets, featuring an integrated AI service for automated ticket classification and summarization.

## Tech Stack Used
* **Language:** Java 21
* **Framework:** Spring Boot 4.0.5
* **Database:** PostgreSQL (Spring Data JPA)
* **Documentation:** springdoc-openapi (Swagger)
* **AI Service:** Google Gemini 2.5 Flash

## Setup Instructions
1. Ensure Java 21 and PostgreSQL are installed on your machine.
2. Configure your environment variables (or `application.yaml`):
   ```env
   DB_URL=jdbc:postgresql://localhost:5432/ticket_manager
   DB_USERNAME=postgres
   DB_PASSWORD=your_password
   GEMINI_API_KEY=your_gemini_key
   PORT=8080

3. Run the application using Maven:
   ```bash
   .\mvnw spring-boot:run
   ```

## API Documentation
Access the interactive Swagger UI at:
`http://localhost:8080/swagger-ui.html`

### Key Endpoints
*   `POST /api/tickets` - Create a new ticket
*   `GET /api/tickets` - Retrieve all tickets (supports filtering by status, priority, category)
*   `GET /api/tickets/{id}` - Get a ticket by its unique ID
*   `PUT /api/tickets/{id}` - Update an existing ticket
*   `PATCH /api/tickets/{id}/status` - Update a ticket's status
*   `DELETE /api/tickets/{id}` - Delete a ticket
*   `POST /api/ai/suggest` - Generate AI suggestions for ticket classification and summary

### AI Feature Explanation
The application uses Google's Gemini LLM to polish ticket titles and descriptions and also set priority and category accordingly. It returns a suggested `category`, `priority`, and a brief `summary`. If the API key is missing or the external API fails, the service falls back to a custom keyword-based scanning algorithm to guarantee uninterrupted service.

## Deployment Links
*   **Live Backend:** https://ticketmanager-backend-production.up.railway.app (Deployed on Railway)
*   **Database:** Serverless PostgreSQL hosted on Neon (https://neon.tech)