// Kullanıcı token yanıt modeli — auth-service'in döndürdüğü yapı
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  roles: string[];
  email: string;
  fullName: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  tcNo: string;
  email: string;
  password: string;
  phone?: string;
  monthlyIncome?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// JWT'den parse edilen kullanıcı durumu (sinyal)
export interface AuthState {
  isLoggedIn: boolean;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  roles: string[];
  accessToken: string | null;
  refreshToken: string | null;
}
