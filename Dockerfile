FROM maven:3.8.1-jdk-8

WORKDIR /root

ADD src/ /root/src

ADD image/ /root/image
COPY test/ /root/test
ADD pom.xml/ /root


#RUN mkdir /test
EXPOSE 8000

ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.2.1/wait /wait
RUN chmod +x /wait

RUN mvn clean compile assembly:single
#RUN mvn clean package


CMD /wait && java -jar target/CSE312HW-1.0-SNAPSHOT-jar-with-dependencies.jar

