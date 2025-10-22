import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: 'dashboard.component.html',
  styleUrls: ['dashboard.component.scss'],
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);

  getUserName(): string {
    const payload = this.auth.payload();
    return payload?.sub || 'User';
  }

  logout() {
    this.auth.logout();
  }
}
