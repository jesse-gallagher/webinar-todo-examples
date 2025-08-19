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
package controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.View;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("token")
@Controller
@RolesAllowed("users")
public class TokenController {
  @Inject
  private OpenIdContext context;
  
  @Inject
  private Models models;
  
  @GET
  @View("token.jsp")
  public void showToken() {
    models.put("token", context.getAccessToken());
  }

}
