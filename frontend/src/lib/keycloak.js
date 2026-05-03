import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'n11',
  clientId: 'n11-frontend',
});

export default keycloak;
