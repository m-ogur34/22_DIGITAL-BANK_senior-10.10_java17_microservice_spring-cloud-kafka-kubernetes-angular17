export type AccountType = 'VADESIZ' | 'VADELI' | 'TASARRUF' | 'YATIRIM';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';

export interface Account {
  id: string;
  iban: string;
  balance: number;
  accountType: AccountType;
  accountTypeName: string;
  status: AccountStatus;
  statusName: string;
  accountName: string;
  currency: string;
  ownerId: string;
  createdAt: string;
  // Vadeli hesap
  vadeGunu?: number;
  faizOrani?: number;
  vadeBitis?: string;
  // Yatırım hesabı
  riskSeviyesi?: number;
  portfoyDegeri?: number;
}

export interface CreateAccountRequest {
  accountType: AccountType;
  accountName?: string;
  vadeGunu?: number;
  faizOrani?: number;
  riskSeviyesi?: number;
}
