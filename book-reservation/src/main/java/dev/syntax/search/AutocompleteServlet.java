package dev.syntax.search;

import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/autocomplete")
public class AutocompleteServlet extends HttpServlet {

    private RestHighLevelClient client;

    @Override
    public void init() throws ServletException {
        client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String keyword = request.getParameter("keyword");

        // 1. Elasticsearch 쿼리 생성
        MatchQueryBuilder matchQuery = QueryBuilders
                .matchQuery("title", keyword)
                .analyzer("autocomplete_search"); // 검색용 analyzer 사용

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(matchQuery)
                .size(10) // 자동완성 최대 10개
                .sort("_score", SortOrder.DESC);

        SearchRequest searchRequest = new SearchRequest("dummy");
        searchRequest.source(sourceBuilder);

        // 2. Elasticsearch 검색 실행
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //  3. 결과 파싱
        List<String> suggestions = new ArrayList<>();
        searchResponse.getHits().forEach(hit -> {
            String title = (String) hit.getSourceAsMap().get("title");
            suggestions.add(title);
        });

        // 4 JSON 응답.
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(new Gson().toJson(suggestions));
    }

    @Override
    public void destroy() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
