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
package org.openntf.xsp.nosql.communication.driver.keep.impl;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openntf.xsp.jakarta.nosql.communication.driver.DominoConstants;
import org.openntf.xsp.jakarta.nosql.communication.driver.ViewInfo;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.AbstractDominoDocumentCollectionManager;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.DQL;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.DQL.DQLTerm;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.EntityUtil;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.QueryConverter;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.QueryConverter.QueryConverterResult;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.ViewInfoImpl;
import org.openntf.xsp.jakarta.nosql.mapping.extension.AccessRights;
import org.openntf.xsp.jakarta.nosql.mapping.extension.DominoRepository.CalendarModScope;
import org.openntf.xsp.jakarta.nosql.mapping.extension.ViewQuery;
import org.openntf.xsp.nosql.communication.driver.keep.AccessTokenSupplier;
import org.openntf.xsp.nosql.communication.driver.keep.BaseUriSupplier;
import org.openntf.xsp.nosql.communication.driver.keep.DataSourceSupplier;
import org.openntf.xsp.nosql.communication.driver.keep.client.api.ApiException;
import org.openntf.xsp.nosql.communication.driver.keep.client.api.DataApi;
import org.openntf.xsp.nosql.communication.driver.keep.client.model.QueryRequest;
import org.openntf.xsp.nosql.communication.driver.keep.client.model.RichTextRepresentation;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

public class KeepDocumentCollectionManager extends AbstractDominoDocumentCollectionManager {

  private final BaseUriSupplier baseUriSupplier;
  private final DataSourceSupplier dataSourceSupplier;
  private final AccessTokenSupplier tokenSupplier;
  private final KeepEntityConverter entityConverter;

  public KeepDocumentCollectionManager(BaseUriSupplier baseUriSupplier,
      DataSourceSupplier dataSourceSupplier, AccessTokenSupplier tokenSupplier) {
    this.baseUriSupplier = baseUriSupplier;
    this.dataSourceSupplier = dataSourceSupplier;
    this.tokenSupplier = tokenSupplier;
    this.entityConverter = new KeepEntityConverter();
  }

  @Override
  public Stream<CommunicationEntity> viewEntryQuery(String entityName, String viewName,
      PageRequest pagination, Sort sorts,
      int maxLevel, boolean docsOnly, ViewQuery viewQuery, boolean singleResult) {
    return viewQuery(entityName, viewName, pagination, sorts, maxLevel, docsOnly, viewQuery, singleResult, false);
  }
  
  @Override
  public Stream<CommunicationEntity> viewDocumentQuery(String entityName, String viewName,
      PageRequest pagination, Sort sorts, int maxLevel, ViewQuery viewQuery, boolean singleResult,
      boolean distinct) {
    return viewQuery(entityName, viewName, pagination, sorts, maxLevel, true, viewQuery, singleResult, true)
        // TODO remove filter when this can be done in Keep
        .filter(distinctByUnid());
  }

