import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { homeDoPerfil, useAuth } from './hooks/useAuth'
import type { TipoUsuario } from './types'
import Layout from './components/Layout'
import { Spinner } from './components/ui'
import { Cadastro, Login } from './pages/Auth'
import Perfil from './pages/Perfil'
import { Agendar, ClienteInicio, ClienteProfissionais, ClienteServicos, Historico, MeusAgendamentos } from './pages/Cliente'
import { BarbeiroAgenda, BarbeiroDisponibilidade, BarbeiroPainel } from './pages/Barbeiro'
import { AdminAgendamentos, AdminBarbeiros, AdminDashboard, AdminHorarios, AdminPagamentos, AdminRelatorios, AdminServicos, AdminUsuarios } from './pages/Admin'

/** Guarda de rota no cliente. A autorização real é feita no backend; isto apenas evita telas inacessíveis. */
function Protegida({ tipo }: { tipo: TipoUsuario }) {
  const { usuario, carregando } = useAuth()
  const loc = useLocation()
  if (carregando) return <Spinner />
  if (!usuario) return <Navigate to="/login" replace state={{ de: loc.pathname }} />
  if (usuario.tipo !== tipo) return <Navigate to={homeDoPerfil(usuario.tipo)} replace />
  return <Outlet />
}

function Raiz() {
  const { usuario, carregando } = useAuth()
  if (carregando) return <Spinner />
  return <Navigate to={usuario ? homeDoPerfil(usuario.tipo) : '/login'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Raiz />} />
      <Route path="/login" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
      <Route element={<Protegida tipo="CLIENTE" />}><Route path="/cliente" element={<Layout />}>
        <Route index element={<ClienteInicio />} /><Route path="agendar" element={<Agendar />} /><Route path="agendamentos" element={<MeusAgendamentos />} />
        <Route path="historico" element={<Historico />} /><Route path="servicos" element={<ClienteServicos />} /><Route path="profissionais" element={<ClienteProfissionais />} /><Route path="perfil" element={<Perfil />} />
      </Route></Route>
      <Route element={<Protegida tipo="BARBEIRO" />}><Route path="/barbeiro" element={<Layout />}>
        <Route index element={<BarbeiroPainel />} /><Route path="agenda" element={<BarbeiroAgenda />} /><Route path="disponibilidade" element={<BarbeiroDisponibilidade />} /><Route path="perfil" element={<Perfil />} />
      </Route></Route>
      <Route element={<Protegida tipo="ADMIN" />}><Route path="/admin" element={<Layout />}>
        <Route index element={<AdminDashboard />} /><Route path="agendamentos" element={<AdminAgendamentos />} /><Route path="pagamentos" element={<AdminPagamentos />} />
        <Route path="relatorios" element={<AdminRelatorios />} /><Route path="clientes" element={<AdminUsuarios tipoFixo="CLIENTE" />} /><Route path="usuarios" element={<AdminUsuarios />} />
        <Route path="barbeiros" element={<AdminBarbeiros />} /><Route path="servicos" element={<AdminServicos />} /><Route path="horarios" element={<AdminHorarios />} /><Route path="perfil" element={<Perfil />} />
      </Route></Route>
      <Route path="*" element={<Raiz />} />
    </Routes>
  )
}
