import type { TagDto } from '../TagDTO'
import { Status } from './Target'

export interface TargetDetails {
  id: number
  name: string
  url: string
  status: Status
  description?: string
  tags?: TagDto[]
  headers?: Record<string, string>
  cookies?: Record<string, string>
  created: Date
  edited?: Date
  lastScan?: Date
  scans: []
}
