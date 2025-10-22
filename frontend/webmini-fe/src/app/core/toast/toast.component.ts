import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../toast.service';
import { trigger, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
      <div
        class="toast toast-{{ toast.type }}"
        [@slideIn]
        (click)="toastService.remove(toast.id)"
      >
        <div class="toast-icon">
          @switch (toast.type) { @case ('success') {
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              clip-rule="evenodd"
            />
          </svg>
          } @case ('error') {
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
              clip-rule="evenodd"
            />
          </svg>
          } @case ('warning') {
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
              clip-rule="evenodd"
            />
          </svg>
          } @case ('info') {
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clip-rule="evenodd"
            />
          </svg>
          } }
        </div>
        <div class="toast-message">{{ toast.message }}</div>
        <button
          class="toast-close"
          (click)="toastService.remove(toast.id); $event.stopPropagation()"
        >
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
      }
    </div>
  `,
  styles: [
    `
      .toast-container {
        position: fixed;
        top: 1.5rem;
        right: 1.5rem;
        z-index: 9999;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        max-width: 400px;
        pointer-events: none;
      }

      .toast {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem 1.25rem;
        background: white;
        border-radius: 12px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15),
          0 0 0 1px rgba(0, 0, 0, 0.05);
        cursor: pointer;
        transition: all 0.3s ease;
        pointer-events: auto;
        min-width: 300px;
      }

      .toast:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2),
          0 0 0 1px rgba(0, 0, 0, 0.05);
      }

      .toast-icon {
        width: 1.5rem;
        height: 1.5rem;
        flex-shrink: 0;
      }

      .toast-message {
        flex: 1;
        font-size: 0.9375rem;
        font-weight: 500;
        line-height: 1.4;
      }

      .toast-close {
        width: 1.25rem;
        height: 1.25rem;
        flex-shrink: 0;
        background: none;
        border: none;
        cursor: pointer;
        opacity: 0.5;
        transition: opacity 0.2s ease;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .toast-close:hover {
        opacity: 1;
      }

      /* Success Toast */
      .toast-success {
        border-left: 4px solid #10b981;
      }

      .toast-success .toast-icon {
        color: #10b981;
      }

      .toast-success .toast-message {
        color: #065f46;
      }

      .toast-success .toast-close {
        color: #065f46;
      }

      /* Error Toast */
      .toast-error {
        border-left: 4px solid #ef4444;
      }

      .toast-error .toast-icon {
        color: #ef4444;
      }

      .toast-error .toast-message {
        color: #991b1b;
      }

      .toast-error .toast-close {
        color: #991b1b;
      }

      /* Warning Toast */
      .toast-warning {
        border-left: 4px solid #f59e0b;
      }

      .toast-warning .toast-icon {
        color: #f59e0b;
      }

      .toast-warning .toast-message {
        color: #92400e;
      }

      .toast-warning .toast-close {
        color: #92400e;
      }

      /* Info Toast */
      .toast-info {
        border-left: 4px solid #3b82f6;
      }

      .toast-info .toast-icon {
        color: #3b82f6;
      }

      .toast-info .toast-message {
        color: #1e40af;
      }

      .toast-info .toast-close {
        color: #1e40af;
      }

      @media (max-width: 640px) {
        .toast-container {
          top: 1rem;
          right: 1rem;
          left: 1rem;
          max-width: none;
        }

        .toast {
          min-width: auto;
        }
      }
    `,
  ],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateX(400px)', opacity: 0 }),
        animate(
          '300ms cubic-bezier(0.4, 0, 0.2, 1)',
          style({ transform: 'translateX(0)', opacity: 1 })
        ),
      ]),
      transition(':leave', [
        animate(
          '200ms cubic-bezier(0.4, 0, 1, 1)',
          style({ transform: 'translateX(400px)', opacity: 0 })
        ),
      ]),
    ]),
  ],
})
export class ToastComponent {
  toastService = inject(ToastService);
}
