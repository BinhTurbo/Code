import {
  HttpInterceptorFn,
  HttpErrorResponse,
  HttpRequest,
  HttpHandlerFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStoreService } from '../features/auth/token-store.service';
import { AuthService } from '../features/auth/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<any>,
  next: HttpHandlerFn
) => {
  const tokens = inject(TokenStoreService);
  const auth = inject(AuthService);
  const router = inject(Router);

  const access = tokens.access();

  const isAuthUrl = req.url.includes('/auth/');
  const isRefreshCall = req.url.includes('/auth/refresh');

  if (access && !isAuthUrl) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${access}` } });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        (error.status === 401 || error.status === 403) &&
        !isAuthUrl &&
        !isRefreshCall
      ) {
        const refresh = tokens.refresh();
        if (!refresh) {
          auth.logout();
          return throwError(() => error);
        }

        return auth.refreshToken(refresh).pipe(
          switchMap((res) => {
            auth.applyLogin(res);
            const retry = req.clone({
              setHeaders: { Authorization: `Bearer ${res.accessToken}` },
            });
            return next(retry);
          }),
          catchError(() => {
            auth.logout();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
