const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000/api/v1'

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.text()
    throw new Error(`API error ${response.status}: ${body || response.statusText}`)
  }

  return response.json() as Promise<T>
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  return parseResponse<T>(response)
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })

  return parseResponse<T>(response)
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  return parseResponse<T>(response)
}

export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(`API error ${response.status}: ${body || response.statusText}`)
  }
}

export function getApiBaseUrl(): string {
  return API_BASE_URL
}
