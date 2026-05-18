import { Component, OnInit, signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SetupService } from '../../core/services/setup.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  private setup = inject(SetupService);
  auth = inject(AuthService);
  configured = signal(true);

  features = [
    { icon: '💰', title: 'Cotisations', desc: 'Suivi et paiements mensuels' },
    { icon: '🏦', title: 'Épargne', desc: 'Dépôts, retraits, intérêts' },
    { icon: '📋', title: 'Prêts', desc: 'Demande et remboursement' },
    { icon: '🤝', title: 'Aides', desc: 'Entraide mutuelle' },
    { icon: '📊', title: 'Rapports', desc: 'PDF et Excel détaillés' },
    { icon: '🔔', title: 'Notifications', desc: 'Email et in-app' },
  ];

  ngOnInit(): void {
    this.setup.isConfigured().subscribe(c => this.configured.set(c));
  }
}
