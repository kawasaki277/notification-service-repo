package com.clinicbooking.notification.infrastructure.out.sms;

import com.clinicbooking.notification.application.port.out.SmsSenderPort;
import com.clinicbooking.notification.domain.DeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

/** TextBee outbound adapter. Empty API key intentionally enables the local simulated mode. */
public class TextbeeSmsAdapter implements SmsSenderPort {
    private static final Logger log=LoggerFactory.getLogger(TextbeeSmsAdapter.class);
    private final RestClient client; private final String apiKey; private final String deviceId;
    public TextbeeSmsAdapter(RestClient.Builder builder,String baseUrl,String apiKey,String deviceId,long connectMs,long readMs){
        this(builder==null?null:RestClientsFactory.create(builder,baseUrl,apiKey,connectMs,readMs),apiKey,deviceId);
    }
    /** Testable constructor for a preconfigured RestClient. */
    public TextbeeSmsAdapter(RestClient client,String apiKey,String deviceId){
        this.client=client; this.apiKey=apiKey==null?"":apiKey.trim(); this.deviceId=deviceId==null?"":deviceId.trim();
    }
    @Override public void send(String to,String body){
        if(apiKey.isBlank()){log.info("[SIMULADO] SMS TextBee a {} | {}",mask(to),body);return;}
        Map<String,Object> payload=Map.of("recipients",List.of(to),"message",body);
        String path=deviceId.isBlank()?"/gateway/send-sms":"/gateway/devices/{deviceId}/send-sms";
        try { if(deviceId.isBlank()) client.post().uri(path).body(payload).retrieve().toBodilessEntity(); else client.post().uri(path,deviceId).body(payload).retrieve().toBodilessEntity(); }
        catch(RestClientResponseException e){throw new DeliveryException("TextBee HTTP "+e.getStatusCode().value()+": "+e.getResponseBodyAsString(),e);}
        catch(RestClientException e){throw new DeliveryException("No fue posible comunicarse con TextBee",e);}
        log.info("SMS aceptado por TextBee para {}",mask(to));
    }
    private static String mask(String phone){if(phone==null||phone.length()<6)return "***";return phone.substring(0,3)+"*".repeat(phone.length()-5)+phone.substring(phone.length()-2);}
}
