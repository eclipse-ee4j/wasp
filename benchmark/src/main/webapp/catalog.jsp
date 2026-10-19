<%--
    The benchmark page: a catalog of products, rendered with a static include, jsp:useBean and jsp:setProperty, a
    classic iteration tag (pooled), a tag file with a fragment attribute, a simple tag, functions, the Expression
    Language on beans, lists, maps and method calls, and a dynamic include with a parameter.
--%><%@ page contentType="text/html;charset=UTF-8" %><%@
    taglib prefix="b" uri="urn:wasp:benchmark" %><%@
    taglib prefix="t" tagdir="/WEB-INF/tags" %><jsp:useBean id="summary" class="org.glassfish.wasp.benchmark.Summary" scope="request"/><jsp:setProperty name="summary" property="title" value="Catalog"/><%@
    include file="/WEB-INF/jspf/header.jspf" %>
<h1>${b:upper(summary.title)} (${catalog.products.size()} products in ${catalog.categories.size()} categories)</h1>
<b:forEach items="${catalog.categories}" var="category">
<h2>${category.name}</h2>
<table>
<b:forEach items="${category.products}" var="product" indexVar="index">
<t:row product="${product}" index="${index}">
  <jsp:attribute name="badge">${product.quantity == 0 ? 'sold out' : (product.quantity lt 3 ? 'few left' : 'in stock')}</jsp:attribute>
</t:row>
</b:forEach>
</table>
</b:forEach>
<p>Total value: <b:money value="${catalog.totalValue}"/></p>
<p>Tags: ${b:join(catalog.tags, ', ')}</p>
<jsp:include page="/footer.jsp"><jsp:param name="rows" value="${catalog.products.size()}"/></jsp:include>
