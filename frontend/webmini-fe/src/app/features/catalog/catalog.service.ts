import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Category, Product, PageResponse } from './catalog.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // ---- Categories ----
  listCategories(
    q = '',
    status = '',
    page = 0,
    size = 10,
    sort = 'createdAt,desc'
  ): Observable<PageResponse<Category>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    if (q) params = params.set('q', q);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<Category>>(`${this.base}/categories`, {
      params,
    });
  }
  createCategory(body: Partial<Category>) {
    return this.http.post<Category>(`${this.base}/categories`, body);
  }
  updateCategory(id: number, body: Partial<Category>) {
    return this.http.put<Category>(`${this.base}/categories/${id}`, body);
  }
  deleteCategory(id: number) {
    return this.http.delete<void>(`${this.base}/categories/${id}`);
  }

  // ---- Products ----
  listProducts(
    filter: any = {},
    page = 0,
    size = 10,
    sort = 'createdAt,desc'
  ): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    for (const [k, v] of Object.entries(filter)) {
      if (v !== undefined && v !== null && v !== '')
        params = params.set(k, String(v));
    }
    return this.http.get<PageResponse<Product>>(`${this.base}/products`, {
      params,
    });
  }
  createProduct(body: Partial<Product>) {
    return this.http.post<Product>(`${this.base}/products`, body);
  }
  updateProduct(id: number, body: Partial<Product>) {
    return this.http.put<Product>(`${this.base}/products/${id}`, body);
  }
  deleteProduct(id: number) {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }
}
