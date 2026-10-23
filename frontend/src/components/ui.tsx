import { useEffect, useRef, useState, type ReactNode, type InputHTMLAttributes, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'
import { AlertCircle, Inbox, Loader2, X } from 'lucide-react'
import type { StatusAgendamento } from '../types'
import { STATUS_LABEL } from '../services/format'
import { ApiError } from '../services/api'

export function Spinner({ texto = 'Carregando…' }: { texto?: string }) {
  return <div className="loading" role="status"><div className="spin" /><p className="muted">{texto}</p></div>
}

export function ErroBox({ msg, onRetry }: { msg: string; onRetry?: () => void }) {
  return (
    <div className="alert alert-erro" role="alert">
      <AlertCircle size={18} /><span style={{ flex: 1 }}>{msg}</span>
      {onRetry && <button className="btn btn-ghost btn-sm" onClick={onRetry}>Tentar novamente</button>}
    </div>
  )
}

export function Vazio({ titulo, texto, acao }: { titulo: string; texto?: string; acao?: ReactNode }) {
  return <div className="empty"><Inbox size={40} className="ico" /><h3>{titulo}</h3>{texto && <p>{texto}</p>}{acao}</div>
}

export function PageHead({ titulo, sub, children }: { titulo: string; sub?: string; children?: ReactNode }) {
  return <div className="page-head"><div><h1>{titulo}</h1>{sub && <p>{sub}</p>}</div>{children && <div className="row">{children}</div>}</div>
}

export function StatusBadge({ status }: { status: StatusAgendamento }) {
  return <span className={`badge b-${status}`}>{STATUS_LABEL[status]}</span>
}

export function Botao({ carregando, children, ...p }: React.ButtonHTMLAttributes<HTMLButtonElement> & { carregando?: boolean }) {
  return <button {...p} disabled={p.disabled || carregando}>{carregando && <Loader2 size={16} className="spin-i" style={{ animation: 'giro .7s linear infinite' }} />}{children}</button>
}

interface FieldProps { label: string; erro?: string; dica?: string; children?: ReactNode }
function Campo({ label, erro, dica, id, children }: FieldProps & { id: string }) {
  return <div className="field"><label htmlFor={id}>{label}</label>{children}{dica && !erro && <span className="hint">{dica}</span>}{erro && <span className="err" id={`${id}-err`}>{erro}</span>}</div>
}
let seq = 0
const useId = () => useRef(`f${++seq}`).current
export function Input({ label, erro, dica, ...p }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return <Campo id={id} label={label} erro={erro} dica={dica}><input id={id} className="input" aria-invalid={!!erro} aria-describedby={erro ? `${id}-err` : undefined} {...p} /></Campo>
}
export function Select({ label, erro, dica, children, ...p }: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId()
  return <Campo id={id} label={label} erro={erro} dica={dica}><select id={id} className="input" aria-invalid={!!erro} {...p}>{children}</select></Campo>
}
export function Textarea({ label, erro, dica, ...p }: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId()
  return <Campo id={id} label={label} erro={erro} dica={dica}><textarea id={id} className="input" rows={3} aria-invalid={!!erro} {...p} /></Campo>
}

export function Modal({ titulo, onClose, children, largo }: { titulo: string; onClose: () => void; children: ReactNode; largo?: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const anterior = document.activeElement as HTMLElement | null
    ref.current?.querySelector<HTMLElement>('input,select,textarea,button:not([aria-label="Fechar"])')?.focus()
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', esc)
    return () => { document.removeEventListener('keydown', esc); anterior?.focus() }
  }, [onClose])
  return (
    <div className="modal-bg" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${largo ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={titulo} ref={ref}>
        <div className="modal-head"><h2>{titulo}</h2><button className="icon-btn" style={{ color: 'inherit' }} aria-label="Fechar" onClick={onClose}><X size={20} /></button></div>
        {children}
      </div>
    </div>
  )
}

export function Confirmar({ titulo, texto, rotulo = 'Confirmar', perigo, onConfirm, onClose }:
  { titulo: string; texto: string; rotulo?: string; perigo?: boolean; onConfirm: () => Promise<void>; onClose: () => void }) {
  const [busy, setBusy] = useState(false)
  return (
    <Modal titulo={titulo} onClose={onClose}>
      <p style={{ marginTop: 0 }}>{texto}</p>
      <div className="modal-foot">
        <button className="btn btn-ghost" onClick={onClose}>Voltar</button>
        <Botao className={`btn ${perigo ? 'btn-danger' : 'btn-primary'}`} carregando={busy} onClick={async () => { setBusy(true); try { await onConfirm() } finally { setBusy(false) } }}>{rotulo}</Botao>
      </div>
    </Modal>
  )
}

export function Paginacao({ pagina, totalPaginas, total, onChange }: { pagina: number; totalPaginas: number; total: number; onChange: (p: number) => void }) {
  return (
    <div className="pager">
      <span>{total} registro(s)</span>
      <div className="row">
        <button className="btn btn-ghost btn-sm" disabled={pagina <= 0} onClick={() => onChange(pagina - 1)}>Anterior</button>
        <span>Página {Math.min(pagina + 1, Math.max(totalPaginas, 1))} de {Math.max(totalPaginas, 1)}</span>
        <button className="btn btn-ghost btn-sm" disabled={pagina + 1 >= totalPaginas} onClick={() => onChange(pagina + 1)}>Próxima</button>
      </div>
    </div>
  )
}

/** Extrai mensagem e erros por campo de uma exceção da API. */
export function errosDe(e: unknown): { msg: string; campos: Record<string, string> } {
  if (e instanceof ApiError) return { msg: e.message, campos: e.campos ?? {} }
  return { msg: 'Erro inesperado. Tente novamente.', campos: {} }
}
