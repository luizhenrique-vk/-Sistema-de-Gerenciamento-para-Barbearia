import { useEffect, useState } from 'react'
import { Plus, Save, Trash2 } from 'lucide-react'
import { api } from '../services/api'
import { useToast } from '../hooks/useToast'
import { DIAS, hhmm } from '../services/format'
import type { Disponibilidade, HorarioFuncionamento } from '../types'
import { Botao, ErroBox, Spinner, errosDe } from './ui'

/** Edita as janelas de atendimento semanais. `base` = '/barbeiro' (próprio barbeiro) ou '/admin/barbeiros/{id}'. */
export default function DisponibilidadeEditor({ base, onSaved }: { base: string; onSaved?: () => void }) {
  const toast = useToast()
  const [itens, setItens] = useState<Disponibilidade[] | null>(null)
  const [func, setFunc] = useState<HorarioFuncionamento[]>([])
  const [erro, setErro] = useState(''); const [busy, setBusy] = useState(false)

  useEffect(() => {
    Promise.all([api.get<Disponibilidade[]>(`${base}/disponibilidade`), api.get<HorarioFuncionamento[]>(base === '/barbeiro' ? '/barbeiro/funcionamento' : '/admin/funcionamento')])
      .then(([d, f]) => { setItens(d.map((x) => ({ ...x, horaInicio: hhmm(x.horaInicio), horaFim: hhmm(x.horaFim) }))); setFunc(f) })
      .catch((e) => setErro(errosDe(e).msg))
  }, [base])

  if (itens === null) return erro ? <ErroBox msg={erro} /> : <Spinner />
  const upd = (i: number, p: Partial<Disponibilidade>) => setItens(itens.map((x, k) => (k === i ? { ...x, ...p } : x)))
  const add = (dia: number) => {
    const f = func.find((x) => x.diaSemana === dia)
    setItens([...itens, { diaSemana: dia, horaInicio: hhmm(f?.abertura ?? '09:00'), horaFim: hhmm(f?.fechamento ?? '18:00') }])
  }
  const salvar = async () => {
    setBusy(true); setErro('')
    try { await api.put(`${base}/disponibilidade`, { itens }); toast.ok('Disponibilidade salva.'); onSaved?.() } catch (e) { setErro(errosDe(e).msg) } finally { setBusy(false) }
  }
  return (
    <div>
      {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
      <p className="muted">Defina os dias e horários em que há atendimento. Os horários devem estar dentro do funcionamento da barbearia.</p>
      {func.map((f) => {
        const doDia = itens.map((x, i) => ({ x, i })).filter((o) => o.x.diaSemana === f.diaSemana)
        return (
          <div key={f.diaSemana} className="week-row">
            <div><strong>{DIAS[f.diaSemana]}</strong><div className="muted" style={{ fontSize: '.8rem' }}>{f.aberto ? `Aberto ${hhmm(f.abertura)}–${hhmm(f.fechamento)}` : 'Fechado'}</div></div>
            <div>{f.aberto && <button className="btn btn-ghost btn-sm" onClick={() => add(f.diaSemana)}><Plus size={14} /> Janela</button>}</div>
            <div className="grid" style={{ gap: 8 }}>
              {doDia.length === 0 && <span className="muted">{f.aberto ? 'Sem atendimento' : '—'}</span>}
              {doDia.map(({ x, i }) => (
                <div key={i} className="row" style={{ gap: 8 }}>
                  <input className="input" style={{ width: 120 }} type="time" aria-label={`Início ${DIAS[x.diaSemana]}`} value={x.horaInicio} onChange={(e) => upd(i, { horaInicio: e.target.value })} />
                  <span>até</span>
                  <input className="input" style={{ width: 120 }} type="time" aria-label={`Fim ${DIAS[x.diaSemana]}`} value={x.horaFim} onChange={(e) => upd(i, { horaFim: e.target.value })} />
                  <button className="icon-btn" style={{ color: 'var(--bad)' }} aria-label="Remover janela" onClick={() => setItens(itens.filter((_, k) => k !== i))}><Trash2 size={17} /></button>
                </div>))}
            </div>
          </div>)
      })}
      <div className="modal-foot"><Botao className="btn btn-primary" carregando={busy} onClick={salvar}><Save size={16} /> Salvar disponibilidade</Botao></div>
    </div>
  )
}
