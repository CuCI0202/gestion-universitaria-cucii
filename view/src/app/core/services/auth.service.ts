import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_NOTIFICATION } from '../interceptors/error.interceptor';
import {
  LoginRequest,
  LoginResponse,
  MeResponse,
  AuthUser,
  UserRole,
  mapRolId,
} from '../models/auth.model';
import { setTokenCookie, getTokenCookie, removeTokenCookie, setRoleCookie, getRoleCookie, removeRoleCookie } from './cookie-utils';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly currentUser = signal<AuthUser | null>(null);
  private readonly _token = signal<string | null>(null);
  private readonly _cachedRole = signal<UserRole | null>(null);

  constructor() {
    const token = getTokenCookie();
    if (token) {
      this._token.set(token);
      const rolId = getRoleCookie();
      if (rolId !== null) {
        this._cachedRole.set(mapRolId(rolId));
      }
      setTimeout(() => this.validateSession(), 0);
    }
  }

  private validateSession(): void {
    this.http.get<MeResponse>(`${environment.apiUrl}/auth/me`).subscribe({
      next: (me) => {
        const rol = mapRolId(me.rolId);
        this.currentUser.set({
          id: me.id,
          nombre: me.nombre,
          apellido: me.apellido,
          email: me.email,
          rolId: me.rolId,
          rol,
        });
        this._cachedRole.set(rol);
        setRoleCookie(me.rolId);
      },
      error: () => this.logout(),
    });
  }

  login(email: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { email, password };

    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, body, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    }).pipe(
      tap((res) => {
        const rol = mapRolId(res.rolId);
        this._token.set(res.token);
        setTokenCookie(res.token);
        this.currentUser.set({
          id: res.id,
          nombre: res.nombre,
          apellido: res.apellido,
          email: res.email,
          rolId: res.rolId,
          rol,
        });
        this._cachedRole.set(rol);
        setRoleCookie(res.rolId);
      })
    );
  }

  logout(): void {
    this._token.set(null);
    this.currentUser.set(null);
    this._cachedRole.set(null);
    removeTokenCookie();
    removeRoleCookie();
  }

  getToken(): string | null {
    return this._token();
  }

  isAuthenticated(): boolean {
    return this._token() !== null;
  }

  getRole(): string | null {
    return this.currentUser()?.rol ?? null;
  }

  isAdmin(): boolean {
    const role = this.currentUser()?.rol ?? this._cachedRole();
    return role === 'admin' || role === 'rector';
  }
}
