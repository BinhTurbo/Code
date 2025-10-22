import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { CatalogService } from '../catalog.service';
import { Category } from '../catalog.models';
import { CategoryDialog } from './category-dialog.component';
import { AuthService } from '../../auth/auth.service';
import { ToastService } from '../../../core/toast.service';
import { ConfirmDialogComponent } from '../../../core/confirm-dialog/confirm-dialog.component';
import type { ConfirmDialogData } from '../../../core/confirm-dialog/confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'app-categories',
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatButtonModule,
  ],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.scss'],
})
export class CategoriesComponent implements OnInit {
  private readonly api = inject(CatalogService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  q = '';
  cols = ['id', 'name', 'status', 'action'];
  rows = signal<Category[]>([]);
  total = signal(0);
  size = signal(10);
  page = signal(0);

  load() {
    this.api
      .listCategories(this.q, '', this.page(), this.size())
      .subscribe((res) => {
        this.rows.set(res.content);
        this.total.set(res.totalElements);
      });
  }

  openDialog(item?: Category) {
    const ref = this.dialog.open(CategoryDialog, { data: item });
    ref.afterClosed().subscribe((ok) => ok && this.load());
  }

  remove(item: Category) {
    const dialogData: ConfirmDialogData = {
      title: 'Xác nhận xóa danh mục',
      message: `Bạn có chắc chắn muốn xóa danh mục "${item.name}"? Hành động này không thể hoàn tác.`,
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
        this.api.deleteCategory(item.id).subscribe({
          next: () => {
            this.toast.success('Xóa danh mục thành công!');
            this.load();
          },
          error: (err) => {
            const msg =
              err?.error?.message ||
              'Không thể xóa danh mục này. Có thể đang có sản phẩm thuộc danh mục.';
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

  isAdmin() {
    return this.auth.payload()?.roles?.includes('ROLE_ADMIN');
  }

  ngOnInit() {
    this.load();
  }
}
