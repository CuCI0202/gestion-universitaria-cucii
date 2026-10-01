import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error' | 'info';

export interface AppNotification {
  id: number;
  type: NotificationType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();

  private counter = 0;

  show(message: string, type: NotificationType = 'info', duration = 5000): number {
    const id = ++this.counter;
    this._notifications.update((list) => [...list, { id, type, message }]);
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
    return id;
  }

  success(message: string, duration = 4000): number {
    return this.show(message, 'success', duration);
  }

  error(message: string, duration = 6000): number {
    return this.show(message, 'error', duration);
  }

  info(message: string, duration = 4000): number {
    return this.show(message, 'info', duration);
  }

  dismiss(id: number): void {
    this._notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
