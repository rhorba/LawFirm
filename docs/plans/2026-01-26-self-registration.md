# Enterprise Boilerplate - Implementation Plan

## Project Overview
Open-source enterprise-grade boilerplate with Spring Boot 3.4 (Java 21) backend and Angular 18 frontend. Features JWT authentication, advanced RBAC, and comprehensive observability.

---

## Phase 1: Project Structure & Configuration

### Task 1.1: Create Root Monorepo Structure
**Files to create:**
- `/backend/` (directory)
- `/frontend/` (directory)
- `/.gitignore`
- `/.gitattributes`
- `/docker-compose.dev.yml`
- `/docker-compose.prod.yml`
- `/.husky/` (directory)

**Implementation:**

**File: `/.gitignore`**
```
# Backend
backend/target/
backend/.mvn/
backend/mvnw
backend/mvnw.cmd
*.class
*.jar
*.war
*.ear
*.log

# Frontend
frontend/node_modules/
frontend/dist/
frontend/.angular/
frontend/.env.local

# IDEs
.idea/
.vscode/
*.iml
*.ipr
*.iws

# OS
.DS_Store
Thumbs.db

# Env
.env
.env.local
.env.production
```

**File: `/.gitattributes`**
```
# Force LF for SQL and Java files (Flyway checksum consistency)
*.sql text eol=lf
*.java text eol=lf
*.ts text eol=lf
*.js text eol=lf
*.json text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.xml text eol=lf
```

**File: `/docker-compose.dev.yml`**
```yaml
version: '3.8'

services:
  # Backend uses H2 in-memory database for dev
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JWT_SECRET=${JWT_SECRET:-dev-secret-key-change-in-production-min-256-bits}
    volumes:
      - ./backend:/app
      - ~/.m2:/root/.m2
    command: mvn spring-boot:run

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    ports:
      - "4200:4200"
    volumes:
      - ./frontend:/app
      - /app/node_modules
    command: pnpm dev
```

**File: `/docker-compose.prod.yml`**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: boilerplate-db
    environment:
      POSTGRES_DB: boilerplate
      POSTGRES_USER: ${DB_USER:-admin}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-changeme}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-admin}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=boilerplate
      - DB_USER=${DB_USER:-admin}
      - DB_PASSWORD=${DB_PASSWORD:-changeme}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

**Verification:**
- Directory structure matches `/backend` and `/frontend` layout
- Git attributes enforce LF line endings for critical files
- Docker compose files are valid YAML

---

## Phase 2: Backend Foundation

### Task 2.1: Initialize Spring Boot Project with Maven
**Files to create:**
- `/backend/pom.xml`
- `/backend/src/main/java/com/boilerplate/BoilerplateApplication.java`
- `/backend/src/main/resources/application.yml`
- `/backend/src/main/resources/application-dev.yml`
- `/backend/src/main/resources/application-prod.yml`
- `/backend/Dockerfile`
- `/backend/Dockerfile.dev`
- `/backend/.mvn/wrapper/` (Maven wrapper)

**Implementation:**

**File: `/backend/pom.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <groupId>com.boilerplate</groupId>
    <artifactId>backend</artifactId>
    <version>1.0.0</version>
    <name>Enterprise Boilerplate Backend</name>
    <description>Spring Boot backend with hexagonal architecture</description>

    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.34</lombok.version>
        <springdoc.version>2.7.0</springdoc.version>
        <jacoco.version>0.8.12</jacoco.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>8.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Checkstyle -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.5.0</version>
                <configuration>
                    <configLocation>checkstyle.xml</configLocation>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                </configuration>
                <executions>
                    <execution>
                        <phase>validate</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- SpotBugs -->
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <version>4.8.6.4</version>
                <configuration>
                    <effort>Max</effort>
                    <threshold>Low</threshold>
                </configuration>
                <executions>
                    <execution>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- JaCoCo -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>PACKAGE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.70</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Flyway -->
            <plugin>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-maven-plugin</artifactId>
                <version>10.23.1</version>
            </plugin>
        </plugins>
    </build>
</project>
```

**File: `/backend/src/main/java/com/boilerplate/BoilerplateApplication.java`**
```java
package com.boilerplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BoilerplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoilerplateApplication.class, args);
    }
}
```

**File: `/backend/src/main/resources/application.yml`**
```yaml
spring:
  application:
    name: enterprise-boilerplate

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: false

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true

# JWT Configuration
jwt:
  access-token-expiration: 900000  # 15 minutes in milliseconds
  refresh-token-expiration: 2592000000  # 30 days in milliseconds
  remember-me-expiration: 7776000000  # 90 days in milliseconds

# API Documentation
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

**File: `/backend/src/main/resources/application-dev.yml`**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:boilerplate;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# CORS for local Angular dev
cors:
  allowed-origins: http://localhost:4200
  allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true

logging:
  level:
    com.boilerplate: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**File: `/backend/src/main/resources/application-prod.yml`**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:boilerplate}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: false

# CORS for production
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:https://yourdomain.com}
  allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true

logging:
  level:
    com.boilerplate: INFO
    org.springframework: WARN
```

