import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: '/auth',
  realm: 'n11',
  clientId: 'n11-frontend',
});

export default keycloak;
