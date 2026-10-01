import { Component, computed, inject, signal } from '@angular/core';
import { GradesService } from '../../core/services/grades.service';
import { StudentsService } from '../../core/services/students.service';
import { ProgramsService } from '../../core/services/programs.service';
import { GroupsService } from '../../core/services/groups.service';
import { GroupStudentsService } from '../../core/services/group-students.service';
import { Subject } from '../../core/models/program.model';
import { Student, fullName } from '../../core/models/student.model';
import { Grade } from '../../core/models/grade.model';
import { PaginationComponent } from '../../shared/components/pagination/pagination';

interface DetailRow {
  subjectId: number;
  subjectClave: string;
  subjectNombre: string;
  score: number | null;
}

interface TermBlock {
  term: number;
  rows: DetailRow[];
}

@Component({
  selector: 'app-browse',
  imports: [PaginationComponent],
  templateUrl: './browse.html',
})
export class Browse {
  private readonly gradesService = inject(GradesService);
  private readonly studentsService = inject(StudentsService);
  private readonly programsService = inject(ProgramsService);
  private readonly groupsService = inject(GroupsService);
  private readonly groupStudentsService = inject(GroupStudentsService);

  // ── List view ─────────────────────────────────────────────────────────────
  readonly filterCurpDraft = signal('');
  readonly filterNameDraft = signal('');
  readonly filterCurp = signal('');
  readonly filterName = signal('');

  readonly totalElements = this.studentsService.totalElements;
  readonly totalPages = this.studentsService.totalPages;
  readonly currentPage = this.studentsService.currentPage;
  readonly pageSize = this.studentsService.pageSize;

  readonly filteredStudents = computed<Student[]>(() => this.studentsService.students());

  readonly hasFilters = computed(() =>
    !!(this.filterCurp() || this.filterName())
  );

  search(): void {
    this.filterCurp.set(this.filterCurpDraft().trim().toUpperCase());
    this.filterName.set(this.filterNameDraft().trim());
    const filters: Record<string, string> = {};
    if (this.filterCurp()) filters['curp'] = this.filterCurp();
    if (this.filterName()) filters['search'] = this.filterName();
    filters['isActive'] = 'true';
    this.studentsService.loadPage(0, this.pageSize(), filters);
  }

  clearFilters(): void {
    this.filterCurpDraft.set('');
    this.filterNameDraft.set('');
    this.filterCurp.set('');
    this.filterName.set('');
    this.studentsService.loadPage(0, this.pageSize(), { isActive: 'true' });
  }

  fullName(student: Student): string {
    return fullName(student);
  }

  // ── Detail view ───────────────────────────────────────────────────────────
  readonly selectedStudent = signal<Student | null>(null);

  readonly studentProgramSubjects = signal<Subject[]>([]);
  readonly studentGrades = signal<Grade[]>([]);

  readonly detailRows = computed<TermBlock[]>(() => {
    const student = this.selectedStudent();
    if (!student) return [];

    const subjects = this.studentProgramSubjects();
    if (subjects.length === 0) return [];

    const grades = this.studentGrades();
    const maxTerm = Math.max(...subjects.map((s) => s.cuatrimestre), 0);

    return Array.from({ length: maxTerm }, (_, i) => {
      const term = i + 1;
      const termSubjects = subjects.filter((s) => s.cuatrimestre === term);
      const rows: DetailRow[] = termSubjects.map((sub) => {
        const grade = grades.find((g) => g.materiaId === sub.id);
        return {
          subjectId: sub.id,
          subjectClave: sub.clave,
          subjectNombre: sub.nombre,
          score: grade?.calificacion ?? null,
        };
      });
      return { term, rows };
    });
  });

  readonly unassignedSubjects = computed<DetailRow[]>(() => {
    const student = this.selectedStudent();
    if (!student) return [];
    const subjects = this.studentProgramSubjects();
    if (subjects.length === 0) return [];
    const grades = this.studentGrades();
    return subjects
      .filter((s) => !grades.find((g) => g.materiaId === s.id))
      .map((s) => ({
        subjectId: s.id,
        subjectClave: s.clave,
        subjectNombre: s.nombre,
        score: null,
      }));
  });

  selectStudent(student: Student): void {
    this.selectedStudent.set(student);
    this.studentProgramSubjects.set([]);
    this.studentGrades.set([]);

    this.gradesService.getByStudent(student.id).subscribe({
      next: (grades) => this.studentGrades.set(grades),
    });

    this.groupStudentsService.getByStudent(student.id).subscribe({
      next: (assignments) => {
        if (assignments.length === 0) return;
        this.groupsService.getByIds(assignments.map((a) => a.groupId)).subscribe({
          next: (groups) => {
            const group = groups[0];
            if (!group) return;
            this.programsService.getSubjectsByProgram(group.planEstudioId).subscribe({
              next: (subjects) => this.studentProgramSubjects.set(subjects),
            });
          },
        });
      },
    });
  }

  backToList(): void {
    this.selectedStudent.set(null);
    this.studentProgramSubjects.set([]);
    this.studentGrades.set([]);
  }

  getGradeClass(score: number): string {
    if (score >= 90) return 'text-green-700 font-semibold';
    if (score >= 70) return 'text-yellow-700 font-semibold';
    return 'text-red-700 font-semibold';
  }

  onPageChange(page: number): void {
    this.studentsService.loadPage(page);
  }

  onSizeChange(size: number): void {
    this.studentsService.loadPage(0, size);
  }
}
