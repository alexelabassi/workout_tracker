These examples define the Java/Spring Boot coding style for this project.

The goal is to generate code that is:
- simple
- explicit
- consistent
- easy to debug
- easy to maintain
- easy to explain in a bachelor's thesis presentation

The code should follow a simple layered architecture:

```text
Controller -> Service -> Repository
```

DTOs are used for API input/output.
Entities are used for persistence.
Manual mappers are used for converting between entities and DTOs.

The examples use generic project/task/domain names only to demonstrate style.
Do not copy the example domain literally unless those classes exist in the current project.

---

# General Style

Prefer:
- clear class names
- clear method names
- small service methods
- constructor injection with `@RequiredArgsConstructor`
- request/response DTO records
- manual mapper classes
- private helper methods for entity loading
- simple custom exceptions
- global exception handling
- readable code over clever code

Avoid:
- business logic in controllers
- exposing entities directly in API responses
- unnecessary interfaces
- generic base services
- generic base controllers
- overengineering
- clever streams
- large methods
- adding libraries without approval
- changing unrelated files

---

# Package Style

Use packages similar to:

```java
controller
service
repository
model
dto.request
dto.response
mapper
exception
```

Example:

```java
com.example.app.project.controller
com.example.app.project.service
com.example.app.project.repository
com.example.app.project.model
com.example.app.project.dto.request
com.example.app.project.dto.response
com.example.app.project.mapper
com.example.app.exception
```

Keep related classes close together.

For shared exception handling, prefer one common package:

```java
com.example.app.exception
```

or:

```java
com.example.app.common.exception
```

---

# Controller Style

Controllers should be thin.

Controllers are responsible for:
- receiving HTTP requests
- triggering request DTO validation with `@Valid`
- calling service methods
- returning response DTOs

Controllers should not:
- load entities
- call repositories
- contain business rules
- construct entities
- manually map complex responses
- catch normal application exceptions manually
- manually build repeated error responses

Example:

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TaskResponse response = taskService.createTask(
                request,
                userPrincipal.getId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TaskResponse response = taskService.completeTask(
                taskId,
                userPrincipal.getId()
        );

        return ResponseEntity.ok(response);
    }
}
```

Controller method names should describe the action:

```java
createTask
completeTask
updateTask
deleteTask
getTaskDetails
```

Avoid vague names:

```java
handle
process
execute
manage
```

---

# Service Style

Services contain business logic.

Services are responsible for:
- loading entities
- checking ownership
- validating domain rules
- calling mappers
- saving entities
- returning response DTOs

Services should not:
- build HTTP responses
- catch normal application exceptions just to return HTTP errors
- contain long blocks of object construction if a mapper can do it
- repeat simple DTO validation such as `@NotNull`, `@Min`, or `@NotBlank`

Good service style:

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long userId) {
        Project project = getProjectForUser(request.projectId(), userId);

        Task task = taskMapper.toEntity(request, project, userId);

        Task savedTask = taskRepository.save(task);

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse completeTask(Long taskId, Long userId) {
        Task task = getTaskForUser(taskId, userId);

        task.complete();

        Task savedTask = taskRepository.save(task);

        return taskMapper.toResponse(savedTask);
    }

    private Project getProjectForUser(Long projectId, Long userId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private Task getTaskForUser(Long taskId, Long userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }
}
```

Preferred service flow:

```java
@Transactional
public ResponseDto method(RequestDto request, Long userId) {
    Entity entity = getEntity(...);

    validateSomething(entity);

    Entity savedEntity = repository.save(entity);

    return mapper.toResponse(savedEntity);
}
```

Avoid this style in services:

```java
Task task = new Task();
task.setUserId(userId);
task.setProject(project);
task.setTitle(request.title());
task.setDescription(request.description());
task.setStatus(TaskStatus.OPEN);
task.setCreatedAt(LocalDateTime.now());
```

If there are many setters, move object creation to a mapper.

---

# Mapper Style

Use simple manual mapper classes.

Mappers are responsible for:
- creating entities from already loaded data
- updating entities
- converting entities to response DTOs
- converting lists to response DTO lists

Mappers should not:
- call repositories
- perform ownership checks
- contain business rules
- decide permissions
- start transactions
- parse JWT tokens
- access the security context

Correct mapper usage:

```java
Project project = getProjectForUser(request.projectId(), userId);

Task task = taskMapper.toEntity(request, project, userId);
```

Incorrect mapper usage:

