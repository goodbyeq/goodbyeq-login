FROM 634957622823.dkr.ecr.ap-south-1.amazonaws.com/tomcat8:5

USER root

COPY goodbyeq-login-1.0.0.war  /usr/share/tomcat8/webapps/goodbyeq-login.war

COPY catalina_base/bin/setenv.sh /usr/share/tomcat8/bin/
RUN /usr/share/tomcat8/bin/setenv.sh

RUN mkdir /staging
COPY config-bootstrap.sh /staging
RUN chmod 755 /staging && \
    chmod 755 /staging/config-bootstrap.sh

CMD [ "/usr/local/bin/dumb-init", "/staging/config-bootstrap.sh" ]
