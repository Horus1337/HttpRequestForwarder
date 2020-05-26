package de.scoopsoftware.trinity.tpc.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Redirector extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Redirector.class);
    private final WebTarget webTarget;

    Redirector(WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo().replace("//", "/");

        WebTarget currentTarget = webTarget.path("relay" + path);
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            currentTarget = currentTarget.queryParam(entry.getKey(), entry.getValue());
        }
        String lotusResponse;
        if ("POST".equals(request.getMethod())) {
            lotusResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                         .post(Entity.json(new ObjectMapper().readValue(request.getInputStream(), Object.class)))
                                         .readEntity(String.class);
        } else if ("PUT".equals(request.getMethod())) {
            lotusResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                         .put(Entity.json(new ObjectMapper().readValue(request.getInputStream(), Object.class)))
                                         .readEntity(String.class);
        } else {
            lotusResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                         .build(request.getMethod())
                                         .invoke()
                                         .readEntity(String.class);
        }
        response.getOutputStream().write(lotusResponse.getBytes(StandardCharsets.UTF_8));
    }
}
