import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from './shared/components/sidebar/sidebar';
import { ConfirmModal } from './shared/components/confirm-modal/confirm-modal';
import { ToastComponent } from './shared/components/toast/toast';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, ConfirmModal, ToastComponent],
  template: `
    @if (auth.isAuthenticated()) {
      <div class="flex h-screen">
        <app-sidebar />
        <main class="flex-1 overflow-auto">
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
    <app-confirm-modal />
    <app-toast />
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
}
