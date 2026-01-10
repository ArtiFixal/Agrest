import { Status } from '@/models/target/Target'

export function getStatusClass(status: Status) {
  switch (status) {
    case Status.ONLINE:
      return 'text-success'
    case Status.OFFLINE:
      return 'text-destructive'
    default:
    case Status.UNKNOWN:
      return 'text-muted-foreground'
  }
}

export function getStatusName(status?: Status) {
  if (status === null) return 'unknown'
  switch (status) {
    case Status.ONLINE:
      return 'online'
    case Status.OFFLINE:
      return 'offline'
    default:
    case Status.UNKNOWN:
      return 'unknown'
  }
}