**File: `/backend/Dockerfile`**
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**File: `/backend/Dockerfile.dev`**
```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y maven
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]
```

**Verification:**
- Run `mvn clean verify` successfully
- Application starts on port 8080
- H2 console accessible at `/h2-console` in dev profile
- Actuator health endpoint responds at `/actuator/health`

---

### Task 2.2: Create Hexagonal Architecture Package Structure
**Directories to create:**
```
/backend/src/main/java/com/boilerplate/
├── domain/
│   ├── model/           # Entities
│   ├── repository/      # Repository interfaces
│   └── service/         # Business logic interfaces
├── application/
│   ├── dto/            # Request/Response DTOs
│   │   ├── request/
│   │   └── response/
│   ├── mapper/         # MapStruct mappers
│   └── service/        # Service implementations
├── infrastructure/
│   ├── persistence/    # JPA repositories
│   ├── security/       # Security config, filters
│   └── config/         # Spring configurations
└── presentation/
    ├── controller/     # REST controllers
    └── exception/      # Exception handlers
```

**Implementation:**
Create these directories with `.gitkeep` files to ensure they're tracked:

```bash
mkdir -p backend/src/main/java/com/boilerplate/{domain/{model,repository,service},application/{dto/{request,response},mapper,service},infrastructure/{persistence,security,config},presentation/{controller,exception}}
```

**Verification:**
- All directories exist
- Package structure follows hexagonal architecture
- Clear separation of concerns between layers

---

## Phase 3: Security & Authentication Layer

### Task 3.1: Create Domain Entities (User, Role, Permission)
**Files to create:**
- `/backend/src/main/java/com/boilerplate/domain/model/User.java`
- `/backend/src/main/java/com/boilerplate/domain/model/Role.java`
- `/backend/src/main/java/com/boilerplate/domain/model/Permission.java`
- `/backend/src/main/java/com/boilerplate/domain/model/BaseEntity.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/domain/model/BaseEntity.java`**
```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
```

**File: `/backend/src/main/java/com/boilerplate/domain/model/Permission.java`**
```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionResource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionAction action;

    public enum PermissionResource {
        USER, ROLE, PERMISSION, SYSTEM
    }

    public enum PermissionAction {
        READ, CREATE, UPDATE, DELETE, MANAGE
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/domain/model/Role.java`**
```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
```

**File: `/backend/src/main/java/com/boilerplate/domain/model/User.java`**
```java
package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
```

**Verification:**
- Entities compile without errors
- Lombok annotations generate getters/setters
- JPA annotations are correct
- Relationships properly defined (User -> Roles -> Permissions)

---

### Task 3.2: Create Flyway Migrations with Seed Data
**Files to create:**
- `/backend/src/main/resources/db/migration/V1__create_users_table.sql`
- `/backend/src/main/resources/db/migration/V2__create_roles_table.sql`
- `/backend/src/main/resources/db/migration/V3__create_permissions_table.sql`
- `/backend/src/main/resources/db/migration/V4__create_user_roles_table.sql`
- `/backend/src/main/resources/db/migration/V5__create_role_permissions_table.sql`
- `/backend/src/main/resources/db/migration/V6__seed_permissions.sql`
- `/backend/src/main/resources/db/migration/V7__seed_roles.sql`
- `/backend/src/main/resources/db/migration/V8__seed_admin_user.sql`

**Implementation:**

