import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../services/api'

/** Carrega dados de uma função assíncrona expondo estados de carregamento e erro, com recarga manual. */
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[] = []) {
  const [dados, setDados] = useState<T | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const executar = useCallback(fn, deps)

  const recarregar = useCallback(() => {
    setCarregando(true); setErro(null)
    return executar().then(setDados).catch((e) => setErro(e instanceof ApiError ? e.message : 'Falha ao carregar.')).finally(() => setCarregando(false))
  }, [executar])

  useEffect(() => { recarregar() }, [recarregar])
  return { dados, erro, carregando, recarregar }
}
