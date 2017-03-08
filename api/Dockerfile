FROM flowdocker/play:0.0.67

RUN apt-get install -y curl
RUN curl -sL https://deb.nodesource.com/setup_5.x | bash -
RUN apt-get install -y nodejs

ADD . /opt/play

WORKDIR /opt/play

RUN sbt 'project api' clean stage

WORKDIR api/target/universal/stage

ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "--service", "play", "delta", "bin/delta-api"]