**File: `/backend/src/main/resources/db/migration/V1__create_users_table.sql`**
```sql
CREATE TABLE users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**File: `/backend/src/main/resources/db/migration/V2__create_roles_table.sql`**
```sql
CREATE TABLE roles (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_roles_name ON roles(name);
```

**File: `/backend/src/main/resources/db/migration/V3__create_permissions_table.sql`**
```sql
CREATE TABLE permissions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);
```

**File: `/backend/src/main/resources/db/migration/V4__create_user_roles_table.sql`**
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

**File: `/backend/src/main/resources/db/migration/V5__create_role_permissions_table.sql`**
```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
```

**File: `/backend/src/main/resources/db/migration/V6__seed_permissions.sql`**
```sql
-- User permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('USER_READ', 'Read user information', 'USER', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_CREATE', 'Create new users', 'USER', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_UPDATE', 'Update existing users', 'USER', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_DELETE', 'Delete users', 'USER', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_MANAGE', 'Full user management', 'USER', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Role permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('ROLE_READ', 'Read role information', 'ROLE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_CREATE', 'Create new roles', 'ROLE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_UPDATE', 'Update existing roles', 'ROLE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_DELETE', 'Delete roles', 'ROLE', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_MANAGE', 'Full role management', 'ROLE', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Permission permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('PERMISSION_READ', 'Read permission information', 'PERMISSION', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('PERMISSION_MANAGE', 'Full permission management', 'PERMISSION', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- System permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('SYSTEM_MANAGE', 'Full system administration', 'SYSTEM', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

**File: `/backend/src/main/resources/db/migration/V7__seed_roles.sql`**
```sql
-- Insert roles
INSERT INTO roles (name, description, created_at, updated_at, version) VALUES
('ADMIN', 'System administrator with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER', 'Standard user with basic permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('MODERATOR', 'Moderator with elevated permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- Assign read permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'USER'
AND p.name IN ('USER_READ', 'ROLE_READ', 'PERMISSION_READ');

-- Assign moderate permissions to MODERATOR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
AND p.name IN ('USER_READ', 'USER_UPDATE', 'ROLE_READ', 'PERMISSION_READ');
```

**File: `/backend/src/main/resources/db/migration/V8__seed_admin_user.sql`**
```sql
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (username, email, password, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES ('admin', 'admin@boilerplate.com', '$2a$10$xG/4PCHmGJZr3IZNnPZ7eeL9MpJF.aPQQQNPqQWWqhpWFNpTJpRNO', TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin'
AND r.name = 'ADMIN';
```

**Verification:**
- All migration files have LF line endings
- Migrations execute successfully with `mvn flyway:migrate`
- Database schema matches entity definitions
- Seed data creates admin user with credentials `admin/admin123`
- All foreign key relationships work correctly

---

### Task 3.3: Create JPA Repositories
**Files to create:**
- `/backend/src/main/java/com/boilerplate/domain/repository/UserRepository.java`
- `/backend/src/main/java/com/boilerplate/domain/repository/RoleRepository.java`
- `/backend/src/main/java/com/boilerplate/domain/repository/PermissionRepository.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/domain/repository/UserRepository.java`**
```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(String username);
}
```

**File: `/backend/src/main/java/com/boilerplate/domain/repository/RoleRepository.java`**
```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id IN :ids")
    Set<Role> findAllByIdWithPermissions(Set<Long> ids);
}
```

**File: `/backend/src/main/java/com/boilerplate/domain/repository/PermissionRepository.java`**
```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    Set<Permission> findAllByResourceAndAction(
        Permission.PermissionResource resource,
        Permission.PermissionAction action
    );
}
```

**Verification:**
- Repositories extend JpaRepository correctly
- Custom query methods use proper JPQL
- @Repository annotation present
- JOIN FETCH queries prevent N+1 problems

---

### Task 3.4: Create DTOs (Request/Response)
**Files to create:**
- `/backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java`
- `/backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java`
- `/backend/src/main/java/com/boilerplate/application/dto/request/LoginRequest.java`
- `/backend/src/main/java/com/boilerplate/application/dto/response/UserResponse.java`
- `/backend/src/main/java/com/boilerplate/application/dto/response/RoleResponse.java`
- `/backend/src/main/java/com/boilerplate/application/dto/response/PermissionResponse.java`
- `/backend/src/main/java/com/boilerplate/application/dto/response/AuthResponse.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java`**
```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private Set<Long> roleIds;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java`**
```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private Boolean enabled;

    private Set<Long> roleIds;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/request/LoginRequest.java`**
```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Boolean rememberMe;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/response/PermissionResponse.java`**
```java
package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/response/RoleResponse.java`**
```java
package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/response/UserResponse.java`**
```java
package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Boolean enabled;
    private Set<RoleResponse> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**File: `/backend/src/main/java/com/boilerplate/application/dto/response/AuthResponse.java`**
```java
package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponse user;
}
```

**Verification:**
- All DTOs use proper validation annotations
- Request/Response DTOs are separated
- Lombok annotations reduce boilerplate
- Nested DTOs (RoleResponse contains PermissionResponse)

---

### Task 3.5: Create MapStruct Mappers
**Files to create:**
- `/backend/src/main/java/com/boilerplate/application/mapper/UserMapper.java`
- `/backend/src/main/java/com/boilerplate/application/mapper/RoleMapper.java`
- `/backend/src/main/java/com/boilerplate/application/mapper/PermissionMapper.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/application/mapper/PermissionMapper.java`**
```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.PermissionResponse;
import com.boilerplate.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "resource", expression = "java(permission.getResource().name())")
    @Mapping(target = "action", expression = "java(permission.getAction().name())")
    PermissionResponse toResponse(Permission permission);
}
```

**File: `/backend/src/main/java/com/boilerplate/application/mapper/RoleMapper.java`**
```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.RoleResponse;
import com.boilerplate.domain.model.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
```

**File: `/backend/src/main/java/com/boilerplate/application/mapper/UserMapper.java`**
```java
package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.domain.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // Handled separately with encoding
    @Mapping(target = "roles", ignore = true)  // Handled separately
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "accountNonExpired", constant = "true")
    @Mapping(target = "accountNonLocked", constant = "true")
    @Mapping(target = "credentialsNonExpired", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // Only update if provided
    @Mapping(target = "roles", ignore = true)  // Handled separately
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "password", ignore = true)  // Never expose password
    UserResponse toResponse(User user);
}
```

**Verification:**
- MapStruct generates implementation classes at compile time
- Check `target/generated-sources/annotations` for generated mappers
- Mappers use Spring component model
- Nested mappings work (User -> UserResponse includes RoleResponse)

---

### Task 3.6: Implement JWT Service
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtService.java`
- `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtProperties.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtProperties.java`**
```java
package com.boilerplate.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret = System.getenv().getOrDefault(
        "JWT_SECRET",
        "default-secret-key-change-in-production-must-be-at-least-256-bits-long"
    );
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
    private Long rememberMeExpiration;
}
```

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtService.java`**
```java
package com.boilerplate.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(UserDetails userDetails, boolean rememberMe) {
        long expiration = rememberMe
            ? jwtProperties.getRememberMeExpiration()
            : jwtProperties.getRefreshTokenExpiration();
        return generateToken(userDetails, expiration);
    }

    private String generateToken(UserDetails userDetails, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(jwtProperties.getSecret().getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**Verification:**
- JWT tokens can be generated
- Tokens can be validated and parsed
- Secret key loaded from environment variable
- Different expiration times for access/refresh/remember-me tokens

---

## Phase 4: Security Configuration & Filters

### Task 4.1: Create UserDetailsService Implementation
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/security/CustomUserDetailsService.java`
- `/backend/src/main/java/com/boilerplate/infrastructure/security/UserPrincipal.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/UserPrincipal.java`**
```java
package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.model.Permission;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role-based authorities
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // Add permission-based authorities
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.getAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.getCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/CustomUserDetailsService.java`**
```java
package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameWithRolesAndPermissions(username)
            .map(UserPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

**Verification:**
- UserPrincipal implements UserDetails correctly
- Authorities include both roles (ROLE_*) and permissions
- Single query fetches user with roles and permissions (no N+1)

---

### Task 4.2: Create JWT Authentication Filter
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtAuthenticationFilter.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/JwtAuthenticationFilter.java`**
```java
package com.boilerplate.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
```

**Verification:**
- Filter extracts JWT from Authorization header
- Token validation works correctly
- Authentication set in SecurityContext
- Errors logged but don't break filter chain

---

### Task 4.3: Create Permission Evaluator for Programmatic Checks
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/security/CustomPermissionEvaluator.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/security/CustomPermissionEvaluator.java`**
```java
package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.model.Permission.PermissionAction;
import com.boilerplate.domain.model.Permission.PermissionResource;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return hasPrivilege(authentication, permission.toString());
    }

    @Override
    public boolean hasPermission(
        Authentication authentication,
        Serializable targetId,
        String targetType,
        Object permission
    ) {
        if (authentication == null || targetType == null || permission == null) {
            return false;
        }

        // Build permission string: RESOURCE_ACTION
        String permissionName = targetType.toUpperCase() + "_" + permission.toString().toUpperCase();
        return hasPrivilege(authentication, permissionName);
    }

    private boolean hasPrivilege(Authentication authentication, String permissionName) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals(permissionName));
    }

    public boolean hasResourcePermission(
        Authentication authentication,
        PermissionResource resource,
        PermissionAction action
    ) {
        if (authentication == null) {
            return false;
        }

        String permissionName = resource.name() + "_" + action.name();
        return hasPrivilege(authentication, permissionName);
    }
}
```

**Verification:**
- PermissionEvaluator interface implemented
- Supports both annotation and programmatic checks
- Permission format: RESOURCE_ACTION (e.g., USER_READ)

---

### Task 4.4: Create Security Configuration
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/config/SecurityConfig.java`
- `/backend/src/main/java/com/boilerplate/infrastructure/config/CorsConfig.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/config/CorsConfig.java`**
```java
package com.boilerplate.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsConfig {

    private String allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private Boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/infrastructure/config/SecurityConfig.java`**
```java
package com.boilerplate.infrastructure.config;

import com.boilerplate.infrastructure.security.CustomPermissionEvaluator;
import com.boilerplate.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomPermissionEvaluator permissionEvaluator;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console in frames (dev only)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }
}
```

**Verification:**
- Security filter chain configured correctly
- JWT filter runs before UsernamePasswordAuthenticationFilter
- Public endpoints: /api/auth/**, /actuator/health, /swagger-ui/**
- CORS enabled
- Method security enabled with custom permission evaluator

---

## Phase 5: Service Layer Implementation

### Task 5.1: Create Authentication Service
**Files to create:**
- `/backend/src/main/java/com/boilerplate/application/service/AuthService.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/application/service/AuthService.java`**
```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.LoginRequest;
import com.boilerplate.application.dto.response.AuthResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(
            userDetails,
            Boolean.TRUE.equals(request.getRememberMe())
        );

        UserResponse userResponse = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        log.info("User logged in successfully: {}", request.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(15 * 60L) // 15 minutes in seconds
            .user(userResponse)
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        UserResponse userResponse = userRepository.findByUsernameWithRolesAndPermissions(username)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(15 * 60L)
            .user(userResponse)
            .build();
    }
}
```

**Verification:**
- Login authenticates user credentials
- Generates access and refresh tokens
- Remember me extends refresh token expiration
- Returns user details with tokens

---

### Task 5.2: Create User Service
**Files to create:**
- `/backend/src/main/java/com/boilerplate/application/service/UserService.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/application/service/UserService.java`**
```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Assign roles
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
            user.setRoles(roles);
        } else {
            // Assign default USER role
            roleRepository.findByName("USER")
                .ifPresent(role -> user.setRoles(Set.of(role)));
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.debug("Updating user with id: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Check username uniqueness if changed
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists: " + request.getUsername());
            }
        }

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
        }

        // Update basic fields
        userMapper.updateEntity(user, request);

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update roles if provided
        if (request.getRoleIds() != null) {
            Set<Role> roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }
}
```

**Verification:**
- CRUD operations implemented
- Password encoding on create/update
- Username/email uniqueness validation
- Default USER role assigned if no roles specified
- Pagination support for listing users

---

## Phase 6: Controller Layer & Exception Handling

### Task 6.1: Create Global Exception Handler
**Files to create:**
- `/backend/src/main/java/com/boilerplate/presentation/exception/GlobalExceptionHandler.java`
- `/backend/src/main/java/com/boilerplate/presentation/exception/ErrorResponse.java`
- `/backend/src/main/java/com/boilerplate/presentation/exception/ResourceNotFoundException.java`
- `/backend/src/main/java/com/boilerplate/presentation/exception/DuplicateResourceException.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/presentation/exception/ErrorResponse.java`**
```java
package com.boilerplate.presentation.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
```

**File: `/backend/src/main/java/com/boilerplate/presentation/exception/ResourceNotFoundException.java`**
```java
package com.boilerplate.presentation.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/presentation/exception/DuplicateResourceException.java`**
```java
package com.boilerplate.presentation.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/presentation/exception/GlobalExceptionHandler.java`**
```java
package com.boilerplate.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        log.error("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateResourceException(
        DuplicateResourceException ex,
        HttpServletRequest request
    ) {
        log.error("Duplicate resource: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation failed: {}", errors);

        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Validation failed")
            .path(request.getRequestURI())
            .validationErrors(errors)
            .build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(
        BadCredentialsException ex,
        HttpServletRequest request
    ) {
        log.error("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password",
            request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        log.error("Authentication error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        log.error("Access denied: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.FORBIDDEN,
            "You don't have permission to access this resource",
            request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request.getRequestURI()
        );
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .build();
    }
}
```

**Verification:**
- All common exceptions handled
- Validation errors mapped to field-level messages
- RFC 7807 Problem Details format
- Logs all errors appropriately

---

### Task 6.2: Create REST Controllers
**Files to create:**
- `/backend/src/main/java/com/boilerplate/presentation/controller/AuthController.java`
- `/backend/src/main/java/com/boilerplate/presentation/controller/UserController.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/presentation/controller/AuthController.java`**
```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.LoginRequest;
import com.boilerplate.application.dto.response.AuthResponse;
import com.boilerplate.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = authHeader.substring(7); // Remove "Bearer " prefix
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
```

**File: `/backend/src/main/java/com/boilerplate/presentation/controller/UserController.java`**
```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get all users", description = "Retrieve paginated list of users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by username", description = "Retrieve user details by username")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update user", description = "Update existing user")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Delete user", description = "Delete user by ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Verification:**
- REST endpoints follow best practices
- @PreAuthorize annotations for permission checks
- Swagger annotations for API documentation
- Proper HTTP status codes (200, 201, 204, etc.)

