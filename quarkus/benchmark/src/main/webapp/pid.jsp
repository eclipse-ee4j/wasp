<%-- The ID of the application's process, so the benchmark can measure its memory --%><%@ page contentType="text/plain" %><%= ProcessHandle.current().pid() %>
