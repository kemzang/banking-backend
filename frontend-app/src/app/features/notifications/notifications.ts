import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Notification, NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-notifications',
  imports: [DecimalPipe, DatePipe],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications implements OnInit, OnDestroy {
  private notif = inject(NotificationService);

  items = signal<Notification[]>([]);
  erreur = signal<string | null>(null);
  private timer: any;

  ngOnInit(): void {
    this.charger();
    // rafraichissement automatique toutes les 5 s (les notifs arrivent en asynchrone)
    this.timer = setInterval(() => this.charger(), 5000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  charger(): void {
    this.notif.list().subscribe({
      next: (n) => this.items.set(n),
      error: () => this.erreur.set('Service de notifications indisponible.'),
    });
  }

  badge(statut: string): string {
    const s = statut === 'VALIDEE' ? 'valide' : statut === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  typeIconClass(type: string): string {
    const map: Record<string, string> = {
      DEPOT: 'type-icon type-icon--depot',
      RETRAIT: 'type-icon type-icon--retrait',
      TRANSFERT: 'type-icon type-icon--transfert',
    };
    return map[type] ?? 'type-icon type-icon--transfert';
  }
}