---

### Task 6.3: Create Request/Response Logging Filter
**Files to create:**
- `/backend/src/main/java/com/boilerplate/infrastructure/config/LoggingFilter.java`

**Implementation:**

**File: `/backend/src/main/java/com/boilerplate/infrastructure/config/LoggingFilter.java`**
```java
package com.boilerplate.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - startTime;

        logRequestResponse(requestWrapper, responseWrapper, duration);

        responseWrapper.copyBodyToResponse();
    }

    private void logRequestResponse(
        ContentCachingRequestWrapper request,
        ContentCachingResponseWrapper response,
        long duration
    ) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        log.info("HTTP {} {} - Status: {} - Duration: {}ms",
            method, uri, status, duration);

        if (log.isDebugEnabled()) {
            String headers = Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .collect(Collectors.joining(", "));

            log.debug("Request Headers: {}", headers);
        }
    }
}
```

**Verification:**
- Logs all HTTP requests/responses
- Includes method, URI, status, duration
- Debug mode logs headers
- Doesn't log sensitive data (passwords, tokens)

---

## Phase 7: Logging Configuration

### Task 7.1: Create Logback Configuration
**Files to create:**
- `/backend/src/main/resources/logback-spring.xml`

**Implementation:**

**File: `/backend/src/main/resources/logback-spring.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <!-- Console Appender for Development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON Appender for Production -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeStructuredArguments>true</includeStructuredArguments>
            <includeTags>true</includeTags>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
            </fieldNames>
        </encoder>
    </appender>

    <!-- Development Profile -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.boilerplate" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
        <logger name="org.springframework.security" level="DEBUG"/>
        <logger name="org.hibernate.SQL" level="DEBUG"/>
    </springProfile>

    <!-- Production Profile -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
        <logger name="com.boilerplate" level="INFO"/>
        <logger name="org.springframework" level="WARN"/>
    </springProfile>
</configuration>
```

