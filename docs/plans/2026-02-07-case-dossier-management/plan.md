# Case/Dossier Management - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Implement comprehensive case/dossier management system with custom numbering, flexible workflows, bilingual support, and financial tracking.

**Architecture:** Hexagonal architecture with Spring Boot 3.4 backend (Java 21), Angular 18 frontend, PostgreSQL database via Flyway migrations, MapStruct for DTO mapping, and TanStack Query for state management.

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Lombok, PostgreSQL, Tailwind CSS, TanStack Query.

---

## Implementation Strategy

**Total Batches:** 8 batches
**Approach:** Incremental implementation with full-stack verification at each checkpoint
**Package Structure:** `com.boilerplate` (to be refactored to `com.lawfirm` later)

---

## BATCH 1: Database Schema & Reference Data (Tribunals, Case Types, Categories)

**Goal:** Set up database tables and seed reference data for tribunals, case types, and case categories.

### Task 1.1: Create Tribunals Table Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V17__create_tribunals_table.sql`

**Implementation:**

```sql
CREATE TABLE tribunals (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_fr VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tribunals_code ON tribunals(code);
CREATE INDEX idx_tribunals_active ON tribunals(active);
```

**Verification:**
```bash
cd backend
mvn clean compile
# Flyway will validate migration syntax
```

Expected: Migration file created, no syntax errors.

---

### Task 1.2: Seed Tribunals Data

**Files:**
- Create: `backend/src/main/resources/db/migration/V18__seed_tribunals.sql`

**Implementation:**

```sql
-- Administrative Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_APPL_1', 'Tribunal d''appel administratif de Rabat', 'محكمة الاستئناف الإدارية بالرباط'),
('TR_ADM_APPL_2', 'Tribunal d''appel administratif de Marrakech', 'محكمة الإستئناف الإدارية بمراكش');

-- Administrative First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_PIN_1', 'Tribunal administratif de Rabat', 'المحكمة الإدارية بالرباط'),
('TR_ADM_PIN_2', 'Tribunal administratif de Casablanca', 'المحكمة الإدارية بالدار البيضاء'),
('TR_ADM_PIN_3', 'Tribunal administratif de Fes', 'المحكمة الإدارية بفاس'),
('TR_ADM_PIN_4', 'Tribunal administratif de Meknes', 'المحكمة الإدارية بمكناس'),
('TR_ADM_PIN_5', 'Tribunal administratif de Oujda', 'المحكمة الإدارية بوجدة'),
('TR_ADM_PIN_6', 'Tribunal administratif de Marrakech', 'المحكمة الإدارية بمراكش'),
('TR_ADM_PIN_7', 'Tribunal administratif de Agadir', 'المحكمة الإدارية بأكادير');

-- Commercial Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_APPL_1', 'Tribunal d''appel commercial de Casablanca', 'محكمة الاستئناف التجارية - الدار البيضاء'),
('TR_COM_APPL_2', 'Tribunal d''appel commercial de Marrakech', 'محكمة الاستئناف التجارية - مراكش'),
('TR_COM_APPL_3', 'Tribunal d''appel commercial de Fes', 'محكمة الاستئناف التجارية - فاس');

-- Commercial First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_PIN_1', 'Tribunal commercial de Rabat', 'المحكمة التجارية - الرباط'),
('TR_COM_PIN_2', 'Tribunal commercial de Casablanca', 'المحكمة التجارية - الدار البيضاء'),
('TR_COM_PIN_3', 'Tribunal commercial de Agadir', 'المحكمة التجارية - أكادير'),
('TR_COM_PIN_4', 'Tribunal commercial de Marrakech', 'المحكمة التجارية مراكش'),
('TR_COM_PIN_5', 'Tribunal commercial de Fes', 'المحكمة التجارية بفاس'),
('TR_COM_PIN_6', 'Tribunal commercial de Meknes', 'المحكمة التجارية بمكناس'),
('TR_COM_PIN_7', 'Tribunal commercial de Oujda', 'المحكمة التجارية بوجدة'),
('TR_COM_PIN_8', 'Tribunal commercial de Tanger', 'المحكمة التجارية بطنجة');

-- Appeal Courts (22 courts)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_APPL_1', 'Tribunal d''appel de Rabat', 'محكمة الاستئناف - بالرباط'),
('TR_APPL_2', 'Tribunal d''appel de Laayoune', 'محكمة الاستئناف العيون'),
('TR_APPL_3', 'Tribunal d''appel de Agadir', 'محكمة الاستئناف أكادير'),
('TR_APPL_4', 'Tribunal d''appel de Ouarzazate', 'محكمة الاستئناف ورزازات'),
('TR_APPL_5', 'Tribunal d''appel de Kenitra', 'محكمة الاستئناف القنيطرة'),
('TR_APPL_6', 'Tribunal d''appel de Settat', 'محكمة الاستئناف سطات'),
('TR_APPL_7', 'Tribunal d''appel de Khouribga', 'محكمة الاستئناف - خريبكة'),
('TR_APPL_8', 'Tribunal d''appel de Casablanca', 'محكمة الاستئناف - الدر البيضاء'),
('TR_APPL_9', 'Tribunal d''appel de Marrakech', 'محكمة الاستئناف - مراكش'),
('TR_APPL_10', 'Tribunal d''appel de Safi', 'محكمة الاستئناف - آسفي'),
('TR_APPL_11', 'Tribunal d''appel de Tanger', 'محكمة الاستئناف - طنجة'),
('TR_APPL_12', 'Tribunal d''appel de Tetouan', 'محكمة الاستئناف - تطوان'),
('TR_APPL_13', 'Tribunal d''appel de Al Hoceima', 'محكمة الاستئناف - الحسيمة'),
('TR_APPL_14', 'Tribunal d''appel de Taza', 'محكمة الاستئناف - تازة'),
('TR_APPL_15', 'Tribunal d''appel de Oujda', 'محكمة الاستئناف - وجدة'),
('TR_APPL_16', 'Tribunal d''appel de Meknes', 'محكمة الاستئناف - مكناس'),
('TR_APPL_17', 'Tribunal d''appel de El Jadida', 'محكمة الاستئناف - الجديدة'),
('TR_APPL_18', 'Tribunal d''appel de Beni Mellal', 'محكمة الاستئناف - بني ملال'),
('TR_APPL_19', 'Tribunal d''appel de Errachidia', 'محكمة الاستئناف - الرشيدية'),
('TR_APPL_20', 'Tribunal d''appel de Nador', 'محكمة الاستئناف - الناظور'),
('TR_APPL_21', 'Tribunal d''appel de Fes', 'محكمة الاستئناف - فاس'),
('TR_APPL_22', 'Tribunal d''appel de Guelmim', 'محكمة الاستئناف - كلميم');

-- First Instance Courts (sample - add all 83)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_PIN_1', 'Tribunal de 1ère instance de Rabat', 'المحكمة الابتدائية - الرباط'),
('TR_PIN_2', 'Tribunal de 1ère instance de Salé', 'المحكمة الابتدائية سلا'),
('TR_PIN_3', 'Tribunal de 1ère instance de Temara', 'المحكمة الابتدائية - تمارة'),
('TR_PIN_4', 'Tribunal de 1ère instance de Khemisset', 'المحكمة االابتدائية - الخميسات'),
('TR_PIN_5', 'Tribunal de 1ère instance de Rommani', 'المحكمة االابتدائية - الرماني'),
('TR_PIN_6', 'Tribunal de 1ère instance de Tiflet', 'المحكمة الابتدائية - تيفلت'),
('TR_PIN_31', 'Tribunal de 1ère instance civile de Casablanca', 'المحكمة الابتدائية المدنية بالدار البيضاء'),
('TR_PIN_37', 'Tribunal de 1ère instance de Marrakech', 'المحكمة الابتدائية - مراكش'),
('TR_PIN_44', 'Tribunal de 1ère instance de Tanger', 'المحكمة الابتدائية - طنجة'),
('TR_PIN_76', 'Tribunal de 1ère instance de Fes', 'المحكمة الابتدائية - فاس');
-- Continue for all remaining tribunals (TR_PIN_7 through TR_PIN_83)

-- Court of Cassation
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_CASS_1', 'Cour de cassation de Rabat', 'محكمة النقض بالرباط');
```

