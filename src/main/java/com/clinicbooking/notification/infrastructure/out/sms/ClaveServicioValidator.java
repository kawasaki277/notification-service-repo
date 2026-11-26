package com.clinicbooking.notification.infrastructure.out.sms;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
/** Optional internal-service authentication, based on the SMS source project's validator. */
@Component public class ClaveServicioValidator {
 private final String expected; public ClaveServicioValidator(@Value("${notification.sms.internal-key:}") String expected){this.expected=expected==null?"":expected;}
 public void validateIfConfigured(String received){if(expected.isBlank())return;boolean ok=received!=null&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),received.getBytes(StandardCharsets.UTF_8));if(!ok)throw new IllegalArgumentException("Clave interna inválida (X-Internal-Key)");}
}
