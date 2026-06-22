import { Injectable, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(
    (localStorage.getItem('bankos_theme') as Theme) ?? 'dark'
  );

  constructor() { this.apply(); }

  toggle(): void {
    const next: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    localStorage.setItem('bankos_theme', next);
    this.apply();
  }

  private apply(): void {
    const html = document.documentElement;
    if (this.theme() === 'dark') {
      html.classList.add('dark');
    } else {
      html.classList.remove('dark');
    }
  }
}
