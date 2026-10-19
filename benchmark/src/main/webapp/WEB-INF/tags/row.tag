<%@ tag body-content="empty" %>
<%@ attribute name="product" required="true" type="org.glassfish.wasp.benchmark.Product" %>
<%@ attribute name="index" required="true" type="java.lang.Integer" %>
<%@ attribute name="badge" fragment="true" %>
<%@ taglib prefix="b" uri="urn:wasp:benchmark" %>
<tr class="${index % 2 == 0 ? 'even' : 'odd'}">
  <td>${index + 1}</td>
  <td>${product.sku}</td>
  <td>${b:upper(product.name)}</td>
  <td><b:money value="${product.price}"/></td>
  <td><b:money value="${product.price * product.quantity}"/></td>
  <td>${empty product.tags ? '-' : b:join(product.tags, ' ')} (${product.tags.size()})</td>
  <td>${product.attributes['color']}/${product.attributes.size}</td>
  <td><jsp:invoke fragment="badge"/></td>
</tr>
