import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CalendarCheck, CalendarX, CheckCircle2, Clock, Pencil, Plus, Save, Search, Trash2, TrendingUp, Wallet, CalendarDays } from 'lucide-react'
import { useAsync } from '../hooks/useAsync'
import { useToast } from '../hooks/useToast'
import { api, qs } from '../services/api'
import { brl, dataCurta, dataHora, DIAS, hhmm, hoje, isoData, PAGAMENTO_LABEL, TIPO_LABEL } from '../services/format'
import { Botao, Confirmar, ErroBox, Input, Modal, PageHead, Paginacao, Select, Spinner, StatusBadge, Textarea, Vazio, errosDe } from '../components/ui'
import DisponibilidadeEditor from '../components/DisponibilidadeEditor'
import type { Agendamento, Barbeiro, Dashboard, FinanceiroResponse, FormaPagamento, HorarioFuncionamento, Pagina, Servico, ServicoRanking, StatusAgendamento, TipoUsuario, Usuario } from '../types'

const COBRE = '#b4532a'

function Estat({ icon, label, valor, sub }: { icon: React.ReactNode; label: string; valor: string | number; sub?: string }) {
  return <div className="card stat"><span className="label">{icon}{label}</span><span className="value">{valor}</span>{sub && <span className="sub">{sub}</span>}</div>
}
function Ranking({ itens }: { itens: ServicoRanking[] }) {
  const max = Math.max(1, ...itens.map((i) => i.quantidade))
  if (!itens.length) return <Vazio titulo="Sem atendimentos finalizados" texto="O ranking aparece quando houver serviços concluídos no período." />
  return <div className="bars">{itens.map((i) => (
    <div key={i.servicoId} className="bar-row"><span>{i.nome}</span><strong>{i.quantidade}× <span className="muted" style={{ fontWeight: 400 }}>· {brl(i.valor)}</span></strong>
      <div className="bar-track"><div className="bar-fill" style={{ width: `${(i.quantidade / max) * 100}%` }} /></div></div>))}</div>
}

// ---------- Dashboard ----------
export function AdminDashboard() {
  const { dados: d, carregando, erro, recarregar } = useAsync(() => api.get<Dashboard>('/admin/relatorios/dashboard'), [])
  if (carregando) return <Spinner />
  if (erro || !d) return <ErroBox msg={erro ?? 'Falha ao carregar.'} onRetry={recarregar} />
  const vazio = d.mes.total === 0
  return (
    <>
      <PageHead titulo="Dashboard" sub="Resumo da operação, calculado com os registros reais do sistema." />
      {vazio && (
        <div className="alert alert-info"><span>Ainda não há agendamentos neste mês. Para começar, <Link to="/admin/servicos">cadastre os serviços</Link>, <Link to="/admin/barbeiros">cadastre os barbeiros</Link> e defina o <Link to="/admin/horarios">horário de funcionamento</Link>.</span></div>
      )}
      <h2 className="mb">Hoje</h2>
      <div className="grid g4 mb">
        <Estat icon={<CalendarCheck size={16} />} label="Agendamentos" valor={d.hoje.total} sub={`${d.hoje.agendados} por vir`} />
        <Estat icon={<CheckCircle2 size={16} />} label="Atendimentos realizados" valor={d.hoje.finalizados} />
        <Estat icon={<CalendarX size={16} />} label="Cancelados" valor={d.hoje.cancelados} />
        <Estat icon={<Wallet size={16} />} label="Faturamento registrado" valor={brl(d.hoje.faturamento)} />
      </div>
      <h2 className="mb">Mês atual</h2>
      <div className="grid g4 mb">
        <Estat icon={<CalendarCheck size={16} />} label="Agendamentos" valor={d.mes.total} />
        <Estat icon={<CheckCircle2 size={16} />} label="Atendimentos realizados" valor={d.mes.finalizados} />
        <Estat icon={<CalendarX size={16} />} label="Cancelados" valor={d.mes.cancelados} />
        <Estat icon={<Wallet size={16} />} label="Faturamento registrado" valor={brl(d.mes.faturamento)} sub={d.mediaAvaliacao ? `Avaliação média ${d.mediaAvaliacao.toFixed(1)}/5` : 'Sem avaliações'} />
      </div>
      <div className="grid g2" style={{ alignItems: 'start' }}>
        <div className="card"><h2 className="mb">Atendimentos nos últimos 7 dias</h2>
          <div style={{ height: 240 }}><ResponsiveContainer><BarChart data={d.ultimosDias.map((x) => ({ dia: dataCurta(x.data + 'T00:00:00').slice(0, 5), qtd: x.agendamentos }))}>
            <CartesianGrid vertical={false} stroke="#e3dccd" /><XAxis dataKey="dia" tickLine={false} /><YAxis allowDecimals={false} width={28} tickLine={false} axisLine={false} />
            <Tooltip formatter={(v) => [v, 'Agendamentos']} /><Bar dataKey="qtd" fill={COBRE} radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></div></div>
        <div className="card"><h2 className="mb">Serviços mais realizados (mês)</h2><Ranking itens={d.topServicos} /></div>
      </div>
    </>
  )
}

