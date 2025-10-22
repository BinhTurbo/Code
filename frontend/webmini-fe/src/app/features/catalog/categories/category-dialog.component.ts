import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { CatalogService } from '../catalog.service';
import { Category } from '../catalog.models';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule],
  template: `
    <div class="dialog-container">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
            />
          </svg>
        </div>
        <div class="header-content">
          <h2 class="dialog-title">
            {{ data ? 'Chỉnh sửa danh mục' : 'Thêm danh mục mới' }}
          </h2>
          <p class="dialog-subtitle">
            {{
              data
                ? 'Cập nhật thông tin danh mục'
                : 'Nhập thông tin danh mục mới'
            }}
          </p>
        </div>
        <button class="close-btn" (click)="ref.close()" type="button">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>

      <!-- Form -->
      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-form">
        <div class="form-group">
          <label class="form-label">
            <span class="label-text">Tên danh mục</span>
            <span class="label-required">*</span>
          </label>
          <div class="input-wrapper">
            <div class="input-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
            <input
              type="text"
              class="form-input"
              placeholder="Nhập tên danh mục (VD: Điện thoại)"
              formControlName="name"
              maxlength="150"
              [class.input-error]="
                form.get('name')?.invalid && form.get('name')?.touched
              "
            />
          </div>
          <div class="char-counter">
            {{ form.get('name')?.value?.length || 0 }}/150 ký tự
          </div>
          <div
            class="error-message"
            *ngIf="form.get('name')?.invalid && form.get('name')?.touched"
          >
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                clip-rule="evenodd"
              />
            </svg>
            <span *ngIf="form.get('name')?.hasError('required')">
              Tên danh mục là bắt buộc
            </span>
            <span *ngIf="form.get('name')?.hasError('minlength')">
              Tên danh mục phải có ít nhất 2 ký tự
            </span>
            <span *ngIf="form.get('name')?.hasError('maxlength')">
              Tên danh mục không được vượt quá 150 ký tự
            </span>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            <span class="label-text">Trạng thái</span>
            <span class="label-required">*</span>
          </label>
          <div class="select-wrapper">
            <div class="select-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
            <select class="form-select" formControlName="status">
              <option value="ACTIVE">Hoạt động</option>
              <option value="INACTIVE">Không hoạt động</option>
            </select>
            <div class="select-arrow">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="dialog-actions">
          <button type="button" class="btn-cancel" (click)="ref.close()">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clip-rule="evenodd"
              />
            </svg>
            Hủy
          </button>
          <button
            type="submit"
            class="btn-submit"
            [disabled]="form.invalid"
            [class.btn-disabled]="form.invalid"
          >
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                clip-rule="evenodd"
              />
            </svg>
            {{ data ? 'Cập nhật' : 'Tạo mới' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        width: 500px;
        max-width: 90vw;
        background: white;
        border-radius: 16px;
        overflow: hidden;
      }

      .dialog-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 1.75rem 2rem;
        display: flex;
        align-items: center;
        gap: 1rem;
        position: relative;
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

      .header-content {
        flex: 1;
      }

      .dialog-title {
        font-size: 1.5rem;
        font-weight: 700;
        color: white;
        margin: 0 0 0.25rem 0;
      }

      .dialog-subtitle {
        font-size: 0.95rem;
        color: rgba(255, 255, 255, 0.9);
        margin: 0;
      }

      .close-btn {
        width: 2.25rem;
        height: 2.25rem;
        background: rgba(255, 255, 255, 0.2);
        border: none;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s ease;
        flex-shrink: 0;
      }

      .close-btn:hover {
        background: rgba(255, 255, 255, 0.3);
        transform: rotate(90deg);
      }

      .close-btn svg {
        width: 1.25rem;
        height: 1.25rem;
        color: white;
      }

      .dialog-form {
        padding: 2rem;
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .form-label {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-size: 0.95rem;
        font-weight: 600;
        color: #374151;
      }

      .label-text {
        color: #1f2937;
      }

      .label-required {
        color: #ef4444;
      }

      .input-wrapper {
        position: relative;
        display: flex;
        align-items: center;
      }

      .input-icon {
        position: absolute;
        left: 1rem;
        width: 1.25rem;
        height: 1.25rem;
        color: #9ca3af;
        pointer-events: none;
        z-index: 1;
      }

      .form-input {
        width: 100%;
        padding: 0.875rem 1rem 0.875rem 3rem;
        border: 2px solid #e5e7eb;
        border-radius: 10px;
        font-size: 0.95rem;
        color: #1f2937;
        transition: all 0.2s ease;
        outline: none;
      }

      .form-input:focus {
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }

      .form-input::placeholder {
        color: #9ca3af;
      }

      .form-input.input-error {
        border-color: #ef4444;
      }

      .form-input.input-error:focus {
        box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
      }

      .char-counter {
        font-size: 0.75rem;
        color: #6b7280;
        text-align: right;
        margin-top: -0.25rem;
      }

      .select-wrapper {
        position: relative;
        display: flex;
        align-items: center;
      }

      .select-icon {
        position: absolute;
        left: 1rem;
        width: 1.25rem;
        height: 1.25rem;
        color: #9ca3af;
        pointer-events: none;
        z-index: 1;
      }

      .form-select {
        width: 100%;
        padding: 0.875rem 3rem 0.875rem 3rem;
        border: 2px solid #e5e7eb;
        border-radius: 10px;
        font-size: 0.95rem;
        color: #1f2937;
        background: white;
        cursor: pointer;
        transition: all 0.2s ease;
        outline: none;
        appearance: none;
      }

      .form-select:focus {
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }

      .select-arrow {
        position: absolute;
        right: 1rem;
        width: 1.25rem;
        height: 1.25rem;
        color: #9ca3af;
        pointer-events: none;
      }

      .error-message {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 0.875rem;
        background: #fef2f2;
        border: 1px solid #fecaca;
        border-radius: 8px;
        font-size: 0.875rem;
        color: #991b1b;
        animation: slideDown 0.2s ease-out;
      }

      @keyframes slideDown {
        from {
          opacity: 0;
          transform: translateY(-5px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .error-message svg {
        width: 1rem;
        height: 1rem;
        flex-shrink: 0;
      }

      .dialog-actions {
        display: flex;
        gap: 0.75rem;
        margin-top: 0.5rem;
      }

      .btn-cancel,
      .btn-submit {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 0.875rem 1.5rem;
        border: none;
        border-radius: 10px;
        font-size: 0.95rem;
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

      .btn-cancel svg {
        width: 1.125rem;
        height: 1.125rem;
      }

      .btn-submit {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
      }

      .btn-submit:hover:not(.btn-disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
      }

      .btn-submit.btn-disabled {
        background: #d1d5db;
        cursor: not-allowed;
        box-shadow: none;
      }

      .btn-submit svg {
        width: 1.125rem;
        height: 1.125rem;
      }

      @media (max-width: 640px) {
        .dialog-container {
          width: 100%;
          border-radius: 0;
        }

        .dialog-header {
          padding: 1.5rem;
        }

        .dialog-title {
          font-size: 1.25rem;
        }

        .dialog-form {
          padding: 1.5rem;
        }

        .dialog-actions {
          flex-direction: column;
        }
      }
    `,
  ],
})
export class CategoryDialog {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CatalogService);
  readonly ref = inject(MatDialogRef<CategoryDialog>);
  readonly data = inject<Category | null>(MAT_DIALOG_DATA);

  // Validation theo DB: name VARCHAR(150) NOT NULL UNIQUE
  form = this.fb.group({
    name: [
      this.data?.name || '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(150)],
    ],
    status: [this.data?.status || 'ACTIVE', Validators.required],
  });

  onSubmit() {
    // Trim whitespace
    const nameControl = this.form.get('name');
    if (nameControl?.value) {
      nameControl.setValue(nameControl.value.trim());
    }

    // Mark all as touched để hiển thị lỗi
    this.form.markAllAsTouched();

    if (this.form.invalid) return;

    const value = this.form.value as Partial<Category>;
    const req = this.data
      ? this.api.updateCategory(this.data.id, value)
      : this.api.createCategory(value);
    req.subscribe(() => this.ref.close(true));
  }
}
