import { Injectable, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { LoginRequest, LoginResponse, JwtPayload } from './auth.models';
import { TokenStoreService } from './token-store.service';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private tokens = inject(TokenStoreService);

  login(body: LoginRequest) {
    const url = `${environment.apiUrl}/auth/login`;
    return this.http.post<LoginResponse>(url, body);
  }

  applyLogin(res: LoginResponse) {
    this.tokens.set(res.accessToken, res.refreshToken, res.accessExpiresInSec);
  }

  logout() {
    this.tokens.clear();
    this.router.navigateByUrl('/auth/login');
  }

  register(body: { username: string; password: string; fullName: string; }) {
    const url = `${environment.apiUrl}/auth/register`;
    return this.http.post<LoginResponse>(url, body);
  }

  refreshToken(refreshToken: string) {
    const url = `${environment.apiUrl}/auth/refresh`;
    return this.http.post<LoginResponse>(url, { refreshToken });
  }


  readonly payload = computed<JwtPayload | null>(() => {
    const access = this.tokens.access();
    if (!access) return null;
    try {
      const base64 = access.split('.')[1];
      const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as JwtPayload;
    } catch { return null; }
  });
}
