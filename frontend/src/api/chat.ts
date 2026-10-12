import { request } from './client.ts'

export type Correction = {
  original: string
  corrected: string
}

export type TutorReply = {
  reply: string
  explanationPt: string | null
  correction: Correction | null
  sources: string[]
}

export type Source = {
  slug: string
  title: string
}

export type ChatResponse = {
  conversationId: number
  messageId: number
  reply: TutorReply
  sources: Source[]
  fallback: boolean
}

export function sendMessage(text: string, conversationId: number | null): Promise<ChatResponse> {
  return request<ChatResponse>('/api/chat', {
    method: 'POST',
    body: JSON.stringify({ conversationId, text }),
  })
}
