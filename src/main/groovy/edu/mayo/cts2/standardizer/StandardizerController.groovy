package edu.mayo.cts2.standardizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class StandardizerController {

    @Autowired
    StandardizerService standardizerService

    @RequestMapping(value = "/standard", method = [RequestMethod.GET, RequestMethod.POST])
    def map(String valueSetDefinitionHref) {
        standardizerService.map(valueSetDefinitionHref)
    }

}
