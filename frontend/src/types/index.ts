export type TipoUsuario = 'CLIENTE' | 'BARBEIRO' | 'ADMIN'
export type StatusAgendamento = 'AGENDADO' | 'CANCELADO' | 'FINALIZADO'
export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO'

export interface Usuario { id: number; nome: string; email: string; telefone: string | null; tipo: TipoUsuario; ativo: boolean; barbeiroId: number | null }
export interface AuthResponse { token: string; usuario: Usuario }
export interface Barbeiro { id: number; usuarioId: number; nome: string; email: string; telefone: string | null; especialidade: string; ativo: boolean; mediaAvaliacao: number | null }
export interface Servico { id: number; nome: string; descricao: string | null; preco: number; duracao: number; ativo: boolean }
export interface Agendamento {
  id: number; clienteId: number; clienteNome: string; clienteTelefone: string | null
  barbeiroId: number; barbeiroNome: string; servicoId: number; servicoNome: string; duracao: number
  dataHora: string; dataHoraFim: string; status: StatusAgendamento; observacoes: string | null
  preco: number; pago: boolean; formaPagamento: FormaPagamento | null; dataPagamento: string | null; avaliado: boolean
}
export interface Pagina<T> { conteudo: T[]; pagina: number; tamanho: number; total: number; totalPaginas: number }
export interface Disponibilidade { diaSemana: number; horaInicio: string; horaFim: string }
export interface HorarioFuncionamento { diaSemana: number; aberto: boolean; abertura: string; fechamento: string }
export interface Notificacao { id: number; mensagem: string; lida: boolean; criadaEm: string }
export interface FinanceiroResponse {
  periodo: string; inicio: string; fim: string; faturamento: number; pendenteRecebimento: number
  atendimentos: number; cancelados: number; ticketMedio: number; serie: { rotulo: string; faturamento: number; atendimentos: number }[]
}
export interface ServicoRanking { servicoId: number; nome: string; quantidade: number; valor: number }
export interface ResumoPeriodo { total: number; agendados: number; finalizados: number; cancelados: number; faturamento: number }
export interface Dashboard { hoje: ResumoPeriodo; mes: ResumoPeriodo; mediaAvaliacao: number | null; topServicos: ServicoRanking[]; ultimosDias: { data: string; agendamentos: number }[] }
