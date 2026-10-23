import { useEffect, useState, type FormEvent } from 'react'
import { Save } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../services/api'
import { Botao, ErroBox, Input, PageHead, Spinner, errosDe } from '../components/ui'
import { useAsync } from '../hooks/useAsync'
import type { Barbeiro, Usuario } from '../types'

export default function Perfil() {
  const { usuario, atualizar } = useAuth()
  const toast = useToast()
  const [f, setF] = useState({ nome: '', email: '', telefone: '', senhaAtual: '', novaSenha: '' })
  const [campos, setCampos] = useState<Record<string, string>>({}); const [erro, setErro] = useState(''); const [busy, setBusy] = useState(false)
  useEffect(() => { if (usuario) setF((o) => ({ ...o, nome: usuario.nome, email: usuario.email, telefone: usuario.telefone ?? '' })) }, [usuario])
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value })
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setErro(''); setCampos({}); setBusy(true)
    try {
      const u = await api.put<Usuario>('/me', { ...f, novaSenha: f.novaSenha || null, senhaAtual: f.senhaAtual || null })
      atualizar(u); setF((o) => ({ ...o, senhaAtual: '', novaSenha: '' })); toast.ok('Perfil atualizado com sucesso.')
    } catch (x) { const r = errosDe(x); setErro(r.msg); setCampos(r.campos) } finally { setBusy(false) }
  }
  return (
    <>
      <PageHead titulo="Meu perfil" sub="Atualize seus dados pessoais e sua senha." />
      <div className="grid g2" style={{ alignItems: 'start' }}>
        <form className="card" onSubmit={salvar} noValidate>
          <h2 className="mb">Dados pessoais</h2>
          {erro && <div className="alert alert-erro" role="alert">{erro}</div>}
          <Input label="Nome" value={f.nome} onChange={set('nome')} erro={campos.nome} required />
          <Input label="E-mail" type="email" value={f.email} onChange={set('email')} erro={campos.email} required />
          <Input label="Telefone" type="tel" value={f.telefone} onChange={set('telefone')} erro={campos.telefone} />
          <h3 className="mb mt">Alterar senha</h3>
          <Input label="Senha atual" type="password" value={f.senhaAtual} onChange={set('senhaAtual')} autoComplete="current-password" dica="Necessária apenas para trocar a senha." />
          <Input label="Nova senha" type="password" value={f.novaSenha} onChange={set('novaSenha')} erro={campos.novaSenha} autoComplete="new-password" />
          <Botao className="btn btn-primary" carregando={busy}><Save size={16} /> Salvar alterações</Botao>
        </form>
        {usuario?.tipo === 'BARBEIRO' && <PerfilProfissional />}
      </div>
    </>
  )
}

function PerfilProfissional() {
  const toast = useToast()
  const { dados, carregando, erro, recarregar } = useAsync(() => api.get<Barbeiro>('/barbeiro/perfil'), [])
  const [esp, setEsp] = useState(''); const [busy, setBusy] = useState(false); const [err, setErr] = useState('')
  useEffect(() => { if (dados) setEsp(dados.especialidade) }, [dados])
  if (carregando) return <div className="card"><Spinner /></div>
  if (erro) return <ErroBox msg={erro} onRetry={recarregar} />
  const salvar = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setErr('')
    try { await api.put('/barbeiro/perfil', { especialidade: esp }); toast.ok('Perfil profissional atualizado.'); recarregar() } catch (x) { setErr(errosDe(x).msg) } finally { setBusy(false) }
  }
  return (
    <form className="card" onSubmit={salvar}>
      <h2 className="mb">Perfil profissional</h2>
      {err && <div className="alert alert-erro" role="alert">{err}</div>}
      <Input label="Especialidade" value={esp} onChange={(e) => setEsp(e.target.value)} required />
      <p className="muted">Avaliação média dos clientes: <strong>{dados?.mediaAvaliacao ? dados.mediaAvaliacao.toFixed(1) + ' / 5' : 'ainda sem avaliações'}</strong></p>
      <Botao className="btn btn-primary" carregando={busy} disabled={!esp.trim()}><Save size={16} /> Salvar</Botao>
    </form>
  )
}
