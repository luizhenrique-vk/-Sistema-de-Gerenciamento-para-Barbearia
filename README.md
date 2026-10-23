# Barbearia Manager

Sistema web para gestão de barbearias: agendamento online, agenda dos barbeiros, controle de pagamentos e relatórios financeiros.

> Projeto de conclusão de curso, evoluído para uma aplicação full stack completa.

## Telas

## Funcionalidades

**Cliente:** cadastro e login, escolha de serviço, barbeiro e horário disponível, cancelamento, histórico de atendimentos e avaliações.

**Barbeiro:** agenda diária e semanal, dados do cliente, definição da própria disponibilidade e finalização de atendimentos.

**Administrador:** dashboard com indicadores, cadastro de clientes, barbeiros e serviços, horário de funcionamento, controle de pagamentos e relatórios diário, semanal e mensal.

## Tecnologias

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Bean Validation
- **Banco:** MySQL 8 com migrações versionadas (Flyway)
- **Frontend:** React 18, TypeScript, Vite, Recharts
- **Testes:** JUnit 5 e MockMvc (testes de integração com H2)

## Destaques técnicos

- **Sem conflito de horários:** cálculo único dos horários livres, validado no backend, com bloqueio por barbeiro para evitar agendamentos simultâneos.
- **Autorização por perfil (Cliente, Barbeiro, Administrador):** o usuário vem sempre do token, nunca do corpo da requisição, e o acesso a dados de outra pessoa retorna 404.
- **Regras de negócio no servidor:** cancelamento com antecedência mínima, estados do agendamento e pagamento registrado uma única vez.
- **Relatórios calculados com dados reais** do banco, sem valores fixos.
- **Segurança:** senhas com BCrypt, segredo do JWT e administrador inicial por variáveis de ambiente, sem credenciais no código.

## Como executar

Requisitos: JDK 21, Maven, Node 20+ e MySQL 8.

```bash
# 1. Banco: crie o schema
CREATE DATABASE barbearia;

# 2. Configure as variáveis (modelo em .env.example)
#    DB_PASSWORD, APP_JWT_SECRET (32+ caracteres), APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD

# 3. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# 4. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

O primeiro administrador é criado automaticamente a partir das variáveis `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`.

Testes do backend: `cd backend && mvn test`

## Estrutura

```
backend/   API REST em camadas (web, service, repository, domain, dto, security)
frontend/  SPA React (pages, components, hooks, services)
```

## Limitações e próximos passos

- Recuperação de senha e envio de lembretes por e-mail (hoje as notificações são internas ao sistema).
- Limite de tentativas de login.
- Backup e monitoramento em produção.
