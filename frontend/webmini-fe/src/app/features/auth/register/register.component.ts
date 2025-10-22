import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

function matchPasswordValidator(
  group: AbstractControl
): ValidationErrors | null {
  const pw = group.get('password')?.value;
  const cf = group.get('confirmPassword')?.value;
  return pw && cf && pw !== cf ? { mismatch: true } : null;
}

// Validator cho username: chỉ cho phép chữ cái, số, gạch dưới và gạch ngang
function usernameValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const usernameRegex = /^[a-zA-Z0-9_-]+$/;
  return usernameRegex.test(control.value) ? null : { invalidUsername: true };
}

// Validator cho độ mạnh của password
function passwordStrengthValidator(
  control: AbstractControl
): ValidationErrors | null {
  if (!control.value) return null;
  const password = control.value;

  // Kiểm tra có ít nhất 1 chữ hoa
  if (!/[A-Z]/.test(password)) {
    return { noUpperCase: true };
  }

  // Kiểm tra có ít nhất 1 chữ thường
  if (!/[a-z]/.test(password)) {
    return { noLowerCase: true };
  }

  // Kiểm tra có ít nhất 1 số
  if (!/[0-9]/.test(password)) {
    return { noNumber: true };
  }

  return null;
}

// Validator cho tên đầy đủ
function fullNameValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const trimmed = control.value.trim();

  // Kiểm tra có ít nhất 2 từ (họ và tên)
  if (trimmed.split(/\s+/).length < 2) {
    return { invalidFullName: true };
  }

  // Kiểm tra chỉ chứa chữ cái, dấu cách và các ký tự tiếng Việt
  const nameRegex = /^[a-zA-ZÀ-ỹ\s]+$/;
  if (!nameRegex.test(trimmed)) {
    return { invalidCharacters: true };
  }

  return null;
}

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: 'register.component.html',
  styleUrls: ['register.component.scss']
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    // full_name VARCHAR(150)
    fullName: [
      '',
      [Validators.required, Validators.maxLength(150), fullNameValidator],
    ],
    // username VARCHAR(100)
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100),
        usernameValidator,
      ],
    ],
    pw: this.fb.group(
      {
        // password_hash VARCHAR(255) - nhưng input giới hạn 72 ký tự (bcrypt limit)
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.maxLength(72), // bcrypt chỉ xử lý tối đa 72 bytes
            passwordStrengthValidator,
          ],
        ],
        confirmPassword: ['', Validators.required],
      },
      { validators: [matchPasswordValidator] }
    ),
  });

  onSubmit() {
    // Trim whitespace từ các trường
    const fullNameControl = this.form.get('fullName');
    const usernameControl = this.form.get('username');

    if (fullNameControl?.value) {
      fullNameControl.setValue(fullNameControl.value.trim());
    }

    if (usernameControl?.value) {
      usernameControl.setValue(usernameControl.value.trim());
    }

    // Đánh dấu tất cả các trường là touched để hiển thị lỗi
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const fullName = this.form.value.fullName!;
    const username = this.form.value.username!;
    const password = (this.form.value.pw as any).password as string;

    this.auth.register({ fullName, username, password }).subscribe({
      next: (res) => {
        // BE trả TokenResponse → auto login
        this.auth.applyLogin(res);
        this.router.navigateByUrl('/'); // vào Dashboard
      },
      error: (err) => {
        const msg = err?.error?.message || 'Đăng ký thất bại';
        this.error.set(msg);
        this.loading.set(false);
      },
    });
  }

  getPasswordStrength(): string {
    const password = this.form.get('pw.password')?.value;
    if (!password) return '';

    let strength = 0;

    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;

    if (strength <= 2) return 'weak';
    if (strength <= 4) return 'medium';
    return 'strong';
  }

  getPasswordStrengthText(): string {
    const strength = this.getPasswordStrength();
    const map: Record<string, string> = {
      weak: 'Yếu',
      medium: 'Trung bình',
      strong: 'Mạnh',
    };
    return map[strength] || '';
  }

  constructor() {
    const tokens = (this.auth as any)['tokens'];
    if (tokens?.access?.()) this.router.navigateByUrl('/');
  }
}
