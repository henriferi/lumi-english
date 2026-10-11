/** Error body returned by the backend (RFC 9457 Problem Details). */
export type ProblemDetail = {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? 'Request failed')
    this.problem = problem
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new ApiError({ title: 'Network error', detail: 'Could not reach the server. Is the backend running?' })
  }

  if (!response.ok) {
    const problem: ProblemDetail = await response.json().catch(() => ({ status: response.status }))
    throw new ApiError(problem)
  }
  return response.json() as Promise<T>
}
