FROM intersystems/iris-community:2026.2@sha256:d4331089a4d19aafa867c26b343eb2b8486cf112ef1522132761e6327691377e
USER root
ARG INSTALL_SENTENCE_TRANSFORMERS=false
RUN mkdir -p /durable && chown irisowner:irisowner /durable
# The pinned 2026.2 image ships CSPpwd under csp/bin, while its password
# initializer resolves bin/CSPpwd. Preserve initialization with the real binary.
RUN test -x /usr/irissys/csp/bin/CSPpwd \
    && ln -s /usr/irissys/csp/bin/CSPpwd /usr/irissys/bin/CSPpwd
RUN if [ "$INSTALL_SENTENCE_TRANSFORMERS" = "true" ]; then \
      python3 -m pip install --no-cache-dir --target /usr/irissys/mgr/python sentence-transformers \
      && chown -R irisowner:irisowner /usr/irissys/mgr/python; \
    fi
USER irisowner
COPY --chown=irisowner:irisowner src /opt/multi-manager/src
COPY --chown=irisowner:irisowner iris.script /opt/multi-manager/iris.script
COPY --chown=irisowner:irisowner --chmod=755 start-demo.sh /opt/multi-manager/start-demo.sh
RUN iris start IRIS \
    && iris session IRIS < /opt/multi-manager/iris.script \
    && iris stop IRIS quietly
