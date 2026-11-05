# AGENTS.md

# Code the Way I Like to Code

These instructions define my coding preferences, architectural principles, and expectations for code quality. Follow them throughout the project.

## 1. Backend Development Principles

### Encapsulation and Domain Modeling

- Avoid using setters as the default approach for modifying object state.
- Prefer meaningful, intention-revealing methods that describe what the object is supposed to do.
- Keep business rules and state transitions inside the domain objects whenever appropriate.
- Respect encapsulation and maintain domain invariants.
- Follow Domain-Driven Design principles when they provide value to the domain. Do not introduce unnecessary DDD complexity.
- Keep DTOs, entities, domain models, and persistence concerns properly separated according to the project's architecture.

Example:

```java
// Avoid
user.setEmail(newEmail);

// Prefer
user.updateEmail(newEmail);
```

### Clean Code and Architecture

- Prioritize readability, maintainability, and simplicity.
- Avoid unnecessary abstractions, premature optimization, and overengineering.
- Follow the existing project architecture and conventions before introducing new patterns.
- Keep responsibilities clearly separated.
- Controllers should handle HTTP concerns, while services should orchestrate application logic and business operations.
- Do not place business logic inside controllers.
- Avoid creating unnecessary layers, interfaces, or classes without a clear reason.
- Prefer composition and clear dependencies over tightly coupled implementations.

## 2. Testing Standards

Tests are an essential part of development, not an optional final step.

Whenever you create a feature or modify existing functionality:

### Unit Tests

- Create meaningful unit tests covering the expected behavior.
- Test both valid and invalid scenarios.
- Include edge cases and boundary conditions when relevant.
- Focus especially on:
  - **Controllers:** HTTP contracts, request validation, response bodies, status codes, and error handling.
  - **Services:** business rules, decision-making, valid scenarios, invalid scenarios, and exceptional conditions.
- Mock external dependencies when appropriate, but avoid mocking the entire application.
- Never write tests simply to make the test suite pass. Tests must validate actual expected behavior.
- Avoid testing implementation details that do not represent observable behavior.

### Integration Tests

- Create integration tests for important application flows.
- Verify that relevant components work correctly together.
- Test persistence, database interactions, and external integrations when relevant.
- Use realistic test scenarios and appropriate test infrastructure.

### Test Execution

- Always run the relevant tests before considering a task complete.
- Run the full test suite when practical, especially after significant changes.
- If tests fail, investigate and fix the underlying problem instead of modifying or removing tests just to obtain a passing result.
- Never claim that tests passed without actually executing them.
- Report any tests that could not be executed and explain why.

## 3. Agent Workflow and Delegation

### Task Planning

- Break complex tasks into smaller, manageable steps.
- Before implementation, understand the existing codebase, architecture, and conventions.
- For complex tasks, use specialized subagents when available and beneficial.
- Delegate well-defined responsibilities to subagents, such as:
  - Codebase exploration and architecture analysis.
  - Test implementation and coverage analysis.
  - Code review and bug detection.
  - Documentation and technical research.

### Review and Responsibility

- Always review subagent output before integrating it.
- Verify that delegated implementations satisfy the original requirements.
- Check architectural consistency, code quality, and test coverage.
- Resolve conflicts between implementations before completing the task.
- The main agent remains responsible for the final result, regardless of delegation.

### Plan mode and questions

- First indentify if the prompt recived is about a question or a new task, if is a question dont change or write anything in codebase, just answer the question.
- Always create a plan and wait the user allowed to processed, never write or make changes without a plan.
- In plan must have to contain the mainly decisions that you had to make and why you choose that one, show the trade offs of decision, also in plan i wanna know what exactly you going to create/modify and how.

### Git

- Prefer do commits to simple and reviewable things avoid make a commit with 1000 lines changed, this help when someone or another agent wanna see the codebase and how its change
- If had an branch named Dev or Development(or something that looks like the aplication is in prod and the main/master branch cant be modify easily) write the code in dev branch

## 4. Skills and Documentation

- Whenever a new task is received, check whether relevant skills are available.
- Use specialized skills when they are relevant to the task.
- Before using unfamiliar frameworks, libraries, or APIs, consult their official documentation or available project references.
- Prefer current, authoritative documentation over assumptions or outdated knowledge.
- Do not use a skill or introduce a tool merely because it is available. There must be a clear benefit.

## 5. Task Completion Checklist

Before considering a task complete:

- [ ] Requirements have been understood and implemented.
- [ ] Existing architecture and conventions have been respected.
- [ ] No unnecessary complexity or abstractions have been introduced.
- [ ] Relevant valid and invalid scenarios have been tested.
- [ ] Relevant integration tests have been implemented or updated.
- [ ] Tests have been executed and their results verified.
- [ ] Subagent contributions have been reviewed, if applicable.
- [ ] No unrelated functionality has been unintentionally broken.
- [ ] Remaining limitations or known issues have been reported.