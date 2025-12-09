export enum Role {
  ANALYST = 0,
  USER = 1,
  ADMIN = 2,
}

export interface User {
  id: string
  email: string
  role: Role
}
