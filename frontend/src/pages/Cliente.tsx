import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarCheck, CalendarX, Clock, Scissors, Star, UserRound } from 'lucide-react'
import { useAsync } from '../hooks/useAsync'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api, qs } from '../services/api'
import { brl, dataHora, dataCurta, hoje, hhmm } from '../services/format'
import { Botao, Confirmar, ErroBox, Modal, PageHead, Spinner, StatusBadge, Textarea, Vazio, errosDe } from '../components/ui'
import type { Agendamento, Barbeiro, Servico } from '../types'

export function ClienteInicio() {
  const { usuario } = useAuth()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Agendamento[]>('/agendamentos/meus'), [])
  const proximos = (dados ?? []).filter((a) => a.status === 'AGENDADO' && new Date(a.dataHora) > new Date()).sort((a, b) => a.dataHora.localeCompare(b.dataHora))
  const feitos = (dados ?? []).filter((a) => a.status === 'FINALIZADO').length
  return (
    <>
      <PageHead titulo={`Olá, ${usuario?.nome.split(' ')[0]}`} sub="Acompanhe seus horários e agende um novo atendimento.">
        <Link to="/cliente/agendar" className="btn btn-primary"><CalendarCheck size={18} /> Novo agendamento</Link>
      </PageHead>
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="grid g2" style={{ alignItems: 'start' }}>
          <div className="card">
            <h2 className="mb">Próximo atendimento</h2>
            {proximos[0] ? (
              <>
                <p style={{ fontSize: '1.3rem', fontFamily: 'var(--font-display)', fontWeight: 700, margin: 0 }}>{dataHora(proximos[0].dataHora)}</p>
                <p className="muted">{proximos[0].servicoNome} com {proximos[0].barbeiroNome} · {brl(proximos[0].preco)}</p>
                <Link to="/cliente/agendamentos" className="btn btn-ghost btn-sm">Gerenciar agendamentos</Link>
              </>
            ) : <Vazio titulo="Nenhum horário marcado" texto="Que tal agendar seu próximo corte?" acao={<Link to="/cliente/agendar" className="btn btn-primary mt">Agendar agora</Link>} />}
          </div>
          <div className="card stat"><span className="label"><Scissors size={16} /> Atendimentos realizados</span><span className="value">{feitos}</span>
            <span className="sub">{proximos.length} agendamento(s) futuro(s)</span></div>
        </div>
      )}
    </>
  )
}

export function ClienteServicos() {
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Servico[]>('/servicos'), [])
  return (
    <>
      <PageHead titulo="Serviços" sub="Valores e duração estimada de cada atendimento." />
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : !dados?.length ? <div className="card"><Vazio titulo="Nenhum serviço disponível" texto="A barbearia ainda não cadastrou serviços." /></div> : (
        <div className="grid g3">{dados.map((s) => (
          <div key={s.id} className="card"><h3>{s.nome}</h3><p className="muted" style={{ minHeight: 44 }}>{s.descricao ?? 'Sem descrição.'}</p>
            <div className="row" style={{ justifyContent: 'space-between' }}><strong style={{ fontSize: '1.2rem' }}>{brl(s.preco)}</strong><span className="muted row" style={{ gap: 4 }}><Clock size={15} />{s.duracao} min</span></div>
            <Link to={`/cliente/agendar?servico=${s.id}`} className="btn btn-ghost btn-sm mt">Agendar este serviço</Link></div>))}</div>
      )}
    </>
  )
}

export function ClienteProfissionais() {
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Barbeiro[]>('/barbeiros'), [])
  return (
    <>
      <PageHead titulo="Profissionais" sub="Conheça os barbeiros e escolha com quem agendar." />
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : !dados?.length ? <div className="card"><Vazio titulo="Nenhum profissional disponível" /></div> : (
        <div className="grid g3">{dados.map((b) => (
          <div key={b.id} className="card"><div className="row"><UserRound size={28} color="var(--copper)" /><div><h3>{b.nome}</h3><span className="muted">{b.especialidade}</span></div></div>
            <p className="row" style={{ gap: 6 }}><Star size={16} color="var(--copper)" />{b.mediaAvaliacao ? <strong>{b.mediaAvaliacao.toFixed(1)}</strong> : <span className="muted">sem avaliações</span>}</p>
            <Link to={`/cliente/agendar?barbeiro=${b.id}`} className="btn btn-ghost btn-sm">Agendar com {b.nome.split(' ')[0]}</Link></div>))}</div>
      )}
    </>
  )
}

