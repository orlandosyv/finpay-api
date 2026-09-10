export type MerchantRole = 'MERCHANT_ADMIN' | 'MERCHANT_USER';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  userId: number;
  email: string;
  merchantId: number;
  merchantName: string;
  role: MerchantRole;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

export interface AuthSession extends LoginResponse {
  accessTokenExpiresAt: number;
  refreshTokenExpiresAt: number;
}
