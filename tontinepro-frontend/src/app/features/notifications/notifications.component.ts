import { Component, OnInit, signal, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationResponse, NotificationType } from '../../core/models/notification.model';

function notifStyle(type: NotificationType): { icon: string; bg: string; text: string } {
  if (type.startsWith('AIDE_'))        return { icon: '🤝', bg: 'bg-green-100',  text: 'text-green-700' };
  if (type.startsWith('PRET_'))        return { icon: '📋', bg: 'bg-blue-100',   text: 'text-blue-700' };
  if (type.startsWith('COTISATION_'))  return { icon: '💰', bg: 'bg-amber-100',  text: 'text-amber-700' };
  if (type.startsWith('EPARGNE_'))     return { icon: '🏦', bg: 'bg-purple-100', text: 'text-purple-700' };
  if (type === 'BIENVENUE')            return { icon: '👋', bg: 'bg-primary-100', text: 'text-primary-700' };
  return { icon: '🔔', bg: 'bg-gray-100', text: 'text-gray-600' };
}

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
})
export class NotificationsComponent implements OnInit {
  private svc = inject(NotificationService);

  notifications = signal<NotificationResponse[]>([]);
  loading       = signal(true);
  marking       = signal(false);

  ngOnInit(): void {
    this.svc.getAll().subscribe({
      next:  n => { this.notifications.set(n); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  marquerLue(id: string): void {
    const n = this.notifications().find(n => n.id === id);
    if (!n || n.lu) return;
    this.svc.markRead(id).subscribe(updated =>
      this.notifications.update(l => l.map(item => item.id === id ? updated : item))
    );
  }

  toutMarquerLu(): void {
    this.marking.set(true);
    this.svc.markAllRead().subscribe({
      next: () => {
        this.notifications.update(l => l.map(n => ({ ...n, lu: true })));
        this.svc.unreadCount.set(0);
        this.marking.set(false);
      },
      error: () => this.marking.set(false),
    });
  }

  nonLues(): number {
    return this.notifications().filter(n => !n.lu).length;
  }

  style(type: NotificationType) { return notifStyle(type); }

  formatDate(d: string): string {
    const date = new Date(d);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return "À l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `Il y a ${diffH}h`;
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }
}
