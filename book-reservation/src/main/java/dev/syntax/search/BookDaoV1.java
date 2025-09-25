package dev.syntax.search;

import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDaoV1 {
    // 제목, 저자, ISBN으로 도서 검색 + 카테고리 이름 가져오기
    public List<Book> searchBooks(String keyword, String criteria) {
        List<Book> books = new ArrayList<>();

        // 1) BOOLEAN MODE + 접두사 검색으로 수정
        String sql = "SELECT b.*, bc.catName, " +
                "MATCH(b.title, b.author, b.isbn) AGAINST (? IN BOOLEAN MODE) AS score " +
                "FROM dummy_new b " +
                "JOIN book_category bc ON b.catCode = bc.catCode " +
                "WHERE MATCH(b.title, b.author, b.isbn) AGAINST (? IN BOOLEAN MODE) " +
                "ORDER BY score DESC";


        System.out.println("Executing SQL: " + sql);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 2) BOOLEAN MODE는 * 붙여야 부분 매칭 가능
            String searchKeyword = keyword + "*";
            pstmt.setString(1, searchKeyword);
            pstmt.setString(2, searchKeyword);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book();
                    book.setBookId(rs.getLong("bookId"));
                    book.setTitle(rs.getString("title"));
                    book.setAuthor(rs.getString("author"));
                    book.setTranslator(rs.getString("translator"));
                    book.setPubDate(rs.getDate("pubDate"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setPage(rs.getInt("page"));
                    book.setImage(rs.getString("image"));
                    book.setCatCode(rs.getLong("catCode"));
                    book.setBorrow(rs.getBoolean("isBorrow"));
                    book.setCatName(rs.getString("catName"));
                    books.add(book);
                }
            }

            // 3) fallback: 결과 없으면 LIKE 검색
            if (books.isEmpty()) {
                String likeSql = "SELECT b.*, bc.catName " +
                        "FROM book b " +
                        "JOIN book_category bc ON b.catCode = bc.catCode " +
                        "WHERE b.title LIKE CONCAT('%', ?, '%')";

                try (PreparedStatement likePstmt = conn.prepareStatement(likeSql)) {
                    likePstmt.setString(1, keyword);

                    try (ResultSet rs = likePstmt.executeQuery()) {
                        while (rs.next()) {
                            Book book = new Book();
                            book.setBookId(rs.getLong("bookId"));
                            book.setTitle(rs.getString("title"));
                            book.setAuthor(rs.getString("author"));
                            book.setTranslator(rs.getString("translator"));
                            book.setPubDate(rs.getDate("pubDate"));
                            book.setIsbn(rs.getString("isbn"));
                            book.setPage(rs.getInt("page"));
                            book.setImage(rs.getString("image"));
                            book.setCatCode(rs.getLong("catCode"));
                            book.setBorrow(rs.getBoolean("isBorrow"));
                            book.setCatName(rs.getString("catName"));
                            books.add(book);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return books;
    }
}
