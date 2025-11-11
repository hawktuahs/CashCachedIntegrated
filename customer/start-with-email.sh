#!/bin/bash

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_MAIL_HOST=smtp.gmail.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=devon08yad@gmail.com
export SPRING_MAIL_PASSWORD=ikvybrisdoubcpdo
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
export APP_MAIL_DEV_MODE=false
export APP_MAIL_FROM=devon08yad@gmail.com

./mvnw spring-boot:run
