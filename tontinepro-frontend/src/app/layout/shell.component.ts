import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../shared/components/header/header.component';
import { BottomNavComponent } from '../shared/components/bottom-nav/bottom-nav.component';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, HeaderComponent, BottomNavComponent],
  template: `
    <app-header />

    <main class="min-h-screen bg-gray-50 px-4 py-4 max-w-2xl mx-auto">
      <router-outlet />
    </main>

    <app-bottom-nav />
  `,
})
export class ShellComponent {}
