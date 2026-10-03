import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Campus } from '../models/campus.model';
import { PaginatedResponse } from '../models/pagination.model';
import { buildParams, QueryFilters } from './http-params';

@Injectable({ providedIn: 'root' })
export class CampusesService {
  private readonly http = inject(HttpClient);

  private readonly _campuses = signal<Campus[]>([]);
  readonly campuses = this._campuses.asReadonly();

  private readonly _totalElements = signal(0);
  readonly totalElements = this._totalElements.asReadonly();

  private readonly _totalPages = signal(0);
  readonly totalPages = this._totalPages.asReadonly();

  private readonly _currentPage = signal(0);
  readonly currentPage = this._currentPage.asReadonly();

  private readonly _pageSize = signal(20);
  readonly pageSize = this._pageSize.asReadonly();

  private _filters: QueryFilters = { isActive: true };

  constructor() {
    this.loadPage(0);
  }

  loadPage(page: number, size?: number, filters?: QueryFilters): void {
    const s = size ?? this._pageSize();
    if (filters !== undefined) this._filters = filters;
    this.http.get<PaginatedResponse<Campus>>(`${environment.apiUrl}/planteles`, {
      params: buildParams({ page, size: s, ...this._filters }),
    }).subscribe({
      next: (res) => {
        this._campuses.set(res.content);
        this._totalElements.set(res.totalElements);
        this._totalPages.set(res.totalPages);
        this._currentPage.set(res.currentPage);
        this._pageSize.set(s);
      },
    });
  }

  getAll(): Observable<Campus[]> {
    return this.http.get<PaginatedResponse<Campus>>(`${environment.apiUrl}/planteles`, {
      params: buildParams({ isActive: true }),
    }).pipe(
      map((res) => res.content),
    );
  }

  getCatalog(size = 100): Observable<Campus[]> {
    return this.http.get<PaginatedResponse<Campus>>(`${environment.apiUrl}/planteles`, {
      params: buildParams({ size, isActive: true }),
    }).pipe(map((res) => res.content));
  }

  getById(id: number): Observable<Campus> {
    return this.http.get<Campus>(`${environment.apiUrl}/planteles/${id}`);
  }

  add(campus: Omit<Campus, 'id' | 'isActive'>): Observable<Campus> {
    return this.http.post<Campus>(`${environment.apiUrl}/planteles`, campus).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }

  update(id: number, changes: Partial<Omit<Campus, 'id'>>): Observable<Campus> {
    return this.http.put<Campus>(`${environment.apiUrl}/planteles/${id}`, changes).pipe(
      tap((res) =>
        this._campuses.update((list) => list.map((c) => (c.id === id ? res : c)))
      ),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/planteles/${id}`).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }

  restore(id: number): Observable<Campus> {
    return this.http.post<Campus>(`${environment.apiUrl}/planteles/${id}/restore`, {}).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }
}
