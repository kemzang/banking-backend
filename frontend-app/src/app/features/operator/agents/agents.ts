import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-operator-agents',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './agents.html',
  styleUrl: './agents.scss',
})
export class OperatorAgents implements OnInit {
  private auth = inject(AuthService);

  agents = signal<UserResponse[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);
  creation = signal(false);

  firstName = '';
  lastName = '';
  email = '';
  password = '';

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.auth.listOperatorAgents().subscribe({
      next: agents => this.agents.set(agents),
      error: () => this.erreur.set('Impossible de charger les agents de votre opérateur.'),
    });
  }

  creer(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.creation.set(true);
    this.auth.createOperatorAgent({
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
    }).subscribe({
      next: agent => {
        this.creation.set(false);
        this.firstName = '';
        this.lastName = '';
        this.email = '';
        this.password = '';
        this.succes.set(`Agent ${agent.email} créé pour l'opérateur #${agent.operatorId}.`);
        this.charger();
      },
      error: e => {
        this.creation.set(false);
        this.erreur.set(e.status === 409 ? 'Cet email est déjà utilisé.' : "Impossible de créer l'agent.");
      },
    });
  }
}