// ---------- Agendamentos ----------
function NovoAgendamento({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const toast = useToast()
  const clientes = useAsync(() => api.get<Pagina<Usuario>>('/admin/usuarios?tipo=CLIENTE&tamanho=50'), [])
  const servicos = useAsync(() => api.get<Servico[]>('/servicos'), [])
  const barbeiros = useAsync(() => api.get<Barbeiro[]>('/barbeiros'), [])
  const [f, setF] = useState({ clienteId: '', servicoId: '', barbeiroId: '', data: hoje(), hora: '', obs: '' })
  const [horarios, setHorarios] = useState<string[] | null>(null); const [erro, setErro] = useState(''); const [busy, setBusy] = useState(false)
  useEffect(() => {
    setF((o) => ({ ...o, hora: '' })); setHorarios(null)
    if (!f.servicoId || !f.barbeiroId || !f.data) return
    api.get<{ horarios: string[] }>(`/barbeiros/${f.barbeiroId}/horarios${qs({ servicoId: f.servicoId, data: f.data })}`).then((r) => setHorarios(r.horarios)).catch((e) => setErro(errosDe(e).msg))
  }, [f.servicoId, f.barbeiroId, f.data])
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => setF({ ...f, [k]: e.target.value })
  const serv = servicos.dados?.find((s) => String(s.id) === f.servicoId)
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErro('')
    try {
      await api.post('/agendamentos', { clienteId: Number(f.clienteId), servicoId: Number(f.servicoId), barbeiroId: Number(f.barbeiroId), dataHora: `${f.data}T${hhmm(f.hora)}:00`, observacoes: f.obs || null })
      toast.ok('Agendamento criado.'); onSaved()
    } catch (x) { setErro(errosDe(x).msg) } finally { setBusy(false) }
  }
  return (
    <Modal titulo="Agendar atendimento" onClose={onClose} largo>
      <form onSubmit={salvar}>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <div className="grid g2">
          <Select label="Cliente" value={f.clienteId} onChange={set('clienteId')} required><option value="">Selecione o cliente</option>{clientes.dados?.conteudo.filter((c) => c.ativo).map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}</Select>
          <Select label="Serviço" value={f.servicoId} onChange={set('servicoId')} required><option value="">Selecione o serviço</option>{servicos.dados?.map((s) => <option key={s.id} value={s.id}>{s.nome} — {brl(s.preco)}</option>)}</Select>
          <Select label="Barbeiro" value={f.barbeiroId} onChange={set('barbeiroId')} required><option value="">Selecione o barbeiro</option>{barbeiros.dados?.map((b) => <option key={b.id} value={b.id}>{b.nome}</option>)}</Select>
          <Input label="Data" type="date" min={hoje()} value={f.data} onChange={set('data')} required />
        </div>
        <Select label="Horário" value={f.hora} onChange={set('hora')} required disabled={!horarios?.length} dica={horarios && !horarios.length ? 'Sem horários livres nesta data.' : undefined}>
          <option value="">{horarios === null ? 'Selecione serviço, barbeiro e data' : 'Selecione o horário disponível'}</option>{horarios?.map((h) => <option key={h} value={h}>{hhmm(h)}</option>)}</Select>
        <Textarea label="Observações (opcional)" value={f.obs} onChange={set('obs')} maxLength={400} />
        <p className="muted">Valor estimado: <strong>{brl(serv?.preco ?? 0)}</strong></p>
        <div className="modal-foot"><button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button><Botao className="btn btn-primary" carregando={busy} disabled={!f.hora || !f.clienteId}><CalendarCheck size={16} /> Confirmar agendamento</Botao></div>
      </form>
    </Modal>
  )
}

