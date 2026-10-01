import { Component, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { GroupsService } from '../../../core/services/groups.service';
import { GroupStudentsService } from '../../../core/services/group-students.service';
import { Group } from '../../../core/models/group.model';
import { Student, fullName } from '../../../core/models/student.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-group-students',
  imports: [PaginationComponent],
  templateUrl: './group-students.html',
})
export class GroupStudents {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly groupsService = inject(GroupsService);
  private readonly groupStudentsService = inject(GroupStudentsService);

  readonly groupId = +this.route.snapshot.params['id'];

  readonly group = signal<Group | null>(null);

  readonly filterDraft = signal('');
  readonly filterQ = signal('');

  readonly assignedStudents = signal<Student[]>([]);
  readonly availableStudents = signal<Student[]>([]);

  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);

  constructor() {
    this.groupsService.getById(this.groupId).subscribe({
      next: (group) => this.group.set(group),
    });
    this.loadAssigned();
    this.loadAvailable(0, this.pageSize());
  }

  private loadAssigned(): void {
    this.groupStudentsService.getStudentsByGroup(this.groupId).subscribe({
      next: (students) => this.assignedStudents.set(students),
    });
  }

  private loadAvailable(page: number, size: number): void {
    this.groupStudentsService
      .getAvailableStudents(this.groupId, page, size, this.filterQ())
      .subscribe({
        next: (res) => {
          this.availableStudents.set(res.content);
          this.totalElements.set(res.totalElements);
          this.totalPages.set(res.totalPages);
          this.currentPage.set(res.currentPage);
          this.pageSize.set(res.pageSize);
        },
      });
  }

  fullName(student: Student): string {
    return fullName(student);
  }

  search(): void {
    this.filterQ.set(this.filterDraft());
    this.loadAvailable(0, this.pageSize());
  }

  clearFilter(): void {
    this.filterDraft.set('');
    this.filterQ.set('');
    this.loadAvailable(0, this.pageSize());
  }

  assign(studentId: number): void {
    this.groupStudentsService.assign(this.groupId, studentId).subscribe({
      next: () => {
        this.loadAssigned();
        this.loadAvailable(this.currentPage(), this.pageSize());
      },
    });
  }

  remove(studentId: number): void {
    this.groupStudentsService.removeByGroupAndStudent(this.groupId, studentId).subscribe({
      next: () => {
        this.loadAssigned();
        this.loadAvailable(this.currentPage(), this.pageSize());
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/groups']);
  }

  onPageChange(page: number): void {
    this.loadAvailable(page, this.pageSize());
  }

  onSizeChange(size: number): void {
    this.loadAvailable(0, size);
  }
}
