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
package bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("oidc")
public class OidcConfig {
  private String domain;
  private String clientId;
  private String clientSecret;
  
  @PostConstruct
  public void load() {
    Properties props = new Properties();
    try(InputStream is = getClass().getResourceAsStream("/oidc.properties")) {
      props.load(is);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    
    this.domain = props.getProperty("domain");
    this.clientId = props.getProperty("clientId");
    this.clientSecret = props.getProperty("clientSecret");
  }
  
  public String getDomain() {
    return domain;
  }
  public String getClientId() {
    return clientId;
  }
  public String getClientSecret() {
    return clientSecret;
  }
}
