import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { StudentsService } from '../../core/services/students.service';
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

  readonly students = this.studentsService.students;
  readonly totalElements = this.studentsService.totalElements;
  readonly totalPages = this.studentsService.totalPages;
  readonly currentPage = this.studentsService.currentPage;
  readonly pageSize = this.studentsService.pageSize;
  readonly STATUS_MAP = STATUS_MAP;
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);

  readonly filtered = computed(() => this.students());

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
    const q = this.filterQ().trim();
    this.studentsService.loadPage(0, this.pageSize(), q ? { search: q } : {});
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.studentsService.loadPage(0, this.pageSize(), {});
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
      this.studentsService.update(id, payload).subscribe();
    } else {
      this.studentsService.add(payload).subscribe();
    }
    this.closeForm();
  }

  delete(id: number): void {
    this.confirm.confirm('¿Eliminar este alumno?').subscribe((ok) => {
      if (ok) this.studentsService.delete(id).subscribe();
    });
  }

  onPageChange(page: number): void {
    this.studentsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.studentsService.loadPage(0, size);
  }
}
