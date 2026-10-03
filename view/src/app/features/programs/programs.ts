import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProgramsService } from '../../core/services/programs.service';
import { Program, Degree } from '../../core/models/program.model';
import { QueryFilters } from '../../core/services/http-params';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-programs',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './programs.html',
})
export class Programs {
  private readonly programsService = inject(ProgramsService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly programs = this.programsService.programs;
  readonly totalElements = this.programsService.totalElements;
  readonly totalPages = this.programsService.totalPages;
  readonly currentPage = this.programsService.currentPage;
  readonly pageSize = this.programsService.pageSize;
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);
  readonly showArchived = signal(false);

  readonly filtered = computed(() => this.programs());

  readonly degrees: Degree[] = ['Licenciatura', 'Maestría', 'Doctorado'];

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    grado: ['Licenciatura' as Degree, Validators.required],
    numeroRvoe: ['', Validators.required],
    fechaRvoe: ['', Validators.required],
    duracionCuatrimestres: [1, [Validators.required, Validators.min(1)]],
  });

  private buildFilters(): QueryFilters {
    const q = this.filterQ().trim();
    return {
      ...(q ? { search: q } : {}),
      ...(this.showArchived() ? {} : { isActive: true }),
    };
  }

  search(): void {
    this.filterQ.set(this.filterDraft());
    this.programsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.programsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  toggleArchived(): void {
    this.showArchived.update((v) => !v);
    this.programsService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  startEdit(program: Program): void {
    this.editingId.set(program.id);
    this.showForm.set(true);
    this.form.setValue({
      nombre: program.nombre ?? '',
      grado: program.grado ?? 'Licenciatura',
      numeroRvoe: program.numeroRvoe ?? '',
      fechaRvoe: program.fechaRvoe ?? '',
      duracionCuatrimestres: program.duracionCuatrimestres ?? 1,
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
    const id = this.editingId();
    if (id !== null) {
      this.programsService.update(id, v).subscribe({
        next: () => this.notifications.success('Carrera actualizada.'),
      });
    } else {
      this.programsService.add(v).subscribe({
        next: () => this.notifications.success('Carrera registrada.'),
      });
    }
    this.closeForm();
  }

  delete(id: number): void {
    this.confirm.confirm('¿Archivar esta carrera?').subscribe((ok) => {
      if (ok) {
        this.programsService.delete(id).subscribe({
          next: () => this.notifications.success('Carrera archivada.'),
        });
      }
    });
  }

  restore(id: number): void {
    this.programsService.restore(id).subscribe({
      next: () => this.notifications.success('Carrera restaurada.'),
    });
  }

  onPageChange(page: number): void {
    this.programsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.programsService.loadPage(0, size);
  }
}
