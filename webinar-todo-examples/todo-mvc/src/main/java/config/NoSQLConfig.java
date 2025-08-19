/**
 * Copyright © 2025 Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package config;

import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.xsp.nosql.communication.driver.keep.AccessTokenSupplier;
import org.openntf.xsp.nosql.communication.driver.keep.BaseUriSupplier;
import org.openntf.xsp.nosql.communication.driver.keep.DataSourceSupplier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

@ApplicationScoped
public class NoSQLConfig {
  @Inject
  @ConfigProperty(name="keep.apiName")
  private String apiName;
  
  @Inject
  @ConfigProperty(name="keep.baseUri")
  private URI baseUri;
  
  @Inject
  OpenIdContext context;
  
  @Produces
  public BaseUriSupplier getBaseUri() {
    return () -> baseUri;
  }
  
  @Produces
  public DataSourceSupplier getDataSource() {
    return () -> apiName;
  }
  
  @Produces
  @Dependent
  public AccessTokenSupplier getAccessToken() {
    return () -> context.getAccessToken().getToken();
  }
}
