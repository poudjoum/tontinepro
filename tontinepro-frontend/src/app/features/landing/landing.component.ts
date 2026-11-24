import { Component, OnInit, signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DocumentationTontineProComponent } from '../../shared/components/documentation-tontinepro/documentation-tontinepro.component';
import { SetupService } from '../../core/services/setup.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, DocumentationTontineProComponent],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  private setup = inject(SetupService);
  auth = inject(AuthService);
  configured = signal(true);

  features = [
    { icon: '💰', title: 'Cotisations', desc: 'Qui a payé, qui est en retard' },
    { icon: '🏦', title: 'Épargne', desc: 'Dépôts, retraits, intérêts' },
    { icon: '📋', title: 'Prêts', desc: 'Demandes et remboursements' },
    { icon: '🤝', title: 'Aides', desc: 'Quand un membre a besoin d’aide' },
    { icon: '📊', title: 'Rapports', desc: 'En PDF ou Excel, prêts à partager' },
    { icon: '🔔', title: 'Notifications', desc: 'Par e-mail et dans l’appli' },
  ];

  ngOnInit(): void {
    this.setup.isConfigured().subscribe(c => this.configured.set(c));
  }
}