export function AdminAgendamentos() {
  const toast = useToast()
  const ini = new Date(); ini.setDate(ini.getDate() - 7); const fim = new Date(); fim.setDate(fim.getDate() + 30)
  const [filtro, setFiltro] = useState({ de: isoData(ini), ate: isoData(fim), status: '', barbeiroId: '' }); const [pagina, setPagina] = useState(0)
  const barbeiros = useAsync(() => api.get<Barbeiro[]>('/admin/barbeiros'), [])
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Pagina<Agendamento>>(`/admin/agendamentos${qs({ ...filtro, pagina, tamanho: 12 })}`), [filtro, pagina])
  const [novo, setNovo] = useState(false); const [acao, setAcao] = useState<{ a: Agendamento; status: StatusAgendamento } | null>(null)
  const executar = async () => {
    if (!acao) return
    try { await api.patch(`/agendamentos/${acao.a.id}/status`, { status: acao.status }); toast.ok('Status atualizado.'); recarregar() } catch (e) { toast.erro(errosDe(e).msg) } finally { setAcao(null) }
  }
  const setF = (k: keyof typeof filtro) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => { setPagina(0); setFiltro({ ...filtro, [k]: e.target.value }) }
  return (
    <>
      <PageHead titulo="Agendamentos" sub="Consulta e gerenciamento de todos os atendimentos."><button className="btn btn-primary" onClick={() => setNovo(true)}><Plus size={18} /> Novo agendamento</button></PageHead>
      <div className="toolbar">
        <input className="input" type="date" aria-label="De" value={filtro.de} onChange={setF('de')} /><input className="input" type="date" aria-label="Até" value={filtro.ate} onChange={setF('ate')} />
        <select className="input" aria-label="Status" value={filtro.status} onChange={setF('status')}><option value="">Todos os status</option><option value="AGENDADO">Agendado</option><option value="FINALIZADO">Finalizado</option><option value="CANCELADO">Cancelado</option></select>
        <select className="input" aria-label="Barbeiro" value={filtro.barbeiroId} onChange={setF('barbeiroId')}><option value="">Todos os barbeiros</option>{barbeiros.dados?.map((b) => <option key={b.id} value={b.id}>{b.nome}</option>)}</select>
      </div>
      {erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{carregando && !dados ? <Spinner /> : !dados?.conteudo.length ? <Vazio titulo="Nenhum agendamento encontrado" texto="Ajuste os filtros ou crie um novo agendamento." /> : (
          <>
            <div className="table-wrap"><table><thead><tr><th>Data / hora</th><th>Cliente</th><th>Serviço</th><th>Barbeiro</th><th>Valor</th><th>Status</th><th /></tr></thead><tbody>{dados.conteudo.map((a) => (
              <tr key={a.id}><td>{dataHora(a.dataHora)}</td><td>{a.clienteNome}</td><td>{a.servicoNome}</td><td>{a.barbeiroNome}</td><td>{brl(a.preco)}</td><td><StatusBadge status={a.status} /></td>
                <td><div className="actions">{a.status === 'AGENDADO' && <>
                  <button className="btn btn-ghost btn-sm" disabled={new Date(a.dataHora) > new Date()} title={new Date(a.dataHora) > new Date() ? 'Disponível a partir do horário do atendimento' : ''} onClick={() => setAcao({ a, status: 'FINALIZADO' })}>Finalizar</button>
                  <button className="btn btn-ghost btn-sm" onClick={() => setAcao({ a, status: 'CANCELADO' })}>Cancelar</button></>}</div></td></tr>))}</tbody></table></div>
            <Paginacao pagina={dados.pagina} totalPaginas={dados.totalPaginas} total={dados.total} onChange={setPagina} />
          </>)}</div>
      )}
      {novo && <NovoAgendamento onClose={() => setNovo(false)} onSaved={() => { setNovo(false); recarregar() }} />}
      {acao && <Confirmar perigo={acao.status === 'CANCELADO'} titulo={acao.status === 'CANCELADO' ? 'Cancelar agendamento' : 'Finalizar atendimento'} texto={`${acao.status === 'CANCELADO' ? 'Cancelar' : 'Finalizar'} ${acao.a.servicoNome} de ${acao.a.clienteNome} em ${dataHora(acao.a.dataHora)}?`} onConfirm={executar} onClose={() => setAcao(null)} />}
    </>
  )
}

