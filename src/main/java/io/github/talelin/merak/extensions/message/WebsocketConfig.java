package io.github.talelin.merak.extensions.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    @Bean
    public MessageWebSocketHandler messageWebSocketHandler() {
        return new MessageWebSocketHandler();
    }

    @Bean
    public MessageInterceptor messageInterceptor() {
        return new MessageInterceptor();
    }

    @Bean
    public WsHandler wsHandler() {
        return new WsHandlerImpl();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry handlerRegistry) {
        handlerRegistry
                .addHandler(messageWebSocketHandler(), "ws/message")
                .addInterceptors(new WebSocketInterceptor())
                .setAllowedOrigins("*");
    }
}
