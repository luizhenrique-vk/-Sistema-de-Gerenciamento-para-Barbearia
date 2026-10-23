import { useState } from 'react'
import { CalendarX, CheckCircle2, Phone, StickyNote } from 'lucide-react'
import { useAsync } from '../hooks/useAsync'
import { useToast } from '../hooks/useToast'
import { useAuth } from '../hooks/useAuth'
import { api, qs } from '../services/api'
import { brl, dataCurta, horaCurta, hoje, isoData } from '../services/format'
import { Confirmar, ErroBox, PageHead, Spinner, StatusBadge, Vazio, errosDe } from '../components/ui'
import DisponibilidadeEditor from '../components/DisponibilidadeEditor'
import type { Agendamento } from '../types'

function ItemAgenda({ a, onChange, mostrarData }: { a: Agendamento; onChange: () => void; mostrarData?: boolean }) {
  const toast = useToast()
  const [acao, setAcao] = useState<'FINALIZADO' | 'CANCELADO' | null>(null)
  const executar = async () => {
    try { await api.patch(`/agendamentos/${a.id}/status`, { status: acao }); toast.ok(acao === 'FINALIZADO' ? 'Atendimento finalizado.' : 'Agendamento cancelado.'); setAcao(null); onChange() }
    catch (e) { toast.erro(errosDe(e).msg); setAcao(null) }
  }
  const podeFinalizar = a.status === 'AGENDADO' && new Date(a.dataHora) <= new Date()
  return (
    <div className="agenda-item" style={{ flexWrap: 'wrap' }}>
      <div className="agenda-time">{horaCurta(a.dataHora)}{mostrarData && <div className="muted" style={{ fontSize: '.78rem', fontFamily: 'var(--font)' }}>{dataCurta(a.dataHora)}</div>}</div>
      <div style={{ flex: 1, minWidth: 200 }}>
        <strong>{a.clienteNome}</strong> <span className="muted">· {a.servicoNome} ({a.duracao} min · {brl(a.preco)})</span>
        <div className="muted row" style={{ gap: 14, fontSize: '.85rem' }}>
          {a.clienteTelefone && <span className="row" style={{ gap: 4 }}><Phone size={13} />{a.clienteTelefone}</span>}
          {a.observacoes && <span className="row" style={{ gap: 4 }}><StickyNote size={13} />{a.observacoes}</span>}
        </div>
      </div>
      <StatusBadge status={a.status} />
      {a.status === 'AGENDADO' && <div className="actions">
        <button className="btn btn-primary btn-sm" disabled={!podeFinalizar} title={podeFinalizar ? '' : 'Disponível a partir do horário do atendimento'} onClick={() => setAcao('FINALIZADO')}><CheckCircle2 size={15} /> Finalizar</button>
        <button className="btn btn-ghost btn-sm" onClick={() => setAcao('CANCELADO')}><CalendarX size={15} /> Cancelar</button></div>}
      {acao && <Confirmar perigo={acao === 'CANCELADO'} titulo={acao === 'FINALIZADO' ? 'Finalizar atendimento' : 'Cancelar agendamento'} texto={acao === 'FINALIZADO' ? `Confirmar que o atendimento de ${a.clienteNome} foi realizado?` : `Cancelar o atendimento de ${a.clienteNome}? O cliente será notificado.`} onConfirm={executar} onClose={() => setAcao(null)} />}
    </div>
  )
}

export function BarbeiroPainel() {
  const { usuario } = useAuth()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Agendamento[]>(`/barbeiro/agenda${qs({ data: hoje(), dias: 1 })}`), [])
  const ativos = (dados ?? []).filter((a) => a.status !== 'CANCELADO')
  return (
    <>
      <PageHead titulo={`Olá, ${usuario?.nome.split(' ')[0]}`} sub="Seus atendimentos de hoje." />
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <>
          <div className="grid g3 mb">
            <div className="card stat"><span className="label">Atendimentos hoje</span><span className="value">{ativos.length}</span></div>
            <div className="card stat"><span className="label">Já finalizados</span><span className="value">{ativos.filter((a) => a.status === 'FINALIZADO').length}</span></div>
            <div className="card stat"><span className="label">Ainda por vir</span><span className="value">{ativos.filter((a) => a.status === 'AGENDADO').length}</span></div>
          </div>
          <div className="card card-flush">{(dados ?? []).length === 0 ? <Vazio titulo="Nenhum atendimento hoje" texto="Quando houver agendamentos, eles aparecerão aqui." /> : dados!.map((a) => <ItemAgenda key={a.id} a={a} onChange={recarregar} />)}</div>
        </>
      )}
    </>
  )
}

export function BarbeiroAgenda() {
  const [data, setData] = useState(hoje()); const [dias, setDias] = useState(1)
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Agendamento[]>(`/barbeiro/agenda${qs({ data, dias })}`), [data, dias])
  const mover = (n: number) => { const d = new Date(data + 'T00:00:00'); d.setDate(d.getDate() + n * dias); setData(isoData(d)) }
  return (
    <>
      <PageHead titulo="Agenda" sub="Atendimentos agendados com dados dos clientes." />
      <div className="toolbar">
        <button className="btn btn-ghost" onClick={() => mover(-1)}>Anterior</button>
        <input className="input" type="date" aria-label="Data" value={data} onChange={(e) => e.target.value && setData(e.target.value)} />
        <button className="btn btn-ghost" onClick={() => mover(1)}>Próximo</button>
        <div className="tabs"><button aria-pressed={dias === 1} onClick={() => setDias(1)}>Dia</button><button aria-pressed={dias === 7} onClick={() => setDias(7)}>Semana</button></div>
      </div>
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{!dados?.length ? <Vazio titulo="Sem atendimentos neste período" /> : dados.map((a) => <ItemAgenda key={a.id} a={a} mostrarData={dias > 1} onChange={recarregar} />)}</div>
      )}
    </>
  )
}

export function BarbeiroDisponibilidade() {
  return (
    <>
      <PageHead titulo="Disponibilidade" sub="Informe os dias e horários em que você atende." />
      <div className="card"><DisponibilidadeEditor base="/barbeiro" /></div>
    </>
  )
}
