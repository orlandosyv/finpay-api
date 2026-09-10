import { MerchantRole } from './auth.models';

export type MerchantStatus = 'ACTIVE' | 'INACTIVE';

export interface CurrentMerchantResponse {
  merchantId: number;
  merchantName: string;
  merchantStatus: MerchantStatus;
  userId: number;
  email: string;
  role: MerchantRole;
}
