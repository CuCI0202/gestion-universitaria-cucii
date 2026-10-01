import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmService } from '../../core/services/confirm.service';
import { GroupsService } from '../../core/services/groups.service';
import { ProgramsService } from '../../core/services/programs.service';
import { CampusesService } from '../../core/services/campuses.service';
import { Group } from '../../core/models/group.model';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-groups',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './groups.html',
})
export class Groups {
  private readonly router = inject(Router);
  private readonly groupsService = inject(GroupsService);
  private readonly programsService = inject(ProgramsService);
  private readonly campusesService = inject(CampusesService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);

  readonly groups = this.groupsService.groups;
  readonly totalElements = this.groupsService.totalElements;
  readonly totalPages = this.groupsService.totalPages;
  readonly currentPage = this.groupsService.currentPage;
  readonly pageSize = this.groupsService.pageSize;
  readonly programs = this.programsService.programs;
  readonly campuses = this.campusesService.campuses;
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);

  readonly filtered = computed(() => this.groups());

  readonly form = this.fb.nonNullable.group({
    clave: ['', Validators.required],
    nombre: ['', Validators.required],
    planEstudioId: ['', Validators.required],
    plantelId: ['', Validators.required],
  });

  search(): void {
    this.filterQ.set(this.filterDraft());
    const q = this.filterQ().trim();
    this.groupsService.loadPage(0, this.pageSize(), q ? { search: q } : {});
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.groupsService.loadPage(0, this.pageSize(), {});
  }

  getProgramName(id: number): string {
    return this.programs().find((p) => p.id === id)?.nombre ?? String(id);
  }

  getCampusName(id: number): string {
    return this.campuses().find((c) => c.id === id)?.nombreOficial ?? String(id);
  }

  startEdit(group: Group): void {
    this.editingId.set(group.id);
    this.showForm.set(true);
    this.form.setValue({
      clave: group.clave ?? '',
      nombre: group.nombre ?? '',
      planEstudioId: String(group.planEstudioId ?? ''),
      plantelId: String(group.plantelId ?? ''),
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
      clave: v.clave,
      nombre: v.nombre,
      planEstudioId: +v.planEstudioId,
      plantelId: +v.plantelId,
    };
    const id = this.editingId();
    if (id !== null) {
      this.groupsService.update(id, payload).subscribe();
    } else {
      this.groupsService.add(payload).subscribe();
    }
    this.closeForm();
  }

  delete(id: number): void {
    this.confirm.confirm('¿Eliminar este grupo?').subscribe((ok) => {
      if (ok) this.groupsService.delete(id).subscribe();
    });
  }

  goToStudents(id: number): void {
    this.router.navigate(['/groups', id, 'students']);
  }

  onPageChange(page: number): void {
    this.groupsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.groupsService.loadPage(0, size);
  }
}