export function Agendar() {
  const toast = useToast()
  const params = new URLSearchParams(window.location.search)
  const servicos = useAsync(() => api.get<Servico[]>('/servicos'), [])
  const barbeiros = useAsync(() => api.get<Barbeiro[]>('/barbeiros'), [])
  const [servicoId, setServicoId] = useState<number | null>(params.get('servico') ? Number(params.get('servico')) : null)
  const [barbeiroId, setBarbeiroId] = useState<number | null>(params.get('barbeiro') ? Number(params.get('barbeiro')) : null)
  const [data, setData] = useState(hoje()); const [hora, setHora] = useState<string | null>(null)
  const [obs, setObs] = useState(''); const [horarios, setHorarios] = useState<string[] | null>(null); const [errHor, setErrHor] = useState('')
  const [busy, setBusy] = useState(false); const [erro, setErro] = useState(''); const [feito, setFeito] = useState<Agendamento | null>(null)

  const servico = servicos.dados?.find((s) => s.id === servicoId)
  const barbeiro = barbeiros.dados?.find((b) => b.id === barbeiroId)

  useEffect(() => {
    setHora(null); setHorarios(null); setErrHor('')
    if (!servicoId || !barbeiroId || !data) return
    let vivo = true
    api.get<{ horarios: string[] }>(`/barbeiros/${barbeiroId}/horarios${qs({ servicoId, data })}`)
      .then((r) => vivo && setHorarios(r.horarios)).catch((e) => vivo && setErrHor(errosDe(e).msg))
    return () => { vivo = false }
  }, [servicoId, barbeiroId, data])

  const confirmar = async () => {
    if (!servicoId || !barbeiroId || !hora) return
    setBusy(true); setErro('')
    try {
      const ag = await api.post<Agendamento>('/agendamentos', { servicoId, barbeiroId, dataHora: `${data}T${hhmm(hora)}:00`, observacoes: obs || null })
      setFeito(ag); toast.ok('Agendamento confirmado!')
    } catch (e) {
      setErro(errosDe(e).msg); setHora(null)
      api.get<{ horarios: string[] }>(`/barbeiros/${barbeiroId}/horarios${qs({ servicoId, data })}`).then((r) => setHorarios(r.horarios)).catch(() => {})
    } finally { setBusy(false) }
  }

  if (servicos.carregando || barbeiros.carregando) return <Spinner />
  if (servicos.erro || barbeiros.erro) return <ErroBox msg={(servicos.erro ?? barbeiros.erro)!} onRetry={() => { servicos.recarregar(); barbeiros.recarregar() }} />
  if (feito) return (
    <div className="card" style={{ maxWidth: 520, margin: '40px auto', textAlign: 'center' }}>
      <CalendarCheck size={44} color="var(--ok)" /><h2 className="mt">Agendamento confirmado</h2>
      <p>{feito.servicoNome} com <strong>{feito.barbeiroNome}</strong><br />{dataHora(feito.dataHora)}</p>
      <div className="row" style={{ justifyContent: 'center' }}><Link to="/cliente/agendamentos" className="btn btn-primary">Ver meus agendamentos</Link><button className="btn btn-ghost" onClick={() => { setFeito(null); setHora(null) }}>Novo agendamento</button></div>
    </div>
  )
  const semDados = !servicos.dados?.length || !barbeiros.dados?.length
  return (
    <>
      <PageHead titulo="Agendar atendimento" sub="Escolha o serviço, o profissional e um horário disponível." />
      {semDados ? <div className="card"><Vazio titulo="Ainda não é possível agendar" texto="A barbearia precisa cadastrar ao menos um serviço e um barbeiro." /></div> : (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) 300px', alignItems: 'start' }}>
          <div className="grid">
            {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
            <section className="card"><h2 className="mb">1. Serviço</h2>
              <div className="grid g2">{servicos.dados!.map((s) => (
                <button key={s.id} className="opt" aria-pressed={servicoId === s.id} onClick={() => setServicoId(s.id)}>
                  <Scissors size={20} color="var(--copper)" /><div><div className="t">{s.nome}</div><div className="s">{s.duracao} min · {brl(s.preco)}</div></div></button>))}</div></section>
            <section className="card"><h2 className="mb">2. Profissional</h2>
              <div className="grid g2">{barbeiros.dados!.map((b) => (
                <button key={b.id} className="opt" aria-pressed={barbeiroId === b.id} onClick={() => setBarbeiroId(b.id)}>
                  <UserRound size={20} color="var(--copper)" /><div><div className="t">{b.nome}</div><div className="s">{b.especialidade}</div></div></button>))}</div></section>
            <section className="card"><h2 className="mb">3. Data e horário</h2>
              <div className="field" style={{ maxWidth: 220 }}><label htmlFor="data">Data</label><input id="data" className="input" type="date" min={hoje()} value={data} onChange={(e) => setData(e.target.value)} /></div>
              {!servicoId || !barbeiroId ? <p className="muted">Selecione o serviço e o profissional para ver os horários.</p>
                : errHor ? <ErroBox msg={errHor} /> : horarios === null ? <Spinner texto="Buscando horários…" />
                : horarios.length === 0 ? <div className="alert alert-info">Sem horários livres nesta data. Tente outro dia ou outro profissional.</div>
                : <div className="slots" role="group" aria-label="Horários disponíveis">{horarios.map((h) => <button key={h} className="slot" aria-pressed={hora === h} onClick={() => setHora(h)}>{hhmm(h)}</button>)}</div>}
              <div className="mt"><Textarea label="Observações (opcional)" value={obs} onChange={(e) => setObs(e.target.value)} maxLength={400} /></div></section>
          </div>
          <aside className="card" style={{ position: 'sticky', top: 20 }}>
            <h2>Resumo</h2>
            <dl className="summary">
              <dt>Serviço</dt><dd>{servico?.nome ?? '—'}</dd><dt>Profissional</dt><dd>{barbeiro?.nome ?? '—'}</dd>
              <dt>Data</dt><dd>{data ? dataCurta(data + 'T00:00:00') : '—'}</dd><dt>Horário</dt><dd>{hora ? hhmm(hora) : '—'}</dd>
              <dt>Valor</dt><dd style={{ fontSize: '1.3rem' }}>{brl(servico?.preco ?? 0)}</dd></dl>
            <Botao className="btn btn-primary btn-block mt" disabled={!hora} carregando={busy} onClick={confirmar}><CalendarCheck size={18} /> Confirmar agendamento</Botao>
          </aside>
        </div>
      )}
    </>
  )
}

