import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { ToastService } from '../../../core/toast.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: 'login.component.html',
  styleUrls: ['login.component.scss'],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  loading = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { username, password } = this.form.value;
    this.auth.login({ username: username!, password: password! }).subscribe({
      next: (res) => {
        this.auth.applyLogin(res);
        this.toast.success('Đăng nhập thành công!');
        this.router.navigateByUrl('/'); // ➜ Dashboard
      },
      error: (err) => {
        const errorMsg = err?.error?.message || 'Sai tài khoản hoặc mật khẩu';
        this.error.set(errorMsg);
        this.toast.error(errorMsg);
        this.loading.set(false);
      },
    });
  }

  // Nếu đã đăng nhập thì chuyển thẳng vào dashboard
  constructor() {
    const tokens = (this.auth as any)['tokens'];
    if (tokens?.access?.()) this.router.navigateByUrl('/');
  }
}