// ---------- Pagamentos ----------
export function AdminPagamentos() {
  const toast = useToast()
  const ini = new Date(); ini.setDate(ini.getDate() - 30)
  const [de, setDe] = useState(isoData(ini)); const [ate, setAte] = useState(hoje()); const [pagina, setPagina] = useState(0)
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Pagina<Agendamento>>(`/admin/agendamentos${qs({ de, ate, status: 'FINALIZADO', pagina, tamanho: 12 })}`), [de, ate, pagina])
  const [alvo, setAlvo] = useState<Agendamento | null>(null); const [forma, setForma] = useState<FormaPagamento>('PIX'); const [busy, setBusy] = useState(false)
  const registrar = async () => {
    if (!alvo) return
    setBusy(true)
    try { await api.patch(`/admin/agendamentos/${alvo.id}/pagamento`, { forma }); toast.ok('Pagamento registrado.'); setAlvo(null); recarregar() } catch (e) { toast.erro(errosDe(e).msg) } finally { setBusy(false) }
  }
  return (
    <>
      <PageHead titulo="Pagamentos" sub="Registre o recebimento dos serviços realizados." />
      <div className="toolbar"><input className="input" type="date" aria-label="De" value={de} onChange={(e) => { setPagina(0); setDe(e.target.value) }} /><input className="input" type="date" aria-label="Até" value={ate} onChange={(e) => { setPagina(0); setAte(e.target.value) }} /></div>
      {erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{carregando && !dados ? <Spinner /> : !dados?.conteudo.length ? <Vazio titulo="Nenhum atendimento finalizado no período" texto="Os atendimentos concluídos aparecem aqui para registro de pagamento." /> : (
          <>
            <div className="table-wrap"><table><thead><tr><th>Atendimento</th><th>Cliente</th><th>Serviço</th><th>Valor</th><th>Pagamento</th><th /></tr></thead><tbody>{dados.conteudo.map((a) => (
              <tr key={a.id}><td>{dataHora(a.dataHora)}</td><td>{a.clienteNome}</td><td>{a.servicoNome}</td><td>{brl(a.preco)}</td>
                <td>{a.pago ? <span className="badge b-ok">Pago · {a.formaPagamento && PAGAMENTO_LABEL[a.formaPagamento]}</span> : <span className="badge b-warn">Pendente</span>}</td>
                <td className="right">{!a.pago && <button className="btn btn-primary btn-sm" onClick={() => { setAlvo(a); setForma('PIX') }}><Wallet size={14} /> Registrar</button>}</td></tr>))}</tbody></table></div>
            <Paginacao pagina={dados.pagina} totalPaginas={dados.totalPaginas} total={dados.total} onChange={setPagina} />
          </>)}</div>
      )}
      {alvo && (
        <Modal titulo="Registrar pagamento" onClose={() => setAlvo(null)}>
          <p style={{ marginTop: 0 }}>{alvo.servicoNome} · {alvo.clienteNome}<br /><strong style={{ fontSize: '1.3rem' }}>{brl(alvo.preco)}</strong></p>
          <Select label="Forma de pagamento" value={forma} onChange={(e) => setForma(e.target.value as FormaPagamento)}>{(Object.keys(PAGAMENTO_LABEL) as FormaPagamento[]).map((k) => <option key={k} value={k}>{PAGAMENTO_LABEL[k]}</option>)}</Select>
          <div className="modal-foot"><button className="btn btn-ghost" onClick={() => setAlvo(null)}>Voltar</button><Botao className="btn btn-primary" carregando={busy} onClick={registrar}>Confirmar recebimento</Botao></div>
        </Modal>
      )}
    </>
  )
}

// ---------- Relatórios ----------
export function AdminRelatorios() {
  const [periodo, setPeriodo] = useState<'DIA' | 'SEMANA' | 'MES'>('SEMANA'); const [data, setData] = useState(hoje())
  const fin = useAsync(() => api.get<FinanceiroResponse>(`/admin/relatorios/financeiro${qs({ periodo, data })}`), [periodo, data])
  const rank = useAsync(() => fin.dados ? api.get<ServicoRanking[]>(`/admin/relatorios/servicos-mais-realizados${qs({ de: fin.dados.inicio, ate: fin.dados.fim })}`) : Promise.resolve([] as ServicoRanking[]), [fin.dados?.inicio, fin.dados?.fim])
  const f = fin.dados
  return (
    <>
      <PageHead titulo="Relatórios financeiros" sub="Faturamento considera atendimentos finalizados com pagamento registrado.">
        <div className="tabs" role="group" aria-label="Período">{(['DIA', 'SEMANA', 'MES'] as const).map((p) => <button key={p} aria-pressed={periodo === p} onClick={() => setPeriodo(p)}>{p === 'DIA' ? 'Diário' : p === 'SEMANA' ? 'Semanal' : 'Mensal'}</button>)}</div>
        <input className="input" style={{ width: 'auto' }} type="date" aria-label="Data de referência" value={data} onChange={(e) => e.target.value && setData(e.target.value)} />
      </PageHead>
      {fin.erro ? <ErroBox msg={fin.erro} onRetry={fin.recarregar} /> : !f ? <Spinner /> : (
        <>
          <p className="muted">Período: {dataCurta(f.inicio + 'T00:00:00')} a {dataCurta(f.fim + 'T00:00:00')}</p>
          <div className="grid g4 mb">
            <Estat icon={<TrendingUp size={16} />} label="Faturamento" valor={brl(f.faturamento)} />
            <Estat icon={<CheckCircle2 size={16} />} label="Atendimentos pagos" valor={f.atendimentos} sub={`Ticket médio ${brl(f.ticketMedio)}`} />
            <Estat icon={<Clock size={16} />} label="A receber" valor={brl(f.pendenteRecebimento)} sub="Finalizados sem pagamento" />
            <Estat icon={<CalendarX size={16} />} label="Cancelamentos" valor={f.cancelados} />
          </div>
          <div className="grid g2" style={{ alignItems: 'start' }}>
            <div className="card"><h2 className="mb">Faturamento por {periodo === 'DIA' ? 'hora' : 'dia'}</h2>
              {f.faturamento === 0 ? <Vazio titulo="Sem faturamento registrado" texto="Registre pagamentos em Pagamentos para ver os valores aqui." /> : (
                <div style={{ height: 280 }}><ResponsiveContainer><BarChart data={f.serie}><CartesianGrid vertical={false} stroke="#e3dccd" /><XAxis dataKey="rotulo" tickLine={false} fontSize={12} /><YAxis width={56} tickLine={false} axisLine={false} tickFormatter={(v) => `R$${v}`} fontSize={12} />
                  <Tooltip formatter={(v) => [brl(Number(v)), 'Faturamento']} /><Bar dataKey="faturamento" fill={COBRE} radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></div>)}</div>
            <div className="card"><h2 className="mb">Serviços mais realizados</h2>{rank.carregando ? <Spinner /> : <Ranking itens={rank.dados ?? []} />}</div>
          </div>
        </>)}
    </>
  )
}

