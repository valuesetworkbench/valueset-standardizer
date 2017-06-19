import com.google.code.externalsorting.ExternalSort
import edu.mayo.cts2.standardizer.EntityDescription
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.query.IndexQuery

MRCONSO = "MRCONSO.RRF"

SORTED_MRCONSO = "SORTED_MRCONSO.RRF"

path = args[0]
HOST = args[1]

def index() {
    Client c = new TransportClient.Builder().settings(new Settings.Builder().put('cluster.name', 'elasticsearch').build()).build()
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), 9301))
    def client = new ElasticsearchTemplate(c)
    client.deleteIndex(EntityDescription.class)
    client.createIndex(EntityDescription.class)
    client.putMapping(EntityDescription.class)

    def inputFile = new File((String) path, SORTED_MRCONSO)

    def docs = []

    def currentCode = ""
    def currentSab = ""
    def currentDesignations = []

    def total = 0;

    inputFile.eachLine { it ->
        def line = it.split('\\|')

        def code = line[13]
        def sab = line[11]
        def designation = line[14]

        if(! (code + sab).equals(currentCode + currentSab)) {

            if(total > 0 && total % 50000 == 0) {
                println "Batching SAB Entities $total"

                if(docs.size() > 0) {
                    client.bulkIndex(docs)
                }

                docs.clear()
            }

            def e = new EntityDescription(name: currentCode, namespace: currentSab, designations: currentDesignations)
            docs.add(new IndexQuery(object: e))
            total++

            currentCode = code
            currentSab = sab
            currentDesignations = []
        }

        currentDesignations += designation
    }

    if(docs.size() > 0) {
        client.bulkIndex(docs)
    }

    println "DONE Loading $total SAB Entities."
}

def comparator = { s1, s2 ->
    def l1 = s1.split('\\|')
    def l2 = s2.split('\\|')

    def code1 = l1[13]
    def sab1 = l1[11]

    def code2 = l2[13]
    def sab2 = l2[11]

    def result = (code1 + sab1) <=> (code2 + sab2)

    result
}

def sortedMrconsoFile = new File((String) path, SORTED_MRCONSO)

if(! sortedMrconsoFile.exists()) {
    ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File((String) args[0], MRCONSO), comparator), sortedMrconsoFile)
}

index()