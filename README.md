## Get Repository Info and install Chart

```bash
helm repo add jenkins https://charts.jenkins.io
helm repo update
helm install [RELEASE_NAME] jenkins/jenkins [flags]
```

## Uninstall Chart

```bash
# Helm 3
$ helm uninstall [RELEASE_NAME]
```

This removes all the Kubernetes components associated with the chart and deletes the release.
## Upgrade Chart

```bash
# Helm 3
$ helm upgrade [RELEASE_NAME] jenkins/jenkins [flags]
```
## Configuration

```bash
# Helm 3
$ helm show values jenkins/jenkins
```

For a summary of all configurable options, see [VALUES_SUMMARY.md](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/VALUES_SUMMARY.md).

### Using a custom image

Notice: `installPlugins` is set to false to disable plugin download. In this case, the image `registry/my-jenkins:v1.2.3` must have the plugins specified as default value for [the `controller.installPlugins` directive](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/VALUES_SUMMARY.md#jenkins-plugins) to ensure that the configuration side-car system works as expected.

In case you are using a private registry you can use 'imagePullSecretName' to specify the name of the secret to use when pulling the image:

```yaml
controller:
  image: "registry/my-jenkins"
  tag: "v1.2.3"
  imagePullSecretName: registry-secret
  installPlugins: false
```

### Configuration as Code

Jenkins Configuration as Code (JCasC) is now a standard component in the Jenkins project.
To allow JCasC's configuration from the helm values, the plugin [`configuration-as-code`](https://plugins.jenkins.io/configuration-as-code/) must be installed in the Jenkins Controller's Docker image (which is the case by default as specified by the [default value of the directive `controller.installPlugins`](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/VALUES_SUMMARY.md#jenkins-plugins)).

JCasc configuration is passed through Helm values under the key `controller.JCasC`.
The section ["Jenkins Configuration as Code (JCasC)" of the page "VALUES_SUMMARY.md"](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/VALUES_SUMMARY.md#jenkins-configuration-as-code-jcasc) lists all the possible directives.

In particular, you may specify custom JCasC scripts by adding sub-key under the `controller.JCasC.configScripts` for each configuration area where each corresponds to a plugin or section of the UI.

The sub-keys (prior to `|` character) are only labels used to give the section a meaningful name.
The only restriction is they must conform to RFC 1123 definition of a DNS label, so they may only contain lowercase letters, numbers, and hyphens.

Each key will become the name of a configuration yaml file on the controller in `/var/jenkins_home/casc_configs` (by default) and will be processed by the Configuration as Code Plugin during Jenkins startup.

The lines after each `|` become the content of the configuration yaml file.

The first line after this is a JCasC root element, e.g. jenkins, credentials, etc.

Best reference is the Documentation link here: `https://<jenkins_url>/configuration-as-code`.

The example below sets custom systemMessage:

```yaml
controller:
  JCasC:
    configScripts:
      welcome-message: |
        jenkins:
          systemMessage: Welcome to HELL!!!
```

More complex example that creates ldap settings:

```yaml
controller:
  JCasC:
    configScripts:
      ldap-settings: |
        jenkins:
          securityRealm:
            ldap:
              configurations:
                - server: ldap.acme.com
                  rootDN: dc=acme,dc=uk
                  managerPasswordSecret: ${LDAP_PASSWORD}
                  groupMembershipStrategy:
                    fromUserRecord:
                      attributeName: "memberOf"
```


Further JCasC examples can be found [here](https://github.com/jenkinsci/configuration-as-code-plugin/tree/master/demos).

When installing, you provide all relevant yaml files (e.g `helm install -f values_main.yaml -f values_jenkins_casc.yaml -f values_jenkins_unclassified.yaml ...`).  Instead of updating the `CASC_JENKINS_CONFIG` environment variable to include multiple paths, multiple CasC yaml files will be created in the same path `var/jenkins_home/casc_configs`.

#### Config as Code With or Without Auto-Reload

Config as Code changes (to `controller.JCasC.configScripts`) can either force a new pod to be created and only be applied at next startup, or can be auto-reloaded on-the-fly.
If you set `controller.sidecars.configAutoReload.enabled` to `true`, a second, auxiliary container will be installed into the Jenkins controller pod, known as a "sidecar".
This watches for changes to configScripts, copies the content onto the Jenkins file-system and issues a POST to `http://<jenkins_url>/reload-configuration-as-code` with a pre-shared key.
You can monitor this sidecar's logs using command `kubectl logs <controller_pod> -c config-reload -f`.
If you want to enable auto-reload then you also need to configure rbac as the container which triggers the reload needs to watch the config maps:

```yaml
controller:
  sidecars:
    configAutoReload:
      enabled: true
rbac:
  create: true
```

### Mounting Volumes into Agent Pods

Your Jenkins Agents will run as pods, and it's possible to inject volumes where needed:

```yaml
agent:
  volumes:
  - type: Secret
    secretName: jenkins-mysecrets
    mountPath: /var/run/secrets/jenkins-mysecrets
```

The supported volume types are: `ConfigMap`, `EmptyDir`, `HostPath`, `Nfs`, `PVC`, `Secret`.
Each type supports a different set of configurable attributes, defined by [the corresponding Java class](https://github.com/jenkinsci/kubernetes-plugin/tree/master/src/main/java/org/csanchez/jenkins/plugins/kubernetes/volumes).

### Script approval list

`controller.scriptApproval` allows to pass function signatures that will be allowed in pipelines.
Example:

```yaml
controller:
  scriptApproval:
    - "method java.util.Base64$Decoder decode java.lang.String"
    - "new java.lang.String byte[]"
    - "staticMethod java.util.Base64 getDecoder"
```

#### Existing PersistentVolumeClaim

1. Create the PersistentVolume
2. Create the PersistentVolumeClaim
3. [Install](#install-chart) the chart, setting `persistence.existingClaim` to `PVC_NAME`


#### Storage Class

It is possible to define which storage class to use, by setting `persistence.storageClass` to `[customStorageClass]`.
If set to a dash (`-`), dynamic provisioning is disabled.
If the storage class is set to null or left undefined (`""`), the default provisioner is used (gp2 on AWS, standard on GKE, AWS & OpenStack).

### Additional Secrets

Additional secrets and Additional Existing Secrets,
can be mounted into the Jenkins controller through the chart or created using `controller.additionalSecrets` or `controller.additionalExistingSecrets`.  
A common use case might be identity provider credentials if using an external LDAP or OIDC-based identity provider.
The secret may then be referenced in JCasC configuration (see [JCasC configuration](#configuration-as-code)).

`values.yaml` controller section, referencing mounted secrets:
```yaml
controller:
  # the 'name' and 'keyName' are concatenated with a '-' in between, so for example:
  # an existing secret "secret-credentials" and a key inside it named "github-password" should be used in Jcasc as ${secret-credentials-github-password}
  # 'name' and 'keyName' must be lowercase RFC 1123 label must consist of lower case alphanumeric characters or '-',
  # and must start and end with an alphanumeric character (e.g. 'my-name',  or '123-abc')
  # existingSecret existing secret "secret-credentials" and a key inside it named "github-username" should be used in Jcasc as ${github-username}
  # When using existingSecret no need to specify the keyName under additionalExistingSecrets.
  existingSecret: secret-credentials
  
  additionalExistingSecrets:
    - name: secret-credentials
      keyName: github-username
    - name: secret-credentials
      keyName: github-password
    - name: secret-credentials
      keyName: token
  
  additionalSecrets:
    - name: client_id
      value: abc123
    - name: client_secret
      value: xyz999
  JCasC:
    securityRealm: |
      oic:
        clientId: ${client_id}
        clientSecret: ${client_secret}
        ...
    configScripts:
      jenkins-casc-configs: |
        credentials:
          system:
            domainCredentials:
            - credentials:
              - string:
                  description: "github access token"
                  id: "github_app_token"
                  scope: GLOBAL
                  secret: ${secret-credentials-token}
              - usernamePassword:
                  description: "github access username password"
                  id: "github_username_pass"
                  password: ${secret-credentials-github-password}
                  scope: GLOBAL
                  username: ${secret-credentials-github-username}
```

For more information, see [JCasC documentation](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/docs/features/secrets.adoc#kubernetes-secrets).

### Secret Claims from HashiCorp Vault

It's possible for this chart to generate `SecretClaim` resources in order to automatically create and maintain Kubernetes `Secrets` from HashiCorp [Vault](https://www.vaultproject.io/) via [`kube-vault-controller`](https://github.com/roboll/kube-vault-controller)

These `Secrets` can then be referenced in the same manner as Additional Secrets above.

This can be achieved by defining required Secret Claims within `controller.secretClaims`, as follows:
```yaml
controller:
  secretClaims:
    - name: jenkins-secret
      path: secret/path
    - name: jenkins-short-ttl
      path: secret/short-ttl-path
      renew: 60
```

### Adding Custom Pod Templates

It is possible to add custom pod templates for the default configured kubernetes cloud.
Add a key under `agent.podTemplates` for each pod template. Each key (prior to `|` character) is just a label, and can be any value.
Keys are only used to give the pod template a meaningful name.  The only restriction is they may only contain RFC 1123 \ DNS label characters: lowercase letters, numbers, and hyphens. Each pod template can contain multiple containers.
There's no need to add the _jnlp_ container since the kubernetes plugin will automatically inject it into the pod.
For this pod templates configuration to be loaded the following values must be set:

```yaml
controller.JCasC.defaultConfig: true
```
The example below creates a python pod template in the kubernetes cloud:

```yaml
agent:
  podTemplates:
    python: |
      - name: python
        label: jenkins-python
        serviceAccount: jenkins
        containers:
          - name: python
            image: python:3
            command: "/bin/sh -c"
            args: "cat"
            ttyEnabled: true
            privileged: true
            resourceRequestCpu: "400m"
            resourceRequestMemory: "512Mi"
            resourceLimitCpu: "1"
            resourceLimitMemory: "1024Mi"
```

Best reference is `https://<jenkins_url>/configuration-as-code/reference#Cloud-kubernetes`.

### Ingress Configuration

This chart provides ingress resources configurable via the `controller.ingress` block.
The simplest configuration looks like the following:

```yaml
controller:
   ingress:
       enabled: true
       apiVersion: "extensions/v1beta1"
       hostName: "jenkins.internal.example.com"
       annotations:
           kubernetes.io/ingress.class: "internal"
```

## Prometheus Metrics

If you want to expose Prometheus metrics you need to install the [Jenkins Prometheus Metrics Plugin](https://github.com/jenkinsci/prometheus-plugin).
It will expose an endpoint (default `/prometheus`) with metrics where a Prometheus Server can scrape.

If you have implemented [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator), you can set `master.prometheus.enabled` to `true` to configure a `ServiceMonitor` and `PrometheusRule`.
If you want to further adjust alerting rules you can do so by configuring `master.prometheus.alertingrules`

If you have implemented Prometheus without using the operator, you can leave `master.prometheus.enabled` set to `false`.

### Running Behind a Forward Proxy

The controller pod uses an Init Container to install plugins etc. If you are behind a corporate proxy it may be useful to set `controller.initContainerEnv` to add environment variables such as `http_proxy`, so that these can be downloaded.

Additionally, you may want to add env vars for the init container, the Jenkins container, and the JVM (`controller.javaOpts`):

```yaml
controller:
  initContainerEnv:
    - name: http_proxy
      value: "http://192.168.64.1:3128"
    - name: https_proxy
      value: "http://192.168.64.1:3128"
    - name: no_proxy
      value: ""
    - name: JAVA_OPTS
      value: "-Dhttps.proxyHost=proxy_host_name_without_protocol -Dhttps.proxyPort=3128"
  containerEnv:
    - name: http_proxy
      value: "http://192.168.64.1:3128"
    - name: https_proxy
      value: "http://192.168.64.1:3128"
  javaOpts: >-
    -Dhttp.proxyHost=192.168.64.1
    -Dhttp.proxyPort=3128
    -Dhttps.proxyHost=192.168.64.1
    -Dhttps.proxyPort=3128
```

### HTTPS Keystore Configuration

[This configuration](https://wiki.jenkins.io/pages/viewpage.action?pageId=135468777) enables jenkins to use keystore in order to serve https.
Here is the [value file section](https://wiki.jenkins.io/pages/viewpage.action?pageId=135468777#RunningJenkinswithnativeSSL/HTTPS-ConfigureJenkinstouseHTTPSandtheJKSkeystore) related to keystore configuration.
Keystore itself should be placed in front of `jenkinsKeyStoreBase64Encoded` key and in base64 encoded format. To achieve that after having `keystore.jks` file simply do this: `cat keystore.jks | base64` and paste the output in front of `jenkinsKeyStoreBase64Encoded`.
After enabling `httpsKeyStore.enable` make sure that `httpPort` and `targetPort` are not the same, as `targetPort` will serve https.
Do not set `controller.httpsKeyStore.httpPort` to `-1` because it will cause readiness and liveliness prob to fail.
If you already have a kubernetes secret that has keystore and its password you can specify its' name in front of `jenkinsHttpsJksSecretName`, You need to remember that your secret should have proper data key names `jenkins-jks-file` and `https-jks-password`. Example:

```yaml
controller:
   httpsKeyStore:
       enable: true
       jenkinsHttpsJksSecretName: ''
       httpPort: 8081
       path: "/var/jenkins_keystore"
       fileName: "keystore.jks"
       password: "changeit"
       jenkinsKeyStoreBase64Encoded: ''
```
### AWS Security Group Policies

To create SecurityGroupPolicies set `awsSecurityGroupPolicies.enabled` to true and add your policies. Each policy requires a `name`, array of `securityGroupIds` and a `podSelector`. Example:

```yaml
awsSecurityGroupPolicies:
  enabled: true
  policies:
    - name: "jenkins-controller"
      securityGroupIds: 
        - sg-123456789
      podSelector:
        matchExpressions:
          - key: app.kubernetes.io/component
            operator: In
            values:
              - jenkins-controller
```