// ---------- Usuários / Clientes ----------
interface FormUsuario { nome: string; email: string; telefone: string; senha: string; tipo: TipoUsuario; ativo: boolean }
export function AdminUsuarios({ tipoFixo }: { tipoFixo?: TipoUsuario }) {
  const toast = useToast()
  const [q, setQ] = useState(''); const [busca, setBusca] = useState(''); const [tipo, setTipo] = useState(''); const [pagina, setPagina] = useState(0)
  useEffect(() => { const t = setTimeout(() => { setBusca(q); setPagina(0) }, 300); return () => clearTimeout(t) }, [q])
  const tipoEf = tipoFixo ?? tipo
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Pagina<Usuario>>(`/admin/usuarios${qs({ tipo: tipoEf, q: busca, pagina, tamanho: 10 })}`), [tipoEf, busca, pagina])
  const [edit, setEdit] = useState<Usuario | 'novo' | null>(null); const [del, setDel] = useState<Usuario | null>(null)
  const nome = tipoFixo === 'CLIENTE' ? 'cliente' : 'usuário'
  return (
    <>
      <PageHead titulo={tipoFixo === 'CLIENTE' ? 'Clientes' : 'Usuários'} sub={tipoFixo === 'CLIENTE' ? 'Cadastro e manutenção de clientes.' : 'Controle de acesso: clientes, barbeiros e administradores.'}>
        <button className="btn btn-primary" onClick={() => setEdit('novo')}><Plus size={18} /> Novo {nome}</button></PageHead>
      <div className="toolbar">
        <div className="search"><Search size={16} /><input className="input" aria-label="Buscar" placeholder="Buscar por nome ou e-mail" value={q} onChange={(e) => setQ(e.target.value)} /></div>
        {!tipoFixo && <select className="input" aria-label="Tipo" value={tipo} onChange={(e) => { setPagina(0); setTipo(e.target.value) }}><option value="">Todos os tipos</option>{(Object.keys(TIPO_LABEL) as TipoUsuario[]).map((t) => <option key={t} value={t}>{TIPO_LABEL[t]}</option>)}</select>}
      </div>
      {erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{carregando && !dados ? <Spinner /> : !dados?.conteudo.length ? <Vazio titulo={`Nenhum ${nome} encontrado`} texto={busca ? 'Tente outra busca.' : `Cadastre o primeiro ${nome}.`} /> : (
          <>
            <div className="table-wrap"><table><thead><tr><th>Nome</th><th>E-mail</th><th>Telefone</th>{!tipoFixo && <th>Tipo</th>}<th>Situação</th><th /></tr></thead><tbody>{dados.conteudo.map((u) => (
              <tr key={u.id}><td><strong>{u.nome}</strong></td><td>{u.email}</td><td>{u.telefone ?? '—'}</td>{!tipoFixo && <td>{TIPO_LABEL[u.tipo]}</td>}
                <td><span className={`badge ${u.ativo ? 'b-ok' : 'b-off'}`}>{u.ativo ? 'Ativo' : 'Inativo'}</span></td>
                <td><div className="actions">{u.tipo !== 'BARBEIRO' ? <>
                  <button className="icon-btn" style={{ color: 'var(--text)' }} aria-label={`Editar ${u.nome}`} onClick={() => setEdit(u)}><Pencil size={17} /></button>
                  <button className="icon-btn" style={{ color: 'var(--bad)' }} aria-label={`Excluir ${u.nome}`} onClick={() => setDel(u)}><Trash2 size={17} /></button></> : <Link to="/admin/barbeiros" className="btn btn-ghost btn-sm">Em Barbeiros</Link>}</div></td></tr>))}</tbody></table></div>
            <Paginacao pagina={dados.pagina} totalPaginas={dados.totalPaginas} total={dados.total} onChange={setPagina} />
          </>)}</div>
      )}
      {edit && <FormUsuarioModal usuario={edit === 'novo' ? null : edit} tipoFixo={tipoFixo} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); recarregar() }} />}
      {del && <Confirmar perigo titulo="Excluir usuário" rotulo="Excluir" texto={`Excluir ${del.nome}? Usuários com histórico não podem ser excluídos, apenas desativados.`}
        onConfirm={async () => { try { await api.del(`/admin/usuarios/${del.id}`); toast.ok('Registro excluído.'); recarregar() } catch (e) { toast.erro(errosDe(e).msg) } finally { setDel(null) } }} onClose={() => setDel(null)} />}
    </>
  )
}

