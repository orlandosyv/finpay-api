import { MerchantRole } from './auth.models';

export type MerchantStatus = 'ACTIVE' | 'SUSPENDED';
export type UserStatus = 'ACTIVE' | 'DISABLED';

export interface CurrentMerchantResponse {
  merchantId: number;
  merchantName: string;
  merchantStatus: MerchantStatus;
  userId: number;
  email: string;
  role: MerchantRole;
}

export interface MerchantUserResponse {
  membershipId: number;
  userId: number;
  email: string;
  status: UserStatus;
  role: MerchantRole;
  createdAt: string;
}

export interface CreateMerchantUserRequest {
  email: string;
  password: string;
  role: MerchantRole;
}
