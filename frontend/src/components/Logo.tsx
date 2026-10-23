export function PoleMark({ size = 38 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" aria-hidden="true">
      <rect width="40" height="40" rx="10" fill="#f3ede2" />
      <rect x="14" y="7" width="12" height="26" rx="4" fill="#fff" stroke="#1c1a17" strokeWidth="2" />
      <clipPath id="pc"><rect x="14" y="7" width="12" height="26" rx="4" /></clipPath>
      <g clipPath="url(#pc)" stroke="#b4532a" strokeWidth="3.4"><path d="M10 12l20 8M10 19l20 8M10 26l20 8M10 5l20 8" /></g>
      <rect x="12" y="4.5" width="16" height="4" rx="2" fill="#1c1a17" /><rect x="12" y="31.5" width="16" height="4" rx="2" fill="#1c1a17" />
    </svg>
  )
}
export function Brand() {
  return <div className="brand"><PoleMark /><div><div className="brand-name">Barbearia</div><div className="brand-sub">Manager</div></div></div>
}
