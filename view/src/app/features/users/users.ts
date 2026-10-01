import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { UsersService } from '../../core/services/users.service';
import { CampusesService } from '../../core/services/campuses.service';
import { User } from '../../core/models/user.model';
import { Campus } from '../../core/models/campus.model';
import { UserRole, mapRolId } from '../../core/models/auth.model';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './users.html',
})
export class Users {
  private readonly usersService = inject(UsersService);
  private readonly campusesService = inject(CampusesService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);

  readonly users = this.usersService.users;
  readonly totalElements = this.usersService.totalElements;
  readonly totalPages = this.usersService.totalPages;
  readonly currentPage = this.usersService.currentPage;
  readonly pageSize = this.usersService.pageSize;
  readonly campuses = signal<Campus[]>([]);
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly editingId = signal<number | null>(null);
  readonly showForm = signal(false);
  readonly showPassword = signal(false);

  constructor() {
    this.campusesService.getCatalog().subscribe({ next: (c) => this.campuses.set(c) });
  }

  readonly filtered = computed(() => this.users());

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    nombre: ['', Validators.required],
    apellido: ['', Validators.required],
    password: [''],
    rolId: [3 as number, Validators.required],
    plantelId: [1 as number, Validators.required],
  });

  readonly roles: { id: number; label: string }[] = [
    { id: 1, label: 'Administrador' },
    { id: 2, label: 'Rector' },
    { id: 3, label: 'Docente' },
    { id: 4, label: 'Servicios Escolares' },
    { id: 5, label: 'Coordinador' },
  ];

  search(): void {
    this.filterQ.set(this.filterDraft());
    const q = this.filterQ().trim();
    this.usersService.loadPage(0, this.pageSize(), q ? { search: q } : {});
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.usersService.loadPage(0, this.pageSize(), {});
  }

  startEdit(user: User): void {
    this.editingId.set(user.id);
    this.showForm.set(true);
    this.showPassword.set(false);
    this.form.setValue({
      email: user.email,
      nombre: user.nombre,
      apellido: user.apellido,
      password: '',
      rolId: user.rolId,
      plantelId: user.plantelId,
    });
  }

  closeForm(): void {
    this.editingId.set(null);
    this.showForm.set(false);
    this.showPassword.set(false);
    this.form.reset();
  }

  private openAddForm(): void {
    this.editingId.set(null);
    this.showForm.set(true);
    this.showPassword.set(false);
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
      this.usersService.update(id, {
        nombre: v.nombre,
        apellido: v.apellido,
        email: v.email,
        rolId: v.rolId,
        plantelId: v.plantelId,
        password: v.password.trim() || undefined,
      }).subscribe();
    } else {
      if (!v.password.trim()) {
        this.form.controls.password.markAsTouched();
        return;
      }
      this.usersService.add(v).subscribe();
    }
    this.closeForm();
  }

  delete(id: number): void {
    this.confirm.confirm('¿Eliminar este usuario?').subscribe((ok) => {
      if (ok) this.usersService.delete(id).subscribe();
    });
  }

  getRoleLabel(rolId: number): string {
    const labels: Record<UserRole, string> = {
      admin: 'Administrador',
      rector: 'Rector',
      docente: 'Docente',
      servicios_escolares: 'Servicios Escolares',
      coordinador: 'Coordinador',
    };
    return labels[mapRolId(rolId)] ?? 'Desconocido';
  }

  onPageChange(page: number): void {
    this.usersService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.usersService.loadPage(0, size);
  }
}