function FormUsuarioModal({ usuario, tipoFixo, onClose, onSaved }: { usuario: Usuario | null; tipoFixo?: TipoUsuario; onClose: () => void; onSaved: () => void }) {
  const toast = useToast()
  const [f, setF] = useState<FormUsuario>({ nome: usuario?.nome ?? '', email: usuario?.email ?? '', telefone: usuario?.telefone ?? '', senha: '', tipo: usuario?.tipo ?? tipoFixo ?? 'CLIENTE', ativo: usuario?.ativo ?? true })
  const [erro, setErro] = useState(''); const [campos, setCampos] = useState<Record<string, string>>({}); const [busy, setBusy] = useState(false)
  const set = (k: keyof FormUsuario) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setF({ ...f, [k]: e.target.value })
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErro(''); setCampos({})
    try {
      const corpo = { ...f, senha: f.senha || null }
      if (usuario) await api.put(`/admin/usuarios/${usuario.id}`, corpo); else await api.post('/admin/usuarios', corpo)
      toast.ok('Registro salvo.'); onSaved()
    } catch (x) { const r = errosDe(x); setErro(r.msg); setCampos(r.campos) } finally { setBusy(false) }
  }
  return (
    <Modal titulo={usuario ? 'Editar' : 'Novo registro'} onClose={onClose}>
      <form onSubmit={salvar} noValidate>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <Input label="Nome" value={f.nome} onChange={set('nome')} erro={campos.nome} required />
        <Input label="E-mail" type="email" value={f.email} onChange={set('email')} erro={campos.email} required />
        <Input label="Telefone" value={f.telefone} onChange={set('telefone')} erro={campos.telefone} />
        {!tipoFixo && <Select label="Tipo" value={f.tipo} onChange={set('tipo')}><option value="CLIENTE">Cliente</option><option value="ADMIN">Administrador</option></Select>}
        <Input label={usuario ? 'Nova senha (opcional)' : 'Senha'} type="password" value={f.senha} onChange={set('senha')} erro={campos.senha} dica="Mínimo de 8 caracteres." autoComplete="new-password" required={!usuario} />
        <div className="field"><label className="row" style={{ gap: 8 }}><input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} /> Cadastro ativo</label></div>
        <div className="modal-foot"><button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button><Botao className="btn btn-primary" carregando={busy}><Save size={16} /> Salvar</Botao></div>
      </form>
    </Modal>
  )
}

// ---------- Barbeiros ----------
export function AdminBarbeiros() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Barbeiro[]>('/admin/barbeiros'), [])
  const [edit, setEdit] = useState<Barbeiro | 'novo' | null>(null); const [disp, setDisp] = useState<Barbeiro | null>(null); const [del, setDel] = useState<Barbeiro | null>(null)
  return (
    <>
      <PageHead titulo="Barbeiros" sub="Profissionais, especialidades e disponibilidade."><button className="btn btn-primary" onClick={() => setEdit('novo')}><Plus size={18} /> Novo barbeiro</button></PageHead>
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{!dados?.length ? <Vazio titulo="Nenhum barbeiro cadastrado" texto="Cadastre os profissionais para liberar os agendamentos." /> : (
          <div className="table-wrap"><table><thead><tr><th>Nome</th><th>Especialidade</th><th>Contato</th><th>Avaliação</th><th>Situação</th><th /></tr></thead><tbody>{dados.map((b) => (
            <tr key={b.id}><td><strong>{b.nome}</strong></td><td>{b.especialidade}</td><td>{b.email}<div className="muted">{b.telefone}</div></td><td>{b.mediaAvaliacao ? `${b.mediaAvaliacao.toFixed(1)} / 5` : '—'}</td>
              <td><span className={`badge ${b.ativo ? 'b-ok' : 'b-off'}`}>{b.ativo ? 'Ativo' : 'Inativo'}</span></td>
              <td><div className="actions"><button className="btn btn-ghost btn-sm" onClick={() => setDisp(b)}><CalendarDays size={14} /> Disponibilidade</button>
                <button className="icon-btn" style={{ color: 'var(--text)' }} aria-label={`Editar ${b.nome}`} onClick={() => setEdit(b)}><Pencil size={17} /></button>
                <button className="icon-btn" style={{ color: 'var(--bad)' }} aria-label={`Excluir ${b.nome}`} onClick={() => setDel(b)}><Trash2 size={17} /></button></div></td></tr>))}</tbody></table></div>)}</div>
      )}
      {edit && <FormBarbeiroModal barbeiro={edit === 'novo' ? null : edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); recarregar() }} />}
      {disp && <Modal titulo={`Disponibilidade — ${disp.nome}`} largo onClose={() => setDisp(null)}><DisponibilidadeEditor base={`/admin/barbeiros/${disp.id}`} onSaved={() => setDisp(null)} /></Modal>}
      {del && <Confirmar perigo titulo="Excluir barbeiro" rotulo="Excluir" texto={`Excluir ${del.nome}? Barbeiros com histórico de atendimentos não podem ser excluídos, apenas desativados.`}
        onConfirm={async () => { try { await api.del(`/admin/barbeiros/${del.id}`); toast.ok('Barbeiro excluído.'); recarregar() } catch (e) { toast.erro(errosDe(e).msg) } finally { setDel(null) } }} onClose={() => setDel(null)} />}
    </>
  )
}

