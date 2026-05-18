import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { NotificationResponse } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private api = `${environment.apiUrl}/notifications`;

  unreadCount = signal(0);

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<NotificationResponse[]>(this.api);
  }

  getUnreadCount() {
    return this.http.get<{ nonLues: number }>(`${this.api}/non-lues/count`).pipe(
      tap(r => this.unreadCount.set(r.nonLues))
    );
  }

  markRead(id: string) {
    return this.http.patch<NotificationResponse>(`${this.api}/${id}/lire`, {}).pipe(
      tap(() => this.unreadCount.update(n => Math.max(0, n - 1)))
    );
  }

  markAllRead() {
    return this.http.patch(`${this.api}/lire-tout`, {}).pipe(
      tap(() => this.unreadCount.set(0))
    );
  }
}