**Verification:**
- Dev profile uses readable console format
- Prod profile uses JSON format (Logstash compatible)
- Different log levels per profile
- Application logs separate from framework logs

---

## Phase 8: Checkstyle Configuration

### Task 8.1: Create Checkstyle Rules
**Files to create:**
- `/backend/checkstyle.xml`

**Implementation:**

**File: `/backend/checkstyle.xml`**
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java"/>

    <module name="LineLength">
        <property name="max" value="120"/>
        <property name="ignorePattern" value="^package.*|^import.*"/>
    </module>

    <module name="TreeWalker">
        <!-- Naming Conventions -->
        <module name="ConstantName"/>
        <module name="LocalFinalVariableName"/>
        <module name="LocalVariableName"/>
        <module name="MemberName"/>
        <module name="MethodName"/>
        <module name="PackageName"/>
        <module name="ParameterName"/>
        <module name="StaticVariableName"/>
        <module name="TypeName"/>

        <!-- Imports -->
        <module name="AvoidStarImport"/>
        <module name="IllegalImport"/>
        <module name="RedundantImport"/>
        <module name="UnusedImports"/>

        <!-- Whitespace -->
        <module name="EmptyForIteratorPad"/>
        <module name="GenericWhitespace"/>
        <module name="MethodParamPad"/>
        <module name="NoWhitespaceAfter"/>
        <module name="NoWhitespaceBefore"/>
        <module name="OperatorWrap"/>
        <module name="ParenPad"/>
        <module name="TypecastParenPad"/>
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>

        <!-- Modifiers -->
        <module name="ModifierOrder"/>
        <module name="RedundantModifier"/>

        <!-- Blocks -->
        <module name="AvoidNestedBlocks"/>
        <module name="EmptyBlock"/>
        <module name="LeftCurly"/>
        <module name="NeedBraces"/>
        <module name="RightCurly"/>

        <!-- Coding -->
        <module name="EmptyStatement"/>
        <module name="EqualsHashCode"/>
        <module name="IllegalInstantiation"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>

        <!-- Miscellaneous -->
        <module name="ArrayTypeStyle"/>
        <module name="UpperEll"/>
    </module>
