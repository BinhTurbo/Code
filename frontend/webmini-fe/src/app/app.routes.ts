import { Routes } from '@angular/router';
import { canActivateAuth } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent
      ),
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
  },
  {
    path: '',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
  },
  {
    path: 'categories',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/catalog/categories/categories.component').then(
        (m) => m.CategoriesComponent
      ),
  },
  {
    path: 'categories/:id',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/catalog/categories/category-detail.component').then(
        (m) => m.CategoryDetailComponent
      ),
  },
  {
    path: 'products',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/catalog/products/products.component').then(
        (m) => m.ProductsComponent
      ),
  },
  {
    path: 'products/:id',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/catalog/products/product-detail.component').then(
        (m) => m.ProductDetailComponent
      ),
  },
  { path: '**', redirectTo: '' },
];
