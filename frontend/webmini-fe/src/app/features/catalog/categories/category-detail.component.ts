import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../catalog.service';
import { Category } from '../catalog.models';
import { ToastService } from '../../../core/toast.service';

@Component({
  standalone: true,
  selector: 'app-category-detail',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './category-detail.component.html',
  styleUrls: ['./category-detail.component.scss'],
})
export class CategoryDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CatalogService);
  private readonly toast = inject(ToastService);

  category = signal<Category | null>(null);
  loading = signal(true);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCategory(+id);
    } else {
      this.loading.set(false);
    }
  }

  loadCategory(id: number) {
    this.api.getCategory(id).subscribe({
      next: (data) => {
        this.category.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error('Không thể tải thông tin danh mục');
        this.loading.set(false);
      },
    });
  }

  goBack() {
    this.router.navigate(['/catalog/categories']);
  }
}
