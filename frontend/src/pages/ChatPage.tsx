import { useEffect, useRef, useState, type FormEvent } from 'react'
import { sendMessage, type ChatResponse } from '../api/chat.ts'
import { ApiError } from '../api/client.ts'
import TutorMessage from '../components/TutorMessage.tsx'

type ChatItem = { role: 'user'; text: string } | { role: 'tutor'; response: ChatResponse }

type ChatState = {
  conversationId: number | null
  items: ChatItem[]
}

const STORAGE_KEY = 'lumi.chat'
const MAX_LENGTH = 500

export default function ChatPage() {
  const [chat, setChat] = useState<ChatState>(loadChat)
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    saveChat(chat)
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [chat])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = text.trim()
    if (!message || sending) return

    setChat((current) => ({ ...current, items: [...current.items, { role: 'user', text: message }] }))
    setText('')
    setSending(true)
    setError(null)
    try {
      const response = await sendMessage(message, chat.conversationId)
      setChat((current) => ({
        conversationId: response.conversationId,
        items: [...current.items, { role: 'tutor', response }],
      }))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unexpected error.')
    } finally {
      setSending(false)
    }
  }

  function startNewConversation() {
    setChat({ conversationId: null, items: [] })
    setError(null)
  }

  return (
    <section className="flex h-[calc(100vh-10rem)] flex-col">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Chat</h1>
        <button
          type="button"
          onClick={startNewConversation}
          disabled={sending || chat.items.length === 0}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm hover:bg-white disabled:opacity-50"
        >
          New conversation
        </button>
      </div>

      <div className="mt-4 flex-1 space-y-4 overflow-y-auto rounded-lg bg-slate-100 p-4">
        {chat.items.length === 0 && (
          <p className="mt-8 text-center text-slate-500">
            Say hello to Lumi! Write anything in English — mistakes are welcome.
          </p>
        )}
        {chat.items.map((item, index) =>
          item.role === 'user' ? (
            <div key={index} className="flex justify-end">
              <p className="max-w-[85%] whitespace-pre-line rounded-lg rounded-tr-none bg-indigo-600 px-4 py-2 text-white">
                {item.text}
              </p>
            </div>
          ) : (
            <TutorMessage key={index} response={item.response} />
          ),
        )}
        {sending && <p className="text-sm text-slate-500">Lumi is typing…</p>}
        <div ref={bottomRef} />
      </div>

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <form onSubmit={handleSubmit} className="mt-4 flex gap-2">
        <input
          type="text"
          value={text}
          maxLength={MAX_LENGTH}
          onChange={(event) => setText(event.target.value)}
          placeholder="Type your message in English…"
          aria-label="Message"
          className="flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 focus:border-indigo-500 focus:outline-none"
        />
        <button
          type="submit"
          disabled={sending || !text.trim()}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </section>
  )
}

/** Keeps the chat while the user switches tabs (sessionStorage); the backend is the real history. */
function loadChat(): ChatState {
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY)
    if (saved) return JSON.parse(saved) as ChatState
  } catch {
    // storage unavailable or corrupted: start empty
  }
  return { conversationId: null, items: [] }
}

function saveChat(chat: ChatState) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(chat))
  } catch {
    // storage unavailable: the chat just won't survive a tab switch
  }
}
