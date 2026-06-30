type SectionHeaderProps = {
  eyebrow?: string
  title: string
  description?: string
}

export function SectionHeader({ eyebrow, title, description }: SectionHeaderProps) {
  return (
    <div>
      {eyebrow && <p className="text-sm uppercase tracking-[0.28em] text-mo-primary">{eyebrow}</p>}
      <h2 className="mt-2 text-2xl font-bold tracking-tight text-white">{title}</h2>
      {description && <p className="mt-2 text-sm leading-6 text-mo-muted">{description}</p>}
    </div>
  )
}