**Note:** Complete file will include all 130+ tribunals. For brevity, representative samples shown.

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Migration contains 130+ INSERT statements, no SQL syntax errors.

---

### Task 1.3: Create Case Types Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V19__create_case_types_table.sql`

**Implementation:**

```sql
CREATE TABLE case_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    number_format_template VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_types_code ON case_types(code);
CREATE INDEX idx_case_types_active ON case_types(active);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Migration file created, table definition valid.

---

### Task 1.4: Seed Case Types

**Files:**
- Create: `backend/src/main/resources/db/migration/V20__seed_case_types.sql`

**Implementation:**

```sql
INSERT INTO case_types (code, name_fr, name_ar, number_format_template) VALUES
('PENAL', 'Pénale', 'جنائي', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('COMMERC', 'Commerciale', 'تجاري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('CIVIL', 'Civile', 'مدني', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('ADM', 'Administrative', 'إداري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}');
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: 4 case types seeded.

---

### Task 1.5: Create Case Categories Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V21__create_case_categories_table.sql`

**Implementation:**

```sql
CREATE TABLE case_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name_ar VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255),
    case_type_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_type_id) REFERENCES case_types(id)
);

CREATE INDEX idx_case_categories_code ON case_categories(code);
CREATE INDEX idx_case_categories_case_type ON case_categories(case_type_id);
CREATE INDEX idx_case_categories_active ON case_categories(active);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Table created with proper foreign key.

---

### Task 1.6: Seed Case Categories (Part 1 - Administrative & Commercial)

**Files:**
- Create: `backend/src/main/resources/db/migration/V22__seed_case_categories.sql`

**Implementation:**

```sql
-- Administrative Court Categories (7xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7101', 'القضايا الاستعجالية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7102', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7103', 'المصادقة على الحجز', id FROM case_types WHERE code = 'ADM';
-- Continue for all 7xxx codes...

-- Commercial Court Categories (8xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8101', 'الاستعجالي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8102', 'الأمر بالأداء', id FROM case_types WHERE code = 'COMMERC';
-- Continue for all 8xxx codes...

-- Civil Court Categories (1xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1101', 'الاستعجالي', id FROM case_types WHERE code = 'CIVIL';
-- Continue for all 1xxx codes...

-- Criminal Cases (2xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2101', 'جنحي عادي تأديبي', id FROM case_types WHERE code = 'PENAL';
-- Continue for all 2xxx codes...

-- Execution/Enforcement Cases (6xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6101', 'البيوعات العقارية', id FROM case_types WHERE code = 'CIVIL';
-- Continue for all 6xxx codes...

-- Court of Cassation codes (no case type link)
INSERT INTO case_categories (code, name_ar) VALUES
('1', 'رمز مدني لمحكمة النقض'),
('2', 'الغرفة الإدارية'),
('3', 'رمز تجاري لمحكمة النقض'),
('4', 'الرمز الإداري لمحكمة النقد'),
('6', 'رمز جنائي لمحكمة النقض');
```

**Note:** Complete file contains 300+ INSERT statements. This is abbreviated for plan clarity.

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: 300+ case categories seeded successfully.

---

### Batch 1 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn flyway:info
```

**Expected Output:**
- V17-V22 migrations pending or successful
- No migration failures
- All tables created with proper indexes and constraints

**Manual DB Check (Optional):**
```sql
SELECT COUNT(*) FROM tribunals; -- Should return 130+
SELECT COUNT(*) FROM case_types; -- Should return 4
SELECT COUNT(*) FROM case_categories; -- Should return 300+
```

---

## BATCH 2: Case Statuses & Workflow Configuration

**Goal:** Create case statuses and configure allowed statuses per case type.

### Task 2.1: Create Case Statuses Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V23__create_case_statuses_table.sql`

**Implementation:**

```sql
CREATE TABLE case_statuses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0,
    is_terminal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_statuses_code ON case_statuses(code);
CREATE INDEX idx_case_statuses_sort_order ON case_statuses(sort_order);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Migration created successfully.

---

### Task 2.2: Seed Case Statuses

**Files:**
- Create: `backend/src/main/resources/db/migration/V24__seed_case_statuses.sql`

**Implementation:**

```sql
INSERT INTO case_statuses (code, name_fr, name_ar, sort_order, is_terminal) VALUES
('DRAFT', 'Brouillon', 'مسودة', 1, false),
('OPEN', 'Ouvert', 'مفتوح', 2, false),
('IN_PROGRESS', 'En cours', 'قيد التقدم', 3, false),
('HEARING', 'Audience', 'جلسة', 4, false),
('JUDGMENT', 'Jugement', 'حكم', 5, false),
('CLOSED', 'Clôturé', 'مغلق', 6, true),
('ARCHIVED', 'Archivé', 'مؤرشف', 7, true);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: 7 statuses inserted.

---

### Task 2.3: Create Case Type-Status Junction Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V25__create_case_type_statuses_table.sql`

**Implementation:**

```sql
CREATE TABLE case_type_statuses (
    case_type_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    PRIMARY KEY (case_type_id, status_id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id) ON DELETE CASCADE,
    FOREIGN KEY (status_id) REFERENCES case_statuses(id) ON DELETE CASCADE
);

CREATE INDEX idx_case_type_statuses_case_type ON case_type_statuses(case_type_id);
CREATE INDEX idx_case_type_statuses_status ON case_type_statuses(status_id);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Junction table created with composite primary key.

---

### Task 2.4: Seed Case Type-Status Relationships

**Files:**
- Create: `backend/src/main/resources/db/migration/V26__seed_case_type_statuses.sql`

**Implementation:**

```sql
-- Assign all statuses to all case types initially (can be customized later by admin)
INSERT INTO case_type_statuses (case_type_id, status_id)
SELECT ct.id, cs.id
FROM case_types ct
CROSS JOIN case_statuses cs;
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: 28 rows inserted (4 case types × 7 statuses).

---

### Batch 2 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn flyway:info
```

**Expected:**
- V23-V26 migrations successful
- All statuses available for all case types

---

## BATCH 3: Lawyers & Case Sequences

**Goal:** Create lawyer management and case number sequencing infrastructure.

### Task 3.1: Create Lawyers Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V27__create_lawyers_table.sql`

**Implementation:**

```sql
CREATE TABLE lawyers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    tax_id VARCHAR(50) UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_lawyers_tax_id ON lawyers(tax_id);
CREATE INDEX idx_lawyers_active ON lawyers(active);
CREATE INDEX idx_lawyers_full_name ON lawyers(first_name, last_name);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Lawyers table created with proper indexes.

---

### Task 3.2: Create Case Sequences Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V28__create_case_sequences_table.sql`

**Implementation:**

```sql
CREATE TABLE case_sequences (
    id BIGSERIAL PRIMARY KEY,
    year INT NOT NULL,
    case_type_code VARCHAR(20) NOT NULL,
    last_sequence INT NOT NULL DEFAULT 0,
    UNIQUE(year, case_type_code)
);

CREATE INDEX idx_case_sequences_year_type ON case_sequences(year, case_type_code);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Sequence table ready for atomic increments.

---

### Batch 3 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn flyway:info
```

**Expected:**
- V27-V28 migrations successful
- Lawyers and sequences tables ready

---

## BATCH 4: Cases & Financial Transactions Tables

**Goal:** Create the main case management tables.

### Task 4.1: Create Cases Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V29__create_cases_table.sql`

**Implementation:**

```sql
CREATE TABLE cases (
    id BIGSERIAL PRIMARY KEY,
    year INT NOT NULL,
    sequence_number INT NOT NULL,
    full_case_number VARCHAR(255) NOT NULL UNIQUE,
    registration_date DATE NOT NULL,
    case_description VARCHAR(500) NOT NULL,
    matter_description TEXT,
    tribunal_id BIGINT NOT NULL,
    case_type_id BIGINT NOT NULL,
    case_category_id BIGINT,
    lawyer_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (tribunal_id) REFERENCES tribunals(id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id),
    FOREIGN KEY (case_category_id) REFERENCES case_categories(id),
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id),
    FOREIGN KEY (status_id) REFERENCES case_statuses(id)
);

CREATE UNIQUE INDEX idx_cases_full_number ON cases(full_case_number);
CREATE INDEX idx_cases_year ON cases(year);
CREATE INDEX idx_cases_tribunal ON cases(tribunal_id);
CREATE INDEX idx_cases_case_type ON cases(case_type_id);
CREATE INDEX idx_cases_case_category ON cases(case_category_id);
CREATE INDEX idx_cases_lawyer ON cases(lawyer_id);
CREATE INDEX idx_cases_status ON cases(status_id);
CREATE INDEX idx_cases_registration_date ON cases(registration_date);
CREATE INDEX idx_cases_deleted_at ON cases(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_cases_year_sequence ON cases(year, sequence_number);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Cases table created with all foreign keys and indexes.

---

### Task 4.2: Create Financial Transactions Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V30__create_financial_transactions_table.sql`

**Implementation:**

```sql
CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('PAYMENT', 'EXPENSE')),
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE,
    payment_reference VARCHAR(100),
    lawyer_payment_year INT,
    fiscal_year_from DATE,
    fiscal_year_to DATE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_case ON financial_transactions(case_id);
CREATE INDEX idx_transactions_payment_date ON financial_transactions(payment_date);
CREATE INDEX idx_transactions_type ON financial_transactions(transaction_type);
CREATE INDEX idx_transactions_lawyer_payment_year ON financial_transactions(lawyer_payment_year);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Financial transactions table created.

---

### Task 4.3: Add Case Permissions

**Files:**
- Create: `backend/src/main/resources/db/migration/V31__add_case_permissions.sql`

**Implementation:**

```sql
-- Add case management permissions
INSERT INTO permissions (name, description) VALUES
('CASE_READ', 'Can view cases'),
('CASE_CREATE', 'Can create cases'),
('CASE_UPDATE', 'Can update cases'),
('CASE_DELETE', 'Can delete cases'),
('CASE_MANAGE', 'Full case management access'),
('LAWYER_READ', 'Can view lawyers'),
('LAWYER_CREATE', 'Can create lawyers'),
('LAWYER_UPDATE', 'Can update lawyers'),
('LAWYER_DELETE', 'Can delete lawyers'),
('LAWYER_MANAGE', 'Full lawyer management access'),
('TRIBUNAL_READ', 'Can view tribunals'),
('TRIBUNAL_MANAGE', 'Can manage tribunal reference data'),
('CASETYPE_READ', 'Can view case types'),
('CASETYPE_MANAGE', 'Can manage case type reference data');

-- Assign to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND p.name IN (
    'CASE_READ', 'CASE_CREATE', 'CASE_UPDATE', 'CASE_DELETE', 'CASE_MANAGE',
    'LAWYER_READ', 'LAWYER_CREATE', 'LAWYER_UPDATE', 'LAWYER_DELETE', 'LAWYER_MANAGE',
    'TRIBUNAL_READ', 'TRIBUNAL_MANAGE',
    'CASETYPE_READ', 'CASETYPE_MANAGE'
);
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: 14 new permissions added and assigned to ADMIN role.

---

### Batch 4 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

**Expected:**
- Application starts successfully
- All Flyway migrations (V17-V31) executed
- Database schema complete with all tables and relationships
- No foreign key constraint errors

**Manual DB Verification:**
```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('tribunals', 'case_types', 'case_categories', 'case_statuses', 'lawyers', 'cases', 'financial_transactions');
-- Should return all 7 tables
```

---

## BATCH 5: Backend Domain Entities & Repositories

**Goal:** Create JPA entities and repositories for all domain models.

### Task 5.1: Create Tribunal Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/Tribunal.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tribunals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tribunal extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String nameFr;

    @Column(nullable = false, length = 255)
    private String nameAr;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity compiles successfully with Lombok and JPA annotations.

---

### Task 5.2: Create Tribunal Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/TribunalRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Tribunal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribunalRepository extends JpaRepository<Tribunal, Long> {

    Optional<Tribunal> findByCode(String code);

    Optional<Tribunal> findByCodeAndActiveTrue(String code);

    List<Tribunal> findAllByActiveTrue();
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository interface created, Spring Data JPA methods recognized.

---

### Task 5.3: Create CaseType Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/CaseType.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "case_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String nameFr;

    @Column(length = 100)
    private String nameAr;

    @Column(nullable = false, length = 255)
    private String numberFormatTemplate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "case_type_statuses",
        joinColumns = @JoinColumn(name = "case_type_id"),
        inverseJoinColumns = @JoinColumn(name = "status_id")
    )
    @Builder.Default
    private Set<CaseStatus> allowedStatuses = new HashSet<>();
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity compiles with ManyToMany relationship.

---

### Task 5.4: Create CaseType Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseTypeRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

    Optional<CaseType> findByCode(String code);

    Optional<CaseType> findByCodeAndActiveTrue(String code);

    @Query("SELECT ct FROM CaseType ct LEFT JOIN FETCH ct.allowedStatuses WHERE ct.code = :code")
    Optional<CaseType> findByCodeWithStatuses(@Param("code") String code);

    List<CaseType> findAllByActiveTrue();
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository with custom JOIN FETCH query compiles.

---

### Task 5.5: Create CaseCategory Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/CaseCategory.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 255)
    private String nameAr;

    @Column(length = 255)
    private String nameFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id")
    private CaseType caseType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity compiles successfully.

---

### Task 5.6: Create CaseCategory Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseCategoryRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.CaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseCategoryRepository extends JpaRepository<CaseCategory, Long> {

    Optional<CaseCategory> findByCode(String code);

    Optional<CaseCategory> findByCodeAndActiveTrue(String code);

    List<CaseCategory> findAllByActiveTrueOrderByCodeAsc();

    List<CaseCategory> findByCaseTypeIdAndActiveTrue(Long caseTypeId);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository compiles.

---

### Task 5.7: Create CaseStatus Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/CaseStatus.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String nameFr;

    @Column(length = 100)
    private String nameAr;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTerminal = false;
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity compiles.

---

### Task 5.8: Create CaseStatus Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseStatusRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseStatusRepository extends JpaRepository<CaseStatus, Long> {

    Optional<CaseStatus> findByCode(String code);

    List<CaseStatus> findAllByOrderBySortOrderAsc();
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository compiles.

---

### Task 5.9: Create Lawyer Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/Lawyer.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lawyers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Lawyer extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 50)
    private String taxId;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "lawyer")
    @Builder.Default
    private List<Case> cases = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity with computed getFullName() compiles.

---

### Task 5.10: Create Lawyer Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/LawyerRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, Long> {

    Optional<Lawyer> findByIdAndActiveTrue(Long id);

    Optional<Lawyer> findByTaxId(String taxId);

    List<Lawyer> findAllByActiveTrue();

    @Query("SELECT COUNT(c) FROM Case c WHERE c.lawyer.id = :lawyerId AND c.deletedAt IS NULL")
    Long countActiveCases(@Param("lawyerId") Long lawyerId);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository with custom count query compiles.

---

### Batch 5 Checkpoint (Part 1)

**Verify:**
```bash
cd backend
mvn clean compile
mvn test
```

**Expected:**
- All entities and repositories compile
- No circular dependency errors
- BaseEntity properly inherited

---

### Task 5.11: Create CaseSequence Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/CaseSequence.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "case_sequences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"year", "case_type_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 20)
    private String caseTypeCode;

    @Column(nullable = false)
    @Builder.Default
    private Integer lastSequence = 0;
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity compiles with unique constraint.

---

### Task 5.12: Create CaseSequence Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseSequenceRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.CaseSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CaseSequenceRepository extends JpaRepository<CaseSequence, Long> {

    Optional<CaseSequence> findByYearAndCaseTypeCode(Integer year, String caseTypeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CaseSequence cs WHERE cs.year = :year AND cs.caseTypeCode = :caseTypeCode")
    Optional<CaseSequence> findByYearAndCaseTypeCodeForUpdate(
        @Param("year") Integer year,
        @Param("caseTypeCode") String caseTypeCode
    );
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository with pessimistic locking compiles.

---

### Task 5.13: Create Case Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/Case.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Case extends BaseEntity {

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, unique = true, length = 255)
    private String fullCaseNumber;

    @Column(nullable = false)
    private LocalDate registrationDate;

    @Column(nullable = false, length = 500)
    private String caseDescription;

    @Column(columnDefinition = "TEXT")
    private String matterDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tribunal_id", nullable = false)
    private Tribunal tribunal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_type_id", nullable = false)
    private CaseType caseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_category_id")
    private CaseCategory caseCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CaseStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FinancialTransaction> transactions = new ArrayList<>();
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Case entity compiles with all relationships.

---

### Task 5.14: Create Case Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByFullCaseNumber(String fullCaseNumber);

    @Query("SELECT c FROM Case c " +
           "LEFT JOIN FETCH c.tribunal " +
           "LEFT JOIN FETCH c.caseType " +
           "LEFT JOIN FETCH c.caseCategory " +
           "LEFT JOIN FETCH c.lawyer " +
           "LEFT JOIN FETCH c.status " +
           "WHERE c.id = :id")
    Optional<Case> findByIdWithDetails(@Param("id") Long id);

    boolean existsByFullCaseNumber(String fullCaseNumber);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository with JpaSpecificationExecutor compiles.

---

### Task 5.15: Create FinancialTransaction Entity

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/model/FinancialTransaction.java`

**Implementation:**

```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FinancialTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "lawyer_payment_year")
    private Integer lawyerPaymentYear;

    @Column(name = "fiscal_year_from")
    private LocalDate fiscalYearFrom;

    @Column(name = "fiscal_year_to")
    private LocalDate fiscalYearTo;

    @Column(columnDefinition = "TEXT")
    private String description;

    public enum TransactionType {
        PAYMENT,
        EXPENSE
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Entity with enum compiles.

---

### Task 5.16: Create FinancialTransaction Repository

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/FinancialTransactionRepository.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByCaseEntityId(Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.transactionType = 'PAYMENT'")
    BigDecimal sumPaymentsByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.transactionType = 'EXPENSE'")
    BigDecimal sumExpensesByCaseId(@Param("caseId") Long caseId);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Repository with sum queries compiles.

---

### Task 5.17: Create CaseSpecification for Dynamic Filtering

**Files:**
- Create: `backend/src/main/java/com/boilerplate/domain/repository/CaseSpecification.java`

**Implementation:**

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Case;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CaseSpecification {

    public static Specification<Case> withFilters(
        Integer year,
        String caseTypeCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Not deleted
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (year != null) {
                predicates.add(criteriaBuilder.equal(root.get("year"), year));
            }

            if (caseTypeCode != null && !caseTypeCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("caseType").get("code"), caseTypeCode));
            }

            if (tribunalCode != null && !tribunalCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("tribunal").get("code"), tribunalCode));
            }

            if (lawyerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("lawyer").get("id"), lawyerId));
            }

            if (statusCode != null && !statusCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status").get("code"), statusCode));
            }

            if (registrationDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("registrationDate"), registrationDateFrom));
            }

            if (registrationDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("registrationDate"), registrationDateTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Specification class compiles.

---

### Batch 5 Final Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

**Expected:**
- Application starts successfully
- All entities load without errors
- Repository beans created
- No circular dependencies

**Test Database Connection:**
```bash
# Application should start and log:
# "Started BoilerplateApplication in X seconds"
# Check logs for any JPA/Hibernate errors
```

---

## BATCH 6: DTOs & MapStruct Mappers

**Goal:** Create Request/Response DTOs and MapStruct mappers for type-safe data transfer.

### Task 6.1: Create TribunalResponse DTO

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/TribunalResponse.java`

**Implementation:**

```java
package com.boilerplate.application.dto.response;

public record TribunalResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Boolean active
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Record DTO compiles.

---

### Task 6.2: Create Tribunal Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/TribunalMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.domain.model.Tribunal;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TribunalMapper {

    TribunalResponse toResponse(Tribunal tribunal);

    List<TribunalResponse> toResponseList(List<Tribunal> tribunals);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: MapStruct generates implementation.

---

### Task 6.3: Create CaseType DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/CaseTypeResponse.java`

**Implementation:**

```java
package com.boilerplate.application.dto.response;

import java.util.List;

public record CaseTypeResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    String numberFormatTemplate,
    Boolean active,
    List<CaseStatusResponse> allowedStatuses
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: DTO compiles.

---

### Task 6.4: Create CaseType Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/CaseTypeMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseTypeResponse;
import com.boilerplate.domain.model.CaseType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CaseStatusMapper.class})
public interface CaseTypeMapper {

    CaseTypeResponse toResponse(CaseType caseType);

    List<CaseTypeResponse> toResponseList(List<CaseType> caseTypes);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Mapper with nested CaseStatusMapper compiles.

---

### Task 6.5: Create CaseCategory DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/CaseCategoryResponse.java`

**Implementation:**

```java
package com.boilerplate.application.dto.response;

public record CaseCategoryResponse(
    Long id,
    String code,
    String nameAr,
    String nameFr,
    String caseTypeCode
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: DTO compiles.

---

### Task 6.6: Create CaseCategory Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/CaseCategoryMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseCategoryResponse;
import com.boilerplate.domain.model.CaseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseCategoryMapper {

    @Mapping(target = "caseTypeCode", source = "caseType.code")
    CaseCategoryResponse toResponse(CaseCategory caseCategory);

    List<CaseCategoryResponse> toResponseList(List<CaseCategory> caseCategories);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Mapper with custom mapping compiles.

---

### Task 6.7: Create CaseStatus DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/CaseStatusResponse.java`

**Implementation:**

```java
package com.boilerplate.application.dto.response;

public record CaseStatusResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Integer sortOrder,
    Boolean isTerminal
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: DTO compiles.

---

### Task 6.8: Create CaseStatus Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/CaseStatusMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseStatusResponse;
import com.boilerplate.domain.model.CaseStatus;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseStatusMapper {

    CaseStatusResponse toResponse(CaseStatus caseStatus);

    List<CaseStatusResponse> toResponseList(List<CaseStatus> caseStatuses);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Mapper compiles.

---

### Task 6.9: Create Lawyer DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/LawyerResponse.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/CreateLawyerRequest.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/UpdateLawyerRequest.java`

**Implementation:**

```java
// LawyerResponse.java
package com.boilerplate.application.dto.response;

public record LawyerResponse(
    Long id,
    String firstName,
    String lastName,
    String fullName,
    String taxId,
    String email,
    String phone,
    Boolean active
) {}

// CreateLawyerRequest.java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLawyerRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    String lastName,

    @Size(max = 50)
    String taxId,

    @Email
    @Size(max = 100)
    String email,

    @Size(max = 20)
    String phone
) {}

// UpdateLawyerRequest.java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateLawyerRequest(
    @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName,

    @Size(max = 50)
    String taxId,

    @Email
    @Size(max = 100)
    String email,

    @Size(max = 20)
    String phone
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: All three DTOs compile with validation.

---

### Task 6.10: Create Lawyer Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/LawyerMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.request.CreateLawyerRequest;
import com.boilerplate.application.dto.request.UpdateLawyerRequest;
import com.boilerplate.application.dto.response.LawyerResponse;
import com.boilerplate.domain.model.Lawyer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LawyerMapper {

    @Mapping(target = "fullName", expression = "java(lawyer.getFullName())")
    LawyerResponse toResponse(Lawyer lawyer);

    List<LawyerResponse> toResponseList(List<Lawyer> lawyers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "cases", ignore = true)
    Lawyer toEntity(CreateLawyerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "cases", ignore = true)
    void updateEntity(UpdateLawyerRequest request, @MappingTarget Lawyer lawyer);
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Mapper with expression and update method compiles.

---

### Task 6.11: Create Case Request DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/CreateCaseRequest.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/UpdateCaseRequest.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/ChangeStatusRequest.java`

**Implementation:**

```java
// CreateCaseRequest.java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCaseRequest(
    @NotBlank(message = "Case type code is required")
    @Size(max = 20)
    String caseTypeCode,

    @Size(max = 10)
    String caseCategoryCode,

    @NotBlank(message = "Tribunal code is required")
    @Size(max = 50)
    String tribunalCode,

    @NotNull(message = "Lawyer ID is required")
    Long lawyerId,

    @NotNull(message = "Registration date is required")
    @PastOrPresent(message = "Registration date cannot be in the future")
    LocalDate registrationDate,

    @NotBlank(message = "Case description is required")
    @Size(max = 500, message = "Case description must not exceed 500 characters")
    String caseDescription,

    @Size(max = 1000, message = "Matter description must not exceed 1000 characters")
    String matterDescription,

    @Size(max = 50)
    String initialStatusCode
) {}

// UpdateCaseRequest.java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCaseRequest(
    @Size(max = 50)
    String tribunalCode,

    @Size(max = 10)
    String caseCategoryCode,

    Long lawyerId,

    @PastOrPresent
    LocalDate registrationDate,

    @Size(max = 500)
    String caseDescription,

    @Size(max = 1000)
    String matterDescription
) {}

// ChangeStatusRequest.java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
    @NotBlank(message = "Status code is required")
    String statusCode,

    @Size(max = 500)
    String reason
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: All request DTOs compile.

---

### Task 6.12: Create Case Response DTOs

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/CaseResponse.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/CaseSummary.java`
- Create: `backend/src/main/java/com/boilerplate/application/dto/response/FinancialSummary.java`

**Implementation:**

```java
// CaseResponse.java
package com.boilerplate.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CaseResponse(
    Long id,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    Integer year,
    Integer sequenceNumber,
    String fullCaseNumber,
    LocalDate registrationDate,
    String caseDescription,
    String matterDescription,

    TribunalResponse tribunal,
    CaseTypeResponse caseType,
    CaseCategoryResponse caseCategory,
    LawyerResponse lawyer,
    CaseStatusResponse status,

    FinancialSummary financialSummary
) {}

// CaseSummary.java
package com.boilerplate.application.dto.response;

import java.time.LocalDate;

public record CaseSummary(
    Long id,
    String fullCaseNumber,
    String caseDescription,
    String tribunalNameFr,
    String caseTypeNameFr,
    String lawyerName,
    String statusNameFr,
    LocalDate registrationDate
) {}

// FinancialSummary.java
package com.boilerplate.application.dto.response;

import java.math.BigDecimal;

public record FinancialSummary(
    BigDecimal totalPayments,
    BigDecimal totalExpenses,
    BigDecimal balance,
    Integer transactionCount
) {}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Response DTOs compile.

---

### Task 6.13: Create Case Mapper

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/mapper/CaseMapper.java`

**Implementation:**

```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseResponse;
import com.boilerplate.application.dto.response.CaseSummary;
import com.boilerplate.application.dto.response.FinancialSummary;
import com.boilerplate.domain.model.Case;
import com.boilerplate.domain.model.FinancialTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {
    TribunalMapper.class,
    CaseTypeMapper.class,
    CaseCategoryMapper.class,
    LawyerMapper.class,
    CaseStatusMapper.class
})
public interface CaseMapper {

    @Mapping(target = "financialSummary", expression = "java(calculateFinancialSummary(caseEntity))")
    CaseResponse toResponse(Case caseEntity);

    @Mapping(target = "tribunalNameFr", source = "tribunal.nameFr")
    @Mapping(target = "caseTypeNameFr", source = "caseType.nameFr")
    @Mapping(target = "lawyerName", expression = "java(caseEntity.getLawyer().getFullName())")
    @Mapping(target = "statusNameFr", source = "status.nameFr")
    CaseSummary toSummary(Case caseEntity);

    List<CaseResponse> toResponseList(List<Case> cases);

    List<CaseSummary> toSummaryList(List<Case> cases);

    default FinancialSummary calculateFinancialSummary(Case caseEntity) {
        if (caseEntity.getTransactions() == null || caseEntity.getTransactions().isEmpty()) {
            return new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        BigDecimal totalPayments = caseEntity.getTransactions().stream()
            .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.PAYMENT)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = caseEntity.getTransactions().stream()
            .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.EXPENSE)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalPayments.subtract(totalExpenses);
        int count = caseEntity.getTransactions().size();

        return new FinancialSummary(totalPayments, totalExpenses, balance, count);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Complex mapper with custom method compiles.

---

### Batch 6 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
```

**Expected:**
- All DTOs and mappers compile
- MapStruct generates implementations
- No compilation errors

**Check Generated Classes:**
```bash
ls target/generated-sources/annotations/com/boilerplate/application/mapper/
# Should show CaseMapperImpl, LawyerMapperImpl, etc.
```

---

## BATCH 7: Services & Business Logic

**Goal:** Implement core business logic for case management, sequencing, and reference data.

### Task 7.1: Create CaseNumberGenerator Service

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/service/CaseNumberGenerator.java`

**Implementation:**

```java
package com.boilerplate.application.service;

import com.boilerplate.presentation.exception.InvalidCaseNumberFormatException;
import org.springframework.stereotype.Service;

@Service
public class CaseNumberGenerator {

    public String generate(
        String template,
        int year,
        String tribunalCode,
        String caseTypeCode,
        int sequence
    ) {
        String result = template;

        result = result.replace("{YEAR}", String.valueOf(year));
        result = result.replace("{TRIBUNAL_CODE}", tribunalCode);
        result = result.replace("{CASETYPE}", caseTypeCode);
        result = result.replace("{SEQ5}", String.format("%05d", sequence));

        // Validate no unreplaced placeholders
        if (result.contains("{") || result.contains("}")) {
            throw new InvalidCaseNumberFormatException("Invalid template: " + template);
        }

        return result;
    }

    public String preview(String template, String tribunalCode, String caseTypeCode) {
        return generate(template, java.time.Year.now().getValue(), tribunalCode, caseTypeCode, 1);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Service compiles.

---

### Task 7.2: Create InvalidCaseNumberFormatException

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/exception/InvalidCaseNumberFormatException.java`

**Implementation:**

```java
package com.boilerplate.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCaseNumberFormatException extends RuntimeException {
    public InvalidCaseNumberFormatException(String message) {
        super(message);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Exception class compiles.

---

### Task 7.3: Create CaseSequenceService

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/service/CaseSequenceService.java`

**Implementation:**

```java
package com.boilerplate.application.service;

import com.boilerplate.domain.model.CaseSequence;
import com.boilerplate.domain.repository.CaseSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaseSequenceService {

    private final CaseSequenceRepository caseSequenceRepository;

    @Transactional
    public synchronized int getNextSequence(int year, String caseTypeCode) {
        CaseSequence sequence = caseSequenceRepository
            .findByYearAndCaseTypeCodeForUpdate(year, caseTypeCode)
            .orElseGet(() -> {
                CaseSequence newSeq = CaseSequence.builder()
                    .year(year)
                    .caseTypeCode(caseTypeCode)
                    .lastSequence(0)
                    .build();
                return caseSequenceRepository.save(newSeq);
            });

        int nextSequence = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSequence);
        caseSequenceRepository.save(sequence);

        return nextSequence;
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Service with synchronized method compiles.

---

### Task 7.4: Create TribunalService

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/service/TribunalService.java`

**Implementation:**

```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.application.mapper.TribunalMapper;
import com.boilerplate.domain.repository.TribunalRepository;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TribunalService {

    private final TribunalRepository tribunalRepository;
    private final TribunalMapper tribunalMapper;

    public List<TribunalResponse> findAll() {
        return tribunalMapper.toResponseList(tribunalRepository.findAllByActiveTrue());
    }

    public TribunalResponse findByCode(String code) {
        return tribunalRepository.findByCodeAndActiveTrue(code)
            .map(tribunalMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal", "code", code));
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Service compiles.

---

### Task 7.5: Create LawyerService

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/service/LawyerService.java`

**Implementation:**

```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateLawyerRequest;
import com.boilerplate.application.dto.request.UpdateLawyerRequest;
import com.boilerplate.application.dto.response.LawyerResponse;
import com.boilerplate.application.mapper.LawyerMapper;
import com.boilerplate.domain.model.Lawyer;
import com.boilerplate.domain.repository.LawyerRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerService {

    private final LawyerRepository lawyerRepository;
    private final LawyerMapper lawyerMapper;

    public List<LawyerResponse> findAll() {
        return lawyerMapper.toResponseList(lawyerRepository.findAllByActiveTrue());
    }

    public LawyerResponse findById(Long id) {
        return lawyerRepository.findByIdAndActiveTrue(id)
            .map(lawyerMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", id));
    }

    @Transactional
    public LawyerResponse create(CreateLawyerRequest request) {
        // Check tax ID uniqueness if provided
        if (request.taxId() != null && !request.taxId().isBlank()) {
            if (lawyerRepository.findByTaxId(request.taxId()).isPresent()) {
                throw new DuplicateResourceException("Lawyer with tax ID " + request.taxId() + " already exists");
            }
        }

        Lawyer lawyer = lawyerMapper.toEntity(request);
        lawyer = lawyerRepository.save(lawyer);
        return lawyerMapper.toResponse(lawyer);
    }

    @Transactional
    public LawyerResponse update(Long id, UpdateLawyerRequest request) {
        Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", id));

        // Check tax ID uniqueness if changed
        if (request.taxId() != null && !request.taxId().equals(lawyer.getTaxId())) {
            if (lawyerRepository.findByTaxId(request.taxId()).isPresent()) {
                throw new DuplicateResourceException("Lawyer with tax ID " + request.taxId() + " already exists");
            }
        }

        lawyerMapper.updateEntity(request, lawyer);
        lawyer = lawyerRepository.save(lawyer);
        return lawyerMapper.toResponse(lawyer);
    }

    @Transactional
    public void deactivate(Long id) {
        Lawyer lawyer = lawyerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", id));
        lawyer.setActive(false);
        lawyerRepository.save(lawyer);
    }

    public Long getCaseCount(Long lawyerId) {
        return lawyerRepository.countActiveCases(lawyerId);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Service with full CRUD logic compiles.

---

### Task 7.6: Create CaseService (Part 1 - Create Case)

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/service/CaseService.java`

**Implementation:**

```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.ChangeStatusRequest;
import com.boilerplate.application.dto.request.CreateCaseRequest;
import com.boilerplate.application.dto.request.UpdateCaseRequest;
import com.boilerplate.application.dto.response.CaseResponse;
import com.boilerplate.application.dto.response.CaseSummary;
import com.boilerplate.application.mapper.CaseMapper;
import com.boilerplate.domain.model.*;
import com.boilerplate.domain.repository.*;
import com.boilerplate.infrastructure.security.UserPrincipal;
import com.boilerplate.presentation.exception.InvalidStatusTransitionException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseService {

    private final CaseRepository caseRepository;
    private final TribunalRepository tribunalRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final CaseCategoryRepository caseCategoryRepository;
    private final LawyerRepository lawyerRepository;
    private final CaseStatusRepository caseStatusRepository;
    private final CaseSequenceService caseSequenceService;
    private final CaseNumberGenerator caseNumberGenerator;
    private final CaseMapper caseMapper;

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, UserPrincipal currentUser) {
        // 1. Validate references
        CaseType caseType = caseTypeRepository.findByCodeAndActiveTrue(request.caseTypeCode())
            .orElseThrow(() -> new ResourceNotFoundException("CaseType", "code", request.caseTypeCode()));

        Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal", "code", request.tribunalCode()));

        Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", request.lawyerId()));

        // 2. Handle optional case category
        CaseCategory caseCategory = null;
        if (request.caseCategoryCode() != null && !request.caseCategoryCode().isBlank()) {
            caseCategory = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory", "code", request.caseCategoryCode()));

            // Validate category belongs to case type
            if (caseCategory.getCaseType() != null &&
                !caseCategory.getCaseType().getCode().equals(caseType.getCode())) {
                throw new IllegalArgumentException(
                    "Case category " + request.caseCategoryCode() +
                    " does not belong to case type " + request.caseTypeCode()
                );
            }
        }

        // 3. Determine initial status
        CaseStatus initialStatus = determineInitialStatus(caseType, request.initialStatusCode());

        // 4. Generate case number
        int year = Year.now().getValue();
        int sequenceNumber = caseSequenceService.getNextSequence(year, caseType.getCode());
        String fullCaseNumber = caseNumberGenerator.generate(
            caseType.getNumberFormatTemplate(),
            year,
            tribunal.getCode(),
            caseType.getCode(),
            sequenceNumber
        );

        // 5. Build and save case
        Case caseEntity = Case.builder()
            .year(year)
            .sequenceNumber(sequenceNumber)
            .fullCaseNumber(fullCaseNumber)
            .registrationDate(request.registrationDate())
            .caseDescription(request.caseDescription())
            .matterDescription(request.matterDescription())
            .tribunal(tribunal)
            .caseType(caseType)
            .caseCategory(caseCategory)
            .lawyer(lawyer)
            .status(initialStatus)
            .build();

        caseEntity = caseRepository.save(caseEntity);

        // 6. TODO: Publish audit event (will be added in later task)

        return caseMapper.toResponse(caseEntity);
    }

    private CaseStatus determineInitialStatus(CaseType caseType, String statusCode) {
        if (statusCode != null && !statusCode.isBlank()) {
            CaseStatus status = caseStatusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStatus", "code", statusCode));

            // Validate status is allowed for this case type
            if (!caseType.getAllowedStatuses().contains(status)) {
                throw new InvalidStatusTransitionException(
                    "Status " + statusCode + " is not allowed for case type " + caseType.getCode()
                );
            }
            return status;
        }

        // Default to first status (DRAFT)
        return caseStatusRepository.findByCode("DRAFT")
            .orElseThrow(() -> new ResourceNotFoundException("CaseStatus", "code", "DRAFT"));
    }

    public CaseResponse findById(Long id) {
        return caseRepository.findByIdWithDetails(id)
            .map(caseMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));
    }

    public Page<CaseSummary> searchCases(
        Integer year,
        String caseTypeCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        java.time.LocalDate registrationDateFrom,
        java.time.LocalDate registrationDateTo,
        Pageable pageable
    ) {
        Specification<Case> spec = CaseSpecification.withFilters(
            year, caseTypeCode, tribunalCode, lawyerId, statusCode,
            registrationDateFrom, registrationDateTo
        );

        return caseRepository.findAll(spec, pageable)
            .map(caseMapper::toSummary);
    }

    @Transactional
    public CaseResponse updateCase(Long id, UpdateCaseRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));

        // Update mutable fields only
        if (request.tribunalCode() != null) {
            Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Tribunal", "code", request.tribunalCode()));
            caseEntity.setTribunal(tribunal);
        }

        if (request.caseCategoryCode() != null) {
            CaseCategory category = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory", "code", request.caseCategoryCode()));
            caseEntity.setCaseCategory(category);
        }

        if (request.lawyerId() != null) {
            Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", request.lawyerId()));
            caseEntity.setLawyer(lawyer);
        }

        if (request.registrationDate() != null) {
            caseEntity.setRegistrationDate(request.registrationDate());
        }

        if (request.caseDescription() != null) {
            caseEntity.setCaseDescription(request.caseDescription());
        }

        if (request.matterDescription() != null) {
            caseEntity.setMatterDescription(request.matterDescription());
        }

        caseEntity = caseRepository.save(caseEntity);
        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public CaseResponse changeStatus(Long id, ChangeStatusRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));

        CaseStatus newStatus = caseStatusRepository.findByCode(request.statusCode())
            .orElseThrow(() -> new ResourceNotFoundException("CaseStatus", "code", request.statusCode()));

        // Validate status is allowed for this case type
        if (!caseEntity.getCaseType().getAllowedStatuses().contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Status " + newStatus.getCode() + " is not allowed for case type " +
                caseEntity.getCaseType().getCode()
            );
        }

        caseEntity.setStatus(newStatus);
        caseEntity = caseRepository.save(caseEntity);

        // TODO: Publish status changed event

        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public void deleteCase(Long id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));

        // Soft delete
        caseEntity.setDeletedAt(LocalDateTime.now());
        caseRepository.save(caseEntity);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Complete CaseService compiles.

---

### Task 7.7: Create InvalidStatusTransitionException

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/exception/InvalidStatusTransitionException.java`

**Implementation:**

```java
package com.boilerplate.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Exception compiles.

---

### Batch 7 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn test
```

**Expected:**
- All services compile
- No circular dependencies
- Application context loads successfully

---

## BATCH 8: Controllers & API Endpoints

**Goal:** Create REST controllers to expose case management APIs.

### Task 8.1: Create TribunalController

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/controller/TribunalController.java`

**Implementation:**

```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.application.service.TribunalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tribunals")
@RequiredArgsConstructor
@Tag(name = "Tribunals", description = "Tribunal reference data management")
public class TribunalController {

    private final TribunalService tribunalService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all active tribunals")
    public ResponseEntity<List<TribunalResponse>> getAllTribunals() {
        return ResponseEntity.ok(tribunalService.findAll());
    }

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tribunal by code")
    public ResponseEntity<TribunalResponse> getTribunalByCode(@PathVariable String code) {
        return ResponseEntity.ok(tribunalService.findByCode(code));
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Controller compiles with Swagger annotations.

---

### Task 8.2: Create LawyerController

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/controller/LawyerController.java`

**Implementation:**

```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.CreateLawyerRequest;
import com.boilerplate.application.dto.request.UpdateLawyerRequest;
import com.boilerplate.application.dto.response.LawyerResponse;
import com.boilerplate.application.service.LawyerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
@Tag(name = "Lawyers", description = "Lawyer management")
public class LawyerController {

    private final LawyerService lawyerService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'LAWYER_READ')")
    @Operation(summary = "Get all active lawyers")
    public ResponseEntity<List<LawyerResponse>> getAllLawyers() {
        return ResponseEntity.ok(lawyerService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'LAWYER_READ')")
    @Operation(summary = "Get lawyer by ID")
    public ResponseEntity<LawyerResponse> getLawyerById(@PathVariable Long id) {
        return ResponseEntity.ok(lawyerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'LAWYER_CREATE')")
    @Operation(summary = "Create new lawyer")
    public ResponseEntity<LawyerResponse> createLawyer(@Valid @RequestBody CreateLawyerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lawyerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'LAWYER_UPDATE')")
    @Operation(summary = "Update lawyer")
    public ResponseEntity<LawyerResponse> updateLawyer(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLawyerRequest request
    ) {
        return ResponseEntity.ok(lawyerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'LAWYER_DELETE')")
    @Operation(summary = "Deactivate lawyer")
    public ResponseEntity<Void> deactivateLawyer(@PathVariable Long id) {
        lawyerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cases/count")
    @PreAuthorize("hasPermission(null, 'LAWYER_READ')")
    @Operation(summary = "Get active case count for lawyer")
    public ResponseEntity<Long> getLawyerCaseCount(@PathVariable Long id) {
        return ResponseEntity.ok(lawyerService.getCaseCount(id));
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Controller with full CRUD compiles.

---

### Task 8.3: Create CaseController

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/controller/CaseController.java`

**Implementation:**

```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.ChangeStatusRequest;
import com.boilerplate.application.dto.request.CreateCaseRequest;
import com.boilerplate.application.dto.request.UpdateCaseRequest;
import com.boilerplate.application.dto.response.CaseResponse;
import com.boilerplate.application.dto.response.CaseSummary;
import com.boilerplate.application.service.CaseService;
import com.boilerplate.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Case/Dossier management")
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CASE_CREATE')")
    @Operation(summary = "Create new case")
    public ResponseEntity<CaseResponse> createCase(
        @Valid @RequestBody CreateCaseRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(caseService.createCase(request, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    @Operation(summary = "Get case by ID")
    public ResponseEntity<CaseResponse> getCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    @Operation(summary = "Search/list cases")
    public ResponseEntity<Page<CaseSummary>> searchCases(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String caseTypeCode,
        @RequestParam(required = false) String tribunalCode,
        @RequestParam(required = false) Long lawyerId,
        @RequestParam(required = false) String statusCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "registrationDate") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(caseService.searchCases(
            year, caseTypeCode, tribunalCode, lawyerId, statusCode,
            registrationDateFrom, registrationDateTo, pageable
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_UPDATE')")
    @Operation(summary = "Update case")
    public ResponseEntity<CaseResponse> updateCase(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCaseRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(caseService.updateCase(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'CASE_UPDATE')")
    @Operation(summary = "Change case status")
    public ResponseEntity<CaseResponse> changeStatus(
        @PathVariable Long id,
        @Valid @RequestBody ChangeStatusRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(caseService.changeStatus(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_DELETE')")
    @Operation(summary = "Delete case (soft delete)")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Verification:**
```bash
cd backend
mvn clean compile
```

Expected: Full case controller compiles.

---

### Batch 8 Checkpoint

**Verify:**
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

**Expected:**
- Application starts successfully
- Swagger UI available at http://localhost:8080/swagger-ui.html
- All endpoints visible in Swagger
- Can test endpoints with existing admin user

**Manual Test:**
```bash
# Get tribunals
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/tribunals

# Create lawyer
curl -X POST -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ahmed","lastName":"Benali","taxId":"TAX123"}' \
  http://localhost:8080/api/lawyers
```

---

## Final Verification & Next Steps

**Complete Backend Verification:**

```bash
cd backend

# 1. Clean build
mvn clean compile

# 2. Run all tests
mvn test

# 3. Start application
mvn spring-boot:run

# 4. Check Swagger UI
# Open browser: http://localhost:8080/swagger-ui.html

# 5. Verify Flyway migrations
mvn flyway:info
```

**Expected State:**
- ✅ 31 Flyway migrations executed (V17-V31)
- ✅ All entities, repositories, services, controllers compile
- ✅ Application starts successfully
- ✅ Swagger UI shows all case management endpoints
- ✅ Database contains 130+ tribunals, 4 case types, 300+ categories, 7 statuses

---

## Frontend Implementation (Next Phase)

The frontend implementation will be covered in a separate execution batch. It will include:

1. **Angular Services** - API clients using TanStack Query
2. **Models/Interfaces** - TypeScript definitions matching backend DTOs
3. **Components** - CaseList, CaseDetail, CaseForm, etc.
4. **Routing** - Lazy-loaded routes with guards
5. **State Management** - Query caching and optimistic updates

**Note:** Frontend implementation should start AFTER backend is fully verified and tested.

---

## Plan Complete

**Summary:**
- **8 Batches** covering database, entities, DTOs, services, and controllers
- **31 Flyway migrations** for complete schema
- **17 entities** with proper relationships
- **25+ DTOs** for type-safe API
- **8 services** with business logic
- **3 controllers** with 15+ endpoints
- **Full RBAC** with 14 case management permissions

**Saved to:** `docs/plans/2026-02-07-case-dossier-management/plan.md`

Ready for execution using the `executing-plans` skill!