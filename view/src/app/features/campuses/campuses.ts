import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { NotificationService } from '../../core/services/notification.service';
import { CampusesService } from '../../core/services/campuses.service';
import { Campus } from '../../core/models/campus.model';
import { QueryFilters } from '../../core/services/http-params';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-campuses',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './campuses.html',
})
export class Campuses {
  private readonly campusesService = inject(CampusesService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly campuses = this.campusesService.campuses;
  readonly totalElements = this.campusesService.totalElements;
  readonly totalPages = this.campusesService.totalPages;
  readonly currentPage = this.campusesService.currentPage;
  readonly pageSize = this.campusesService.pageSize;
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);
  readonly showArchived = signal(false);

  readonly filtered = computed(() => this.campuses());

  readonly form = this.fb.nonNullable.group({
    nombreOficial: ['', Validators.required],
    nombreCorto: [''],
    direccionCalle: [''],
    direccionNumeroExt: [''],
    direccionNumeroInt: [''],
    colonia: [''],
    codigoPostal: ['', Validators.pattern(/^\d{5}$/)],
    ciudadMunicipio: ['', Validators.required],
    estado: ['', Validators.required],
    pais: ['México'],
    directorNombre: [''],
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
    this.campusesService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.campusesService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  toggleArchived(): void {
    this.showArchived.update((v) => !v);
    this.campusesService.loadPage(0, this.pageSize(), this.buildFilters());
  }

  startEdit(campus: Campus): void {
    this.editingId.set(campus.id);
    this.showForm.set(true);
    this.form.setValue({
      nombreOficial: campus.nombreOficial ?? '',
      nombreCorto: campus.nombreCorto ?? '',
      direccionCalle: campus.direccionCalle ?? '',
      direccionNumeroExt: campus.direccionNumeroExt ?? '',
      direccionNumeroInt: campus.direccionNumeroInt ?? '',
      colonia: campus.colonia ?? '',
      codigoPostal: campus.codigoPostal ?? '',
      ciudadMunicipio: campus.ciudadMunicipio ?? '',
      estado: campus.estado ?? '',
      pais: campus.pais ?? 'México',
      directorNombre: campus.directorNombre ?? '',
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
      nombreOficial: v.nombreOficial,
      nombreCorto: v.nombreCorto || undefined,
      direccionCalle: v.direccionCalle || undefined,
      direccionNumeroExt: v.direccionNumeroExt || undefined,
      direccionNumeroInt: v.direccionNumeroInt || undefined,
      colonia: v.colonia || undefined,
      codigoPostal: v.codigoPostal || undefined,
      ciudadMunicipio: v.ciudadMunicipio,
      estado: v.estado,
      pais: v.pais || undefined,
      directorNombre: v.directorNombre || undefined,
    };
    const id = this.editingId();
    if (id !== null) {
      this.campusesService.update(id, payload).subscribe({
        next: () => this.notifications.success('Plantel actualizado.'),
      });
    } else {
      this.campusesService.add(payload).subscribe({
        next: () => this.notifications.success('Plantel registrado.'),
      });
    }
    this.closeForm();
  }

  delete(id: number): void {
    this.confirm.confirm('¿Archivar este plantel?').subscribe((ok) => {
      if (ok) {
        this.campusesService.delete(id).subscribe({
          next: () => this.notifications.success('Plantel archivado.'),
        });
      }
    });
  }

  restore(id: number): void {
    this.campusesService.restore(id).subscribe({
      next: () => this.notifications.success('Plantel restaurado.'),
    });
  }

  onPageChange(page: number): void {
    this.campusesService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.campusesService.loadPage(0, size);
  }
}
