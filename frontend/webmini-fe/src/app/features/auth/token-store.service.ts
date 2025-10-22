import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenStoreService {
  private accessKey = 'accessToken';
  private refreshKey = 'refreshToken';

  private _access = signal<string | null>(localStorage.getItem(this.accessKey));
  private _refresh = signal<string | null>(localStorage.getItem(this.refreshKey));

  access() { return this._access(); }
  refresh() { return this._refresh(); }

  set(access: string, refresh: string, _expiresSec?: number) {
    localStorage.setItem(this.accessKey, access);
    localStorage.setItem(this.refreshKey, refresh);
    this._access.set(access);
    this._refresh.set(refresh);
  }

  clear() {
    localStorage.removeItem(this.accessKey);
    localStorage.removeItem(this.refreshKey);
    this._access.set(null);
    this._refresh.set(null);
  }
}
