package br.edu.unigran.barbearia;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes de integração das regras de negócio: agendamento, conflitos, permissões, cancelamento, pagamento e relatório. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxoAgendamentoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ServicoRepository servicos;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired PasswordEncoder encoder;

    String adminToken, barbeiroToken, clienteAToken, clienteBToken;
    Long barbeiroId, servicoId, clienteAId, clienteBId;
    LocalDate segunda;

    @BeforeEach
    void preparar() throws Exception {
        Usuario admin = new Usuario();
        admin.setNome("Admin"); admin.setEmail("admin@teste.com"); admin.setSenha(encoder.encode("senha-admin-123")); admin.setTipo(TipoUsuario.ADMIN);
        usuarios.save(admin);
        adminToken = login("admin@teste.com", "senha-admin-123");

        JsonNode b = enviar(post("/api/admin/barbeiros"), adminToken, Map.of("nome", "João Barbeiro", "email", "joao@teste.com", "senha", "senha-joao-123", "especialidade", "Degradê"), 201);
        barbeiroId = b.get("id").asLong();
        JsonNode s = enviar(post("/api/admin/servicos"), adminToken, Map.of("nome", "Corte + Barba", "descricao", "Combo", "preco", 60.00, "duracao", 60), 201);
        servicoId = s.get("id").asLong();
        barbeiroToken = login("joao@teste.com", "senha-joao-123");

        segunda = LocalDate.now().plusDays(14).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        enviar(put("/api/barbeiro/disponibilidade"), barbeiroToken, Map.of("itens", java.util.List.of(Map.of("diaSemana", 1, "horaInicio", "09:00", "horaFim", "12:00"))), 200);

        clienteAToken = registrar("Cliente A", "a@teste.com");
        clienteBToken = registrar("Cliente B", "b@teste.com");
        clienteAId = usuarios.findByEmailIgnoreCase("a@teste.com").orElseThrow().getId();
        clienteBId = usuarios.findByEmailIgnoreCase("b@teste.com").orElseThrow().getId();
    }

    // ---------- helpers ----------
    JsonNode enviar(MockHttpServletRequestBuilder req, String token, Object corpo, int esperado) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        MvcResult r = mvc.perform(req).andReturn();
        assertEquals(esperado, r.getResponse().getStatus(), "Resposta: " + r.getResponse().getContentAsString());
        String c = r.getResponse().getContentAsString();
        return c.isBlank() ? null : json.readTree(c);
    }
    String login(String email, String senha) throws Exception { return enviar(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200).get("token").asText(); }
    String registrar(String nome, String email) throws Exception {
        return enviar(post("/api/auth/register"), null, Map.of("nome", nome, "email", email, "senha", "senha-cliente-123"), 201).get("token").asText();
    }
    Map<String, Object> pedido(String hora) {
        return Map.of("barbeiroId", barbeiroId, "servicoId", servicoId, "dataHora", segunda + "T" + hora);
    }

    // ---------- testes ----------
    @Test
    void cadastroPublicoSempreCriaCliente() throws Exception {
        JsonNode r = enviar(post("/api/auth/register"), null, Map.of("nome", "Espertinho", "email", "e@teste.com", "senha", "senha-123456", "tipo", "ADMIN"), 201);
        assertEquals("CLIENTE", r.get("usuario").get("tipo").asText());
        assertFalse(r.toString().contains("senha-123456"));
        assertFalse(r.get("usuario").has("senha"));
    }

    @Test
    void loginComCredenciaisInvalidasRetorna401() throws Exception {
        enviar(post("/api/auth/login"), null, Map.of("email", "admin@teste.com", "senha", "errada-errada"), 401);
        enviar(post("/api/auth/login"), null, Map.of("email", "naoexiste@teste.com", "senha", "qualquer-coisa"), 401);
    }

    @Test
    void horariosLivresRespeitamDisponibilidadeEDuracao() throws Exception {
        JsonNode r = enviar(get("/api/barbeiros/" + barbeiroId + "/horarios?servicoId=" + servicoId + "&data=" + segunda), clienteAToken, null, 200);
        // janela 09:00-12:00, serviço de 60 min, passo de 30 min => 09:00 ... 11:00
        assertTrue(r.get("horarios").get(0).asText().startsWith("09:00"));
        assertEquals(5, r.get("horarios").size());
        // terça-feira: sem disponibilidade
        JsonNode terca = enviar(get("/api/barbeiros/" + barbeiroId + "/horarios?servicoId=" + servicoId + "&data=" + segunda.plusDays(1)), clienteAToken, null, 200);
        assertEquals(0, terca.get("horarios").size());
    }

    @Test
    void naoPermiteConflitoDeAgendamentoComSobreposicao() throws Exception {
        enviar(post("/api/agendamentos"), clienteAToken, pedido("09:00:00"), 201);
        enviar(post("/api/agendamentos"), clienteBToken, pedido("09:00:00"), 409);   // mesmo horário
        enviar(post("/api/agendamentos"), clienteBToken, pedido("09:30:00"), 409);   // sobrepõe (09:00-10:00)
        enviar(post("/api/agendamentos"), clienteBToken, pedido("10:00:00"), 201);   // encosta, mas não sobrepõe
        JsonNode livres = enviar(get("/api/barbeiros/" + barbeiroId + "/horarios?servicoId=" + servicoId + "&data=" + segunda), clienteBToken, null, 200);
        assertEquals(1, livres.get("horarios").size()); // restou apenas 11:00
    }

    @Test
    void naoPermiteHorarioForaDaDisponibilidadeNemPassado() throws Exception {
        enviar(post("/api/agendamentos"), clienteAToken, pedido("14:00:00"), 409);
        enviar(post("/api/agendamentos"), clienteAToken, Map.of("barbeiroId", barbeiroId, "servicoId", servicoId, "dataHora", LocalDate.now().minusDays(3) + "T10:00:00"), 422);
    }

    @Test
    void clienteNaoEscolheOutroClienteNoAgendamento() throws Exception {
        Map<String, Object> corpo = new java.util.HashMap<>(pedido("09:00:00"));
        corpo.put("clienteId", clienteBId);
        JsonNode r = enviar(post("/api/agendamentos"), clienteAToken, corpo, 201);
        assertEquals(clienteAId, r.get("clienteId").asLong());
    }

    @Test
    void clienteNaoAcessaNemCancelaAgendamentoDeOutro() throws Exception {
        JsonNode ag = enviar(post("/api/agendamentos"), clienteAToken, pedido("09:00:00"), 201);
        enviar(post("/api/agendamentos/" + ag.get("id").asLong() + "/cancelar"), clienteBToken, null, 404);
        assertEquals(0, enviar(get("/api/agendamentos/meus"), clienteBToken, null, 200).size());
        JsonNode cancelado = enviar(post("/api/agendamentos/" + ag.get("id").asLong() + "/cancelar"), clienteAToken, null, 200);
        assertEquals("CANCELADO", cancelado.get("status").asText());
        // horário volta a ficar livre
        enviar(post("/api/agendamentos"), clienteBToken, pedido("09:00:00"), 201);
    }

    @Test
    void permissoesPorPerfil() throws Exception {
        enviar(get("/api/servicos"), null, null, 401);
        enviar(get("/api/admin/usuarios"), clienteAToken, null, 403);
        enviar(get("/api/admin/relatorios/dashboard"), barbeiroToken, null, 403);
        enviar(get("/api/barbeiro/agenda"), clienteAToken, null, 403);
        enviar(post("/api/admin/servicos"), clienteAToken, Map.of("nome", "X", "preco", 10, "duracao", 30), 403);
        enviar(get("/api/admin/usuarios"), adminToken, null, 200);
    }

    @Test
    void clienteNaoCancelaComMenosDeDuasHorasDeAntecedencia() throws Exception {
        Agendamento a = novoAgendamento(LocalDateTime.now().plusMinutes(60), StatusAgendamento.AGENDADO, clienteAId);
        enviar(post("/api/agendamentos/" + a.getId() + "/cancelar"), clienteAToken, null, 422);
        // o administrador pode cancelar
        enviar(post("/api/agendamentos/" + a.getId() + "/cancelar"), adminToken, null, 200);
    }

    @Test
    void barbeiroFinalizaSomenteOsProprios_PagamentoEhRegistradoUmaVez_RelatorioUsaDadosReais() throws Exception {
        Agendamento a = novoAgendamento(LocalDateTime.now().minusHours(3), StatusAgendamento.AGENDADO, clienteAId);
        // outro barbeiro não enxerga o agendamento
        JsonNode outro = enviar(post("/api/admin/barbeiros"), adminToken, Map.of("nome", "Pedro", "email", "pedro@teste.com", "senha", "senha-pedro-123", "especialidade", "Barba"), 201);
        String tokenPedro = login("pedro@teste.com", "senha-pedro-123");
        enviar(patch("/api/agendamentos/" + a.getId() + "/status"), tokenPedro, Map.of("status", "FINALIZADO"), 404);

        enviar(patch("/api/agendamentos/" + a.getId() + "/status"), barbeiroToken, Map.of("status", "FINALIZADO"), 200);
        // pagamento: só admin, só uma vez
        enviar(patch("/api/admin/agendamentos/" + a.getId() + "/pagamento"), barbeiroToken, Map.of("forma", "PIX"), 403);
        JsonNode pago = enviar(patch("/api/admin/agendamentos/" + a.getId() + "/pagamento"), adminToken, Map.of("forma", "PIX"), 200);
        assertTrue(pago.get("pago").asBoolean());
        enviar(patch("/api/admin/agendamentos/" + a.getId() + "/pagamento"), adminToken, Map.of("forma", "PIX"), 409);

        JsonNode fin = enviar(get("/api/admin/relatorios/financeiro?periodo=DIA&data=" + a.getDataHora().toLocalDate()), adminToken, null, 200);
        assertEquals(0, new BigDecimal("60.00").compareTo(fin.get("faturamento").decimalValue()));
        assertEquals(1, fin.get("atendimentos").asInt());
        JsonNode rank = enviar(get("/api/admin/relatorios/servicos-mais-realizados?de=" + a.getDataHora().toLocalDate() + "&ate=" + a.getDataHora().toLocalDate()), adminToken, null, 200);
        assertEquals("Corte + Barba", rank.get(0).get("nome").asText());
        assertEquals(1, rank.get(0).get("quantidade").asInt());
    }

    @Test
    void avaliacaoSomenteDoProprioAtendimentoFinalizadoEUmaVez() throws Exception {
        Agendamento a = novoAgendamento(LocalDateTime.now().minusDays(1), StatusAgendamento.FINALIZADO, clienteAId);
        Agendamento futuro = novoAgendamento(LocalDateTime.now().plusDays(5), StatusAgendamento.AGENDADO, clienteAId);
        enviar(post("/api/avaliacoes"), clienteBToken, Map.of("agendamentoId", a.getId(), "nota", 5), 404);
        enviar(post("/api/avaliacoes"), clienteAToken, Map.of("agendamentoId", futuro.getId(), "nota", 5), 422);
        enviar(post("/api/avaliacoes"), clienteAToken, Map.of("agendamentoId", a.getId(), "nota", 6), 400);
        enviar(post("/api/avaliacoes"), clienteAToken, Map.of("agendamentoId", a.getId(), "nota", 5, "comentario", "Ótimo"), 201);
        enviar(post("/api/avaliacoes"), clienteAToken, Map.of("agendamentoId", a.getId(), "nota", 4), 409);
    }

    @Test
    void servicoComAgendamentoNaoPodeSerExcluidoMasPodeSerDesativado() throws Exception {
        novoAgendamento(LocalDateTime.now().plusDays(3), StatusAgendamento.AGENDADO, clienteAId);
        enviar(delete("/api/admin/servicos/" + servicoId), adminToken, null, 409);
        enviar(put("/api/admin/servicos/" + servicoId), adminToken, Map.of("nome", "Corte + Barba", "preco", 60, "duracao", 60, "ativo", false), 200);
        assertEquals(0, enviar(get("/api/servicos"), clienteAToken, null, 200).size());
    }

    @Test
    void clienteAtualizaApenasOProprioPerfilEExigeSenhaAtualParaTrocarSenha() throws Exception {
        enviar(put("/api/me"), clienteAToken, Map.of("nome", "Novo Nome", "email", "a@teste.com", "novaSenha", "outra-senha-123"), 422);
        JsonNode ok = enviar(put("/api/me"), clienteAToken, Map.of("nome", "Novo Nome", "email", "a@teste.com", "senhaAtual", "senha-cliente-123", "novaSenha", "outra-senha-123"), 200);
        assertEquals("Novo Nome", ok.get("nome").asText());
        login("a@teste.com", "outra-senha-123");
        enviar(put("/api/me"), clienteAToken, Map.of("nome", "X", "email", "b@teste.com"), 409); // e-mail de outro usuário
    }

    private Agendamento novoAgendamento(LocalDateTime inicio, StatusAgendamento status, Long clienteId) {
        Agendamento a = new Agendamento();
        a.setCliente(usuarios.findById(clienteId).orElseThrow());
        a.setBarbeiro(barbeiros.findById(barbeiroId).orElseThrow());
        Servico s = servicos.findById(servicoId).orElseThrow();
        a.setServico(s); a.setDataHora(inicio); a.setDataHoraFim(inicio.plusMinutes(s.getDuracao()));
        a.setPrecoCobrado(s.getPreco()); a.setStatus(status);
        return agendamentos.saveAndFlush(a);
    }
}
