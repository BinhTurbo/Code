import { Component, inject, OnInit, signal } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
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
import { ToastService } from '../../../core/toast.service';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatAutocompleteModule,
    MatInputModule,
    MatOptionModule,
    MatDialogModule,
  ],
  templateUrl: './product-dialog.component.html',
  styleUrls: ['./product-dialog.component.scss'],
})
export class ProductDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CatalogService);
  private readonly toast = inject(ToastService);
  readonly ref = inject(MatDialogRef<ProductDialog>);
  readonly data = inject<Product | null>(MAT_DIALOG_DATA);

  // Validation theo DB schema
  form = this.fb.group({
    // sku VARCHAR(100) NOT NULL UNIQUE
    sku: [
      this.data?.sku || '',
      [Validators.required, Validators.minLength(3), Validators.maxLength(100)],
    ],
    // name VARCHAR(200) NOT NULL
    name: [
      this.data?.name || '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(200)],
    ],
    // category_id BIGINT NOT NULL
    categoryId: [this.data?.categoryId || null, Validators.required],
    // price DECIMAL(18,2) NOT NULL DEFAULT 0, CHECK (price >= 0)
    price: [
      this.data?.price ?? 0,
      [
        Validators.required,
        Validators.min(0),
        Validators.max(999999999999.99), // Safe number for DECIMAL(18,2)
      ],
    ],
    // stock INT NOT NULL DEFAULT 0, CHECK (stock >= 0)
    stock: [
      this.data?.stock ?? 0,
      [
        Validators.required,
        Validators.min(0),
        Validators.max(2147483647), // INT max
      ],
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
    req.subscribe({
      next: () => {
        this.toast.success(
          this.data
            ? 'Cập nhật sản phẩm thành công!'
            : 'Thêm sản phẩm mới thành công!'
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
