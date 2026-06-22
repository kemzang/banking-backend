import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SessionService } from './core/services/session.service';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';
import { ToastComponent } from './shared/toast/toast';
import { SessionWarningComponent } from './shared/session-warning/session-warning';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, SessionWarningComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private session = inject(SessionService);
  private auth    = inject(AuthService);
  // ThemeService s'auto-initialise dans le constructeur (apply dark/light)
  private _theme  = inject(ThemeService);

  ngOnInit(): void {
    if (this.auth.hasValidToken()) {
      this.session.start();
    }
  }
}
