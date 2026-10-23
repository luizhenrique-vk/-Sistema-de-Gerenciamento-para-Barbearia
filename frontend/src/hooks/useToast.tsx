import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { CheckCircle2, AlertCircle } from 'lucide-react'

interface Toast { id: number; tipo: 'ok' | 'erro'; texto: string }
const Ctx = createContext<{ ok: (t: string) => void; erro: (t: string) => void } | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [itens, setItens] = useState<Toast[]>([])
  const add = useCallback((tipo: Toast['tipo'], texto: string) => {
    const id = Date.now() + Math.random()
    setItens((l) => [...l, { id, tipo, texto }])
    setTimeout(() => setItens((l) => l.filter((x) => x.id !== id)), 5000)
  }, [])
  return (
    <Ctx.Provider value={{ ok: (t) => add('ok', t), erro: (t) => add('erro', t) }}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {itens.map((t) => (
          <div key={t.id} className={`toast toast-${t.tipo}`}>
            {t.tipo === 'ok' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}<span>{t.texto}</span>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  )
}
export const useToast = () => {
  const c = useContext(Ctx)
  if (!c) throw new Error('useToast fora do ToastProvider')
  return c
}
