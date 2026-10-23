import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { Bell, CalendarCheck, CalendarDays, Clock, Gauge, History, LogOut, Menu, Scissors, Settings, Star, UserCog, Users, Wallet, BarChart3, UserRound, Home, Contact, CheckCheck } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { api } from '../services/api'
import type { Notificacao, TipoUsuario } from '../types'
import { Brand } from './Logo'
import { dataHora } from '../services/format'
import { TIPO_LABEL } from '../services/format'

type Item = { to: string; label: string; icon: React.ReactNode; end?: boolean }
const ic = 18
const MENUS: Record<TipoUsuario, Item[]> = {
  CLIENTE: [
    { to: '/cliente', label: 'Início', icon: <Home size={ic} />, end: true },
    { to: '/cliente/agendar', label: 'Agendar', icon: <CalendarCheck size={ic} /> },
    { to: '/cliente/agendamentos', label: 'Meus agendamentos', icon: <CalendarDays size={ic} /> },
    { to: '/cliente/historico', label: 'Histórico', icon: <History size={ic} /> },
    { to: '/cliente/servicos', label: 'Serviços', icon: <Scissors size={ic} /> },
    { to: '/cliente/profissionais', label: 'Profissionais', icon: <Users size={ic} /> },
    { to: '/cliente/perfil', label: 'Meu perfil', icon: <UserRound size={ic} /> },
  ],
  BARBEIRO: [
    { to: '/barbeiro', label: 'Painel', icon: <Gauge size={ic} />, end: true },
    { to: '/barbeiro/agenda', label: 'Agenda', icon: <CalendarDays size={ic} /> },
    { to: '/barbeiro/disponibilidade', label: 'Disponibilidade', icon: <Clock size={ic} /> },
    { to: '/barbeiro/perfil', label: 'Perfil profissional', icon: <UserRound size={ic} /> },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard', icon: <Gauge size={ic} />, end: true },
    { to: '/admin/agendamentos', label: 'Agendamentos', icon: <CalendarDays size={ic} /> },
    { to: '/admin/pagamentos', label: 'Pagamentos', icon: <Wallet size={ic} /> },
    { to: '/admin/relatorios', label: 'Relatórios', icon: <BarChart3 size={ic} /> },
    { to: '/admin/clientes', label: 'Clientes', icon: <Contact size={ic} /> },
    { to: '/admin/barbeiros', label: 'Barbeiros', icon: <Star size={ic} /> },
    { to: '/admin/servicos', label: 'Serviços', icon: <Scissors size={ic} /> },
    { to: '/admin/horarios', label: 'Horários', icon: <Settings size={ic} /> },
    { to: '/admin/usuarios', label: 'Usuários', icon: <UserCog size={ic} /> },
    { to: '/admin/perfil', label: 'Meu perfil', icon: <UserRound size={ic} /> },
  ],
}

function Notificacoes() {
  const [aberto, setAberto] = useState(false)
  const [dados, setDados] = useState<{ naoLidas: number; itens: Notificacao[] }>({ naoLidas: 0, itens: [] })
  const carregar = () => api.get<typeof dados>('/notificacoes').then(setDados).catch(() => {})
  useEffect(() => { carregar(); const t = setInterval(carregar, 60000); return () => clearInterval(t) }, [])
  const marcar = async () => { await api.post('/notificacoes/lidas'); carregar() }
  return (
    <div style={{ position: 'relative' }}>
      <button className="btn btn-dark btn-block" aria-expanded={aberto} onClick={() => { setAberto(!aberto); if (!aberto) carregar() }}>
        <Bell size={16} /> Notificações{dados.naoLidas > 0 && <span className="badge b-warn">{dados.naoLidas}</span>}
      </button>
      {aberto && (
        <div className="notif-panel" role="region" aria-label="Notificações">
          <div className="row" style={{ padding: '10px 14px', justifyContent: 'space-between', borderBottom: '1px solid var(--line)' }}>
            <strong>Notificações</strong>
            {dados.naoLidas > 0 && <button className="btn btn-ghost btn-sm" onClick={marcar}><CheckCheck size={14} /> Marcar lidas</button>}
          </div>
          <div style={{ maxHeight: 320, overflowY: 'auto' }}>
            {dados.itens.length === 0 && <div className="item muted">Nenhuma notificação ainda.</div>}
            {dados.itens.map((n) => <div key={n.id} className={`item ${n.lida ? '' : 'nova'}`}>{n.mensagem}<div className="muted" style={{ fontSize: '.75rem' }}>{dataHora(n.criadaEm)}</div></div>)}
          </div>
        </div>
      )}
    </div>
  )
}

export default function Layout() {
  const { usuario, sair } = useAuth()
  const [aberto, setAberto] = useState(false)
  const loc = useLocation()
  useEffect(() => setAberto(false), [loc.pathname])
  if (!usuario) return null
  return (
    <div className="shell">
      <div className={`scrim ${aberto ? 'open' : ''}`} onClick={() => setAberto(false)} />
      <aside className={`sidebar ${aberto ? 'open' : ''}`} aria-label="Menu principal">
        <Brand />
        <nav className="nav">
          {MENUS[usuario.tipo].map((i) => <NavLink key={i.to} to={i.to} end={i.end}>{i.icon}{i.label}</NavLink>)}
        </nav>
        <div className="side-foot">
          <div className="who"><strong>{usuario.nome}</strong><span>{TIPO_LABEL[usuario.tipo]}</span></div>
          <Notificacoes />
          <button className="btn btn-dark btn-block" onClick={sair}><LogOut size={16} /> Sair</button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button className="icon-btn" aria-label="Abrir menu" onClick={() => setAberto(true)}><Menu size={22} /></button>
          <strong style={{ fontFamily: 'var(--font-display)' }}>Barbearia Manager</strong><span style={{ width: 38 }} />
        </header>
        <main className="page" id="conteudo"><Outlet /></main>
      </div>
    </div>
  )
}
