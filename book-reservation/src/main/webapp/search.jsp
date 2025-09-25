<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="dev.syntax.search.Book" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>도서 검색</title>
    <style>
        table { width:100%; border-collapse:collapse; margin-top:20px; }
        th, td { border:1px solid #999; padding:8px; text-align:left; }
        th { background:#eee; }
        img { max-width:50px; }
        .muted { color:#888; font-size:0.9em; }
        .actions form { display:inline; margin:0; }
        .actions button[disabled] { opacity:.5; cursor:not-allowed; }
    </style>
</head>

<body>
<%
    String ok = request.getParameter("ok");
    String reason = request.getParameter("reason");
    if ("1".equals(ok)) {
%>
<p style="color:green;">대출이 완료되었습니다.</p>
<%
} else if ("0".equals(ok)) {
    if ("dup".equals(reason)) {
%>
<p style="color:red;">이미 대출 중인 도서입니다.</p>
<%
} else {
%>
<p style="color:red;">대출에 실패했습니다.</p>
<%
        }
    }
%>

<h2>도서 검색</h2>

<%
    Integer loginUserId = (Integer) session.getAttribute("userId");
    String keywordParam = request.getParameter("keyword");
%>

<form action="<%= request.getContextPath()%>/search" method="get">
    <select name="criteria" id="criteria">
        <option value="title"  <%= "title".equals(request.getParameter("criteria"))  ? "selected" : "" %>>제목</option>
        <option value="author" <%= "author".equals(request.getParameter("criteria")) ? "selected" : "" %>>저자</option>
        <option value="isbn"   <%= "isbn".equals(request.getParameter("criteria"))   ? "selected" : "" %>>ISBN</option>
    </select>

    <!-- ✅ datalist 연결된 검색창 -->
    <input type="text" name="keyword" id="keyword"
           value="<%= keywordParam != null ? keywordParam : "" %>"
           placeholder="검색어 입력" list="suggestions">
    <datalist id="suggestions"></datalist>

    <input type="submit" value="검색">
    <a href='MypageServlet'>마이페이지로 이동</a>
</form>

<%
    // ✅ keyword가 있는 경우만 결과 출력
    if (keywordParam != null && !keywordParam.trim().isEmpty()) {
        List<Book> books = (List<Book>) request.getAttribute("bookList");
        if (books != null && !books.isEmpty()) {
%>
<h3>검색 결과 (<%= books.size() %>건)</h3>

<% if (loginUserId == null) { %>
<p class="muted">※ 대출하려면 먼저 로그인하세요.</p>
<% } %>

<table>
    <thead>
    <tr>
        <th>제목</th>
        <th>분류</th>
        <th>저자</th>
        <th>옮긴이</th>
        <th>출판일</th>
        <th>ISBN</th>
        <th>페이지</th>
        <th>대출 여부</th>
        <th>대출</th>
    </tr>
    </thead>
    <tbody>
    <%
        for (Book book : books) {
            boolean borrowed = book.isBorrow();
    %>
    <tr>
        <td><%= book.getTitle() %></td>
        <td><%= book.getCatName() %></td>
        <td><%= book.getAuthor() %></td>
        <td><%= book.getTranslator() %></td>
        <td><%= book.getPubDate() %></td>
        <td><%= book.getIsbn() %></td>
        <td><%= book.getPage() %></td>
        <td><%= borrowed ? "대출중" : "대출 가능" %></td>
        <td class="actions">
            <form action="<%= request.getContextPath() %>/borrow" method="post">
                <input type="hidden" name="bookId" value="<%= book.getBookId() %>">
                <input type="hidden" name="userId" value="<%= loginUserId != null ? loginUserId : 0 %>">
                <button type="submit"
                        <%= (loginUserId == null || borrowed) ? "disabled" : "" %>>
                    대출하기
                </button>
            </form>
        </td>
    </tr>
    <%
        }
    %>
    </tbody>
</table>
<%
} else {
%>
<p>검색 결과가 없습니다.</p>
<%
        }
    }
%>

<script>
    document.addEventListener("DOMContentLoaded", () => {
        const input = document.getElementById("keyword");
        const datalist = document.getElementById("suggestions");

        input.addEventListener("input", async () => {
            const keyword = input.value.trim();
            if (!keyword) {
                datalist.innerHTML = "";
                return;
            }

            try {
                const res = await fetch("<%= request.getContextPath() %>/autocomplete?criteria=title&keyword=" + encodeURIComponent(keyword));
                if (!res.ok) return;
                const suggestions = await res.json();

                datalist.innerHTML = "";
                suggestions.forEach(s => {
                    const option = document.createElement("option");
                    option.value = typeof s === "string" ? s : s.title; // 문자열 리스트 대응
                    datalist.appendChild(option);
                });
            } catch (e) {
                console.error("자동완성 요청 실패", e);
            }
        });
    });
</script>

</body>
</html>
