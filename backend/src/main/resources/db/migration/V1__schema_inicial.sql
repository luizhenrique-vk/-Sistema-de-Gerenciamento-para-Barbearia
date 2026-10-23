CREATE TABLE usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  telefone VARCHAR(20),
  senha VARCHAR(100) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_usuario_email UNIQUE (email),
  CONSTRAINT ck_usuario_tipo CHECK (tipo IN ('CLIENTE','BARBEIRO','ADMIN'))
);

CREATE TABLE barbeiro (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  especialidade VARCHAR(120) NOT NULL,
  CONSTRAINT uk_barbeiro_usuario UNIQUE (usuario_id),
  CONSTRAINT fk_barbeiro_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE servico (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  descricao VARCHAR(400),
  preco DECIMAL(10,2) NOT NULL,
  duracao INT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT ck_servico_preco CHECK (preco >= 0),
  CONSTRAINT ck_servico_duracao CHECK (duracao > 0)
);

CREATE TABLE agendamento (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cliente_id BIGINT NOT NULL,
  barbeiro_id BIGINT NOT NULL,
  servico_id BIGINT NOT NULL,
  data_hora TIMESTAMP NOT NULL,
  data_hora_fim TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL,
  observacoes VARCHAR(400),
  preco_cobrado DECIMAL(10,2) NOT NULL,
  pago BOOLEAN NOT NULL DEFAULT FALSE,
  forma_pagamento VARCHAR(20),
  data_pagamento TIMESTAMP NULL,
  lembrete_enviado BOOLEAN NOT NULL DEFAULT FALSE,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ag_cliente FOREIGN KEY (cliente_id) REFERENCES usuario(id),
  CONSTRAINT fk_ag_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro(id),
  CONSTRAINT fk_ag_servico FOREIGN KEY (servico_id) REFERENCES servico(id),
  CONSTRAINT ck_ag_status CHECK (status IN ('AGENDADO','CANCELADO','FINALIZADO'))
);
CREATE INDEX idx_ag_barbeiro_data ON agendamento (barbeiro_id, data_hora);
CREATE INDEX idx_ag_cliente_data ON agendamento (cliente_id, data_hora);

CREATE TABLE disponibilidade (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  barbeiro_id BIGINT NOT NULL,
  dia_semana INT NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fim TIME NOT NULL,
  CONSTRAINT fk_disp_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro(id),
  CONSTRAINT ck_disp_dia CHECK (dia_semana BETWEEN 1 AND 7),
  CONSTRAINT ck_disp_horas CHECK (hora_fim > hora_inicio)
);

CREATE TABLE horario_funcionamento (
  dia_semana INT PRIMARY KEY,
  aberto BOOLEAN NOT NULL,
  abertura TIME NOT NULL,
  fechamento TIME NOT NULL,
  CONSTRAINT ck_hf_dia CHECK (dia_semana BETWEEN 1 AND 7)
);
-- Regra padrão de funcionamento (configurável pelo administrador): seg-sáb 09:00-19:00, domingo fechado.
INSERT INTO horario_funcionamento (dia_semana, aberto, abertura, fechamento) VALUES
 (1, TRUE, '09:00:00', '19:00:00'), (2, TRUE, '09:00:00', '19:00:00'), (3, TRUE, '09:00:00', '19:00:00'),
 (4, TRUE, '09:00:00', '19:00:00'), (5, TRUE, '09:00:00', '19:00:00'), (6, TRUE, '09:00:00', '19:00:00'),
 (7, FALSE, '09:00:00', '19:00:00');

CREATE TABLE avaliacao (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  agendamento_id BIGINT NOT NULL,
  nota INT NOT NULL,
  comentario VARCHAR(500),
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_avaliacao_ag UNIQUE (agendamento_id),
  CONSTRAINT fk_av_ag FOREIGN KEY (agendamento_id) REFERENCES agendamento(id),
  CONSTRAINT ck_av_nota CHECK (nota BETWEEN 1 AND 5)
);

CREATE TABLE notificacao (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  mensagem VARCHAR(300) NOT NULL,
  lida BOOLEAN NOT NULL DEFAULT FALSE,
  criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE INDEX idx_notif_usuario ON notificacao (usuario_id, lida);
