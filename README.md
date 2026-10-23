# Barbearia Manager

Sistema web de gestão de barbearias, derivado do TCC *"Desenvolvimento de um Sistema Web para Gestão de Barbearias Utilizando Java, Spring Boot e Banco de Dados Relacional"*.

**Stack:** Java 21 · Spring Boot 3.3 (Web, Data JPA, Security, Validation) · Flyway · MySQL 8 · JWT · React 18 + TypeScript + Vite · Recharts · Lucide.

## Estrutura
```
backend/   API REST (controllers, services, repositories, domain, dto, security, config, exception) + testes
  src/main/resources/db/migration/V1__schema_inicial.sql   modelo relacional com PK/FK/CHECK/UNIQUE
frontend/  SPA React (pages, components, hooks, services, types)
docker-compose.yml   MySQL local
.env.example         variáveis de ambiente
```

## Executar (desenvolvimento)
Requisitos: JDK 21, Maven 3.9+, Node 20+, Docker (ou MySQL 8 instalado).

```bash
cp .env.example .env        # preencha DB_PASSWORD, APP_JWT_SECRET (>=32 chars), APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD
set -a; source .env; set +a
docker compose up -d        # MySQL

cd backend && mvn spring-boot:run        # API em :8080 (Flyway cria as tabelas)
cd frontend && npm install && npm run dev   # SPA em :5173 (proxy /api -> :8080)
```
O primeiro administrador é criado **somente** se não existir nenhum e as variáveis `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` estiverem definidas. Não há credenciais fixas no código. Depois: Admin → cadastrar serviços, barbeiros, horário de funcionamento; o barbeiro define sua disponibilidade; clientes se cadastram e agendam.

## Testes
```bash
cd backend && mvn test      # testes de integração com H2 em modo MySQL (FluxoAgendamentoTest)
cd frontend && npm run build
```

## Mapeamento TCC → sistema
| Caso de uso / requisito | Onde |
|---|---|
| Autenticar usuário | `AuthController`, `JwtAuthFilter`, tela Login |
| Gerenciar perfil | `PUT /api/me`, página Perfil |
| Consultar serviços / profissionais | `GET /api/servicos`, `/api/barbeiros` |
| Selecionar horário / Agendar | `HorarioService.horariosLivres`, `AgendamentoService.criar`, página Agendar |
| Cancelar agendamento (+ regra) | `AgendamentoService.cancelar` (antecedência mínima 2 h para cliente) |
| Histórico / Avaliar | `/api/agendamentos/historico`, `/api/avaliacoes` |
| Visualizar agenda / Definir disponibilidade / Atualizar status | `BarbeiroAreaController`, `PATCH /api/agendamentos/{id}/status` |
| Administrar usuários, barbeiros, serviços, horários | `AdminController` |
| Controle de pagamentos | `PATCH /api/admin/agendamentos/{id}/pagamento` |
| Relatórios diário/semanal/mensal, serviços mais realizados | `RelatorioService` |
| Notificar cliente / lembretes | `Notificacao` (internas) + job `gerarLembretes` a cada 15 min |

## Decisões técnicas (não definidas no TCC)
- **Entidades adicionais:** `Disponibilidade`, `HorarioFuncionamento`, `Avaliacao`, `Notificacao`; campos de pagamento e `precoCobrado` em `Agendamento` (preserva o valor mesmo se o preço do serviço mudar). `Usuario` ganhou `telefone` e `ativo` (RF de cadastro de clientes).
- **Perfis:** o TCC cita "administrador e funcionário" nos RFs e "Barbeiro" nos casos de uso; adotou-se Cliente/Barbeiro/Admin (diagrama de classes).
- **Horários:** grade de 30 min; o horário deve caber na janela do barbeiro ∩ funcionamento. Sem sobreposição entre agendamentos ativos do barbeiro (bloqueio pessimista por barbeiro evita corrida) nem do mesmo cliente.
- **Cancelamento:** cliente com ≥ 2 h de antecedência; barbeiro (próprios) e admin sem restrição. Finalizar só após o horário do atendimento.
- **Faturamento** = atendimentos finalizados **com pagamento registrado**; finalizados sem pagamento aparecem como "A receber".
- **Exclusão:** registros com histórico não são apagados (409); devem ser desativados.
- **Sessão:** JWT em `sessionStorage` (8 h). Acesso a registro alheio responde 404.
- **Lembretes/notificações:** internas (sino no menu). Envio por e-mail/SMS/WhatsApp **não** implementado.

## Pendências / dependem de configuração externa
- **Recuperação de senha:** o protótipo do TCC prevê; requer servidor SMTP. Não implementada (não há botão sem função). Hoje o administrador redefine senhas em Usuários.
- **Backup periódico (RNF):** configurar no servidor/provedor (ex.: `mysqldump` agendado ou backup gerenciado).
- **Alta disponibilidade / HTTPS:** responsabilidade da infraestrutura de publicação.
- Limitação de tentativas de login (rate limiting) não implementada; recomenda-se no proxy reverso.

## Publicação
1. Banco MySQL gerenciado; defina `DB_URL`, `DB_USER`, `DB_PASSWORD`.
2. `cd backend && mvn -DskipTests package` → `java -jar target/barbearia-manager-1.0.0.jar` com `APP_JWT_SECRET`, `APP_CORS_ORIGINS` (URL do front), `APP_ADMIN_EMAIL/PASSWORD` (apenas no 1º start).
3. `cd frontend && npm run build` → servir `dist/` (Nginx/Netlify/etc.) com HTTPS e proxy de `/api` para o backend.
