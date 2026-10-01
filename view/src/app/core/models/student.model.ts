export interface Student {
  id: number;
  nombres: string;
  primerApellido: string;
  segundoApellido?: string;
  curp: string;
  correoInstitucional: string;
  estatusId: number;
  isActive: boolean;
}

export const STATUS_MAP: Record<number, { label: string; classes: string }> = {
  1: { label: 'Invasión', classes: 'bg-yellow-100 text-yellow-800 border border-yellow-300' },
  2: { label: 'Cursando', classes: 'bg-green-100 text-green-800 border border-green-300' },
  3: { label: 'Egresado', classes: 'bg-emerald-500 text-white' },
  4: { label: 'Baja', classes: 'bg-red-100 text-red-800 border border-red-300' },
};

export function fullName(student: Student): string {
  return `${student.nombres} ${student.primerApellido}${student.segundoApellido ? ' ' + student.segundoApellido : ''}`;
}
