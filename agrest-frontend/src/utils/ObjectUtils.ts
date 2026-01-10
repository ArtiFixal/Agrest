import { toRaw } from 'vue'

/**
 * Converts all nested structures to plain objects.
 *
 * @param obj Object to convert
 *
 * @returns Object with plain data.
 */
export function deepToRaw<T>(obj: T): T {
  const raw = toRaw(obj)

  if (Array.isArray(raw)) {
    return raw.map((item) => deepToRaw(item)) as T
  }

  if (raw instanceof Map) {
    return Object.fromEntries(raw) as T
  }

  if (raw instanceof Set) {
    return Array.from(raw) as T
  }

  if (raw !== null && typeof raw === 'object') {
    const rawObj = raw as Record<string, unknown>
    return Object.keys(rawObj).reduce(
      (acc, key) => {
        acc[key] = deepToRaw(rawObj[key])
        return acc
      },
      {} as Record<string, unknown>,
    ) as T
  }
  return raw
}
