#
# Copyright 2024 Aklivity Inc
#
# Licensed under the Aklivity Community License (the "License"); you may not use
# this file except in compliance with the License.  You may obtain a copy of the
# License at
#
#   https://www.aklivity.io/aklivity-community-license/
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#

FROM eclipse-temurin:22-jdk AS build

WORKDIR /app

COPY target/service-udf-java-shaded.jar ./service-udf-java.jar

ENV JAVA_OPTS="--add-opens=java.base/java.nio=ALL-UNNAMED"

ENTRYPOINT java $JAVA_OPTS -cp "$CLASSPATH" io.aklivity.zillabase.udf.server.ZillabaseUdfServerMain
