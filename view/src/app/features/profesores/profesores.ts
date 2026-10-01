import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { GroupsService } from '../../core/services/groups.service';
import { ProgramsService } from '../../core/services/programs.service';
import { UsersService } from '../../core/services/users.service';
import { TeacherAssignmentsService } from '../../core/services/teacher-assignments.service';
import { Subject } from '../../core/models/program.model';
import { Group } from '../../core/models/group.model';
import { User } from '../../core/models/user.model';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-profesores',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './profesores.html',
})
export class Profesores {
  private readonly usersService = inject(UsersService);
  private readonly groupsService = inject(GroupsService);
  private readonly programsService = inject(ProgramsService);
  private readonly assignmentsService = inject(TeacherAssignmentsService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);

  readonly filtered = this.assignmentsService.assignments;
  readonly totalElements = this.assignmentsService.totalElements;
  readonly totalPages = this.assignmentsService.totalPages;
  readonly currentPage = this.assignmentsService.currentPage;
  readonly pageSize = this.assignmentsService.pageSize;

  readonly teachers = signal<User[]>([]);
  readonly groups = signal<Group[]>([]);

  readonly showAddForm = signal(false);
  readonly selectedGroupId = signal<number | null>(null);
  readonly filterDraft = signal('');
  readonly filterQ = signal('');
  readonly duplicateError = signal(false);

  private readonly _subjects = signal<Subject[]>([]);
  readonly availableSubjects = this._subjects.asReadonly();

  readonly addForm = this.fb.nonNullable.group({
    userId: ['', Validators.required],
    groupId: ['', Validators.required],
    subjectId: ['', Validators.required],
  });

  constructor() {
    this.usersService.getTeachers().subscribe({ next: (teachers) => this.teachers.set(teachers) });
    this.groupsService.getAll().subscribe({ next: (groups) => this.groups.set(groups) });
  }

  search(): void {
    this.filterQ.set(this.filterDraft());
    const q = this.filterQ().trim();
    this.assignmentsService.loadPage(0, this.pageSize(), q ? { search: q } : {});
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.assignmentsService.loadPage(0, this.pageSize(), {});
  }

  onGroupChange(value: string): void {
    const gId = value ? +value : null;
    this.selectedGroupId.set(gId);
    this.addForm.patchValue({ subjectId: '' });

    if (gId) {
      const group = this.groups().find((g) => g.id === gId);
      if (group) {
        this.programsService.getSubjectsByProgram(group.planEstudioId).subscribe({
          next: (subjects) => this._subjects.set(subjects),
        });
      }
    } else {
      this._subjects.set([]);
    }
  }

  toggleAddForm(): void {
    this.showAddForm.update((v) => !v);
    if (!this.showAddForm()) {
      this.addForm.reset();
      this.selectedGroupId.set(null);
      this._subjects.set([]);
      this.duplicateError.set(false);
    }
  }

  submitAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    this.assignmentsService
      .add({ userId: +v.userId, groupId: +v.groupId, subjectId: +v.subjectId })
      .subscribe({
        next: () => {
          this.duplicateError.set(false);
          this.addForm.reset();
          this.selectedGroupId.set(null);
          this._subjects.set([]);
          this.showAddForm.set(false);
        },
        error: () => this.duplicateError.set(true),
      });
  }

  delete(id: number): void {
    this.confirm.confirm('¿Eliminar esta asignación?').subscribe((ok) => {
      if (ok) this.assignmentsService.delete(id).subscribe();
    });
  }

  onPageChange(page: number): void {
    this.assignmentsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.assignmentsService.loadPage(0, size);
  }
}