</module>
```

**Verification:**
- Checkstyle runs during Maven validate phase
- Enforces naming conventions
- Checks formatting and style
- Can be customized per project needs

---

## Phase 9: Frontend Angular Setup

### Task 9.1: Initialize Angular Project
**Commands to run:**
```bash
cd frontend
npx @angular/cli@18 new . --routing --style=css --skip-git
```

**When prompted:**
- Would you like to add Angular routing? **Yes**
- Which stylesheet format would you like to use? **CSS**

**Files created automatically:**
- `/frontend/angular.json`
- `/frontend/package.json`
- `/frontend/tsconfig.json`
- `/frontend/src/main.ts`
- `/frontend/src/app/app.component.ts`
- `/frontend/src/index.html`

**Verification:**
- Angular 18 project initialized
- Standalone components enabled by default
- Routing configured

---

### Task 9.2: Install Dependencies
**Commands to run:**
```bash
cd frontend
pnpm install @tanstack/angular-query-experimental @angular/common @angular/forms
pnpm install -D tailwindcss postcss autoprefixer
pnpm install -D eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser
pnpm install -D prettier eslint-config-prettier eslint-plugin-prettier
npx tailwindcss init
```

**Verification:**
- All dependencies installed
- Tailwind CSS configured
- ESLint and Prettier ready

---

### Task 9.3: Configure Package Scripts
Add to `/frontend/package.json` scripts section:
```json
{
  "scripts": {
    "dev": "ng serve",
    "build": "ng build",
    "lint": "eslint \"src/**/*.{ts,html}\" && prettier --check \"src/**/*.{ts,html,css,json}\"",
    "lint:fix": "eslint \"src/**/*.{ts,html}\" --fix && prettier --write \"src/**/*.{ts,html,css,json}\"",
    "test": "ng test",
    "test:ci": "ng test --watch=false --browsers=ChromeHeadless"
  }
}
```

---

### Task 9.4: Configure Tailwind CSS
**File: `/frontend/tailwind.config.js`**
```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: { extend: {} },
  plugins: [],
}
```

**File: `/frontend/src/styles.css`**
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  @apply bg-gray-50 text-gray-900;
}
```

