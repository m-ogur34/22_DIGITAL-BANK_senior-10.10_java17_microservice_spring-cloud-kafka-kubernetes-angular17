export type TransactionStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
export type TransactionType = 'DEBIT' | 'CREDIT' | 'REVERSAL' | 'EXTERNAL_TRANSFER' | 'LOAN_PAYMENT';

export interface Transaction {
  id: string;
  senderIban: string;
  receiverIban: string;
  amount: number;
  description: string;
  type: TransactionType;
  status: TransactionStatus;
  referenceId: string;
  internal: boolean;
  createdAt: string;
}

export interface TransferRequest {
  senderIban: string;
  receiverIban: string;
  amount: number;
  description?: string;
}

export interface TransferResponse {
  transactionId: string;
  referenceId: string;
  status: TransactionStatus;
  message: string;
  amount: number;
  senderIban: string;
  receiverIban: string;
}

export interface TransactionSearchRequest {
  keyword?: string;
  iban?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  status?: TransactionStatus;
  page?: number;
  size?: number;
}
