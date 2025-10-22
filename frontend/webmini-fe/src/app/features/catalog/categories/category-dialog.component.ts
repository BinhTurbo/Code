import { Component, inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { CatalogService } from '../catalog.service';
import { Category } from '../catalog.models';
import { ToastService } from '../../../core/toast.service';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
  ],
  templateUrl: './category-dialog.component.html',
  styleUrls: ['./category-dialog.component.scss'],
})
export class CategoryDialog {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CatalogService);
  private readonly toast = inject(ToastService);
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
    req.subscribe({
      next: () => {
        this.toast.success(
          this.data
            ? 'Cập nhật danh mục thành công!'
            : 'Thêm danh mục mới thành công!'
        );
        this.ref.close(true);
      },
      error: (err) => {
        const msg = err?.error?.message || 'Có lỗi xảy ra. Vui lòng thử lại!';
        this.toast.error(msg);
      },
    });
  }
}
