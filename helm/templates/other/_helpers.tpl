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
{{- range $key, $val := .Values.config.podLabels -}}
{{ $key }}: {{ $val | quote }}
{{ end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "ubiquia.adminServiceAccountName" -}}
{{- if .Values.config.k8s.adminServiceAccount.create }}
{{- default (printf "%s-admin" (include "ubiquia.fullname" .) ) .Values.config.k8s.adminServiceAccount.name }}
{{- else }}
{{- default "default" .Values.adminServiceAccount.name }}
{{- end }}
{{- end }}

{{- define "ubiquia.core.commService.image" -}}
{{ .Values.components.core.commService.image.registry }}/{{ .Values.components.core.commService.image.repository }}:{{ .Values.components.core.commService.image.tag }}
{{- end }}

{{- define "ubiquia.core.flowService.image" -}}
{{ .Values.components.core.flowService.image.registry }}/{{ .Values.components.core.flowService.image.repository }}:{{ .Values.components.core.flowService.image.tag }}
{{- end }}
