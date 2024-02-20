FROM jenkins/jenkins:lts-jdk17
RUN jenkins-plugin-cli --plugins \
configuration-as-code:1775.v810dc950b_514 \
pipeline-model-definition:2.2144.v077a_d1928a_40 \
kubernetes:4179.v3b_88431df708 \
kubernetes-cli:1.12.1 \
git:5.2.1 \
bitbucket:241.v6d24a_57f9359 \
active-directory:2.34 \
workflow-scm-step:415.v434365564324 \
matrix-auth:3.2.1 \
blueocean:1.27.10 \
email-ext:2.104 \
timestamper:1.26 \
kubernetes-credentials-provider:1.234.vf3013b_35f5b_a \
ssh-slaves:2.948.vb_8050d697fec \
pipeline-utility-steps:2.16.1 \
pipeline-aws:1.43 \
ws-cleanup:0.45 \
throttle-concurrents:2.14 \
uno-choice:2.8.1 \
purge-build-queue-plugin:88.v23b_97b_f2c7a_d \
ansicolor:1.0.4 \
pipeline-stage-view:2.34

## dark-theme:372.v79b_02c754b_29 