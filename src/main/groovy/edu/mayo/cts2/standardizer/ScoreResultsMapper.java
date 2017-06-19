package edu.mayo.cts2.standardizer;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import java.util.Iterator;

public class ScoreResultsMapper extends DefaultResultMapper {

    public ScoreResultsMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
        super(mappingContext);
    }

    @Override
    public <T> Page<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        Page<T> resultPage = super.mapResults(response, clazz, pageable);
        Iterator<T> it = resultPage.getContent().iterator();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                T next = it.next();
                if (next instanceof  Scoreable) {
                    ((Scoreable) next).setScore(hit.score());
                }
            }
        }
        return resultPage;
    }
}