function FormBarbeiroModal({ barbeiro, onClose, onSaved }: { barbeiro: Barbeiro | null; onClose: () => void; onSaved: () => void }) {
  const toast = useToast()
  const [f, setF] = useState({ nome: barbeiro?.nome ?? '', email: barbeiro?.email ?? '', telefone: barbeiro?.telefone ?? '', senha: '', especialidade: barbeiro?.especialidade ?? '', ativo: barbeiro?.ativo ?? true })
  const [erro, setErro] = useState(''); const [campos, setCampos] = useState<Record<string, string>>({}); const [busy, setBusy] = useState(false)
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value })
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErro(''); setCampos({})
    try { const c = { ...f, senha: f.senha || null }; if (barbeiro) await api.put(`/admin/barbeiros/${barbeiro.id}`, c); else await api.post('/admin/barbeiros', c); toast.ok('Barbeiro salvo.'); onSaved() }
    catch (x) { const r = errosDe(x); setErro(r.msg); setCampos(r.campos) } finally { setBusy(false) }
  }
  return (
    <Modal titulo={barbeiro ? 'Editar barbeiro' : 'Novo barbeiro'} onClose={onClose}>
      <form onSubmit={salvar} noValidate>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <Input label="Nome" value={f.nome} onChange={set('nome')} erro={campos.nome} required />
        <Input label="Especialidade" value={f.especialidade} onChange={set('especialidade')} erro={campos.especialidade} required />
        <Input label="E-mail (login)" type="email" value={f.email} onChange={set('email')} erro={campos.email} required />
        <Input label="Telefone" value={f.telefone} onChange={set('telefone')} erro={campos.telefone} />
        <Input label={barbeiro ? 'Nova senha (opcional)' : 'Senha de acesso'} type="password" value={f.senha} onChange={set('senha')} erro={campos.senha} dica="Mínimo de 8 caracteres." autoComplete="new-password" required={!barbeiro} />
        <div className="field"><label className="row" style={{ gap: 8 }}><input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} /> Disponível para agendamentos (ativo)</label></div>
        <div className="modal-foot"><button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button><Botao className="btn btn-primary" carregando={busy}><Save size={16} /> Salvar</Botao></div>
      </form>
    </Modal>
  )
}

// ---------- Serviços ----------
export function AdminServicos() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Servico[]>('/admin/servicos'), [])
  const [edit, setEdit] = useState<Servico | 'novo' | null>(null); const [del, setDel] = useState<Servico | null>(null)
  return (
    <>
      <PageHead titulo="Serviços" sub="Catálogo oferecido pela barbearia."><button className="btn btn-primary" onClick={() => setEdit('novo')}><Plus size={18} /> Novo serviço</button></PageHead>
      {carregando ? <Spinner /> : erro ? <ErroBox msg={erro} onRetry={recarregar} /> : (
        <div className="card card-flush">{!dados?.length ? <Vazio titulo="Nenhum serviço cadastrado" texto="Cadastre corte, barba e demais serviços com valor e duração." /> : (
          <div className="table-wrap"><table><thead><tr><th>Serviço</th><th>Descrição</th><th>Duração</th><th>Preço</th><th>Situação</th><th /></tr></thead><tbody>{dados.map((s) => (
            <tr key={s.id}><td><strong>{s.nome}</strong></td><td className="muted">{s.descricao ?? '—'}</td><td>{s.duracao} min</td><td>{brl(s.preco)}</td><td><span className={`badge ${s.ativo ? 'b-ok' : 'b-off'}`}>{s.ativo ? 'Ativo' : 'Inativo'}</span></td>
              <td><div className="actions"><button className="icon-btn" style={{ color: 'var(--text)' }} aria-label={`Editar ${s.nome}`} onClick={() => setEdit(s)}><Pencil size={17} /></button>
                <button className="icon-btn" style={{ color: 'var(--bad)' }} aria-label={`Excluir ${s.nome}`} onClick={() => setDel(s)}><Trash2 size={17} /></button></div></td></tr>))}</tbody></table></div>)}</div>
      )}
      {edit && <FormServicoModal servico={edit === 'novo' ? null : edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); recarregar() }} />}
      {del && <Confirmar perigo titulo="Excluir serviço" rotulo="Excluir" texto={`Excluir ${del.nome}? Serviços já agendados não podem ser excluídos, apenas desativados.`}
        onConfirm={async () => { try { await api.del(`/admin/servicos/${del.id}`); toast.ok('Serviço excluído.'); recarregar() } catch (e) { toast.erro(errosDe(e).msg) } finally { setDel(null) } }} onClose={() => setDel(null)} />}
    </>
  )
}

