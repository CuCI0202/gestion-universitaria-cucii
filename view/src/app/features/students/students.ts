import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { NotificationService } from '../../core/services/notification.service';
import { StudentsService } from '../../core/services/students.service';
import { QueryFilters } from '../../core/services/http-params';
import { Student, fullName, STATUS_MAP } from '../../core/models/student.model';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

const CURP_PATTERN = /^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$/;

@Component({
  selector: 'app-students',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './students.html',
})
export class Students {
  private readonly studentsService = inject(StudentsService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly students = this.studentsService.students;
  readonly totalElements = this.studentsService.totalElements;
  readonly totalPages = this.studentsService.totalPages;
  readonly currentPage = this.studentsService.currentPage;
  readonly pageSize = this.studentsService.pageSize;
  readonly STATUS_MAP = STATUS_MAP;
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly showArchived = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);

  readonly filtered = computed(() => this.students());

  private buildFilters(): QueryFilters {
    const q = this.filterQ().trim();
    return {
      ...(q ? { search: q } : {}),
      ...(this.showArchived() ? {} : { isActive: true }),
    };
  }

  readonly form = this.fb.nonNullable.group({
    nombres: ['', Validators.required],
    primerApellido: ['', Validators.required],
    segundoApellido: [''],
    curp: ['', [Validators.required, Validators.pattern(CURP_PATTERN)]],
    correoInstitucional: ['', [Validators.required, Validators.email]],
    estatusId: [1, Validators.required],
  });

  fullName(student: Student): string {
    return fullName(student);
  }

  search(): void {
    this.filterQ.set(this.filterDraft());
    this.studentsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.studentsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  toggleArchived(): void {
    this.showArchived.update((v) => !v);
    this.studentsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  startEdit(student: Student): void {
    this.editingId.set(student.id);
    this.showForm.set(true);
    this.form.setValue({
      nombres: student.nombres ?? '',
      primerApellido: student.primerApellido ?? '',
      segundoApellido: student.segundoApellido ?? '',
      curp: student.curp ?? '',
      correoInstitucional: student.correoInstitucional ?? '',
      estatusId: student.estatusId ?? 1,
    });
  }

  closeForm(): void {
    this.editingId.set(null);
    this.showForm.set(false);
    this.form.reset();
  }

  private openAddForm(): void {
    this.editingId.set(null);
    this.showForm.set(true);
    this.form.reset();
  }

  toggleForm(): void {
    if (this.showForm()) {
      this.closeForm();
    } else {
      this.openAddForm();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const payload = {
      nombres: v.nombres,
      primerApellido: v.primerApellido,
      segundoApellido: v.segundoApellido || undefined,
      curp: v.curp,
      correoInstitucional: v.correoInstitucional,
      estatusId: v.estatusId,
    };
    const id = this.editingId();
    if (id !== null) {
      this.studentsService.update(id, payload).subscribe({
        next: () => {
          this.notifications.success('Alumno actualizado.');
          this.closeForm();
        },
      });
    } else {
      this.studentsService.add(payload).subscribe({
        next: () => {
          this.notifications.success('Alumno registrado.');
          this.closeForm();
        },
      });
    }
  }

  delete(id: number): void {
    this.confirm.confirm('¿Archivar este alumno?').subscribe((ok) => {
      if (ok) {
        this.studentsService.delete(id).subscribe({
          next: () => this.notifications.success('Alumno archivado.'),
        });
      }
    });
  }

  restore(id: number): void {
    this.studentsService.restore(id).subscribe({
      next: () => this.notifications.success('Alumno restaurado.'),
    });
  }

  onPageChange(page: number): void {
    this.studentsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.studentsService.loadPage(0, size);
  }
}
