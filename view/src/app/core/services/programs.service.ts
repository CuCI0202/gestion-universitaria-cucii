import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Program, Subject } from '../models/program.model';
import { PaginatedResponse } from '../models/pagination.model';
import { buildParams, QueryFilters } from './http-params';

@Injectable({ providedIn: 'root' })
export class ProgramsService {
  private readonly http = inject(HttpClient);

  private readonly _programs = signal<Program[]>([]);
  readonly programs = this._programs.asReadonly();

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
    this.http.get<PaginatedResponse<any>>(`${environment.apiUrl}/planes-estudio/con-materias-count`, {
      params: buildParams({ page, size: s, ...this._filters }),
    }).subscribe({
      next: (res) => {
        this._programs.set(res.content.map((p: any) => ({
          id: p.id,
          nombre: p.nombre,
          grado: p.grado,
          numeroRvoe: p.numeroRvoe,
          fechaRvoe: p.fechaRvoe,
          duracionCuatrimestres: p.duracionCuatrimestres,
          cantidadMaterias: p.cantidadMaterias ?? 0,
          materias: [],
        })));
        this._totalElements.set(res.totalElements);
        this._totalPages.set(res.totalPages);
        this._currentPage.set(res.currentPage);
        this._pageSize.set(s);
      },
    });
  }

  getAll(): Observable<Program[]> {
    return of(this._programs());
  }

  getById(id: number): Observable<Program | undefined> {
    return of(this._programs().find((p) => p.id === id));
  }

  add(data: {
    nombre: string;
    grado: string;
    numeroRvoe: string;
    fechaRvoe: string;
    duracionCuatrimestres: number;
  }): Observable<Program> {
    return this.http.post<any>(`${environment.apiUrl}/planes-estudio`, data).pipe(
      tap(() => this.loadPage(this._currentPage())),
      map((res) => ({ ...res, materias: [], cantidadMaterias: 0 })),
    );
  }

  update(id: number, data: Partial<{
    nombre: string;
    grado: string;
    numeroRvoe: string;
    fechaRvoe: string;
    duracionCuatrimestres: number;
  }>): Observable<Program> {
    return this.http.put<any>(`${environment.apiUrl}/planes-estudio/${id}`, data).pipe(
      tap((res) => {
        const existing = this._programs().find((p) => p.id === id);
        this._programs.update((list) =>
          list.map((p) => (p.id === id ? { ...res, materias: existing?.materias ?? [], cantidadMaterias: existing?.cantidadMaterias ?? 0 } : p))
        );
      }),
      map((res) => {
        const existing = this._programs().find((p) => p.id === id);
        return { ...res, materias: existing?.materias ?? [], cantidadMaterias: existing?.cantidadMaterias ?? 0 };
      }),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/planes-estudio/${id}`).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }

  getSubjectsByProgram(programId: number): Observable<Subject[]> {
    return this.http.get<any>(`${environment.apiUrl}/planes-estudio/${programId}/con-materias`).pipe(
      map((res) => res.materias ?? []),
    );
  }

  addSubject(programId: number, subject: Omit<Subject, 'id'>): Observable<Subject> {
    return this.http.post<any>(`${environment.apiUrl}/materias`, {
      ...subject,
      planEstudioId: programId,
    }).pipe(
      tap((res) =>
        this._programs.update((list) =>
          list.map((p) =>
            p.id === programId ? { ...p, materias: [...p.materias, res], cantidadMaterias: p.cantidadMaterias + 1 } : p
          )
        )
      ),
    );
  }

  updateSubject(programId: number, subjectId: number, changes: Omit<Subject, 'id'>): Observable<Subject> {
    return this.http.put<any>(`${environment.apiUrl}/materias/${subjectId}`, {
      ...changes,
      planEstudioId: programId,
    }).pipe(
      tap((res) =>
        this._programs.update((list) =>
          list.map((p) =>
            p.id !== programId ? p : {
              ...p,
              materias: p.materias.map((s) => (s.id === subjectId ? res : s)),
            }
          )
        )
      ),
    );
  }

  deleteSubject(programId: number, subjectId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/materias/${subjectId}`).pipe(
      tap(() =>
        this._programs.update((list) =>
          list.map((p) =>
            p.id !== programId ? p : { ...p, materias: p.materias.filter((s) => s.id !== subjectId), cantidadMaterias: p.cantidadMaterias - 1 }
          )
        )
      ),
    );
  }
}