```java
Task task = taskMapper.toEntity(request);
```

This is bad if the request only contains IDs, because the mapper would need to load entities. Entity loading belongs in the service.

Example mapper:

```java
@Component
public class TaskMapper {

    public Task toEntity(CreateTaskRequest request, Project project, Long userId) {
        Task task = new Task();

        task.setUserId(userId);
        task.setProject(project);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.OPEN);
        task.setCreatedAt(LocalDateTime.now());

        return task;
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getCreatedAt(),
                task.getCompletedAt()
        );
    }

    public List<TaskResponse> toResponseList(List<Task> tasks) {
        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }
}
```

Mapper method names:

```java
toEntity
toResponse
toResponseList
updateEntity
```

Avoid names like:

```java
convert
map
process
buildData
```

unless the project already uses them consistently.

---

# DTO Style

Use request DTOs for input and response DTOs for output.

Prefer Java records for simple DTOs.

Request DTOs should carry IDs and simple values.
Services load the actual entities.

Example request:

```java
public record CreateTaskRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must have at most 100 characters")
        String title,

        @Size(max = 500, message = "Description must have at most 500 characters")
        String description
) {
}
```

Example response:

```java
public record TaskResponse(
        Long id,
        Long projectId,
        String projectName,
        String title,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
```

DTO rules:
- do not expose entities directly
- keep DTOs endpoint-specific
- keep DTOs small
- do not put business logic inside DTOs
- avoid returning unnecessary internal fields
- use validation annotations on request DTOs when simple input validation is needed

Good:

```java
public record AddCommentRequest(
        @NotNull(message = "Task ID is required")
        Long taskId,

        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must have at most 1000 characters")
        String content
) {
}
```

Bad:

```java
public record AddCommentRequest(
        Task task,
        User user,
        Comment comment
) {
}
```

---

## DTO Validation Rules

Use `jakarta.validation` annotations on request DTOs for simple input validation.

DTO validation should handle basic input rules, such as:
- required fields
- non-blank text
- string length limits
- positive numbers
- non-negative numbers
- valid email format
- basic date constraints

Services should not repeat simple DTO validation.

Services should still handle business rules that require database access or domain knowledge.

Examples of DTO validation:
- `title` must not be blank
- `duration` must be positive
- `progress` cannot be negative
- `projectId` must not be null

Examples of service validation:
- user owns the project
- task is not already completed
- comment belongs to the selected task
- resource is in a valid state for the requested operation

Controllers must use `@Valid` before `@RequestBody` when the request DTO has validation annotations.

Good:

```java
@PostMapping
public ResponseEntity<TaskResponse> createTask(
        @Valid @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
) {
    TaskResponse response = taskService.createTask(
            request,
            userPrincipal.getId()
    );

    return ResponseEntity.ok(response);
}
```

Bad:

```java
@PostMapping
public ResponseEntity<TaskResponse> createTask(
        @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
) {
    TaskResponse response = taskService.createTask(
            request,
            userPrincipal.getId()
    );

    return ResponseEntity.ok(response);
}
```

Reason:
Without `@Valid`, Spring will not automatically trigger validation for the request DTO.

Good numeric validation:

```java
public record LogEntryRequest(
        @NotNull(message = "Task ID is required")
        Long taskId,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be positive")
        Integer durationMinutes,

        @PositiveOrZero(message = "Progress cannot be negative")
        Integer progress
) {
}
```

