package edu.mayo.cts2.standardizer
import edu.mayo.cts2.framework.core.json.JsonConverter
import edu.mayo.cts2.framework.model.valuesetdefinition.IteratableResolvedValueSet
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.SearchQuery
import org.springframework.stereotype.Component

import java.util.concurrent.Executors

@Component
class StandardizerService implements DisposableBean, InitializingBean {

    final UMLS_ENTITY_BASE_URI = { sab -> "http://umls.nlm.nih.gov/sab/${sab}/" }

    @Value('${elasticSearchHost:localhost}')
    String elasticSearchHost

    @Value('${elasticSearchPort:9300}')
    int elasticSearchPort

    @Value('${elasticSearchClusterName:elasticsearch}')
    String elasticSearchClusterName

    def threadPool = Executors.newFixedThreadPool(4)

    def jsonConverter = new JsonConverter()

    ElasticsearchTemplate client

    StandardizerService() {
        //
    }

    StandardizerService(elasticSearchHost, elasticSearchPort, elasticSearchClusterName) {
        this.client = this.buildClient(elasticSearchHost, elasticSearchPort, elasticSearchClusterName)
    }

    def buildClient(elasticSearchHost, elasticSearchPort, elasticSearchClusterName) throws Exception {
        Client c = new TransportClient.Builder().settings(new Settings.Builder().put('cluster.name', (String) elasticSearchClusterName).build()).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(elasticSearchHost), elasticSearchPort))

        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
        ScoreResultsMapper mapper = new ScoreResultsMapper(converter.getMappingContext())

        new ElasticsearchTemplate(c, converter, mapper)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        this.client = this.buildClient(elasticSearchHost, elasticSearchPort, elasticSearchClusterName)
    }

    @Override
    void destroy() throws Exception {
        threadPool.shutdown()
    }

    def map(valueSetDefinitionHref) {
        IteratableResolvedValueSet vsd = jsonConverter.fromJson(
                        getJson(valueSetDefinitionHref + "/resolution"))

        def list = []

        vsd.entry.each { entry ->
            list.add(['from':entry, 'result':this.query(entry.designation)])
        }

        def byCodesystem = [:]
        list.each {
            it.result.each { k, v ->
                if(! byCodesystem.containsKey(k)) {
                    byCodesystem.put(k, [])
                }

                byCodesystem.get(k).add(['from': it.from, 'result': findHighestRanked(v)])
            }
        }

        def stats = [:]
        byCodesystem.each { k, v ->
            def avg = v.inject(0) {total, i -> total + i.result.score} / vsd.entry.size()

            stats.put(k, ['avg': avg, 'missing': vsd.entry.size() - v.size()])
        }

        ['stats': stats, 'results': byCodesystem, "vsd": vsd.entry]
    }

    def findHighestRanked(entityDescriptions) {
        def highest = null
        entityDescriptions.each {
            if(highest == null || highest.score < it.score) {
                highest = it
            }
        }

        highest
    }

    def query(text) {
        text = text.toLowerCase()

        SearchQuery query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchQuery("designations", text)).withPageable(new PageRequest(0, 50)).build()

        def map = [:]
        client.queryForList(query, EntityDescription.class).each {
            def namespace = it.namespace
            if(! map.containsKey(namespace)) {
                map.put(namespace, [])
            }

            map.get(namespace).add(it)
        }

        map
    }

    def getJson(href) {
        href.replace(" ", "%20").toURL().
                getText(requestProperties: [Accept: 'application/json'])
    }

}