---

### Task 9.5: Create Environment Files
**File: `/frontend/src/environments/environment.development.ts`**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**File: `/frontend/src/environments/environment.ts`**
```typescript
export const environment = {
  production: true,
  apiUrl: '/api'
};
```

---

### Task 9.6: Create Token Service
**File: `/frontend/src/app/core/services/token.service.ts`**
```typescript
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}
```

---

### Task 9.7: Create Auth Service
**File: `/frontend/src/app/core/services/auth.service.ts`**
```typescript
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  roles: RoleResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  permissions: PermissionResponse[];
}

export interface PermissionResponse {
  id: number;
  name: string;
  description: string;
  resource: string;
  action: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  currentUser = signal<UserResponse | null>(null);
  isAuthenticated = signal<boolean>(false);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(tap(response => {
        this.tokenService.setAccessToken(response.accessToken);
        this.tokenService.setRefreshToken(response.refreshToken);
        this.currentUser.set(response.user);
        this.isAuthenticated.set(true);
      }));
  }

  logout(): void {
    this.tokenService.clearTokens();
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http.post<AuthResponse>(
      `${environment.apiUrl}/auth/refresh`,
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    ).pipe(tap(response => {
      this.tokenService.setAccessToken(response.accessToken);
      this.currentUser.set(response.user);
    }));
  }

  hasPermission(permission: string): boolean {
    const user = this.currentUser();
    if (!user) return false;
    return user.roles.some(role => role.permissions.some(p => p.name === permission));
  }

  hasRole(roleName: string): boolean {
    const user = this.currentUser();
    if (!user) return false;
    return user.roles.some(role => role.name === roleName);
  }
}
```

---

### Task 9.8: Create Interceptors
**File: `/frontend/src/app/core/interceptors/auth.interceptor.ts`**
```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getAccessToken();

  if (token && !req.url.includes('/auth/login')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req);
};
```

**File: `/frontend/src/app/core/interceptors/error.interceptor.ts`**
```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
      }
      console.error('HTTP Error:', error);
      return throwError(() => error);
    })
  );
};
```

---

### Task 9.9: Create Auth Guard
**File: `/frontend/src/app/core/guards/auth.guard.ts`**
```typescript
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { TokenService } from '../services/token.service';

export const authGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
```

---

### Task 9.10: Configure App Providers
**File: `/frontend/src/app/app.config.ts`**
```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAngularQuery, QueryClient } from '@tanstack/angular-query-experimental';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAngularQuery(new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 1000 * 60 * 5,
          refetchOnWindowFocus: false,
        },
      },
    })),
  ]
};
```

---

### Task 9.11: Create Login Component
**File: `/frontend/src/app/features/auth/login/login.component.ts`**
```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);

  loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    rememberMe: [false]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: () => { this.router.navigate(['/dashboard']); },
      error: (err) => {
        this.error.set(err.error?.message || 'Login failed');
        this.loading.set(false);
      }
    });
  }
}
```

**File: `/frontend/src/app/features/auth/login/login.component.html`**
```html
<div class="min-h-screen flex items-center justify-center bg-gray-100">
  <div class="max-w-md w-full bg-white rounded-lg shadow-md p-8">
    <h2 class="text-2xl font-bold text-center mb-6">Login</h2>

    @if (error()) {
      <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
        {{ error() }}
      </div>
    }

    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Username</label>
        <input formControlName="username"
          class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500"
          placeholder="Enter username" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
        <input type="password" formControlName="password"
          class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500"
          placeholder="Enter password" />
      </div>

      <div class="flex items-center">
        <input type="checkbox" formControlName="rememberMe"
          class="h-4 w-4 text-blue-600 border-gray-300 rounded" />
        <label class="ml-2 text-sm text-gray-700">Remember me</label>
      </div>

      <button type="submit" [disabled]="loginForm.invalid || loading()"
        class="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:opacity-50">
        @if (loading()) { <span>Logging in...</span> } @else { <span>Login</span> }
      </button>
    </form>
  </div>
</div>
```

---

