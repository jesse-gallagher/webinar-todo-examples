<%--

    Copyright © 2025 Jesse Gallagher

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions" %>
<t:layout>
	<form action="${mvc.basePath}/todos/${todo.documentId}" method="POST" enctype="application/x-www-form-urlencoded">
		<dl>
			<dt>Created</dt>
			<dd><c:out value="${todo.created}"/></dd>
			
			<dt>Title</dt>
			<dd><input name="title" type="text" value="${fn:escapeXml(todo.title)}"/></dd>
			
			<dt>Status</dt>
			<dd>
				<input name="status" type="radio" value="Incomplete" ${todo.status == 'Incomplete' ? 'checked' : ''}> Incomplete
				<input name="status" type="radio" value="Complete" ${todo.status == 'Complete' ? 'checked' : ''}> Complete
			</dd>
		</dl>
		
		<input type="submit" value="Save"/>
	</form>
</t:layout>