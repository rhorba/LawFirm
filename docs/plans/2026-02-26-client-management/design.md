# Client Management — Design Document
**Date:** 2026-02-26
**Status:** Approved, ready for implementation

---

## Decisions Made

| Question | Decision |
|---|---|
| Client types | All three: INDIVIDUAL, CORPORATE, GOVERNMENT in one table |
| Case relationship | One client per case (`cases.client_id FK`) |
| Conflict checking | Simple `conflict_checked` boolean flag on case (manual) |
| Frontend pattern | Inline modal, same pattern as Lawyer management |

---

## 1. Database Schema (Flyway)

### V41 — Create clients table
```sql
CREATE TABLE clients (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    client_type      VARCHAR(20)  NOT NULL,           -- INDIVIDUAL | CORPORATE | GOVERNMENT

    -- Common fields
    first_name       VARCHAR(100),
    last_name        VARCHAR(100),
    phone            VARCHAR(20),
    email            VARCHAR(100) UNIQUE,
    address          TEXT,
    notes            TEXT,
    active           BOOLEAN NOT NULL DEFAULT TRUE,

    -- INDIVIDUAL only
    cin              VARCHAR(20) UNIQUE,              -- national ID, unique
    gender           VARCHAR(10),                     -- MALE | FEMALE
    date_of_birth    DATE,                            -- age computed at runtime

    -- CORPORATE / GOVERNMENT only
    company_name     VARCHAR(200),
    tax_number       VARCHAR(50) UNIQUE
);

CREATE INDEX idx_clients_type     ON clients(client_type);
CREATE INDEX idx_clients_active   ON clients(active);
CREATE INDEX idx_clients_cin      ON clients(cin);
CREATE INDEX idx_clients_name     ON clients(last_name, first_name);
```

### V42 — Seed client permissions
```sql
INSERT INTO permissions (name) VALUES
  ('CLIENT_READ'), ('CLIENT_CREATE'),
  ('CLIENT_UPDATE'), ('CLIENT_DELETE'), ('CLIENT_MANAGE');

-- Assign all to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CLIENT_READ','CLIENT_CREATE','CLIENT_UPDATE','CLIENT_DELETE','CLIENT_MANAGE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
```

### V43 — Add client_id FK to cases
```sql
ALTER TABLE cases ADD COLUMN client_id BIGINT;
ALTER TABLE cases ADD CONSTRAINT fk_cases_client
    FOREIGN KEY (client_id) REFERENCES clients(id);
CREATE INDEX idx_cases_client ON cases(client_id);
```

---

## 2. Domain Model

### Enums
```java
// ClientType.java
public enum ClientType { INDIVIDUAL, CORPORATE, GOVERNMENT }

// Gender.java
public enum Gender { MALE, FEMALE }
```

### Client.java
```java
@Entity @Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Client extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType clientType;

    // Common
    @Column(length = 100) private String firstName;
    @Column(length = 100) private String lastName;
    @Column(length = 20)  private String phone;
    @Column(length = 100, unique = true) private String email;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) @Builder.Default private Boolean active = true;

    // INDIVIDUAL only
    @Column(length = 20, unique = true) private String cin;
    @Enumerated(EnumType.STRING) @Column(length = 10) private Gender gender;
    @Column private LocalDate dateOfBirth;

    // CORPORATE / GOVERNMENT only
    @Column(length = 200) private String companyName;
    @Column(length = 50, unique = true) private String taxNumber;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<Case> cases = new ArrayList<>();

    public String getFullName() {
        return clientType == ClientType.INDIVIDUAL
            ? firstName + " " + lastName
            : companyName;
    }

    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
```