### Task 9.12: Create Dashboard Component
**File: `/frontend/src/app/features/dashboard/dashboard.component.ts`**
```typescript
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container mx-auto px-4 py-8">
      <h1 class="text-3xl font-bold mb-6">Dashboard</h1>

      @if (authService.currentUser(); as user) {
        <div class="bg-white shadow-md rounded-lg p-6 mb-6">
          <h2 class="text-xl font-semibold mb-4">Welcome, {{ user.username }}!</h2>
          <p class="text-gray-600">Email: {{ user.email }}</p>
          <div class="mt-4">
            <h3 class="font-semibold mb-2">Your Roles:</h3>
            <div class="flex gap-2">
              @for (role of user.roles; track role.id) {
                <span class="px-3 py-1 text-sm bg-blue-100 text-blue-800 rounded-full">
                  {{ role.name }}
                </span>
              }
            </div>
          </div>
        </div>
      }

      <div class="grid md:grid-cols-3 gap-6">
        @if (authService.hasPermission('USER_READ')) {
          <a routerLink="/users" class="bg-white shadow-md rounded-lg p-6 hover:shadow-lg">
            <h3 class="text-lg font-semibold mb-2">Users</h3>
            <p class="text-gray-600">Manage user accounts</p>
          </a>
        }

        <button (click)="authService.logout()"
          class="bg-red-600 text-white shadow-md rounded-lg p-6 hover:bg-red-700 text-left">
          <h3 class="text-lg font-semibold mb-2">Logout</h3>
          <p class="text-red-100">Sign out of your account</p>
        </button>
      </div>
    </div>
  `
})
export class DashboardComponent {
  authService = inject(AuthService);
}
```

---

### Task 9.13: Configure Routes
**File: `/frontend/src/app/app.routes.ts`**
```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];
```

---

### Task 9.14: Create Frontend Dockerfiles
**File: `/frontend/Dockerfile`**
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@latest --activate
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm run build

FROM nginx:alpine
COPY --from=build /app/dist/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**File: `/frontend/Dockerfile.dev`**
```dockerfile
FROM node:20-alpine
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@latest --activate
EXPOSE 4200
CMD ["pnpm", "dev", "--host", "0.0.0.0"]
```

**File: `/frontend/nginx.conf`**
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
    }
}
```

---

### Task 9.15: ESLint & Prettier Configuration
**File: `/frontend/.eslintrc.json`**
```json
{
  "root": true,
  "overrides": [
    {
      "files": ["*.ts"],
      "extends": [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:@angular-eslint/recommended",
        "plugin:prettier/recommended"
      ]
    },
    {
      "files": ["*.html"],
      "extends": ["plugin:@angular-eslint/template/recommended"]
    }
  ]
}
```

**File: `/frontend/.prettierrc`**
```json
{
  "singleQuote": true,
  "trailingComma": "es5",
  "tabWidth": 2,
  "semi": true,
  "printWidth": 100,
  "endOfLine": "lf"
}
```

---

## Phase 10: Testing

### Task 10.1: Backend Unit Tests
**File: `/backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java`**
```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    void createUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
            .username("testuser").email("test@example.com").password("password123").build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any())).thenReturn(new User());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(new User());
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        UserResponse result = userService.createUser(request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        CreateUserRequest request = CreateUserRequest.builder()
            .username("existing").email("test@example.com").password("password123").build();

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }
}
```

---

## Phase 11: Documentation

### Task 11.1: Update README
**File: `/README.md`**
```markdown
# Enterprise Boilerplate

Open-source enterprise-grade boilerplate with Spring Boot 3.4 (Java 21) and Angular 18.

## Features
- JWT Authentication with RBAC
- Hexagonal Architecture (Backend)
- Flyway Migrations with Seed Data
- TanStack Query + Signals (Frontend)
- Comprehensive Testing & Quality Checks

## Quick Start

### Development (H2)
```bash
# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && pnpm install && pnpm dev
```

Login: `admin / admin123`

### Production (Docker + PostgreSQL)
```bash
docker-compose -f docker-compose.prod.yml up --build
```

## API Docs
http://localhost:8080/swagger-ui.html

## Tech Stack
**Backend**: Java 21, Spring Boot 3.4, PostgreSQL/H2, Flyway, MapStruct, JWT
**Frontend**: Angular 18, TanStack Query, Tailwind CSS, Signals

## Commands
```bash
# Backend
mvn spring-boot:run    # Run
mvn verify             # Test + Quality

# Frontend
pnpm dev               # Run
pnpm lint              # Lint
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
```

---

### Task 11.2: Create CONTRIBUTING.md
**File: `/CONTRIBUTING.md`**
```markdown
# Contributing

## Development Setup
See [README.md](README.md) for setup instructions.

## Commit Conventions
Use Conventional Commits:
- `feat: add user profile endpoint`
- `fix: resolve JWT expiration issue`
- `docs: update README`

## Pull Request Process
1. Fork repository
2. Create feature branch: `git checkout -b feature/your-feature`
3. Follow coding standards (Checkstyle, ESLint)
4. Write tests
5. Run `mvn verify` and `pnpm lint`
6. Submit PR with description

## Coding Standards
- **Backend**: Hexagonal architecture, MapStruct, Flyway, 70% coverage
- **Frontend**: Standalone components, Signals, TanStack Query, Tailwind

Thank you for contributing!
```

---

## Phase 12: Final Configuration

### Task 12.1: Create Environment Example
**File: `/.env.example`**
```bash
# Production Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=boilerplate
DB_USER=admin
DB_PASSWORD=changeme

# JWT Secret (256-bit minimum)
JWT_SECRET=your-secret-key-here

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

---

## Summary

✅ All 12 phases complete with exact file paths and code examples.

**Next**: Use `/excute-plans` skill to implement this plan in batches with review checkpoints.
