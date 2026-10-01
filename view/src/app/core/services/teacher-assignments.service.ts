import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TeacherAssignment } from '../models/teacher-assignment.model';
import { PaginatedResponse } from '../models/pagination.model';
import { buildParams, QueryFilters } from './http-params';

export interface TeacherAssignmentRequest {
  userId: number;
  groupId: number;
  subjectId: number;
}

interface ProfesorGrupoDetalleResponse {
  id: number;
  usuarioId: number;
  usuarioNombre: string;
  grupoId: number;
  grupoClave: string;
  grupoNombre: string;
  materiaId: number;
  materiaClave: string;
  materiaNombre: string;
}

@Injectable({ providedIn: 'root' })
export class TeacherAssignmentsService {
  private readonly http = inject(HttpClient);

  private readonly _assignments = signal<TeacherAssignment[]>([]);
  readonly assignments = this._assignments.asReadonly();

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
    this.http.get<PaginatedResponse<ProfesorGrupoDetalleResponse>>(`${environment.apiUrl}/profesores-grupos`, {
      params: buildParams({ page, size: s, ...this._filters }),
    }).subscribe({
      next: (res) => {
        this._assignments.set(res.content.map(toAssignment));
        this._totalElements.set(res.totalElements);
        this._totalPages.set(res.totalPages);
        this._currentPage.set(res.currentPage);
        this._pageSize.set(s);
      },
    });
  }

  add(assignment: TeacherAssignmentRequest): Observable<void> {
    return this.http.post<unknown>(`${environment.apiUrl}/profesores-grupos`, {
      usuarioId: assignment.userId,
      grupoId: assignment.groupId,
      materiaId: assignment.subjectId,
    }).pipe(
      tap(() => this.loadPage(this._currentPage())),
      map(() => void 0),
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/profesores-grupos/${id}`).pipe(
      tap(() => this.loadPage(this._currentPage())),
    );
  }
}

function toAssignment(res: ProfesorGrupoDetalleResponse): TeacherAssignment {
  return {
    id: res.id,
    userId: res.usuarioId,
    userName: res.usuarioNombre,
    groupId: res.grupoId,
    groupClave: res.grupoClave,
    groupName: res.grupoNombre,
    subjectId: res.materiaId,
    subjectClave: res.materiaClave,
    subjectName: res.materiaNombre,
  };
}
