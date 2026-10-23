import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, LogIn, UserPlus } from 'lucide-react'
import { homeDoPerfil, useAuth } from '../hooks/useAuth'
import { Botao, Input, errosDe } from '../components/ui'
import { PoleMark } from '../components/Logo'

function Arte() {
  return (
    <div className="auth-art">
      <div className="row"><PoleMark size={44} /><div><div className="brand-name" style={{ fontSize: '1.3rem' }}>Barbearia</div><div className="brand-sub">Manager</div></div></div>
      <div><h1>Sua agenda, seu caixa e seus clientes num só lugar.</h1><p className="mt">Agendamento online, controle de pagamentos e relatórios gerenciais para barbearias.</p></div>
      <small style={{ color: '#8a8271' }}>Sistema de Gestão de Barbearias</small>
    </div>
  )
}

export function Login() {
  const { usuario, entrar } = useAuth()
  const nav = useNavigate()
  const loc = useLocation() as { state?: { de?: string } }
  const [email, setEmail] = useState(''); const [senha, setSenha] = useState(''); const [ver, setVer] = useState(false)
  const [erro, setErro] = useState(''); const [busy, setBusy] = useState(false)
  if (usuario) return <Navigate to={homeDoPerfil(usuario.tipo)} replace />
  const enviar = async (e: FormEvent) => {
    e.preventDefault(); setErro(''); setBusy(true)
    try { const u = await entrar(email, senha); nav(loc.state?.de ?? homeDoPerfil(u.tipo), { replace: true }) }
    catch (x) { setErro(errosDe(x).msg) } finally { setBusy(false) }
  }
  return (
    <div className="auth"><Arte />
      <div className="auth-form"><form className="auth-box" onSubmit={enviar} noValidate>
        <h2>Entrar</h2><p className="muted mb">Acesse com seu e-mail e senha.</p>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <Input label="E-mail" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="voce@email.com" />
        <div style={{ position: 'relative' }}>
          <Input label="Senha" type={ver ? 'text' : 'password'} autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
          <button type="button" className="icon-btn" style={{ position: 'absolute', right: 6, top: 28, color: 'var(--muted)' }} aria-label={ver ? 'Ocultar senha' : 'Mostrar senha'} onClick={() => setVer(!ver)}>{ver ? <EyeOff size={18} /> : <Eye size={18} />}</button>
        </div>
        <Botao className="btn btn-primary btn-block" carregando={busy} disabled={!email || !senha}><LogIn size={18} /> Entrar</Botao>
        <p className="muted mt" style={{ textAlign: 'center' }}>Primeira vez aqui? <Link to="/cadastro">Criar conta de cliente</Link></p>
      </form></div>
    </div>
  )
}

export function Cadastro() {
  const { usuario, cadastrar } = useAuth()
  const nav = useNavigate()
  const [f, setF] = useState({ nome: '', email: '', telefone: '', senha: '' })
  const [erro, setErro] = useState(''); const [campos, setCampos] = useState<Record<string, string>>({}); const [busy, setBusy] = useState(false)
  if (usuario) return <Navigate to={homeDoPerfil(usuario.tipo)} replace />
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value })
  const enviar = async (e: FormEvent) => {
    e.preventDefault(); setErro(''); setCampos({}); setBusy(true)
    try { const u = await cadastrar(f); nav(homeDoPerfil(u.tipo), { replace: true }) }
    catch (x) { const r = errosDe(x); setErro(r.msg); setCampos(r.campos) } finally { setBusy(false) }
  }
  return (
    <div className="auth"><Arte />
      <div className="auth-form"><form className="auth-box" onSubmit={enviar} noValidate>
        <h2>Criar conta</h2><p className="muted mb">Cadastro de cliente para agendar seus atendimentos.</p>
        {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
        <Input label="Nome completo" value={f.nome} onChange={set('nome')} erro={campos.nome} autoComplete="name" required />
        <Input label="E-mail" type="email" value={f.email} onChange={set('email')} erro={campos.email} autoComplete="email" required />
        <Input label="Telefone" type="tel" value={f.telefone} onChange={set('telefone')} erro={campos.telefone} autoComplete="tel" placeholder="(67) 99999-9999" />
        <Input label="Senha" type="password" value={f.senha} onChange={set('senha')} erro={campos.senha} dica="Mínimo de 8 caracteres." autoComplete="new-password" required />
        <Botao className="btn btn-primary btn-block" carregando={busy} disabled={!f.nome || !f.email || f.senha.length < 8}><UserPlus size={18} /> Criar conta</Botao>
        <p className="muted mt" style={{ textAlign: 'center' }}>Já tem conta? <Link to="/login">Entrar</Link></p>
      </form></div>
    </div>
  )
}
