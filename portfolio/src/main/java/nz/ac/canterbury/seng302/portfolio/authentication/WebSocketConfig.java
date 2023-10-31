package nz.ac.canterbury.seng302.portfolio.authentication;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSocket use
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints (StompEndpointRegistry registry) {
        registry.addEndpoint("websocket") //Registers endpoint that the connection exists on
                .setAllowedOriginPatterns("https://*.canterbury.ac.nz") //Set CORS allowed origin (also allows localhost)
                .withSockJS() //Fallback if browser doesn't support WebSockets
                .setClientLibraryUrl( "https://cdn.jsdelivr.net/sockjs/1.4.0/sockjs.min.js" ); //SockJS Library
    }

    @Override
    public void configureMessageBroker (MessageBrokerRegistry registry) {
        //All WebSocket message receivers should be prefixed with "/websocketsReceive"
        registry.enableSimpleBroker("/websocketsReceive");
        //All WebSocket message senders should be prefixed with "/websocketsSend"
        registry.setApplicationDestinationPrefixes("/websocketsSend");
    }

}
