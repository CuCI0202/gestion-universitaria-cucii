export interface Campus {
  id: number;
  nombreOficial: string;
  nombreCorto?: string;
  direccionCalle?: string;
  direccionNumeroExt?: string;
  direccionNumeroInt?: string;
  colonia?: string;
  codigoPostal?: string;
  ciudadMunicipio: string;
  estado: string;
  pais?: string;
  directorNombre?: string;
  isActive: boolean;
}
