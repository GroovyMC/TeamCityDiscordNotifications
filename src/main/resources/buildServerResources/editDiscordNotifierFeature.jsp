<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<tr class="noBorder" id="discordNotifier.url.container">
  <th><label for="discordNotifier.url">Webhook URL:</label></th>
  <td>
    <props:textProperty name="discordNotifier.url"/>
  </td>
</tr>