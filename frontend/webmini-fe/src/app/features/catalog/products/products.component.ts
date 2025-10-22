import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { HttpClient } from '@angular/common/http';
import { CatalogService } from '../catalog.service';
import { Product } from '../catalog.models';
import { ProductDialog } from './product-dialog.component';
import { AuthService } from '../../auth/auth.service';
import { ToastService } from '../../../core/toast.service';
import { ConfirmDialogComponent } from '../../../core/confirm-dialog/confirm-dialog.component';
import type { ConfirmDialogData } from '../../../core/confirm-dialog/confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'app-products',
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatButtonModule,
    MatMenuModule,
  ],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss'],
})
export class ProductsComponent implements OnInit {
  private readonly api = inject(CatalogService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  q = '';
  cols = ['sku', 'name', 'categoryName', 'price', 'stock', 'status', 'action'];
  rows = signal<Product[]>([]);
  total = signal(0);
  size = signal(10);
  page = signal(0);

  load() {
    this.api
      .listProducts({ q: this.q }, this.page(), this.size())
      .subscribe((res) => {
        this.rows.set(res.content);
        this.total.set(res.totalElements);
      });
  }

  openDialog(item?: Product) {
    const ref = this.dialog.open(ProductDialog, { data: item });
    ref.afterClosed().subscribe((ok) => ok && this.load());
  }

  remove(item: Product) {
    const dialogData: ConfirmDialogData = {
      title: 'Xác nhận xóa sản phẩm',
      message: `Bạn có chắc chắn muốn xóa sản phẩm "${item.name}" (SKU: ${item.sku})? Hành động này không thể hoàn tác.`,
      confirmText: 'Xóa',
      cancelText: 'Hủy',
      type: 'danger',
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: dialogData,
      width: '450px',
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.api.deleteProduct(item.id).subscribe({
          next: () => {
            this.toast.success('Xóa sản phẩm thành công!');
            this.load();
          },
          error: (err) => {
            const msg = err?.error?.message || 'Không thể xóa sản phẩm này.';
            this.toast.error(msg);
          },
        });
      }
    });
  }

  pageChange(e: PageEvent) {
    this.page.set(e.pageIndex);
    this.size.set(e.pageSize);
    this.load();
  }

  exportProducts(format: 'csv' | 'pdf') {
    const url = `http://localhost:8081/api/products/export?format=${format}`;
    // Use HttpClient so that auth interceptor (if any) attaches tokens/cookies
    this.http.get(url, { responseType: 'blob' as 'json' }).subscribe({
      next: (data: any) => {
        const blob = data as Blob;
        const filename = `products_${this.currentTimestamp()}.${format}`;
        const objectUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = objectUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(objectUrl);
        this.toast.success(`Xuất dữ liệu ${format.toUpperCase()} thành công!`);
      },
      error: () => {
        this.toast.error('Xuất dữ liệu thất bại. Vui lòng thử lại!');
      },
    });
  }

  private currentTimestamp() {
    const d = new Date();
    const yyyy = d.getFullYear();
    const MM = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const HH = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    return `${yyyy}${MM}${dd}_${HH}${mm}${ss}`;
  }

  isAdmin() {
    return this.auth.payload()?.roles?.includes('ROLE_ADMIN');
  }

  ngOnInit() {
    this.load();
  }
}
