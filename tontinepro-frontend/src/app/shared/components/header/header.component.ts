import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, FormsModule],
  templateUrl: './header.component.html',
})
export class HeaderComponent implements OnInit {
  auth = inject(AuthService);
  notifs = inject(NotificationService);
  ctx = inject(TontineContextService);

  ngOnInit(): void {
    this.ctx.init();
  }

  changerTontine(id: string): void {
    this.ctx.selectionner(id);
  }
}