### Case.java addition
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "client_id")
private Client client;
```

### ClientRepository.java
```java
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByCin(String cin);
    Optional<Client> findByTaxNumber(String taxNumber);
    Optional<Client> findByEmail(String email);
    boolean existsByCin(String cin);
    boolean existsByTaxNumber(String taxNumber);
    boolean existsByEmail(String email);

    @Query("SELECT c FROM Client c WHERE c.active = true AND " +
           "(:search IS NULL OR " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.lastName)  LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.cin)       LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.email)     LIKE LOWER(CONCAT('%',:search,'%'))) AND " +
           "(:type IS NULL OR c.clientType = :type)")
    Page<Client> search(@Param("search") String search,
                        @Param("type") ClientType type,
                        Pageable pageable);
}
```

---

## 3. Application Layer

### DTOs

```java
// CreateClientRequest.java
public record CreateClientRequest(
    @NotNull ClientType clientType,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$") String phone,
    @Email @Size(max = 100) String email,
    String address,
    String notes,
    @Size(max = 20) String cin,
    Gender gender,
    LocalDate dateOfBirth,
    @Size(max = 200) String companyName,
    @Size(max = 50) String taxNumber
) {}

// UpdateClientRequest.java
public record UpdateClientRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$") String phone,
    @Email @Size(max = 100) String email,
    String address,
    String notes,
    @Size(max = 20) String cin,
    Gender gender,
    LocalDate dateOfBirth,
    @Size(max = 200) String companyName,
    @Size(max = 50) String taxNumber
) {}

// ClientResponse.java
public record ClientResponse(
    Long id, String fullName, ClientType clientType,
    String firstName, String lastName,
    String phone, String email, String address, String notes,
    String cin, Gender gender, LocalDate dateOfBirth, Integer age,
    String companyName, String taxNumber,
    Boolean active, int caseCount,
    LocalDateTime createdAt
) {}

// ClientSummary.java  (list view)
public record ClientSummary(
    Long id, String fullName, ClientType clientType,
    String cin, String phone, String email,
    Boolean active, int caseCount
) {}
```

### ClientMapper.java
```java
@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "age",       expression = "java(client.getAge())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientResponse toResponse(Client client);

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientSummary toSummary(Client client);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "active",    constant = "true")
    @Mapping(target = "cases",     ignore = true)
    Client toEntity(CreateClientRequest request);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "version",    ignore = true)
    @Mapping(target = "active",     ignore = true)
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "cases",      ignore = true)
    void updateEntity(UpdateClientRequest request, @MappingTarget Client client);
}
```

### ClientService.java (key logic)
```java
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ClientService {

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        validateByType(request);
        validateAge(request);
        checkUniqueness(request.cin(), request.taxNumber(), request.email(), null);
        return clientMapper.toResponse(clientRepository.save(clientMapper.toEntity(request)));
    }

    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        checkUniquenessOnUpdate(request, client);
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    public Page<ClientSummary> search(String search, ClientType type, int page, int size) {
        return clientRepository.search(search, type, PageRequest.of(page, size))
            .map(clientMapper::toSummary);
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        client.setActive(false);
        clientRepository.save(client);
    }

    // Business rule: INDIVIDUAL needs firstName+lastName; CORPORATE/GOVERNMENT needs companyName
    private void validateByType(CreateClientRequest r) {
        if (r.clientType() == INDIVIDUAL
            && (r.firstName() == null || r.lastName() == null))
            throw new BusinessRuleException("Individual clients require first and last name");
        if ((r.clientType() == CORPORATE || r.clientType() == GOVERNMENT)
            && (r.companyName() == null || r.companyName().isBlank()))
            throw new BusinessRuleException("Corporate/Government clients require a company name");
    }

    // Business rule: age 18–100 for individuals
    private void validateAge(CreateClientRequest r) {
        if (r.clientType() == INDIVIDUAL && r.dateOfBirth() != null) {
            int age = Period.between(r.dateOfBirth(), LocalDate.now()).getYears();
            if (age < 18 || age > 100)
                throw new BusinessRuleException("Client must be between 18 and 100 years old");
        }
    }
}
```

---

## 4. Presentation Layer

### ClientController.java
```java
@RestController @RequestMapping("/api/clients") @RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management")
public class ClientController {

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    public ResponseEntity<Page<ClientSummary>> search(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClientType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) { ... }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) { ... }

    @GetMapping("/{id}/cases")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    public ResponseEntity<List<CaseSummary>> getClientCases(@PathVariable Long id) { ... }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_CREATE')")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest req) { ... }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_UPDATE')")
    public ResponseEntity<ClientResponse> update(
        @PathVariable Long id, @Valid @RequestBody UpdateClientRequest req) { ... }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_DELETE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) { ... }

    @GetMapping("/export")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    public void export(@RequestParam(required = false) String search,
                       @RequestParam(required = false) ClientType type,
                       HttpServletResponse response) throws IOException { ... }
}
```

---

## 5. Frontend (Angular 18)

### File Structure
```
frontend/src/app/
├── core/models/
│   └── client.model.ts
├── services/
│   └── client.service.ts
└── features/clients/
    ├── client-list/
    │   ├── client-list.component.ts
    │   └── client-list.component.html
    └── client-form/
        ├── client-form.component.ts
        └── client-form.component.html
