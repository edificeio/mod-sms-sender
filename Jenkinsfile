#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh clean init install publish'
        }
      }
    }
}

