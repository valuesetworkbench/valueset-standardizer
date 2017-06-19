package edu.mayo.cts2.standardizer

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldIndex
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "standardizer", type = "EntityDescription")
public class EntityDescription implements Scoreable {

    @Id
    String id

    double score

    @Field(type = FieldType.String, store = true, index = FieldIndex.not_analyzed)
    String name

    @Field(type = FieldType.String, store = true, index = FieldIndex.not_analyzed)
    String namespace;

    @Field(type = FieldType.String, store = true)
    List<String> designations = [];

}