```

### client.model.ts
```typescript
export type ClientType = 'INDIVIDUAL' | 'CORPORATE' | 'GOVERNMENT';
export type Gender = 'MALE' | 'FEMALE';

export interface ClientSummary {
  id: number; fullName: string; clientType: ClientType;
  cin?: string; phone?: string; email?: string;
  active: boolean; caseCount: number;
}

export interface ClientResponse {
  id: number; fullName: string; clientType: ClientType;
  firstName?: string; lastName?: string;
  phone?: string; email?: string; address?: string; notes?: string;
  cin?: string; gender?: Gender; dateOfBirth?: string; age?: number;
  companyName?: string; taxNumber?: string;
  active: boolean; caseCount: number; createdAt: string;
}

export interface CreateClientRequest {
  clientType: ClientType;
  firstName?: string; lastName?: string;
  phone?: string; email?: string; address?: string; notes?: string;
  cin?: string; gender?: Gender; dateOfBirth?: string;
  companyName?: string; taxNumber?: string;
}

export interface ClientSearchParams {
  search?: string;
  type?: ClientType;
  page?: number;
  size?: number;
}
```

### Key component signals (client-list)
```typescript
clients    = signal<PageResponse<ClientSummary> | null>(null);
loading    = signal(false);
error      = signal<string | null>(null);
search     = signal('');
typeFilter = signal<ClientType | ''>('');
page       = signal(0);
size       = signal(20);
showModal  = signal(false);
editingClient = signal<ClientResponse | null>(null);
```

### Dynamic form (client-form) — fields shown by clientType
| Field | INDIVIDUAL | CORPORATE | GOVERNMENT |
|---|---|---|---|
| firstName / lastName | required | optional (contact) | optional (contact) |
| CIN | required | — | — |
| gender | shown | — | — |
| dateOfBirth | shown | — | — |
| companyName | — | required | required |
| taxNumber | — | required | optional |
| phone / email / address | all types | all types | all types |

### Route
```typescript
{ path: 'clients', loadComponent: () =>
    import('./features/clients/client-list/client-list.component')
      .then(m => m.ClientListComponent) }
```

---

## 6. Business Rules Summary

| Rule | Where enforced |
|---|---|
| INDIVIDUAL requires firstName + lastName | Service + frontend form validation |
| CORPORATE/GOVERNMENT requires companyName | Service + frontend form validation |
| Age 18–100 for individuals | Service (computed from dateOfBirth) |
| CIN unique (individuals) | DB UNIQUE constraint + service check |
| taxNumber unique (corporate/gov) | DB UNIQUE constraint + service check |
| email unique | DB UNIQUE constraint + service check |
| Conflict check | `conflict_checked` boolean on Case (manual flag) |
| Soft delete only | `active = false`, never hard delete |

---

## 7. API Endpoints Summary

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | /api/clients | CLIENT_READ | Search/list with filters |
| GET | /api/clients/:id | CLIENT_READ | Get full client detail |
| GET | /api/clients/:id/cases | CLIENT_READ | All cases for client |
| POST | /api/clients | CLIENT_CREATE | Create client |
| PUT | /api/clients/:id | CLIENT_UPDATE | Update client |
| DELETE | /api/clients/:id | CLIENT_DELETE | Soft deactivate |
| GET | /api/clients/export | CLIENT_READ | Excel export |
