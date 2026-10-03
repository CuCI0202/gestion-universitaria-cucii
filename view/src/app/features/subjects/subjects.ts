import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmService } from '../../core/services/confirm.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProgramsService } from '../../core/services/programs.service';
import { Program, Subject } from '../../core/models/program.model';

@Component({
  selector: 'app-subjects',
  imports: [ReactiveFormsModule],
  templateUrl: './subjects.html',
})
export class Subjects {
  private readonly programsService = inject(ProgramsService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly programs = signal<Program[]>([]);
  readonly selectedProgramId = signal<number>(0);
  readonly editingId = signal<number | null>(null);
  readonly showArchived = signal(false);
  private readonly _subjects = signal<Subject[]>([]);
  readonly subjects = this._subjects.asReadonly();

  constructor() {
    this.programsService.getCatalog().subscribe({ next: (p) => this.programs.set(p) });
  }

  readonly availableTerms = computed(() => {
    const id = this.selectedProgramId();
    if (!id) return [];
    const program = this.programs().find((p) => p.id === id);
    if (!program) return [];
    return Array.from({ length: program.duracionCuatrimestres }, (_, i) => i + 1);
  });

  readonly form = this.fb.nonNullable.group({
    clave: ['', Validators.required],
    nombre: ['', Validators.required],
    cuatrimestre: [1, [Validators.required, Validators.min(1)]],
    creditos: [0, [Validators.required, Validators.min(0)]],
  });

  private loadSubjects(): void {
    const id = this.selectedProgramId();
    if (!id) return;
    this.programsService.getSubjectsByProgramFiltered(id, this.showArchived()).subscribe({
      next: (res) => this._subjects.set(res),
    });
  }

  toggleArchived(): void {
    this.showArchived.update((v) => !v);
    this.loadSubjects();
  }

  onProgramChange(value: string): void {
    this.selectedProgramId.set(+value);
    this.closeForm();
    this.loadSubjects();
  }

  startEdit(subject: Subject): void {
    this.editingId.set(subject.id);
    this.form.setValue({
      clave: subject.clave,
      nombre: subject.nombre,
      cuatrimestre: subject.cuatrimestre,
      creditos: subject.creditos ?? 0,
    });
  }

  closeForm(): void {
    this.editingId.set(null);
    this.form.reset();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const programId = this.selectedProgramId();
    const id = this.editingId();
    if (id !== null) {
      this.programsService.updateSubject(programId, id, v).subscribe(() => {
        this.notifications.success('Materia actualizada.');
        this.loadSubjects();
        this.closeForm();
      });
    } else {
      this.programsService.addSubject(programId, v).subscribe(() => {
        this.notifications.success('Materia registrada.');
        this.loadSubjects();
        this.closeForm();
      });
    }
  }

  delete(subjectId: number): void {
    this.confirm.confirm('¿Archivar esta materia?').subscribe((ok) => {
      if (ok) this.programsService.deleteSubject(this.selectedProgramId(), subjectId).subscribe(() => {
        this.notifications.success('Materia archivada.');
        this.loadSubjects();
      });
    });
  }

  restore(subjectId: number): void {
    this.programsService.restoreSubject(this.selectedProgramId(), subjectId).subscribe(() => {
      this.notifications.success('Materia restaurada.');
      this.loadSubjects();
    });
  }
}
