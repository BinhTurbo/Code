import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../catalog.service';
import { Product } from '../catalog.models';
import { ToastService } from '../../../core/toast.service';

@Component({
  standalone: true,
  selector: 'app-product-detail',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CatalogService);
  private readonly toast = inject(ToastService);

  product = signal<Product | null>(null);
  loading = signal(true);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(+id);
    } else {
      this.loading.set(false);
    }
  }

  loadProduct(id: number) {
    this.api.getProduct(id).subscribe({
      next: (data) => {
        this.product.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error('Không thể tải thông tin sản phẩm');
        this.loading.set(false);
      },
    });
  }

  goBack() {
    this.router.navigate(['/catalog/products']);
  }
}