export function MeusAgendamentos() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Agendamento[]>('/agendamentos/meus'), [])
  const [cancelar, setCancelar] = useState<Agendamento | null>(null)
  const [aviso, setAviso] = useState('')
  const lista = useMemo(() => (dados ?? []).filter((a) => a.status === 'AGENDADO').sort((a, b) => a.dataHora.localeCompare(b.dataHora)), [dados])
  const recentes = (dados ?? []).filter((a) => a.status === 'CANCELADO').slice(0, 5)
  const confirmar = async () => {
    if (!cancelar) return
    try { await api.post(`/agendamentos/${cancelar.id}/cancelar`); toast.ok('Agendamento cancelado.'); setCancelar(null); recarregar() }
    catch (e) { setAviso(errosDe(e).msg); setCancelar(null) }
  }
  return (
    <>
      <PageHead titulo="Meus agendamentos" sub="Atendimentos marcados. O cancelamento exige antecedência mínima de 2 horas."><Link to="/cliente/agendar" className="btn btn-primary"><CalendarCheck size={18} /> Novo</Link></PageHead>
      {aviso && <div className="alert alert-warn" role="alert">{aviso}</div>}
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <>
          <div className="card card-flush">{lista.length === 0 ? <Vazio titulo="Você não tem agendamentos ativos" texto="Escolha um serviço e um horário para começar." acao={<Link to="/cliente/agendar" className="btn btn-primary mt">Agendar</Link>} /> : lista.map((a) => (
            <div key={a.id} className="agenda-item"><div className="agenda-time">{dataCurta(a.dataHora).slice(0, 5)}<div className="muted" style={{ fontSize: '.8rem', fontFamily: 'var(--font)' }}>{dataHora(a.dataHora).split('· ')[1]}</div></div>
              <div style={{ flex: 1 }}><strong>{a.servicoNome}</strong><div className="muted">com {a.barbeiroNome} · {a.duracao} min · {brl(a.preco)}</div></div>
              <StatusBadge status={a.status} /><button className="btn btn-ghost btn-sm" onClick={() => setCancelar(a)}><CalendarX size={15} /> Cancelar</button></div>))}</div>
          {recentes.length > 0 && <p className="muted mt">Cancelados recentemente: {recentes.map((a) => `${a.servicoNome} (${dataCurta(a.dataHora)})`).join(', ')}.</p>}
        </>
      )}
      {cancelar && <Confirmar perigo titulo="Cancelar agendamento" rotulo="Cancelar agendamento" texto={`Deseja cancelar ${cancelar.servicoNome} com ${cancelar.barbeiroNome} em ${dataHora(cancelar.dataHora)}?`} onConfirm={confirmar} onClose={() => setCancelar(null)} />}
    </>
  )
}

