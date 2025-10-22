export interface Category {
  id: number;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  categoryId: number;
  categoryName: string;
  price: number;
  stock: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  size: number;
  number: number;
}
