import { Component, OnInit, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationResponse } from '../../core/models/notification.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe],
  templateUrl: './notifications.component.html',
})
export class NotificationsComponent implements OnInit {
  private svc = inject(NotificationService);
  notifications: NotificationResponse[] = [];

  ngOnInit(): void {
    this.svc.getAll().subscribe(n => (this.notifications = n));
    this.svc.markAllRead().subscribe();
  }
}
