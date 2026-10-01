import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { GradesService } from '../../core/services/grades.service';
import { StudentsService } from '../../core/services/students.service';
import { GroupsService } from '../../core/services/groups.service';
import { GroupStudentsService } from '../../core/services/group-students.service';
import { extractErrorMessage } from '../../core/interceptors/error.interceptor';
import { Student, fullName } from '../../core/models/student.model';
import { Subject } from '../../core/models/program.model';
import { Group } from '../../core/models/group.model';

@Component({
  selector: 'app-upload',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './upload.html',
})
export class Upload {
  private readonly auth = inject(AuthService);
  private readonly gradesService = inject(GradesService);
  private readonly studentsService = inject(StudentsService);
  private readonly groupsService = inject(GroupsService);
  private readonly groupStudentsService = inject(GroupStudentsService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // ── Student search ────────────────────────────────────────────────────────
  readonly searchQuery = signal('');
  readonly foundStudent = signal<Student | null>(null);
  readonly searchError = signal('');
  readonly showModal = signal(false);
  readonly searchResults = signal<Student[]>([]);

  readonly studentGroups = signal<Group[]>([]);

  readonly availableTerms = signal<number[]>([]);
  readonly studentSubjects = signal<Subject[]>([]);

  fullName(student: Student): string {
    return fullName(student);
  }

  searchStudent(): void {
    const query = this.searchQuery().trim();
    if (!query) return;
    this.studentsService.searchStudents(query, 10).subscribe({
      next: (results) => {
        this.searchResults.set(results);
        this.showModal.set(true);
        this.searchError.set(results.length ? '' : 'No se encontraron alumnos.');
      },
      error: () => {
        this.searchResults.set([]);
        this.showModal.set(true);
        this.searchError.set('Error al buscar alumnos.');
      },
    });
  }

  selectStudent(student: Student): void {
    this.foundStudent.set(student);
    this.showModal.set(false);
    this.searchError.set('');
    this.searchQuery.set(student.curp);
    this.form.patchValue({ groupId: '', term: 0, subjectId: '' });
    this.availableTerms.set([]);
    this.studentSubjects.set([]);
    this.studentGroups.set([]);

    this.groupStudentsService.getByStudent(student.id).subscribe({
      next: (assignments) => {
        if (assignments.length === 0) return;
        this.groupsService.getByIds(assignments.map((a) => a.groupId)).subscribe({
          next: (groups) => this.studentGroups.set(groups),
        });
      },
    });
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  // ── Group / Term / Subject ─────────────────────────────────────────────────
  onGroupChange(value: string): void {
    const gId = value ? +value : null;
    this.form.patchValue({ term: 0, subjectId: '' });
    this.availableTerms.set([]);
    this.studentSubjects.set([]);

    if (gId) {
      this.groupsService.getCuatrimestresCount(gId).subscribe({
        next: (count) => this.availableTerms.set(
          Array.from({ length: count }, (_, i) => i + 1)
        ),
      });
    }
  }

  onTermChange(value: string): void {
    const term = value ? +value : 0;
    const gId = this.form.getRawValue().groupId;
    this.form.patchValue({ subjectId: '' });
    this.studentSubjects.set([]);

    if (gId && term > 0) {
      this.groupsService.getSubjectsByGroupAndTerm(+gId, term).subscribe({
        next: (subjects) => this.studentSubjects.set(subjects),
      });
    }
  }

  // ── Manual form ──────────────────────────────────────────────────────────
  readonly form = this.fb.nonNullable.group({
    groupId: ['', Validators.required],
    term: [0, [Validators.required, Validators.min(1)]],
    subjectId: ['', Validators.required],
    score: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  readonly successMsg = signal('');
  readonly errorMsg = signal('');

  submitManual(): void {
    if (this.form.invalid || !this.foundStudent()) {
      this.form.markAllAsTouched();
      if (!this.foundStudent()) this.searchError.set('Busca un alumno antes de registrar.');
      return;
    }

    const v = this.form.getRawValue();
    const student = this.foundStudent()!;
    const registradoPor = this.auth.currentUser()?.id;

    this.gradesService.addGrade({
      alumnoId: student.id,
      grupoId: +v.groupId,
      materiaId: +v.subjectId,
      calificacion: +v.score,
      registradoPor: registradoPor ?? 0,
    }).subscribe({
      next: () => {
        this.successMsg.set('Calificación registrada correctamente.');
        this.errorMsg.set('');
        this.form.reset({ groupId: '', term: 0, score: 0 });
        this.foundStudent.set(null);
        this.searchQuery.set('');
        this.searchError.set('');
        this.availableTerms.set([]);
        this.studentSubjects.set([]);
        this.studentGroups.set([]);
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg.set(extractErrorMessage(err));
      },
    });
  }
}
