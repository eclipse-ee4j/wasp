<%@ page contentType="text/plain" import="java.io.InputStream, java.util.Properties" %><%
    Properties properties = new Properties();
    try (InputStream in = application.getResourceAsStream("/WEB-INF/data.properties")) {
        if (in != null) {
            properties.load(in);
        }
    }
%><%= properties.getProperty("greeting") %>|<%= application.getResource("/WEB-INF/data.properties") != null %>
