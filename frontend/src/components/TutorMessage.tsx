import type { ChatResponse } from '../api/chat.ts'

type Props = {
  response: ChatResponse
}

export default function TutorMessage({ response }: Props) {
  const { reply, sources } = response

  return (
    <div className="max-w-[85%] space-y-3 rounded-lg rounded-tl-none border border-slate-200 bg-white p-4">
      <p className="whitespace-pre-line">{reply.reply}</p>

      {reply.correction && (
        <div className="rounded-md bg-amber-50 px-3 py-2 text-sm">
          <span className="text-slate-500 line-through decoration-red-400">{reply.correction.original}</span>
          <span className="mx-2 text-slate-400">→</span>
          <span className="font-medium text-green-800">{reply.correction.corrected}</span>
        </div>
      )}

      {reply.explanationPt && (
        <p className="border-l-2 border-indigo-200 pl-3 text-sm italic text-slate-600">{reply.explanationPt}</p>
      )}

      {sources.length > 0 && (
        <p className="text-xs text-slate-500">
          <span aria-hidden="true">📚 </span>
          Source: {sources.map((source) => source.title).join(' · ')}
        </p>
      )}
    </div>
  )
}
