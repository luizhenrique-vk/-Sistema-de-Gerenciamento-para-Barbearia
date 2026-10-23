package br.edu.unigran.barbearia.config;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.repository.UsuarioRepository;
import org.slf4j.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador de forma controlada: apenas quando NÃO existe nenhum administrador e as variáveis
 * APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD estão definidas. Nenhuma credencial fixa existe no código.
 */
@Component
public class AdminInicial implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder, AppProperties props) { this.usuarios = usuarios; this.encoder = encoder; this.props = props; }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.existsByTipo(TipoUsuario.ADMIN)) return;
        String email = props.adminInicialEmail(), senha = props.adminInicialSenha();
        if (email == null || email.isBlank() || senha == null || senha.length() < 8) {
            log.warn("Nenhum administrador cadastrado. Defina APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD (mín. 8 caracteres) e reinicie para criar o primeiro acesso.");
            return;
        }
        Usuario u = new Usuario();
        u.setNome("Administrador"); u.setEmail(email.trim().toLowerCase()); u.setSenha(encoder.encode(senha)); u.setTipo(TipoUsuario.ADMIN);
        usuarios.save(u);
        log.info("Administrador inicial criado: {}", u.getEmail());
    }
}
