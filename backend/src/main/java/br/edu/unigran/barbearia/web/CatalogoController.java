package br.edu.unigran.barbearia.web;

import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/** Consultas disponíveis a qualquer usuário autenticado: serviços, profissionais e horários livres. */
@RestController
@RequestMapping("/api")
public class CatalogoController {
    private final ServicoService servicos;
    private final BarbeiroService barbeiros;
    private final AgendamentoService agendamentos;

    public CatalogoController(ServicoService servicos, BarbeiroService barbeiros, AgendamentoService agendamentos) { this.servicos = servicos; this.barbeiros = barbeiros; this.agendamentos = agendamentos; }

    @GetMapping("/servicos")
    public List<ServicoResponse> servicos() { return servicos.listar(true); }

    @GetMapping("/barbeiros")
    public List<BarbeiroResponse> barbeiros() { return barbeiros.listar(true); }

    @GetMapping("/barbeiros/{id}/horarios")
    public SlotsResponse horarios(@PathVariable Long id, @RequestParam Long servicoId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return new SlotsResponse(data, agendamentos.horariosLivres(id, servicoId, data));
    }
}
