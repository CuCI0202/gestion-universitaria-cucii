import { HttpParams } from '@angular/common/http';

export type QueryFilters = Record<string, string | number | boolean | null | undefined>;

export function buildParams(filters: QueryFilters): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value === null || value === undefined || value === '') continue;
    params = params.set(key, String(value));
  }
  return params;
}
