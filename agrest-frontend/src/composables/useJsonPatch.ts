// composables/useJsonPatch.ts
import { reactive, onBeforeUnmount, toRaw } from 'vue'
import { BehaviorSubject, Subject } from 'rxjs'
import { map, debounceTime, distinctUntilChanged } from 'rxjs/operators'
import { compare, applyPatch, type Operation } from 'fast-json-patch'

interface PatchState<T> {
  document: T
  patches: Operation[]
  canUndo: boolean
  canRedo: boolean
}

interface PatchHistory {
  patches: Operation[]
  inversePatches: Operation[]
}

export function useJsonPatch<T extends object>(initialDocument: T) {
  const state = reactive<PatchState<T>>({
    document: structuredClone(initialDocument),
    patches: [],
    canUndo: false,
    canRedo: false,
  })

  const documentSub = new BehaviorSubject<T>(structuredClone(initialDocument))
  const patchesSub = new Subject<Operation[]>()
  // Change history
  const history: PatchHistory[] = []
  const redoStack: PatchHistory[] = []

  const subscription = documentSub
    .pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
      map((newDoc) => {
        const rawStateDoc = toRaw(state.document) as T
        const rawNewDoc = toRaw(newDoc) as T

        const patches = compare(rawStateDoc, rawNewDoc)
        const inversePatches = compare(rawNewDoc, rawStateDoc)
        return { newDoc, patches, inversePatches }
      }),
    )
    .subscribe(({ newDoc, patches, inversePatches }) => {
      if (patches.length > 0) {
        state.document = newDoc as any
        state.patches = patches
        history.push({ patches, inversePatches })
        redoStack.length = 0
        updateUndoRedoState()
        patchesSub.next(patches)
      }
    })

  const applyPatches = (patches: Operation[]) => {
    try {
      const rawDoc = toRaw(state.document) as T
      const result = applyPatch(structuredClone(rawDoc), patches, true)

      if (result.newDocument) {
        state.document = result.newDocument as any
        documentSub.next(result.newDocument)
        return { success: true, document: result.newDocument }
      }
    } catch (error) {
      console.error('Failed to apply patches:', error)
      return { success: false, error }
    }
  }

  const updateDocument = (newDocument: T) => {
    const cloned = structuredClone(toRaw(newDocument) as T)
    documentSub.next(cloned)
  }

  const undo = () => {
    if (history.length === 0) return

    const lastHistory = history.pop()!
    redoStack.push(lastHistory)
    applyPatches(lastHistory.inversePatches)
    updateUndoRedoState()
  }

  const redo = () => {
    if (redoStack.length === 0) return

    const nextHistory = redoStack.pop()!
    history.push(nextHistory)
    applyPatches(nextHistory.patches)
    updateUndoRedoState()
  }

  const updateUndoRedoState = () => {
    state.canUndo = history.length > 0
    state.canRedo = redoStack.length > 0
  }

  onBeforeUnmount(() => {
    subscription.unsubscribe()
    documentSub.complete()
    patchesSub.complete()
  })

  return {
    state,
    document: documentSub.asObservable(),
    patches: patchesSub.asObservable(),
    updateDocument,
    applyPatches,
    undo,
    redo,
  }
}
