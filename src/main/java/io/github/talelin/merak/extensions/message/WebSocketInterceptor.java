package io.github.talelin.merak.extensions.message;

import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.Claim;
import io.github.talelin.core.token.DoubleJWT;
import io.github.talelin.merak.common.interceptor.AuthorizeVerifyResolverImpl;
import io.github.talelin.merak.model.PermissionDO;
import io.github.talelin.merak.model.UserDO;
import io.github.talelin.merak.service.GroupService;
import io.github.talelin.merak.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class WebSocketInterceptor implements HandshakeInterceptor {
    @Autowired
    private DoubleJWT jwt;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler webSocketHandler, Map<String, Object> attributes) throws Exception {
        List<String> authorization = request.getHeaders().get(AuthorizeVerifyResolverImpl.authorizationHeader);
        if (authorization == null || authorization.isEmpty()) {
            writeMessageToBody(response, "authorization field is required");
            return false;
        }
        String tokenStr = authorization.get(0);
        Map<String, Claim> claims;
        try {
            claims = jwt.decodeAccessToken(tokenStr);
        } catch (TokenExpiredException e) {
            writeMessageToBody(response, "token is expired");
            return false;
        } catch (AlgorithmMismatchException | SignatureVerificationException | JWTDecodeException | InvalidClaimException e) {
            writeMessageToBody(response, "token is invalid");
            return false;
        }
        if (claims == null) {
            writeMessageToBody(response, "token is invalid, can't be decode");
            return false;
        }
        int identity = claims.get("identity").asInt();
        UserDO user = userService.getById(identity);
        if (user == null) {
            writeMessageToBody(response, "user is not found");
            return false;
        }
        attributes.put("user", user);
        // 超级管理员，接收一切事件
        if (verifyAdmin(user)) {
            attributes.put("events", "*");
            return true;
        }
        long userId = user.getId();
        // 获得用户可以接收的事件
        List<PermissionDO> permissions = userService.getUserPermissionsByModule(userId, "消息推送");
        String events = permissions.stream().map(PermissionDO::getName).collect(Collectors.joining(","));
        attributes.put("events", events);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

    }

    private boolean verifyAdmin(UserDO user) {
        return groupService.checkIsRootByUserId(user.getId());
    }

    private void writeMessageToBody(ServerHttpResponse response, String message) throws IOException {
        response.getBody().write(message.getBytes(Charset.forName("UTF8")));
    }

}