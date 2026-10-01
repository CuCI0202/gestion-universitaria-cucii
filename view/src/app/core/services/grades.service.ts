import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Grade } from '../models/grade.model';
import { PaginatedResponse } from '../models/pagination.model';
import { buildParams, QueryFilters } from './http-params';

interface CalificacionResponse {
  id: number;
  alumnoId: number;
  grupoId: number;
  materiaId: number;
  calificacion: number;
  registradoPor: number;
}

interface CalificacionRequest {
  alumnoId: number;
  grupoId: number;
  materiaId: number;
  calificacion: number;
  registradoPor: number;
}

@Injectable({ providedIn: 'root' })
export class GradesService {
  private readonly http = inject(HttpClient);

  private readonly _grades = signal<Grade[]>([]);
  readonly grades = this._grades.asReadonly();

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
    this.http.get<PaginatedResponse<CalificacionResponse>>(`${environment.apiUrl}/calificaciones`, {
      params: buildParams({ page, size: s, ...this._filters }),
    }).subscribe({
      next: (res) => {
        this._grades.set(res.content.map(toGrade));
        this._totalElements.set(res.totalElements);
        this._totalPages.set(res.totalPages);
        this._currentPage.set(res.currentPage);
        this._pageSize.set(s);
      },
    });
  }

  getAll(): Observable<Grade[]> {
    return this.http.get<PaginatedResponse<CalificacionResponse>>(`${environment.apiUrl}/calificaciones`).pipe(
      map((res) => res.content.map(toGrade)),
    );
  }

  getByStudent(alumnoId: number): Observable<Grade[]> {
    return this.http.get<PaginatedResponse<CalificacionResponse>>(`${environment.apiUrl}/calificaciones`, {
      params: buildParams({ alumnoId, size: 100 }),
    }).pipe(map((res) => res.content.map(toGrade)));
  }

  addGrade(grade: Omit<Grade, 'id'>): Observable<Grade> {
    return this.http.post<CalificacionResponse>(`${environment.apiUrl}/calificaciones`, grade).pipe(
      tap(() => this.loadPage(this._currentPage())),
      map(toGrade),
    );
  }

  update(id: number, changes: Partial<Omit<Grade, 'id'>>): Observable<Grade> {
    return this.http.put<CalificacionResponse>(`${environment.apiUrl}/calificaciones/${id}`, changes).pipe(
      tap((res) =>
        this._grades.update((list) => list.map((g) => (g.id === id ? toGrade(res) : g)))
      ),
      map(toGrade),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/calificaciones/${id}`).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }
}

function toGrade(res: CalificacionResponse): Grade {
  return {
    id: res.id,
    alumnoId: res.alumnoId,
    grupoId: res.grupoId,
    materiaId: res.materiaId,
    calificacion: res.calificacion,
    registradoPor: res.registradoPor,
  };
}
