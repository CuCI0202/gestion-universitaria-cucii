export interface Subject {
  id: number;
  clave: string;
  nombre: string;
  cuatrimestre: number;
  creditos: number | null;
  isActive: boolean;
}
