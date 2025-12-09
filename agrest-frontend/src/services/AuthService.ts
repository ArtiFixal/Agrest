import { httpClient } from '@/services/RxHttpClient'
import type { LoginCredentials } from '@/models/user/LoginCredentials'
import type { User } from '@/models/user/User'
import type { Observable } from 'rxjs'

export class AuthService {
  public login(credentials: LoginCredentials): Observable<User> {
    return httpClient.post('/v1/auth/login', credentials, false)
  }
}
