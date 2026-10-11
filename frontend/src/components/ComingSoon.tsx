type Props = {
  title: string
  phase: number
  description: string
}

export default function ComingSoon({ title, phase, description }: Props) {
  return (
    <section>
      <h1 className="text-2xl font-semibold">{title}</h1>
      <div className="mt-6 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-slate-500">
        <p>{description}</p>
        <p className="mt-2 text-sm">Coming in Phase {phase}.</p>
      </div>
    </section>
  )
}
