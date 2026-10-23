// Snippet for service deploy Jenkinsfiles (after deploy stage):
//
// stage('Smoke Tests') {
//   steps {
//     build job: 'municipal-services/core-smoke-tests',
//       wait: true,
//       propagate: true,
//       parameters: [
//         string(name: 'ENVIRONMENT', value: env.DEPLOY_ENV),
//         string(name: 'SERVICE', value: 'pgr-services'), // this service
//         string(name: 'EXPECTED_VERSION', value: env.BUILD_VERSION ?: '')
//       ]
//   }
// }
//
// Pre-deploy CI (same repo build job):
//   sh 'mvn test'                          // unit (surefire)
//   sh 'mvn verify -DskipUnitTests'        // integration (failsafe) — per-service README
//   sh 'mvn verify -Powasp'                // optional dependency-check profile
//
// Scheduled (UAT/prod): trigger core-smoke-tests with SERVICE=ALL every 30-60 min.
