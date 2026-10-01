import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { User, UsuarioResponse, UsuarioRequest } from '../models/user.model';
import { PaginatedResponse } from '../models/pagination.model';
import { buildParams, QueryFilters } from './http-params';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  private readonly _users = signal<User[]>([]);
  readonly users = this._users.asReadonly();

  private readonly _totalElements = signal(0);
  readonly totalElements = this._totalElements.asReadonly();

  private readonly _totalPages = signal(0);
  readonly totalPages = this._totalPages.asReadonly();

  private readonly _currentPage = signal(0);
  readonly currentPage = this._currentPage.asReadonly();

  private readonly _pageSize = signal(20);
  readonly pageSize = this._pageSize.asReadonly();

  private _filters: QueryFilters = {};

  constructor() {
    this.loadPage(0);
  }

  loadPage(page: number, size?: number, filters?: QueryFilters): void {
    const s = size ?? this._pageSize();
    if (filters !== undefined) this._filters = filters;
    this.http.get<PaginatedResponse<UsuarioResponse>>(`${environment.apiUrl}/usuarios`, {
      params: buildParams({ page, size: s, ...this._filters }),
    }).subscribe({
      next: (res) => {
        this._users.set(res.content);
        this._totalElements.set(res.totalElements);
        this._totalPages.set(res.totalPages);
        this._currentPage.set(res.currentPage);
        this._pageSize.set(s);
      },
    });
  }

  add(data: UsuarioRequest): Observable<User> {
    return this.http.post<User>(`${environment.apiUrl}/usuarios`, data).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }

  update(id: number, data: Partial<UsuarioRequest>): Observable<User> {
    const existing = this._users().find((u) => u.id === id);
    const body: UsuarioRequest = {
      nombre: data.nombre ?? existing!.nombre,
      apellido: data.apellido ?? existing!.apellido,
      email: data.email ?? existing!.email,
      password: data.password ?? '',
      rolId: data.rolId ?? existing!.rolId,
      plantelId: data.plantelId ?? existing!.plantelId,
    };

    return this.http.put<User>(`${environment.apiUrl}/usuarios/${id}`, body).pipe(
      tap((res) =>
        this._users.update((list) => list.map((u) => (u.id === id ? res : u)))
      ),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/usuarios/${id}`, {
      params: { deactivate: true },
    }).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/usuarios/${id}`);
  }

  getTeachers(): Observable<User[]> {
    return this.http.get<PaginatedResponse<UsuarioResponse>>(`${environment.apiUrl}/usuarios`, {
      params: buildParams({ rolId: 3, size: 100 }),
    }).pipe(map((res) => res.content));
  }
}
