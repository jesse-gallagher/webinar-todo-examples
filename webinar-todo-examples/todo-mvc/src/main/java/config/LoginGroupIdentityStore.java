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

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

@ApplicationScoped
public class LoginGroupIdentityStore implements IdentityStore {

  @Override
  public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
    if(validationResult != null) {
      Principal principal = validationResult.getCallerPrincipal();
      if(principal != null && !"anonymous".equalsIgnoreCase(principal.getName())) {
        return Collections.singleton("users");
      }
    }
    return Collections.emptySet();
  }
  
  @Override
  public Set<ValidationType> validationTypes() {
    return Collections.singleton(ValidationType.PROVIDE_GROUPS);
  }

}
