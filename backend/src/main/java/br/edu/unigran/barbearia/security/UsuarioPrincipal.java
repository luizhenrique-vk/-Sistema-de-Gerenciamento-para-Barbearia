package br.edu.unigran.barbearia.security;

import br.edu.unigran.barbearia.domain.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public record UsuarioPrincipal(Long id, String email, String senha, TipoUsuario tipo, boolean ativo) implements UserDetails {
    public static UsuarioPrincipal de(Usuario u) {
        return new UsuarioPrincipal(u.getId(), u.getEmail(), u.getSenha(), u.getTipo(), u.isAtivo());
    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name())); }
    @Override public String getPassword() { return senha; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return ativo; }
    public boolean ehAdmin() { return tipo == TipoUsuario.ADMIN; }
}
