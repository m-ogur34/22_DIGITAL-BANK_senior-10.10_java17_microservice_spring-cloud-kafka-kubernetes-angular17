import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TransactionSearchRequest, TransferRequest } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {

  private readonly BASE = environment.apiUrls.transaction;

  constructor(private http: HttpClient) {}

  transfer(request: TransferRequest): Observable<any> {
    return this.http.post<any>(`${this.BASE}/transactions/transfer`, request);
  }

  getHistory(page = 0, size = 20): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.BASE}/transactions`, { params });
  }

  search(request: TransactionSearchRequest): Observable<any> {
    let params = new HttpParams();
    if (request.keyword) params = params.set('keyword', request.keyword);
    if (request.iban) params = params.set('iban', request.iban);
    if (request.startDate) params = params.set('startDate', request.startDate);
    if (request.endDate) params = params.set('endDate', request.endDate);
    if (request.minAmount !== undefined) params = params.set('minAmount', request.minAmount);
    if (request.maxAmount !== undefined) params = params.set('maxAmount', request.maxAmount);
    if (request.status) params = params.set('status', request.status);
    params = params.set('page', request.page ?? 0).set('size', request.size ?? 20);
    return this.http.get<any>(`${this.BASE}/transactions/search`, { params });
  }
}
