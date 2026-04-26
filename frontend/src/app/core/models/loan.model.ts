export type LoanType = 'IHTIYAC' | 'KONUT' | 'TASIT';
export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISBURSED' | 'CLOSED';

export interface LoanApplication {
  applicationId: string;
  loanType: LoanType;
  loanTypeName: string;
  requestedAmount: number;
  approvedAmount: number;
  termMonths: number;
  annualInterestRate: number;
  monthlyInstallment: number;
  totalPayment: number;
  status: LoanStatus;
  statusName: string;
  creditScore: number;
  rejectionReason?: string;
  appliedAt: string;
}

export interface LoanApplicationRequest {
  loanType: LoanType;
  amount: number;
  termMonths: number;
  disbursementIban: string;
  sigortaIsteniyor: boolean;
}

export interface Installment {
  number: number;
  dueDate: string;
  amount: number;
  principalAmount: number;
  interestAmount: number;
  remainingPrincipal: number;
  paid: boolean;
  paymentDate?: string;
}

export interface InstallmentPlan {
  loanApplicationId: string;
  totalAmount: number;
  monthlyInstallment: number;
  termMonths: number;
  installments: Installment[];
}
