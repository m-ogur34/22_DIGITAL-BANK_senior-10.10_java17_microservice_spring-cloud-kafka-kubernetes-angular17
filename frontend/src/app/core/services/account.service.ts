import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Account, CreateAccountRequest } from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {

  private readonly BASE = environment.apiUrls.account;

  constructor(private http: HttpClient) {}

  getMyAccounts(): Observable<any> {
    return this.http.get<any>(`${this.BASE}/accounts`);
  }

  getBalance(iban: string): Observable<any> {
    return this.http.get<any>(`${this.BASE}/accounts/${iban}/balance`);
  }

  createAccount(request: CreateAccountRequest): Observable<any> {
    return this.http.post<any>(`${this.BASE}/accounts`, request);
  }

  freezeAccount(iban: string): Observable<any> {
    return this.http.put<any>(`${this.BASE}/accounts/${iban}/freeze`, {});
  }

  activateAccount(iban: string): Observable<any> {
    return this.http.put<any>(`${this.BASE}/accounts/${iban}/activate`, {});
  }
}