  @Override
  public void putInFolder(String entityId, String folderName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeFromFolder(String entityId, String folderName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommunicationEntity insert(CommunicationEntity entity, boolean computeWithForm) {
    EntityMetadata mapping = EntityUtil.getClassMapping(entity.name());
    try(DataApi api = getDataApi()) {
      Map<String, Object> doc = entityConverter.convertNoSQLEntity(entity, true, mapping);
      doc.remove("@meta");
      doc = api.createDocument(dataSourceSupplier.get(), doc, RichTextRepresentation.HTML, null);
      String id = entityConverter.getUnid(doc);
      entity.add(Element.of(DominoConstants.FIELD_ID, id));
      return entity;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CommunicationEntity update(CommunicationEntity entity, boolean computeWithForm) {
    Optional<Element> maybeId = entity.find(DominoConstants.FIELD_ID);
    if (!maybeId.isPresent()) {
      // Then consider it an insert
      return insert(entity, computeWithForm);
    } else {
      EntityMetadata mapping = EntityUtil.getClassMapping(entity.name());
      try(DataApi api = getDataApi()) {
        Map<String, Object> doc = entityConverter.convertNoSQLEntity(entity, true, mapping);
        api.updateDocument(dataSourceSupplier.get(), maybeId.get().get(String.class), doc,
            "default", RichTextRepresentation.HTML, null, null);
        return entity;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public boolean existsById(String unid) {
    try(DataApi api = getDataApi()) {
      Map<String, Object> doc = api.getDocument(unid, dataSourceSupplier.get(), "default", //$NON-NLS-1$
          false, null, RichTextRepresentation.HTML);
      return doc != null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<CommunicationEntity> getByNoteId(String entityName, String noteId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<CommunicationEntity> getById(String entityName, String id) {
    EntityMetadata mapping = EntityUtil.getClassMapping(entityName);
    try(DataApi api = getDataApi()) {
      Map<String, Object> doc = api.getDocument(id, dataSourceSupplier.get(), "default", //$NON-NLS-1$
          false, null, RichTextRepresentation.HTML);
      return entityConverter.convertDocuments(entityName, Arrays.asList(doc), mapping)
          .findFirst();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(entity -> insert(entity, false))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void delete(DeleteQuery query) {
    try(DataApi api = getDataApi()) {
      Collection<String> unids = query.columns();
      if (unids != null) {
        unids = unids.stream()
            .filter(unid -> unid != null && !unid.isEmpty())
            .collect(Collectors.toSet());
      }
      if (unids == null || unids.isEmpty()) {
        // Then query for docs
        String entityName = query.name();
        SelectQuery docQuery = SelectQuery.builder()
            .from(entityName)
            .where(query.condition().get())
            .build();

        QueryConverterResult queryResult = QueryConverter.select(docQuery, EntityUtil.getClassMapping(entityName));
        QueryRequest req = new QueryRequest();
        req.setForms(Arrays.asList(entityName));
        req.setQuery(queryResult.getStatement().toString());
        req.setMode("dql");
        unids = api.query(
            dataSourceSupplier.get(),
            "execute", //$NON-NLS-1$
            req,
            RichTextRepresentation.HTML,
            Integer.MAX_VALUE,
            0)
            .stream()
            .map(doc -> (Map<String, Object>) doc.get("@meta"))
            .map(meta -> (String) meta.get("unid"))
            .collect(Collectors.toList());
      }

      for (String unid : unids) {
        api.deleteDocument(dataSourceSupplier.get(), unid, "default", RichTextRepresentation.MIME);
      }
      // BulkUnids bulkUnids = new BulkUnids();
      // bulkUnids.setUnids(new ArrayList<>(unids));
      // bulkUnids.setMode("default");
      // api.bulkDeleteDocuments(dataSourceSupplier.get(), bulkUnids);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<CommunicationEntity> select(SelectQuery query) {
    String entityName = query.name();
    EntityMetadata mapping = EntityUtil.getClassMapping(entityName);

    QueryConverterResult queryResult = QueryConverter.select(query, mapping);

    long skip = queryResult.getSkip();
    long limit = queryResult.getLimit();

    // Sorting is not available in queries, and this is left as a reminder
    // if intentionally ignoring them
    @SuppressWarnings("unused")
    List<Sort<?>> sorts = query.sorts();

    QueryRequest req = new QueryRequest();
    req.setForms(Arrays.asList(entityName));
    req.setQuery(queryResult.getStatement().toString());
    req.setMode("dql");
    try(DataApi api = getDataApi()) {
      List<Map<String, Object>> docs = api.query(
          dataSourceSupplier.get(),
          "execute", //$NON-NLS-1$
          req,
          RichTextRepresentation.HTML,
          (int) (limit < 1 ? Integer.MAX_VALUE : limit),
          (int) (skip < 1 ? 0 : skip));
      return entityConverter.convertDocuments(entityName, docs, mapping);
    } catch (ProcessingException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long count(String documentCollection) {
    DQLTerm dql = DQL.item(DominoConstants.FIELD_NAME).isEqualTo(documentCollection);
    QueryRequest req = new QueryRequest();
    req.setForms(Arrays.asList(documentCollection));
    req.setQuery(dql.toString());
    try(DataApi api = getDataApi()) {
      return api.query(
          dataSourceSupplier.get(),
          "execute", //$NON-NLS-1$
          req,
          RichTextRepresentation.HTML,
          Integer.MAX_VALUE,
          0)
          .size();
    } catch (ProcessingException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<ViewInfo> getViewInfo() {
    try(DataApi dataApi = getDataApi()) {
      return dataApi.fetchViews(dataSourceSupplier.get(), "all", true)
          .stream()
          .map(view -> {
            String typeString = view.getType();
            if(typeString == null || typeString.isEmpty()) {
              typeString = "VIEW";
            }
            ViewInfo.Type type = ViewInfo.Type.valueOf(typeString.toUpperCase());
            // TODO columns
            return new ViewInfoImpl(type, view.getTitle(), view.getAliases(), view.getUnid(), "", Collections.emptyList());
          });
    }
  }

  @Override
  public void close() {

  }

  private DataApi getDataApi() {
    return RestClientBuilder.newBuilder()
        .baseUri(baseUriSupplier.get())
        .register((ClientRequestFilter) (ctx) -> {
          ctx.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSupplier.get()); //$NON-NLS-1$
        })
        .build(DataApi.class);
  }
  
  private Predicate<CommunicationEntity> distinctByUnid() {
    final Set<Object> seen = new HashSet<>();
    return t -> seen.add(t.find(DominoConstants.FIELD_ID).get().get());
  }
  
  @SuppressWarnings("unchecked")
  private Stream<CommunicationEntity> viewQuery(String entityName, String viewName,
      PageRequest pagination, Sort sorts,
      int maxLevel, boolean docsOnly, ViewQuery viewQuery, boolean singleResult,
      boolean documents) {

    EntityMetadata mapping = EntityUtil.getClassMapping(entityName);
    try(DataApi dataApi = getDataApi()) {
      Integer count = null;
      Integer skip = 0;
      if(pagination != null) {
        skip = (int)(pagination.size() * (pagination.page()-1));
        count = pagination.size();

        if(skip > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Domino does not support skipping more than Integer.MAX_VALUE entries");
        }
      }
      
      String key = null;
      Boolean keyAllowPartial = null;
      if(viewQuery != null) {
        if(viewQuery.getKey() != null) {
          key = String.valueOf(viewQuery.getKey());
          keyAllowPartial = !viewQuery.isExact();
        }
      }
      
      String sortColumn = null;
      String direction = null;
      if(sorts != null) {
        sortColumn = EntityUtil.findItemName(sorts.property(), mapping);
        direction = sorts.isAscending() ? "asc" : "desc";
      }
      
      List<Map<String, Object>> entries =
          (List<Map<String, Object>>) (List<?>) dataApi.fetchViewEntries(
              viewName,
              this.dataSourceSupplier.get(),
              count,
              docsOnly ? "documents": "all",
              skip,
              key,
              keyAllowPartial,
              documents,
              null,
              sortColumn,
              direction,
              null,
              RichTextRepresentation.HTML,
              "default",
              null);
      Stream<CommunicationEntity> result = entityConverter.convertDocuments(entityName, entries, mapping);
      if(singleResult) {
        result = result.limit(1);
      }
      return result;
    } catch (ProcessingException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<CommunicationEntity> getByName(String entityName, String name, String userName) {
    return Optional.empty();
  }

  @Override
  public Optional<CommunicationEntity> getProfileDocument(String entityName, String profileName,
      String userName) {
    return Optional.empty();
  }

  @Override
  public String readCalendarRange(TemporalAccessor start, TemporalAccessor end,
      PageRequest pagination) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<String> readCalendarEntry(String uid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String createCalendarEntry(String icalData, boolean sendInvitations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateCalendarEntry(String uid, String icalData, String comment,
      boolean sendInvitations, boolean overwrite, String recurId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeCalendarEntry(String uid, CalendarModScope scope, String recurId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public AccessRights queryEffectiveAccess() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommunicationEntity send(CommunicationEntity entity, boolean attachForm,
      boolean computeWithForm, boolean save) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OffsetDateTime queryLastModified() {
    throw new UnsupportedOperationException();
  }
}
