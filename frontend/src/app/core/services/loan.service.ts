import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoanApplicationRequest } from '../models/loan.model';

@Injectable({ providedIn: 'root' })
export class LoanService {

  private readonly BASE = environment.apiUrls.loan;

  constructor(private http: HttpClient) {}

  apply(request: LoanApplicationRequest, monthlyIncome: number): Observable<any> {
    return this.http.post<any>(`${this.BASE}/loans/apply`, request, {
      headers: { 'X-Monthly-Income': monthlyIncome.toString() }
    });
  }

  getMyLoans(): Observable<any> {
    return this.http.get<any>(`${this.BASE}/loans`);
  }

  getInstallmentPlan(loanId: string): Observable<any> {
    return this.http.get<any>(`${this.BASE}/loans/${loanId}/installments`);
  }
}
