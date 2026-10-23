export const brl = (v: number | null | undefined) => (v ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
export const hhmm = (t: string) => t.slice(0, 5)
const parse = (s: string) => new Date(s)
export const dataCurta = (s: string) => parse(s).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
export const horaCurta = (s: string) => parse(s).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
export const dataHora = (s: string) => `${dataCurta(s)} · ${horaCurta(s)}`
export const isoData = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
export const hoje = () => isoData(new Date())
export const DIAS = ['', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado', 'Domingo']
export const STATUS_LABEL = { AGENDADO: 'Agendado', CANCELADO: 'Cancelado', FINALIZADO: 'Finalizado' } as const
export const PAGAMENTO_LABEL = { DINHEIRO: 'Dinheiro', PIX: 'Pix', CARTAO_CREDITO: 'Cartão de crédito', CARTAO_DEBITO: 'Cartão de débito' } as const
export const TIPO_LABEL = { CLIENTE: 'Cliente', BARBEIRO: 'Barbeiro', ADMIN: 'Administrador' } as const
