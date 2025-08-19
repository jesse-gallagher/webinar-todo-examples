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
<t:layout>
	<div id="todos">
		<table>
			<thead>
				<tr>
					<th>Created</th>
					<th>Title</th>
					<th>Status</th>
					<th></th>
				</tr>
			</thead>
			<tbody>
			<c:forEach items="${todos}" var="todo">
				<tr>
					<td><c:out value="${todo.created}"/></td>
					<td><a href="${mvc.basePath}/todos/${todo.documentId}"><c:out value="${todo.title}"/></a></td>
					<td><c:out value="${todo.status}"/></td>
					<td>
						<form action="${mvc.basePath}/todos/${todo.documentId}/delete" method="POST" enctype="application/x-www-form-urlencoded">
							<input type="submit" value="Delete" onclick="return confirm('Delete this To-Do?')"/>
						</form>
						
					</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</div>
	
	<c:if test="${param.status ne 'Completed'}">
	<fieldset>
		<legend>New To-Do</legend>
		
		<form action="${mvc.basePath}/todos" method="POST" enctype="application/x-www-form-urlencoded">
			<dl>
				<dt>Title</dt>
				<dd><input name="title" type="text"></dd>
			</dl>
			
			<input type="submit" value="Save"/>
		</form>
	</fieldset>
	</c:if>
</t:layout>