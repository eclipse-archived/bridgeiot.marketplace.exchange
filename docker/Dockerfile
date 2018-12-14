# Copyright (c) 2016-2017 in alphabetical order:
# Atos IT Solutions and Services GmbH, National University of Ireland Galway, Siemens AG
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

FROM quay.io/atos_bigiot/dev_sbt_base:alpine

ADD sbt /usr/local/bin/sbt
RUN sed -i 's/\r//' /usr/local/bin/sbt && chmod +x /usr/local/bin/sbt
ADD run.sh /usr/local/bin/run.sh
RUN sed -i 's/\r//' /usr/local/bin/run.sh && chmod +x /usr/local/bin/run.sh

WORKDIR /prj/exchange

CMD [ "/usr/local/bin/run.sh" ]
