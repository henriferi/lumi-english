import { request } from './client.ts'

export const AGE_BANDS = ['KID', 'TEEN', 'ADULT'] as const
export const LEVELS = ['PRE_A1', 'A1', 'A2'] as const

export type AgeBand = (typeof AGE_BANDS)[number]
export type Level = (typeof LEVELS)[number]

export type Settings = {
  name: string
  ageBand: AgeBand
  level: Level
}

export function getSettings(): Promise<Settings> {
  return request<Settings>('/api/settings')
}

export function saveSettings(settings: Settings): Promise<Settings> {
  return request<Settings>('/api/settings', { method: 'PUT', body: JSON.stringify(settings) })
}
