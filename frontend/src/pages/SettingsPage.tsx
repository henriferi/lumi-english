import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client.ts'
import { AGE_BANDS, LEVELS, getSettings, saveSettings, type AgeBand, type Level, type Settings } from '../api/settings.ts'

const ageBandLabels: Record<AgeBand, string> = {
  KID: 'Kid',
  TEEN: 'Teen',
  ADULT: 'Adult',
}

const levelLabels: Record<Level, string> = {
  PRE_A1: 'Pre-A1 — starting from zero',
  A1: 'A1 — beginner',
  A2: 'A2 — elementary',
}

type Status =
  | { kind: 'loading' }
  | { kind: 'idle' }
  | { kind: 'saving' }
  | { kind: 'saved' }
  | { kind: 'error'; message: string; fieldErrors?: Record<string, string> }

export default function SettingsPage() {
  const [form, setForm] = useState<Settings>({ name: '', ageBand: 'ADULT', level: 'PRE_A1' })
  const [status, setStatus] = useState<Status>({ kind: 'loading' })

  useEffect(() => {
    getSettings()
      .then((settings) => {
        setForm(settings)
        setStatus({ kind: 'idle' })
      })
      .catch((error: unknown) => setStatus(toErrorStatus(error)))
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setStatus({ kind: 'saving' })
    try {
      setForm(await saveSettings(form))
      setStatus({ kind: 'saved' })
    } catch (error) {
      setStatus(toErrorStatus(error))
    }
  }

  const fieldErrors = status.kind === 'error' ? status.fieldErrors : undefined
  const busy = status.kind === 'loading' || status.kind === 'saving'

  return (
    <section>
      <h1 className="text-2xl font-semibold">Settings</h1>
      <p className="mt-1 text-slate-500">The tutor adapts its tone to your age group and its English to your level.</p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-6 rounded-lg border border-slate-200 bg-white p-6">
        <div>
          <label htmlFor="name" className="block text-sm font-medium">
            Name
          </label>
          <input
            id="name"
            type="text"
            maxLength={50}
            required
            value={form.name}
            disabled={busy}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
          {fieldErrors?.name && <p className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>}
        </div>

        <fieldset>
          <legend className="text-sm font-medium">Age group</legend>
          <div className="mt-2 flex gap-2">
            {AGE_BANDS.map((ageBand) => (
              <label
                key={ageBand}
                className={`cursor-pointer rounded-md border px-4 py-2 text-sm ${
                  form.ageBand === ageBand ? 'border-indigo-500 bg-indigo-50 text-indigo-700' : 'border-slate-300'
                }`}
              >
                <input
                  type="radio"
                  name="ageBand"
                  value={ageBand}
                  checked={form.ageBand === ageBand}
                  disabled={busy}
                  onChange={() => setForm({ ...form, ageBand })}
                  className="sr-only"
                />
                {ageBandLabels[ageBand]}
              </label>
            ))}
          </div>
          {fieldErrors?.ageBand && <p className="mt-1 text-sm text-red-600">{fieldErrors.ageBand}</p>}
        </fieldset>

        <div>
          <label htmlFor="level" className="block text-sm font-medium">
            English level
          </label>
          <select
            id="level"
            value={form.level}
            disabled={busy}
            onChange={(event) => setForm({ ...form, level: event.target.value as Level })}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          >
            {LEVELS.map((level) => (
              <option key={level} value={level}>
                {levelLabels[level]}
              </option>
            ))}
          </select>
          {fieldErrors?.level && <p className="mt-1 text-sm text-red-600">{fieldErrors.level}</p>}
        </div>

        <div className="flex items-center gap-4">
          <button
            type="submit"
            disabled={busy}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {status.kind === 'saving' ? 'Saving…' : 'Save'}
          </button>
          {status.kind === 'saved' && <span className="text-sm text-green-700">Settings saved.</span>}
          {status.kind === 'error' && <span className="text-sm text-red-600">{status.message}</span>}
        </div>
      </form>
    </section>
  )
}

function toErrorStatus(error: unknown): Status {
  if (error instanceof ApiError) {
    return { kind: 'error', message: error.message, fieldErrors: error.problem.errors }
  }
  return { kind: 'error', message: 'Unexpected error.' }
}
