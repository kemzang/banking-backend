import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { UserResponse } from '../core/models/auth.models';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private auth = inject(AuthService);
  private router = inject(Router);

  user = signal<UserResponse | null>(null);

  constructor() {
    this.auth.me().subscribe({ next: (u) => this.user.set(u) });
  }

  deconnexion(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
