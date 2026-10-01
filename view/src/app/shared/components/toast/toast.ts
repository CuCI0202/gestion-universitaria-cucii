import { Component, inject } from '@angular/core';
import { NotificationService, NotificationType } from '../../../core/services/notification.service';

const CONTAINER_CLASSES: Record<NotificationType, string> = {
  success: 'border-green-200 bg-green-50',
  error: 'border-red-200 bg-red-50',
  info: 'border-blue-200 bg-blue-50',
};

const TEXT_CLASSES: Record<NotificationType, string> = {
  success: 'text-green-800',
  error: 'text-red-800',
  info: 'text-blue-800',
};

const DOT_CLASSES: Record<NotificationType, string> = {
  success: 'bg-green-500',
  error: 'bg-red-500',
  info: 'bg-blue-500',
};

@Component({
  selector: 'app-toast',
  templateUrl: './toast.html',
})
export class ToastComponent {
  private readonly service = inject(NotificationService);
  readonly notifications = this.service.notifications;

  containerClass(type: NotificationType): string {
    return `flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg text-sm pointer-events-auto ${CONTAINER_CLASSES[type]}`;
  }

  textClass(type: NotificationType): string {
    return TEXT_CLASSES[type];
  }

  dotClass(type: NotificationType): string {
    return DOT_CLASSES[type];
  }

  dismiss(id: number): void {
    this.service.dismiss(id);
  }
}
