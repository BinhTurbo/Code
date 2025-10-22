import { Component, inject, signal } from '@angular/core';
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
  template: `
    <div class="page-container">
      <!-- Header Section -->
      <div class="page-header">
        <div class="header-content">
          <div class="title-section">
            <div class="icon-wrapper">
              <svg
                class="header-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                />
              </svg>
            </div>
            <div>
              <h2 class="page-title">Quản lý danh mục</h2>
              <p class="page-subtitle">Danh sách các danh mục sản phẩm</p>
            </div>
          </div>
          <div class="header-stats">
            <div class="stat-card">
              <div class="stat-value">{{ total() }}</div>
              <div class="stat-label">Tổng danh mục</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Search and Actions Section -->
      <div class="toolbar">
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
              clip-rule="evenodd"
            />
          </svg>
          <input
            [(ngModel)]="q"
            placeholder="Tìm kiếm danh mục..."
            class="search-input"
            (keyup.enter)="load()"
          />
          <button class="search-btn" (click)="load()">Tìm kiếm</button>
        </div>
        <button *ngIf="isAdmin()" class="btn-add" (click)="openDialog()">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
              clip-rule="evenodd"
            />
          </svg>
          Thêm danh mục
        </button>
      </div>

      <!-- Table Section -->
      <div class="table-container">
        <div class="table-wrapper">
          <table mat-table [dataSource]="rows()" class="custom-table">
            <!-- ID Column -->
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef class="table-header">
                <span class="header-text">ID</span>
              </th>
              <td mat-cell *matCellDef="let r" class="table-cell">
                <span class="cell-id">{{ r.id }}</span>
              </td>
            </ng-container>

            <!-- Name Column -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef class="table-header">
                <span class="header-text">Tên danh mục</span>
              </th>
              <td mat-cell *matCellDef="let r" class="table-cell">
                <div class="category-name">
                  <div class="category-avatar">
                    {{ r.name.charAt(0).toUpperCase() }}
                  </div>
                  <span class="category-text">{{ r.name }}</span>
                </div>
              </td>
            </ng-container>

            <!-- Status Column -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef class="table-header">
                <span class="header-text">Trạng thái</span>
              </th>
              <td mat-cell *matCellDef="let r" class="table-cell">
                <span
                  class="status-badge"
                  [class.status-active]="r.status === 'ACTIVE'"
                  [class.status-inactive]="r.status === 'INACTIVE'"
                >
                  <span class="status-dot"></span>
                  {{ r.status === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động' }}
                </span>
              </td>
            </ng-container>

            <!-- Action Column -->
            <ng-container matColumnDef="action">
              <th mat-header-cell *matHeaderCellDef class="table-header">
                <span class="header-text">Thao tác</span>
              </th>
              <td mat-cell *matCellDef="let r" class="table-cell">
                <div class="action-buttons" *ngIf="isAdmin()">
                  <button
                    class="btn-edit"
                    (click)="openDialog(r)"
                    title="Chỉnh sửa"
                  >
                    <svg
                      class="action-icon"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path
                        d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"
                      />
                    </svg>
                    Sửa
                  </button>
                  <button class="btn-delete" (click)="remove(r)" title="Xóa">
                    <svg
                      class="action-icon"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path
                        fill-rule="evenodd"
                        d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                        clip-rule="evenodd"
                      />
                    </svg>
                    Xóa
                  </button>
                </div>
                <div class="no-actions" *ngIf="!isAdmin()">
                  <span class="text-muted">Không có quyền</span>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr
              mat-row
              *matRowDef="let row; columns: cols"
              class="table-row"
            ></tr>
          </table>

          <!-- Empty State -->
          <div class="empty-state" *ngIf="rows().length === 0">
            <svg
              class="empty-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
              />
            </svg>
            <h3 class="empty-title">Không có danh mục nào</h3>
            <p class="empty-text">
              {{
                q
                  ? 'Không tìm thấy kết quả phù hợp'
                  : 'Bắt đầu bằng cách thêm danh mục mới'
              }}
            </p>
            <button
              *ngIf="isAdmin() && !q"
              class="btn-add-empty"
              (click)="openDialog()"
            >
              Thêm danh mục đầu tiên
            </button>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination-wrapper" *ngIf="rows().length > 0">
          <mat-paginator
            [length]="total()"
            [pageSize]="size()"
            [pageSizeOptions]="[5, 10, 20, 50]"
            (page)="pageChange($event)"
            class="custom-paginator"
          >
          </mat-paginator>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .page-container {
        min-height: 100vh;
        background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
        padding: 1.5rem;
      }

      /* Header Section */
      .page-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 16px;
        padding: 2rem;
        margin-bottom: 2rem;
        box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
        animation: fadeIn 0.5s ease-out;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(-10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .header-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 1.5rem;
      }

      .title-section {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .icon-wrapper {
        background: rgba(255, 255, 255, 0.2);
        padding: 0.875rem;
        border-radius: 12px;
        backdrop-filter: blur(10px);
      }

      .header-icon {
        width: 2rem;
        height: 2rem;
        color: white;
      }

      .page-title {
        font-size: 1.875rem;
        font-weight: 700;
        color: white;
        margin: 0;
      }

      .page-subtitle {
        font-size: 0.95rem;
        color: rgba(255, 255, 255, 0.9);
        margin: 0.25rem 0 0 0;
      }

      .header-stats {
        display: flex;
        gap: 1rem;
      }

      .stat-card {
        background: rgba(255, 255, 255, 0.2);
        padding: 1rem 1.5rem;
        border-radius: 12px;
        backdrop-filter: blur(10px);
        text-align: center;
        min-width: 120px;
      }

      .stat-value {
        font-size: 2rem;
        font-weight: 700;
        color: white;
        line-height: 1;
      }

      .stat-label {
        font-size: 0.875rem;
        color: rgba(255, 255, 255, 0.9);
        margin-top: 0.25rem;
      }

      /* Toolbar Section */
      .toolbar {
        display: flex;
        gap: 1rem;
        margin-bottom: 1.5rem;
        flex-wrap: wrap;
      }

      .search-box {
        flex: 1;
        min-width: 300px;
        display: flex;
        align-items: center;
        background: white;
        border-radius: 12px;
        padding: 0.5rem 1rem;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        transition: box-shadow 0.3s ease;
      }

      .search-box:focus-within {
        box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
      }

      .search-icon {
        width: 1.25rem;
        height: 1.25rem;
        color: #9ca3af;
        margin-right: 0.75rem;
      }

      .search-input {
        flex: 1;
        border: none;
        outline: none;
        font-size: 0.95rem;
        color: #374151;
      }

      .search-input::placeholder {
        color: #9ca3af;
      }

      .search-btn {
        padding: 0.5rem 1.25rem;
        background: #667eea;
        color: white;
        border: none;
        border-radius: 8px;
        font-size: 0.9rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
        margin-left: 0.5rem;
      }

      .search-btn:hover {
        background: #5568d3;
        transform: translateY(-1px);
      }

      .btn-add {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 1.5rem;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
        border: none;
        border-radius: 12px;
        font-size: 0.95rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
      }

      .btn-add:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
      }

      .btn-icon {
        width: 1.25rem;
        height: 1.25rem;
      }

      /* Table Section */
      .table-container {
        background: white;
        border-radius: 16px;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
        overflow: hidden;
      }

      .table-wrapper {
        overflow-x: auto;
      }

      .custom-table {
        width: 100%;
        border-collapse: collapse;
      }

      .table-header {
        background: #f9fafb;
        padding: 1rem 1.5rem;
        border-bottom: 2px solid #e5e7eb;
        font-weight: 600;
        color: #374151;
        font-size: 0.875rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .table-cell {
        padding: 1.25rem 1.5rem;
        border-bottom: 1px solid #f3f4f6;
        color: #4b5563;
      }

      .table-row {
        transition: background-color 0.2s ease;
      }

      .table-row:hover {
        background-color: #f9fafb;
      }

      .cell-id {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 40px;
        padding: 0.25rem 0.75rem;
        background: #f3f4f6;
        color: #6b7280;
        border-radius: 6px;
        font-size: 0.875rem;
        font-weight: 600;
      }

      .category-name {
        display: flex;
        align-items: center;
        gap: 0.875rem;
      }

      .category-avatar {
        width: 2.5rem;
        height: 2.5rem;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        font-weight: 700;
        font-size: 1.1rem;
      }

      .category-text {
        font-weight: 600;
        color: #1f2937;
        font-size: 0.95rem;
      }

      .status-badge {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1rem;
        border-radius: 20px;
        font-size: 0.875rem;
        font-weight: 600;
      }

      .status-active {
        background: #d1fae5;
        color: #065f46;
      }

      .status-inactive {
        background: #fee2e2;
        color: #991b1b;
      }

      .status-dot {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: 50%;
        display: inline-block;
      }

      .status-active .status-dot {
        background: #10b981;
        animation: pulse 2s infinite;
      }

      .status-inactive .status-dot {
        background: #ef4444;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }

      .action-buttons {
        display: flex;
        gap: 0.5rem;
      }

      .btn-edit,
      .btn-delete {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 8px;
        font-size: 0.875rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .btn-edit {
        background: #dbeafe;
        color: #1e40af;
      }

      .btn-edit:hover {
        background: #bfdbfe;
        transform: translateY(-1px);
      }

      .btn-delete {
        background: #fee2e2;
        color: #991b1b;
      }

      .btn-delete:hover {
        background: #fecaca;
        transform: translateY(-1px);
      }

      .action-icon {
        width: 1rem;
        height: 1rem;
      }

      .no-actions {
        color: #9ca3af;
        font-size: 0.875rem;
        font-style: italic;
      }

      /* Empty State */
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 4rem 2rem;
        text-align: center;
      }

      .empty-icon {
        width: 5rem;
        height: 5rem;
        color: #d1d5db;
        margin-bottom: 1.5rem;
      }

      .empty-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #374151;
        margin: 0 0 0.5rem 0;
      }

      .empty-text {
        color: #6b7280;
        margin: 0 0 1.5rem 0;
      }

      .btn-add-empty {
        padding: 0.75rem 1.5rem;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        border-radius: 8px;
        font-size: 0.95rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
      }

      .btn-add-empty:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
      }

      /* Pagination */
      .pagination-wrapper {
        border-top: 1px solid #f3f4f6;
        padding: 1rem;
      }

      ::ng-deep .custom-paginator {
        background: transparent;
      }

      ::ng-deep .mat-mdc-paginator-container {
        justify-content: center;
      }

      /* Responsive */
      @media (max-width: 768px) {
        .page-container {
          padding: 1rem;
        }

        .page-header {
          padding: 1.5rem;
        }

        .header-content {
          flex-direction: column;
          align-items: flex-start;
        }

        .page-title {
          font-size: 1.5rem;
        }

        .toolbar {
          flex-direction: column;
        }

        .search-box {
          min-width: 100%;
        }

        .btn-add {
          width: 100%;
          justify-content: center;
        }

        .table-cell,
        .table-header {
          padding: 0.875rem 1rem;
          font-size: 0.875rem;
        }

        .action-buttons {
          flex-direction: column;
        }

        .btn-edit,
        .btn-delete {
          width: 100%;
          justify-content: center;
        }
      }
    `,
  ],
})
export class CategoriesComponent {
  private api = inject(CatalogService);
  private dialog = inject(MatDialog);
  private auth = inject(AuthService);

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
    if (confirm('Xoá category này?')) {
      this.api.deleteCategory(item.id).subscribe(() => this.load());
    }
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