function FormServicoModal({ servico, onClose, onSaved }: { servico: Servico | null; onClose: () => void; onSaved: () => void }) {
  const toast = useToast()
  const [f, setF] = useState({ nome: servico?.nome ?? '', descricao: servico?.descricao ?? '', preco: servico ? String(servico.preco) : '', duracao: servico ? String(servico.duracao) : '30', ativo: servico?.ativo ?? true })
  const [erro, setErro] = useState(''); const [campos, setCampos] = useState<Record<string, string>>({}); const [busy, setBusy] = useState(false)
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => setF({ ...f, [k]: e.target.value })
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErro(''); setCampos({})
    try {
      const c = { nome: f.nome, descricao: f.descricao || null, preco: Number(f.preco.replace(',', '.')), duracao: Number(f.duracao), ativo: f.ativo }
      if (servico) await api.put(`/admin/servicos/${servico.id}`, c); else await api.post('/admin/servicos', c)
      toast.ok('Serviço salvo.'); onSaved()
    } catch (x) { const r = errosDe(x); setErro(r.msg); setCampos(r.campos) } finally { setBusy(false) }
  }
  return (
    <Modal titulo={servico ? 'Editar serviço' : 'Novo serviço'} onClose={onClose}>
      <form onSubmit={salvar} noValidate>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <Input label="Nome" value={f.nome} onChange={set('nome')} erro={campos.nome} required />
        <Textarea label="Descrição" value={f.descricao} onChange={set('descricao')} maxLength={400} />
        <div className="grid g2"><Input label="Preço (R$)" inputMode="decimal" value={f.preco} onChange={set('preco')} erro={campos.preco} required /><Input label="Duração (min)" type="number" min={5} step={5} value={f.duracao} onChange={set('duracao')} erro={campos.duracao} required /></div>
        <div className="field"><label className="row" style={{ gap: 8 }}><input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} /> Disponível para agendamento</label></div>
        <div className="modal-foot"><button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button><Botao className="btn btn-primary" carregando={busy}><Save size={16} /> Salvar</Botao></div>
      </form>
    </Modal>
  )
}

// ---------- Horário de funcionamento ----------
export function AdminHorarios() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<HorarioFuncionamento[]>('/admin/funcionamento'), [])
  const [dias, setDias] = useState<HorarioFuncionamento[] | null>(null); const [busy, setBusy] = useState(false); const [err, setErr] = useState('')
  useEffect(() => { if (dados) setDias(dados.map((d) => ({ ...d, abertura: hhmm(d.abertura), fechamento: hhmm(d.fechamento) }))) }, [dados])
  if (carregando || !dias) return erro ? <ErroBox msg={erro} onRetry={recarregar} /> : <Spinner />
  const upd = (i: number, p: Partial<HorarioFuncionamento>) => setDias(dias.map((d, k) => (k === i ? { ...d, ...p } : d)))
  const salvar = async () => {
    setBusy(true); setErr('')
    try { await api.put('/admin/funcionamento', { dias }); toast.ok('Horários de funcionamento salvos.'); recarregar() } catch (e) { setErr(errosDe(e).msg) } finally { setBusy(false) }
  }
  return (
    <>
      <PageHead titulo="Horários de funcionamento" sub="Regras gerais de atendimento. A disponibilidade de cada barbeiro deve respeitar estes horários." />
      <div className="card">
        {err && <div className="alert alert-erro" role="alert">{err}</div>}
        {dias.map((d, i) => (
          <div key={d.diaSemana} className="week-row">
            <strong>{DIAS[d.diaSemana]}</strong>
            <label className="row" style={{ gap: 8 }}><input type="checkbox" checked={d.aberto} onChange={(e) => upd(i, { aberto: e.target.checked })} /> Aberto</label>
            <div className="row" style={{ gap: 8, opacity: d.aberto ? 1 : 0.4 }}>
              <input className="input" style={{ width: 120 }} type="time" aria-label={`Abertura ${DIAS[d.diaSemana]}`} disabled={!d.aberto} value={d.abertura} onChange={(e) => upd(i, { abertura: e.target.value })} /><span>até</span>
              <input className="input" style={{ width: 120 }} type="time" aria-label={`Fechamento ${DIAS[d.diaSemana]}`} disabled={!d.aberto} value={d.fechamento} onChange={(e) => upd(i, { fechamento: e.target.value })} /></div>
          </div>))}
        <div className="modal-foot"><Botao className="btn btn-primary" carregando={busy} onClick={salvar}><Save size={16} /> Salvar horários</Botao></div>
      </div>
    </>
  )
}