Common imports:

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
```

If validation annotations do not run, make sure the project has the validation starter dependency.

Maven:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Final validation split:

```text
DTOs validate simple input shape and value ranges.
Services validate business rules.
Database constraints protect data integrity.
```

---

# Entity Style

Entities represent database tables.

Entities should contain:
- fields
- JPA annotations
- simple helper methods when useful

Entities should not contain:
- repository calls
- complex business workflows
- API-specific logic
- DTO mapping

---

## Entity Lombok And Lazy Loading Rules

Do not use `@Data` on JPA entities.

Use:
- `@Getter`
- `@Setter`
- `@NoArgsConstructor`

Avoid:
- `@ToString` on entities with relationships
- `@EqualsAndHashCode` on entities with relationships
- manually generated `toString()` methods that print full associated objects

Reason:
JPA relationships are often lazy-loaded. Generated `toString()`, `equals()`, and `hashCode()` methods can accidentally access lazy relationships, causing extra SQL queries, `LazyInitializationException`, infinite recursion, or poor performance.

Do not expose entities directly in controller responses.

Return DTOs instead.

Log entity IDs and simple fields instead of full entities.

Good:

```java
log.info("Project id={}, userId={}", project.getId(), project.getUserId());
```

Bad:

```java
log.info("Project={}", project);
```

If a response needs relationship data, fetch it deliberately inside the service/repository layer and map it to a DTO.

Good:

```java
@Transactional(readOnly = true)
public ProjectResponse getProject(Long projectId, Long userId) {
    Project project = projectRepository.findByIdAndUserId(projectId, userId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

    return projectMapper.toResponse(project);
}
```

For endpoints that need relationship data, prefer a specific repository method with `join fetch`, entity graph, or DTO projection instead of relying on accidental lazy loading.

---

## Entity Example

```java
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String title;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    public boolean isOpen() {
        return TaskStatus.OPEN.equals(status);
    }

    public void complete() {
        this.completedAt = LocalDateTime.now();
        this.status = TaskStatus.COMPLETED;
    }
}
```

Simple entity helper methods are allowed when they make the service cleaner.

Good helper methods:

```java
isOpen()
complete()
archive()
```

Avoid putting large business workflows inside entities.

---

# Repository Style

Repositories should be simple Spring Data JPA interfaces.

Example:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
```

Repository rules:
- use derived query methods when readable
- include `userId` in ownership-sensitive queries
- avoid custom queries unless needed
- do not put business logic in repositories

Good:

```java
Optional<Project> findByIdAndUserId(Long id, Long userId);
```

Bad:

```java
Optional<Project> findById(Long id);
```

when the entity belongs to a user and ownership matters.

If an endpoint needs relationship data, prefer a specific repository method instead of accidental lazy loading.

Example:

```java
@Query("""
       select t from Task t
       join fetch t.project
       where t.id = :taskId and t.userId = :userId
       """)
Optional<Task> findByIdAndUserIdWithProject(Long taskId, Long userId);
```

---

# Exception Style

Use simple custom runtime exceptions.

Example:

```java
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

```java
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
```

```java
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
```

Exception messages should be clear:

```java
throw new NotFoundException("Project not found");
throw new NotFoundException("Task not found");
throw new BadRequestException("Task is already completed");
throw new BadRequestException("Comment does not belong to selected task");
```

Avoid vague messages:

```java
throw new RuntimeException("Error");
throw new RuntimeException("Invalid");
throw new RuntimeException("Something went wrong");
```

Keep exception classes small.

Do not create a large exception hierarchy unless the project really needs it.

---

## Global Exception Handler Style

Use a global exception handler to keep API error responses consistent.

Services should throw clear custom exceptions.

Controllers should not manually catch normal application exceptions.

A global exception handler should convert exceptions into HTTP responses.

Preferred flow:

1. Controller receives request.
2. Controller calls service.
3. Service throws a custom exception if something is wrong.
4. `GlobalExceptionHandler` catches the exception.
5. API returns a consistent error response.

Recommended files:

```text
exception/
  BadRequestException.java
  ForbiddenException.java
  NotFoundException.java
  GlobalExceptionHandler.java
  ErrorResponse.java
  ValidationErrorResponse.java
```

Error response DTO:

```java
public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp
) {
}
```

Validation error response DTO:

```java
public record ValidationErrorResponse(
        String message,
        int status,
        Map<String, String> errors,
        LocalDateTime timestamp
) {
}
```

Global handler example:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse response = new ValidationErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        ErrorResponse response = new ErrorResponse(
                "Data integrity violation",
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        ErrorResponse response = new ErrorResponse(
                "Unexpected server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

Do not expose raw unexpected exception messages to the client.

Bad:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
    ErrorResponse response = new ErrorResponse(
            exception.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}
```

Reason:
Unexpected exception messages may expose internal implementation details.

Status code rules:
- `400 BAD_REQUEST` for invalid request data or invalid business state
- `401 UNAUTHORIZED` for missing or invalid authentication
- `403 FORBIDDEN` for authenticated users trying to access forbidden resources
- `404 NOT_FOUND` for missing resources
- `409 CONFLICT` for duplicate values or data conflicts
- `500 INTERNAL_SERVER_ERROR` for unexpected server errors

Security-specific `401` responses may be handled by Spring Security configuration instead of the global exception handler.

Final exception rule:

```text
Services throw exceptions.
The global exception handler converts exceptions into HTTP responses.
Controllers stay clean.
```

