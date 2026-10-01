import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificationService } from '../services/notification.service';

export const SKIP_ERROR_NOTIFICATION = new HttpContextToken<boolean>(() => false);

const STATUS_MESSAGES: Record<number, string> = {
  0: 'No se pudo conectar con el servidor.',
  403: 'No tienes permisos para realizar esta acción.',
  404: 'El recurso solicitado no existe.',
  409: 'El registro entra en conflicto con otro existente.',
  500: 'Error interno del servidor.',
};

export function extractErrorMessage(err: HttpErrorResponse): string {
  const body = err.error;
  if (body && typeof body === 'object' && typeof body.error === 'string' && body.error.trim()) {
    return body.error;
  }
  if (typeof body === 'string' && body.trim()) {
    return body;
  }
  return STATUS_MESSAGES[err.status] ?? `Ocurrió un error (${err.status}).`;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(SKIP_ERROR_NOTIFICATION)) {
    return next(req);
  }
  const notifications = inject(NotificationService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401) {
        notifications.error(extractErrorMessage(err));
      }
      return throwError(() => err);
    })
  );
};
