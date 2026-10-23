import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, tokenStore } from '../services/api'
import type { AuthResponse, Usuario } from '../types'

interface AuthCtx {
  usuario: Usuario | null
  carregando: boolean
  entrar: (email: string, senha: string) => Promise<Usuario>
  cadastrar: (d: { nome: string; email: string; telefone: string; senha: string }) => Promise<Usuario>
  sair: () => void
  atualizar: (u: Usuario) => void
}
const Ctx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(!!tokenStore.get())

  const sair = useCallback(() => { tokenStore.clear(); setUsuario(null) }, [])

  useEffect(() => {
    if (!tokenStore.get()) return
    api.get<Usuario>('/me').then(setUsuario).catch(() => tokenStore.clear()).finally(() => setCarregando(false))
  }, [])

  useEffect(() => {
    window.addEventListener('bm:sessao-expirada', sair)
    return () => window.removeEventListener('bm:sessao-expirada', sair)
  }, [sair])

  const valor = useMemo<AuthCtx>(() => ({
    usuario, carregando, sair, atualizar: setUsuario,
    entrar: async (email, senha) => {
      const r = await api.post<AuthResponse>('/auth/login', { email, senha })
      tokenStore.set(r.token); setUsuario(r.usuario); return r.usuario
    },
    cadastrar: async (d) => {
      const r = await api.post<AuthResponse>('/auth/register', d)
      tokenStore.set(r.token); setUsuario(r.usuario); return r.usuario
    },
  }), [usuario, carregando, sair])

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>
}

export const useAuth = () => {
  const c = useContext(Ctx)
  if (!c) throw new Error('useAuth fora do AuthProvider')
  return c
}
export const homeDoPerfil = (t: Usuario['tipo']) => (t === 'ADMIN' ? '/admin' : t === 'BARBEIRO' ? '/barbeiro' : '/cliente')
