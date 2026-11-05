# ATS Optimizer 🚀

Sistema inteligente para processamento, análise e otimização de currículos para aprovação em filtros **ATS (Applicant Tracking Systems)**. Projetado para execução 100% local com Docker, garantindo alta coesão e divisão de responsabilidades com o padrão **Package by Feature**.

---

## 🛠️ Tecnologias e Dependências

- **Linguagem & Runtime:** Java 21
- **Framework:** Spring Boot 3.4.x
- **Extração de Texto:** Apache Tika 3.1.0 (PDF, DOCX, DOC, RTF, TXT)
- **Mensageria:** RabbitMQ 3.13 (com `TopicExchange` e `RabbitTemplate`)
- **Inteligência Artificial:** Spring AI 1.0.0 (`AiClient` genérico com estruturação de retorno tipada)
- **Banco de Dados:** PostgreSQL 16 (Hibernate 6 com suporte nativo a JSONB)
- **Documentação da API:** SpringDoc OpenAPI 2.8.5 (Swagger UI)
- **Containerização:** Docker & Docker Compose v2

---

## 🏗️ Arquitetura do Fluxo

```
[Cliente Desktop / HTTP / Swagger]
            │
            ▼ (MultipartFile)
   ┌──────────────────────────────────────────────────┐
   │ ResumeController (/api/v1/resumes/upload)        │
   └────────┬─────────────────────────────────────────┘
            │
            ▼
   ┌──────────────────────────────────────────────────┐
   │ TikaDocumentTextExtractor (Apache Tika)          │ -> Extração síncrona de texto
   └────────┬─────────────────────────────────────────┘
            │
            ▼
   ┌──────────────────────────────────────────────────┐
   │ ResumeRepository (PostgreSQL)                    │ -> Salva Resume (Status: UPLOADED)
   └────────┬─────────────────────────────────────────┘
            │
            ▼
   ┌──────────────────────────────────────────────────┐
   │ ResumeEventPublisher (RabbitTemplate)            │ -> Publica no Topic Exchange "ats.exchange"
   └────────┬─────────────────────────────────────────┘
            │
            ▼ (Routing Key: "resume.analyze")
   ┌──────────────────────────────────────────────────┐
   │ ResumeAnalysisListener (@RabbitListener)         │
   └────────┬─────────────────────────────────────────┘
            │
            ▼
   ┌──────────────────────────────────────────────────┐
   │ ResumeAnalysisService -> AiClient (sendPrompt)   │ -> Spring AI (OpenAI API / Local LLM)
   └────────┬─────────────────────────────────────────┘
            │
            ▼ (Structured Output -> ResumeAnalysisResult)
   ┌──────────────────────────────────────────────────┐
   │ ResumeRepository                                 │ -> Atualiza Resume (Status: COMPLETED + Análise)
   └──────────────────────────────────────────────────┘
```

---

## 📦 Estrutura do Código (Package by Feature)

A aplicação não utiliza God Classes; cada camada e componente possui responsabilidade única:

- `com.atsoptimizer.parser`:
  - `DocumentTextExtractor` e `TikaDocumentTextExtractor`: isolam a extração e higienização do texto via Apache Tika.
- `com.atsoptimizer.ai`:
  - `AiClient`: componente com método genérico `<T> T sendPrompt(String system, String user, Class<T> entity)`.
  - `ResumeAnalysisPromptBuilder`: constrói prompts especializados em heurísticas de ATS (Taleo, Greenhouse, Workday).
  - `dto.ResumeAnalysisResult`: record tipado com keywords, perfil, inconsistências, nota (0-100%) e sugestões.
- `com.atsoptimizer.queue`:
  - `RabbitMQConfig`: define `TopicExchange`, `Queue`, `Binding` e `RabbitTemplate` com conversor JSON Jackson.
  - `ResumeEventPublisher`: publica mensagens tipadas na fila.
  - `ResumeAnalysisListener`: consome mensagens e dispara o serviço de análise.
- `com.atsoptimizer.resume`:
  - `Resume`: entidade JPA com status (`UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED`) e dados da análise.
  - `ResumeService`: gerencia o upload, extração síncrona e publicação inicial.
  - `ResumeAnalysisService`: gerencia o processamento da IA e atualização da entidade.
  - `ResumeController`: endpoints REST para upload e consulta.
- `com.atsoptimizer.config`:
  - `OpenApiConfig`: configuração OpenAPI / Swagger.
  - `GlobalExceptionHandler`: tratamento de exceções de extração, validação e limites de arquivo.

---

## 🚀 Como Executar

### 1. Pré-requisitos
- Docker & Docker Compose instalados.
- Chave de API OpenAI (ou endpoint local compatível como LM Studio / Ollama).

### 2. Subindo com Docker Compose (Banco + Fila + Backend)

Crie um arquivo `.env` ou exporte as variáveis no seu terminal:
```bash
export OPENAI_API_KEY="sua-chave-openai-ou-mock"
export OPENAI_BASE_URL="https://api.openai.com" # ou http://localhost:11434/v1 para Ollama local
```

Inicie os containers:
```bash
docker compose up -d --build
```

### 3. Executando Apenas a Infraestrutura (Postgres + RabbitMQ) e o Backend Localmente

Caso prefira rodar o backend diretamente na sua IDE ou via terminal:
```bash
# Inicie apenas o PostgreSQL e o RabbitMQ
docker compose up -d postgres rabbitmq

# Entre na pasta backend e execute
cd backend
./mvnw spring-boot:run
```

---

## 📖 Documentação Interativa (Swagger)

Acesse a interface Swagger UI no navegador:
- **URL Swagger:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **RabbitMQ Management UI:** `http://localhost:15672` (Usuário: `guest`, Senha: `guest`)

---

## 🧪 Endpoints Principais

### 1. Upload de Currículo
```http
POST /api/v1/resumes/upload
Content-Type: multipart/form-data
```
**Parâmetro:** `file` (PDF, DOCX, TXT)

**Resposta (201 Created):**
```json
{
  "id": 1,
  "fileName": "curriculo_lucas.pdf",
  "contentType": "application/pdf",
  "fileSize": 124500,
  "status": "UPLOADED",
  "createdAt": "2026-11-03T21:30:00",
  "updatedAt": "2026-11-03T21:30:00"
}
```

### 2. Consulta de Status e Diagnóstico ATS
```http
GET /api/v1/resumes/{id}
```

**Resposta (200 OK):**
```json
{
  "id": 1,
  "fileName": "curriculo_lucas.pdf",
  "status": "COMPLETED",
  "analysis": {
    "resumeId": 1,
    "score": 88,
    "inferredObjective": "Engenheiro de Software Backend Sênior",
    "overallProfile": "Profissional com sólida bagagem em microsserviços e Java...",
    "structureEvaluation": "Formatação limpa e de fácil leitura para robôs ATS...",
    "keywords": ["Java", "Spring Boot", "PostgreSQL", "Docker", "RabbitMQ"],
    "missingKeywords": ["Kubernetes", "AWS ECS", "Kafka"],
    "inconsistencies": [],
    "strengths": ["Experiência prática relevante", "Uso de tecnologias modernas"],
    "weaknesses": ["Ausência de métricas quantificáveis de impacto nos projetos"],
    "improvementSuggestions": [
      "Adicionar métricas (ex: redução de latência em X%, aumento de throughput)",
      "Incluir certificações relevantes"
    ],
    "atsCompatibilitySummary": "O currículo possui excelente aderência e probabilidade de aprovação superior a 85% em triagens automáticas.",
    "analyzedAt": "2026-11-03T21:30:05"
  }
}
```
