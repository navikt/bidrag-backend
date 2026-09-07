# Chainguard/Wolfi-basert dev-image: kilde for busybox (sh/printenv),
# siden minimal-varianten av jre-imaget verken har pakkehåndtering eller shell.
FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21-dev AS tools

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY --from=tools /bin/busybox /bin/sh
COPY --from=tools /bin/busybox /bin/printenv
# busybox er dynamisk lenket mot libcrypt, som ikke finnes i minimal-imaget
COPY --from=tools /usr/lib/libcrypt.so.1.1.0 /usr/lib/libcrypt.so.1.1.0
COPY --from=tools /usr/lib/libcrypt.so.1 /usr/lib/libcrypt.so.1

WORKDIR /app

COPY ./target/app.jar app.jar

EXPOSE 8080
ARG JDK_JAVA_OPTIONS
ENV TZ="Europe/Oslo"
ENV SPRING_PROFILES_ACTIVE=nais

ENV JDK_JAVA_OPTIONS="-Dhttp.proxyHost=webproxy.nais \
                -Dhttps.proxyHost=webproxy.nais \
                -Dhttp.proxyPort=8088 \
                -Dhttps.proxyPort=8088 \
                -Dhttp.nonProxyHosts=localhost|127.0.0.1|10.254.0.1|*.local|*.adeo.no|*.nav.no|*.aetat.no|*.devillo.no|*.oera.no|*.nais.io|*.aivencloud.com|*.intern.dev.nav.no|*.nais-system"
# Chainguard sitt jre-image har kun ["java"] som default entrypoint (ikke ["java","-jar"] som distroless), og imaget kjører allerede som nonroot (uid 65532) på samme måte som distroless :nonroot-varianten.
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]