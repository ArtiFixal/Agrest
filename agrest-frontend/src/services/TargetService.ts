import type { TargetCreationDTO } from '@/models/target/TargetCreationDTO'
import { mergeMap, Observable, take } from 'rxjs'
import { httpClient } from './RxHttpClient'
import { useSSEStream } from '@/services/SSEStream'
import { useJsonPatch } from '@/composables/useJsonPatch'
import type { Target } from '@/models/target/Target'
import type { TargetDetails } from '@/models/target/TargetDetails'
import { deepToRaw } from '@/utils/ObjectUtils'

export class TargetService {
  public addTarget(targetData: TargetCreationDTO, swagger?: File): Observable<number> {
    const formData = this.packDataToForm(targetData, swagger)
    return httpClient.post('/v1/targets', formData, true, {
      Accept: 'application/json',
    })
  }

  public editTarget(
    targetID: number,
    originalData: TargetCreationDTO,
    updatedData: TargetCreationDTO,
    swagger?: File,
  ): Observable<unknown> {
    const rawOriginal = deepToRaw(originalData)
    const rawUpdated = deepToRaw(updatedData)

    const patch = useJsonPatch(rawOriginal)
    patch.updateDocument(rawUpdated)

    return patch.patches.pipe(
      take(1),
      mergeMap((patches) => {
        const formData = this.packDataToForm(patches, swagger)
        return httpClient.patch(`/v1/targets/${targetID}`, formData, true)
      }),
    )
  }

  public getTargetDetails(targetID: number): Observable<TargetDetails> {
    return httpClient.get(`/v1/targets/${targetID}`, true)
  }

  public getTargetForUpdate(targetID: number): Observable<TargetCreationDTO> {
    return httpClient.get(`/v1/targets/${targetID}/dto`, true)
  }

  public getTargetPageStream(pageNumer: number, pageSize: number) {
    return useSSEStream<Target>({
      url: `/v1/targets?page=${pageNumer}&size=${pageSize}`,
    })
  }

  private packDataToForm(targetData: unknown, swagger?: File): FormData {
    const formData = new FormData()
    const targetBlob = new Blob([JSON.stringify(targetData)], { type: 'application/json' })
    formData.append('target', targetBlob)

    if (swagger) formData.append('swagger', swagger)
    return formData
  }
}
