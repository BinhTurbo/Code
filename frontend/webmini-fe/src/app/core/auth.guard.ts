
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStoreService } from '../features/auth/token-store.service';

export const canActivateAuth: CanActivateFn = () => {
  const tokens = inject(TokenStoreService);
  const router = inject(Router);
  return tokens.access() ? true : router.createUrlTree(['/auth/login']);
};
