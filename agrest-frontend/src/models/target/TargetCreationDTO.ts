export interface TargetCreationDTO {
  id?: number
  name: string
  url: string
  description?: string
  tags?: string[]
  cookies?: Map<string, string>
  headers?: Map<string, string>
}