---

# Validation Style

Validate business rules in services.

Example:

```java
private void validateTaskIsOpen(Task task) {
    if (!task.isOpen()) {
        throw new BadRequestException("Task is not open");
    }
}
```

Use private helper methods when validation is reused or improves readability.

Validation methods should have clear names:

```java
validateTaskIsOpen
validateProjectBelongsToUser
validateCommentBelongsToTask
```

Avoid hiding important validation inside mappers.

Remember the validation split:

```text
DTO validation = simple input shape and value ranges.
Service validation = business rules.
Database constraints = data integrity.
```

---

# Domain Rule Style

Keep domain rules in a separate file when the project has meaningful business logic.

Recommended file:

```text
DOMAIN_RULES.md
```

`CODE_STYLE_EXAMPLES.md` should explain how code is written.
`DOMAIN_RULES.md` should explain what the application means.

Example of domain-rule wording:

```md
A Project belongs to a User.
A Task belongs to a Project.
A Comment belongs to a Task.
A completed Task cannot be edited unless it is reopened first.
```

Do not put project-specific domain rules directly in this file unless they are only examples.

---

# Example: Adding A Comment

Service:

```java
@Service
@RequiredArgsConstructor
public class CommentService {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse addComment(AddCommentRequest request, Long userId) {
        Task task = getOpenTaskForUser(request.taskId(), userId);

        Comment comment = commentMapper.toEntity(request, task, userId);

        Comment savedComment = commentRepository.save(comment);

        return commentMapper.toResponse(savedComment);
    }

    private Task getOpenTaskForUser(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.isOpen()) {
            throw new BadRequestException("Task is not open");
        }

        return task;
    }
}
```

Mapper:

```java
@Component
public class CommentMapper {

    public Comment toEntity(AddCommentRequest request, Task task, Long userId) {
        Comment comment = new Comment();

        comment.setTask(task);
        comment.setUserId(userId);
        comment.setContent(request.content());
        comment.setCreatedAt(LocalDateTime.now());

        return comment;
    }

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
```

DTOs:

```java
public record AddCommentRequest(
        @NotNull(message = "Task ID is required")
        Long taskId,

        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must have at most 1000 characters")
        String content
) {
}
```

```java
public record CommentResponse(
        Long id,
        Long taskId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}
```

---

# Test Style

Tests should be readable and focused.

Prefer:
- clear test names
- arrange/act/assert structure
- testing service behavior
- testing important domain rules

Example test style:

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskShouldSaveTask() {
        Long userId = 1L;
        CreateTaskRequest request = new CreateTaskRequest(
                10L,
                "Implement search",
                "Add simple search endpoint"
        );

        Project project = new Project();
        project.setId(10L);
        project.setName("Bachelor Thesis");

        Task task = new Task();
        task.setId(100L);

        TaskResponse response = new TaskResponse(
                100L,
                10L,
                "Bachelor Thesis",
                "Implement search",
                "Add simple search endpoint",
                "OPEN",
                LocalDateTime.now(),
                null
        );

        when(projectRepository.findByIdAndUserId(10L, userId))
                .thenReturn(Optional.of(project));

        when(taskMapper.toEntity(request, project, userId))
                .thenReturn(task);

        when(taskRepository.save(task))
                .thenReturn(task);

        when(taskMapper.toResponse(task))
                .thenReturn(response);

        TaskResponse result = taskService.createTask(request, userId);

        assertEquals(100L, result.id());
        assertEquals("OPEN", result.status());

        verify(taskRepository).save(task);
    }
}
```

Test names should describe behavior:

```java
createTaskShouldSaveTask
completeTaskShouldRejectTaskFromAnotherUser
addCommentShouldRejectClosedTask
```

Avoid vague names:

```java
test1
testService
shouldWork
```

---

# Formatting Style

Use standard Java formatting.

Prefer this:

```java
TaskResponse response = taskService.createTask(
        request,
        userPrincipal.getId()
);
```

over very long one-line calls.

Use blank lines between logical steps:

```java
Project project = getProjectForUser(request.projectId(), userId);

Task task = taskMapper.toEntity(request, project, userId);

Task savedTask = taskRepository.save(task);

return taskMapper.toResponse(savedTask);
```

This makes the flow easier to read.

---

# Final Rule

Generated code should be easy to explain.

If a piece of code would be hard to explain in a bachelor's thesis presentation, simplify it.

Prefer boring, predictable Java code over clever architecture.
