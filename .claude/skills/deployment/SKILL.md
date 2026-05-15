---
name: deployment
description: >
  Deployment and release management skill. Use when the user needs to deploy code, set up environments,
  configure hosting, manage releases, rollback, blue-green/canary deployments, environment variables,
  domain/DNS setup, SSL certificates, or production readiness checks. Trigger on: "deploy", "release",
  "rollback", "staging", "production", "hosting", "Vercel", "Netlify", "AWS", "Docker deploy",
  "Kubernetes deploy", "environment", "env vars", "domain", "SSL", "CDN", "go live", or shipping code.
---

# Deployment Engineer

## Role
You manage the release process: environments, deployment strategies, rollback plans, and go-live.

**YAGNI for Deployment**: Side project → push to main and deploy. Startup → staging + production with basic rollback. Enterprise → blue-green/canary with full observability. Don't build NASA-grade deployment for a blog.

## Pre-Deployment Checklist
```markdown
## Ready to Deploy? [Feature/Version]

### Code
- [ ] All tests passing
- [ ] Code reviewed and approved
- [ ] No critical security findings
- [ ] Environment variables documented
- [ ] Database migrations tested

### Infrastructure
- [ ] Target environment healthy
- [ ] Sufficient resources (CPU, memory, disk)
- [ ] Dependencies available (DB, cache, queues)
- [ ] SSL/TLS certificates valid
- [ ] DNS configured correctly

### Rollback
- [ ] Rollback procedure documented
- [ ] Previous version tagged and available
- [ ] Database migration reversible (or backward compatible)
- [ ] Rollback tested in staging

### Monitoring
- [ ] Health check endpoint working
- [ ] Alerting configured
- [ ] Log aggregation active
- [ ] Error tracking active (Sentry/similar)
```

## Deployment Strategies
| Strategy | Risk | Downtime | Best For |
|---|---|---|---|
| **Rolling** | Low | Zero | Standard deploys |
| **Blue-Green** | Very Low | Zero | Critical services |
| **Canary** | Lowest | Zero | High-traffic, risky changes |
| **Recreate** | High | Yes | Dev/staging, breaking changes |
| **Feature Flags** | Lowest | Zero | Gradual rollout |

### Rolling Deployment (default)
```
1. Deploy to 1 instance → health check → OK?
2. Deploy to next batch → health check → OK?
3. Repeat until all instances updated
4. If any fails → stop and rollback
```

### Blue-Green
```
1. Deploy new version to "green" (inactive)
2. Run smoke tests on green
3. Switch traffic: blue → green
4. Monitor for 15 min
5. If OK → decommission blue
6. If NOT → switch back to blue
```

## Environment Management
```
local → dev → staging → production
  │       │       │          │
  └─ each has own DB, secrets, config
```

**Rules:**
- Never share secrets between environments
- Staging mirrors production config (same infra, smaller scale)
- Use environment-specific `.env` files, never commit them
- Feature flags for testing in production safely

## This Project — Deploy Commands

### Development (H2 in-memory, hot-reload)
```bash
# Full stack via Docker
docker compose -f docker-compose.dev.yml up --build
# OR run natively (Windows)
cd backend && mvn spring-boot:run          # http://localhost:8080
cd frontend && pnpm install && pnpm dev    # http://localhost:4200
```

### Production (Docker Compose + PostgreSQL)
```bash
# Set env vars first (.env file or shell exports):
# POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, JWT_SECRET, CORS_ALLOWED_ORIGINS

docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml logs -f    # watch startup
docker compose -f docker-compose.prod.yml ps         # check health

# Rollback to previous image
docker compose -f docker-compose.prod.yml down
# edit docker-compose.prod.yml to pin previous image tag
docker compose -f docker-compose.prod.yml up -d
```

### Access Points After Deploy
| Service | Dev | Production |
|---|---|---|
| Frontend | http://localhost:4200 | http://localhost |
| Backend API | http://localhost:8080/api | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console | — (prod only uses PostgreSQL) |
| Health check | http://localhost:8080/actuator/health | same |

### Pre-Deploy Database Check
```bash
# Verify Flyway migrations will apply cleanly
mvn flyway:info -Dspring.profiles.active=prod

# Run migrations only (without starting app)
mvn flyway:migrate -Dspring.profiles.active=prod
```

## Post-Deployment
1. Verify health checks pass
2. Run smoke tests
3. Monitor error rates for 15 min
4. Check logs for anomalies
5. Notify team: "Deployed [version] to [env] ✅"

## Handoff Points
- **← From Tester**: Receives green light (tests pass)
- **← From DevOps**: Receives infra/pipeline configs
- **← From DBA**: Receives migration status, DB readiness confirmation
- **→ PM**: Reports deployment status
- **→ Digital Marketer**: Signals "feature is live" for announcements
