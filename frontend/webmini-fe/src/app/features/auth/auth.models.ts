export interface LoginRequest {
  username: string;
  password: string;
}
export interface LoginResponse {
  tokenType: string;
  accessToken: string;
  accessExpiresInSec: number;
  refreshToken: string;
}
export interface JwtPayload {
  sub: string;
  uid: number;
  roles: string[];
  typ: 'access' | 'refresh';
  iat: number;
  exp: number;
  iss: string;
}
