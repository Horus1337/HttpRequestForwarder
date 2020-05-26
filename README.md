# HttpRequestForwarder
Simple HttpServlet that can be used in a Jetty to forward requests

I needed a Resource, that can be used by a Jetty, that will forward the complete Reqest to a different target, that may use specific SSL configuration (that is why I could not use HttpServletResponse sendRedirect)

It was more complicated then anticipated, that's why I thought I'd share it.

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
