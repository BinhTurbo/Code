import { Component, inject, OnInit, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  FormControl,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { CatalogService } from '../catalog.service';
import { Product, Category } from '../catalog.models';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { startWith } from 'rxjs';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatAutocompleteModule,
    MatInputModule,
    MatOptionModule,
  ],
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
              d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
            />
          </svg>
        </div>
        <div class="header-content">
          <h2 class="dialog-title">
            {{ data ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới' }}
          </h2>
          <p class="dialog-subtitle">
            {{
              data
                ? 'Cập nhật thông tin sản phẩm'
                : 'Nhập thông tin sản phẩm mới'
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
        <!-- SKU: VARCHAR(100) NOT NULL UNIQUE -->
        <div class="form-group">
          <label class="form-label">
            <span class="label-text">Mã SKU</span>
            <span class="label-required">*</span>
            <span class="label-hint" *ngIf="data">(Không thể thay đổi)</span>
          </label>
          <div class="input-wrapper">
            <div class="input-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
            <input
              type="text"
              class="form-input"
              [class.input-readonly]="!!data"
              placeholder="Nhập mã SKU (VD: PROD-001)"
              formControlName="sku"
              maxlength="100"
              [readonly]="!!data"
              [class.input-error]="
                form.get('sku')?.invalid && form.get('sku')?.touched
              "
            />
          </div>
          <div class="char-counter">
            {{ form.get('sku')?.value?.length || 0 }}/100 ký tự
          </div>
          <div
            class="error-message"
            *ngIf="form.get('sku')?.invalid && form.get('sku')?.touched"
          >
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                clip-rule="evenodd"
              />
            </svg>
            <span *ngIf="form.get('sku')?.hasError('required')">
              Mã SKU là bắt buộc
            </span>
            <span *ngIf="form.get('sku')?.hasError('minlength')">
              Mã SKU phải có ít nhất 3 ký tự
            </span>
            <span *ngIf="form.get('sku')?.hasError('maxlength')">
              Mã SKU không được vượt quá 100 ký tự
            </span>
          </div>
        </div>

        <!-- Name: VARCHAR(200) NOT NULL -->
        <div class="form-group">
          <label class="form-label">
            <span class="label-text">Tên sản phẩm</span>
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
              placeholder="Nhập tên sản phẩm"
              formControlName="name"
              maxlength="200"
              [class.input-error]="
                form.get('name')?.invalid && form.get('name')?.touched
              "
            />
          </div>
          <div class="char-counter">
            {{ form.get('name')?.value?.length || 0 }}/200 ký tự
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
              Tên sản phẩm là bắt buộc
            </span>
            <span *ngIf="form.get('name')?.hasError('minlength')">
              Tên sản phẩm phải có ít nhất 2 ký tự
            </span>
            <span *ngIf="form.get('name')?.hasError('maxlength')">
              Tên sản phẩm không được vượt quá 200 ký tự
            </span>
          </div>
        </div>

        <!-- Category Search -->
        <div class="form-group">
          <label class="form-label">
            <span class="label-text">Danh mục</span>
            <span class="label-required">*</span>
          </label>
          <div class="autocomplete-wrapper">
            <div class="input-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
            <input
              type="text"
              class="form-input autocomplete-input"
              placeholder="Gõ để tìm danh mục..."
              [formControl]="categorySearchCtrl"
              [matAutocomplete]="auto"
              autocomplete="off"
            />
            <div class="loading-icon" *ngIf="loadingCats()">
              <svg class="spinner" viewBox="0 0 24 24">
                <circle
                  class="spinner-circle"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                  fill="none"
                />
              </svg>
            </div>
            <mat-autocomplete
              #auto="matAutocomplete"
              (optionSelected)="onCategorySelected($event.option.value)"
              class="custom-autocomplete"
            >
              <mat-option *ngIf="loadingCats()" disabled class="loading-option">
                <span>Đang tải...</span>
              </mat-option>
              <ng-container *ngIf="!loadingCats()">
                <mat-option
                  *ngFor="let c of categories()"
                  [value]="c"
                  class="category-option"
                >
                  <div class="category-option-content">
                    <div class="category-avatar">{{ c.name.charAt(0) }}</div>
                    <span>{{ c.name }}</span>
                  </div>
                </mat-option>
                <mat-option
                  *ngIf="categories().length === 0"
                  disabled
                  class="empty-option"
                >
                  <span>Không tìm thấy danh mục</span>
                </mat-option>
              </ng-container>
            </mat-autocomplete>
          </div>

          <div class="selected-category" *ngIf="selectedCategoryName()">
            <div class="selected-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
            <span>{{ selectedCategoryName() }}</span>
          </div>

          <div
            class="error-message"
            *ngIf="
              form.get('categoryId')?.invalid && form.get('categoryId')?.touched
            "
          >
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                clip-rule="evenodd"
              />
            </svg>
            <span>Vui lòng chọn danh mục</span>
          </div>
        </div>

        <!-- Price & Stock Row: price DECIMAL(18,2) >= 0, stock INT >= 0 -->
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">
              <span class="label-text">Giá bán</span>
              <span class="label-required">*</span>
            </label>
            <div class="input-wrapper">
              <div class="input-icon">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path
                    d="M8.433 7.418c.155-.103.346-.196.567-.267v1.698a2.305 2.305 0 01-.567-.267C8.07 8.34 8 8.114 8 8c0-.114.07-.34.433-.582zM11 12.849v-1.698c.22.071.412.164.567.267.364.243.433.468.433.582 0 .114-.07.34-.433.582a2.305 2.305 0 01-.567.267z"
                  />
                  <path
                    fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-13a1 1 0 10-2 0v.092a4.535 4.535 0 00-1.676.662C6.602 6.234 6 7.009 6 8c0 .99.602 1.765 1.324 2.246.48.32 1.054.545 1.676.662v1.941c-.391-.127-.68-.317-.843-.504a1 1 0 10-1.51 1.31c.562.649 1.413 1.076 2.353 1.253V15a1 1 0 102 0v-.092a4.535 4.535 0 001.676-.662C13.398 13.766 14 12.991 14 12c0-.99-.602-1.765-1.324-2.246A4.535 4.535 0 0011 9.092V7.151c.391.127.68.317.843.504a1 1 0 101.511-1.31c-.563-.649-1.413-1.076-2.354-1.253V5z"
                    clip-rule="evenodd"
                  />
                </svg>
              </div>
              <input
                type="number"
                class="form-input"
                placeholder="0"
                formControlName="price"
                min="0"
                step="0.01"
                [class.input-error]="
                  form.get('price')?.invalid && form.get('price')?.touched
                "
              />
              <span class="input-suffix">đ</span>
            </div>
            <div
              class="error-message"
              *ngIf="form.get('price')?.invalid && form.get('price')?.touched"
            >
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                  clip-rule="evenodd"
                />
              </svg>
              <span *ngIf="form.get('price')?.hasError('required')">
                Giá bán là bắt buộc
              </span>
              <span *ngIf="form.get('price')?.hasError('min')">
                Giá bán phải >= 0
              </span>
              <span *ngIf="form.get('price')?.hasError('max')">
                Giá bán không được vượt quá 9,999,999,999,999,999.99
              </span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="label-text">Tồn kho</span>
              <span class="label-required">*</span>
            </label>
            <div class="input-wrapper">
              <div class="input-icon">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path
                    d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
                  />
                </svg>
              </div>
              <input
                type="number"
                class="form-input"
                placeholder="0"
                formControlName="stock"
                min="0"
                step="1"
                [class.input-error]="
                  form.get('stock')?.invalid && form.get('stock')?.touched
                "
              />
            </div>
            <div
              class="error-message"
              *ngIf="form.get('stock')?.invalid && form.get('stock')?.touched"
            >
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                  clip-rule="evenodd"
                />
              </svg>
              <span *ngIf="form.get('stock')?.hasError('required')">
                Tồn kho là bắt buộc
              </span>
              <span *ngIf="form.get('stock')?.hasError('min')">
                Tồn kho phải >= 0
              </span>
              <span *ngIf="form.get('stock')?.hasError('max')">
                Tồn kho không được vượt quá 2,147,483,647
              </span>
            </div>
          </div>
        </div>

        <!-- Status -->
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
      /* Override Material Dialog Container */
      ::ng-deep .cdk-overlay-pane {
        max-width: 95vw !important;
      }

      ::ng-deep .mat-mdc-dialog-container {
        --mdc-dialog-container-shape: 16px;
        padding: 0 !important;
        overflow: visible !important;
      }

      ::ng-deep .mat-mdc-dialog-surface {
        padding: 0 !important;
        border-radius: 16px !important;
        overflow: hidden !important;
      }

      ::ng-deep .mdc-dialog__container {
        width: auto !important;
      }

      .dialog-container {
        width: 700px;
        max-width: 100%;
        background: white;
        border-radius: 16px;
        overflow: hidden;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
      }

      /* Header */
      .dialog-header {
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        padding: 1.75rem 2rem;
        display: flex;
        align-items: center;
        gap: 1rem;
        position: relative;
        flex-shrink: 0;
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
        min-width: 0;
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

      /* Form */
      .dialog-form {
        padding: 2rem;
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
        overflow-y: auto;
        overflow-x: hidden;
        flex: 1;
      }

      .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        min-width: 0;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1.5rem;
      }

      .form-label {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        flex-wrap: wrap;
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

      .label-hint {
        font-size: 0.875rem;
        color: #6b7280;
        font-weight: 400;
        font-style: italic;
      }

      /* Input */
      .input-wrapper {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
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
        background: white;
        box-sizing: border-box;
      }

      .form-input:focus {
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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

      .form-input.input-readonly {
        background: #f9fafb;
        cursor: not-allowed;
        color: #6b7280;
      }

      .input-suffix {
        position: absolute;
        right: 1rem;
        font-size: 0.95rem;
        color: #6b7280;
        font-weight: 600;
        pointer-events: none;
      }

      /* Autocomplete */
      .autocomplete-wrapper {
        position: relative;
        width: 100%;
      }

      .autocomplete-input {
        padding-right: 3rem;
      }

      .loading-icon {
        position: absolute;
        right: 1rem;
        top: 50%;
        transform: translateY(-50%);
        width: 1.25rem;
        height: 1.25rem;
      }

      .spinner {
        animation: spin 1s linear infinite;
        color: #3b82f6;
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .spinner-circle {
        stroke-dasharray: 50;
        stroke-dashoffset: 25;
      }

      /* Selected Category */
      .selected-category {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 1rem;
        background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
        border: 2px solid #93c5fd;
        border-radius: 10px;
        font-size: 0.95rem;
        color: #1e40af;
        font-weight: 600;
        word-break: break-word;
      }

      .selected-icon {
        width: 1.25rem;
        height: 1.25rem;
        color: #2563eb;
        flex-shrink: 0;
      }

      /* Select */
      .select-wrapper {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
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
        box-sizing: border-box;
      }

      .form-select:focus {
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }

      .select-arrow {
        position: absolute;
        right: 1rem;
        width: 1.25rem;
        height: 1.25rem;
        color: #9ca3af;
        pointer-events: none;
      }

      /* Error Message */
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

      /* Actions */
      .dialog-actions {
        display: flex;
        gap: 1rem;
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
        white-space: nowrap;
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

      /* Autocomplete Panel Styles */
      ::ng-deep .custom-autocomplete {
        margin-top: 0.5rem;
      }

      ::ng-deep .mat-mdc-autocomplete-panel {
        border-radius: 12px !important;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15) !important;
        overflow: hidden;
        max-width: 650px;
      }

      ::ng-deep .category-option {
        padding: 0.75rem 1rem !important;
        transition: all 0.2s ease;
      }

      ::ng-deep .category-option:hover {
        background: #f3f4f6 !important;
      }

      ::ng-deep .category-option-content {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      ::ng-deep .category-avatar {
        width: 2rem;
        height: 2rem;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        font-weight: 700;
        font-size: 0.875rem;
        flex-shrink: 0;
      }

      ::ng-deep .loading-option,
      ::ng-deep .empty-option {
        padding: 1rem !important;
        text-align: center;
        color: #6b7280;
        font-style: italic;
      }

      /* Responsive */
      @media (max-width: 768px) {
        .dialog-container {
          width: 100%;
          max-height: 100vh;
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

        .form-row {
          grid-template-columns: 1fr;
          gap: 1rem;
        }

        .dialog-actions {
          flex-direction: column;
          gap: 0.75rem;
        }
      }

      @media (max-width: 640px) {
        .dialog-container {
          max-width: 100vw;
        }

        .dialog-header {
          padding: 1.25rem;
        }

        .dialog-form {
          padding: 1.25rem;
        }
      }

      .char-counter {
        font-size: 0.75rem;
        color: #6b7280;
        text-align: right;
        margin-top: -0.25rem;
      }

      .input-suffix {
        position: absolute;
        right: 1rem;
        color: #6b7280;
        font-weight: 600;
        pointer-events: none;
      }

      .form-input:has(+ .input-suffix) {
        padding-right: 2.5rem;
      }
    `,
  ],
})
export class ProductDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CatalogService);
  readonly ref = inject(MatDialogRef<ProductDialog>);
  readonly data = inject<Product | null>(MAT_DIALOG_DATA);

  // Validation theo DB schema
  form = this.fb.group({
    // sku VARCHAR(100) NOT NULL UNIQUE
    sku: [
      this.data?.sku || '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100)
      ]
    ],
    // name VARCHAR(200) NOT NULL
    name: [
      this.data?.name || '',
      [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(200)
      ]
    ],
    // category_id BIGINT NOT NULL
    categoryId: [this.data?.categoryId || null, Validators.required],
    // price DECIMAL(18,2) NOT NULL DEFAULT 0, CHECK (price >= 0)
    price: [
      this.data?.price ?? 0,
      [
        Validators.required,
        Validators.min(0),
        Validators.max(9999999999999999.99) // DECIMAL(18,2) max
      ]
    ],
    // stock INT NOT NULL DEFAULT 0, CHECK (stock >= 0)
    stock: [
      this.data?.stock ?? 0,
      [
        Validators.required,
        Validators.min(0),
        Validators.max(2147483647) // INT max
      ]
    ],
    // status ENUM('ACTIVE','INACTIVE')
    status: [this.data?.status || 'ACTIVE', Validators.required],
  });

  categorySearchCtrl = new FormControl<string>(this.data?.categoryName || '', {
    nonNullable: true,
  });

  categories = signal<Category[]>([]);
  loadingCats = signal<boolean>(false);
  selectedCategoryName = signal<string>(this.data?.categoryName || '');

  ngOnInit() {
    this.queryCategories(this.categorySearchCtrl.value);

    this.categorySearchCtrl.valueChanges
      .pipe(
        startWith(this.categorySearchCtrl.value),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          this.loadingCats.set(true);
          return this.api.listCategories(q || '', 'ACTIVE', 0, 20, 'name,asc');
        })
      )
      .subscribe((res) => {
        this.categories.set(res.content);
        this.loadingCats.set(false);
      });
  }

  private queryCategories(q: string | null) {
    this.loadingCats.set(true);
    this.api
      .listCategories(q || '', 'ACTIVE', 0, 20, 'name,asc')
      .subscribe((res) => {
        this.categories.set(res.content);
        this.loadingCats.set(false);
      });
  }

  onCategorySelected(cat: Category) {
    this.form.patchValue({ categoryId: cat.id });
    this.selectedCategoryName.set(cat.name);
    this.categorySearchCtrl.setValue(cat.name, { emitEvent: false });
  }

  onSubmit() {
    // Trim whitespace
    const skuControl = this.form.get('sku');
    const nameControl = this.form.get('name');

    if (skuControl?.value && !this.data) {
      skuControl.setValue(skuControl.value.trim());
    }

    if (nameControl?.value) {
      nameControl.setValue(nameControl.value.trim());
    }

    // Mark all as touched để hiển thị lỗi
    this.form.markAllAsTouched();

    if (this.form.invalid) return;

    const value = this.form.value as Partial<Product>;
    const req = this.data
      ? this.api.updateProduct(this.data.id, value)
      : this.api.createProduct(value);
    req.subscribe(() => this.ref.close(true));
  }
}