export function Historico() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Agendamento[]>('/agendamentos/historico'), [])
  const [av, setAv] = useState<Agendamento | null>(null); const [nota, setNota] = useState(5); const [com, setCom] = useState(''); const [busy, setBusy] = useState(false); const [err, setErr] = useState('')
  const enviar = async () => {
    if (!av) return
    setBusy(true); setErr('')
    try { await api.post('/avaliacoes', { agendamentoId: av.id, nota, comentario: com || null }); toast.ok('Avaliação registrada. Obrigado!'); setAv(null); setCom(''); setNota(5); recarregar() }
    catch (e) { setErr(errosDe(e).msg) } finally { setBusy(false) }
  }
  return (
    <>
      <PageHead titulo="Histórico de atendimentos" sub="Serviços já realizados e avaliações." />
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{!dados?.length ? <Vazio titulo="Nenhum atendimento realizado ainda" texto="Seus atendimentos finalizados aparecerão aqui." /> : (
          <div className="table-wrap"><table><thead><tr><th>Data</th><th>Serviço</th><th>Profissional</th><th>Valor</th><th /></tr></thead><tbody>{dados.map((a) => (
            <tr key={a.id}><td>{dataHora(a.dataHora)}</td><td>{a.servicoNome}</td><td>{a.barbeiroNome}</td><td>{brl(a.preco)}</td>
              <td className="right">{a.avaliado ? <span className="badge b-ok"><Star size={13} /> Avaliado</span> : <button className="btn btn-ghost btn-sm" onClick={() => { setAv(a); setErr('') }}><Star size={14} /> Avaliar</button>}</td></tr>))}</tbody></table></div>)}</div>
      )}
      {av && (
        <Modal titulo="Avaliar atendimento" onClose={() => setAv(null)}>
          <p style={{ marginTop: 0 }}>{av.servicoNome} com {av.barbeiroNome} em {dataCurta(av.dataHora)}</p>
          {err && <div className="alert alert-erro" role="alert">{err}</div>}
          <div className="field"><span style={{ fontWeight: 600, fontSize: '.88rem' }} id="nota-l">Nota</span>
            <div className="stars" role="radiogroup" aria-labelledby="nota-l">{[1, 2, 3, 4, 5].map((n) => (
              <button key={n} role="radio" aria-checked={nota === n} aria-label={`${n} estrela(s)`} onClick={() => setNota(n)}><Star size={28} fill={n <= nota ? 'currentColor' : 'none'} /></button>))}</div></div>
          <Textarea label="Comentário (opcional)" value={com} onChange={(e) => setCom(e.target.value)} maxLength={500} />
          <div className="modal-foot"><button className="btn btn-ghost" onClick={() => setAv(null)}>Voltar</button><Botao className="btn btn-primary" carregando={busy} onClick={enviar}>Enviar avaliação</Botao></div>
        </Modal>
      )}
    </>
  )
}

