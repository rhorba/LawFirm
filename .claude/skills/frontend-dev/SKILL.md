---
name: frontend-dev
description: >
  Frontend development skill for UI components, state management, and client-side logic. Use when the
  user needs Angular components, CSS/Tailwind styling, responsive layouts, forms, state management,
  API integration on client side, accessibility, animations, routing, or any browser-side code.
  Trigger on: "component", "UI", "page", "layout", "CSS", "Tailwind", "Angular", "form", "modal",
  "responsive", "mobile", "accessibility", "a11y", "state management", "signal", "frontend",
  "client-side", "interceptor", "guard", "service", or visual/interface work.
---

# Frontend Developer

## Stack
**Angular 18 · Standalone Components · Signals · TanStack Query (@tanstack/angular-query-experimental) · Tailwind CSS 3 · TypeScript 5.5 (strict) · ESLint + Prettier · PNPM**

## Before Writing Code
1. Read existing patterns under `frontend/src/app/`
2. Use **standalone components** — no NgModules
3. Use **Signals** for reactive state (`signal()`, `computed()`, `effect()`)
4. Use **TanStack Query** for all server state — never manage loading/error state manually
5. **YAGNI**: don't build reusable libraries for a single use case

## Layer-Based Architecture
```
frontend/src/app/
├── core/           ← Services, guards, interceptors, models (loaded once, app-wide)
│   ├── services/   ← TokenService, AuthService, UserService (API calls)
│   ├── guards/     ← authGuard (functional guard)
│   └── interceptors/ ← authInterceptor, errorInterceptor (functional)
└── features/       ← Feature-specific components (lazy-loaded)
    ├── auth/       ← LoginComponent
    ├── dashboard/  ← DashboardComponent
    └── users/      ← UserListComponent, etc.
```

## Component Template (standalone)
```typescript
@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `...`,
})
export class ExampleComponent {
  private exampleService = inject(ExampleService);

  // Signal for local UI state
  isLoading = signal(false);

  // TanStack Query for server state
  query = injectQuery(() => ({
    queryKey: ['examples'],
    queryFn: () => this.exampleService.getAll(),
  }));
}
```

## State Management Rules
| Data Type | Solution |
|---|---|
| Server data (lists, entities) | TanStack Query (`injectQuery`, `injectMutation`) |
| Auth state (current user) | Signal in `AuthService` (`currentUser = signal(null)`) |
| UI state (open/close, hover) | Component `signal()` |
| URL state (filters, page) | Router query params |
| Form data | `ReactiveFormsModule` (`FormBuilder`, `FormGroup`) |

## Service Pattern (core/services)
```typescript
@Injectable({ providedIn: 'root' })
export class ExampleService {
  private http = inject(HttpClient);
  private apiUrl = '/api/examples';

  getAll(): Observable<Example[]> {
    return this.http.get<Example[]>(this.apiUrl);
  }

  create(data: CreateExampleRequest): Observable<Example> {
    return this.http.post<Example>(this.apiUrl, data);
  }
}
```

## Functional Guard (core/guards)
```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isAuthenticated()) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
```

## Functional Interceptor (core/interceptors)
```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(TokenService).getAccessToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

## Tailwind CSS Rules
- Mobile-first: `class="flex flex-col md:flex-row"`
- No inline styles — Tailwind classes only
- Consistent spacing: use Tailwind scale (p-4, m-2, gap-3, etc.)
- Breakpoints: `sm:640px md:768px lg:1024px xl:1280px`

## TypeScript Rules
- Strict mode enabled — no `any`, explicit return types on services
- Shared models in `core/models/` or `core/services/` — match backend DTOs exactly
- Use `interface` for API shapes, `type` for unions/computed

## Accessibility Checklist
- [ ] Semantic HTML (`button` not `div (click)`)
- [ ] Keyboard navigable (Tab, Enter, Escape)
- [ ] Form labels linked to inputs (`[for]`, `aria-label`)
- [ ] Color contrast ≥ 4.5:1 for text
- [ ] Focus management for modals/dialogs

## Routing (lazy-loaded)
```typescript
export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login.component') },
  { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component'),
    canActivate: [authGuard] },
];
```

## Verification Commands
```bash
# Frontend (run in /frontend)
pnpm dev          # Start dev server → http://localhost:4200
pnpm lint         # ESLint + Prettier check
pnpm lint:fix     # Auto-fix lint issues
pnpm build        # Production build (checks for TS errors)
pnpm test         # Run unit tests
```

## Handoff Points
- **← From Tech Lead**: Component specs, UI architecture
- **← From UX Designer**: User flows, wireframes, interaction specs
- **← From UI Designer**: Design tokens, visual specs, component styles
- **← From Backend Dev**: API contracts (endpoint URLs, request/response TypeScript interfaces)
- **← From Copywriter**: Copy for UI text, button labels, error messages
- **→ Tester**: Components for UI/e2e testing
- **→ UX Designer**: Flag feasibility issues with proposed interactions
- **→ UI Designer**: Flag implementation constraints on visual designs
