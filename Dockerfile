# Chainguard/Wolfi-basert dev-image: kilde for nb-locale (apk) og busybox (sh/printenv),
# siden minimal-varianten av jre-imaget verken har pakkehåndtering eller shell.
FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21-dev AS tools
USER root
RUN apk add --no-cache glibc-locale-nb

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY --from=tools /bin/busybox /bin/sh
COPY --from=tools /bin/busybox /bin/printenv
# busybox er dynamisk lenket mot libcrypt, som ikke finnes i minimal-imaget
COPY --from=tools /usr/lib/libcrypt.so.1.1.0 /usr/lib/libcrypt.so.1.1.0
COPY --from=tools /usr/lib/libcrypt.so.1 /usr/lib/libcrypt.so.1

# Copy locale files from the tools-stage
COPY --from=tools /usr/lib/locale/ /usr/lib/locale/

WORKDIR /app

COPY ./target/app.jar app.jar

EXPOSE 8080
ENV LANG=nb_NO.UTF-8 LANGUAGE='nb_NO:nb' LC_ALL=nb_NO.UTF-8 TZ="Europe/Oslo"
ENV SPRING_PROFILES_ACTIVE=nais

# Chainguard sitt jre-image har kun ["java"] som default entrypoint (ikke ["java","-jar"] som distroless)
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]