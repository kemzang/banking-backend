import { Component, OnInit, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerService, Client } from '../../../core/services/customer.service';
import { UserResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-client-profile',
  standalone: true,
  templateUrl: './client-profile.html',
  styleUrl: './client-profile.scss',
})
export class ClientProfile implements OnInit {
  private auth = inject(AuthService);
  private customers = inject(CustomerService);
  user = signal<UserResponse | null>(null);
  client = signal<Client | null>(null);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.auth.me().subscribe({ next: user => this.user.set(user) });
    this.customers.getClientParEmail(this.auth.email()).subscribe({
      next: client => this.client.set(client),
      error: () => this.erreur.set('Votre profil client est introuvable.'),
    });
  }
}
