FROM jenkins/jenkins:lts-jdk11
RUN jenkins-plugin-cli --plugins \
pipeline-model-definition:2.2144.v077a_d1928a_40 \
kubernetes:3995.v227c16b_675ee \
git:5.2.0 maven-plugin:3.23 \
gradle:2.8.2 \
bitbucket:223.vd12f2bca5430 \
active-directory:2.31 \
workflow-scm-step:415.v434365564324 \
matrix-auth:3.1.10 \
ant:497.v94e7d9fffa_b_9 \
blueocean:1.27.6 \
email-ext:2.100

# dark-theme:359.vb_d6175e5f6f9
#configuration-as-code:1670.v564dc8b_982d0 \