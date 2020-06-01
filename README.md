# HttpRequestForwarder
Simple HttpServlet that can be used in a Jetty to forward requests

I needed a Resource, that can be used by a Jetty, that will forward the complete Reqest to a different target, that may use specific SSL configuration (that is why I could not use HttpServletResponse sendRedirect)

It was more complicated then anticipated, that's why I thought I'd share it.

    import java.io.IOException;
    import java.io.InputStream;
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
    import com.google.common.io.ByteStreams;
    
    public class RequestForwarder extends HttpServlet {
        private static final Logger logger = LoggerFactory.getLogger(RequestForwarder.class);
        private static final String prefix = "relay";
        private final WebTarget webTarget;
    
        RequestForwarder(WebTarget webTarget) {
            this.webTarget = webTarget;
        }
    
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String path = request.getPathInfo().replace("//", "/");
    
            WebTarget currentTarget = webTarget.path(prefix + path);
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (var entry : parameterMap.entrySet()) {
                currentTarget = currentTarget.queryParam(entry.getKey(), (Object[]) entry.getValue());
            }
    
            InputStream relayResponse;
            if ("POST".equals(request.getMethod())) {
                relayResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                             .accept(MediaType.APPLICATION_OCTET_STREAM)
                                             .post(Entity.json(new ObjectMapper().readValue(request.getInputStream(), Object.class)))
                                             .readEntity(InputStream.class);
            } else if ("PUT".equals(request.getMethod())) {
                relayResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                             .accept(MediaType.APPLICATION_OCTET_STREAM)
                                             .put(Entity.json(new ObjectMapper().readValue(request.getInputStream(), Object.class)))
                                             .readEntity(InputStream.class);
            } else {
                relayResponse = currentTarget.request(MediaType.APPLICATION_JSON)
                                             .accept(MediaType.APPLICATION_OCTET_STREAM)
                                             .build(request.getMethod())
                                             .invoke()
                                             .readEntity(InputStream.class);
            }
            ByteStreams.copy(relayResponse, response.getOutputStream());
        }
    }
    


The Webtarget can be set up for example like this and point anywhere

    public static WebTarget createResource(String url, ObjectMapper objectMapper, SslContextFactory sslContextFactory) throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        if (sslContextFactory != null) {
            logger.info("building with ssl");
            sslContextFactory.start();
            builder.sslContext(sslContextFactory.getSslContext());
        }

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper(objectMapper);

        return builder.withConfig(new ClientConfig(jacksonProvider)).build().target(url);
    }
