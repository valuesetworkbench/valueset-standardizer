FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp
ADD standardizer.jar app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -DelasticSearchHost=$elasticSearchHost -jar /app.jar"]