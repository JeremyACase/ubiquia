{{/*
Expand the name of the chart.
*/}}
{{- define "ubiquia.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ubiquia.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ubiquia.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ubiquia.labels" -}}
helm.sh/chart: {{ include "ubiquia.chart" . }}
{{ include "ubiquia.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "ubiquia.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ubiquia.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Pod labels
*/}}
{{- define "ubiquia.podLabels" -}}
{{- range $key, $val := .Values.config.podLabels }}
{{ $key }}: {{ $val | quote }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ubiquia.serviceAccountName" -}}
{{- if .Values.k8s.serviceAccount.create }}
{{- default (include "ubiquia.fullname" .) .Values.k8s.serviceAccount.name }}
{{- else }}
{{- default "ubiquia" }}
{{- end }}
{{- end }}

{{- define "ubiquia.core.beliefStateGeneratorService.image" -}}
{{ .Values.ubiquia.components.core.beliefStateGeneratorService.image.registry }}/{{ .Values.ubiquia.components.core.beliefStateGeneratorService.image.repository }}:{{ .Values.ubiquia.components.core.beliefStateGeneratorService.image.tag }}
{{- end }}

{{- define "ubiquia.core.communicationService.image" -}}
{{ .Values.ubiquia.components.core.communicationService.image.registry }}/{{ .Values.ubiquia.components.core.communicationService.image.repository }}:{{ .Values.ubiquia.components.core.communicationService.image.tag }}
{{- end }}

{{- define "ubiquia.core.flowService.image" -}}
{{ .Values.ubiquia.components.core.flowService.image.registry }}/{{ .Values.ubiquia.components.core.flowService.image.repository }}:{{ .Values.ubiquia.components.core.flowService.image.tag }}
{{- end }}

{{- define "ubiquia.test.beliefStateGeneratorService.image" -}}
{{ .Values.ubiquia.components.test.beliefStateGeneratorService.image.registry }}/{{ .Values.ubiquia.components.test.beliefStateGeneratorService.image.repository }}:{{ .Values.ubiquia.components.test.beliefStateGeneratorService.image.tag }}
{{- end }}

{{- define "ubiquia.test.flowService.image" -}}
{{ .Values.ubiquia.components.test.flowService.image.registry }}/{{ .Values.ubiquia.components.test.flowService.image.repository }}:{{ .Values.ubiquia.components.test.flowService.image.tag }}
{{- end }}

{{- define "ubiquia.util.simulationService.image" -}}
{{ .Values.ubiquia.components.util.simulationService.image.registry }}/{{ .Values.ubiquia.components.util.simulationService.image.repository }}:{{ .Values.ubiquia.components.util.simulationService.image.tag }}
{{- end }}

{{- define "ubiquia.util.ubiquiaDashboard.image" -}}
{{ .Values.ubiquia.components.util.ubiquiaDashboard.image.registry }}/{{ .Values.ubiquia.components.util.ubiquiaDashboard.image.repository }}:{{ .Values.ubiquia.components.util.ubiquiaDashboard.image.tag }}
{{- end }}

{{/*
Ubiquia Dashboard — access via the Communication Service
---------------------------------------------------------
The dashboard has no NodePort of its own. It is proxied exclusively through
the Communication Service, which is exposed at NodePort 30080.

When ubiquia.components.util.ubiquiaDashboard.enabled is true, open:

  http://<node-ip>:30080/ubiquia/core/communication-service/dashboard/

To find <node-ip> in a local KinD cluster:
  kubectl get nodes -o wide
  # use the INTERNAL-IP of any node, or 'localhost' if using kind with port-forwarding

To check whether the dashboard is enabled in the current release:
  helm get values <release-name> -n <namespace> | grep -A2 ubiquiaDashboard

Disabled by default for featherweight profiles (no UI overhead on edge nodes).
*/}}