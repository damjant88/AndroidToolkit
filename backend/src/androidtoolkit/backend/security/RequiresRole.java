package androidtoolkit.backend.security;

import androidtoolkit.domain.tenant.TenantRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to restrict endpoint access to users with a minimum tenant role.
 * Users with the specified role or higher (OWNER > ADMIN > USER) are allowed.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    TenantRole value();
}
