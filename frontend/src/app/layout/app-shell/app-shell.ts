import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthSessionService } from '../../core/auth/auth-session.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  protected readonly session = this.authSession.session;
  protected readonly isAdmin = this.authSession.isAdmin;
  protected readonly loggingOut = signal(false);

  protected logout(): void {
    this.loggingOut.set(true);

    this.authSession
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        error: () => void this.router.navigateByUrl('/login'),
      });
  }
}
