import type { TagDto } from '../TagDTO'

export enum Status {
  ONLINE,
  OFFLINE,
  UNKNOWN,
}

export interface Target {
  id: number
  name: string
  url: string
  tags: TagDto[]
  lastScan?: Date
  status: Status
}
