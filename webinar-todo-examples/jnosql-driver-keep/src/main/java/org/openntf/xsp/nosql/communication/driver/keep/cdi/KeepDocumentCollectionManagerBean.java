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
package org.openntf.xsp.nosql.communication.driver.keep.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import org.eclipse.jnosql.mapping.DatabaseQualifier;
import org.openntf.xsp.jakarta.nosql.communication.driver.DominoDocumentManager;
import org.openntf.xsp.nosql.communication.driver.keep.impl.KeepDocumentCollectionManager;
import org.openntf.xsp.nosql.communication.driver.keep.impl.KeepDocumentConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class KeepDocumentCollectionManagerBean implements Bean<DominoDocumentManager> {
  private final String provider;
  
  public KeepDocumentCollectionManagerBean(String provider) {
    this.provider = provider;
  }

  @Override
  public DominoDocumentManager create(CreationalContext<DominoDocumentManager> creationalContext) {
    KeepDocumentConfiguration baseConfig = new KeepDocumentConfiguration();
    return baseConfig.get().apply(provider);
  }

  @Override
  public void destroy(DominoDocumentManager instance,
      CreationalContext<DominoDocumentManager> creationalContext) {
    
  }

  @Override
  public Set<Type> getTypes() {
    return Set.of(DominoDocumentManager.class);
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return Set.of(DatabaseQualifier.ofDocument(provider));
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return ApplicationScoped.class;
  }

  @Override
  public String getName() {
    return getClass().getName();
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Set.of();
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public Class<?> getBeanClass() {
    return KeepDocumentCollectionManager.class;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Set.of();
  }

  @Override
  public boolean isNullable() {
    return false;
  }

}
