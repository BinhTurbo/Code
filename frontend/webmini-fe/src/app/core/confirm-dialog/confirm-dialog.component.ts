import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'danger' | 'warning' | 'info';
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="confirm-dialog">
      <!-- Header -->
      <div
        class="dialog-header"
        [class.header-danger]="data.type === 'danger'"
        [class.header-warning]="data.type === 'warning'"
        [class.header-info]="!data.type || data.type === 'info'"
      >
        <div class="header-icon">
          @if (data.type === 'danger') {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          } @else if (data.type === 'warning') {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          } @else {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          }
        </div>
        <h2 class="dialog-title">{{ data.title }}</h2>
      </div>

      <!-- Content -->
      <div class="dialog-content">
        <p class="dialog-message">{{ data.message }}</p>
      </div>

      <!-- Actions -->
      <div class="dialog-actions">
        <button class="btn-cancel" (click)="dialogRef.close(false)">
          {{ data.cancelText || 'Hủy' }}
        </button>
        <button
          class="btn-confirm"
          [class.btn-danger]="data.type === 'danger'"
          [class.btn-warning]="data.type === 'warning'"
          [class.btn-info]="!data.type || data.type === 'info'"
          (click)="dialogRef.close(true)"
        >
          {{ data.confirmText || 'Xác nhận' }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .confirm-dialog {
        width: 450px;
        max-width: 90vw;
        background: white;
        border-radius: 16px;
        overflow: hidden;
      }

      .dialog-header {
        padding: 1.75rem 2rem;
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .header-danger {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
      }

      .header-warning {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
      }

      .header-info {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
      }

      .header-icon {
        width: 3rem;
        height: 3rem;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        backdrop-filter: blur(10px);
      }

      .header-icon svg {
        width: 1.75rem;
        height: 1.75rem;
        color: white;
      }

      .dialog-title {
        font-size: 1.375rem;
        font-weight: 700;
        color: white;
        margin: 0;
        flex: 1;
      }

      .dialog-content {
        padding: 2rem;
      }

      .dialog-message {
        font-size: 1rem;
        line-height: 1.6;
        color: #4b5563;
        margin: 0;
      }

      .dialog-actions {
        display: flex;
        gap: 0.75rem;
        padding: 0 2rem 2rem 2rem;
      }

      .btn-cancel,
      .btn-confirm {
        flex: 1;
        padding: 0.875rem 1.5rem;
        border: none;
        border-radius: 10px;
        font-size: 0.9375rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .btn-cancel {
        background: #f3f4f6;
        color: #4b5563;
      }

      .btn-cancel:hover {
        background: #e5e7eb;
        transform: translateY(-1px);
      }

      .btn-confirm {
        color: white;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .btn-confirm:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
      }

      .btn-danger {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
      }

      .btn-warning {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
      }

      .btn-info {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
      }

      @media (max-width: 640px) {
        .confirm-dialog {
          width: 100%;
          border-radius: 0;
        }

        .dialog-header {
          padding: 1.5rem;
        }

        .dialog-title {
          font-size: 1.25rem;
        }

        .dialog-content {
          padding: 1.5rem;
        }

        .dialog-actions {
          flex-direction: column;
          padding: 0 1.5rem 1.5rem 1.5rem;
        }
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent>);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
